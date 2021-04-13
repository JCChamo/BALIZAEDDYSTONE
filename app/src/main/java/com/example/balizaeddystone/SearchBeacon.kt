package com.example.balizaeddystone

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.altbeacon.beacon.*
import java.lang.reflect.Method

class SearchBeacon : AppCompatActivity(), View.OnClickListener, BeaconConsumer, RangeNotifier {

    var actionBar: ActionBar? = null
    private lateinit var mProgressBar: ProgressBar
    private lateinit var bluetoothManager : BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var name: TextView
    private lateinit var mac: TextView
    private lateinit var voltage: TextView
    private lateinit var temp: TextView
    private lateinit var advertCount: TextView
    private lateinit var uptime: TextView
    private val REQUEST_ENABLE_BT = 1
    var colorDrawable: ColorDrawable? = null
    private lateinit var scanButton: Button
    private lateinit var dataButton: Button
    val MY_PERMISSIONS_REQUEST_LOCATION = 99
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanning = false
    private lateinit var mLeScanCallback : ScanCallback
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var scanResult : ScanResult


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.scanButton)
        dataButton = findViewById(R.id.dataButton)
        mProgressBar = findViewById(R.id.progressbar)
        name = findViewById(R.id.name)
        mac = findViewById(R.id.mac)
        voltage = findViewById(R.id.voltage)
        temp = findViewById(R.id.temp)
        advertCount = findViewById(R.id.advertCount)
        uptime = findViewById(R.id.uptime)

        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            MY_PERMISSIONS_REQUEST_LOCATION
        )

        actionBar = supportActionBar
        colorDrawable = ColorDrawable(Color.parseColor("#cfff95"))
        actionBar!!.setBackgroundDrawable(colorDrawable)
        mProgressBar.visibility = View.GONE

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

//        beaconManager = BeaconManager.getInstanceForApplication(this.applicationContext)
//        beaconManager.foregroundBetweenScanPeriod = 2000
//        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))
//        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT))
//        beaconManager.bind(this)

        scanButton.setOnClickListener(this)
        dataButton.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.scanButton -> {
                if (checkBluetoothConnectivity()) {
                    dataButton.visibility = View.VISIBLE
                    progressBarAction()
                    searchDevice()
                }
            }
            R.id.dataButton -> {
                voltage.visibility = View.VISIBLE
                temp.visibility = View.VISIBLE
                advertCount.visibility = View.VISIBLE
                uptime.visibility = View.VISIBLE
                getData()
            }
        }
    }

    private fun searchDevice() {
        mLeScanCallback = object : ScanCallback(){
            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e(":::", "ERROR: $errorCode")
            }

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if (result?.device?.name == "HA_V16"){
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice(result?.device?.address)
//                    for (i in result.scanRecord?.bytes?.indices!!)
//                        Log.d(":::", String.format("Byte %d: %02X", i, result.scanRecord?.bytes!![i]))
                    name.text = bluetoothDevice.name
                    mac.text = bluetoothDevice.address

                    scanResult = result
                }
            }
        }
        scanLeDevice()
    }

    private fun getData(){
        voltage.text = "VOLTAJE: " + (hexadecimalToDecimal(
            (scanResult.scanRecord?.bytes!![14].toString()))*256 +
                hexadecimalToDecimal(scanResult.scanRecord?.bytes!![15].toString())).toString() + " mV"

        temp.text = "TEMPERATURA: " + (hexadecimalToDecimal(
            (scanResult.scanRecord?.bytes!![16].toString())) +
                hexadecimalToDecimal(scanResult.scanRecord?.bytes!![17].toString())/256).toString() + " ºC"

//        uptime.text = "TIEMPO: " + (hexadecimalToDecimal(
//            (scanResult.scanRecord?.bytes!![24].toString())) =>
//        hexadecimalToDecimal(scanResult.scanRecord?.bytes!![25].toString())).toString() + " ms"
    }

    private fun scanLeDevice() {
        val SCAN_PERIOD = 2000L
        bluetoothLeScanner?.let { scanner ->
            if (!scanning) {
                Handler().postDelayed({
                    scanning = false
                    scanner.stopScan(mLeScanCallback)
                }, SCAN_PERIOD)
                scanning = true
                scanner.startScan(mLeScanCallback)
            } else {
                scanning = false
                scanner.stopScan(mLeScanCallback)
            }
        }
    }

    private fun checkBluetoothConnectivity() : Boolean {
        if(!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return false
        }
        return true
    }

    private fun progressBarAction(){
        mProgressBar.visibility = View.VISIBLE
        Handler().postDelayed({
            mProgressBar.visibility = View.GONE
        }, 2000)
    }

    override fun onBeaconServiceConnect() {
//        val region = Region("all-beacons-region", null, null, null)
//        beaconManager.startRangingBeaconsInRegion(region)
//        beaconManager.addRangeNotifier(this)
//        Toast.makeText(applicationContext, "BUSCANDO BEACONS", Toast.LENGTH_SHORT).show()
    }

    override fun didRangeBeaconsInRegion(p0: MutableCollection<Beacon>?, p1: Region?) {
        if (p0!!.isEmpty())
            Log.d(":::", "NO SE HAN DETECTADO BEACONS")
        else {
            p0!!.forEach {
                if (it.serviceUuid == 0xfeaa && it.beaconTypeCode == 0x00) {
                    if (it.extraDataFields.size > 0) {
                        //it.extraDataFields[0].toString()
                    }
                }
            }
        }
    }

    private fun hexadecimalToDecimal(hexaDecimalN : String) : Long {
        var i = hexaDecimalN.length - 1
        var decimalN: Long = 0
        var base = 1
        while(i >= 0) {
            val charAtPos = hexaDecimalN[i]

            val lastDigit = if((charAtPos >= '0') && (charAtPos <= '9')) {
                charAtPos - '0'
            } else if((charAtPos >= 'A') && (charAtPos <= 'F')) {
                charAtPos.toInt() - 55
            } else if((charAtPos >= 'a') && (charAtPos <= 'f')) {
                charAtPos.toInt() - 87
            } else {
                0
            }

            decimalN += lastDigit * base
            base *= 16

            i--
        }
        return decimalN
    }
}
