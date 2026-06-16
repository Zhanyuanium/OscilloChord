package me.doubao.oscillochord.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class AudioEngine {
    val sampleRate = 44100

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
        // Don't stop the engine — single coroutine handles empty state gracefully
    }

    private var engineRunning = false

    private fun ensurePlaying() {
        if (engineRunning) return
        engineRunning = true
        job?.cancel()

        job = scope.launch {
            // Create AudioTrack once and keep it alive to avoid startup latency
            val track = try {
                val bufSize = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                ) * 2
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(bufSize).build()
                    .also { it.play(); audioTrack = it }
            } catch (e: RuntimeException) { null }

            if (track == null) { engineRunning = false; return@launch }

            // Use small chunks (~12ms) for low latency. write() blocks until
            // the audio HW consumes the chunk, so smaller = more responsive.
            val chunkSize = 512
            val chunk = ShortArray(chunkSize)
            var smoothCount = 1.0f

            while (isActive) {
                val active = HashMap(oscillators)
                val targetCount = active.size.toFloat().coerceAtLeast(1f)
                val lerpSpeed = 0.01f

                if (active.isEmpty()) {
                    chunk.fill(0)
                    track.write(chunk, 0, chunk.size)
                    smoothCount = 1.0f
                    continue
                }

                for (i in chunk.indices) {
                    smoothCount += (targetCount - smoothCount) * lerpSpeed
                    var sum = 0.0f
                    for (osc in active.values) {
                        sum += osc.nextSample()
                    }
                    sum /= smoothCount
                    chunk[i] = (sum * Short.MAX_VALUE * 0.9f).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                track.write(chunk, 0, chunk.size)
            }
        }
    }

    fun destroy() {
        engineRunning = false
        job?.cancel()
        val t = audioTrack
        audioTrack = null
        try { t?.stop() } catch (_: Exception) {}
        try { t?.release() } catch (_: Exception) {}
        oscillators.clear()
        scope.cancel()
    }

    val activeNoteCount: Int get() = oscillators.size
}
