package me.doubao.oscillochord.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    state: SettingsState,
    onOctaveCountChange: (Int) -> Unit,
    onBlackKeyLayoutChange: (String) -> Unit,
    onSlideModeChange: (String) -> Unit,
    onShowNoteLabelsChange: (Boolean) -> Unit,
    onWaveformChange: (String) -> Unit,
    onBaseFrequencyChange: (Double) -> Unit,
    onTuningSystemChange: (String) -> Unit,
    onTrailFadeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Keyboard section
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("键盘", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("八度数: ${state.octaveCount}", style = MaterialTheme.typography.bodySmall)
                Slider(value = state.octaveCount.toFloat(),
                    onValueChange = { onOctaveCountChange(it.toInt()) },
                    valueRange = 1f..5f, steps = 3)
                Spacer(Modifier.height(8.dp))

                // Segmented button: key layout
                Text("黑键布局", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val layoutOptions = listOf("PIANO" to "钢琴式", "EQUAL_WIDTH" to "等宽式")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    layoutOptions.forEachIndexed { index, (key, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, layoutOptions.size),
                            onClick = { onBlackKeyLayoutChange(key) },
                            selected = state.blackKeyLayout == key
                        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Segmented button: slide mode
                Text("滑动行为", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val slideOptions = listOf("FOLLOW_KEYS" to "跟随按键", "SHIFT_OCTAVE" to "切换八度")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    slideOptions.forEachIndexed { index, (key, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, slideOptions.size),
                            onClick = { onSlideModeChange(key) },
                            selected = state.slideMode == key
                        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Toggle: show note labels (no extra text)
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("显示音名", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    Switch(checked = state.showNoteLabels,
                        onCheckedChange = onShowNoteLabelsChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Audio section
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("音频", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                // Dropdown for waveform
                var expanded by remember { mutableStateOf(false) }
                val waveforms = listOf("SINE" to "正弦波", "SQUARE" to "方波",
                    "TRIANGLE" to "三角波", "SAWTOOTH" to "锯齿波")
                val currentLabel = waveforms.find { it.first == state.waveform }?.second ?: "正弦波"
                ExposedDropdownMenuBox(expanded = expanded,
                    onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("乐器类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        waveforms.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) },
                                onClick = { onWaveformChange(key); expanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("标准音频率: ${String.format("%.0f", state.baseFrequency)} Hz",
                    style = MaterialTheme.typography.bodySmall)
                Slider(value = state.baseFrequency.toFloat(),
                    onValueChange = { onBaseFrequencyChange(it.toDouble()) },
                    valueRange = 415f..466f)

                Spacer(Modifier.height(8.dp))
                Text("律制", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val tuningOptions = listOf(
                    "EQUAL" to "十二平均律",
                    "JUST" to "纯律",
                    "PYTHAGOREAN" to "五度相生率"
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tuningOptions.forEachIndexed { index, (key, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, tuningOptions.size),
                            onClick = { onTuningSystemChange(key) },
                            selected = state.tuningSystem == key
                        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Oscilloscope section
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("示波器", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("轨迹渐隐", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    Switch(checked = state.trailFadeEnabled,
                        onCheckedChange = onTrailFadeChange)
                }
            }
        }
    }
}
