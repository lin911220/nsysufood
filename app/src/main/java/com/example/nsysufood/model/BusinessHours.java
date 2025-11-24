package com.example.nsysufood.model;

import java.io.Serializable;

public class BusinessHours implements Serializable {
    private String monday;
    private String tuesday;
    private String wednesday;
    private String thursday;
    private String friday;
    private String saturday;
    private String sunday;

    // Firebase 需要一個空的建構式 (Empty Constructor)
    public BusinessHours() {
    }

    public String getMonday() { return monday; }
    public String getTuesday() { return tuesday; }
    public String getWednesday() { return wednesday; }
    public String getThursday() { return thursday; }
    public String getFriday() { return friday; }
    public String getSaturday() { return saturday; }
    public String getSunday() { return sunday; }
}