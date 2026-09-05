package com.powerpointremote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.powerpointremote.databinding.ActivityMainBinding
import java.io.IOException
import java.io.PrintWriter
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var writer: PrintWriter? = null
    private var isConnected = false
    private val executor = Executors.newSingleThreadExecutor()

    // UUID estándar del perfil Serial Port (SPP)
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val pairedDevices = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()

    companion object {
        private const val REQUEST_PERMISSIONS = 1001
        private const val REQUEST_ENABLE_BT = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val manager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = manager?.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no tiene Bluetooth", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupListeners()
        checkPermissionsAndLoad()
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener { loadPairedDevices() }

        binding.btnConnect.setOnClickListener {
            if (isConnected) {
                disconnect()
            } else {
                connectToSelected()
            }
        }

        binding.btnNext.setOnClickListener { sendCommand("NEXT") }
        binding.btnPrev.setOnClickListener { sendCommand("PREV") }
        binding.btnStart.setOnClickListener { sendCommand("START") }
        binding.btnEnd.setOnClickListener { sendCommand("END") }
        binding.btnBlack.setOnClickListener { sendCommand("BLACK") }
        binding.btnWhite.setOnClickListener { sendCommand("WHITE") }
    }

    private fun checkPermissionsAndLoad() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            ensureBluetoothEnabled()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                ensureBluetoothEnabled()
            } else {
                Toast.makeText(this, "Se necesitan permisos de Bluetooth", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ensureBluetoothEnabled() {
        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this, getString(R.string.enable_bt), Toast.LENGTH_LONG).show()
        } else {
            loadPairedDevices()
        }
    }

    private fun loadPairedDevices() {
        if (!hasBluetoothPermission()) {
            Toast.makeText(this, "Sin permiso de Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        pairedDevices.clear()
        deviceNames.clear()

        try {
            val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
            for (device in bonded) {
                pairedDevices.add(device)
                val name = device.name ?: device.address
                deviceNames.add("$name\n${device.address}")
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        if (deviceNames.isEmpty()) {
            deviceNames.add(getString(R.string.no_paired))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, deviceNames)
        binding.spinnerDevices.adapter = adapter
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun connectToSelected() {
        if (!hasBluetoothPermission()) {
            Toast.makeText(this, "Sin permiso de Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val position = binding.spinnerDevices.selectedItemPosition
        if (position < 0 || position >= pairedDevices.size) {
            Toast.makeText(this, "Selecciona un dispositivo válido", Toast.LENGTH_SHORT).show()
            return
        }

        val device = pairedDevices[position]

        binding.tvStatus.text = getString(R.string.status_connecting)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
        binding.btnConnect.isEnabled = false

        executor.execute {
            var tmpSocket: BluetoothSocket? = null
            try {
                // Método principal
                tmpSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothAdapter?.cancelDiscovery()
                tmpSocket.connect()

                socket = tmpSocket
                writer = PrintWriter(tmpSocket.outputStream, true)

                runOnUiThread {
                    isConnected = true
                    binding.tvStatus.text = getString(R.string.status_connected)
                    binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                    binding.btnConnect.text = getString(R.string.disconnect)
                    binding.btnConnect.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.error)
                    binding.layoutControls.visibility = View.VISIBLE
                    binding.cardDevices.visibility = View.GONE
                    binding.btnConnect.isEnabled = true
                    Toast.makeText(this, "¡Conectado por Bluetooth!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                // Fallback: reflection method (algunos dispositivos lo necesitan)
                try {
                    tmpSocket?.close()
                    val method = device.javaClass.getMethod(
                        "createRfcommSocket", Int::class.javaPrimitiveType
                    )
                    tmpSocket = method.invoke(device, 1) as BluetoothSocket
                    tmpSocket.connect()

                    socket = tmpSocket
                    writer = PrintWriter(tmpSocket.outputStream, true)

                    runOnUiThread {
                        isConnected = true
                        binding.tvStatus.text = getString(R.string.status_connected)
                        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                        binding.btnConnect.text = getString(R.string.disconnect)
                        binding.btnConnect.backgroundTintList =
                            ContextCompat.getColorStateList(this, R.color.error)
                        binding.layoutControls.visibility = View.VISIBLE
                        binding.cardDevices.visibility = View.GONE
                        binding.btnConnect.isEnabled = true
                        Toast.makeText(this, "¡Conectado por Bluetooth!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e2: Exception) {
                    runOnUiThread {
                        isConnected = false
                        binding.tvStatus.text = getString(R.string.status_disconnected)
                        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
                        binding.btnConnect.isEnabled = true
                        Toast.makeText(
                            this,
                            "Error de conexión: ${e.message}\n¿Está el servidor corriendo y el notebook emparejado?",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: SecurityException) {
                runOnUiThread {
                    binding.btnConnect.isEnabled = true
                    Toast.makeText(this, "Error de permisos Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun disconnect() {
        executor.execute {
            try {
                writer?.close()
                socket?.close()
            } catch (_: Exception) {
            }
            socket = null
            writer = null

            runOnUiThread {
                isConnected = false
                binding.tvStatus.text = getString(R.string.status_disconnected)
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
                binding.btnConnect.text = getString(R.string.connect)
                binding.btnConnect.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.accent)
                binding.layoutControls.visibility = View.GONE
                binding.cardDevices.visibility = View.VISIBLE
                Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendCommand(cmd: String) {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado", Toast.LENGTH_SHORT).show()
            return
        }

        executor.execute {
            try {
                writer?.println(cmd)
                writer?.flush()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error al enviar: ${e.message}", Toast.LENGTH_SHORT).show()
                    disconnect()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        executor.shutdown()
    }
}
