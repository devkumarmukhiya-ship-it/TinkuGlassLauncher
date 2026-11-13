package com.tinku.glasslauncher;

import android.os.Looper;

import android.os.Handler;

import android.Manifest;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.util.TypedValue;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends Activity {
    private static final String STAR = "\u2606";
    private static final String OTHER = "\u25CB";
    private static final int ROW_DP = 21;

    private FrameLayout rootFrame;
    private ListView listViewLetter;
    private ListView listViewSearch;
    private LinearLayout alphabetBar;
    private LinearLayout clockBox;
    private LinearLayout favoritesBox;
    private LinearLayout searchOverlay;
    private ImageView searchButton;
    private EditText searchInput;
    private TextClock textClock;
    private TextClock textDate;
    private FrameLayout letterOverlay;
    private Dialog drawerDialog;
    private TextView textLetter;
    private List<ResolveInfo> allApps = new ArrayList<ResolveInfo>();
    private List<ResolveInfo> installedApps = new ArrayList<ResolveInfo>();
    private List<DrawerAdapter.Row> drawerRows = new ArrayList<DrawerAdapter.Row>();
    private Map<String, Integer> sectionPos = new HashMap<String, Integer>();
    private List<ResolveInfo> searchApps = new ArrayList<ResolveInfo>();
    private List<String> keys = new ArrayList<String>();
    private DrawerAdapter drawerAdapter;
    private AppAdapter searchAdapter;
    private PackageManager pm;
    private SharedPreferences prefs;
    private String currentKey = "";
    private boolean dragMode = false;
    private View dragTarget = null;
    private String dragKx = "";
    private String dragKy = "";
    private boolean dragRefreshFav = false;
    private final List<View> floatingFavs = new ArrayList<View>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tinku", MODE_PRIVATE);
        pm = getPackageManager();
        rootFrame = findViewById(R.id.rootFrame);
        listViewLetter = findViewById(R.id.listViewLetter);
        listViewSearch = findViewById(R.id.listViewSearch);
        alphabetBar = findViewById(R.id.alphabetBar);
        clockBox = findViewById(R.id.clockBox);
        favoritesBox = findViewById(R.id.favoritesBox);
        searchOverlay = findViewById(R.id.searchOverlay);
        searchButton = findViewById(R.id.searchButton);
        searchInput = findViewById(R.id.searchInput);
        textClock = findViewById(R.id.textClock);
        textDate = findViewById(R.id.textDate);
        letterOverlay = findViewById(R.id.letterOverlay);
        textLetter = findViewById(R.id.textLetter);
        FontKit.watch(this, favoritesBox, FontKit.HOME);
        FontKit.watch(this, alphabetBar, FontKit.HOME);
        FontKit.watch(this, listViewLetter, FontKit.ALL);
        FontKit.watch(this, listViewSearch, FontKit.ALL);

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xB3FFFFFF);
        searchButton.setBackground(circle);
        searchButton.setColorFilter(0xFF222222, PorterDuff.Mode.SRC_IN);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearch();
            }
        });

        loadApps();

        textLetter.setVisibility(View.GONE);
        drawerAdapter = new DrawerAdapter(this, drawerRows);
        listViewLetter.setAdapter(drawerAdapter);
        listViewLetter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= drawerRows.size()) return;
                ResolveInfo app = drawerRows.get(position).app;
                if (app == null) return;
                launchApp(app);
                hideOverlay();
            }
        });
        listViewLetter.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= drawerRows.size()) return false;
                ResolveInfo app = drawerRows.get(position).app;
                if (app == null) return false;
                showMenu(app);
                return true;
            }
        });
        letterOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOverlay();
            }
        });
        setupDrawerDialog();
        rootFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDrawerOpen()) hideOverlay();
            }
        });

        searchAdapter = new AppAdapter(this, searchApps, R.layout.item_app_icon);
        listViewSearch.setAdapter(searchAdapter);
        listViewSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ResolveInfo info = searchApps.get(position);
                hideSearch();
                launchApp(info);
            }
        });
        listViewSearch.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showMenu(searchApps.get(position));
                return true;
            }
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterSearch(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH && !searchApps.isEmpty()) {
                    ResolveInfo info = searchApps.get(0);
                    hideSearch();
                    launchApp(info);
                    return true;
                }
                return false;
            }
        });

        View.OnLongClickListener homeLong = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showHomeMenu();
                return true;
            }
        };
        rootFrame.setOnLongClickListener(homeLong);

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
        favoritesBox.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int oldL, int oldT, int oldR, int oldB) {
                if ((r - l) != (oldR - oldL) || (b - t) != (oldB - oldT)) {
                    applyFavPos();
                }
            }
        });

        setupV7();
        setupV8();
        setupV12();
        setupV13();
        setupGestures();
        setupTinku();
        setupV30();
        setupV31();
        setupV32();
        setupV34();
        setupAlphabetTouch();
        buildAlphabetBar();
        applyClockStyle();
        applyAlign();
        applyClockPos();
        applyFavPos();
    }

    @Override
    protected void onResume() {
        checkLockDue();
        super.onResume();
        tinkuResumed = true;
        tinkuApplyListen();
        refreshWeather(false);
        refreshInfo();
        loadApps();
        buildAlphabetBar();
        applyClockStyle();
        applyAlign();
        applyClockPos();
        applyFavPos();
        applyAppFont();
        applyV31();
        applyMedia();
    }

    private void applyClockStyle() {
        if (prefs.getBoolean("stacked", false)) {
            textClock.setFormat12Hour("h\nmm");
            textClock.setFormat24Hour("HH\nmm");
            textClock.setLineSpacing(0f, 0.82f);
        } else {
            textClock.setFormat12Hour("h:mm");
            textClock.setFormat24Hour("HH:mm");
            textClock.setLineSpacing(0f, 1f);
        }
    }

    // ===== Step 7: lock screen, ubho weekday, video-style home =====
    private LockView lockView;
    private DayView dayView;
    private boolean dayOpen = false;
    private BroadcastReceiver screenReceiver;

    private void setupV7() {
        if (!prefs.getBoolean("v7", false)) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean("v7", true);
            ed.putBoolean("lockscreen", true);
            ed.putBoolean("mono", true);
            ed.putBoolean("stacked", true);
            ed.putString("align", "left");
            ed.putString("favalign", "left");
            ed.putString("favlayout", "vertical");
            ed.putFloat("cx", 0.07f);
            ed.putFloat("cy", 0.07f);
            ed.putFloat("fx", 0.07f);
            ed.putFloat("fy", 0.40f);
            ed.apply();
            Set<String> favs = getFavorites();
            if (favs.isEmpty()) {
                String[] want = {
                    "com.android.vending", "com.instagram.android",
                    "com.google.android.apps.tachyon", "com.google.android.apps.messaging",
                    "com.google.android.apps.photos", "com.android.settings"
                };
                for (int i = 0; i < want.length; i++) {
                    for (int j = 0; j < installedApps.size(); j++) {
                        if (installedApps.get(j).activityInfo.packageName.equals(want[i])) {
                            favs.add(want[i]);
                            break;
                        }
                    }
                }
                saveFavorites(favs);
            }
        }

        // wallpaper mathi halka dark gradient (text spasta dekhincha)
        View scrim = new View(this);
        scrim.setVisibility(View.GONE);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] { 0x00000000, 0x00000000, 0x00000000 }));
        rootFrame.addView(scrim, 0, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // home clock/date style
        textClock.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        textClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 54);
        textClock.setIncludeFontPadding(false);
        textClock.setTextColor(0xFFFFFFFF);
        textDate.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textDate.setLetterSpacing(0.16f);
        textDate.setTextColor(0xCCFFFFFF);
        textDate.setFormat12Hour("d MMM yyyy");
        textDate.setFormat24Hour("d MMM yyyy");

        // ubho THURSDAY (clock ma tap garda)
        dayView = new DayView(this);
        dayView.setVisibility(View.GONE);
        dayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDay();
            }
        });
        rootFrame.addView(dayView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.LEFT));
        View.OnClickListener dayClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDay();
            }
        };
        clockBox.setOnClickListener(dayClick);
        textClock.setOnClickListener(dayClick);
        textDate.setOnClickListener(dayClick);

        // lock screen (sabai bhanda mathi)
        lockView = new LockView(this);
        lockView.setPrefs(prefs);
        lockView.setVisibility(prefs.getBoolean("lockscreen", true) ? View.VISIBLE : View.GONE);
        rootFrame.addView(lockView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!prefs.getBoolean("lockscreen", true)) return;
                if (lockDelayHandling()) return;
                hideOverlay();
                if (searchOverlay.getVisibility() == View.VISIBLE) hideSearch();
                if (dayOpen) toggleDay();
                lockView.showLock();
            }
        };
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        pkgReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshAll();
            }
        };
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pkgFilter.addDataScheme("package");
        registerReceiver(pkgReceiver, pkgFilter);
    }

    private BroadcastReceiver pkgReceiver;

    private void unregisterV7() {
        try {
            if (pkgReceiver != null) unregisterReceiver(pkgReceiver);
        } catch (Exception e) {
            // already gone
        }
        pkgReceiver = null;
        try {
            if (chargeReceiver != null) unregisterReceiver(chargeReceiver);
        } catch (Exception e) {
            // already gone
        }
        chargeReceiver = null;
        try {
            if (screenReceiver != null) unregisterReceiver(screenReceiver);
        } catch (Exception e) {
            // already gone
        }
        screenReceiver = null;
        unregisterV8();
    }

    private void showLockNow() {
        if (lockView == null) return;
        hideOverlay();
        lockView.showLock();
    }

    private void toggleDay() {
        dayView.animate().cancel();
        clockBox.animate().cancel();
        if (dayOpen) {
            dayOpen = false;
            dayView.animate().alpha(0f).translationX(-dp(16)).setDuration(180)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!dayOpen) dayView.setVisibility(View.GONE);
                    }
                }).start();
            clockBox.animate().alpha(1f).setDuration(220).start();
        } else {
            dayOpen = true;
            dayView.setDay(new java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH)
                .format(new java.util.Date()).toUpperCase(java.util.Locale.ENGLISH));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) dayView.getLayoutParams();
            lp.leftMargin = (int) clockBox.getX() + dp(2);
            lp.topMargin = (int) clockBox.getY();
            dayView.setLayoutParams(lp);
            dayView.setAlpha(0f);
            dayView.setTranslationX(-dp(16));
            dayView.setVisibility(View.VISIBLE);
            dayView.animate().alpha(1f).translationX(0f).setDuration(240).start();
            clockBox.animate().alpha(0f).setDuration(200).start();
        }
    }

    private Drawable glassPanel(boolean blur) {
        String glass = prefs.getString("glass", "clear");
        int a;
        int b;
        int line;
        if (!blur) {
            a = 0xE62C3340;
            b = 0xCC2C3340;
            line = 0x1FFFFFFF;
        } else if ("dark".equals(glass)) {
            a = 0x99202632;
            b = 0x77202632;
            line = 0x33FFFFFF;
        } else if ("light".equals(glass)) {
            a = 0x66FFFFFF;
            b = 0x4DFFFFFF;
            line = 0x66FFFFFF;
        } else {
            a = 0x00000000;
            b = 0x00000000;
            line = 0x33FFFFFF;
        }
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { a, b });
        float rr = dp(30);
        g.setCornerRadii(new float[] { 0, 0, rr, rr, rr, rr, 0, 0 });
        g.setStroke(dp(1), line);
        return g;
    }

    // ===== Step 8: day+date slot (+ buttons), clear wallpaper =====
    private TextView plusTop;
    private TextView plusBottom;
    private TextView infoTop;
    private TextView infoBottom;
    private View.OnClickListener plusClick;
    private BroadcastReceiver infoReceiver;

    private void setupV8() {
        // status bar transparent: wallpaper pura screen ma spasta dekhincha (Niagara jasto)
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.setStatusBarColor(0);
        win.getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // clock ko shadow (wallpaper jun sukai bhaye pani padhna milne)
        textClock.setShadowLayer(8f, 0f, 2f, 0x66000000);

        // "+" tap garda day/date mathi ya tala jaanchha
        plusClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean top = (v == plusTop);
                toggleSection(top);
                if (true) return;
                prefs.edit().putString("dateslot", top ? "top" : "bottom").apply();
                refreshInfo();
                TextView shown = top ? infoTop : infoBottom;
                shown.setAlpha(0f);
                shown.animate().alpha(1f).setDuration(220).start();
            }
        };

        clockBox.setOrientation(LinearLayout.VERTICAL);
        plusTop = makePlus();
        plusBottom = makePlus();
        infoTop = makeInfo();
        infoBottom = makeInfo();
        clockBox.addView(plusTop, 0, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        clockBox.addView(infoTop, 1, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        clockBox.addView(infoBottom, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        clockBox.addView(plusBottom, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textDate.setVisibility(View.GONE);

        infoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshInfo();
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_DATE_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        registerReceiver(infoReceiver, f);

        refreshInfo();
        applyV8Align();
    }

    private TextView makePlus() {
        TextView t = new TextView(this);
        t.setText("+");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        t.setTextColor(0xCCFFFFFF);
        t.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setPadding(dp(2), dp(6), dp(14), dp(6));
        t.setShadowLayer(6f, 0f, 1f, 0x66000000);
        t.setOnClickListener(plusClick);
        return t;
    }

    private TextView makeInfo() {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setLetterSpacing(0.18f);
        t.setLineSpacing(0f, 1.1f);
        t.setTextColor(0xF2FFFFFF);
        t.setPadding(dp(2), 0, dp(2), dp(4));
        t.setShadowLayer(6f, 0f, 1f, 0x66000000);
        return t;
    }

    private void refreshInfo() {
        refreshInfoV12();
        if (true) return;
        if (infoTop == null || infoBottom == null) return;
        java.util.Date now = new java.util.Date();
        String day = new java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH)
            .format(now).toUpperCase(java.util.Locale.ENGLISH);
        String date = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ENGLISH).format(now);
        String text = day + "\n" + date;
        String extra = extraInfo();
        if (extra.length() > 0) text += "\n" + extra;
        infoTop.setText(text);
        infoBottom.setText(text);
        boolean top = !"bottom".equals(prefs.getString("dateslot", "top"));
        infoTop.setVisibility(top ? View.VISIBLE : View.GONE);
        infoBottom.setVisibility(top ? View.GONE : View.VISIBLE);
    }

    private void applyV8Align() {
        applyV13Align();
        if (plusTop == null) return;
        int g = isLeft() ? Gravity.LEFT : Gravity.RIGHT;
        plusTop.setGravity(g);
        plusBottom.setGravity(g);
        infoTop.setGravity(g);
        infoBottom.setGravity(g);
    }

    private void unregisterV8() {
        try {
            if (infoReceiver != null) unregisterReceiver(infoReceiver);
        } catch (Exception e) {
            // already gone
        }
        infoReceiver = null;
    }

    // ===== Step 9: weather, battery line, swipe gestures =====
    private boolean weatherBusy = false;
    private GestureDetector gestures;

    private int batteryPercent() {
        try {
            Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b == null) return -1;
            int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return scale > 0 ? (level * 100) / scale : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String extraInfo() {
        StringBuilder sb = new StringBuilder();
        if (prefs.getBoolean("weather", false)) {
            String t = prefs.getString("wtext", null);
            sb.append(t != null ? t.toUpperCase(java.util.Locale.ENGLISH) : "WEATHER...");
        }
        if (prefs.getBoolean("showbat", false)) {
            int pct = batteryPercent();
            if (pct >= 0) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("BATTERY ").append(pct).append("%");
            }
        }
        return sb.toString();
    }

    private void refreshWeather(boolean force) {
        if (!prefs.getBoolean("weather", false) || weatherBusy) return;
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("wtime", 0L) < 30L * 60L * 1000L) return;
        weatherBusy = true;
        final String city = prefs.getString("wcity", "Kathmandu");
        final boolean fahrenheit = "f".equals(prefs.getString("wunit", "c"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String text = Weather.fetch(prefs, city, fahrenheit);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        weatherBusy = false;
                        if (text != null) {
                            prefs.edit().putString("wtext", text)
                                .putLong("wtime", System.currentTimeMillis()).apply();
                        } else if (prefs.getString("wtext", null) == null) {
                            prefs.edit().putString("wtext", "Weather unavailable").apply();
                        }
                        refreshInfo();
                    }
                });
            }
        }).start();
    }

    private void showCityDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(22), dp(24), dp(24));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F1F1);
        float r = dp(28);
        bg.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        box.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("Weather city");
        title.setTextColor(0xFF111111);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, dp(14));
        box.addView(title);

        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(0xFF111111);
        input.setTextSize(20);
        input.setText(prefs.getString("wcity", "Kathmandu"));
        input.setSelectAllOnFocus(true);
        GradientDrawable ibg = new GradientDrawable();
        ibg.setColor(0xFFFFFFFF);
        ibg.setCornerRadius(dp(14));
        ibg.setStroke(dp(1), 0xFFB0B0B0);
        input.setBackground(ibg);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        box.addView(input, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.RIGHT);
        buttons.setPadding(0, dp(18), 0, 0);
        TextView cancel = makeButton("Cancel", 0xFF00695C, 0x00000000);
        TextView save = makeButton("Save", 0xFFFFFFFF, 0xFF00796B);
        buttons.addView(cancel);
        buttons.addView(save);
        box.addView(buttons, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = input.getText().toString().trim();
                if (t.length() > 0) {
                    prefs.edit().putString("wcity", t).remove("wtext").remove("wtime").apply();
                    refreshInfo();
                    refreshWeather(true);
                }
                d.dismiss();
            }
        });

        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        showD(d);
        input.requestFocus();
    }

    private void setupGestures() {
        gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                if (!prefs.getBoolean("gestures", false)) return false;
                if (dragMode || isDrawerOpen()) return false;
                if (searchOverlay.getVisibility() == View.VISIBLE) return false;
                if (lockView != null && lockView.getVisibility() == View.VISIBLE) return false;
                if (e1.getRawX() > getResources().getDisplayMetrics().widthPixels - dp(72)) return false;
                float dy = e2.getY() - e1.getY();
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dy) < dp(120) || Math.abs(dy) < Math.abs(dx) * 1.5f) return false;
                if (dy > 0) {
                    expandNotifications();
                } else {
                    showSearch();
                }
                return true;
            }
        });
    }

    private void expandNotifications() {
        try {
            Object sb = getSystemService("statusbar");
            Class<?> c = Class.forName("android.app.StatusBarManager");
            c.getMethod("expandNotificationsPanel").invoke(sb);
        } catch (Throwable t) {
            Toast.makeText(this, "Could not open the notification panel", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Step 12: + / - show-hide, text tone, anti-overlap =====
    private void refreshInfoV12() {
        if (infoTop == null || infoBottom == null || plusTop == null || plusBottom == null) return;
        java.util.Date now = new java.util.Date();
        String day = new java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH)
            .format(now).toUpperCase(java.util.Locale.ENGLISH);
        String date = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ENGLISH).format(now);
        boolean showTop = prefs.getBoolean("showtop", false) || prefs.getBoolean("events", false);
        boolean showBottom = prefs.getBoolean("showbottom", false)
            || prefs.getBoolean("weather", false) || prefs.getBoolean("showbat", false);
        String ev = nextEventText();
        infoTop.setText(day + "\n" + date + (ev.length() > 0 ? "\n" + ev : ""));
        String extra = extraInfo();
        infoBottom.setText(extra.length() > 0 ? extra : "WEATHER...");
        infoTop.setVisibility(showTop ? View.VISIBLE : View.GONE);
        infoBottom.setVisibility(showBottom ? View.VISIBLE : View.GONE);
        plusTop.setText(showTop ? "\u2212" : "+");
        plusBottom.setText(showBottom ? "\u2212" : "+");
    }

    /** top = true: day/date (clock ko mathi), false: weather + battery (clock ko tala) */
    private void toggleSection(boolean top) {
        boolean on;
        SharedPreferences.Editor ed = prefs.edit();
        if (top) {
            on = !(prefs.getBoolean("showtop", false) || prefs.getBoolean("events", false));
            ed.putBoolean("showtop", on);
            if (!on) ed.putBoolean("events", false);
        } else {
            on = !(prefs.getBoolean("showbottom", false)
                || prefs.getBoolean("weather", false) || prefs.getBoolean("showbat", false));
            ed.putBoolean("showbottom", on);
            ed.putBoolean("weather", on);
            ed.putBoolean("showbat", on);
        }
        ed.apply();
        if (!top && on) refreshWeather(true);
        refreshInfoV12();
        if (on) {
            TextView shown = top ? infoTop : infoBottom;
            shown.setAlpha(0f);
            shown.animate().alpha(1f).setDuration(220).start();
        }
    }

    private int homeTextColor() {
        int hcy = prefs.getInt("home_color", 0);
        if (hcy != 0) return hcy;
        return prefs.getBoolean("darktext", false) ? 0xFF1B1F27 : 0xFFFFFFFF;
    }

    private void applyTextTone() {
        boolean dark = prefs.getBoolean("darktext", false);
        int main = dark ? 0xFF1B1F27 : 0xFFFFFFFF;
        int soft = dark ? 0xCC1B1F27 : 0xCCFFFFFF;
        float sr = dark ? 0f : 8f;
        int sc = dark ? 0 : 0x66000000;
        int hcx = prefs.getInt("home_color", 0);
        if (hcx != 0) {
            main = hcx;
            soft = (hcx & 0x00FFFFFF) | 0xCC000000;
        }
        textClock.setTextColor(main);
        textClock.setShadowLayer(sr, 0f, 2f, sc);
        if (infoTop != null) {
            infoTop.setTextColor(main);
            infoTop.setShadowLayer(sr * 0.75f, 0f, 1f, sc);
        }
        if (infoBottom != null) {
            infoBottom.setTextColor(main);
            infoBottom.setShadowLayer(sr * 0.75f, 0f, 1f, sc);
        }
        if (plusTop != null) {
            plusTop.setTextColor(soft);
            plusTop.setShadowLayer(sr * 0.75f, 0f, 1f, sc);
        }
        if (plusBottom != null) {
            plusBottom.setTextColor(soft);
            plusBottom.setShadowLayer(sr * 0.75f, 0f, 1f, sc);
        }
        if (dayView != null) dayView.setColor(main);
        if (clockText != null) clockText.setTone(main, !dark);
        refreshFavorites();
    }

    /** clock block lamo bhayo bhane favourites list tala sarne (overlap nahune) */
    private void nudgeFavs() {
        if (clockBox.getWidth() == 0 || favoritesBox.getWidth() == 0) return;
        float cl = clockBox.getX();
        float cr = cl + clockBox.getWidth();
        float fl = favoritesBox.getX();
        float fr = fl + favoritesBox.getWidth();
        if (cr < fl || fr < cl) return;
        float ft = favoritesBox.getY();
        if (ft < clockBox.getY()) return;
        float limit = clockBox.getY() + clockBox.getHeight() + dp(14);
        if (ft < limit) favoritesBox.setY(limit);
    }

    private void setupV12() {
        if (!prefs.getBoolean("v12", false)) {
            prefs.edit().putBoolean("v12", true).putBoolean("darktext", true).apply();
        }
        clockBox.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int oldL, int oldT, int oldR, int oldB) {
                if ((r - l) != (oldR - oldL) || (b - t) != (oldB - oldT)) {
                    applyFavPos();
                    rootFrame.post(new Runnable() {
                        @Override
                        public void run() {
                            nudgeFavs();
                        }
                    });
                }
            }
        });
        applyTextTone();
        refreshInfoV12();
    }

    // ===== Step 13: Tinku widget sheet, clock styles, lock screen setup =====
    private ClockText clockText;
    private final android.os.Handler lockHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long lockOffAt = 0L;
    private final Runnable idleLockRun = new Runnable() {
        @Override
        public void run() {
            if (lockView == null || !prefs.getBoolean("lockscreen", true)) return;
            if (lockView.getVisibility() == View.VISIBLE) return;
            if (isDrawerOpen()) return;
            hideOverlay();
            lockView.showLock();
        }
    };

    private void setupV13() {
        if (!prefs.getBoolean("v13", false)) {
            prefs.edit().putBoolean("v13", true)
                .putBoolean("lock_date", true).putBoolean("lock_batt", true).putBoolean("lock_sec", true)
                .putString("lock_delay", "0").putString("lock_idle", "0")
                .apply();
        }
        clockText = new ClockText(this);
        int idx = clockBox.indexOfChild(textClock);
        if (idx < 0) idx = 2;
        clockBox.addView(clockText, idx, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textClock.setVisibility(View.GONE);
        clockText.setStyleIndex(prefs.getInt("clockstyle", 36));
        clockText.setOnClickListener(v -> toggleDay());
        clockText.setOnLongClickListener(v -> clockBox.performLongClick());
        applyV13Align();
        applyTextTone();
    }

    private void applyV13Align() {
        if (clockText != null) clockText.setAlignRight(!isLeft());
    }

    private void applyClockStyleV13() {
        if (clockText == null) return;
        clockText.setStyleIndex(prefs.getInt("clockstyle", 36));
        applyAnalogColors();
        applyClockAlpha();
        clockBox.requestLayout();
    }

    // ---------- lock screen timer ----------
    private long lockSecs(String key) {
        try {
            return Long.parseLong(prefs.getString(key, "0"));
        } catch (Exception e) {
            return 0L;
        }
    }

    /** true = abhi lock nagarne (timer pachi herne) */
    private boolean lockDelayHandling() {
        if (lockSecs("lock_delay") == 0L) return false;
        lockOffAt = System.currentTimeMillis();
        return true;
    }

    private void checkLockDue() {
        if (lockOffAt > 0L && lockView != null && prefs.getBoolean("lockscreen", true)) {
            long sec = lockSecs("lock_delay");
            if (sec >= 0L && System.currentTimeMillis() - lockOffAt >= sec * 1000L) {
                hideOverlay();
                lockView.showLock();
            }
        }
        lockOffAt = 0L;
        scheduleIdleLock();
    }

    private void scheduleIdleLock() {
        lockHandler.removeCallbacks(idleLockRun);
        if (lockView == null || !prefs.getBoolean("lockscreen", true)) return;
        long sec = lockSecs("lock_idle");
        if (sec > 0L) lockHandler.postDelayed(idleLockRun, sec * 1000L);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        scheduleIdleLock();
    }

    // ---------- calendar ----------
    private void ensureCalendarPermission() {
        if (!prefs.getBoolean("events", false)) return;
        if (android.os.Build.VERSION.SDK_INT >= 23
            && checkSelfPermission("android.permission.READ_CALENDAR")
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { "android.permission.READ_CALENDAR" }, 1301);
        }
    }

    private String nextEventText() {
        if (!prefs.getBoolean("events", false)) return "";
        if (android.os.Build.VERSION.SDK_INT >= 23
            && checkSelfPermission("android.permission.READ_CALENDAR")
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        android.database.Cursor c = null;
        try {
            long now = System.currentTimeMillis();
            android.net.Uri.Builder ub = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon();
            android.content.ContentUris.appendId(ub, now);
            android.content.ContentUris.appendId(ub, now + 2L * 24L * 3600L * 1000L);
            c = getContentResolver().query(ub.build(),
                new String[] {
                    android.provider.CalendarContract.Instances.TITLE,
                    android.provider.CalendarContract.Instances.BEGIN,
                    android.provider.CalendarContract.Instances.ALL_DAY },
                null, null, android.provider.CalendarContract.Instances.BEGIN + " ASC");
            while (c != null && c.moveToNext()) {
                String title = c.getString(0);
                long begin = c.getLong(1);
                boolean allDay = c.getInt(2) == 1;
                if (title == null || title.length() == 0) continue;
                if (!allDay && begin < now - 30L * 60L * 1000L) continue;
                String when = allDay ? "TODAY"
                    : new java.text.SimpleDateFormat("EEE h:mm a", java.util.Locale.ENGLISH)
                        .format(new java.util.Date(begin)).toUpperCase(java.util.Locale.ENGLISH);
                return when + " \u00b7 " + title.toUpperCase(java.util.Locale.getDefault());
            }
        } catch (Exception e) {
            // calendar padhna sakiena
        } finally {
            if (c != null) c.close();
        }
        return "";
    }

    // ---------- common sheet helpers ----------
    private LinearLayout sheetBox(String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(20), dp(24), dp(28));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F1F1);
        float r = dp(28);
        bg.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        box.setBackground(bg);
        TextView ht = new TextView(this);
        ht.setText(title);
        ht.setTextColor(0xFF111111);
        ht.setTextSize(24);
        ht.setPadding(0, 0, 0, dp(8));
        box.addView(ht);
        return box;
    }

    private void showBottom(Dialog d, View content) {
        d.setContentView(content);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        showD(d);
    }

    private void addTapRow(LinearLayout box, String label, String sub, final Runnable r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        TextView t = new TextView(this);
        t.setText(label + "  \u203A");
        t.setTextColor(0xFF111111);
        t.setTextSize(17);
        row.addView(t);
        if (sub != null) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextColor(0xFF666666);
            s.setTextSize(13);
            row.addView(s);
        }
        row.setOnClickListener(v -> r.run());
        box.addView(row, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addSeekRow(LinearLayout box, String label, final String key,
                            final float min, final float max, final float def, final Runnable onChange) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(0xFF111111);
        t.setTextSize(16);
        t.setPadding(0, dp(12), 0, 0);
        box.addView(t);
        android.widget.SeekBar sb = new android.widget.SeekBar(this);
        sb.setMax(1000);
        float cur = prefs.getFloat(key, def);
        sb.setProgress(Math.round((cur - min) / (max - min) * 1000f));
        sb.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar s, int p, boolean fromUser) {
                if (!fromUser) return;
                prefs.edit().putFloat(key, min + (max - min) * p / 1000f).apply();
                if (onChange != null) onChange.run();
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar s) {
            }
        });
        box.addView(sb, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ---------- Tinku widget sheet (home ma long press) ----------
    private View widgetRow(String glyph, String title, String sub, boolean enabled,
                           final String key, final Runnable tap, final Runnable change) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(15), dp(16), dp(15));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF5F5F5);
        bg.setCornerRadius(dp(22));
        row.setBackground(bg);
        row.setAlpha(enabled ? 1f : 0.5f);

        TextView g = new TextView(this);
        g.setText(glyph);
        g.setTextSize(22);
        g.setTextColor(0xFF333333);
        g.setGravity(Gravity.CENTER);
        row.addView(g, new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, dp(8), 0);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFF222222);
        t.setTextSize(18);
        col.addView(t);
        if (sub != null) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextColor(0xFF666666);
            s.setTextSize(14);
            col.addView(s);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (key != null) {
            final android.widget.Switch sw = new android.widget.Switch(this);
            sw.setChecked(prefs.getBoolean(key, false));
            sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                    prefs.edit().putBoolean(key, on).apply();
                    if (change != null) change.run();
                }
            });
            row.addView(sw);
            if (tap == null) {
                row.setOnClickListener(v -> sw.toggle());
            } else {
                row.setOnClickListener(v -> tap.run());
            }
        } else {
            TextView ch = new TextView(this);
            ch.setText("\u203A");
            ch.setTextSize(26);
            ch.setTextColor(0xFF555555);
            row.addView(ch);
            row.setOnClickListener(v -> {
                if (tap != null) tap.run();
            });
        }
        return row;
    }

    private LinearLayout.LayoutParams rowLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(4);
        return lp;
    }

    private void showWidgetSheet() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // main options (adha sheet ma dekhine) pahila, baki mathi swipe garda
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(6), dp(16), dp(28));

        TextView title = new TextView(this);
        title.setText("Tinku widget");
        title.setTextColor(0xFF333333);
        title.setTextSize(28);
        title.setPadding(dp(8), 0, 0, dp(14));
        box.addView(title);

        box.addView(widgetRow("\u25F7", "Clock style", null, true, null,
            () -> { d.dismiss(); showClockStyles(); }, null), rowLp());
        box.addView(widgetRow("\u25A6", "Calendar", "Show upcoming events, Show date", true, null,
            () -> { d.dismiss(); showCalendarDialog(); }, null), rowLp());
        box.addView(widgetRow("\u2600\uFE0E", "Weather", "Location, Temperature units", true, "weather",
            () -> { d.dismiss(); showWeatherDialog(); },
            () -> {
                if (prefs.getBoolean("weather", false)) refreshWeather(true);
                refreshInfoV12();
            }), rowLp());
        box.addView(widgetRow("\u2195", "Move Widget", null, true, null,
            () -> { d.dismiss(); startDrag(clockBox, "cx", "cy"); }, null), rowLp());
        box.addView(widgetRow("\u25AE", "Show Battery Percentage", null, true, "showbat",
            null, () -> refreshInfoV12()), rowLp());

        View gap = new View(this);
        box.addView(gap, new LinearLayout.LayoutParams(1, dp(12)));

        box.addView(widgetRow("\u2606", "Edit favourites", null, true, null,
            () -> { d.dismiss(); showFavMenu(); }, null), rowLp());
        box.addView(widgetRow("\u2699\uFE0E", "Tinku settings", "Lock screen, voice, glass and more", true, null,
            () -> { d.dismiss(); showSettings(); }, null), rowLp());

        View gap2 = new View(this);
        box.addView(gap2, new LinearLayout.LayoutParams(1, dp(12)));

        box.addView(widgetRow("\u229E", "Add Custom Widget", "Next step ma aaucha", false, null,
            () -> Toast.makeText(this, "Add Custom Widget: next step ma", Toast.LENGTH_SHORT).show(), null), rowLp());
        box.addView(widgetRow("\u25B7", "Show Media Widget", "Music play/pause (notification access chahincha)", true, "media",
            () -> { if (!hasNotifAccess()) startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); },
            () -> applyMedia()), rowLp());

        final int screenH = getResources().getDisplayMetrics().heightPixels;
        final int fullH = (int) (screenH * 0.92f);
        final int peekH = (int) (screenH * 0.58f);
        final int collapsedY = fullH - peekH;
        final int dismissY = collapsedY + dp(90);
        final int slop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
        final boolean[] expanded = { false };
        final float[] st = new float[3];

        final ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(box);

        final LinearLayout panel = new LinearLayout(this) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                int a = ev.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {
                    st[0] = ev.getRawY();
                    st[1] = getTranslationY();
                    st[2] = 0f;
                } else if (a == MotionEvent.ACTION_MOVE) {
                    float dy = ev.getRawY() - st[0];
                    if (Math.abs(dy) > slop) {
                        if (!expanded[0]) {
                            st[2] = 1f;
                            return true;
                        }
                        if (dy > 0 && sv.getScrollY() == 0) {
                            st[2] = 1f;
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                int a = ev.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {
                    st[0] = ev.getRawY();
                    st[1] = getTranslationY();
                    return true;
                }
                if (a == MotionEvent.ACTION_MOVE) {
                    float ty = st[1] + (ev.getRawY() - st[0]);
                    if (ty < 0f) ty = 0f;
                    setTranslationY(ty);
                    st[2] = 1f;
                    return true;
                }
                if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    if (st[2] == 0f) return true;
                    float ty = getTranslationY();
                    if (ty > dismissY) {
                        d.dismiss();
                    } else {
                        float target = ty < collapsedY / 2f ? 0f : (float) collapsedY;
                        expanded[0] = target == 0f;
                        animate().translationY(target).setDuration(180).start();
                    }
                    return true;
                }
                return true;
            }
        };
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFEBEBEB);
        float r = dp(30);
        bg.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        panel.setBackground(bg);

        View handle = new View(this);
        GradientDrawable hb = new GradientDrawable();
        hb.setColor(0xFFB8B8B8);
        hb.setCornerRadius(dp(3));
        handle.setBackground(hb);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(44), dp(5));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.topMargin = dp(12);
        hlp.bottomMargin = dp(10);
        panel.addView(handle, hlp);
        panel.addView(sv, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        FrameLayout root = new FrameLayout(this);
        root.setOnClickListener(v -> d.dismiss());
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, fullH, Gravity.BOTTOM);
        root.addView(panel, plp);

        panel.setTranslationY(fullH);
        panel.post(new Runnable() {
            @Override
            public void run() {
                panel.animate().translationY(collapsedY).setDuration(220).start();
            }
        });

        d.setContentView(root);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        showD(d);
    }

    private void showFavMenu() {
        showSheet(null, "Favourites",
            new String[] {
                "Favourites: icon on left",
                "Favourites: icon on right",
                "Reset favourites positions",
                "Hidden apps"
            },
            new int[] { 0, 0, android.R.drawable.ic_menu_revert, android.R.drawable.ic_menu_view },
            new Runnable[] {
                () -> { prefs.edit().putString("favalign", "left").apply(); refreshFavorites(); },
                () -> { prefs.edit().putString("favalign", "right").apply(); refreshFavorites(); },
                () -> {
                    SharedPreferences.Editor ed = prefs.edit();
                    for (String k : prefs.getAll().keySet()) {
                        if (k.startsWith("fpx:") || k.startsWith("fpy:")) ed.remove(k);
                    }
                    ed.remove("fx").remove("fy");
                    ed.apply();
                    refreshFavorites();
                    applyFavPos();
                },
                () -> showHiddenApps()
            });
    }

    private void showCalendarDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = sheetBox("Calendar");
        addSwitchRow(box, "Show date and day", "showtop", () -> refreshInfoV12());
        addSwitchRow(box, "Show upcoming events", "events", () -> {
            ensureCalendarPermission();
            refreshInfoV12();
        });
        showBottom(d, box);
    }

    private void showWeatherDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = sheetBox("Weather");
        addSwitchRow(box, "Show weather", "weather", () -> {
            if (prefs.getBoolean("weather", false)) refreshWeather(true);
            refreshInfoV12();
        });
        addChoiceRow(box, "Temperature units", "wunit", "c",
            new String[] { "c", "f" },
            new String[] { "\u00b0C", "\u00b0F" },
            () -> {
                prefs.edit().remove("wtext").remove("wtime").apply();
                refreshWeather(true);
                refreshInfoV12();
            });
        TextView city = makeButton("Change location", 0xFFFFFFFF, 0xFF00695C);
        city.setOnClickListener(v -> { d.dismiss(); showCityDialog(); });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(14);
        box.addView(city, clp);
        showBottom(d, box);
    }

    // ---------- Clock style gallery ----------
    private View styleTile(final int idx, boolean selected, final Dialog d) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.BOTTOM);
        tile.setPadding(dp(14), dp(10), dp(14), dp(12));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[] { 0xFF8E8E8E, 0xFF5C5C5C });
        g.setCornerRadius(dp(24));
        if (selected) g.setStroke(dp(3), 0xFF1565C0);
        tile.setBackground(g);

        ClockText ct = new ClockText(this);
        ct.setSample(true);
        ct.setStyleIndex(idx);
        ct.setTone(0xFFFFFFFF, false);
        tile.addView(ct, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!ClockText.isDay(idx)) {
            TextView dt = new TextView(this);
            dt.setText("Thu, 30 Aug  100%");
            dt.setTextColor(0xFFFFFFFF);
            dt.setTextSize(12);
            tile.addView(dt);
        }
        tile.setOnClickListener(v -> {
            prefs.edit().putInt("clockstyle", idx).apply();
            applyClockStyleV13();
            d.dismiss();
        });
        return tile;
    }

    private void showD(Dialog d) {
        d.show();
        FontKit.apply(this, d.getWindow().getDecorView(), FontKit.ALL);
    }

    private void applyAppFont() {
        for (int i = 0; i < rootFrame.getChildCount(); i++) {
            View ch = rootFrame.getChildAt(i);
            boolean overlay = (ch == letterOverlay || ch == searchOverlay);
            FontKit.apply(this, ch, overlay ? FontKit.ALL : FontKit.HOME);
        }
        FontKit.apply(this, letterOverlay, FontKit.ALL);
    }

    private void showFontPicker() {
        final Dialog d = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFFEBEBEB);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(8), dp(16), dp(32));
        TextView title = new TextView(this);
        title.setText("Font style");
        title.setTextSize(28);
        title.setTextColor(0xFF777777);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(48), 0, dp(8));
        col.addView(title);
        TextView hint = new TextView(this);
        hint.setText("Tap a font to use it. Settings > Apply font to decides where it shows.");
        hint.setTextSize(14);
        hint.setTextColor(0xFF777777);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(8), 0, dp(8), dp(16));
        col.addView(hint);
        final String cur = FontKit.chosenFile(this);
        List<String[]> items = new ArrayList<String[]>();
        items.add(new String[] { "System default", "" });
        items.addAll(FontKit.available(this));
        for (int i = 0; i < items.size(); i++) {
            final String[] it = items.get(i);
            TextView row = new TextView(this);
            boolean on = it[1].equals(cur);
            row.setText((on ? "\u2713  " : "") + it[0] + "\n05:00  Thu 12 Nov  \u0928\u092E\u0938\u094D\u0924\u0947");
            Typeface tf = FontKit.load(this, it[1]);
            if (tf != null) row.setTypeface(tf);
            row.setTextSize(20);
            row.setTextColor(0xFF111111);
            row.setPadding(dp(18), dp(14), dp(18), dp(14));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(16));
            g.setColor(on ? 0xFFB2DFDB : 0xFFFFFFFF);
            row.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(6), 0, dp(6));
            row.setOnClickListener(v -> {
                prefs.edit().putString("tinkufont", it[1]).apply();
                d.dismiss();
                recreate();
            });
            col.addView(row, lp);
        }
        sv.addView(col);
        d.setContentView(sv);
        d.show();
    }

    private void showClockStyles() {
        final Dialog d = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setBackgroundColor(0xFFEBEBEB);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), dp(8), dp(8), dp(24));
        TextView title = new TextView(this);
        title.setText("Clock style");
        title.setTextSize(30);
        title.setTextColor(0xFF777777);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(56), 0, dp(24));
        col.addView(title);
        int cur = prefs.getInt("clockstyle", 36);
        for (int i = 0; i < ClockText.COUNT; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int k = 0; k < 2; k++) {
                int idx = i + k;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(150), 1f);
                lp.setMargins(dp(5), dp(5), dp(5), dp(5));
                row.addView(styleTile(idx, idx == cur, d), lp);
            }
            col.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        sv.addView(col);
        d.setContentView(sv);
        showD(d);
        d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    // ---------- Lock screen setup ----------
    private void showLockSettings() {
        lockHandler.removeCallbacks(idleLockRun);
        final Dialog d = new Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar);
        d.setOnDismissListener(x -> scheduleIdleLock());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LockView pv = new LockView(this);
        pv.setPrefs(prefs);
        pv.setPreview(true);
        root.addView(pv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout box = sheetBox("Lock screen setup");
        addSwitchRow(box, "Lock screen on", "lockscreen", () -> {});
        addTapRow(box, "Background photo", "Choose from gallery", () -> pickLockBg());
        addTapRow(box, "Remove background photo", "Back to plain colour", () -> removeLockBg());
        addTapRow(box, "Background colour", "Used when no photo is set",
            () -> showColorPicker("Lock background colour", "lock_bgcolor", () -> {}));
        addChoiceRow(box, "Show lock screen after screen off", "lock_delay", "0",
            new String[] { "0", "5", "10", "15", "30", "60", "300", "1800", "-1" },
            new String[] { "Immediately", "After 5 seconds", "After 10 seconds", "After 15 seconds",
                "After 30 seconds", "After 1 minute",
                "After 5 minutes", "After 30 minutes", "Never (only at launch)" },
            () -> {});
        addChoiceRow(box, "Lock after idle on home", "lock_idle", "0",
            new String[] { "0", "5", "10", "30", "60", "120", "300" },
            new String[] { "Off", "5 seconds", "10 seconds", "30 seconds", "1 minute", "2 minutes", "5 minutes" },
            () -> scheduleIdleLock());
        addSeekRow(box, "Clock: up / down", "lock_cy", 0.20f, 0.70f, 0.44f, null);
        addSeekRow(box, "Clock: left / right", "lock_dx", -0.25f, 0.25f, 0f, null);
        addSeekRow(box, "Clock size", "lock_scale", 0.70f, 1.30f, 1f, null);
        addSeekRow(box, "Battery: up / down", "lock_by", 0.70f, 0.95f, 0.86f, null);
        addSeekRow(box, "Battery: size", "lock_bscale", 0.70f, 2.00f, 1f, null);
        addSeekRow(box, "Battery: left / right", "lock_bdx", -0.40f, 0.40f, 0f, null);
        addSeekRow(box, "Date and day: size", "lock_dscale", 0.70f, 1.80f, 1f, null);
        addSeekRow(box, "Date and day: left / right", "lock_ddx", -0.30f, 0.30f, 0f, null);
        addSwitchRow(box, "Show date and day", "lock_date", () -> {});
        addSwitchRow(box, "Show seconds wheel", "lock_sec", () -> {});
        addSwitchRow(box, "Show battery", "lock_batt", () -> {});

        TextView reset = makeButton("Reset position and size", 0xFF00695C, 0xFFD7E8E5);
        reset.setOnClickListener(v -> {
            prefs.edit().remove("lock_cy").remove("lock_dx").remove("lock_scale").remove("lock_by")
                .remove("lock_bscale").remove("lock_bdx").remove("lock_dscale").remove("lock_ddx").apply();
            d.dismiss();
            showLockSettings();
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(16);
        box.addView(reset, rlp);

        TextView done = makeButton("Done", 0xFFFFFFFF, 0xFF00695C);
        done.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(8);
        box.addView(done, dlp);

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(box);
        int sh = (int) (getResources().getDisplayMetrics().heightPixels * 0.5f);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, sh));

        d.setContentView(root);
        showD(d);
        d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    // ===== Step 11: Tinku voice assistant =====
    // The mic is open ONLY while: (a) push-to-talk (long-press the search button),
    // or (b) the "tinkulisten" switch is on AND the launcher is on screen.
    // onPause always closes it.

    private static final int REQ_MIC = 311;
    private Tinku tinku;
    private TextView tinkuStatus;
    private boolean tinkuResumed = false;
    private int tinkuMode = 0;            // 0 mic closed, 1 push-to-talk, 2 always listening
    private int tinkuAskedFor = 0;
    private Runnable tinkuPending;
    private String tinkuPendingSig = "";
    private long tinkuAwakeUntil = 0;     // after a bare "Tinku", the next sentence needs no wake word
    private final Handler tinkuH = new Handler(Looper.getMainLooper());
    private final Runnable tinkuRestore = new Runnable() {
        @Override
        public void run() {
            if (tinku != null && tinku.isActive()) {
                tinkuStatus.setText(tinkuMode == 2 ? "● Tinku" : "● Listening…");
            } else {
                tinkuStatus.setVisibility(View.GONE);
            }
        }
    };

    private void setupTinku() {
        if (!prefs.getBoolean("tl_v17", false)) prefs.edit().putBoolean("tinkulisten", false).putBoolean("tl_v17", true).apply();
        tinkuStatus = new TextView(this);
        tinkuStatus.setTextColor(0xFFFFFFFF);
        tinkuStatus.setTextSize(15);
        tinkuStatus.setMaxLines(4);
        tinkuStatus.setEllipsize(TextUtils.TruncateAt.END);
        tinkuStatus.setGravity(Gravity.CENTER);
        tinkuStatus.setPadding(dp(16), dp(9), dp(16), dp(9));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xD9000000);
        pill.setCornerRadius(dp(22));
        tinkuStatus.setBackground(pill);
        tinkuStatus.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = dp(52);
        lp.leftMargin = dp(24);
        lp.rightMargin = dp(24);
        rootFrame.addView(tinkuStatus, lp);
        tinkuSetupDot();

        tinku = new Tinku(this, new Tinku.Listener() {
            @Override
            public void onPartial(ArrayList<String> alts) {
                tinkuHeard(alts, false);
            }

            @Override
            public void onFinal(ArrayList<String> alts) {
                tinkuHeard(alts, true);
            }

            @Override
            public void onState(boolean listening) {
                if (listening) {
                    tinkuShow(tinkuMode == 2 ? "● Tinku" : "● Listening…", 0);
                } else {
                    tinkuMode = 0;
                    tinkuCancelPending();
                    tinkuH.removeCallbacks(tinkuRestore);
                    tinkuH.postDelayed(tinkuRestore, 1800);
                    // Push-to-talk ended: go back to always-listening if that switch is on.
                    tinkuH.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tinkuApplyListen();
                        }
                    }, 800);
                }
            }

            @Override
            public void onProblem(String message, boolean fatal) {
                if (fatal && !message.startsWith("Internet")) prefs.edit().putBoolean("tinkulisten", false).apply();
                tinkuShow(message, 3500);
            }
        });

        searchButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                tinkuStartPtt();
                return true;
            }
        });
    }

    private String tinkuLang() {
        return prefs.getString("tinkulang", "ne-NP");
    }

    private boolean tinkuHasMic() {
        return Build.VERSION.SDK_INT < 23
            || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void tinkuAskMic(int forWhat) {
        tinkuAskedFor = forWhat;
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQ_MIC);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req != REQ_MIC) return;
        boolean ok = res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED;
        int what = tinkuAskedFor;
        tinkuAskedFor = 0;
        if (!ok) {
            if (what == 2) prefs.edit().putBoolean("tinkulisten", false).apply();
            Toast.makeText(this, "Microphone permission denied. Tinku cannot listen.",
                Toast.LENGTH_LONG).show();
            return;
        }
        if (what == 1) tinkuStartPtt(); else tinkuApplyListen();
    }

    /** Make the mic match the settings. Safe to call any time. */
    private void tinkuApplyListen() {
        if (tinku == null || !tinkuResumed) return;
        if (tinkuSpeaking) return;
        boolean want = prefs.getBoolean("tinkulisten", false);
        if (!want) {
            if (tinku.isContinuous()) tinku.stop();
            return;
        }
        if (tinku.isActive()) return;
        if (!Tinku.available(this)) {
            prefs.edit().putBoolean("tinkulisten", false).apply();
            Toast.makeText(this, "Voice service not available on this phone (needs Google speech services)",
                Toast.LENGTH_LONG).show();
            return;
        }
        if (!tinkuHasMic()) {
            tinkuAskMic(2);
            return;
        }
        tinkuMode = 2;
        tinku.start(true, tinkuLang());
    }

    private void tinkuStartPtt() {
        if (tinku == null) return;
        if (tinkuMode == 1 && tinku.isActive()) {
            tinku.stop();
            return;
        }
        if (!Tinku.available(this)) {
            Toast.makeText(this, "Voice service not available on this phone (needs Google speech services)",
                Toast.LENGTH_LONG).show();
            return;
        }
        if (!tinkuHasMic()) {
            tinkuAskMic(1);
            return;
        }
        tinkuSpeakStop();
        tinkuCancelPending();
        tinkuMode = 1;
        tinku.start(false, tinkuLang());
    }

    private void tinkuPause() {
        tinkuResumed = false;
        tinkuSpeakStop();
        tinkuCancelPending();
        if (tinku != null) tinku.stop();
        tinkuMode = 0;
        tinkuAwakeUntil = 0;
        if (tinkuStatus != null) tinkuStatus.setVisibility(View.GONE);
    }

    // ===== Step 18: Tinku mic indicator (green = on, white = off) =====
    private View tinkuDot;
    private boolean tinkuDotOn = false;
    private final Runnable tinkuDotRun = new Runnable() {
        @Override
        public void run() {
            boolean on = tinku != null && tinku.isActive();
            if (tinkuDot != null && on != tinkuDotOn) {
                tinkuDotOn = on;
                tinkuDotPaint();
            }
            tinkuH.postDelayed(this, 300);
        }
    };

    private void tinkuDotPaint() {
        if (tinkuDot == null) return;
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(tinkuDotOn ? 0xFF2ECC71 : 0xFFFFFFFF);
        g.setStroke(dp(1), tinkuDotOn ? 0xFF1E8449 : 0x66000000);
        tinkuDot.setBackground(g);
    }

    private void tinkuSetupDot() {
        tinkuDot = new View(this);
        tinkuDotPaint();
        tinkuDot.setOnClickListener(v -> tinkuStartPtt());
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
            dp(16), dp(16), Gravity.TOP | Gravity.RIGHT);
        dlp.topMargin = dp(58);
        dlp.rightMargin = dp(20);
        rootFrame.addView(tinkuDot, dlp);
        tinkuH.removeCallbacks(tinkuDotRun);
        tinkuH.post(tinkuDotRun);
    }
    private void tinkuLangChanged() {
        if (tinku != null && tinku.isActive()) tinku.start(tinkuMode == 2, tinkuLang());
    }

    private void tinkuShow(String msg, int holdMs) {
        if (tinkuStatus == null) return;
        tinkuStatus.setText(msg);
        tinkuStatus.setVisibility(View.VISIBLE);
        tinkuH.removeCallbacks(tinkuRestore);
        if (holdMs > 0) tinkuH.postDelayed(tinkuRestore, holdMs);
    }

    private void tinkuCancelPending() {
        if (tinkuPending != null) tinkuH.removeCallbacks(tinkuPending);
        tinkuPending = null;
        tinkuPendingSig = "";
    }

    private List<String> tinkuLabels() {
        List<String> l = new ArrayList<String>();
        for (int i = 0; i < installedApps.size(); i++) l.add(labelOf(installedApps.get(i)));
        return l;
    }

    private void tinkuHeard(ArrayList<String> alts, boolean fin) {
        final TinkuBrain.Cmd c = TinkuBrain.parse(alts, tinkuLabels(),
            tinkuMode == 2 && android.os.SystemClock.uptimeMillis() > tinkuAwakeUntil);
        if (c == null) return;                          // not for Tinku
        if (!fin) {
            if (tinkuMode == 1 || c.type != TinkuBrain.NONE) {
                String top = alts.get(0);
                if (top.length() > 60) top = top.substring(0, 60) + "…";
                tinkuShow("● " + top, 0);
            }
            if (c.sure && c.type != TinkuBrain.NONE) {
                String sig = c.sig();
                if (!sig.equals(tinkuPendingSig)) {
                    tinkuCancelPending();
                    tinkuPendingSig = sig;
                    tinkuPending = new Runnable() {
                        @Override
                        public void run() {
                            tinkuPending = null;
                            tinkuPendingSig = "";
                            if (tinku != null) tinku.consumed();
                            tinkuRun(c);
                        }
                    };
                    tinkuH.postDelayed(tinkuPending, 450);
                }
            } else {
                tinkuCancelPending();
            }
            return;
        }
        tinkuCancelPending();
        tinkuAwakeUntil = c.bareWake ? android.os.SystemClock.uptimeMillis() + 7000 : 0;
        tinkuRun(c);
    }

    private void tinkuRun(TinkuBrain.Cmd c) {
        if (tinkuPreRun(c)) return;
        String say = c.say;
        switch (c.type) {
            case TinkuBrain.OPEN_APP:
                if (c.app >= 0 && c.app < installedApps.size()) {
                    if (searchOverlay.getVisibility() == View.VISIBLE) hideSearch();
                    launchApp(installedApps.get(c.app));
                }
                break;
            case TinkuBrain.TOGGLE: {
                boolean cur = prefs.getBoolean(c.key, "lockscreen".equals(c.key));
                boolean val = c.hasOnOff ? c.on : !cur;
                prefs.edit().putBoolean(c.key, val).apply();
                if ("mono".equals(c.key)) refreshAll();
                else if ("stacked".equals(c.key)) applyClockStyle();
                else if ("weather".equals(c.key)) { refreshInfo(); refreshWeather(false); }
                else if ("showbat".equals(c.key)) refreshInfo();
                say = c.say + (val ? " on" : " off");
                break;
            }
            case TinkuBrain.SET:
                prefs.edit().putString(c.key, c.value).apply();
                if ("favlayout".equals(c.key)) refreshFavorites();
                break;
            case TinkuBrain.FAV_ADD:
            case TinkuBrain.FAV_REMOVE:
                if (c.app >= 0 && c.app < installedApps.size()) {
                    String pkg = installedApps.get(c.app).activityInfo.packageName;
                    Set<String> favs = getFavorites();
                    if (c.type == TinkuBrain.FAV_REMOVE) {
                        favs.remove(pkg);
                        prefs.edit().remove("fpx:" + pkg).remove("fpy:" + pkg).apply();
                    } else {
                        favs.add(pkg);
                    }
                    saveFavorites(favs);
                    refreshFavorites();
                    refreshDrawerKeepScroll();
                }
                break;
            case TinkuBrain.HIDE:
                if (c.app >= 0 && c.app < installedApps.size()) {
                    Set<String> h = getHidden();
                    h.add(installedApps.get(c.app).activityInfo.packageName);
                    saveHidden(h);
                    refreshAll();
                }
                break;
            case TinkuBrain.SEARCH:
                showSearch();
                searchInput.setText(c.value);
                searchInput.setSelection(searchInput.getText().length());
                break;
            case TinkuBrain.NOTIF:
                expandNotifications();
                break;
            case TinkuBrain.SETTINGS:
                showSettings();
                break;
            case TinkuBrain.LOCK:
                showLockNow();
                break;
            case TinkuBrain.HIDDEN_LIST:
                showHiddenApps();
                break;
            case TinkuBrain.CLOSE:
                if (searchOverlay.getVisibility() == View.VISIBLE) hideSearch();
                hideOverlay();
                break;
            case TinkuBrain.SYSTEM:
                tinkuOpenSystem(c.key);
                break;
            case TinkuBrain.INFO:
                say = tinkuInfo(c.key);
                break;
            case TinkuBrain.STOP_LISTEN:
                prefs.edit().putBoolean("tinkulisten", false).apply();
                if (tinku != null) tinku.stop();
                break;
            default:
                break;
        }
        if (say != null && say.length() > 0) {
            tinkuShow(c.type == TinkuBrain.NONE ? say : "✓ " + say, 2500);
        }
        tinkuSpeakFor(c, say);
    }

    private String tinkuInfo(String key) {
        if ("battery".equals(key)) return "Battery " + batteryPercent() + "%";
        if ("weather".equals(key)) {
            String w = prefs.getString("wtext", null);
            return w != null ? w : "No weather yet. Turn on weather in Settings";
        }
        return new java.text.SimpleDateFormat("h:mm a · EEE, d MMM",
            java.util.Locale.getDefault()).format(new java.util.Date());
    }

    private void tinkuOpenSystem(String key) {
        String action;
        boolean q = Build.VERSION.SDK_INT >= 29;
        if ("wifi".equals(key)) action = q ? "android.settings.panel.action.INTERNET_CONNECTIVITY" : Settings.ACTION_WIFI_SETTINGS;
        else if ("bluetooth".equals(key)) action = Settings.ACTION_BLUETOOTH_SETTINGS;
        else if ("airplane".equals(key)) action = Settings.ACTION_AIRPLANE_MODE_SETTINGS;
        else if ("hotspot".equals(key)) action = "android.settings.TETHER_SETTINGS";
        else if ("location".equals(key)) action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        else if ("display".equals(key)) action = Settings.ACTION_DISPLAY_SETTINGS;
        else if ("sound".equals(key)) action = q ? "android.settings.panel.action.VOLUME" : Settings.ACTION_SOUND_SETTINGS;
        else action = Settings.ACTION_SETTINGS;
        try {
            Intent i = new Intent(action);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            try {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Throwable ignored) {
            }
        }
    }

    // ===== Step 12: Tinku bolera jawaf dinxa =====
    private TinkuSpeak tinkuSpeak;
    private boolean tinkuSpeaking = false;

    private boolean tinkuNe(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '\u0900' && ch <= '\u097F') return true;
        }
        return false;
    }

    /** Speak text. The mic is closed while Tinku talks, so it never hears itself. */
    private void tinkuSpeakOut(String text, boolean ne, final Runnable after) {
        if (tinkuSpeak == null) tinkuSpeak = new TinkuSpeak(this);
        tinkuSpeaking = true;
        if (tinku != null) tinku.stop();
        tinkuShow("Tinku: " + text, 6000);
        tinkuSpeak.speak(text, ne ? "ne" : "en", new Runnable() {
            @Override
            public void run() {
                tinkuSpeaking = false;
                if (after != null) after.run();
                if (tinkuResumed) tinkuApplyListen();
            }
        });
    }

    private void tinkuSpeakStop() {
        tinkuSpeaking = false;
        if (tinkuSpeak != null) tinkuSpeak.stop();
    }

    private void tinkuSpeakShutdown() {
        tinkuH.removeCallbacks(tinkuDotRun);
        tinkuSpeaking = false;
        if (tinkuSpeak != null) tinkuSpeak.shutdown();
        tinkuSpeak = null;
    }

    /** Handles "Tinku" alone and questions. true = fully handled here. */
    private boolean tinkuPreRun(final TinkuBrain.Cmd c) {
        final boolean ne = tinkuNe(c.heard);
        if (c.bareWake) {
            final boolean wasPtt = tinkuMode == 1;
            tinkuSpeakOut(ne ? "हजुर?" : "Yes?", ne, new Runnable() {
                @Override
                public void run() {
                    tinkuAwakeUntil = android.os.SystemClock.uptimeMillis() + 7000;
                    if (wasPtt && tinkuResumed) tinkuStartPtt();
                }
            });
            return true;
        }
        if (c.type != TinkuBrain.NONE) return false;
        String a = TinkuAnswer.answer(c.heard, ne);
        if (a != null) {
            tinkuSpeakOut(a, ne, null);
            return true;
        }
        final String q = TinkuAnswer.clean(c.heard);
        if (q.split("\\s+").length >= 3) {
            tinkuSpeakOut(ne ? "मैले यो थाहा पाइनँ, गुगलमा खोज्दै छु"
                : "I don't know that one. Searching Google", ne, new Runnable() {
                @Override
                public void run() {
                    tinkuWebSearch(q);
                }
            });
        } else {
            tinkuSpeakOut(ne ? "मैले बुझिनँ, फेरि भन्नुस्" : "Sorry, I didn't get that", ne, null);
        }
        return true;
    }

    private void tinkuWebSearch(String q) {
        try {
            Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
            i.putExtra(android.app.SearchManager.QUERY, q);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            try {
                Intent v = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.google.com/search?q=" + android.net.Uri.encode(q)));
                v.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(v);
            } catch (Throwable ignored) {
            }
        }
    }

    /** After a command ran: say the result out loud (not for commands that leave the launcher). */
    private void tinkuSpeakFor(TinkuBrain.Cmd c, String say) {
        boolean ne = tinkuNe(c.heard);
        String t = null;
        switch (c.type) {
            case TinkuBrain.INFO:
                if ("battery".equals(c.key)) {
                    t = ne ? "ब्याट्री " + batteryPercent() + " प्रतिशत छ"
                        : "Battery is " + batteryPercent() + " percent";
                } else if ("weather".equals(c.key)) {
                    String w = prefs.getString("wtext", null);
                    if (w == null) {
                        t = ne ? "मौसमको जानकारी छैन। सेटिङमा वेदर अन गर्नुस्"
                            : "No weather yet. Turn on weather in settings";
                    } else {
                        t = (ne ? "मौसम " : "Weather ") + w;
                    }
                } else {
                    t = TinkuAnswer.timeSpoken(ne);
                }
                break;
            case TinkuBrain.TOGGLE:
            case TinkuBrain.SET:
            case TinkuBrain.FAV_ADD:
            case TinkuBrain.FAV_REMOVE:
            case TinkuBrain.HIDE:
                t = ne ? "ठीक छ, भयो" : (say != null && say.length() > 0 ? say : "Done");
                break;
            default:
                break;
        }
        if (t != null) tinkuSpeakOut(t, ne, null);
    }
    // ===== Step 30: clock opacity, analog colours, text colour =====
    private SharedPreferences.OnSharedPreferenceChangeListener v30Listener;

    private void setupV30() {
        applyClockAlpha();
        applyAnalogColors();
        v30Listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences shared, String key) {
                if ("clk_alpha".equals(key)) applyClockAlpha();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(v30Listener);
    }

    private void applyClockAlpha() {
        if (clockText == null) return;
        float a = 1f;
        try {
            a = prefs.getFloat("clk_alpha", 1f);
        } catch (Exception e) {
            a = 1f;
        }
        clockText.setAlpha(Math.max(0.1f, Math.min(1f, a)));
    }

    private void applyAnalogColors() {
        if (clockText == null) return;
        clockText.setAnalogColors(prefs.getInt("clk_hands", 0), prefs.getInt("clk_nums", 0));
    }

    /** Colour chooser: first circle = Auto (follow the normal text colour). */
    private void showColorPicker(String title, final String key, final Runnable after) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(20), dp(16), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F1F1);
        bg.setCornerRadius(dp(24));
        box.setBackground(bg);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(20);
        t.setTextColor(0xFF111111);
        t.setPadding(dp(8), 0, 0, dp(10));
        box.addView(t);
        final int[] cols = { 0xFFFFFFFF, 0xFF000000, 0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047,
            0xFF00897B, 0xFF00ACC1, 0xFF1E88E5, 0xFF5E35B1, 0xFFD81B60, 0xFF9E9E9E };
        LinearLayout row = null;
        for (int i = 0; i < cols.length + 1; i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                box.addView(row);
            }
            final int c = i == 0 ? 0 : cols[i - 1];
            TextView sw = new TextView(this);
            sw.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i == 0 ? 0xFFE0E0E0 : c);
            g.setStroke(dp(2), 0x33000000);
            sw.setBackground(g);
            if (i == 0) {
                sw.setText("Auto");
                sw.setTextSize(12);
                sw.setTextColor(0xFF333333);
            }
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (c == 0) {
                        prefs.edit().remove(key).apply();
                    } else {
                        prefs.edit().putInt(key, c).apply();
                    }
                    d.dismiss();
                    if (after != null) after.run();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), dp(56));
            lp.setMargins(dp(8), dp(8), dp(8), dp(8));
            row.addView(sw, lp);
        }
        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.show();
    }
    // ===== Step 31: lock screen background (gallery photo / colour) =====
    private static final int REQ_LOCKBG = 3131;

    private void pickLockBg() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, REQ_LOCKBG);
        } catch (Throwable t) {
            Toast.makeText(this, "Gallery khulena", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeLockBg() {
        String old = prefs.getString("lock_bgfile", "");
        if (old.length() > 0) {
            try {
                new java.io.File(old).delete();
            } catch (Throwable ignored) {
            }
        }
        prefs.edit().remove("lock_bgfile").apply();
        if (lockView != null) lockView.invalidate();
        Toast.makeText(this, "Photo hatayo", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_LOCKBG || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            java.io.InputStream in = getContentResolver().openInputStream(data.getData());
            java.io.File out = new java.io.File(getFilesDir(), "lockbg_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(out);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
            fo.close();
            in.close();
            String old = prefs.getString("lock_bgfile", "");
            prefs.edit().putString("lock_bgfile", out.getAbsolutePath()).apply();
            if (old.length() > 0) new java.io.File(old).delete();
            if (lockView != null) lockView.invalidate();
            Toast.makeText(this, "Lock screen background set", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "Photo lina sakiyena", Toast.LENGTH_SHORT).show();
        }
    }
    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private String labelOf(ResolveInfo info) {
        return AppAdapter.labelOf(this, info);
    }

    private void loadApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        allApps = pm.queryIntentActivities(intent, 0);
        Collections.sort(allApps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return String.CASE_INSENSITIVE_ORDER.compare(labelOf(a), labelOf(b));
            }
        });
        Set<String> hidden = getHidden();
        installedApps = new ArrayList<ResolveInfo>();
        for (int i = 0; i < allApps.size(); i++) {
            ResolveInfo info = allApps.get(i);
            if (!hidden.contains(info.activityInfo.packageName)) {
                installedApps.add(info);
            }
        }
    }

    private void refreshAll() {
        int first = listViewLetter.getFirstVisiblePosition();
        View v0 = listViewLetter.getChildAt(0);
        int top = (v0 == null) ? 0 : v0.getTop();
        loadApps();
        buildAlphabetBar();
        refreshFavorites();
        if (isDrawerOpen()) {
            listViewLetter.setSelectionFromTop(first, top);
        }
        if (searchOverlay.getVisibility() == View.VISIBLE) {
            filterSearch(searchInput.getText().toString());
        }
    }

    private String keyOf(ResolveInfo info) {
        String label = labelOf(info).trim();
        if (label.length() == 0) return OTHER;
        char c = Character.toUpperCase(label.charAt(0));
        if (c >= 'A' && c <= 'Z') return String.valueOf(c);
        return OTHER;
    }

    private Set<String> getFavorites() {
        return new HashSet<String>(prefs.getStringSet("favs", new HashSet<String>()));
    }

    private void saveFavorites(Set<String> favs) {
        prefs.edit().putStringSet("favs", favs).apply();
    }

    private Set<String> getHidden() {
        return new HashSet<String>(prefs.getStringSet("hidden", new HashSet<String>()));
    }

    private void saveHidden(Set<String> hidden) {
        prefs.edit().putStringSet("hidden", hidden).apply();
    }

    private boolean isLeft() {
        return "left".equals(prefs.getString("align", "left"));
    }

    private boolean isFavLeft() {
        return "left".equals(prefs.getString("favalign", "left"));
    }

    private boolean isFavLine() {
        return !"free".equals(prefs.getString("favlayout", "free"));
    }

    private boolean hasFavPos(String pkg) {
        return prefs.contains("fpx:" + pkg) && prefs.contains("fpy:" + pkg);
    }

    private void applyAlign() {
        int g = isLeft() ? Gravity.LEFT : Gravity.RIGHT;
        textClock.setGravity(g);
        textDate.setGravity(g);
        applyV8Align();
        refreshFavorites();
    }

    private void applyPos(final View v, final String kx, final String ky,
                          final float dx, final float dy) {
        rootFrame.post(new Runnable() {
            @Override
            public void run() {
                int w = rootFrame.getWidth();
                int h = rootFrame.getHeight();
                int vw = v.getWidth();
                int vh = v.getHeight();
                if (w == 0 || vw == 0) return;
                float fx = prefs.getFloat(kx, dx);
                float fy = prefs.getFloat(ky, dy);
                v.setX(fx * Math.max(0, w - vw));
                v.setY(fy * Math.max(0, h - vh));
            }
        });
    }

    private void applyClockPos() {
        applyPos(clockBox, "cx", "cy", 0.06f, 0.55f);
        rootFrame.post(new Runnable() {
            @Override
            public void run() {
                applyFavPos();
            }
        });
    }

    private void applyFavPos() {
        if ("grid".equals(prefs.getString("favlayout", "free"))) {
            rootFrame.post(new Runnable() {
                @Override
                public void run() {
                    if (clockBox.getWidth() == 0) return;
                    favoritesBox.setX(clockBox.getX());
                    favoritesBox.setY(clockBox.getY() + clockBox.getHeight() + dp(14));
                }
            });
            return;
        }
        applyPos(favoritesBox, "fx", "fy", 0.06f, 0.92f);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (dragMode && dragTarget != null) {
            handleDrag(e);
            return true;
        }
        if (gestures != null) gestures.onTouchEvent(e);
        return super.dispatchTouchEvent(e);
    }

    private void handleDrag(MotionEvent e) {
        int a = e.getActionMasked();
        int[] loc = new int[2];
        rootFrame.getLocationOnScreen(loc);
        int w = rootFrame.getWidth();
        int h = rootFrame.getHeight();
        int vw = dragTarget.getWidth();
        int vh = dragTarget.getHeight();
        float nx = e.getRawX() - loc[0] - vw / 2f;
        float ny = e.getRawY() - loc[1] - vh / 2f;
        nx = Math.max(0, Math.min(nx, w - vw));
        ny = Math.max(0, Math.min(ny, h - vh));
        dragTarget.setX(nx);
        dragTarget.setY(ny);
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            float fx = (w - vw) > 0 ? nx / (w - vw) : 0f;
            float fy = (h - vh) > 0 ? ny / (h - vh) : 0f;
            prefs.edit().putFloat(dragKx, fx).putFloat(dragKy, fy).apply();
            dragMode = false;
            dragTarget = null;
            Toast.makeText(this, "Position saved", Toast.LENGTH_SHORT).show();
            applyFavPos();
            if (dragRefreshFav) {
                dragRefreshFav = false;
                refreshFavorites();
            }
        }
    }

    private void startDrag(View target, String kx, String ky) {
        if (target.getWidth() == 0) {
            Toast.makeText(this, "Nothing to move yet. Add a favourite first.", Toast.LENGTH_LONG).show();
            return;
        }
        dragRefreshFav = false;
        dragTarget = target;
        dragKx = kx;
        dragKy = ky;
        dragMode = true;
        Toast.makeText(this, "Touch anywhere on the screen, then lift your finger", Toast.LENGTH_LONG).show();
    }

    private void startDragFav(ResolveInfo info, View row) {
        if (isFavLine()) {
            startDrag(favoritesBox, "fx", "fy");
            return;
        }
        String pkg = info.activityInfo.packageName;
        String kx = "fpx:" + pkg;
        String ky = "fpy:" + pkg;
        if (row.getParent() == favoritesBox) {
            int[] rl = new int[2];
            int[] fl = new int[2];
            row.getLocationOnScreen(rl);
            rootFrame.getLocationOnScreen(fl);
            int w = rootFrame.getWidth();
            int h = rootFrame.getHeight();
            int vw = row.getWidth();
            int vh = row.getHeight();
            float nx = rl[0] - fl[0];
            float ny = rl[1] - fl[1];
            float fx = (w - vw) > 0 ? nx / (w - vw) : 0f;
            float fy = (h - vh) > 0 ? ny / (h - vh) : 0f;
            prefs.edit().putFloat(kx, fx).putFloat(ky, fy).apply();
            favoritesBox.removeView(row);
            addFloatingFav(row, pkg);
            row.setTranslationX(nx);
            row.setTranslationY(ny);
        }
        startDrag(row, kx, ky);
        if (dragMode) dragRefreshFav = true;
    }

    private void addFloatingFav(final View row, final String pkg) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.LEFT);
        int idx = rootFrame.indexOfChild(alphabetBar);
        rootFrame.addView(row, idx < 0 ? rootFrame.getChildCount() : idx, lp);
        floatingFavs.add(row);
        row.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int oldL, int oldT, int oldR, int oldB) {
                if ((r - l) != (oldR - oldL) || (b - t) != (oldB - oldT)) {
                    applyPos(row, "fpx:" + pkg, "fpy:" + pkg, 0.06f, 0.5f);
                }
            }
        });
    }

    private void showClockMenu() {
        showSheet(null, "Clock",
            new String[] {
                "Move clock (drag anywhere)",
                "Align text left",
                "Align text right",
                "Reset clock position"
            },
            new int[] {
                android.R.drawable.ic_menu_mylocation,
                0,
                0,
                android.R.drawable.ic_menu_revert
            },
            new Runnable[] {
                () -> startDrag(clockBox, "cx", "cy"),
                () -> { prefs.edit().putString("align", "left").apply(); applyAlign(); },
                () -> { prefs.edit().putString("align", "right").apply(); applyAlign(); },
                () -> { prefs.edit().remove("cx").remove("cy").apply(); applyClockPos(); }
            });
    }

    private void addSwitchRow(LinearLayout box, String label, final String key, final Runnable onChange) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF111111);
        tv.setTextSize(18);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key, false));
        sw.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(key, on).apply();
            onChange.run();
        });
        row.addView(sw);
        box.addView(row, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void styleChips(List<TextView> chips, String[] values, String key, String def) {
        String cur = prefs.getString(key, def);
        for (int i = 0; i < chips.size(); i++) {
            boolean on = values[i].equals(cur);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(20));
            g.setColor(on ? 0xFF00796B : 0xFFDDDDDD);
            chips.get(i).setBackground(g);
            chips.get(i).setTextColor(on ? 0xFFFFFFFF : 0xFF111111);
        }
    }

    private void addChoiceRow(LinearLayout box, String label, final String key, final String def,
                              final String[] values, String[] names, final Runnable onChange) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF111111);
        tv.setTextSize(18);
        tv.setPadding(0, dp(12), 0, dp(8));
        box.addView(tv);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final List<TextView> chips = new ArrayList<TextView>();
        for (int i = 0; i < values.length; i++) {
            final String v = values[i];
            TextView c = new TextView(this);
            c.setText(names[i]);
            c.setTextSize(15);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(10), dp(10), dp(10), dp(10));
            c.setOnClickListener(x -> {
                prefs.edit().putString(key, v).apply();
                styleChips(chips, values, key, def);
                onChange.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = dp(8);
            row.addView(c, lp);
            chips.add(c);
        }
        styleChips(chips, values, key, def);
        box.addView(row, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ---------- settings: main list of groups, each group opens its own sheet ----------
    private LinearLayout settingsBox(String title, final Dialog d, boolean back) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(20), dp(24), dp(28));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F1F1);
        float r = dp(28);
        bg.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        box.setBackground(bg);
        TextView ht = new TextView(this);
        ht.setText(back ? "\u2039  " + title : title);
        ht.setTextColor(0xFF111111);
        ht.setTextSize(24);
        ht.setPadding(0, 0, 0, dp(12));
        if (back) {
            ht.setOnClickListener(v -> {
                d.dismiss();
                showSettings();
            });
        }
        box.addView(ht);
        return box;
    }

    private void showSettingsSheet(Dialog d, LinearLayout box) {
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        d.setContentView(sv);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        showD(d);
    }

    private void addSettingsCard(LinearLayout box, String icon, String title, String sub, final Runnable r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(18));
        g.setColor(0xFFFFFFFF);
        row.setBackground(g);

        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(20);
        ic.setGravity(Gravity.CENTER);
        GradientDrawable cg = new GradientDrawable();
        cg.setShape(GradientDrawable.OVAL);
        cg.setColor(0xFF00796B);
        ic.setBackground(cg);
        row.addView(ic, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, dp(8), 0);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFF111111);
        t.setTextSize(18);
        tx.addView(t);
        TextView st = new TextView(this);
        st.setText(sub);
        st.setTextColor(0xFF666666);
        st.setTextSize(13);
        tx.addView(st);
        row.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView ch = new TextView(this);
        ch.setText("\u203A");
        ch.setTextSize(26);
        ch.setTextColor(0xFF888888);
        row.addView(ch);

        row.setOnClickListener(v -> r.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        box.addView(row, lp);
    }

    private void showSettings() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = settingsBox("Settings", d, false);
        addSettingsCard(box, "\uD83C\uDFA8", "Theme", "Icons, font style, text colour",
            () -> { d.dismiss(); showThemeSettings(); });
        addSettingsCard(box, "\uD83C\uDFE0", "Home screen layout", "Favourites, app list glass, gestures",
            () -> { d.dismiss(); showLayoutSettings(); });
        addSettingsCard(box, "\uD83D\uDD52", "Date & Clock", "Clock, weather, battery",
            () -> { d.dismiss(); showDateClockSettings(); });
        addSettingsCard(box, "\uD83D\uDD12", "Lock screen setup", "Timer, position, date, battery",
            () -> { d.dismiss(); showLockSettings(); });
        addSettingsCard(box, "\uD83C\uDF99", "Tinku voice assistant", "Listen for \"Tinku\", mic",
            () -> { d.dismiss(); showVoiceSettings(); });
        addSettingsCard(box, "ℹ", "About this app", "Version, features, source",
            () -> { d.dismiss(); showAbout(); });
        showSettingsSheet(d, box);
    }

    // ===== About this app: version ra features =====
    private void showAbout() {
        String ver = "?";
        String code = "?";
        try {
            android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            ver = pi.versionName;
            code = Build.VERSION.SDK_INT >= 28
                ? String.valueOf(pi.getLongVersionCode())
                : String.valueOf(pi.versionCode);
        } catch (Exception e) {
            // version padhna sakiyena
        }
        String msg = "Tinku Glass Launcher\n\n"
            + "Version: " + ver + " (build " + code + ")\n"
            + "Package: " + getPackageName() + "\n\n"
            + "Features:\n"
            + "- WELCOME header, time-based greeting\n"
            + "- Favourites ra Search Now bar\n"
            + "- Presets: Monochrome, Minimal, Full\n"
            + "- Music card (play/pause)\n"
            + "- Drawer folders (Social, Media, Settings...)\n"
            + "- Charge voice alerts (customizable)\n\n"
            + "Source: github.com/devkumarmukhiya-ship-it/TinkuGlassLauncher";
        new android.app.AlertDialog.Builder(this)
            .setTitle("About this app")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show();
    }

    private void showThemeSettings() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = settingsBox("Theme", d, true);
        addTapRow(box, "Presets", "Monochrome, Minimal, Full", () -> { d.dismiss(); showPresets(); });
        addSwitchRow(box, "Monochrome icons", "mono", () -> refreshAll());
        addTapRow(box, "Font style", FontKit.label(this), () -> { d.dismiss(); showFontPicker(); });
        addChoiceRow(box, "Apply font to", "fontscope", "clock",
            new String[] { "clock", "home", "all" },
            new String[] { "Clock only", "Home screen", "Whole launcher" },
            () -> { d.dismiss(); recreate(); });
        addSwitchRow(box, "Dark text on home (light wallpaper)", "darktext", () -> applyTextTone());
        addTapRow(box, "Text colour", "Clock, date, weather, favourites",
            () -> { d.dismiss(); showColorPicker("Text colour", "home_color", () -> applyTextTone()); });
        showSettingsSheet(d, box);
    }

    private void showLayoutSettings() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = settingsBox("Home screen layout", d, true);
        final String mPos = prefs.getString("media_pos", "top");
        final String mSide = prefs.getString("media_side", "right");
        addTapRow(box, "Music card jaga", "Abhi: " + mPos + " (tap garera badlaunus)", () -> {
            String next = "top".equals(mPos) ? "middle" : ("middle".equals(mPos) ? "bottom" : "top");
            prefs.edit().putString("media_pos", next).apply();
            applyMediaPos();
            d.dismiss();
            showLayoutSettings();
        });
        addTapRow(box, "Music card side", "Abhi: " + mSide + " (tap garera badlaunus)", () -> {
            prefs.edit().putString("media_side", "left".equals(mSide) ? "right" : "left").apply();
            applyMediaPos();
            d.dismiss();
            showLayoutSettings();
        });
        addChoiceRow(box, "Favourites layout", "favlayout", "free",
            new String[] { "free", "vertical", "grid" },
            new String[] { "Free", "Vertical line", "Grid" },
            () -> refreshFavorites());
        addChoiceRow(box, "Favourites position", "favpos", "bottom",
            new String[] { "top", "middle", "bottom" },
            new String[] { "Top", "Middle", "Bottom" },
            () -> {
                String fp = prefs.getString("favpos", "bottom");
                float fyv = "top".equals(fp) ? 0.12f : ("middle".equals(fp) ? 0.5f : 0.92f);
                prefs.edit().putFloat("fy", fyv).apply();
                applyFavPos();
            });
        addChoiceRow(box, "Grid: columns (across)", "gridcols", "3",
            new String[] { "2", "3" },
            new String[] { "2", "3" },
            () -> refreshFavorites());
        addChoiceRow(box, "Grid: rows (down)", "gridrows", "3",
            new String[] { "2", "3", "4" },
            new String[] { "2", "3", "4" },
            () -> refreshFavorites());
        addChoiceRow(box, "App list glass", "glass", "clear",
            new String[] { "clear", "light", "dark" },
            new String[] { "Clear", "Light", "Dark" },
            () -> {});
        addChoiceRow(box, "Blur strength", "blurstr", "medium",
            new String[] { "soft", "medium", "strong" },
            new String[] { "Soft", "Medium", "Strong" },
            () -> {});
        addSwitchRow(box, "Swipe gestures (down = notifications, up = search)", "gestures", () -> {});

        boolean blurOk = false;
        if (Build.VERSION.SDK_INT >= 31) {
            WindowManager wmx = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            blurOk = wmx != null && wmx.isCrossWindowBlurEnabled();
        }
        TextView blurInfo = new TextView(this);
        blurInfo.setText("Blur on this phone: " + (blurOk ? "working" : "not available")
            + " (Android " + Build.VERSION.RELEASE + ")");
        blurInfo.setTextColor(0xFF555555);
        blurInfo.setTextSize(14);
        blurInfo.setPadding(0, dp(14), 0, 0);
        box.addView(blurInfo);
        showSettingsSheet(d, box);
    }

    private void showDateClockSettings() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = settingsBox("Date & Clock", d, true);
        addSwitchRow(box, "Stacked clock (hour over minute)", "stacked", () -> applyClockStyle());
        addTapRow(box, "Clock style", "Digital and analog styles", () -> { d.dismiss(); showClockStyles(); });
        addSeekRow(box, "Clock opacity", "clk_alpha", 0.15f, 1f, 1f, null);
        addTapRow(box, "Analog clock: number colour", "Auto = follows text colour",
            () -> { d.dismiss(); showColorPicker("Number colour", "clk_nums", () -> applyAnalogColors()); });
        addTapRow(box, "Analog clock: hands colour", "Hour and minute hands",
            () -> { d.dismiss(); showColorPicker("Hands colour", "clk_hands", () -> applyAnalogColors()); });
        addSwitchRow(box, "Show weather under the date", "weather", () -> {
            refreshInfo();
            refreshWeather(false);
        });
        TextView cityRow = new TextView(this);
        cityRow.setText("Weather city: " + prefs.getString("wcity", "Kathmandu") + "  \u203A");
        cityRow.setTextColor(0xFF111111);
        cityRow.setTextSize(18);
        cityRow.setPadding(0, dp(12), 0, dp(12));
        cityRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                showCityDialog();
            }
        });
        box.addView(cityRow);
        addChoiceRow(box, "Temperature unit", "wunit", "c",
            new String[] { "c", "f" },
            new String[] { "\u00B0C", "\u00B0F" },
            () -> {
                prefs.edit().remove("wtext").remove("wtime").apply();
                refreshInfo();
                refreshWeather(true);
            });
        addSwitchRow(box, "Show battery % under the date", "showbat", () -> refreshInfo());
        showSettingsSheet(d, box);
    }

    private void showVoiceSettings() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = settingsBox("Tinku voice assistant", d, true);
        addSwitchRow(box, "Listen for \"Tinku\" (mic ON only while launcher is open)", "tinkulisten",
            () -> tinkuApplyListen());
        addSwitchRow(box, "Charge voice alerts (charger lagda/hatda ra percent ma bolne)", "charge_sound", () -> {});
        addTapRow(box, "Charge alert settings", "Kun percent ma k bolne, message set garnus", () -> {
            d.dismiss();
            showChargeEditor();
        });
        showSettingsSheet(d, box);
    }

    private void showHomeMenu() {
        showWidgetSheet();
        if (true) return;
        showSheet(null, "Home screen",
            new String[] {
                "Settings",
                "Show lock screen",
                "Favourites: icon on left",
                "Favourites: icon on right",
                "Reset favourites positions",
                "Hidden apps"
            },
            new int[] {
                android.R.drawable.ic_menu_preferences,
                android.R.drawable.ic_lock_idle_lock,
                0,
                0,
                android.R.drawable.ic_menu_revert,
                android.R.drawable.ic_menu_view
            },
            new Runnable[] {
                () -> showSettings(),
                () -> showLockNow(),
                () -> { prefs.edit().putString("favalign", "left").apply(); refreshFavorites(); },
                () -> { prefs.edit().putString("favalign", "right").apply(); refreshFavorites(); },
                () -> {
                    SharedPreferences.Editor ed = prefs.edit();
                    for (String k : prefs.getAll().keySet()) {
                        if (k.startsWith("fpx:") || k.startsWith("fpy:")) ed.remove(k);
                    }
                    ed.remove("fx").remove("fy");
                    ed.apply();
                    refreshFavorites();
                    applyFavPos();
                },
                () -> showHiddenApps()
            });
    }

    private void showHiddenApps() {
        Set<String> hidden = getHidden();
        final List<ResolveInfo> list = new ArrayList<ResolveInfo>();
        for (int i = 0; i < allApps.size(); i++) {
            ResolveInfo info = allApps.get(i);
            if (hidden.contains(info.activityInfo.packageName)) {
                list.add(info);
            }
        }
        if (list.isEmpty()) {
            Toast.makeText(this, "No hidden apps", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[list.size()];
        Runnable[] acts = new Runnable[list.size()];
        for (int i = 0; i < list.size(); i++) {
            final String p = list.get(i).activityInfo.packageName;
            names[i] = labelOf(list.get(i));
            acts[i] = () -> {
                Set<String> h = getHidden();
                h.remove(p);
                saveHidden(h);
                refreshAll();
            };
        }
        showSheet(null, "Hidden apps (tap to show again)", names, null, acts);
    }

    private TextView makeButton(String text, int textColor, int bgColor) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(textColor);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(20), dp(12), dp(20), dp(12));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(24));
        g.setColor(bgColor);
        b.setBackground(g);
        return b;
    }

    private void showRename(final ResolveInfo info) {
        final String pkg = info.activityInfo.packageName;
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(22), dp(24), dp(24));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F1F1);
        float r = dp(28);
        bg.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        box.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("Rename app");
        title.setTextColor(0xFF111111);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, dp(14));
        box.addView(title);

        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(0xFF111111);
        input.setHintTextColor(0xFF777777);
        input.setTextSize(20);
        input.setText(labelOf(info));
        input.setSelectAllOnFocus(true);
        GradientDrawable ibg = new GradientDrawable();
        ibg.setColor(0xFFFFFFFF);
        ibg.setCornerRadius(dp(14));
        ibg.setStroke(dp(1), 0xFFB0B0B0);
        input.setBackground(ibg);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        box.addView(input, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.RIGHT);
        buttons.setPadding(0, dp(18), 0, 0);

        TextView cancel = makeButton("Cancel", 0xFF00695C, 0x00000000);
        TextView reset = makeButton("Reset", 0xFF00695C, 0x00000000);
        TextView save = makeButton("Save", 0xFFFFFFFF, 0xFF00796B);
        buttons.addView(cancel);
        buttons.addView(reset);
        buttons.addView(save);
        box.addView(buttons, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().remove("name:" + pkg).apply();
                d.dismiss();
                refreshAll();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = input.getText().toString().trim();
                if (t.length() == 0) {
                    prefs.edit().remove("name:" + pkg).apply();
                } else {
                    prefs.edit().putString("name:" + pkg, t).apply();
                }
                d.dismiss();
                refreshAll();
            }
        });

        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        showD(d);
        input.requestFocus();
    }

    private void refreshFavorites() {
        for (int i = 0; i < floatingFavs.size(); i++) {
            rootFrame.removeView(floatingFavs.get(i));
        }
        floatingFavs.clear();
        favoritesBox.removeAllViews();
        String layout = prefs.getString("favlayout", "free");
        if ("horizontal".equals(layout)) {
            layout = "grid";
            prefs.edit().putString("favlayout", "grid").apply();
        }
        final boolean line = !"free".equals(layout);
        final boolean horiz = "grid".equals(layout);
        favoritesBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout gridRow = null;
        int gridCount = 0;
        int gridCols = 3;
        int gridRowsMax = 3;
        try {
            gridCols = Integer.parseInt(prefs.getString("gridcols", "3"));
            gridRowsMax = Integer.parseInt(prefs.getString("gridrows", "3"));
        } catch (Exception ignored) {
        }
        if (gridCols < 2) gridCols = 2;
        if (gridCols > 3) gridCols = 3;
        if (gridRowsMax < 2) gridRowsMax = 2;
        if (gridRowsMax > 4) gridRowsMax = 4;
        final int gridLimit = gridCols * gridRowsMax;
        int rw = rootFrame.getWidth();
        if (rw <= 0) rw = getResources().getDisplayMetrics().widthPixels;
        final int cellW = Math.max(dp(56), ((int) (rw * 0.55f) - dp(16)) / gridCols);
        final int iconPx = Math.min(dp(52), (int) (cellW * 0.52f));
        Set<String> favs = getFavorites();
        boolean left = isFavLeft();
        for (int i = 0; i < installedApps.size(); i++) {
            final ResolveInfo info = installedApps.get(i);
            final String pkg = info.activityInfo.packageName;
            if (!favs.contains(pkg)) continue;

            final LinearLayout row = new LinearLayout(this);
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(AppAdapter.iconOf(this, info));
            int isz = horiz ? iconPx : dp(38);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(isz, isz);

            TextView tv = new TextView(this);
            tv.setText(labelOf(info));
            tv.setTextColor(0xFFFFFFFF);
            tv.setShadowLayer(6, 0, 0, 0x88000000);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);

            if (horiz) {
                row.setOrientation(LinearLayout.VERTICAL);
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                row.setPadding(dp(2), dp(8), dp(2), dp(8));
                tv.setTextSize(11);
                tv.setMaxWidth(cellW - dp(4));
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, dp(4), 0, 0);
                tv.setTextColor(homeTextColor());
                if (prefs.getBoolean("darktext", false)) tv.setShadowLayer(0f, 0f, 0f, 0);
                row.addView(iv, ilp);
                row.addView(tv);
            } else {
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL | (left ? Gravity.LEFT : Gravity.RIGHT));
                row.setPadding(0, dp(8), 0, dp(8));
                tv.setTextSize(13);
                tv.setShadowLayer(6f, 0f, 1f, 0x66000000);
                tv.setTextColor(homeTextColor());
                if (prefs.getBoolean("darktext", false)) tv.setShadowLayer(0f, 0f, 0f, 0);
                tv.setMaxWidth(dp(240));
                if (left) {
                    tv.setPadding(dp(16), 0, 0, 0);
                    row.addView(iv, ilp);
                    row.addView(tv);
                } else {
                    tv.setPadding(0, 0, dp(16), 0);
                    row.addView(tv);
                    row.addView(iv, ilp);
                }
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
                    showMenu(info, row);
                    return true;
                }
            });
            if (!line && hasFavPos(pkg)) {
                addFloatingFav(row, pkg);
            } else if (horiz) {
                if (gridCount >= gridLimit) continue;
                if (gridRow == null || gridCount % gridCols == 0) {
                    gridRow = new LinearLayout(this);
                    gridRow.setOrientation(LinearLayout.HORIZONTAL);
                    favoritesBox.addView(gridRow, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                }
                gridRow.addView(row, new LinearLayout.LayoutParams(
                    cellW, LinearLayout.LayoutParams.WRAP_CONTENT));
                gridCount++;
            } else {
                favoritesBox.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }
    }

    private void showSheet(Drawable icon, CharSequence title, String[] labels,
                           int[] icons, final Runnable[] actions) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(20), dp(24), dp(28));

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
                TextView tv = new TextView(this);
                tv.setText(labels[i]);
                tv.setTextColor(0xFF111111);
                tv.setTextSize(18);
                tv.setPadding(dp(20), 0, 0, 0);
                row.addView(tv);
            } else if (icons != null) {
                row.addView(new View(this), new LinearLayout.LayoutParams(dp(26), dp(26)));
                TextView tv = new TextView(this);
                tv.setText(labels[i]);
                tv.setTextColor(0xFF111111);
                tv.setTextSize(18);
                tv.setPadding(dp(20), 0, 0, 0);
                row.addView(tv);
            } else {
                TextView tv = new TextView(this);
                tv.setText(labels[i]);
                tv.setTextColor(0xFF111111);
                tv.setTextSize(18);
                row.addView(tv);
            }

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

        ScrollView sv = new ScrollView(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F1F1);
        float r = dp(28);
        bg.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        sv.setBackground(bg);
        sv.addView(box);

        d.setContentView(sv);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        showD(d);
    }

    private void showMenu(final ResolveInfo info) {
        showMenu(info, null);
    }

    private void showMenu(final ResolveInfo info, final View homeRow) {
        final String pkg = info.activityInfo.packageName;
        final boolean isFav = getFavorites().contains(pkg);
        List<String> labels = new ArrayList<String>();
        List<Integer> icons = new ArrayList<Integer>();
        List<Runnable> actions = new ArrayList<Runnable>();

        if (homeRow != null) {
            final boolean line = isFavLine();
            labels.add(line ? "Move all favourites" : "Drag anywhere");
            icons.add(android.R.drawable.ic_menu_mylocation);
            actions.add(() -> startDragFav(info, homeRow));
            if (line ? prefs.contains("fx") : hasFavPos(pkg)) {
                labels.add("Reset position");
                icons.add(android.R.drawable.ic_menu_revert);
                actions.add(() -> {
                    if (line) {
                        prefs.edit().remove("fx").remove("fy").apply();
                        applyFavPos();
                    } else {
                        prefs.edit().remove("fpx:" + pkg).remove("fpy:" + pkg).apply();
                        refreshFavorites();
                    }
                });
            }
        }

        labels.add(isFav ? "Remove from favourites" : "Favourite");
        icons.add(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        actions.add(() -> {
            Set<String> favs = getFavorites();
            if (isFav) {
                favs.remove(pkg);
                prefs.edit().remove("fpx:" + pkg).remove("fpy:" + pkg).apply();
            } else {
                favs.add(pkg);
            }
            saveFavorites(favs);
            refreshFavorites();
            refreshDrawerKeepScroll();
        });

        labels.add("Rename");
        icons.add(android.R.drawable.ic_menu_edit);
        actions.add(() -> showRename(info));

        labels.add("Hide from app list");
        icons.add(android.R.drawable.ic_menu_view);
        actions.add(() -> {
            Set<String> h = getHidden();
            h.add(pkg);
            saveHidden(h);
            refreshAll();
            Toast.makeText(this, "Hidden. Long-press the home screen > Hidden apps to show again", Toast.LENGTH_LONG).show();
        });

        labels.add("App info");
        icons.add(android.R.drawable.ic_menu_info_details);
        actions.add(() -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + pkg));
            startActivity(i);
        });

        final boolean sysApp = isSystemApp(info);
        if (!sysApp) {
            labels.add("Uninstall");
            icons.add(android.R.drawable.ic_menu_delete);
            actions.add(() -> {
                Intent i = new Intent(Intent.ACTION_DELETE);
                i.setData(Uri.parse("package:" + pkg));
                startActivity(i);
            });
        }

        int[] ic = new int[icons.size()];
        for (int k = 0; k < ic.length; k++) ic[k] = icons.get(k);
        showSheet(info.loadIcon(pm), sysApp ? labelOf(info) + " (System app)" : labelOf(info),
            labels.toArray(new String[0]), ic, actions.toArray(new Runnable[0]));
    }

    /** true for apps that came with the phone (cannot be uninstalled normally). */
    private boolean isSystemApp(ResolveInfo info) {
        int f = info.activityInfo.applicationInfo.flags;
        return (f & (android.content.pm.ApplicationInfo.FLAG_SYSTEM
            | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private void launchApp(ResolveInfo info) {
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
    }

    private void buildAlphabetBar() {
        TreeSet<String> letters = new TreeSet<String>();
        boolean hasOther = false;
        for (int i = 0; i < installedApps.size(); i++) {
            String k = keyOf(installedApps.get(i));
            if (OTHER.equals(k)) hasOther = true; else letters.add(k);
        }
        keys.clear();
        keys.add(STAR);
        keys.addAll(letters);
        if (hasOther) keys.add(OTHER);

        alphabetBar.removeAllViews();
        for (int i = 0; i < keys.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(keys.get(i));
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(16);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setIncludeFontPadding(false);
            tv.setShadowLayer(4, 0, 0, 0x88000000);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ROW_DP));
            tv.setLayoutParams(lp);
            alphabetBar.addView(tv);
        }
        rebuildDrawer();
    }

    private void rebuildDrawer() {
        Map<String, List<ResolveInfo>> groups = new HashMap<String, List<ResolveInfo>>();
        List<ResolveInfo> favApps = new ArrayList<ResolveInfo>();
        Set<String> favs = getFavorites();
        for (int i = 0; i < installedApps.size(); i++) {
            ResolveInfo info = installedApps.get(i);
            String k = keyOf(info);
            List<ResolveInfo> g = groups.get(k);
            if (g == null) {
                g = new ArrayList<ResolveInfo>();
                groups.put(k, g);
            }
            g.add(info);
            if (favs.contains(info.activityInfo.packageName)) favApps.add(info);
        }

        drawerRows.clear();
        sectionPos.clear();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            sectionPos.put(key, drawerRows.size());
            drawerRows.add(DrawerAdapter.Row.header(key));
            List<ResolveInfo> list = STAR.equals(key) ? favApps : groups.get(key);
            if (list == null) continue;
            for (int j = 0; j < list.size(); j++) {
                drawerRows.add(DrawerAdapter.Row.app(list.get(j)));
            }
        }
        drawerAdapter.notifyDataSetChanged();
    }

    private void refreshDrawerKeepScroll() {
        int first = listViewLetter.getFirstVisiblePosition();
        View v0 = listViewLetter.getChildAt(0);
        int top = (v0 == null) ? 0 : v0.getTop();
        rebuildDrawer();
        listViewLetter.setSelectionFromTop(first, top);
    }

    private void setupAlphabetTouch() {
        alphabetBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    if (keys.isEmpty()) return true;
                    float y = event.getY() - v.getPaddingTop();
                    int index = (int) (y / dp(ROW_DP));
                    if (index < 0) index = 0;
                    if (index >= keys.size()) index = keys.size() - 1;
                    showKey(keys.get(index));
                }
                return true;
            }
        });
    }

    private void showKey(String key) {
        if (key.equals(currentKey) && isDrawerOpen()) {
            return;
        }
        currentKey = key;
        openDrawer();
        Integer pos = sectionPos.get(key);
        if (pos != null) listViewLetter.setSelectionFromTop(pos, 0);
    }

    private boolean isDrawerOpen() {
        return drawerDialog != null && drawerDialog.isShowing();
    }

    /** App list lives in its own half-screen window so only that half gets the blur. */
    private void setupDrawerDialog() {
        rootFrame.removeView(letterOverlay);
        letterOverlay.setVisibility(View.VISIBLE);
        letterOverlay.setBackgroundColor(Color.TRANSPARENT);
        letterOverlay.setPadding(dp(20), dp(16), dp(8), dp(16));

        drawerDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        drawerDialog.setContentView(letterOverlay);
        drawerDialog.setCanceledOnTouchOutside(false);
        drawerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                currentKey = "";
            }
        });
        Window w = drawerDialog.getWindow();
        w.setFormat(PixelFormat.TRANSLUCENT);
        w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setGravity(Gravity.LEFT | Gravity.TOP);
        w.setLayout(getResources().getDisplayMetrics().widthPixels / 2,
            ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void openDrawer() {
        if (isDrawerOpen()) return;
        if (prefs.getBoolean("folders", false)) {
            showFolders();
            return;
        }
        Window w = drawerDialog.getWindow();
        boolean blur = false;
        if (Build.VERSION.SDK_INT >= 31) {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            blur = wm != null && wm.isCrossWindowBlurEnabled();
        }
        String strength = prefs.getString("blurstr", "medium");
        int radius = "soft".equals(strength) ? 60 : ("strong".equals(strength) ? 200 : 120);
        if (blur) {
            w.setBackgroundDrawable(glassPanel(true));
            w.setBackgroundBlurRadius(radius);
        } else {
            w.setBackgroundDrawable(glassPanel(false));
        }
        drawerDialog.show();
        letterOverlay.setAlpha(0f);
        letterOverlay.setTranslationX(-dp(28));
        letterOverlay.animate().alpha(1f).translationX(0f).setDuration(200).start();
    }

    @Override
    protected void onPause() {
        lockHandler.removeCallbacks(idleLockRun);
        super.onPause();
        tinkuPause();
        hideOverlay();
    }

    @Override
    protected void onDestroy() {
        tinkuPause();
        tinkuSpeakShutdown();
        unregisterV7();
        if (isDrawerOpen()) drawerDialog.dismiss();
        super.onDestroy();
    }

    private void hideOverlay() {
        if (isDrawerOpen()) drawerDialog.dismiss();
        currentKey = "";
    }

    private void showSearch() {
        hideOverlay();
        searchInput.setText("");
        filterSearch("");
        searchOverlay.setVisibility(View.VISIBLE);
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearch() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        searchOverlay.setVisibility(View.GONE);
        searchInput.setText("");
    }

    private void filterSearch(String q) {
        String query = q.trim().toLowerCase();
        searchApps.clear();
        for (int i = 0; i < installedApps.size(); i++) {
            ResolveInfo info = installedApps.get(i);
            String label = labelOf(info).toLowerCase();
            if (query.length() == 0 || label.contains(query)) {
                searchApps.add(info);
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (lockView != null && lockView.getVisibility() == View.VISIBLE) return;
        if (searchOverlay.getVisibility() == View.VISIBLE) {
            hideSearch();
        } else if (isDrawerOpen()) {
            hideOverlay();
        }
    }

    // ===== Step 31: WELCOME header, greeting, bottom "Search Now" bar =====
    private TextView welcomeView;
    private TextView greetingView;
    private TextView searchBarView;

    private void setupV31() {
        welcomeView = new TextView(this);
        welcomeView.setText("WELCOME");
        welcomeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        welcomeView.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        welcomeView.setTextColor(0xFFFFFFFF);
        welcomeView.setShadowLayer(8f, 0f, 2f, 0x66000000);
        welcomeView.setPadding(dp(2), 0, dp(2), dp(6));
        clockBox.addView(welcomeView, 0, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        greetingView = new TextView(this);
        greetingView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        greetingView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        greetingView.setTextColor(0xFFFFFFFF);
        greetingView.setShadowLayer(8f, 0f, 2f, 0x66000000);
        greetingView.setPadding(dp(2), dp(8), dp(2), 0);
        int gi = clockText != null ? clockBox.indexOfChild(clockText) + 1 : clockBox.getChildCount();
        clockBox.addView(greetingView, gi, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        searchBarView = new TextView(this);
        searchBarView.setText("Search Now");
        searchBarView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        searchBarView.setTextColor(0xFF111111);
        searchBarView.setGravity(Gravity.CENTER_VERTICAL);
        searchBarView.setPadding(dp(22), 0, dp(22), 0);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xF2FFFFFF);
        pill.setCornerRadius(dp(28));
        searchBarView.setBackground(pill);
        searchBarView.setOnClickListener(v -> showSearch());
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(56), Gravity.BOTTOM);
        slp.setMargins(dp(20), 0, dp(20), dp(24));
        rootFrame.addView(searchBarView, slp);

        applyV31();
    }

    private void applyV31() {
        if (welcomeView == null) return;
        welcomeView.setVisibility(prefs.getBoolean("welcome", false) ? View.VISIBLE : View.GONE);
        greetingView.setText(greetingText());
        greetingView.setVisibility(prefs.getBoolean("greeting", false) ? View.VISIBLE : View.GONE);
        searchBarView.setVisibility(prefs.getBoolean("searchbar", false) ? View.VISIBLE : View.GONE);
        // greeting lamo bhayo bhane favourites tala sarne (overlap nahune)
        clockBox.post(new Runnable() {
            @Override
            public void run() {
                nudgeFavs();
            }
        });
    }

    private String greetingText() {
        int h = new java.util.GregorianCalendar().get(java.util.Calendar.HOUR_OF_DAY);
        if (h < 12) return "Good Morning";
        if (h < 17) return "Good Afternoon";
        return "Good Evening";
    }

    // ===== Step 32: music widget (play/pause) =====
    private LinearLayout mediaBox;
    private TextView mediaTitle;
    private TextView mediaArtist;
    private TextView mediaPlay;
    private android.media.session.MediaController activeMedia;
    private final android.media.session.MediaController.Callback mediaCb =
        new android.media.session.MediaController.Callback() {
            @Override
            public void onMetadataChanged(android.media.MediaMetadata m) {
                updateMedia();
            }

            @Override
            public void onPlaybackStateChanged(android.media.session.PlaybackState s) {
                updateMedia();
            }
        };

    private void setupV32() {
        mediaBox = new LinearLayout(this);
        mediaBox.setOrientation(LinearLayout.VERTICAL);
        mediaBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x33000000);
        bg.setCornerRadius(dp(18));
        mediaBox.setBackground(bg);

        mediaTitle = new TextView(this);
        mediaTitle.setTextColor(0xFFFFFFFF);
        mediaTitle.setTextSize(15);
        mediaTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        mediaTitle.setSingleLine(true);
        mediaBox.addView(mediaTitle);

        mediaArtist = new TextView(this);
        mediaArtist.setTextColor(0xCCFFFFFF);
        mediaArtist.setTextSize(12);
        mediaArtist.setSingleLine(true);
        mediaBox.addView(mediaArtist);

        mediaPlay = new TextView(this);
        mediaPlay.setTextColor(0xFFFFFFFF);
        mediaPlay.setTextSize(20);
        mediaPlay.setPadding(0, dp(4), 0, 0);
        mediaBox.addView(mediaPlay);

        mediaBox.setOnClickListener(v -> toggleMediaPlay());
        rootFrame.addView(mediaBox, new FrameLayout.LayoutParams(
            dp(210), ViewGroup.LayoutParams.WRAP_CONTENT));
        applyMediaPos();
        applyMedia();
    }

    private boolean hasNotifAccess() {
        String flat = android.provider.Settings.Secure.getString(
            getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private void applyMedia() {
        if (mediaBox == null) return;
        boolean on = prefs.getBoolean("media", false) && hasNotifAccess();
        applyMediaPos();
        if (!on) mediaBox.setVisibility(View.GONE);
        if (activeMedia != null) {
            activeMedia.unregisterCallback(mediaCb);
            activeMedia = null;
        }
        if (!on) return;
        registerSessionListener();
        try {
            android.media.session.MediaSessionManager msm =
                (android.media.session.MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<android.media.session.MediaController> list = msm.getActiveSessions(
                new android.content.ComponentName(this, MediaNotifService.class));
            if (list != null && !list.isEmpty()) {
                activeMedia = list.get(0);
                activeMedia.registerCallback(mediaCb);
            }
        } catch (SecurityException e) {
            activeMedia = null;
        }
        updateMedia();
    }

    private void updateMedia() {
        if (mediaTitle == null) return;
        if (activeMedia == null) {
            mediaBox.setVisibility(View.GONE);
            mediaTitle.setText("Music chhaina");
            mediaArtist.setText("");
            mediaPlay.setText("▶");
            return;
        }
        android.media.MediaMetadata m = activeMedia.getMetadata();
        CharSequence t = m == null ? null : m.getText(android.media.MediaMetadata.METADATA_KEY_TITLE);
        CharSequence a = m == null ? null : m.getText(android.media.MediaMetadata.METADATA_KEY_ARTIST);
        mediaTitle.setText(t != null ? t : "Music");
        mediaArtist.setText(a != null ? a : "");
        android.media.session.PlaybackState ps = activeMedia.getPlaybackState();
        boolean playing = ps != null && ps.getState() == android.media.session.PlaybackState.STATE_PLAYING;
        // sirf music/video chalda matra dekhaune; roke pachhi aafai hide
        mediaBox.setVisibility(playing && prefs.getBoolean("media", false) ? View.VISIBLE : View.GONE);
        mediaPlay.setText(playing ? "❚❚" : "▶");
    }

    private boolean mediaSessionListening = false;

    /** Naya app ma music suru bhayo bhane pani aafai pakadcha */
    private void registerSessionListener() {
        if (mediaSessionListening) return;
        try {
            android.media.session.MediaSessionManager msm =
                (android.media.session.MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            msm.addOnActiveSessionsChangedListener(list -> applyMedia(),
                new android.content.ComponentName(this, MediaNotifService.class));
            mediaSessionListening = true;
        } catch (SecurityException e) {
            // notification access chhaina
        }
    }

    /** Music card ko thau: top / middle / bottom ra left / right */
    private void applyMediaPos() {
        if (mediaBox == null) return;
        String pos = prefs.getString("media_pos", "top");
        String side = prefs.getString("media_side", "right");
        int hg = "left".equals(side) ? Gravity.LEFT : Gravity.RIGHT;
        int vg;
        int top = 0;
        int bottom = 0;
        if ("middle".equals(pos)) {
            vg = Gravity.CENTER_VERTICAL;
        } else if ("bottom".equals(pos)) {
            vg = Gravity.BOTTOM;
            bottom = dp(110);
        } else {
            vg = Gravity.TOP;
            top = dp(120);
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            dp(210), ViewGroup.LayoutParams.WRAP_CONTENT, vg | hg);
        lp.setMargins(dp(16), top, dp(16), bottom);
        mediaBox.setLayoutParams(lp);
    }

    private void toggleMediaPlay() {
        if (activeMedia == null) return;
        android.media.session.PlaybackState ps = activeMedia.getPlaybackState();
        boolean playing = ps != null && ps.getState() == android.media.session.PlaybackState.STATE_PLAYING;
        if (playing) {
            activeMedia.getTransportControls().pause();
        } else {
            activeMedia.getTransportControls().play();
        }
    }

    // ===== Step 33: drawer folder grid (Social, Media, Settings...) =====
    private String categoryName(ResolveInfo r) {
        try {
            android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(r.activityInfo.packageName, 0);
            if (Build.VERSION.SDK_INT >= 26) {
                int c = ai.category;
                if (c == android.content.pm.ApplicationInfo.CATEGORY_SOCIAL) return "Social";
                if (c == android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY) return "Productivity";
                if (c == android.content.pm.ApplicationInfo.CATEGORY_GAME) return "Games";
                if (c == android.content.pm.ApplicationInfo.CATEGORY_AUDIO
                    || c == android.content.pm.ApplicationInfo.CATEGORY_VIDEO
                    || c == android.content.pm.ApplicationInfo.CATEGORY_IMAGE) return "Media";
                if (c == android.content.pm.ApplicationInfo.CATEGORY_MAPS
                    || c == android.content.pm.ApplicationInfo.CATEGORY_NEWS) return "Info";
            }
            if ((ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) return "Settings";
        } catch (Exception e) {
            // package na bhetiyo
        }
        return "Other";
    }

    private void showFolders() {
        final Dialog d = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xE6000000);
        sv.setOnClickListener(v -> d.dismiss());
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(56), dp(24), dp(40));

        java.util.LinkedHashMap<String, List<ResolveInfo>> groups =
            new java.util.LinkedHashMap<String, List<ResolveInfo>>();
        for (ResolveInfo r : installedApps) {
            String k = categoryName(r);
            List<ResolveInfo> l = groups.get(k);
            if (l == null) {
                l = new ArrayList<ResolveInfo>();
                groups.put(k, l);
            }
            l.add(r);
        }

        for (Map.Entry<String, List<ResolveInfo>> e : groups.entrySet()) {
            TextView t = new TextView(this);
            t.setText(e.getKey());
            t.setTextColor(0xFFFFFFFF);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            t.setPadding(0, dp(18), 0, dp(10));
            box.addView(t);

            LinearLayout row = null;
            int i = 0;
            for (final ResolveInfo r : e.getValue()) {
                if (i % 4 == 0) {
                    row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    box.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(r.loadIcon(pm));
                iv.setPadding(dp(8), dp(8), dp(8), dp(8));
                iv.setOnClickListener(v -> {
                    d.dismiss();
                    launchApp(r);
                });
                row.addView(iv, new LinearLayout.LayoutParams(0, dp(76), 1f));
                i++;
            }
        }

        sv.addView(box);
        d.setContentView(sv);
        Window fw = d.getWindow();
        if (fw != null) fw.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        showD(d);
    }

    // ===== Step 34: charger/battery voice alerts (customizable, offline TTS) =====
    // Rule format: U|percent|message  (U = charging ma >=, D = charger nalagda <=)
    // {p} = real-time battery percent
    private static final String DEF_RULES =
        "U|93|{p} percent, please disconnect the charger\n"
        + "U|75|Battery charged {p} percent\n"
        + "U|50|Battery charged {p} percent\n"
        + "D|20|Battery low, please connect the charger";
    private static final String DEF_CONNECT = "Battery {p} percent, charger connected, battery start charging";
    private static final String DEF_DISCONNECT = "Charger disconnected, battery {p} percent";

    private BroadcastReceiver chargeReceiver;
    private final HashSet<String> chargeFired = new HashSet<String>();
    private boolean chargeFirstRun = true;

    private void setupV34() {
        chargeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String a = intent.getAction();
                if (Intent.ACTION_BATTERY_CHANGED.equals(a)) {
                    chargeCheck(intent);
                    return;
                }
                boolean plugged = Intent.ACTION_POWER_CONNECTED.equals(a);
                int pct = chargePct();
                chargeFired.clear();
                chargePrime(pct, plugged);
                if (plugged) {
                    chargeSay(prefs.getString("msg_connect", DEF_CONNECT), pct);
                } else {
                    chargeSay(prefs.getString("msg_disconnect", DEF_DISCONNECT), pct);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(chargeReceiver, f);
    }

    private int chargePct() {
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return -1;
        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level < 0 || scale <= 0) return -1;
        return level * 100 / scale;
    }

    private boolean chargePlugged(Intent b) {
        int st = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL;
    }

    private void chargeCheck(Intent b) {
        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level < 0 || scale <= 0) return;
        int pct = level * 100 / scale;
        boolean plugged = chargePlugged(b);
        if (chargeFirstRun) {
            chargeFirstRun = false;
            chargePrime(pct, plugged);
            return;
        }
        for (String[] r : chargeRules()) {
            String k = r[0] + "|" + r[1];
            if (chargeFired.contains(k)) continue;
            if (ruleHit(r, pct, plugged)) {
                chargeFired.add(k);
                chargeSay(r[2], pct);
                break; // ek choti ek matra bolchha
            }
        }
    }

    /** Already-passed rules lai pre-mark garne, taki app khulda ya connect garda purano percent nabolos. */
    private void chargePrime(int pct, boolean plugged) {
        if (pct < 0) return;
        for (String[] r : chargeRules()) {
            if (ruleHit(r, pct, plugged)) chargeFired.add(r[0] + "|" + r[1]);
        }
    }

    private boolean ruleHit(String[] r, int pct, boolean plugged) {
        int p = Integer.parseInt(r[1]);
        if ("U".equals(r[0])) return plugged && pct >= p;
        return !plugged && pct <= p;
    }

    private void chargeSay(String tpl, int pct) {
        if (!prefs.getBoolean("charge_sound", true)) return;
        if (tpl == null || tpl.trim().isEmpty()) return;
        String text = tpl.replace("{p}", pct < 0 ? "?" : String.valueOf(pct));
        tinkuSpeakOut(text, false, null);
    }

    private List<String[]> chargeRules() {
        String raw = prefs.getString("charge_rules", DEF_RULES);
        List<String[]> out = new ArrayList<String[]>();
        for (String line : raw.split("\n")) {
            String[] p = line.split("\\|", 3);
            if (p.length == 3 && p[1].trim().matches("\\d{1,3}")) {
                out.add(new String[]{p[0].trim(), p[1].trim(), p[2]});
            }
        }
        return out;
    }

    private void saveChargeRules(List<String[]> rules) {
        StringBuilder sb = new StringBuilder();
        for (String[] r : rules) {
            sb.append(r[0]).append('|').append(r[1]).append('|').append(r[2].replace("\n", " ")).append('\n');
        }
        prefs.edit().putString("charge_rules", sb.toString()).apply();
        chargeFired.clear();
    }

    /** Charge alert ko list + message editor (Tinku widget bata khulcha). */
    private void showChargeEditor() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = sheetBox("Charge alerts");

        addTapRow(box, "Charger connected message",
            prefs.getString("msg_connect", DEF_CONNECT), () -> {
                d.dismiss();
                showTextEditor("Charger connected message", "msg_connect", DEF_CONNECT);
            });
        addTapRow(box, "Charger disconnected message",
            prefs.getString("msg_disconnect", DEF_DISCONNECT), () -> {
                d.dismiss();
                showTextEditor("Charger disconnected message", "msg_disconnect", DEF_DISCONNECT);
            });

        List<String[]> rules = chargeRules();
        for (int i = 0; i < rules.size(); i++) {
            final int idx = i;
            String[] r = rules.get(i);
            String title = ("D".equals(r[0]) ? "Low " : "Charging ") + r[1] + "%";
            addTapRow(box, title, r[2], () -> {
                d.dismiss();
                showRuleEditor(idx);
            });
        }
        addTapRow(box, "+ Naya alert", "Percent ra message aafai banaunus", () -> {
            d.dismiss();
            showRuleEditor(-1);
        });
        addTapRow(box, "Default ma farkaaunus", "Sabai alert ra message reset", () -> {
            d.dismiss();
            prefs.edit().remove("charge_rules").remove("msg_connect").remove("msg_disconnect").apply();
            chargeFired.clear();
        });
        showBottom(d, box);
    }

    private void showTextEditor(String title, final String key, String def) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setText(prefs.getString(key, def));
        et.setHint("{p} = real battery percent");
        new android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(et)
            .setPositiveButton("Save", (dl, w) -> prefs.edit().putString(key, et.getText().toString()).apply())
            .setNeutralButton("Silent", (dl, w) -> prefs.edit().putString(key, "").apply())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showRuleEditor(final int idx) {
        final List<String[]> rules = chargeRules();
        final boolean isNew = idx < 0 || idx >= rules.size();
        final String[] cur = isNew ? new String[]{"U", "80", "Battery {p} percent"} : rules.get(idx);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        final android.widget.EditText pct = new android.widget.EditText(this);
        pct.setHint("Percent (jastai 80)");
        pct.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        pct.setText(cur[1]);
        final android.widget.CheckBox low = new android.widget.CheckBox(this);
        low.setText("Low alert (charger nalagda bolne)");
        low.setChecked("D".equals(cur[0]));
        final android.widget.EditText msg = new android.widget.EditText(this);
        msg.setHint("Message ({p} = real percent)");
        msg.setText(cur[2]);
        box.addView(pct);
        box.addView(low);
        box.addView(msg);

        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this)
            .setTitle(isNew ? "Naya alert" : "Alert edit")
            .setView(box)
            .setPositiveButton("Save", (dl, w) -> {
                String ps = pct.getText().toString().trim();
                if (!ps.matches("\\d{1,3}")) return;
                int pv = Integer.parseInt(ps);
                if (pv < 1 || pv > 100) return;
                String[] nr = new String[]{
                    low.isChecked() ? "D" : "U",
                    String.valueOf(pv),
                    msg.getText().toString().replace("|", "/").trim()
                };
                if (isNew) rules.add(nr); else rules.set(idx, nr);
                saveChargeRules(rules);
            })
            .setNegativeButton("Cancel", null);
        if (!isNew) {
            b.setNeutralButton("Delete", (dl, w) -> {
                rules.remove(idx);
                saveChargeRules(rules);
            });
        }
        b.show();
    }

    // ===== Presets: ek click ma settings bundle apply =====
    private void showPresets() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = sheetBox("Presets");
        addTapRow(box, "Monochrome", "Welcome, weather, battery, dark glass", () -> {
            d.dismiss();
            applyPreset("mono");
        });
        addTapRow(box, "Minimal", "Ekdam saaf, kunai info nadekhine", () -> {
            d.dismiss();
            applyPreset("minimal");
        });
        addTapRow(box, "Full", "Sabai info, calendar sahit", () -> {
            d.dismiss();
            applyPreset("full");
        });
        showBottom(d, box);
    }

    private void applyPreset(String name) {
        SharedPreferences.Editor ed = prefs.edit();
        if ("minimal".equals(name)) {
            ed.putBoolean("showtop", false)
              .putBoolean("showbottom", false)
              .putBoolean("weather", false)
              .putBoolean("showbat", false)
              .putBoolean("events", false)
              .putBoolean("stacked", false)
              .putBoolean("darktext", false)
              .putBoolean("welcome", false)
              .putBoolean("greeting", false)
              .putBoolean("searchbar", false)
              .putBoolean("media", false)
              .putBoolean("folders", false)
              .putBoolean("charge_sound", false);
        } else {
            ed.putBoolean("showtop", true)
              .putBoolean("showbottom", true)
              .putBoolean("weather", true)
              .putBoolean("showbat", true)
              .putBoolean("events", "full".equals(name))
              .putBoolean("stacked", true)
              .putBoolean("darktext", false)
              .putString("glass", "dark")
              .putInt("home_color", "mono".equals(name) ? 0xFFFFFFFF : 0)
              .putBoolean("welcome", true)
              .putBoolean("greeting", true)
              .putBoolean("searchbar", true)
              .putBoolean("media", true)
              .putBoolean("folders", true)
              .putBoolean("charge_sound", true);
        }
        // layout pani preset sanga: favourites vertical, sabai kura left
        ed.putString("favlayout", "minimal".equals(name) ? "free" : "vertical")
          .putString("favalign", "left")
          .putString("align", "left");
        ed.apply();

        if (prefs.getBoolean("weather", false)) refreshWeather(true);
        if (prefs.getBoolean("events", false)) ensureCalendarPermission();
        // screen fresh bata setup hunchha (preset ko sabai setting ek saath)
        recreate();
    }
}
