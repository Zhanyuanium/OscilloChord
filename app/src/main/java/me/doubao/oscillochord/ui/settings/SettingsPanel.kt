package me.doubao.oscillochord.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPanel(
    state: SettingsState,
    onOctaveCountChange: (Int) -> Unit,
    onBlackKeyLayoutChange: (String) -> Unit,
    onSlideModeChange: (String) -> Unit,
    onShowNoteLabelsChange: (Boolean) -> Unit,
    onWaveformChange: (String) -> Unit,
    onBaseFrequencyChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Keyboard section
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("键盘", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("八度数: ${state.octaveCount}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = state.octaveCount.toFloat(),
                    onValueChange = { onOctaveCountChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Spacer(Modifier.height(8.dp))
                LabeledSwitch("黑键布局", state.blackKeyLayout == "PIANO",
                    { onBlackKeyLayoutChange(if (it) "PIANO" else "EQUAL_WIDTH") }, "钢琴式", "等宽式")
                LabeledSwitch("滑动行为", state.slideMode == "FOLLOW_KEYS",
                    { onSlideModeChange(if (it) "FOLLOW_KEYS" else "SHIFT_OCTAVE") }, "跟随按键", "切换八度")
                LabeledSwitch("显示音名", state.showNoteLabels, onShowNoteLabelsChange, "开", "关")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Audio section
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("音频", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                WaveformSelector(state.waveform, onWaveformChange)
                Spacer(Modifier.height(8.dp))
                Text("标准音频率: ${String.format("%.0f", state.baseFrequency)} Hz",
                    style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = state.baseFrequency.toFloat(),
                    onValueChange = { onBaseFrequencyChange(it.toDouble()) },
                    valueRange = 415f..466f
                )
            }
        }
    }
}

@Composable
private fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    labelOn: String,
    labelOff: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            if (checked) labelOn else labelOff,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun WaveformSelector(current: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "SINE" to "正弦波",
        "SQUARE" to "方波",
        "TRIANGLE" to "三角波",
        "SAWTOOTH" to "锯齿波"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = current == key,
                onClick = { onSelect(key) },
                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
            )
        }
    }
}
