package com.google.android.apps.nexuslauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;

import com.android.launcher3.IconProvider;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.R;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;

import java.util.Calendar;
import java.util.List;

public class DynamicIconProvider extends IconProvider {
    private final BroadcastReceiver mDateChangeReceiver;
    private final Context mContext;
    private final PackageManager mPackageManager;

    public DynamicIconProvider(Context context) {
        mContext = context;
        mDateChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (UserHandle user : UserManagerCompat.getInstance(context).getUserProfiles()) {
                    LauncherModel model = LauncherAppState.getInstance(context).getModel();
                    model.onPackageChanged("com.google.android.calendar", user);
                    List<ShortcutInfoCompat> shortcuts = DeepShortcutManager.getInstance(context).queryForPinnedShortcuts("com.google.android.calendar", user);
                    if (!shortcuts.isEmpty()) {
                        model.updatePinnedShortcuts("com.google.android.calendar", shortcuts, user);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter("android.intent.action.DATE_CHANGED");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        mContext.registerReceiver(mDateChangeReceiver, intentFilter, null, new Handler(LauncherModel.getWorkerLooper()));
        mPackageManager = mContext.getPackageManager();
    }

    private int getDayOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
    }

    private int getDayResId(Bundle bundle, Resources resources) {
        if (bundle != null) {
            int dateArrayId = bundle.getInt("com.google.android.calendar.dynamic_icons_nexus_round", 0);
            if (dateArrayId != 0) {
                try {
                    TypedArray dateIds = resources.obtainTypedArray(dateArrayId);
                    int dateId = dateIds.getResourceId(getDayOfMonth(), 0);
                    dateIds.recycle();
                    return dateId;
                } catch (Resources.NotFoundException ex) {
                }
            }
        }
        return 0;
    }

    private boolean isCalendar(String s) {
        return "com.google.android.calendar".equals(s);
    }

    @Override
    public Drawable getIcon(LauncherActivityInfo launcherActivityInfo, int iconDpi, boolean flattenDrawable) {
        Drawable drawable = null;
        String packageName = launcherActivityInfo.getApplicationInfo().packageName;
        if (isCalendar(packageName)) {
            try {
                Bundle metaData = mPackageManager.getActivityInfo(launcherActivityInfo.getComponentName(), PackageManager.GET_META_DATA | PackageManager.GET_UNINSTALLED_PACKAGES).metaData;
                Resources resourcesForApplication = mPackageManager.getResourcesForApplication(packageName);
                int dayResId = getDayResId(metaData, resourcesForApplication);
                if (dayResId != 0) {
                    drawable = resourcesForApplication.getDrawableForDensity(dayResId, iconDpi);
                }
            } catch (NameNotFoundException e) {
            }
        }
        return drawable == null ? super.getIcon(launcherActivityInfo, iconDpi, flattenDrawable) : drawable;
    }

    @Override
    public String getIconSystemState(final String s) {
        return isCalendar(s) ? mSystemState + " " + getDayOfMonth() : mSystemState;
    }
}
