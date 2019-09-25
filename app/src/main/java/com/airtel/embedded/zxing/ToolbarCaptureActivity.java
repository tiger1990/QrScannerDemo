package com.airtel.embedded.zxing;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import com.airtel.barcodescanner.CaptureManager;
import com.airtel.barcodescanner.ZxingScannerView;
import com.airtel.qrscandemo.R;

/**
 * Sample Activity extending from ActionBarActivity to display a Toolbar.
 */
public class ToolbarCaptureActivity extends AppCompatActivity {
    private CaptureManager capture;
    private ZxingScannerView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.capture_appcompat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        toolbar.setTitle("Scan Barcode");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        barcodeScannerView = (ZxingScannerView)findViewById(R.id.zxing_barcode_scanner);

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
