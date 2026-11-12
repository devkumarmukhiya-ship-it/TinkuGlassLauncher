package com.tinku.glasslauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Home clock with many styles (font + layout). Style index 0..COUNT-1 (even = normal, odd = first part dim). */
public class ClockText extends View {
    private static final int L_INLINE = 0;      // 5:00
    private static final int L_INLINE_PAD = 1;  // 05:00
    private static final int L_SPLIT = 2;       // 05 big + 00 small
    private static final int L_STACK = 3;       // 05 over 00
    private static final int L_DAY = 4;         // Thu. big + small line
    private static final int L_NONE = 5;        // no clock
    private static final int L_TIGHT = 6;       // 0500 (no colon)
    private static final int L_GAP = 7;         // 05 00 (gap, no colon)
    private static final int L_BARS = 8;        // T H U cut in two, info line in the middle
    private static final int FILL = 0;
    private static final int STROKE = 1;

    private static final class S {
        final int layout;
        final String font;
        final int face;
        final float size;
        final int mode;
        final float spacing;

        S(int layout, String font, int face, float size, int mode, float spacing) {
            this.layout = layout;
            this.font = font;
            this.face = face;
            this.size = size;
            this.mode = mode;
            this.spacing = spacing;
        }
    }

    private static final S[] BASE = new S[] {
        new S(L_INLINE, "sans-serif-medium", Typeface.NORMAL, 44, FILL, 0f),
        new S(L_NONE, "sans-serif", Typeface.NORMAL, 44, FILL, 0f),
        new S(L_INLINE, "sans-serif-light", Typeface.NORMAL, 46, FILL, 0f),
        new S(L_INLINE, "sans-serif-thin", Typeface.NORMAL, 50, FILL, 0f),
        new S(L_INLINE, "sans-serif", Typeface.NORMAL, 44, FILL, 0f),
        new S(L_INLINE, "asset:FredokaOne-Regular.ttf", Typeface.NORMAL, 46, FILL, 0f),
        new S(L_INLINE, "asset:Lobster-Regular.ttf", Typeface.NORMAL, 46, FILL, 0f),
        new S(L_INLINE, "asset:ShadowsIntoLight.ttf", Typeface.NORMAL, 52, FILL, 0f),
        new S(L_INLINE, "sans-serif-light", Typeface.ITALIC, 46, FILL, 0f),
        new S(L_INLINE, "asset:BebasNeue-Regular.ttf", Typeface.NORMAL, 54, FILL, 0f),
        new S(L_INLINE, "asset:PressStart2P-Regular.ttf", Typeface.NORMAL, 30, FILL, 0.05f),
        new S(L_INLINE, "sans-serif-black", Typeface.NORMAL, 46, STROKE, 0f),
        new S(L_SPLIT, "sans-serif-black", Typeface.NORMAL, 56, FILL, 0f),
        new S(L_SPLIT, "sans-serif-medium", Typeface.NORMAL, 56, FILL, 0f),
        new S(L_SPLIT, "serif", Typeface.BOLD, 56, FILL, 0f),
        new S(L_INLINE_PAD, "asset:Chango-Regular.ttf", Typeface.NORMAL, 44, FILL, 0f),
        new S(L_INLINE_PAD, "sans-serif-condensed", Typeface.NORMAL, 54, FILL, 0f),
        new S(L_STACK, "sans-serif-light", Typeface.NORMAL, 54, FILL, 0f),
        new S(L_STACK, "asset:BebasNeue-Regular.ttf", Typeface.NORMAL, 58, FILL, 0f),
        new S(L_STACK, "sans-serif-black", Typeface.NORMAL, 52, FILL, 0f),
        new S(L_STACK, "sans-serif-thin", Typeface.NORMAL, 56, FILL, 0f),
        new S(L_STACK, "sans-serif-medium", Typeface.NORMAL, 54, STROKE, 0f),
        new S(L_DAY, "asset:Sacramento-Regular.ttf", Typeface.NORMAL, 66, FILL, 0f),
        new S(L_DAY, "asset:ArchivoBlack-Regular.ttf", Typeface.NORMAL, 54, FILL, 0f),
        new S(L_INLINE_PAD, "serif", Typeface.NORMAL, 48, FILL, 0f),
        new S(L_SPLIT, "sans-serif-light", Typeface.NORMAL, 58, STROKE, 0f),
        new S(L_DAY, "sans-serif-condensed", Typeface.BOLD, 60, FILL, 0f),
        new S(L_INLINE, "asset:Monoton-Regular.ttf", Typeface.NORMAL, 40, FILL, 0f),
        new S(L_DAY, "asset:Mansalva-Regular.ttf", Typeface.NORMAL, 62, FILL, 0f),
        new S(L_BARS, "sans-serif-light", Typeface.NORMAL, 62, FILL, 0.12f),
        new S(L_BARS, "sans-serif-thin", Typeface.NORMAL, 58, FILL, 0.22f),
        new S(L_GAP, "asset:BebasNeue-Regular.ttf", Typeface.NORMAL, 58, FILL, 0f),
        new S(L_GAP, "asset:BebasNeue-Regular.ttf", Typeface.NORMAL, 58, STROKE, 0f),
        new S(L_TIGHT, "asset:Chango-Regular.ttf", Typeface.NORMAL, 46, FILL, 0f),
        new S(L_TIGHT, "asset:AbrilFatface-Regular.ttf", Typeface.NORMAL, 52, FILL, 0f),
        new S(L_TIGHT, "sans-serif-light", Typeface.NORMAL, 54, STROKE, 0f),
        new S(L_INLINE_PAD, "asset:Chewy-Regular.ttf", Typeface.NORMAL, 50, FILL, 0f),
        new S(L_SPLIT, "asset:FredokaOne-Regular.ttf", Typeface.NORMAL, 56, FILL, 0f),
        new S(L_SPLIT, "asset:Lobster-Regular.ttf", Typeface.NORMAL, 56, FILL, 0f)
    };

