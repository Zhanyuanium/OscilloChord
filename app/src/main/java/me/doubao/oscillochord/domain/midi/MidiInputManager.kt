package me.doubao.oscillochord.domain.midi

import android.content.Context
import android.media.midi.*

class MidiInputManager(
    private val context: Context,
    private val onNoteOn: (Int) -> Unit,
    private val onNoteOff: (Int) -> Unit
) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private val openedDevices = mutableMapOf<Int, MidiDevice>()

    fun startScan() {
        midiManager.devices?.forEach { device ->
            if (device.inputPortCount > 0) openDevice(device)
        }

        midiManager.registerDeviceCallback(object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo) {
                if (device.inputPortCount > 0) openDevice(device)
            }
            override fun onDeviceRemoved(device: MidiDeviceInfo) {
                openedDevices.remove(device.id)?.close()
            }
        }, null)
    }

    private fun openDevice(deviceInfo: MidiDeviceInfo) {
        midiManager.openDevice(deviceInfo, { device ->
            openedDevices[deviceInfo.id] = device
            // TODO: Implement actual MIDI data receiving via MidiReceiver
            // For complete MIDI input support, use a polling thread on the
            // MidiInputPort's containing MidiReceiver, or implement a
            // MidiDeviceService for system-level MIDI routing.
        }, null)
    }

    fun destroy() {
        openedDevices.values.forEach { it.close() }
        openedDevices.clear()
    }
}
