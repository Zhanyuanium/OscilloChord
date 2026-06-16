package me.doubao.oscillochord.domain.lissajous

import org.junit.Assert.assertEquals
import org.junit.Test

class LissajousProjectorTest {
    private val projector = LissajousProjector()

    @Test
    fun `2 signals degenerate to classic Lissajous`() {
        // [1,0] → angles (0, π): x=1*cos(0)+0*cos(π)=1, y=1*sin(0)+0*sin(π)=0
        val (x1, y1) = projector.project(floatArrayOf(1.0f, 0.0f))
        assertEquals(1.0f, x1, 0.01f)
        assertEquals(0.0f, y1, 0.01f)

        // [0,1] → x=0+1*cos(π)=-1, y=0+1*sin(π)=0
        val (x2, y2) = projector.project(floatArrayOf(0.0f, 1.0f))
        assertEquals(-1.0f, x2, 0.01f)
        assertEquals(0.0f, y2, 0.01f)
    }

    @Test
    fun `3 signals project correctly`() {
        // angles: 0, 2π/3, 4π/3. cos: 1, -0.5, -0.5. sin: 0, 0.866, -0.866
        val (x, y) = projector.project(floatArrayOf(1.0f, 1.0f, 1.0f))
        assertEquals(0.0f, x, 0.01f)
        assertEquals(0.0f, y, 0.01f)
    }

    @Test
    fun `single signal projects to x-axis`() {
        val (x, y) = projector.project(floatArrayOf(1.0f))
        assertEquals(1.0f, x, 0.01f)
        assertEquals(0.0f, y, 0.01f)
    }

    @Test
    fun `empty signals returns zero`() {
        val (x, y) = projector.project(floatArrayOf())
        assertEquals(0.0f, x, 0.01f)
        assertEquals(0.0f, y, 0.01f)
    }
}