    public static final int COUNT = BASE.length * 2;

    public static boolean isDay(int idx) {
        int l = BASE[Math.max(0, Math.min(COUNT - 1, idx)) / 2].layout;
        return l == L_DAY || l == L_BARS;
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSmall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int style = 36;
    private boolean sample = false;
    private boolean right = false;
    private boolean shadow = true;
    private int color = Color.WHITE;

    private String hStr = "";
    private String mStr = "";
    private String dayStr = "";
    private String smallStr = "";
    private String dayUp = "";
    private String infoUp = "";
    private float contentW = 0f;
    private float contentH = 0f;

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            requestLayout();
            invalidate();
        }
    };

    public ClockText(Context c) {
        super(c);
    }

    public void setStyleIndex(int idx) {
        style = Math.max(0, Math.min(COUNT - 1, idx));
        requestLayout();
        invalidate();
    }

    public void setSample(boolean s) {
        sample = s;
        requestLayout();
        invalidate();
    }

    public void setAlignRight(boolean r) {
        right = r;
        invalidate();
    }

    public void setTone(int c, boolean withShadow) {
        color = c;
        shadow = withShadow;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!sample) {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_TIME_TICK);
            f.addAction(Intent.ACTION_TIME_CHANGED);
            f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getContext().registerReceiver(tick, f);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!sample) {
            try {
                getContext().unregisterReceiver(tick);
            } catch (Exception e) {
                // not registered
            }
        }
        super.onDetachedFromWindow();
    }

    private static final java.util.HashMap<String, Typeface> FACES = new java.util.HashMap<String, Typeface>();

    /** Font from assets/fonts when the name starts with "asset:", else a system font. Falls back safely. */
    private Typeface faceFor(S s) {
        if (!sample) {
            Typeface o = FontKit.chosen(getContext());
            if (o != null) return o;
        }
        if (s.font.startsWith("asset:")) {
            String name = s.font.substring(6);
            Typeface t = FACES.get(name);
            if (t == null) {
                try {
                    t = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + name);
                } catch (Throwable e) {
                    t = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                }
                FACES.put(name, t);
            }
            return t;
        }
        return Typeface.create(s.font, s.face);
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private int battery() {
        Intent b = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return 100;
        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level < 0 || scale <= 0) return 100;
        return Math.round(level * 100f / scale);
    }

    private void prepare() {
        S s = BASE[style / 2];
        int h;
        int m;
        if (sample) {
            h = 5;
            m = 0;
            dayStr = "Thu.";
            smallStr = "5:00 \u00b7 30 Aug \u00b7 100%";
        } else {
            Calendar cal = Calendar.getInstance();
            boolean is24 = DateFormat.is24HourFormat(getContext());
            h = cal.get(is24 ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
            if (!is24 && h == 0) h = 12;
            m = cal.get(Calendar.MINUTE);
            Date now = cal.getTime();
            dayStr = new SimpleDateFormat("EEE", Locale.ENGLISH).format(now) + ".";
            smallStr = h + ":" + two(m) + " \u00b7 "
                + new SimpleDateFormat("d MMM", Locale.ENGLISH).format(now) + " \u00b7 " + battery() + "%";
        }
        boolean pad = s.layout != L_INLINE;
        hStr = pad ? two(h) : String.valueOf(h);
        mStr = two(m);
        dayUp = dayStr.replace(".", "").toUpperCase(Locale.ENGLISH);
        infoUp = smallStr.replace(" \u00b7 ", "   ").toUpperCase(Locale.ENGLISH);

        float f = sample ? 0.85f : 1f;
        p.setTypeface(faceFor(s));
        p.setTextSize(sp(s.size * f));
        p.setLetterSpacing(s.spacing);
        pSmall.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pSmall.setTextSize(sp(12));
        pSmall.setLetterSpacing(s.layout == L_BARS ? 0.18f : 0.04f);

        Paint.FontMetrics fm = p.getFontMetrics();
        float lineH = fm.descent - fm.ascent;
        switch (s.layout) {
            case L_INLINE:
            case L_INLINE_PAD:
                contentW = p.measureText(hStr + ":" + mStr);
                contentH = lineH;
                break;
            case L_SPLIT: {
                float big = p.getTextSize();
                p.setTextSize(big * 0.46f);
                float wm = p.measureText(mStr);
                p.setTextSize(big);
                contentW = p.measureText(hStr) + dp(6) + wm;
                contentH = lineH;
                break;
            }
            case L_TIGHT:
                contentW = p.measureText(hStr + mStr);
                contentH = lineH;
                break;
            case L_GAP:
                contentW = p.measureText(hStr) + dp(12) + p.measureText(mStr);
                contentH = lineH;
                break;
            case L_BARS:
                contentW = Math.max(p.measureText(dayUp), pSmall.measureText(infoUp));
                contentH = lineH;
                break;
            case L_STACK:
                contentW = Math.max(p.measureText(hStr), p.measureText(mStr));
                contentH = lineH * 1.84f;
                break;
            case L_DAY:
                contentW = Math.max(p.measureText(dayStr), pSmall.measureText(smallStr));
                contentH = lineH + dp(20);
                break;
            default:
                contentW = dp(2);
                contentH = dp(2);
                break;
        }
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        prepare();
        int w = resolveSize((int) Math.ceil(contentW) + (int) dp(8), wSpec);
        int h = (int) Math.ceil(contentH) + (int) dp(6);
        setMeasuredDimension(w, h);
    }

    private void setPaint(int part) {
        S s = BASE[style / 2];
        boolean dim = (style % 2 == 1) && part == 0;
        int c = dim ? ((color & 0x00FFFFFF) | 0x73000000) : color;
        p.setColor(c);
        if (s.mode == STROKE) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1.4f));
            p.clearShadowLayer();
        } else {
            p.setStyle(Paint.Style.FILL);
            if (shadow) {
                p.setShadowLayer(dp(3), 0f, dp(1), 0x66000000);
            } else {
                p.clearShadowLayer();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        S s = BASE[style / 2];
        if (s.layout == L_NONE) return;
        prepare();
        float ox = right ? (getWidth() - contentW - dp(4)) : dp(4);
        Paint.FontMetrics fm = p.getFontMetrics();
        float base = dp(3) - fm.ascent;
        switch (s.layout) {
            case L_INLINE:
            case L_INLINE_PAD: {
                setPaint(0);
                String first = hStr + ":";
                canvas.drawText(first, ox, base, p);
                float w1 = p.measureText(first);
                setPaint(1);
                canvas.drawText(mStr, ox + w1, base, p);
                break;
            }
            case L_SPLIT: {
                setPaint(0);
                canvas.drawText(hStr, ox, base, p);
                float w1 = p.measureText(hStr);
                float big = p.getTextSize();
                p.setTextSize(big * 0.46f);
                setPaint(1);
                canvas.drawText(mStr, ox + w1 + dp(6), base, p);
                p.setTextSize(big);
                break;
            }
            case L_STACK: {
                float lineH = fm.descent - fm.ascent;
                setPaint(0);
                canvas.drawText(hStr, ox, base, p);
                setPaint(1);
                canvas.drawText(mStr, ox, base + lineH * 0.92f, p);
                break;
            }
            case L_TIGHT: {
                setPaint(0);
                canvas.drawText(hStr, ox, base, p);
                float w1 = p.measureText(hStr);
                setPaint(1);
                canvas.drawText(mStr, ox + w1, base, p);
                break;
            }
            case L_GAP: {
                setPaint(0);
                canvas.drawText(hStr, ox, base, p);
                float w1 = p.measureText(hStr);
                setPaint(1);
                canvas.drawText(mStr, ox + w1 + dp(12), base, p);
                break;
            }
            case L_BARS: {
                float mid = base - p.getTextSize() * 0.36f;
                float half = dp(7);
                float lw = ox + contentW + dp(4);
                float dx = ox + (contentW - p.measureText(dayUp)) / 2f;
                setPaint(0);
                canvas.save();
                canvas.clipRect(0f, 0f, lw, mid - half);
                canvas.drawText(dayUp, dx, base, p);
                canvas.restore();
                setPaint(1);
                canvas.save();
                canvas.clipRect(0f, mid + half, lw, getHeight());
                canvas.drawText(dayUp, dx, base, p);
                canvas.restore();
                pSmall.setColor((color & 0x00FFFFFF) | 0xE6000000);
                if (shadow) {
                    pSmall.setShadowLayer(dp(3), 0f, dp(1), 0x66000000);
                } else {
                    pSmall.clearShadowLayer();
                }
                float ix = ox + (contentW - pSmall.measureText(infoUp)) / 2f;
                canvas.drawText(infoUp, ix, mid + pSmall.getTextSize() * 0.35f, pSmall);
                break;
            }
            case L_DAY: {
                setPaint(0);
                canvas.drawText(dayStr, ox, base, p);
                pSmall.setColor((color & 0x00FFFFFF) | 0xE6000000);
                if (shadow) {
                    pSmall.setShadowLayer(dp(3), 0f, dp(1), 0x66000000);
                } else {
                    pSmall.clearShadowLayer();
                }
                canvas.drawText(smallStr, ox, base + fm.descent + dp(16), pSmall);
                break;
            }
            default:
                break;
        }
    }
}
