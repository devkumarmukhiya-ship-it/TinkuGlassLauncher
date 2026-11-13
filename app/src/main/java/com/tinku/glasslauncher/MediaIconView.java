package com.tinku.glasslauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Music card ko button: shuffle, prev, play/pause (gol), next, repeat.
 * Sabai icon code le nai banaieko (24x24 grid), tyasaile kunai drawable file chahidaina.
 */
public class MediaIconView extends View {

    public static final int SHUFFLE = 0;
    public static final int PREV = 1;
    public static final int PLAY = 2;
    public static final int PAUSE = 3;
    public static final int NEXT = 4;
    public static final int REPEAT = 5;
    public static final int REPEAT_ONE = 6;

    private static final int LIGHT = 0xFFE8E9EE;
    private static final int DARK = 0xFF121212;

    private int type;
    private boolean active = true;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public MediaIconView(Context c, int type) {
        super(c);
        this.type = type;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.MITER);
        stroke.setStrokeCap(Paint.Cap.BUTT);
        fill.setStyle(Paint.Style.FILL);
        fill.setTextAlign(Paint.Align.CENTER);
    }

    public void setType(int t) {
        if (t == type) return;
        type = t;
        invalidate();
    }

    public int getType() {
        return type;
    }

    /** shuffle / repeat: on bhayo bhane ujjyalo + tala tiplo, off bhayo bhane maddham */
    public void setActive(boolean a) {
        if (a == active) return;
        active = a;
        invalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    private boolean isToggle() {
        return type == SHUFFLE || type == REPEAT || type == REPEAT_ONE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float s = Math.min(w, h);
        int press = isPressed() ? 0xB0 : 0xFF;

        if (type == PLAY || type == PAUSE) {
            float d = s * 0.96f;
            float cx = w / 2f;
            float cy = h / 2f;
            fill.setColor(LIGHT);
            fill.setAlpha(press);
            canvas.drawCircle(cx, cy, d / 2f, fill);
            fill.setColor(DARK);
            fill.setAlpha(press);
            if (type == PAUSE) {
                float bw = d * 0.115f;
                float gap = d * 0.10f;
                float bh = d * 0.40f;
                canvas.drawRect(cx - gap / 2f - bw, cy - bh / 2f, cx - gap / 2f, cy + bh / 2f, fill);
                canvas.drawRect(cx + gap / 2f, cy - bh / 2f, cx + gap / 2f + bw, cy + bh / 2f, fill);
            } else {
                path.reset();
                path.moveTo(cx - d * 0.11f, cy - d * 0.21f);
                path.lineTo(cx - d * 0.11f, cy + d * 0.21f);
                path.lineTo(cx + d * 0.21f, cy);
                path.close();
                canvas.drawPath(path, fill);
            }
            return;
        }

        int col = LIGHT;
        int alpha = press;
        if (isToggle() && !active) alpha = (int) (alpha * 0.45f);
        stroke.setColor(col);
        stroke.setAlpha(alpha);
        fill.setColor(col);
        fill.setAlpha(alpha);

        float u = s * 0.62f / 24f;
        canvas.save();
        canvas.translate(w / 2f - 12f * u, h / 2f - 12f * u);
        canvas.scale(u, u);
        stroke.setStrokeWidth(2.1f);

        switch (type) {
            case SHUFFLE:
                path.reset();
                path.moveTo(3f, 21f);
                path.lineTo(21f, 3f);
                path.moveTo(15f, 3f);
                path.lineTo(21f, 3f);
                path.lineTo(21f, 9f);
                path.moveTo(3f, 3f);
                path.lineTo(10f, 10f);
                path.moveTo(14f, 14f);
                path.lineTo(21f, 21f);
                path.moveTo(15f, 21f);
                path.lineTo(21f, 21f);
                path.lineTo(21f, 15f);
                canvas.drawPath(path, stroke);
                break;
            case PREV:
                canvas.drawRect(4.5f, 5f, 6.5f, 19f, fill);
                path.reset();
                path.moveTo(19f, 5.8f);
                path.lineTo(19f, 18.2f);
                path.lineTo(8.8f, 12f);
                path.close();
                canvas.drawPath(path, stroke);
                break;
            case NEXT:
                canvas.drawRect(17.5f, 5f, 19.5f, 19f, fill);
                path.reset();
                path.moveTo(5f, 5.8f);
                path.lineTo(5f, 18.2f);
                path.lineTo(15.2f, 12f);
                path.close();
                canvas.drawPath(path, stroke);
                break;
            case REPEAT:
            case REPEAT_ONE:
                path.reset();
                path.moveTo(4.5f, 11.5f);
                path.lineTo(4.5f, 7.5f);
                path.lineTo(20f, 7.5f);
                path.moveTo(17f, 4.5f);
                path.lineTo(20f, 7.5f);
                path.lineTo(17f, 10.5f);
                path.moveTo(19.5f, 12.5f);
                path.lineTo(19.5f, 16.5f);
                path.lineTo(4f, 16.5f);
                path.moveTo(7f, 13.5f);
                path.lineTo(4f, 16.5f);
                path.lineTo(7f, 19.5f);
                canvas.drawPath(path, stroke);
                if (type == REPEAT_ONE) {
                    fill.setTextSize(8.5f);
                    fill.setFakeBoldText(true);
                    canvas.drawText("1", 12f, 15f, fill);
                    fill.setFakeBoldText(false);
                }
                break;
            default:
                break;
        }
        // shuffle / repeat on chha bhanera dekhaune tiplo
        if (isToggle() && active) {
            canvas.drawCircle(12f, 23f, 1.2f, fill);
        }
        canvas.restore();
    }
}
