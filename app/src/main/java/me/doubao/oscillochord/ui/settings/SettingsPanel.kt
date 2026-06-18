package me.doubao.oscillochord.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import me.doubao.oscillochord.R
import me.doubao.oscillochord.domain.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    state: SettingsState,
    onOctaveCountChange: (Int) -> Unit,
    onBlackKeyLayoutChange: (BlackKeyLayoutSetting) -> Unit,
    onSlideModeChange: (SlideModeSetting) -> Unit,
    onShowNoteLabelsChange: (Boolean) -> Unit,
    onWaveformChange: (WaveformSetting) -> Unit,
    onBaseFrequencyChange: (Double) -> Unit,
    onTuningSystemChange: (TuningSetting) -> Unit,
    onTrailFadeChange: (Boolean) -> Unit,
    onTrailLengthChange: (Int) -> Unit,
    onViewModeChange: (ViewModeSetting) -> Unit,
    onNoteNamingChange: (NoteNamingSetting) -> Unit,
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
                Text(stringResource(R.string.settings_keyboard), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_octave_count, state.octaveCount), style = MaterialTheme.typography.bodySmall)
                Slider(value = state.octaveCount.toFloat(),
                    onValueChange = { onOctaveCountChange(it.toInt()) },
                    valueRange = 1f..5f, steps = 3)
                Spacer(Modifier.height(8.dp))

                // Segmented button: key layout
                Text(stringResource(R.string.settings_black_key_layout), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val layoutOptions = BlackKeyLayoutSetting.entries.map {
                    it to stringResource(
                        when (it) {
                            BlackKeyLayoutSetting.PIANO -> R.string.settings_key_layout_piano
                            BlackKeyLayoutSetting.EQUAL_WIDTH -> R.string.settings_key_layout_equal
                        }
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    layoutOptions.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, layoutOptions.size),
                            onClick = { onBlackKeyLayoutChange(value) },
                            selected = state.blackKeyLayout == value
                        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Segmented button: slide mode
                Text(stringResource(R.string.settings_slide_mode), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val slideOptions = SlideModeSetting.entries.map {
                    it to stringResource(
                        when (it) {
                            SlideModeSetting.FOLLOW_KEYS -> R.string.settings_slide_follow_keys
                            SlideModeSetting.SHIFT_OCTAVE -> R.string.settings_slide_shift_octave
                        }
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    slideOptions.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, slideOptions.size),
                            onClick = { onSlideModeChange(value) },
                            selected = state.slideMode == value
                        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Toggle: show note labels
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_show_note_labels), style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    Switch(checked = state.showNoteLabels,
                        onCheckedChange = onShowNoteLabelsChange)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_note_naming), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val namingOptions = NoteNamingSetting.entries.map {
                    it to stringResource(
                        when (it) {
                            NoteNamingSetting.SHARP -> R.string.settings_note_sharp
                            NoteNamingSetting.FLAT -> R.string.settings_note_flat
                        }
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    namingOptions.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, namingOptions.size),
                            onClick = { onNoteNamingChange(value) },
                            selected = state.noteNaming == value
                        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Audio section
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(stringResource(R.string.settings_audio), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                // Dropdown for waveform
                var expanded by remember { mutableStateOf(false) }
                val waveforms = WaveformSetting.entries.map {
                    it to stringResource(
                        when (it) {
                            WaveformSetting.SINE -> R.string.settings_waveform_sine
                            WaveformSetting.SQUARE -> R.string.settings_waveform_square
                            WaveformSetting.TRIANGLE -> R.string.settings_waveform_triangle
                            WaveformSetting.SAWTOOTH -> R.string.settings_waveform_sawtooth
                        }
                    )
                }
                val currentLabel = waveforms.find { it.first == state.waveform }?.second
                    ?: stringResource(R.string.settings_waveform_sine)
                ExposedDropdownMenuBox(expanded = expanded,
                    onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_waveform)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        waveforms.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) },
                                onClick = { onWaveformChange(value); expanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_base_frequency, state.baseFrequency),
                    style = MaterialTheme.typography.bodySmall)
                Slider(value = state.baseFrequency.toFloat(),
                    onValueChange = { onBaseFrequencyChange(it.toDouble()) },
                    valueRange = 415f..466f)

                Spacer(Modifier.height(8.dp))
                var tuningExpanded by remember { mutableStateOf(false) }
                val tuningOptions = TuningSetting.entries.map {
                    it to stringResource(
                        when (it) {
                            TuningSetting.EQUAL -> R.string.settings_tuning_equal
                            TuningSetting.JUST -> R.string.settings_tuning_just
                            TuningSetting.PYTHAGOREAN -> R.string.settings_tuning_pythagorean
                        }
                    )
                }
                val tuningLabel = tuningOptions.find { it.first == state.tuningSystem }?.second
                    ?: stringResource(R.string.settings_tuning_equal)
                ExposedDropdownMenuBox(expanded = tuningExpanded, onExpandedChange = { tuningExpanded = it }) {
                    OutlinedTextField(
                        value = tuningLabel, onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.settings_tuning_system)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tuningExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded = tuningExpanded, onDismissRequest = { tuningExpanded = false }) {
                        tuningOptions.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) },
                                onClick = { onTuningSystemChange(value); tuningExpanded = false })
                        }
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
                Text(stringResource(R.string.settings_oscilloscope), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_trail_fade), style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    Switch(checked = state.trailFadeEnabled,
                        onCheckedChange = onTrailFadeChange)
                }
                Spacer(Modifier.height(8.dp))
                val trailValues = intArrayOf(1024, 2048, 4096, 8192, 16384)
                val trailIdx = trailValues.indexOf(state.trailLength).coerceAtLeast(0)
                Text("${stringResource(R.string.settings_trail_length)}: ${trailValues[trailIdx]}",
                    style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = trailIdx.toFloat(),
                    onValueChange = { onTrailLengthChange(trailValues[it.roundToInt().coerceIn(0, 4)]) },
                    valueRange = 0f..4f,
                    steps = 3
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // View mode
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(stringResource(R.string.settings_view_mode), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                val viewOptions = ViewModeSetting.entries.map {
                    it to stringResource(
                        when (it) {
                            ViewModeSetting.SQUARE -> R.string.settings_view_square
                            ViewModeSetting.WIDE -> R.string.settings_view_wide
                        }
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    viewOptions.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, viewOptions.size),
                            onClick = { onViewModeChange(value) },
                            selected = state.viewMode == value
                        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
