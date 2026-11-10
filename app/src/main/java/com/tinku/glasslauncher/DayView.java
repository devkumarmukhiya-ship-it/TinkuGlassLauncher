package com.tinku.glasslauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;

/** Big weekday name written vertically (bottom to top), like THURSDAY in the video. */
public class DayView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text = "";

    public DayView(Context c) {
        super(c);
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        p.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 46,
            getResources().getDisplayMetrics()));
    }

    public void setDay(String s) {
        text = s;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        Paint.FontMetrics fm = p.getFontMetrics();
        int th = (int) Math.ceil(fm.descent - fm.ascent);
        int tw = (int) Math.ceil(p.measureText(text));
        setMeasuredDimension(th + 8, tw + 8);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint.FontMetrics fm = p.getFontMetrics();
        canvas.save();
        canvas.translate(0, getHeight());
        canvas.rotate(-90);
        canvas.drawText(text, 4, 4 - fm.ascent, p);
        canvas.restore();
    }
}
