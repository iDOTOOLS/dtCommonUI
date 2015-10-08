
package com.edmodo.cropper.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
public class PaintUtil {

    // Private Constants ///////////////////////////////////////////////////////

    private static final int DEFAULT_CORNER_COLOR = Color.WHITE;
    private static final String SEMI_TRANSPARENT = "#AAFFFFFF";
    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#B0000000";
    private static final float DEFAULT_LINE_THICKNESS_DP = 3;
    private static final float DEFAULT_CORNER_THICKNESS_DP = 5;
    private static final float DEFAULT_GUIDELINE_THICKNESS_PX = 1;

    // Public Methods //////////////////////////////////////////////////////////

    public static Paint newBorderPaint(Context context) {

        // Set the line thickness for the crop window border.
        final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                                DEFAULT_LINE_THICKNESS_DP,
                                                                context.getResources().getDisplayMetrics());

        final Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor(SEMI_TRANSPARENT));
        borderPaint.setStrokeWidth(lineThicknessPx);
        borderPaint.setStyle(Paint.Style.STROKE);

        return borderPaint;
    }

    public static Paint newGuidelinePaint() {

        final Paint paint = new Paint();
        paint.setColor(Color.parseColor(SEMI_TRANSPARENT));
        paint.setStrokeWidth(DEFAULT_GUIDELINE_THICKNESS_PX);

        return paint;
    }

    public static Paint newBackgroundPaint(Context context) {

        final Paint paint = new Paint();
        paint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));

        return paint;
    }

    public static Paint newCornerPaint(Context context) {

        // Set the line thickness for the crop window border.
        final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                                DEFAULT_CORNER_THICKNESS_DP,
                                                                context.getResources().getDisplayMetrics());

        final Paint cornerPaint = new Paint();
        cornerPaint.setColor(DEFAULT_CORNER_COLOR);
        cornerPaint.setStrokeWidth(lineThicknessPx);
        cornerPaint.setStyle(Paint.Style.STROKE);

        return cornerPaint;
    }

    /**
     * Returns the value of the corner thickness
     * 
     * @return Float equivalent to the corner thickness
     */
    public static float getCornerThickness() {
        return DEFAULT_CORNER_THICKNESS_DP;
    }

    /**
     * Returns the value of the line thickness of the border
     * 
     * @return Float equivalent to the line thickness
     */
    public static float getLineThickness() {
        return DEFAULT_LINE_THICKNESS_DP;
    }

}
