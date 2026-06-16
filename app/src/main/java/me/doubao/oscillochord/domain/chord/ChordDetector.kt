package me.doubao.oscillochord.domain.chord

data class ChordResult(
    val abbreviation: String,
    val root: Int,
    val template: ChordTemplate
)

class ChordDetector {
    fun identify(midiNotes: Set<Int>): ChordResult? {
        if (midiNotes.size < 3) return null

        for (rootMidi in midiNotes.sorted()) {
            val intervalsMod12 = midiNotes.map { note ->
                ((note - rootMidi) % 12 + 12) % 12
            }.toSet()

            for (template in ChordDatabase.templates) {
                if (intervalsMod12 == template.semitones) {
                    return ChordResult(
                        abbreviation = template.abbreviation,
                        root = rootMidi,
                        template = template
                    )
                }
            }
        }

        return null
    }
}
