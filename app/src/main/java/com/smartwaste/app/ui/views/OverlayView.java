package com.smartwaste.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.smartwaste.app.model.Prediction;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OverlayView draws detection boxes on top of a CameraX PreviewView.
 * It knows:
 *  - imageWidth, imageHeight  (the resolution we sent to Roboflow, e.g. 1088×1088)
 *  - viewWidth, viewHeight    (the on-screen size of the PreviewView)
 * It then computes a uniform scale so the image fits into the view, plus offsets to center.
 */
public class OverlayView extends View {

    private final Paint boxPaint;
    private final Paint textPaint;
    private List<Prediction> predictions = new ArrayList<>();

    // The resolution (in pixels) of the JPEG frames we send to Roboflow:
    private float imageWidth  = 1f;
    private float imageHeight = 1f;

    // The actual on-screen size (in pixels) of the PreviewView (and thus this overlay):
    private float viewWidth  = 1f;
    private float viewHeight = 1f;

    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Call this once (or whenever your frame resolution changes) to tell the overlay
     * the size (width, height) of the image you send to Roboflow.
     * E.g. setImageSize(1088, 1088).
     */
    public void setImageSize(int w, int h) {
        this.imageWidth = (float) w;
        this.imageHeight = (float) h;
        invalidate();
    }

    /**
     * Call this after your PreviewView has been laid out, to tell the overlay
     * its on-screen size (the same as the PreviewView's width/height).
     */
    public void setViewSize(int w, int h) {
        this.viewWidth = (float) w;
        this.viewHeight = (float) h;
        invalidate();
    }

    /**
     * Provide a new list of predictions (x, y, width, height in image-space, plus label/confidence).
     * Predictions.x/y/width/height must be in the same coordinate system as imageWidth/imageHeight.
     */
    public void setPredictions(List<Prediction> newPredictions) {
        this.predictions = new ArrayList<>(newPredictions);
        postInvalidate();  // redraw on UI thread
    }

    public Map<String, Integer> getDetectionSummary() {
        Map<String, Integer> counts = new HashMap<>();
        for (Prediction p : predictions) {
            String label = p.label;
            counts.put(label, counts.getOrDefault(label, 0) + 1);
        }
        return counts;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (predictions == null || predictions.isEmpty()) return;

        // 1) Compute uniform scale so that image (imageWidth×imageHeight) fits into view (viewWidth×viewHeight)
        float scaleX = viewWidth  / imageWidth;
        float scaleY = viewHeight / imageHeight;
        float scale  = Math.min(scaleX, scaleY);

        // 2) Compute the scaled image size
        float scaledImageWidth  = imageWidth  * scale;
        float scaledImageHeight = imageHeight * scale;

        // 3) Compute offsets to center the scaled image in the view
        float offsetX = (viewWidth  - scaledImageWidth)  / 2f;
        float offsetY = (viewHeight - scaledImageHeight) / 2f;

        // 4) Draw each prediction
        for (Prediction p : predictions) {
            // a) Convert center-based (x, y, w, h) in image space → (left, top, right, bottom)
            float left   = p.x - (p.width  / 2f);
            float top    = p.y - (p.height / 2f);
            float right  = p.x + (p.width  / 2f);
            float bottom = p.y + (p.height / 2f);

            // b) Scale those coordinates into view space and add offset to center
            float leftV   = left   * scale + offsetX;
            float topV    = top    * scale + offsetY;
            float rightV  = right  * scale + offsetX;
            float bottomV = bottom * scale + offsetY;

            // c) Draw the bounding box
            RectF rect = new RectF(leftV, topV, rightV, bottomV);
            canvas.drawRect(rect, boxPaint);

            // d) Draw the label above the box
            String text = String.format("%s: %.1f%%", p.label, p.confidence * 100f);
            canvas.drawText(text, leftV, topV - 10f, textPaint);
        }
    }
}
