package me.doubao.oscillochord.ui.settings

import androidx.compose.foundation.background
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Keyboard
        SectionHeader("键盘")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("八度数: ${state.octaveCount}", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.octaveCount.toFloat(),
                onValueChange = { onOctaveCountChange(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.weight(1f)
            )
        }
        ToggleRow("黑键布局", state.blackKeyLayout == "PIANO",
            { onBlackKeyLayoutChange(if (it) "PIANO" else "EQUAL_WIDTH") }, "钢琴式", "等宽式")
        ToggleRow("滑动行为", state.slideMode == "FOLLOW_KEYS",
            { onSlideModeChange(if (it) "FOLLOW_KEYS" else "SHIFT_OCTAVE") }, "跟随按键", "切换八度")
        ToggleRow("显示音名", state.showNoteLabels, onShowNoteLabelsChange, "开", "关")

        Spacer(modifier = Modifier.height(12.dp))

        // Audio
        SectionHeader("音频")
        WaveformDropdown(state.waveform, onWaveformChange)
        Spacer(modifier = Modifier.height(4.dp))
        Text("标准音频率: ${String.format("%.0f", state.baseFrequency)} Hz", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = state.baseFrequency.toFloat(),
            onValueChange = { onBaseFrequencyChange(it.toDouble()) },
            valueRange = 415f..466f
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun ToggleRow(
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun WaveformDropdown(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("SINE" to "正弦波", "SQUARE" to "方波", "TRIANGLE" to "三角波", "SAWTOOTH" to "锯齿波")
    val currentLabel = options.find { it.first == current }?.second ?: "正弦波"

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("乐器类型: $currentLabel", style = MaterialTheme.typography.bodySmall)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(key); expanded = false }
                )
            }
        }
    }
}
