package com.tinku.glasslauncher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private FrameLayout rootFrame;
    private ListView listViewLetter;
    private LinearLayout alphabetBar;
    private LinearLayout clockBox;
    private LinearLayout favoritesBox;
    private TextClock textClock;
    private TextClock textDate;
    private FrameLayout letterOverlay;
    private TextView textLetter;
    private List<ResolveInfo> installedApps = new ArrayList<ResolveInfo>();
    private List<ResolveInfo> letterApps = new ArrayList<ResolveInfo>();
    private AppAdapter letterAdapter;
    private PackageManager pm;
    private SharedPreferences prefs;
    private char currentLetter = 0;
    private boolean dragMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tinku", MODE_PRIVATE);
        pm = getPackageManager();
        rootFrame = findViewById(R.id.rootFrame);
        listViewLetter = findViewById(R.id.listViewLetter);
        alphabetBar = findViewById(R.id.alphabetBar);
        clockBox = findViewById(R.id.clockBox);
        favoritesBox = findViewById(R.id.favoritesBox);
        textClock = findViewById(R.id.textClock);
        textDate = findViewById(R.id.textDate);
        letterOverlay = findViewById(R.id.letterOverlay);
        textLetter = findViewById(R.id.textLetter);

        loadApps();

        letterAdapter = new AppAdapter(this, letterApps, R.layout.item_app_icon);
        listViewLetter.setAdapter(letterAdapter);
        listViewLetter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchApp(letterApps.get(position));
                hideOverlay();
            }
        });
        listViewLetter.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showMenu(letterApps.get(position));
                return true;
            }
        });
        letterOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOverlay();
            }
        });

        View.OnLongClickListener clockLong = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showClockMenu();
                return true;
            }
        };
        clockBox.setOnLongClickListener(clockLong);
        textClock.setOnLongClickListener(clockLong);
        textDate.setOnLongClickListener(clockLong);

        clockBox.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int oldL, int oldT, int oldR, int oldB) {
                if ((r - l) != (oldR - oldL) || (b - t) != (oldB - oldT)) {
                    applyClockPos();
                }
            }
        });

        setupAlphabetBar();
        applyAlign();
        applyClockPos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        applyAlign();
        applyClockPos();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void loadApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        installedApps = pm.queryIntentActivities(intent, 0);
        Collections.sort(installedApps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return String.CASE_INSENSITIVE_ORDER.compare(
                    a.loadLabel(pm).toString(),
                    b.loadLabel(pm).toString()
                );
            }
        });
    }

    private Set<String> getFavorites() {
        return new HashSet<String>(prefs.getStringSet("favs", new HashSet<String>()));
    }

    private void saveFavorites(Set<String> favs) {
        prefs.edit().putStringSet("favs", favs).apply();
    }

    private boolean isLeft() {
        return "left".equals(prefs.getString("align", "left"));
    }

    private void applyAlign() {
        int g = isLeft() ? Gravity.LEFT : Gravity.RIGHT;
        textClock.setGravity(g);
        textDate.setGravity(g);
        refreshFavorites();
    }

    private void applyClockPos() {
        rootFrame.post(new Runnable() {
            @Override
            public void run() {
                int w = rootFrame.getWidth();
                int h = rootFrame.getHeight();
                int cw = clockBox.getWidth();
                int ch = clockBox.getHeight();
                if (w == 0 || cw == 0) return;
                float fx = prefs.getFloat("cx", 0.06f);
                float fy = prefs.getFloat("cy", 0.55f);
                clockBox.setX(fx * Math.max(0, w - cw));
                clockBox.setY(fy * Math.max(0, h - ch));
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (dragMode) {
            handleDrag(e);
            return true;
        }
        return super.dispatchTouchEvent(e);
    }

    private void handleDrag(MotionEvent e) {
        int a = e.getActionMasked();
        int[] loc = new int[2];
        rootFrame.getLocationOnScreen(loc);
        int w = rootFrame.getWidth();
        int h = rootFrame.getHeight();
        int cw = clockBox.getWidth();
        int ch = clockBox.getHeight();
        float nx = e.getRawX() - loc[0] - cw / 2f;
        float ny = e.getRawY() - loc[1] - ch / 2f;
        nx = Math.max(0, Math.min(nx, w - cw));
        ny = Math.max(0, Math.min(ny, h - ch));
        clockBox.setX(nx);
        clockBox.setY(ny);
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            float fx = (w - cw) > 0 ? nx / (w - cw) : 0f;
            float fy = (h - ch) > 0 ? ny / (h - ch) : 0f;
            prefs.edit().putFloat("cx", fx).putFloat("cy", fy).apply();
            dragMode = false;
            Toast.makeText(this, "Clock position saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void showClockMenu() {
        showSheet(null, "Clock",
            new String[] { "Move clock (drag anywhere)", "Align text left", "Align text right", "Reset position" },
            new int[] { android.R.drawable.ic_menu_mylocation, 0, 0, android.R.drawable.ic_menu_revert },
            new Runnable[] {
                () -> {
                    dragMode = true;
                    Toast.makeText(this, "Touch and drag anywhere, then lift your finger", Toast.LENGTH_LONG).show();
                },
                () -> { prefs.edit().putString("align", "left").apply(); applyAlign(); },
                () -> { prefs.edit().putString("align", "right").apply(); applyAlign(); },
                () -> { prefs.edit().remove("cx").remove("cy").apply(); applyClockPos(); }
            });
    }

    private void refreshFavorites() {
        favoritesBox.removeAllViews();
        Set<String> favs = getFavorites();
        boolean left = isLeft();
        for (int i = 0; i < installedApps.size(); i++) {
            final ResolveInfo info = installedApps.get(i);
            if (!favs.contains(info.activityInfo.packageName)) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL | (left ? Gravity.LEFT : Gravity.RIGHT));
            row.setPadding(0, dp(8), 0, dp(8));

            ImageView iv = new ImageView(this);
            iv.setImageDrawable(info.loadIcon(pm));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(48), dp(48));

            TextView tv = new TextView(this);
            tv.setText(info.loadLabel(pm));
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(22);
            tv.setShadowLayer(6, 0, 0, 0x88000000);
            tv.setSingleLine(true);

            if (left) {
                tv.setPadding(dp(16), 0, 0, 0);
                row.addView(iv, ilp);
                row.addView(tv);
            } else {
                tv.setPadding(0, 0, dp(16), 0);
                row.addView(tv);
                row.addView(iv, ilp);
            }

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchApp(info);
                }
            });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showMenu(info);
                    return true;
                }
            });
            favoritesBox.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void showSheet(Drawable icon, CharSequence title, String[] labels,
                           int[] icons, final Runnable[] actions) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(20), dp(24), dp(28));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F1F1);
        float r = dp(28);
        bg.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        box.setBackground(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));
        if (icon != null) {
            ImageView hi = new ImageView(this);
            hi.setImageDrawable(icon);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(44), dp(44));
            hlp.rightMargin = dp(16);
            header.addView(hi, hlp);
        }
        TextView ht = new TextView(this);
        ht.setText(title);
        ht.setTextColor(0xFF111111);
        ht.setTextSize(24);
        header.addView(ht);
        box.addView(header);

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(15), 0, dp(15));

            if (icons != null && icons[i] != 0) {
                ImageView iv = new ImageView(this);
                iv.setImageResource(icons[i]);
                iv.setColorFilter(0xFF333333, PorterDuff.Mode.SRC_IN);
                row.addView(iv, new LinearLayout.LayoutParams(dp(26), dp(26)));
            } else {
                row.addView(new View(this), new LinearLayout.LayoutParams(dp(26), dp(26)));
            }

            TextView tv = new TextView(this);
            tv.setText(labels[i]);
            tv.setTextColor(0xFF111111);
            tv.setTextSize(18);
            tv.setPadding(dp(20), 0, 0, 0);
            row.addView(tv);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                    actions[idx].run();
                }
            });
            box.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        d.show();
    }

    private void showMenu(final ResolveInfo info) {
        final String pkg = info.activityInfo.packageName;
        final boolean isFav = getFavorites().contains(pkg);
        showSheet(info.loadIcon(pm), info.loadLabel(pm),
            new String[] { isFav ? "Remove from favourites" : "Favourite", "App info", "Uninstall" },
            new int[] {
                isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off,
                android.R.drawable.ic_menu_info_details,
                android.R.drawable.ic_menu_delete
            },
            new Runnable[] {
                () -> {
                    Set<String> favs = getFavorites();
                    if (isFav) favs.remove(pkg); else favs.add(pkg);
                    saveFavorites(favs);
                    refreshFavorites();
                },
                () -> {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(Uri.parse("package:" + pkg));
                    startActivity(i);
                },
                () -> {
                    Intent i = new Intent(Intent.ACTION_DELETE);
                    i.setData(Uri.parse("package:" + pkg));
                    startActivity(i);
                }
            });
    }

    private void launchApp(ResolveInfo info) {
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
    }

    private void setupAlphabetBar() {
        final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < letters.length(); i++) {
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(letters.charAt(i)));
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(11);
            tv.setShadowLayer(4, 0, 0, 0x88000000);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            tv.setLayoutParams(lp);
            alphabetBar.addView(tv);
        }

        alphabetBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    float y = event.getY() - v.getPaddingTop();
                    int h = v.getHeight() - v.getPaddingTop() - v.getPaddingBottom();
                    if (h <= 0) return true;
                    int index = (int) (y / h * letters.length());
                    if (index < 0) index = 0;
                    if (index >= letters.length()) index = letters.length() - 1;
                    showLetter(letters.charAt(index));
                }
                return true;
            }
        });
    }

    private void showLetter(char letter) {
        if (letter == currentLetter && letterOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        currentLetter = letter;
        letterApps.clear();
        for (int i = 0; i < installedApps.size(); i++) {
            ResolveInfo info = installedApps.get(i);
            String label = info.loadLabel(pm).toString();
            if (label.length() > 0 && Character.toUpperCase(label.charAt(0)) == letter) {
                letterApps.add(info);
            }
        }
        textLetter.setText(String.valueOf(letter));
        letterAdapter.notifyDataSetChanged();
        letterOverlay.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        letterOverlay.setVisibility(View.GONE);
        currentLetter = 0;
    }

    @Override
    public void onBackPressed() {
        if (letterOverlay.getVisibility() == View.VISIBLE) {
            hideOverlay();
        }
    }
}
