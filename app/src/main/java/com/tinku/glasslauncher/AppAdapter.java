package com.tinku.glasslauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

    /** App icon, optionally turned into a single-colour (monochrome) icon. */
    public static Drawable iconOf(Context ctx, ResolveInfo info) {
        Drawable d = info.loadIcon(ctx.getPackageManager());
        SharedPreferences p = ctx.getSharedPreferences("tinku", Context.MODE_PRIVATE);
        if (!p.getBoolean("mono", false)) {
            return d;
        }
        if (Build.VERSION.SDK_INT >= 33 && d instanceof AdaptiveIconDrawable) {
            Drawable m = ((AdaptiveIconDrawable) d).getMonochrome();
            if (m != null) {
                m = m.mutate();
                m.setTint(0xFFFFFFFF);
                return new InsetDrawable(m, -0.2f);
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
        icon.setImageDrawable(iconOf(context, info));

        return convertView;
    }
}
