package com.research.usage_stats;
import android.Manifest;
import android.app.Notification;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;



import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.research.usage_stats.data.AppItem;
import com.research.usage_stats.data.DataManager;
import com.research.usage_stats.storage.Apps;
import com.research.usage_stats.storage.Date;
import com.research.usage_stats.storage.TimeLine;
import com.research.usage_stats.ui.MainActivity;
import com.research.usage_stats.util.AppUtil;
import com.research.usage_stats.util.SortEnum;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FireBase extends Worker {


    SharedPreferences sharedPreferences= getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.roll_number),Context.MODE_PRIVATE);
    String rollNum = sharedPreferences.getString(getApplicationContext().getString(R.string.roll_number),"");
    Context context;
    private final int NOTIFICATION_ID=10;
    MyNotificationManager notificationManager;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
    int mTotal;
    Date date;
    List<Apps> appsList= new ArrayList<>();
    List<AppItem> items= new ArrayList<>();

    public FireBase(@NonNull  Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context=context;
        notificationManager=MyNotificationManager.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        setForegroundAsync(createForeGroundInfo("Data Sync In Process"));
        items=DataManager.getInstance().getApps(getApplicationContext(),1 ,2);
        mTotal = 0;
        for (AppItem item : items) {
            if (item.mUsageTime <= 0) continue;
            mTotal += item.mUsageTime;
        }

        if(!items.isEmpty()){
            String todayDate=String.format(Locale.getDefault(),"%s",new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new java.util.Date(items.get(0).mEventTime)));
            long totalDataUsed=0;
            String totalTime = AppUtil.formatMilliSeconds(mTotal);
            int i=0;
            while(i<items.size()){
                List<TimeLine> timeLines= new ArrayList<>();
                String name= items.get(i).mName;
                List<AppItem> TimeLineItem=DataManager.getInstance().getTargetAppTimeline(getApplicationContext(), items.get(i).mPackageName, 2);
                List<AppItem> newList = new ArrayList<>();
                for (AppItem item : TimeLineItem) {
                    if (item.mEventType == UsageEvents.Event.USER_INTERACTION || item.mEventType == UsageEvents.Event.NONE) {
                        continue;
                    }
                    if (item.mEventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        AppItem newItem = item.copy();
                        newItem.mEventType = -1;
                        newList.add(newItem);
                    }
                    newList.add(item);
                }

                int j=0;
                StringBuilder StartTime=new StringBuilder("");
                StringBuilder EndTime=new StringBuilder("");
                StringBuilder SessionTime=new StringBuilder("");
                while (j<newList.size()){
                    AppItem item= newList.get(j);
                    String desc = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date(item.mEventTime));
                    if(item.mEventType==-1){
                        desc = AppUtil.formatMilliSeconds(item.mUsageTime);
                        SessionTime.append(desc);
                    }else if(item.mEventType==1){
                        StartTime.append(desc);
                    }else if(item.mEventType==2){
                        EndTime.append(desc);
                        TimeLine timeLine= new TimeLine(StartTime.toString(),EndTime.toString(),SessionTime.toString());
                        timeLines.add(timeLine);
                        StartTime.setLength(0);
                        EndTime.setLength(0);
                        SessionTime.setLength(0);

                    }
                    j++;
                }
                long wifiData= getWifi(items.get(i).mPackageName);
                long mobileData=items.get(i).mMobile;
                totalDataUsed+=wifiData+mobileData;
                String wifi=AppUtil.humanReadableByteCount(wifiData);
                String timeUsed=AppUtil.formatMilliSeconds(items.get(i).mUsageTime);
                String timeOpen=String.valueOf(items.get(i).mCount);
                String MobileData=String.format(Locale.getDefault(), "%s", AppUtil.humanReadableByteCount(items.get(i).mMobile));
                Apps apps= new Apps(name,timeUsed,timeOpen,MobileData,wifi,timeLines);
                appsList.add(apps);
                i++;
            }
            String total_DataUsed=AppUtil.humanReadableByteCount(totalDataUsed);
            DatabaseReference myRef = database.getReference(rollNum+"/"+todayDate.replace('.','-').substring(0,11));
            date= new Date(todayDate,totalTime,total_DataUsed,appsList);
            myRef.setValue(date);
        }
        return Result.success();
    }

    private long getWifi(String mPackageName) {
        long totalWifi = 0;
        long totalMobile = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
            int targetUid = AppUtil.getAppUid(getApplicationContext().getPackageManager(), mPackageName);
            long[] range = AppUtil.getTimeRange(SortEnum.getSortEnum(1));
            try {
                if (networkStatsManager != null) {
                    NetworkStats networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI, "", range[0], range[1]);
                    if (networkStats != null) {
                        while (networkStats.hasNextBucket()) {
                            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                            networkStats.getNextBucket(bucket);
                            if (bucket.getUid() == targetUid) {
                                totalWifi += bucket.getTxBytes() + bucket.getRxBytes();
                            }
                        }
                    }
                    TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        NetworkStats networkStatsM = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, null, range[0], range[1]);
                        if (networkStatsM != null) {
                            while (networkStatsM.hasNextBucket()) {
                                NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                                networkStatsM.getNextBucket(bucket);
                                if (bucket.getUid() == targetUid) {
                                    totalMobile += bucket.getTxBytes() + bucket.getRxBytes();
                                }
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return totalWifi;
    }

    private ForegroundInfo createForeGroundInfo(String message){
        Notification notification=notificationManager.getNotification(MainActivity.class,message,1,false,NOTIFICATION_ID);
        return new ForegroundInfo(NOTIFICATION_ID,notification);
    }
}
