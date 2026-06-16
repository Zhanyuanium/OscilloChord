package me.doubao.oscillochord.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top half: info panel + oscilloscope + settings panel
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left: info panel
            Box(
                modifier = Modifier
                    .weight(0.18f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = "和弦信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Center: oscilloscope
            Box(
                modifier = Modifier
                    .weight(0.64f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "示波器",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Right: settings panel
            Box(
                modifier = Modifier
                    .weight(0.18f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Bottom half: piano keyboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "钢琴键盘",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
