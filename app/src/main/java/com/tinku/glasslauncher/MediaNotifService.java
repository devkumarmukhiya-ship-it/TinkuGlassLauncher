package com.tinku.glasslauncher;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Music session padhna (MediaSessionManager) lai notification access chahincha.
 * Tapaile Settings ma yo app ko notification access on garnu parchha.
 *
 * Extra: kunai music app le album art MediaSession ma pathayena, notification ma matra
 * rakhyo bhane, tyo notification ko thulo icon yahibata tanincha.
 */
public class MediaNotifService extends NotificationListenerService {

    /** Music card le sunne: music app ko notification aayo/badliyo bhane card refresh garne */
    static volatile Runnable hook;
    /** Hal bajdai gareko music app ko package (aru app ko notification le card nachalaos) */
    static volatile String watchPkg;
    private static volatile MediaNotifService inst;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Override
    public void onListenerConnected() {
        inst = this;
    }

    @Override
    public void onListenerDisconnected() {
        if (inst == this) inst = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Runnable h = hook;
        String w = watchPkg;
        if (h != null && w != null && sbn != null && w.equals(sbn.getPackageName())) {
            MAIN.post(h);
        }
    }

    /** Yo package ko notification ma bhayeko album art (thulo icon), chhaina bhane null */
    static Bitmap largeIconFor(Context ctx, String pkg) {
        MediaNotifService s = inst;
        if (s == null || pkg == null) return null;
        try {
            StatusBarNotification[] all = s.getActiveNotifications();
            if (all == null) return null;
            for (StatusBarNotification sbn : all) {
                if (!pkg.equals(sbn.getPackageName())) continue;
                Notification n = sbn.getNotification();
                if (n == null) continue;
                Bitmap b = null;
                Icon ic = n.getLargeIcon();
                if (ic != null) {
                    b = toBitmap(ic.loadDrawable(ctx));
                }
                if (b == null && n.extras != null) {
                    Object o = n.extras.get(Notification.EXTRA_LARGE_ICON);
                    if (o instanceof Bitmap) b = (Bitmap) o;
                }
                if (b != null) return b;
            }
        } catch (Exception e) {
            // access chhaina ya notification padhna sakiyena
        }
        return null;
    }

    private static Bitmap toBitmap(Drawable d) {
        if (d == null) return null;
        if (d instanceof BitmapDrawable) return ((BitmapDrawable) d).getBitmap();
        int w = Math.max(1, d.getIntrinsicWidth());
        int h = Math.max(1, d.getIntrinsicHeight());
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return bmp;
    }
}
