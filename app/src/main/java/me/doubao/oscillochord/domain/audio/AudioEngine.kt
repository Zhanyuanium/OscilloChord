package me.doubao.oscillochord.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import me.doubao.oscillochord.domain.chord.TuningSystem
import java.util.concurrent.ConcurrentHashMap

class AudioEngine {
    val sampleRate = 44100
    private val bufferSize: Int by lazy {
        try {
            AudioTrack.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
        } catch (e: RuntimeException) { 4096 }
    }

    private var audioTrack: AudioTrack? = null
    private val oscillators = ConcurrentHashMap<Int, Oscillator>()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var baseFrequency = 440.0
    private var waveform = Waveform.SINE
    private var tuningSystem = TuningSystem.EQUAL

    fun setBaseFrequency(hz: Double) {
        baseFrequency = hz
        oscillators.values.forEach { it.baseFrequency = hz }
    }

    fun setWaveform(waveform: Waveform) {
        this.waveform = waveform
        oscillators.values.forEach { it.waveform = waveform }
    }

    fun setTuningSystem(system: TuningSystem) {
        tuningSystem = system
        oscillators.values.forEach { it.tuningSystem = system }
    }

    fun noteOn(midiNote: Int) {
        if (oscillators.containsKey(midiNote)) return
        oscillators[midiNote] = Oscillator(
            midiNote = midiNote, baseFrequency = baseFrequency,
            waveform = waveform, amplitude = 1.0f,
            tuningSystem = tuningSystem
        )
        ensurePlaying()
    }

    fun noteOff(midiNote: Int) {
        oscillators.remove(midiNote)
        // AudioTrack stays alive — no race between stop/start
    }

    @Volatile
    private var engineRunning = false

    private fun ensurePlaying() {
        if (engineRunning) return
        engineRunning = true

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize)
                .build()
            audioTrack?.play()
        } catch (e: RuntimeException) {
            engineRunning = false; return
        }

        job = scope.launch {
            val bufSz = bufferSize / 2 // short count
            val buffer = ShortArray(bufSz)
            var smoothCount = 1.0f

            while (isActive) {
                val active = HashMap(oscillators)
                val targetCount = active.size.toFloat().coerceAtLeast(1f)
                val lerpSpeed = 0.005f

                if (active.isEmpty()) {
                    buffer.fill(0)
                    audioTrack?.write(buffer, 0, buffer.size)
                    smoothCount = 1.0f
                    continue
                }

                for (i in buffer.indices) {
                    smoothCount += (targetCount - smoothCount) * lerpSpeed
                    var sum = 0.0f
                    for (osc in active.values) sum += osc.nextSample()
                    sum /= smoothCount
                    buffer[i] = (sum * Short.MAX_VALUE * 0.9f).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun destroy() {
        engineRunning = false
        job?.cancel()
        oscillators.clear()
        scope.cancel()
        try { audioTrack?.stop() } catch (_: Exception) {}
        try { audioTrack?.release() } catch (_: Exception) {}
        audioTrack = null
    }

    val activeNoteCount: Int get() = oscillators.size
}
