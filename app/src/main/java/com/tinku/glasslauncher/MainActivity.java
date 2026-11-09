package com.tinku.glasslauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
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
import java.util.List;

public class MainActivity extends Activity {
    private ListView listView;
    private ListView listViewLetter;
    private LinearLayout alphabetBar;
    private FrameLayout letterOverlay;
    private TextView textLetter;
    private List<ResolveInfo> installedApps;
    private List<ResolveInfo> letterApps = new ArrayList<ResolveInfo>();
    private AppAdapter letterAdapter;
    private PackageManager pm;
    private char currentLetter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listViewApps);
        listViewLetter = findViewById(R.id.listViewLetter);
        alphabetBar = findViewById(R.id.alphabetBar);
        letterOverlay = findViewById(R.id.letterOverlay);
        textLetter = findViewById(R.id.textLetter);
        pm = getPackageManager();

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

        AppAdapter adapter = new AppAdapter(this, installedApps);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchApp(installedApps.get(position));
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

        letterOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOverlay();
            }
        });

        setupAlphabetBar();
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
