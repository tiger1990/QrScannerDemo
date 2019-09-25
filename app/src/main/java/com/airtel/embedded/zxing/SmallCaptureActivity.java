package com.airtel.embedded.zxing;

import com.airtel.barcodescanner.CaptureActivity;
import com.airtel.barcodescanner.ZxingScannerView;
import com.airtel.qrscandemo.R;

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
