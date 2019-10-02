package com.qrscanner.embedded.zxing

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.qrscanner.barcodescanner.BarcodeCallback
import com.qrscanner.barcodescanner.BarcodeResult
import com.qrscanner.barcodescanner.DefaultDecoderFactory
import com.qrscanner.barcodescanner.ZxingScannerView
import com.qrscanner.core.BitMapScaler
import com.qrscanner.core.ZxingUtils
import com.qrscanner.qrscandemo.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import kotlinx.android.synthetic.main.continuous_scan.*
import java.io.File
import java.io.IOException

/**
 * Created by B0204525 on 13,September,2019
 */
class ZxingScannerActivity : AppCompatActivity(), ZxingScannerView.TorchListener, View.OnClickListener
{
    private val TAG = ZxingScannerActivity::class.java.simpleName

    companion object{
        private const val PICK_IMAGE_REQUEST_CODE = 1046
        private const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 1051
    }

    private var barcodeView: ZxingScannerView? = null
    private var beepManager: BeepManager? = null
    private var lastText: String? = null
    private var isTorchEnabled: Boolean = false

    private val callback = object : BarcodeCallback {
        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}

        override fun barcodeResult(result: BarcodeResult) {
            // Prevent duplicate scans
            if (result.text == null || result.text == lastText) return

            lastText = result.text
            barcodeView?.setStatusText(result.text)
            beepManager?.playBeepSoundAndVibrate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.continuous_scan)
        beepManager = BeepManager(this)
        barcodeView = barcode_scanner

        barcodeView?.apply {

            //this.setFullScreenScan()
            this.cameraSettings.isAutoFocusEnabled = true
            this.cameraSettings.isScanInverted = true
            this.cameraSettings.isExposureEnabled = true

            val formats = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39)
            this.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
            this.initializeFromIntent(intent)
            this.decodeContinuous(callback)
        }
    }

    private fun toggleTorch() {
        if (isTorchEnabled) {
            barcodeView?.setTorchOff()
        } else {
            barcodeView?.setTorchOn()
        }
    }

    private fun toggleListeners(enableListener: Boolean) {
        if (!ZxingUtils.hasFlash(applicationContext)) {
            barcodeView?.flashlight?.visibility = View.GONE
        } else {
            when (enableListener) {
                true -> {
                    barcodeView?.flashlight?.setOnClickListener(this)
                    barcodeView?.setTorchListener(this)
                    barcodeView?.galleryPicker?.setOnClickListener(this)
                }
                false -> {
                    barcodeView?.flashlight?.setOnClickListener(null)
                    barcodeView?.setTorchListener(null)
                    barcodeView?.galleryPicker?.setOnClickListener(null)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView?.resume()
        toggleListeners(enableListener = true)

    }

    override fun onPause() {
        super.onPause()
        barcodeView?.pause()
        toggleListeners(enableListener = false)
    }

    override fun onTorchOn() {
        isTorchEnabled = true
        barcodeView?.flashlight?.setImageResource(R.drawable.vector_flash_on)
    }

    override fun onTorchOff() {
        isTorchEnabled = false
        barcodeView?.flashlight?.setImageResource(R.drawable.vector_flash_off)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.flashlight -> toggleTorch()

            R.id.galleryPicker -> pickImage()
        }
    }

    private fun pickImage() {

        if (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Create intent for picking a photo from the gallery
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            if (intent.resolveActivity(packageManager) != null) {

                intent.type = "image/*"
                intent.putExtra("crop", "true")
                intent.putExtra("scale", true)
                intent.putExtra("aspectX", 16)
                intent.putExtra("aspectY", 9)
                startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST_CODE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // pick image after request permission success
                pickImage()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == PICK_IMAGE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) return

            intent?.data?.let {photoUri ->
                try {
                    var selectedBitmap = uriToBitmap(photoUri)

                    barcodeView?.let {
                        val viewFinder = it.viewFinder
                        val resizedBitmap = BitMapScaler.scaleToFill(selectedBitmap!!, viewFinder.width, viewFinder.width)
                        it.scanBitmap(resizedBitmap)
                    }

                } catch (e: IOException) { e.printStackTrace() }
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap { return MediaStore.Images.Media.getBitmap(contentResolver, uri) }

    private fun uriToImageFile(uri: Uri): File? {
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                val filePath = cursor.getString(columnIndex)
                cursor.close()
                return File(filePath)
            }
            cursor.close()
        }
        return null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return barcodeView?.onKeyDown(keyCode, event) ?: true || super.onKeyDown(keyCode, event)
    }
}