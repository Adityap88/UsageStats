package com.research.usage_stats.app;

import android.app.Application;
import android.content.Intent;

import com.research.usage_stats.R;
import com.research.usage_stats.AppConst;
import com.research.usage_stats.MyNotificationManager;
import com.research.usage_stats.data.AppItem;
import com.research.usage_stats.data.DataManager;
import com.research.usage_stats.db.DbHistoryExecutor;
import com.research.usage_stats.db.DbIgnoreExecutor;
import com.research.usage_stats.service.AppService;
import com.research.usage_stats.util.CrashHandler;
import com.research.usage_stats.util.PreferenceManager;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    public static MyNotificationManager notificationManager;
    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.init(this);
        getApplicationContext().startService(new Intent(getApplicationContext(), AppService.class));
        DbIgnoreExecutor.init(getApplicationContext());
        DbHistoryExecutor.init(getApplicationContext());
        DataManager.init();
        addDefaultIgnoreAppsToDB();
        if (AppConst.CRASH_TO_FILE) CrashHandler.getInstance().init();
        notificationManager = MyNotificationManager.getInstance(this);
        notificationManager.registerNotificationChannelChannel(
                getString(R.string.channelId),
                "BackgroundService",
                "BackgroundService");

    }

    public static MyNotificationManager getMyAppsNotificationManager(){
        return notificationManager;
    }



    private void addDefaultIgnoreAppsToDB() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> mDefaults = new ArrayList<>();
                mDefaults.add("com.android.settings");
                mDefaults.add(getPackageName());
                for (String packageName : mDefaults) {
                    AppItem item = new AppItem();
                    item.mPackageName = packageName;
                    item.mEventTime = System.currentTimeMillis();
                    DbIgnoreExecutor.getInstance().insertItem(item);
                }
            }
        }).run();
    }
}
