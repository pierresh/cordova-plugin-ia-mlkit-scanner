package com.intelliacc.MLKitBarcodeScanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.CornerPathEffect;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.intelliacc.MLKitBarcodeScanner.camera.MLKitGraphicOverlay;

public class MLKitBarcodeGraphic extends MLKitGraphicOverlay.Graphic {

    private static final int COLOR_CHOICES[] = { Color.rgb(0, 150, 136), Color.rgb(64, 196, 255), Color.rgb(103, 58, 183) };
    private static int CURRENT_COLOR_INDEX = 0;

    private int _Id;
    private Paint _RectPaint;
    private Paint _TextPaint;
    private Paint _TextBackgroundPaint;
    private volatile Barcode _Barcode;

    MLKitBarcodeGraphic(MLKitGraphicOverlay overlay) {
        super(overlay);

        CURRENT_COLOR_INDEX = (CURRENT_COLOR_INDEX + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[CURRENT_COLOR_INDEX];

        _RectPaint = new Paint();
        _RectPaint.setColor(selectedColor);
        _RectPaint.setStyle(Paint.Style.STROKE);
        _RectPaint.setStrokeWidth(4.0f);
        _RectPaint.setPathEffect(new CornerPathEffect(16));

        _TextPaint = new Paint();
        _TextPaint.setColor(Color.WHITE);
        _TextPaint.setTextSize(36.0f);

        _TextBackgroundPaint = new Paint();
        _TextBackgroundPaint.setColor(selectedColor);
        _TextBackgroundPaint.setStyle(Paint.Style.FILL);
        _TextBackgroundPaint.setPathEffect(new CornerPathEffect(8));
    }

    public int getId() {
        return _Id;
    }

    public void setId(int id) {
        this._Id = id;
    }

    public Barcode getBarcode() {
        return _Barcode;
    }

    public void updateItem(Barcode barcode) {
        _Barcode = barcode;
        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        Barcode barcode = _Barcode;
        if (barcode == null) {
            return;
        }

        RectF rect = new RectF(barcode.getBoundingBox());
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);

        // Draw rounded rectangle
        Path path = new Path();
        float radius = 16;
        path.moveTo(rect.left + radius, rect.top);
        path.lineTo(rect.right - radius, rect.top);
        path.quadTo(rect.right, rect.top, rect.right, rect.top + radius);
        path.lineTo(rect.right, rect.bottom - radius);
        path.quadTo(rect.right, rect.bottom, rect.right - radius, rect.bottom);
        path.lineTo(rect.left + radius, rect.bottom);
        path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - radius);
        path.lineTo(rect.left, rect.top + radius);
        path.quadTo(rect.left, rect.top, rect.left + radius, rect.top);
        path.close();

        canvas.drawPath(path, _RectPaint);

        // Draw text background
        String text = barcode.getRawValue();
        float textWidth = _TextPaint.measureText(text);
        float textHeight = _TextPaint.getTextSize();
        float padding = 16;

        RectF textRect = new RectF(
            rect.left,
            rect.bottom + 8,
            rect.left + textWidth + padding * 2,
            rect.bottom + textHeight + padding * 2
        );

        Path textPath = new Path();
        textPath.moveTo(textRect.left + 8, textRect.top);
        textPath.lineTo(textRect.right - 8, textRect.top);
        textPath.quadTo(textRect.right, textRect.top, textRect.right, textRect.top + 8);
        textPath.lineTo(textRect.right, textRect.bottom - 8);
        textPath.quadTo(textRect.right, textRect.bottom, textRect.right - 8, textRect.bottom);
        textPath.lineTo(textRect.left + 8, textRect.bottom);
        textPath.quadTo(textRect.left, textRect.bottom, textRect.left, textRect.bottom - 8);
        textPath.lineTo(textRect.left, textRect.top + 8);
        textPath.quadTo(textRect.left, textRect.top, textRect.left + 8, textRect.top);
        textPath.close();

        canvas.drawPath(textPath, _TextBackgroundPaint);

        // Draw text
        canvas.drawText(text, textRect.left + padding, textRect.bottom - padding, _TextPaint);
    }
}
