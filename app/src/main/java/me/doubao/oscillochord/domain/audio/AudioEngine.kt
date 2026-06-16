package me.doubao.oscillochord.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AudioEngine {
    val sampleRate = 44100
    private val bufferSize: Int by lazy {
        try {
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        } catch (e: RuntimeException) {
            // Running in unit test environment without Android runtime
            4096
        }
    }

    private var audioTrack: AudioTrack? = null
    private val oscillators = mutableMapOf<Int, Oscillator>()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var baseFrequency = 440.0
    private var waveform = Waveform.SINE

    private val _sampleSnapshot = MutableSharedFlow<Map<Int, Float>>(replay = 0)
    val sampleSnapshot: SharedFlow<Map<Int, Float>> = _sampleSnapshot

    fun setBaseFrequency(hz: Double) {
        baseFrequency = hz
        oscillators.values.forEach { it.baseFrequency = hz }
    }

    fun setWaveform(waveform: Waveform) {
        this.waveform = waveform
        oscillators.values.forEach { it.waveform = waveform }
    }

    fun noteOn(midiNote: Int) {
        if (oscillators.containsKey(midiNote)) return
        oscillators[midiNote] = Oscillator(
            midiNote = midiNote,
            baseFrequency = baseFrequency,
            waveform = waveform
        )
        ensurePlaying()
    }

    fun noteOff(midiNote: Int) {
        oscillators.remove(midiNote)
        if (oscillators.isEmpty()) {
            stop()
        }
    }

    fun allNotesOff() {
        oscillators.clear()
        stop()
    }

    private fun ensurePlaying() {
        if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) return
        start()
    }

    private fun start() {
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            audioTrack?.play()
        } catch (e: RuntimeException) {
            // Unit test environment: no Android audio runtime available
            audioTrack = null
            return
        }
        job = scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive) {
                val active = oscillators.toMap()
                if (active.isEmpty()) break

                for (i in buffer.indices) {
                    var sum = 0.0f
                    for ((_, osc) in active) {
                        sum += osc.nextSample()
                    }
                    // Normalize to prevent clipping
                    sum /= active.size.coerceAtLeast(1)
                    buffer[i] = (sum * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)

                // Emit sample snapshot for oscilloscope
                val snapshot = active.mapValues { (_, osc) -> osc.nextSample() }
                _sampleSnapshot.emit(snapshot)
            }
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun destroy() {
        allNotesOff()
        scope.cancel()
    }

    val activeNoteCount: Int get() = oscillators.size
    val activeNotes: Set<Int> get() = oscillators.keys.toSet()
}
