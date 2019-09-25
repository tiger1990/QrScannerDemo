package com.airtel.embedded.zxing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.airtel.barcodescanner.AnimatedViewFinder;
import com.airtel.barcodescanner.ZxingScannerView;
import com.airtel.core.BitMapScaler;
import com.airtel.core.ZxingUtils;
import com.airtel.qrscandemo.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.airtel.barcodescanner.BarcodeCallback;
import com.airtel.barcodescanner.BarcodeResult;
import com.airtel.barcodescanner.DefaultDecoderFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

/**
 * This sample performs continuous scanning, displaying the barcode and source image whenever
 * a barcode is scanned.
 */
public class ContinuousCaptureActivity extends Activity implements ZxingScannerView.TorchListener, View.OnClickListener
{
    // PICK_PHOTO_CODE is a constant integer
    public final static int PICK_IMAGE_REQUEST_CODE = 1046;
    private static final String TAG = ContinuousCaptureActivity.class.getSimpleName();
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 1051;
    private ZxingScannerView barcodeView;
    private BeepManager beepManager;
    private String lastText;
    private boolean isTorchEnabled;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }
            lastText = result.getText();
            barcodeView.setStatusText(result.getText());
            beepManager.playBeepSoundAndVibrate();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.continuous_scan);
        beepManager = new BeepManager(this);
        barcodeView = findViewById(R.id.barcode_scanner);
        barcodeView.setFullScreenScan();
        barcodeView.getCameraSettings().setAutoFocusEnabled(true);
        barcodeView.getCameraSettings().setScanInverted(true);
        barcodeView.getCameraSettings().setExposureEnabled(true);

        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callback);
    }


    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.flashlight:

                toggleTorch();
                break;

            case R.id.galleryPicker:

                pickImage();
                break;
        }
    }

    private void pickImage() {

        if (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Create intent for picking a photo from the gallery
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (intent.resolveActivity(getPackageManager()) != null) {

                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("scale", true);
                intent.putExtra("aspectX", 16);
                intent.putExtra("aspectY", 9);
                startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // pick image after request permission success
                    pickImage();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_IMAGE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            if (data != null) {
                Uri photoUri = data.getData();
                // Do something with the photo based on Uri
                Bitmap selectedBitmap = null;
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

                    AnimatedViewFinder viewFinder = barcodeView.getViewFinder();
                    Bitmap resizedBitmap = BitMapScaler.scaleToFill(selectedBitmap, viewFinder.getWidth(), viewFinder.getWidth());
                    barcodeView.scanBitmap(resizedBitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void toggleTorch() {
        if (isTorchEnabled) {
            barcodeView.setTorchOff();
        } else {
            barcodeView.setTorchOn();
        }
    }

    private void toggleTorchListener(boolean enableListener) {
        if (!ZxingUtils.hasFlash(getApplicationContext())) {
            barcodeView.getFlashlight().setVisibility(View.GONE);
        } else {
            if (true) {
                barcodeView.getFlashlight().setOnClickListener(this);
                barcodeView.setTorchListener(this);
            } else {
                barcodeView.getFlashlight().setOnClickListener(null);
                barcodeView.setTorchListener(null);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
        toggleTorchListener(true);
        barcodeView.getGalleryPicker().setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
        toggleTorchListener(false);
        barcodeView.getGalleryPicker().setOnClickListener(null);
    }

    @Override
    public void onTorchOn() {
        isTorchEnabled = true;
        barcodeView.getFlashlight().setImageResource(R.drawable.vector_flash_on);
    }

    @Override
    public void onTorchOff() {
        isTorchEnabled = false;
        barcodeView.getFlashlight().setImageResource(R.drawable.vector_flash_off);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

}
