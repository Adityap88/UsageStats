package com.research.usage_stats.storage;

import java.util.ArrayList;
import java.util.List;

public class Date {
    String date;
    String totalTime;
//    String unlockCount;
    String total_DataUsed;
    List<Apps> app= new ArrayList<>();

    public Date(){

    }


//    public Date(String date, String totalTime, String unlockCount, String total_DataUsed, List<Apps> app) {
//        this.date = date;
//        this.totalTime = totalTime;
//        this.unlockCount = unlockCount;
//        this.total_DataUsed = total_DataUsed;
//        this.app = app;
//    }


    public Date(String date, String totalTime, String total_DataUsed, List<Apps> app) {
        this.date = date;
        this.totalTime = totalTime;
        this.total_DataUsed = total_DataUsed;
        this.app = app;
    }

    public String getTotal_DataUsed() {
        return total_DataUsed;
    }

    public String getDate() {
        return date;
    }

    public String getTotalTime() {
        return totalTime;
    }

//    public String getUnlockCount() {
//        return unlockCount;
//    }

    public List<Apps> getAppList() {
        return app;
    }
}
