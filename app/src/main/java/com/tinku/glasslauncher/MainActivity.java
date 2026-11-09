package com.tinku.glasslauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private ListView listViewLetter;
    private LinearLayout alphabetBar;
    private LinearLayout homeBox;
    private LinearLayout clockBox;
    private LinearLayout favoritesBox;
    private View spacer;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tinku", MODE_PRIVATE);
        pm = getPackageManager();
        listViewLetter = findViewById(R.id.listViewLetter);
        alphabetBar = findViewById(R.id.alphabetBar);
        homeBox = findViewById(R.id.homeBox);
        clockBox = findViewById(R.id.clockBox);
        favoritesBox = findViewById(R.id.favoritesBox);
        spacer = findViewById(R.id.spacer);
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

        setupAlphabetBar();
        applyLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        applyLayout();
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

    private void applyLayout() {
        int g = isLeft() ? Gravity.LEFT : Gravity.RIGHT;
        textClock.setGravity(g);
        textDate.setGravity(g);
        boolean top = "top".equals(prefs.getString("vert", "bottom"));
        homeBox.removeAllViews();
        if (top) {
            homeBox.addView(clockBox);
            homeBox.addView(spacer);
            homeBox.addView(favoritesBox);
        } else {
            homeBox.addView(spacer);
            homeBox.addView(clockBox);
            homeBox.addView(favoritesBox);
        }
        refreshFavorites();
    }

    private void showClockMenu() {
        String[] options = new String[] { "Left", "Right", "Top", "Bottom" };
        new AlertDialog.Builder(this)
            .setTitle("Clock position")
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor e = prefs.edit();
                    if (which == 0) e.putString("align", "left");
                    else if (which == 1) e.putString("align", "right");
                    else if (which == 2) e.putString("vert", "top");
                    else e.putString("vert", "bottom");
                    e.apply();
                    applyLayout();
                }
            })
            .show();
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

    private void showMenu(final ResolveInfo info) {
        final String pkg = info.activityInfo.packageName;
        final boolean isFav = getFavorites().contains(pkg);
        String[] options = new String[] {
            isFav ? "Remove from favourites" : "Favourite",
            "App info",
            "Uninstall"
        };
        new AlertDialog.Builder(this)
            .setTitle(info.loadLabel(pm))
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        Set<String> favs = getFavorites();
                        if (isFav) favs.remove(pkg); else favs.add(pkg);
                        saveFavorites(favs);
                        refreshFavorites();
                    } else if (which == 1) {
                        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.setData(Uri.parse("package:" + pkg));
                        startActivity(i);
                    } else {
                        Intent i = new Intent(Intent.ACTION_DELETE);
                        i.setData(Uri.parse("package:" + pkg));
                        startActivity(i);
                    }
                }
            })
            .show();
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
