package com.tinku.glasslauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.HashMap;
import java.util.List;

public class AppAdapter extends BaseAdapter {
    private Context context;
    private List<ResolveInfo> apps;
    private PackageManager pm;
    private int layoutRes;

    public AppAdapter(Context context, List<ResolveInfo> apps) {
        this(context, apps, R.layout.item_app);
    }

    public AppAdapter(Context context, List<ResolveInfo> apps, int layoutRes) {
        this.context = context;
        this.apps = apps;
        this.pm = context.getPackageManager();
        this.layoutRes = layoutRes;
    }

    public static String labelOf(Context ctx, ResolveInfo info) {
        SharedPreferences p = ctx.getSharedPreferences("tinku", Context.MODE_PRIVATE);
        String custom = p.getString("name:" + info.activityInfo.packageName, null);
        if (custom != null && custom.trim().length() > 0) {
            return custom;
        }
        return info.loadLabel(ctx.getPackageManager()).toString();
    }

    private static final HashMap<String, Boolean> GLYPH_CACHE = new HashMap<String, Boolean>();

    /** True when the icon's foreground layer is just a glyph (not a full-bleed picture). */
    private static boolean foregroundIsGlyph(String key, Drawable fg) {
        Boolean cached = GLYPH_CACHE.get(key);
        if (cached != null) return cached;
        boolean ok = false;
        try {
            int n = 108;
            Bitmap bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bmp);
            Drawable t = fg;
            if (fg.getConstantState() != null) {
                t = fg.getConstantState().newDrawable().mutate();
            }
            t.setBounds(0, 0, n, n);
            t.draw(cv);
            int lo = 21;
            int hi = 87;
            int solid = 0;
            int total = 0;
            for (int y = lo; y < hi; y += 3) {
                for (int x = lo; x < hi; x += 3) {
                    total++;
                    if (Color.alpha(bmp.getPixel(x, y)) > 200) solid++;
                }
            }
            bmp.recycle();
            ok = solid > 0 && total > 0 && (solid / (float) total) < 0.8f;
        } catch (Throwable e) {
            ok = false;
        }
        GLYPH_CACHE.put(key, ok);
        return ok;
    }

    /** App icon, optionally turned into a single-colour (monochrome) icon. */
    public static int tone(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("tinku", Context.MODE_PRIVATE);
        return sp.getBoolean("darktext", false) ? 0xFF1B1F27 : 0xFFFFFFFF;
    }

    public static Drawable iconOf(Context ctx, ResolveInfo info) {
        return iconOf(ctx, info, tone(ctx));
    }

    public static Drawable iconOf(Context ctx, ResolveInfo info, int tint) {
        Drawable d = info.loadIcon(ctx.getPackageManager());
        SharedPreferences p = ctx.getSharedPreferences("tinku", Context.MODE_PRIVATE);
        if (!p.getBoolean("mono", false)) {
            return d;
        }
        if (Build.VERSION.SDK_INT >= 26 && d instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable ai = (AdaptiveIconDrawable) d;
            if (Build.VERSION.SDK_INT >= 33) {
                Drawable m = ai.getMonochrome();
                if (m != null) {
                    m = m.mutate();
                    m.setTint(tint);
                    return new InsetDrawable(m, -0.2f);
                }
            }
            Drawable fg = ai.getForeground();
            String key = info.activityInfo.packageName + "/" + info.activityInfo.name;
            if (fg != null && foregroundIsGlyph(key, fg)) {
                Drawable w = fg.mutate();
                w.setTint(tint);
                return new InsetDrawable(w, -0.2f);
            }
        }
        Drawable g = d.mutate();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);
        g.setColorFilter(new ColorMatrixColorFilter(cm));
        return g;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        }

        ResolveInfo info = apps.get(position);
        ImageView icon = convertView.findViewById(R.id.imageViewAppIcon);
        TextView name = convertView.findViewById(R.id.textViewAppName);

        name.setText(labelOf(context, info));
        icon.setImageDrawable(iconOf(context, info, 0xFFFFFFFF));

        return convertView;
    }
}
