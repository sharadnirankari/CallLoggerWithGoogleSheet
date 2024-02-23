package com.airxstudio.calllogger;

public class CallLogModel {
    private String number;
    private String name;
    private String callType;
    private String date;

    public CallLogModel() {
    }

    public CallLogModel(String number, String name, String callType, String date) {
        this.number = number;
        this.name = name;
        this.callType = callType;
        this.date = date;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

