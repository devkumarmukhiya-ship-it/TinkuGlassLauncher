package com.tinku.glasslauncher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

/** One long list: a big letter header followed by that letter's apps. */
public class DrawerAdapter extends BaseAdapter {

    public static class Row {
        public final String header;
        public final ResolveInfo app;

        private Row(String header, ResolveInfo app) {
            this.header = header;
            this.app = app;
        }

        public static Row header(String key) {
            return new Row(key, null);
        }

        public static Row app(ResolveInfo info) {
            return new Row(null, info);
        }
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;

    private final Context context;
    private final PackageManager pm;
    private final List<Row> rows;

    public DrawerAdapter(Context context, List<Row> rows) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.rows = rows;
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header != null ? TYPE_HEADER : TYPE_APP;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return rows.get(position).app != null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Row row = rows.get(position);
        if (row.header != null) {
            TextView tv;
            if (convertView instanceof TextView) {
                tv = (TextView) convertView;
            } else {
                tv = new TextView(context);
                tv.setTextColor(0x99FFFFFF);
                tv.setTextSize(52);
                tv.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                tv.setGravity(Gravity.LEFT);
                float d = context.getResources().getDisplayMetrics().density;
                tv.setPadding(0, (int) (18 * d), 0, (int) (4 * d));
                tv.setLayoutParams(new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            tv.setText(row.header);
            return tv;
        }

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_app_icon, parent, false);
        }
        ImageView icon = convertView.findViewById(R.id.imageViewAppIcon);
        TextView name = convertView.findViewById(R.id.textViewAppName);
        name.setText(AppAdapter.labelOf(context, row.app));
        icon.setImageDrawable(row.app.loadIcon(pm));
        return convertView;
    }
}
