package me.doubao.oscillochord.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class AudioEngine {
    val sampleRate = 44100
    private val bufferSize: Int by lazy {
        try {
            // Use 2x the minimum buffer to avoid underruns and clicks
            AudioTrack.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            ) * 2
        } catch (e: RuntimeException) { 4096 }
    }

    private var audioTrack: AudioTrack? = null
    private val oscillators = ConcurrentHashMap<Int, Oscillator>()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var baseFrequency = 440.0
    private var waveform = Waveform.SINE

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
            midiNote = midiNote, baseFrequency = baseFrequency,
            waveform = waveform, amplitude = 1.0f
        )
        ensurePlaying()
    }

    fun noteOff(midiNote: Int) {
        oscillators.remove(midiNote)
        if (oscillators.isEmpty()) stop()
    }

    private fun ensurePlaying() {
        if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) return
        start()
    }

    private fun start() {
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize).build()
            audioTrack?.play()
        } catch (e: RuntimeException) {
            audioTrack = null; return
        }

        job = scope.launch {
            val bufSize = bufferSize / 2
            val buffer = ShortArray(bufSize)
            var smoothCount = 1.0f
            while (isActive) {
                val active = HashMap(oscillators)
                if (active.isEmpty()) break
                val targetCount = active.size.toFloat().coerceAtLeast(1f)
                val lerpSpeed = 0.005f // smooth transition over ~200 samples ≈ 4.5ms

                for (i in buffer.indices) {
                    smoothCount += (targetCount - smoothCount) * lerpSpeed
                    var sum = 0.0f
                    for (osc in active.values) {
                        sum += osc.nextSample()
                    }
                    sum /= smoothCount
                    buffer[i] = (sum * Short.MAX_VALUE * 0.9f).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    private fun stop() {
        job?.cancel(); job = null
        audioTrack?.stop(); audioTrack?.release()
        audioTrack = null
    }

    fun destroy() { oscillators.clear(); stop(); scope.cancel() }

    val activeNoteCount: Int get() = oscillators.size
}
