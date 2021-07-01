package com.research.usage_stats.storage;

import java.util.List;

public class TimeLine {

    String startTime;
    String endTime;
    String usageTime;

    public TimeLine(){

    }

    public TimeLine(String startTime, String endTime, String usageTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.usageTime = usageTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getUsageTime() {
        return usageTime;
    }
}
