package com.qrscanner.barcodescanner;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.ColorInt;
import com.qrscanner.core.ZxingUtils;
import com.qrscanner.zing_embedded.R;
import com.google.zxing.ResultPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by B0204525 on 09,September,2019
 */

public class AnimatedViewFinder extends View {
    protected static final String TAG = AnimatedViewFinder.class.getSimpleName();

    private static final float PORTRAIT_WIDTH_RATIO = 6f/8;
    private static final float PORTRAIT_WIDTH_HEIGHT_RATIO = 0.75f;

    private static final float LANDSCAPE_HEIGHT_RATIO = 5f/8;
    private static final float LANDSCAPE_WIDTH_HEIGHT_RATIO = 1.4f;
    private static final int MIN_DIMENSION_DIFF = 50;

    private static final float DEFAULT_SQUARE_DIMENSION_RATIO = 5f / 8;

    protected static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    protected static final long ANIMATION_DELAY = 17L;
    protected static final int CURRENT_POINT_OPACITY = 0xA0;
    protected static final int MAX_RESULT_POINTS = 20;
    protected static final int POINT_SIZE = 10;
    protected Bitmap resultBitmap;
    protected int scannerAlpha;
    protected List<ResultPoint> possibleResultPoints;
    protected List<ResultPoint> lastPossibleResultPoints;
    protected CameraPreview cameraPreview;
    // Cache the framingRect and previewSize, so that we can still draw it after the preview
    // stopped.
    protected Rect framingRect;
    protected Size previewSize;
    protected Paint mLaserPaint;
    protected Paint mFinderMaskPaint;
    protected Paint mBorderPaint;
    protected int mBorderLineLength;
    @ColorInt
    private int mLaserColor = getResources().getColor(R.color.zxing_viewfinder_laser);
    @ColorInt
    private int mLaserResultPointColor = getResources().getColor(R.color.zxing_possible_result_points);
    @ColorInt
    private int mMaskColor = getResources().getColor(R.color.zxing_viewfinder_mask);
    @ColorInt
    private int mBorderColor = getResources().getColor(R.color.viewfinder_border);
    @ColorInt
    private int mResultViewColor = getResources().getColor(R.color.zxing_result_view);
    private int mBorderWidth = getResources().getInteger(R.integer.viewfinder_border_width);
    private int mBorderLength = getResources().getInteger(R.integer.viewfinder_border_length);
    private boolean mRoundedCorner = false;
    private boolean mIsLaserEnabled = false;
    private boolean mSquaredFinder = false;

    private int mViewFinderOffset = 10;

    private int mCornerRadius = 8;
    private float mBorderAlpha = 1.0f;

    private int frames = 6;
    private boolean revAnimation;
    private float endY;
    private IDimensionChangeListener dimensionChangeListener;


    public interface IDimensionChangeListener { void onDimensionChanged(Rect framingRect);}

    public AnimatedViewFinder(Context context) {
        super(context);
        init();
    }

    // This constructor is used when the class is built from an XML resource.
    public AnimatedViewFinder(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.view_finder,
                0, 0);

