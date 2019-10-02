package com.qrscanner.embedded.zxing;

import com.qrscanner.barcodescanner.CaptureActivity;
import com.qrscanner.barcodescanner.ZxingScannerView;
import com.qrscanner.qrscandemo.R;

/**
 * This activity has a margin.
 */
public class SmallCaptureActivity extends CaptureActivity {
    @Override
    protected ZxingScannerView initializeContent() {
        setContentView(R.layout.capture_small);
        return (ZxingScannerView)findViewById(R.id.zxing_barcode_scanner);
    }
}
