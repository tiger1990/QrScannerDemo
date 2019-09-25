package com.airtel.embedded.zxing;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.airtel.barcodescanner.AnimatedViewFinder;
import com.airtel.barcodescanner.CaptureManager;
import com.airtel.barcodescanner.ZxingScannerView;
import com.airtel.core.ZxingUtils;
import com.airtel.qrscandemo.R;
import java.util.Random;

/**
 * Custom Scannner Activity extending from Activity to display a custom layout form scanner view.
 */
public class CustomScannerActivity extends Activity implements
        ZxingScannerView.TorchListener {

    private CaptureManager capture;
    private ZxingScannerView barcodeScannerView;
    private Button switchFlashlightButton;
    private AnimatedViewFinder viewfinderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_scanner);

        barcodeScannerView = (ZxingScannerView)findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setTorchListener(this);

        switchFlashlightButton = (Button)findViewById(R.id.switch_flashlight);

        viewfinderView = (AnimatedViewFinder) findViewById(R.id.zxing_viewfinder_view);

        // if the device does not have flashlight in its camera,
        // then remove the switch flashlight button...
        if (!ZxingUtils.hasFlash(getApplicationContext())) {
            switchFlashlightButton.setVisibility(View.GONE);
        }

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        changeMaskColor(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    public void switchFlashlight(View view) {
        if (getString(R.string.turn_on_flashlight).equals(switchFlashlightButton.getText())) {
            barcodeScannerView.setTorchOn();
        } else {
            barcodeScannerView.setTorchOff();
        }
    }

    public void changeMaskColor(View view) {
        Random rnd = new Random();
        int color = Color.argb(100, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        viewfinderView.setMaskColor(color);
    }

    @Override
    public void onTorchOn() {
        switchFlashlightButton.setText(R.string.turn_off_flashlight);
    }

    @Override
    public void onTorchOff() {
        switchFlashlightButton.setText(R.string.turn_on_flashlight);
    }
}
