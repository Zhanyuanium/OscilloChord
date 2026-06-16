package me.doubao.oscillochord.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun InfoPanel(
    state: InfoPanelState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Chord abbreviation title (empty if not recognized)
        Text(
            text = state.chordAbbreviation,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        if (state.chordAbbreviation.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Note list
        state.notes.forEach { note ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = note.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (note.isRoot)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${note.intervalFromRoot}  ${"%.2f".format(Locale.ROOT, note.frequencyHz)}Hz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (state.notes.isEmpty()) {
            Text(
                text = "按下键盘开始",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