        try {

            this.mIsLaserEnabled = attributes.getBoolean(R.styleable.view_finder_laserEnabled, mIsLaserEnabled);
            this.mLaserColor = attributes.getColor(R.styleable.view_finder_laserColor, mLaserColor);
            this.mMaskColor = attributes.getColor(R.styleable.view_finder_maskColor, mMaskColor);
            this.mResultViewColor = attributes.getColor(R.styleable.view_finder_result_view_color, mResultViewColor);
            this.mSquaredFinder = attributes.getBoolean(R.styleable.view_finder_squaredFinder, mSquaredFinder);
            this.mLaserResultPointColor = attributes.getColor(R.styleable.view_finder_result_points_color, mLaserResultPointColor);

            this.mBorderWidth = attributes.getDimensionPixelSize(R.styleable.view_finder_borderWidth, mBorderWidth);
            this.mBorderLength = attributes.getDimensionPixelSize(R.styleable.view_finder_borderLength, mBorderLength);
            this.mBorderColor = attributes.getColor(R.styleable.view_finder_borderColor, mBorderColor);
            this.mRoundedCorner = attributes.getBoolean(R.styleable.view_finder_roundedCorner, mRoundedCorner);
            this.mCornerRadius = attributes.getDimensionPixelSize(R.styleable.view_finder_cornerRadius, mCornerRadius);
            this.mBorderAlpha = attributes.getFloat(R.styleable.view_finder_borderAlpha, mBorderAlpha);

            mViewFinderOffset = attributes.getDimensionPixelSize(R.styleable.view_finder_finderOffset, mViewFinderOffset);

        } finally {
            attributes.recycle();
        }
        init();
    }


    private void init() {
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(MAX_RESULT_POINTS);
        lastPossibleResultPoints = new ArrayList<>(MAX_RESULT_POINTS);

        //set up laser paint
        mLaserPaint = new Paint();
        mLaserPaint.setColor(mLaserColor);
        mLaserPaint.setStyle(Paint.Style.STROKE);
        mLaserPaint.setStrokeCap(Paint.Cap.ROUND);
        mLaserPaint.setStrokeJoin(Paint.Join.MITER);
        mLaserPaint.setAntiAlias(true);
        mLaserPaint.setStrokeWidth(2f);

        PathEffect mPathEffect = new DashPathEffect(new float[]{10f, 2f}, 1);
        mLaserPaint.setPathEffect(mPathEffect);

        //finder mask paint
        mFinderMaskPaint = new Paint();
        mFinderMaskPaint.setColor(mMaskColor);

        //border paint
        mBorderPaint = new Paint();
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setAntiAlias(true);

        mBorderLineLength = mBorderLength;

        setBorderColor(mBorderColor);
        setLaserColor(mLaserColor);
        setLaserEnabled(mIsLaserEnabled);
        setBorderStrokeWidth(mBorderWidth);
        setBorderLineLength(mBorderLength);
        setMaskColor(mMaskColor);
        setBorderCornerRounded(mRoundedCorner);
        setBorderCornerRadius(mCornerRadius);
        setSquareViewFinder(mSquaredFinder);
    }

    public void setCameraPreview(CameraPreview view) {
        this.cameraPreview = view;
        view.addStateListener(new CameraPreview.StateListener() {
            @Override
            public void previewSized() {
                refreshSizes();
                invalidate();
            }

            @Override
            public void previewStarted() { }

            @Override
            public void previewStopped() { }

            @Override
            public void cameraError(Exception error) { }

            @Override
            public void cameraClosed() { }
        });
    }

    protected void refreshSizes() {
        if (cameraPreview == null) { return; }

        Rect framingRect = cameraPreview.getFramingRect();
        Size previewSize = cameraPreview.getPreviewSize();
        if (framingRect != null && previewSize != null) {
            this.framingRect = framingRect;
            this.previewSize = previewSize;
        }
        updateFramingRect();
    }

    public Rect getFramingRect() {
        return framingRect;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null || previewSize == null) {
            return;
        }

        drawViewFinderMask(canvas);
        drawViewFinderBorder(canvas);

        if (mIsLaserEnabled) {
            //drawLaser(canvas);
            drawLaserFinder(canvas);
        }
    }


    public void drawViewFinderMask(Canvas canvas) {
        Rect framingRect = getFramingRect();

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        mFinderMaskPaint.setColor(resultBitmap != null ? mResultViewColor : mMaskColor);

        canvas.drawRect(0, 0, width, framingRect.top, mFinderMaskPaint);
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom + 1, mFinderMaskPaint);
        canvas.drawRect(framingRect.right + 1, framingRect.top, width, framingRect.bottom + 1, mFinderMaskPaint);
        canvas.drawRect(0, framingRect.bottom + 1, width, height, mFinderMaskPaint);
    }

    public void drawViewFinderBorder(Canvas canvas) {
        Rect framingRect = getFramingRect();

        // Top-left corner
        Path path = new Path();
        path.moveTo(framingRect.left, framingRect.top + mBorderLineLength);
        path.lineTo(framingRect.left, framingRect.top);
        path.lineTo(framingRect.left + mBorderLineLength, framingRect.top);
        canvas.drawPath(path, mBorderPaint);

        // Top-right corner
        path.moveTo(framingRect.right, framingRect.top + mBorderLineLength);
        path.lineTo(framingRect.right, framingRect.top);
        path.lineTo(framingRect.right - mBorderLineLength, framingRect.top);
        canvas.drawPath(path, mBorderPaint);

        // Bottom-right corner
        path.moveTo(framingRect.right, framingRect.bottom - mBorderLineLength);
        path.lineTo(framingRect.right, framingRect.bottom);
        path.lineTo(framingRect.right - mBorderLineLength, framingRect.bottom);
        canvas.drawPath(path, mBorderPaint);

        // Bottom-left corner
        path.moveTo(framingRect.left, framingRect.bottom - mBorderLineLength);
        path.lineTo(framingRect.left, framingRect.bottom);
        path.lineTo(framingRect.left + mBorderLineLength, framingRect.bottom);
        canvas.drawPath(path, mBorderPaint);
    }

    private void drawLaserFinder(Canvas canvas) {
        Rect framingRect = getFramingRect();

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            mLaserPaint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, framingRect, mLaserPaint);
        } else {

            float leftOffsetDotted = framingRect.left + 5;
            float rightOffsetDotted = framingRect.right - 5;
            int distanceFactor = 5;
            int numLines = 35;
            int totalLineDistance = numLines * distanceFactor;

            if (endY <= 0) {
                endY = framingRect.top + totalLineDistance;
            }

            float top = framingRect.top;
            float rectHeight = framingRect.height();

            // draw the line to product animation
            if ((endY >= top + rectHeight + frames)) {
                revAnimation = true;
            } else if (endY <= top + frames + totalLineDistance) {
                revAnimation = false;
            }
            // check if the line has reached to bottom
            if (revAnimation) {
                endY -= frames;
            } else {
                endY += frames;
            }

            drawPossiblePoints(canvas);
            drawLineDottedView(canvas, leftOffsetDotted, rightOffsetDotted, distanceFactor, numLines);

            postInvalidateDelayed(ANIMATION_DELAY,
                    framingRect.left - POINT_SIZE,
                    framingRect.top - POINT_SIZE,
                    framingRect.right + POINT_SIZE,
                    framingRect.bottom + POINT_SIZE);
        }
    }

    private void drawPossiblePoints(Canvas canvas) {

        // Draw a red "laser scanner" line through the middle to show decoding is active
        mLaserPaint.setColor(mLaserColor);
        mLaserPaint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        final int middle = framingRect.height() / 2 + framingRect.top;
        //canvas.drawRect(framingRect.left + 2, middle - 1, framingRect.right - 1, middle + 2, mLaserPaint);

        final float scaleX = this.getWidth() / (float) previewSize.width;
        final float scaleY = this.getHeight() / (float) previewSize.height;

        // draw the last possible result points
        if (!lastPossibleResultPoints.isEmpty()) {
            mLaserPaint.setAlpha(CURRENT_POINT_OPACITY / 2);
            mLaserPaint.setColor(mLaserResultPointColor);
            float radius = POINT_SIZE / 2.0f;
            for (final ResultPoint point : lastPossibleResultPoints) {
                canvas.drawCircle(
                        (int) (point.getX() * scaleX),
                        (int) (point.getY() * scaleY),
                        radius, mLaserPaint
                );
            }
            lastPossibleResultPoints.clear();
        }

        // draw current possible result points
        if (!possibleResultPoints.isEmpty()) {
            mLaserPaint.setAlpha(CURRENT_POINT_OPACITY);
            mLaserPaint.setColor(mLaserResultPointColor);
            for (final ResultPoint point : possibleResultPoints) {
                canvas.drawCircle(
                        (int) (point.getX() * scaleX),
                        (int) (point.getY() * scaleY),
                        POINT_SIZE, mLaserPaint
                );
            }

            // swap and clear buffers
            final List<ResultPoint> temp = possibleResultPoints;
            possibleResultPoints = lastPossibleResultPoints;
            lastPossibleResultPoints = temp;
            possibleResultPoints.clear();
        }
    }

    private void drawLineDottedView(Canvas canvas, float leftOffset, float rightOffset, int distanceFactor, int numLines) {
        for (int i = 1; i <= numLines; i++) {
            mLaserPaint.setAlpha(190 - i * distanceFactor);
            canvas.drawLine(leftOffset, endY - (distanceFactor * i), rightOffset, endY - (distanceFactor * i), mLaserPaint);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    public void drawResultBitmap(Bitmap result) {
        resultBitmap = result;
        invalidate();
    }

    /**
     * Only call from the UI thread.
     *
     * @param point a point to draw, relative to the preview frame
     */
    public void addPossibleResultPoint(ResultPoint point) {
        if (possibleResultPoints.size() < MAX_RESULT_POINTS)
            possibleResultPoints.add(point);
    }

    public void setLaserColor(int laserColor) { mLaserPaint.setColor(laserColor); }

    public void setMaskColor(int maskColor) { this.mMaskColor = maskColor; }

    public void setBorderColor(int borderColor) { mBorderPaint.setColor(borderColor); }

    public void setBorderStrokeWidth(int borderStrokeWidth) { mBorderPaint.setStrokeWidth(borderStrokeWidth); }

    public void setBorderLineLength(int borderLineLength) { mBorderLineLength = borderLineLength; }

    public void setLaserEnabled(boolean isLaserEnabled) { mIsLaserEnabled = isLaserEnabled; }

    public void setBorderCornerRounded(boolean isBorderCornersRounded) {
        if (isBorderCornersRounded) {
            mBorderPaint.setStrokeJoin(Paint.Join.ROUND);
        } else {
            mBorderPaint.setStrokeJoin(Paint.Join.BEVEL);
        }
    }

    public void setBorderAlpha(float alpha) {
        int colorAlpha = (int) (255 * alpha);
        mBorderAlpha = alpha;
        mBorderPaint.setAlpha(colorAlpha);
    }

    public void setBorderCornerRadius(int borderCornersRadius) {
        mBorderPaint.setPathEffect(new CornerPathEffect(borderCornersRadius));
    }

    public void setupViewFinder() {
        updateFramingRect();
        invalidate();
    }

    public void setSquareViewFinder(boolean isSquareViewFinder) {
        mSquaredFinder = isSquareViewFinder;
    }

    public void setViewFinderOffset(int offset) {
        mViewFinderOffset = offset;
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    public synchronized void updateFramingRect() {
        Point viewResolution = new Point(getWidth(), getHeight());
        int width;
        int height;
        int orientation = ZxingUtils.getScreenOrientation(getContext());

        if(mSquaredFinder) {
            if(orientation != Configuration.ORIENTATION_PORTRAIT) {
                height = (int) (getHeight() * DEFAULT_SQUARE_DIMENSION_RATIO);
                width = height;
            } else {
                width = (int) (getWidth() * DEFAULT_SQUARE_DIMENSION_RATIO);
                height = width;
            }
        } else {
            if(orientation != Configuration.ORIENTATION_PORTRAIT) {
                height = (int) (getHeight() * LANDSCAPE_HEIGHT_RATIO);
                width = (int) (LANDSCAPE_WIDTH_HEIGHT_RATIO * height);
            } else {
                width = (int) (getWidth() * PORTRAIT_WIDTH_RATIO);
                height = (int) (PORTRAIT_WIDTH_HEIGHT_RATIO * width);
            }
        }

        if(width > getWidth()) {
            width = getWidth() - MIN_DIMENSION_DIFF;
        }

        if(height > getHeight()) {
            height = getHeight() - MIN_DIMENSION_DIFF;
        }

        int leftOffset = (viewResolution.x - width) / 2;
        int topOffset =  (viewResolution.y - height) / 5;

        framingRect = new Rect(leftOffset + mViewFinderOffset, topOffset + mViewFinderOffset, leftOffset + width - mViewFinderOffset, topOffset + height - mViewFinderOffset);
        dimensionChangeListener.onDimensionChanged(framingRect);
    }

    public void attachDimensionChangeListener(IDimensionChangeListener dimensionChangeListener) {

        this.dimensionChangeListener = dimensionChangeListener;
    }
}