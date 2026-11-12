package com.tinku.glasslauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** One font for Tinku, chosen in settings, applied to the clock / home screen / whole launcher. */
public class FontKit {

    public static final int CLOCK = 0, HOME = 1, ALL = 2;

    /** {label, file in assets/fonts}. Only files that really exist are shown. */
    public static final String[][] LIST = {
        { "Poppins", "Poppins-Regular.ttf" },
        { "Lato", "Lato-Regular.ttf" },
        { "Mukta (Nepali ready)", "Mukta-Regular.ttf" },
        { "Hind (Nepali ready)", "Hind-Regular.ttf" },
        { "Ubuntu", "Ubuntu-Regular.ttf" },
        { "Righteous", "Righteous-Regular.ttf" },
        { "Pacifico", "Pacifico-Regular.ttf" },
        { "Satisfy", "Satisfy-Regular.ttf" },
        { "Indie Flower", "IndieFlower-Regular.ttf" },
        { "Bangers", "Bangers-Regular.ttf" },
        { "Special Elite", "SpecialElite-Regular.ttf" },
        { "Fredoka", "FredokaOne-Regular.ttf" },
        { "Chewy", "Chewy-Regular.ttf" },
        { "Lobster", "Lobster-Regular.ttf" },
        { "Shadows Into Light", "ShadowsIntoLight.ttf" },
        { "Bebas Neue", "BebasNeue-Regular.ttf" },
        { "Archivo Black", "ArchivoBlack-Regular.ttf" },
        { "Abril Fatface", "AbrilFatface-Regular.ttf" },
        { "Chango", "Chango-Regular.ttf" },
        { "Sacramento", "Sacramento-Regular.ttf" },
        { "Mansalva", "Mansalva-Regular.ttf" },
        { "Monoton", "Monoton-Regular.ttf" },
        { "Press Start 2P", "PressStart2P-Regular.ttf" }
    };

    private static final HashMap<String, Typeface> CACHE = new HashMap<String, Typeface>();

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("tinku", Context.MODE_PRIVATE);
    }

    public static Typeface load(Context c, String file) {
        if (file == null || file.length() == 0) return null;
        Typeface t = CACHE.get(file);
        if (t == null) {
            try {
                t = Typeface.createFromAsset(c.getApplicationContext().getAssets(), "fonts/" + file);
            } catch (Throwable e) {
                return null;
            }
            CACHE.put(file, t);
        }
        return t;
    }

    public static String chosenFile(Context c) {
        return prefs(c).getString("tinkufont", "");
    }

    /** The chosen font, or null for the system default. */
    public static Typeface chosen(Context c) {
        return load(c, chosenFile(c));
    }

    public static int scope(Context c) {
        String s = prefs(c).getString("fontscope", "clock");
        if ("all".equals(s)) return ALL;
        if ("home".equals(s)) return HOME;
        return CLOCK;
    }

    public static String label(Context c) {
        String f = chosenFile(c);
        for (int i = 0; i < LIST.length; i++) if (LIST[i][1].equals(f)) return LIST[i][0];
        return "System default";
    }

    public static List<String[]> available(Context c) {
        HashSet<String> have = new HashSet<String>();
        try {
            String[] f = c.getApplicationContext().getAssets().list("fonts");
            if (f != null) for (int i = 0; i < f.length; i++) have.add(f[i]);
        } catch (Exception e) {
            // none
        }
        List<String[]> out = new ArrayList<String[]>();
        for (int i = 0; i < LIST.length; i++) if (have.contains(LIST[i][1])) out.add(LIST[i]);
        return out;
    }

    private static void walk(View v, Typeface tf) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            Typeface cur = t.getTypeface();
            t.setTypeface(tf, cur == null ? Typeface.NORMAL : cur.getStyle());
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), tf);
        }
    }

    /** Put the chosen font on every text under v, if the chosen scope reaches this level. */
    public static void apply(Context c, View v, int level) {
        if (v == null || scope(c) < level) return;
        Typeface tf = chosen(c);
        if (tf != null) walk(v, tf);
    }

    /** Also style rows the list / box adds later. */
    public static void watch(final Context c, ViewGroup g, final int level) {
        if (g == null) return;
        g.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                apply(c, child, level);
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        });
    }
}
