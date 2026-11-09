package com.tinku.glasslauncher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

        name.setText(info.loadLabel(pm));
        icon.setImageDrawable(info.loadIcon(pm));

        return convertView;
    }
}
