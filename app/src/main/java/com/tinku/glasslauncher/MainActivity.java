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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private ListView listView;
    private ListView listViewLetter;
    private LinearLayout alphabetBar;
    private LinearLayout favoritesBox;
    private FrameLayout letterOverlay;
    private TextView textLetter;
    private List<ResolveInfo> installedApps;
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
        listView = findViewById(R.id.listViewApps);
        listViewLetter = findViewById(R.id.listViewLetter);
        alphabetBar = findViewById(R.id.alphabetBar);
        favoritesBox = findViewById(R.id.favoritesBox);
        letterOverlay = findViewById(R.id.letterOverlay);
        textLetter = findViewById(R.id.textLetter);
        pm = getPackageManager();

        loadApps();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchApp(installedApps.get(position));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showMenu(installedApps.get(position));
                return true;
            }
        });

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

        setupAlphabetBar();
        refreshFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        refreshFavorites();
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
        listView.setAdapter(new AppAdapter(this, installedApps));
    }

    private Set<String> getFavorites() {
        return new HashSet<String>(prefs.getStringSet("favs", new HashSet<String>()));
    }

    private void saveFavorites(Set<String> favs) {
        prefs.edit().putStringSet("favs", favs).apply();
    }

    private void refreshFavorites() {
        favoritesBox.removeAllViews();
        Set<String> favs = getFavorites();
        for (int i = 0; i < installedApps.size(); i++) {
            final ResolveInfo info = installedApps.get(i);
            if (favs.contains(info.activityInfo.packageName)) {
                TextView tv = new TextView(this);
                tv.setText(info.loadLabel(pm));
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(28);
                tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                tv.setGravity(Gravity.RIGHT);
                tv.setPadding(0, 8, 0, 8);
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchApp(info);
                    }
                });
                tv.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        showMenu(info);
                        return true;
                    }
                });
                favoritesBox.addView(tv);
            }
        }
    }

    private void showMenu(final ResolveInfo info) {
        final String pkg = info.activityInfo.packageName;
        final boolean isFav = getFavorites().contains(pkg);
        String[] options = new String[] {
            isFav ? "Unpin" : "Pin to top",
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
                    } else if (which == 2) {
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
            tv.setTextColor(0xCCFFFFFF);
            tv.setTextSize(11);
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
                    float y = event.getY();
                    int height = v.getHeight();
                    if (height == 0) return true;
                    int index = (int) (y / height * letters.length());
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
