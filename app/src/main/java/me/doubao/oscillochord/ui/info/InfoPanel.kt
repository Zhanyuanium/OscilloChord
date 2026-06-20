package me.doubao.oscillochord.ui.info

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.doubao.oscillochord.R
import java.util.Locale

@StringRes
private fun intervalResId(semitones: Int): Int = when (semitones) {
    -1 -> R.string.interval_root
    0 -> R.string.interval_root
    1 -> R.string.interval_minor_second
    2 -> R.string.interval_major_second
    3 -> R.string.interval_minor_third
    4 -> R.string.interval_major_third
    5 -> R.string.interval_perfect_fourth
    6 -> R.string.interval_tritone
    7 -> R.string.interval_perfect_fifth
    8 -> R.string.interval_minor_sixth
    9 -> R.string.interval_major_sixth
    10 -> R.string.interval_minor_seventh
    11 -> R.string.interval_major_seventh
    else -> R.string.interval_root
}

@Composable
fun InfoPanel(
    state: InfoPanelState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Chord title — fixed height regardless of content
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.chordAbbreviation.isNotEmpty()) {
                    Text(
                        text = state.chordAbbreviation,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Note cards — single row each
        state.notes.forEach { note ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (note.isRoot)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(0.35f),
                        color = if (note.isRoot) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(intervalResId(note.intervalSemitones)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(0.25f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "${"%.2f".format(Locale.ROOT, note.frequencyHz)}${stringResource(R.string.unit_hz)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.4f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        if (state.notes.isEmpty()) {
            Text(
                text = stringResource(R.string.info_title_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
