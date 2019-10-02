package com.qrscanner.barcodescanner;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageView;
import com.qrscanner.barcodescanner.camera.CameraParametersCallback;
import com.qrscanner.barcodescanner.camera.CameraSettings;
import com.qrscanner.core.ZxingUtils;
import com.qrscanner.zing_embedded.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.DecodeFormatManager;
import com.google.zxing.client.android.DecodeHintManager;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.HybridBinarizer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates BarcodeView, ViewfinderView and status text.
 * <p>
 * To customize the UI, use BarcodeView and ViewfinderView directly.
 */
public class ZxingScannerView extends FrameLayout implements AnimatedViewFinder.IDimensionChangeListener {
    private String TAG = ZxingScannerView.class.getSimpleName();
    private BarcodeView barcodeView;
    private AnimatedViewFinder viewFinder;
    private TextView statusView;
    private TorchListener torchListener;//The instance of @link TorchListener to send events callback.
    private AppCompatImageView flashlight, galleryPicker;
    private LinearLayout galleryParent;
    private WrappedCallback wrappedCallback;

    public ZxingScannerView(Context context) {
        super(context);
        initialize();
    }

    public ZxingScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    public ZxingScannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs);
    }

    public void scanBitmap(Bitmap resizedBitmap) {

        int[] intArray = new int[resizedBitmap.getWidth() * resizedBitmap.getHeight()];
        resizedBitmap.getPixels(intArray, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(resizedBitmap.getWidth(), resizedBitmap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();
        Result rawResult = null;
        Handler resultHandler = barcodeView.getBarcodeResultHandler();
        try {
            rawResult = reader.decode(bitmap);
            if (rawResult != null) {
                // Don't log the barcode contents for security.
                long end = System.currentTimeMillis();
                if (resultHandler != null) {
                    BarcodeResult barcodeResult = new BarcodeResult(rawResult, null);
                    Message message = Message.obtain(resultHandler, R.id.zxing_decode_succeeded, barcodeResult);
                    Bundle bundle = new Bundle();
                    message.setData(bundle);
                    message.sendToTarget();
                }
            } else {
                if (resultHandler != null) {
                    Message message = Message.obtain(resultHandler, R.id.zxing_decode_failed);
                    message.sendToTarget();
                }
            }

        } catch (NotFoundException | ChecksumException | FormatException ex) {
            if (resultHandler != null) {
                Message message = Message.obtain(resultHandler, R.id.zxing_decode_failed);
                message.sendToTarget();
            }
        }
    }

    /**
     * Initialize the view with the xml configuration based on styleable attributes.
     *
     * @param attrs The attributes to use on view.
     */
    private void initialize(AttributeSet attrs) {
        // Get attributes set on view
        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.zxing_view);

        int scannerLayout = attributes.getResourceId(R.styleable.zxing_view_zxing_scanner_layout, R.layout.zxing_scanner_view);

        attributes.recycle();

        inflate(getContext(), scannerLayout, this);

        barcodeView = (BarcodeView) findViewById(R.id.barcodeSurface);

        if (barcodeView == null) {
            throw new IllegalArgumentException(
                    "There is no a com.qrscanner.barcodescanner.BarcodeView on provided layout " +
                            "with the id \"zxing_barcode_surface\".");
        }

        // Pass on any preview-related attributes
        barcodeView.initializeAttributes(attrs);


        viewFinder = (AnimatedViewFinder) findViewById(R.id.animatedFinderView);

        if (viewFinder == null) {
            throw new IllegalArgumentException(
                    "There is no a com.qrscanner.barcodescanner.ViewfinderView on provided layout " +
                            "with the id \"zxing_viewfinder_view\".");
        }

        viewFinder.setCameraPreview(barcodeView);
        viewFinder.attachDimensionChangeListener(this);
        // statusView is optional
        statusView = (TextView) findViewById(R.id.statusView);
        flashlight = (AppCompatImageView) findViewById(R.id.flashlight);
        galleryPicker = (AppCompatImageView) findViewById(R.id.galleryPicker);
        galleryParent = (LinearLayout) findViewById(R.id.flashGalleryParent);
    }

    @Override
    public void onDimensionChanged(Rect framingRect) {

        int left = framingRect.left;
        int bottom = framingRect.bottom;
        int top = framingRect.top;
        int right = framingRect.right;

        FrameLayout.LayoutParams layoutParamsStatus = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        layoutParamsStatus.setMargins(0, top - 150, 0, 0);
        statusView.setLayoutParams(layoutParamsStatus);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, bottom + 80, 0, 0);
        galleryParent.setLayoutParams(layoutParams);
    }

    /**
     * Initialize with no custom attributes setted.
     */
    private void initialize() {
        initialize(null);
    }

    /**
     * Convenience method to initialize camera id, decode formats and prompt message from an intent.
     *
     * @param intent the intent, as generated by IntentIntegrator
     */
    public void initializeFromIntent(Intent intent) {
        // Scan the formats the intent requested, and return the result to the calling activity.
        Set<BarcodeFormat> decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        Map<DecodeHintType, Object> decodeHints = DecodeHintManager.parseDecodeHints(intent);

        CameraSettings settings = new CameraSettings();

        if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
            int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
            if (cameraId >= 0) {
                settings.setRequestedCameraId(cameraId);
            }
        }

        if (intent.hasExtra(Intents.Scan.TORCH_ENABLED)) {
            if (intent.getBooleanExtra(Intents.Scan.TORCH_ENABLED, false)) {
                this.setTorchOn();
            }
        }

        String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
        if (customPromptMessage != null) {
            setStatusText(customPromptMessage);
        }

        // Check what type of scan. Default: normal scan
        int scanType = intent.getIntExtra(Intents.Scan.SCAN_TYPE, 0);

        String characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

        barcodeView.setCameraSettings(settings);
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(decodeFormats, decodeHints, characterSet, scanType));
    }

    public DecoderFactory getDecoderFactory() {
        return barcodeView.getDecoderFactory();
    }

    public void setDecoderFactory(DecoderFactory decoderFactory) {
        barcodeView.setDecoderFactory(decoderFactory);
    }

    public CameraSettings getCameraSettings() {
        return barcodeView.getCameraSettings();
    }

    public void setCameraSettings(CameraSettings cameraSettings) {
        barcodeView.setCameraSettings(cameraSettings);
    }

    public void setStatusText(String text) {
        // statusView is optional when using a custom layout
        if (statusView != null) {
            statusView.setText(text);
        }
    }

    /**
     * @see BarcodeView#pause()
     */
    public void pause() {
        barcodeView.pause();
    }

    /**
     * @see BarcodeView#pauseAndWait()
     */
    public void pauseAndWait() {
        barcodeView.pauseAndWait();
    }

    /**
     * @see BarcodeView#resume()
     */
    public void resume() {
        barcodeView.resume();
    }

    public BarcodeView getBarcodeView() {
        return barcodeView;
    }

    public AnimatedViewFinder getViewFinder() {
        return viewFinder;
    }

    public TextView getStatusView() {
        return statusView;
    }

    public AppCompatImageView getFlashlight() {
        return flashlight;
    }

    public AppCompatImageView getGalleryPicker() {
        return galleryPicker;
    }

    /**
     * @see BarcodeView#decodeSingle(BarcodeCallback)
     */
    public void decodeSingle(BarcodeCallback callback) {
        wrappedCallback = new WrappedCallback(callback);
        barcodeView.decodeSingle(wrappedCallback);
    }

    /**
     * @see BarcodeView#decodeContinuous(BarcodeCallback)
     */
    public void decodeContinuous(BarcodeCallback callback) {
        wrappedCallback = new WrappedCallback(callback);
        barcodeView.decodeContinuous(wrappedCallback);
    }

    /**
     * Turn on the device's flashlight.
     */
    public void setTorchOn() {
        barcodeView.setTorch(true);

        if (torchListener != null) {
            torchListener.onTorchOn();
        }
    }

    /**
     * Turn off the device's flashlight.
     */
    public void setTorchOff() {
        barcodeView.setTorch(false);

        if (torchListener != null) {
            torchListener.onTorchOff();
        }
    }

    public void setFullScreenScan() {
        Point size = ZxingUtils.getScreenResolution(getContext());
        barcodeView.setFramingRectSize(new Size(size.x, size.y));
    }

    /**
     * Changes the settings for Camera.
     * Must be called after {@link #resume()}.
     *
     * @param callback {@link CameraParametersCallback}
     */
    public void changeCameraParameters(CameraParametersCallback callback) {
        barcodeView.changeCameraParameters(callback);
    }

    /**
     * Handles focus, camera
     * <p>
     * Note that this view is not usually focused, so the Activity should call this directly.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setTorchListener(TorchListener listener) {
        this.torchListener = listener;
    }

    /**
     * The Listener to torch/fflashlight events (turn on, turn off).
     */
    public interface TorchListener {

        void onTorchOn();

        void onTorchOff();
    }

    private class WrappedCallback implements BarcodeCallback {
        private BarcodeCallback delegate;

        public WrappedCallback(BarcodeCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void barcodeResult(BarcodeResult result) {
            delegate.barcodeResult(result);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            for (ResultPoint point : resultPoints) {
                viewFinder.addPossibleResultPoint(point);
            }
            delegate.possibleResultPoints(resultPoints);
        }
    }
}

