package com.research.usage_stats.storage;

import java.util.List;

public class Apps {
    String App_name;
    String Time_Used;
    String Times_Open;
    String Mobile_Data;
    String WIFI;
    List<TimeLine> App_Timeline;

    public Apps(){

    }


    public Apps(String name, String timeUsed, String timeOpen, String dataUsed, String wifi, List<TimeLine> timeline) {
        this.App_name = name;
        this.Time_Used = timeUsed;
        this.Times_Open= timeOpen;
        this.Mobile_Data = dataUsed;
        this.WIFI = wifi;
        this.App_Timeline = timeline;
    }

    public String getApp_name() {
        return App_name;
    }

    public String getTime_Used() {
        return Time_Used;
    }

    public String getTimes_Open() {
        return Times_Open;
    }

    public String getMobile_Data() {
        return Mobile_Data;
    }

    public String getWIFI() {
        return WIFI;
    }

    public List<TimeLine> getApp_Timeline() {
        return App_Timeline;
    }
}
