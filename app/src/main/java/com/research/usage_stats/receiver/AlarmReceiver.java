package com.research.usage_stats.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.research.usage_stats.service.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, AlarmService.class));
    }
}
