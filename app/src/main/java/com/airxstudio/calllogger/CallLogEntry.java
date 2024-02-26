package com.airxstudio.calllogger;

public class CallLogEntry {
    private String date;
    private String time;
    private String callType;
    private String phoneNumber;
    private long ringDuration;
    private long callDuration;

    public CallLogEntry(String date, String time, String callType, String phoneNumber, long ringDuration, long callDuration) {
        this.date = date;
        this.time = time;
        this.callType = callType;
        this.phoneNumber = phoneNumber;
        this.ringDuration = ringDuration;
        this.callDuration = callDuration;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getCallType() {
        return callType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getRingDuration() {
        return ringDuration;
    }

    public long getCallDuration() {
        return callDuration;
    }
}
