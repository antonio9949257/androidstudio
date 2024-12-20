package com.example.pruebatec

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pruebatec.ui.theme.PruebaTecTheme
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var socket: BluetoothSocket
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar para UART

    private val devicesFound = mutableStateListOf<BluetoothDevice>()

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startBluetoothDiscovery()
            } else {
                Toast.makeText(this, "Bluetooth no habilitado", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si Bluetooth está soportado
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Solicitar habilitar Bluetooth si está apagado
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        }

        // Registrar receptor para encontrar dispositivos Bluetooth
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        // Comenzar la búsqueda
        startBluetoothDiscovery()

        setContent {
            PruebaTecTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Greeting(name = "Usuario", modifier = Modifier.padding(16.dp))

                        Button(onClick = { startBluetoothDiscovery() }) {
                            Text(text = "Buscar dispositivos Bluetooth")
                        }

                        DeviceList(devices = devicesFound)
                    }
                }
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (!devicesFound.contains(it)) { // Evitar duplicados
                        devicesFound.add(it)
                        Toast.makeText(applicationContext, "Dispositivo encontrado: ${it.name ?: "Desconocido"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun startBluetoothDiscovery() {
        bluetoothAdapter?.startDiscovery()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()

            val outputStream: OutputStream = socket.outputStream
            val inputStream: InputStream = socket.inputStream

            Toast.makeText(this, "Conexión exitosa con ${device.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun DeviceList(devices: List<BluetoothDevice>) {
        if (devices.isEmpty()) {
            Text(text = "No se encontraron dispositivos.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(devices) { device ->
                    Button(
                        onClick = { connectToDevice(device) },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(text = device.name ?: "Dispositivo desconocido")
                    }
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "¡Hola, $name!",
            modifier = modifier
                .padding(16.dp)                          // Relleno alrededor del texto
                .background(Color(0xFF6200EE))            // Fondo morado
                .padding(16.dp)                          // Relleno adicional
                .fillMaxWidth()                          // Ocupa todo el ancho disponible
                .wrapContentHeight()                     // Ajuste automático en altura
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        if (::socket.isInitialized) {
            socket.close()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGreeting() {
    PruebaTecTheme {
        Greeting(name = "Vista Previa", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier) {
    Text(
        text = "¡Hola, $name!",
        modifier = modifier
            .padding(16.dp)                          // Relleno alrededor del texto
            .background(Color(0xFF6200EE))            // Fondo morado
            .padding(16.dp)                          // Relleno adicional
            .fillMaxWidth()                          // Ocupa todo el ancho disponible
            .wrapContentHeight()                     // Ajuste automático en altura
    )
}
