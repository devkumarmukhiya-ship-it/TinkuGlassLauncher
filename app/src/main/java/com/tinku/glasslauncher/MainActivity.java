package com.tinku.glasslauncher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0x66FFFFFF);
        searchButton.setBackground(circle);
        searchButton.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
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
        clockBox.setOnLongClickListener(homeLong);
        textClock.setOnLongClickListener(homeLong);
        textDate.setOnLongClickListener(homeLong);

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

        setupAlphabetTouch();
        buildAlphabetBar();
        applyAlign();
        applyClockPos();
        applyFavPos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        buildAlphabetBar();
        applyAlign();
        applyClockPos();
        applyFavPos();
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
        if (letterOverlay.getVisibility() == View.VISIBLE) {
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

    private void applyAlign() {
        int g = isLeft() ? Gravity.LEFT : Gravity.RIGHT;
        textClock.setGravity(g);
        textDate.setGravity(g);
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
    }

    private void applyFavPos() {
        applyPos(favoritesBox, "fx", "fy", 0.06f, 0.92f);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (dragMode && dragTarget != null) {
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
        }
    }

    private void startDrag(View target, String kx, String ky) {
        if (target.getWidth() == 0) {
            Toast.makeText(this, "Nothing to move yet. Add a favourite first.", Toast.LENGTH_LONG).show();
            return;
        }
        dragTarget = target;
        dragKx = kx;
        dragKy = ky;
        dragMode = true;
        Toast.makeText(this, "Touch anywhere on the screen, then lift your finger", Toast.LENGTH_LONG).show();
    }

    private void showHomeMenu() {
        showSheet(null, "Home screen",
            new String[] {
                "Move clock (drag anywhere)",
                "Move favourites (drag anywhere)",
                "Align text left",
                "Align text right",
                "Reset positions",
                "Hidden apps"
            },
            new int[] {
                android.R.drawable.ic_menu_mylocation,
                android.R.drawable.ic_menu_mylocation,
                0,
                0,
                android.R.drawable.ic_menu_revert,
                android.R.drawable.ic_menu_view
            },
            new Runnable[] {
                () -> startDrag(clockBox, "cx", "cy"),
                () -> startDrag(favoritesBox, "fx", "fy"),
                () -> { prefs.edit().putString("align", "left").apply(); applyAlign(); },
                () -> { prefs.edit().putString("align", "right").apply(); applyAlign(); },
                () -> {
                    prefs.edit().remove("cx").remove("cy").remove("fx").remove("fy").apply();
                    applyClockPos();
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
        d.show();
        input.requestFocus();
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
            tv.setText(labelOf(info));
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(22);
            tv.setShadowLayer(6, 0, 0, 0x88000000);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
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
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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
        d.show();
    }

    private void showMenu(final ResolveInfo info) {
        final String pkg = info.activityInfo.packageName;
        final boolean isFav = getFavorites().contains(pkg);
        showSheet(info.loadIcon(pm), labelOf(info),
            new String[] {
                isFav ? "Remove from favourites" : "Favourite",
                "Rename",
                "Hide from app list",
                "App info",
                "Uninstall"
            },
            new int[] {
                isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off,
                android.R.drawable.ic_menu_edit,
                android.R.drawable.ic_menu_view,
                android.R.drawable.ic_menu_info_details,
                android.R.drawable.ic_menu_delete
            },
            new Runnable[] {
                () -> {
                    Set<String> favs = getFavorites();
                    if (isFav) favs.remove(pkg); else favs.add(pkg);
                    saveFavorites(favs);
                    refreshFavorites();
                    refreshDrawerKeepScroll();
                },
                () -> showRename(info),
                () -> {
                    Set<String> h = getHidden();
                    h.add(pkg);
                    saveHidden(h);
                    refreshAll();
                    Toast.makeText(this, "Hidden. Long-press the home screen > Hidden apps to show again", Toast.LENGTH_LONG).show();
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
        if (key.equals(currentKey) && letterOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        currentKey = key;
        letterOverlay.setVisibility(View.VISIBLE);
        Integer pos = sectionPos.get(key);
        if (pos != null) listViewLetter.setSelectionFromTop(pos, 0);
    }

    private void hideOverlay() {
        letterOverlay.setVisibility(View.GONE);
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
        if (searchOverlay.getVisibility() == View.VISIBLE) {
            hideSearch();
        } else if (letterOverlay.getVisibility() == View.VISIBLE) {
            hideOverlay();
        }
    }
}
