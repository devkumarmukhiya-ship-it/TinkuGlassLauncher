package com.tinku.glasslauncher;

// STEP12
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Lock screen like the video: fixed hour on the left, minute wheel (inner ring, pill on it),
 *  second wheel (outer ring + ticks) running, date + day on the right, battery at the bottom. */
public class LockView extends View {
    private final Paint hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint battStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint battFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint battText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GestureDetector detector;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE", Locale.ENGLISH);
    private VelocityTracker vt;
    private float downY;
    private boolean unlocking = false;

    private int cachedDay = -1;
    private String dateStr = "";
    private String dayStr = "";
    private long lastBattMs = 0;
    private int battPct = -1;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (getVisibility() == View.VISIBLE) {
                invalidate();
                handler.postDelayed(this, 33);
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    };

    public LockView(Context c) {
        super(c);
        setClickable(true);

        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        hourPaint.setColor(Color.WHITE);
        hourPaint.setTypeface(medium);
        hourPaint.setTextAlign(Paint.Align.CENTER);

        numPaint.setColor(Color.WHITE);
        numPaint.setTypeface(medium);
        numPaint.setTextAlign(Paint.Align.CENTER);

        tickPaint.setColor(Color.WHITE);
        tickPaint.setStrokeWidth(dp(1.2f));
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        pillPaint.setColor(Color.WHITE);
        pillPaint.setStyle(Paint.Style.STROKE);
        pillPaint.setStrokeWidth(dp(1.6f));

        pillText.setColor(Color.WHITE);
        pillText.setTypeface(medium);
        pillText.setTextAlign(Paint.Align.CENTER);

        datePaint.setColor(Color.WHITE);
        datePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        datePaint.setTextSize(sp(10));
        datePaint.setLetterSpacing(0.2f);

        battStroke.setColor(Color.WHITE);
        battStroke.setStyle(Paint.Style.STROKE);
        battStroke.setStrokeWidth(dp(1.2f));

        battFill.setColor(Color.WHITE);
        battFill.setStyle(Paint.Style.FILL);

        battText.setColor(Color.WHITE);
        battText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        battText.setTextSize(sp(10));

        detector = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                unlock();
                return true;
            }
        });
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    /** Show the lock screen again (e.g. after screen off). */
    public void showLock() {
        animate().cancel();
        unlocking = false;
        setTranslationY(0f);
        setAlpha(1f);
        setVisibility(View.VISIBLE);
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        invalidate();
    }

    /** Slide up and reveal the home screen. */
    public void unlock() {
        if (unlocking || getVisibility() != View.VISIBLE) return;
        unlocking = true;
        animate().translationY(-getHeight()).alpha(0f).setDuration(240)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    setVisibility(View.GONE);
                    setTranslationY(0f);
                    setAlpha(1f);
                    unlocking = false;
                }
            }).start();
    }

    private static float wrap(float x) {
        return ((x + 30f) % 60f + 60f) % 60f - 30f;
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private static float centerBaseline(Paint p, float cy) {
        Paint.FontMetrics fm = p.getFontMetrics();
        return cy - (fm.ascent + fm.descent) / 2f;
    }

    /** 0 = pill ko chheu ma (nadekhine), 1 = pill bata tada (pura dekhine) */
    private static float bandFade(float deg, float band) {
        return Math.max(0f, Math.min(1f, (Math.abs(deg) - band) / 12f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        long nowMs = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        float sec = cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f;
        float minF = min + sec / 60f;

        int dayKey = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.DAY_OF_YEAR);
        if (dayKey != cachedDay) {
            cachedDay = dayKey;
            Date d = cal.getTime();
            dateStr = dateFmt.format(d).toUpperCase(Locale.ENGLISH);
            dayStr = dayFmt.format(d).toUpperCase(Locale.ENGLISH);
        }

        // sabai size screen ko chaudai anusar
        float R1 = w * 0.18f;   // minute wheel (pill yahi ring ma)
        float R2 = w * 0.25f;   // second wheel numbers
        float R3 = w * 0.29f;   // ticks
        float pillW = w * 0.19f;
        float pillH = w * 0.092f;
        float cy = h * 0.44f;
        hourPaint.setTextSize(w * 0.125f);
        pillText.setTextSize(w * 0.056f);
        numPaint.setTextSize(w * 0.03f);

        float hourW = hourPaint.measureText("00");
        float tw = Math.max(datePaint.measureText(dateStr), datePaint.measureText(dayStr));

        // pura group (hour + wheels + date) screen ko bich ma
        float leftExt = Math.max(hourW / 2f, R3 * 0.5f);
        float rightExt = R3 + w * 0.014f + w * 0.04f + tw;
        float cx = (w - (leftExt + rightExt)) / 2f + leftExt;
        float pillX = cx + R1;

        // ticks (60 ota, har second ko ek) - second wheel
        for (int i = 0; i < 60; i++) {
            float deg = wrap(i - sec) * 6f;
            if (Math.abs(deg) > 125f) continue;
            double a = Math.toRadians(deg);
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            boolean major = (i % 5 == 0);
            float len = w * (major ? 0.014f : 0.007f);
            float fade = Math.max(0.1f, 1f - Math.abs(deg) / 140f);
            tickPaint.setAlpha((int) (255 * fade * (major ? 1f : 0.7f)));
            canvas.drawLine(cx + R3 * cos, cy - R3 * sin,
                cx + (R3 + len) * cos, cy - (R3 + len) * sin, tickPaint);
        }

        // second numbers (bahira ring) - har second ghumchha
        for (int v = 0; v < 60; v += 5) {
            float deg = wrap(v - sec) * 6f;
            if (Math.abs(deg) > 110f) continue;
            float alpha = Math.max(0.15f, 1f - Math.abs(deg) / 125f) * bandFade(deg, 15f);
            if (alpha <= 0f) continue;
            double a = Math.toRadians(deg);
            numPaint.setAlpha((int) (255 * alpha));
            canvas.drawText(two(v), cx + R2 * (float) Math.cos(a),
                centerBaseline(numPaint, cy - R2 * (float) Math.sin(a)), numPaint);
        }

        // minute numbers (bhitri ring) - minute anusar ghumchha
        for (int v = 0; v < 60; v += 5) {
            float deg = wrap(v - minF) * 6f;
            if (Math.abs(deg) > 110f) continue;
            float alpha = Math.max(0.15f, 1f - Math.abs(deg) / 125f) * bandFade(deg, 20f);
            if (alpha <= 0f) continue;
            double a = Math.toRadians(deg);
            numPaint.setAlpha((int) (255 * alpha));
            canvas.drawText(two(v), cx + R1 * (float) Math.cos(a),
                centerBaseline(numPaint, cy - R1 * (float) Math.sin(a)), numPaint);
        }

        // minute pill
        rect.set(pillX - pillW / 2f, cy - pillH / 2f, pillX + pillW / 2f, cy + pillH / 2f);
        canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, pillPaint);
        canvas.drawText(two(min), pillX, centerBaseline(pillText, cy), pillText);

        // ghanta (left ma fixed)
        canvas.drawText(two(hour), cx, centerBaseline(hourPaint, cy), hourPaint);

        // date + day
        float dateX = cx + R3 + w * 0.014f + w * 0.04f;
        dateX = Math.min(dateX, w - tw - dp(14));
        canvas.drawText(dateStr, dateX, cy + dp(6), datePaint);
        canvas.drawText(dayStr, dateX, cy + dp(20), datePaint);

        // battery
        if (nowMs - lastBattMs > 20000 || battPct < 0) {
            lastBattMs = nowMs;
            Intent b = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b != null) {
                int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                if (level >= 0 && scale > 0) battPct = Math.round(level * 100f / scale);
            }
        }
        if (battPct >= 0) {
            int pct = battPct;
            String t = pct + "%";
            float bw = dp(8);
            float bh = dp(14);
            float total = bw + dp(8) + battText.measureText(t);
            float bx = (w - total) / 2f;
            float by = h * 0.86f;
            rect.set(bx, by - bh / 2f, bx + bw, by + bh / 2f);
            canvas.drawRoundRect(rect, dp(2), dp(2), battStroke);
            rect.set(bx + bw * 0.3f, by - bh / 2f - dp(1.5f), bx + bw * 0.7f, by - bh / 2f);
            canvas.drawRect(rect, battFill);
            float inner = bh - dp(3);
            float bottom = by + bh / 2f - dp(1.5f);
            rect.set(bx + dp(1.5f), bottom - inner * pct / 100f, bx + bw - dp(1.5f), bottom);
            canvas.drawRect(rect, battFill);
            canvas.drawText(t, bx + bw + dp(8), centerBaseline(battText, by), battText);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        detector.onTouchEvent(e);
        if (unlocking) return true;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downY = e.getRawY();
                if (vt != null) vt.recycle();
                vt = VelocityTracker.obtain();
                vt.addMovement(e);
                animate().cancel();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (vt != null) vt.addMovement(e);
                float dy = e.getRawY() - downY;
                if (dy < 0) {
                    setTranslationY(dy);
                    setAlpha(Math.max(0.2f, 1f - (-dy) / (getHeight() * 0.7f)));
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                float dy = e.getRawY() - downY;
                float vy = 0f;
                if (vt != null) {
                    vt.addMovement(e);
                    vt.computeCurrentVelocity(1000);
                    vy = vt.getYVelocity();
                    vt.recycle();
                    vt = null;
                }
                boolean up = e.getActionMasked() == MotionEvent.ACTION_UP;
                if (up && (dy < -getHeight() * 0.15f || vy < -1400f)) {
                    unlock();
                } else {
                    animate().translationY(0f).alpha(1f).setDuration(200).start();
                }
                return true;
            }
        }
        return true;
    }
}
