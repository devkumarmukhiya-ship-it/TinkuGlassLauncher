package com.tinku.glasslauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Dial screen: number mathi, tala 3x4 keypad (outline gol), backspace ra hariyo CALL button.
 * Call garna phone ko dialer kholinchha (kunai permission chahidaina).
 */
public class DialView extends LinearLayout {

    public interface Listener {
        void onCall(String number);
        void onClose();
    }

    private static final String[] DIGITS = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#" };
    private static final String[] LETTERS = { "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", "" };

    private final StringBuilder number = new StringBuilder();
    private final TextView display;
    private Listener listener;

    public DialView(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(0xF2000000);
        setClickable(true);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(dp(24), dp(48), dp(24), dp(28));

        // ---- mathi: band garne (X) ----
        LinearLayout top = new LinearLayout(ctx);
        top.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        TextView close = new TextView(ctx);
        close.setText("✕");
        close.setTextColor(0xCCFFFFFF);
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Close dial screen");
        close.setOnClickListener(v -> {
            if (listener != null) listener.onClose();
        });
        top.addView(close, new LayoutParams(dp(44), dp(44)));
        addView(top, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- number dekhine ठाउँ ----
        display = new TextView(ctx);
        display.setTextColor(0xFFFFFFFF);
        display.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        display.setGravity(Gravity.CENTER);
        display.setSingleLine(true);
        display.setHint("Enter number");
        display.setHintTextColor(0x55FFFFFF);
        display.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
        LayoutParams dlp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.9f);
        addView(display, dlp);

        // ---- keypad ----
        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int c = 0; c < 3; c++) {
                final int idx = r * 3 + c;
                row.addView(key(ctx, idx), new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            }
            addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        // ---- tala: khali | CALL | backspace ----
        LinearLayout bottom = new LinearLayout(ctx);
        bottom.setOrientation(HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.addView(new View(ctx), new LayoutParams(0, 1, 1f));

        FrameLayoutHolder callHolder = new FrameLayoutHolder(ctx);
        TextView call = new TextView(ctx);
        call.setText("CALL");
        call.setTextColor(0xFFFFFFFF);
        call.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        call.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        call.setLetterSpacing(0.15f);
        call.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF2EA84F);
        g.setCornerRadius(dp(34));
        call.setBackground(g);
        call.setContentDescription("Call");
        call.setOnClickListener(v -> {
            if (number.length() > 0 && listener != null) listener.onCall(number.toString());
        });
        callHolder.addView(call, new LayoutParams(dp(150), dp(62)));
        bottom.addView(callHolder, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.6f));

        BackspaceView back = new BackspaceView(ctx);
        back.setContentDescription("Delete");
        back.setOnClickListener(v -> backspace());
        back.setOnLongClickListener(v -> {
            clear();
            return true;
        });
        bottom.addView(back, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        addView(bottom, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        refresh();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void clear() {
        number.setLength(0);
        refresh();
    }

    private void backspace() {
        if (number.length() > 0) number.setLength(number.length() - 1);
        refresh();
    }

    private void append(String s) {
        if (number.length() >= 20) return;
        number.append(s);
        refresh();
    }

    private void refresh() {
        display.setText(number.toString());
        int n = number.length();
        display.setTextSize(TypedValue.COMPLEX_UNIT_SP, n > 14 ? 24 : (n > 10 ? 30 : 38));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    /** Ek key: outline gol, ma ank, tala akshar (ABC...) */
    private View key(Context ctx, final int idx) {
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(0x00000000);
        ring.setStroke(dp(1), 0x66FFFFFF);
        cell.setBackground(null);

        TextView ringView = new TextView(ctx);
        ringView.setBackground(ring);
        ringView.setGravity(Gravity.CENTER);
        ringView.setSingleLine(false);
        ringView.setTextColor(0xFFFFFFFF);
        String d = DIGITS[idx];
        String l = LETTERS[idx];
        ringView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        if (l.length() > 0) {
            android.text.SpannableString sp = new android.text.SpannableString(d + "\n" + l);
            sp.setSpan(new android.text.style.RelativeSizeSpan(0.8f), d.length() + 1, sp.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ringView.setText(sp);
        } else {
            ringView.setText(d);
        }
        ringView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        ringView.setLineSpacing(0f, 0.8f);
        ringView.setIncludeFontPadding(false);
        ringView.setClickable(true);
        ringView.setOnClickListener(v -> append(d));
        if (idx == 10) {
            // 0 ko long press = +
            ringView.setOnLongClickListener(v -> {
                append("+");
                return true;
            });
        }
        // outline chhoto garera pressed ma halka bharne
        ringView.setOnTouchListener((v, ev) -> {
            int a = ev.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) {
                ring.setColor(0x33FFFFFF);
            } else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) {
                ring.setColor(0x00000000);
            }
            return false;
        });
        cell.addView(ringView, new LayoutParams(dp(74), dp(74)));
        return cell;
    }

    /** Khali FrameLayout (button lai bich ma rakhna) */
    private static class FrameLayoutHolder extends android.widget.FrameLayout {
        FrameLayoutHolder(Context c) {
            super(c);
        }

        @Override
        public void addView(View child, ViewGroup.LayoutParams p) {
            LayoutParams lp = new LayoutParams(p.width, p.height, Gravity.CENTER);
            super.addView(child, lp);
        }
    }

    /** Backspace icon (code le nai banaieko) */
    private static class BackspaceView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        BackspaceView(Context c) {
            super(c);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0xCCFFFFFF);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth(), getHeight());
            float u = s * 0.42f / 24f;
            p.setStrokeWidth(2f * u);
            cv.save();
            cv.translate(getWidth() / 2f - 12f * u, getHeight() / 2f - 12f * u);
            cv.scale(u, u);
            p.setStrokeWidth(2f);
            path.reset();
            path.moveTo(9f, 5f);
            path.lineTo(21f, 5f);
            path.lineTo(21f, 19f);
            path.lineTo(9f, 19f);
            path.lineTo(2f, 12f);
            path.close();
            cv.drawPath(path, p);
            cv.drawLine(12.5f, 9f, 17.5f, 15f, p);
            cv.drawLine(17.5f, 9f, 12.5f, 15f, p);
            cv.restore();
        }
    }
}
