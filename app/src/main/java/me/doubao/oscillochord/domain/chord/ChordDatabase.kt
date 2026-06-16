package me.doubao.oscillochord.domain.chord

data class ChordTemplate(
    val abbreviation: String,
    val semitones: Set<Int>
)

object ChordDatabase {
    val templates: List<ChordTemplate> = listOf(
        // Triads
        ChordTemplate("M",    setOf(0, 4, 7)),
        ChordTemplate("m",    setOf(0, 3, 7)),
        ChordTemplate("dim",  setOf(0, 3, 6)),
        ChordTemplate("aug",  setOf(0, 4, 8)),
        // Sevenths
        ChordTemplate("7",    setOf(0, 4, 7, 10)),
        ChordTemplate("M7",   setOf(0, 4, 7, 11)),
        ChordTemplate("m7",   setOf(0, 3, 7, 10)),
        ChordTemplate("dim7", setOf(0, 3, 6, 9)),
        ChordTemplate("m7♭5", setOf(0, 3, 6, 10)),
        // Ninths
        ChordTemplate("9",    setOf(0, 4, 7, 10, 14)),
        ChordTemplate("M9",   setOf(0, 4, 7, 11, 14)),
        // Suspended
        ChordTemplate("sus2", setOf(0, 2, 7)),
        ChordTemplate("sus4", setOf(0, 5, 7)),
        // Add
        ChordTemplate("add9", setOf(0, 4, 7, 14)),
    )
}
