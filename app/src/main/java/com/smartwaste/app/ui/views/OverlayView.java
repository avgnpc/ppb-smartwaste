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

import java.util.ArrayList;
import java.util.List;

/**
 * A simple overlay view that draws bounding boxes and labels over a camera preview.
 */
public class OverlayView extends View {

    private final Paint boxPaint;
    private final Paint textPaint;
    private List<Prediction> predictions = new ArrayList<>();

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
     * Update the list of predictions and redraw.
     */
    public void setPredictions(List<Prediction> newPredictions) {
        this.predictions = new ArrayList<>(newPredictions);
        postInvalidate(); // Triggers onDraw on UI thread
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (predictions == null || predictions.isEmpty()) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        for (Prediction p : predictions) {
            // Roboflow's API returns bounding boxes in pixel coordinates relative to the image we sent.
            // But if we sent full 640×480 or similar camera frames, we can directly draw using those coords.
            // (If using normalized coordinates [0..1], multiply by viewWidth/viewHeight.)

            // Compute rectangle corners from center x,y,w,h:
            float left = p.x - p.width / 2f;
            float top = p.y - p.height / 2f;
            float right = p.x + p.width / 2f;
            float bottom = p.y + p.height / 2f;

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRect(rect, boxPaint);

            // Draw label & confidence just above the box:
            String text = String.format("%s: %.1f%%", p.label, p.confidence * 100f);
            canvas.drawText(text, left, top - 10f, textPaint);
        }
    }
}
