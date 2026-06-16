package me.doubao.oscillochord.domain.chord

import org.junit.Assert.*
import org.junit.Test

class ChordDetectorTest {
    private val detector = ChordDetector()

    @Test
    fun `C major triad`() {
        val r = detector.identify(setOf(60, 64, 67))
        assertEquals("M", r?.abbreviation)
        assertEquals(60, r?.root)
    }

    @Test
    fun `A minor triad`() {
        val r = detector.identify(setOf(69, 72, 76))
        assertEquals("m", r?.abbreviation)
        assertEquals(69, r?.root)
    }

    @Test
    fun `G7 dominant`() {
        val r = detector.identify(setOf(67, 71, 74, 77))
        assertEquals("7", r?.abbreviation)
        assertEquals(67, r?.root)
    }

    @Test
    fun `diminished triad`() {
        val r = detector.identify(setOf(62, 65, 68))
        assertEquals("dim", r?.abbreviation)
    }

    @Test
    fun `inversion still matches`() {
        // E4, G4, C5 = C major first inversion, root is C5 (MIDI 72)
        val r = detector.identify(setOf(64, 67, 72))
        assertEquals("M", r?.abbreviation)
        assertEquals(72, r?.root)
    }

    @Test
    fun `two notes is not a chord`() {
        assertNull(detector.identify(setOf(60, 64)))
    }

    @Test
    fun `unrecognized cluster`() {
        assertNull(detector.identify(setOf(60, 61, 62)))
    }

    @Test
    fun `sus4 identified`() {
        val r = detector.identify(setOf(60, 65, 67))
        assertEquals("sus4", r?.abbreviation)
    }
}
