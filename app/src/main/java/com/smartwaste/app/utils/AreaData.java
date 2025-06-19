package com.smartwaste.app.utils;

public class AreaData {
    public double lat;
    public double lng;
    public String areaName;
    public int trashCount;

    public AreaData(double lat, double lng, String areaName, int trashCount) {
        this.lat = lat;
        this.lng = lng;
        this.areaName = areaName;
        this.trashCount = trashCount;
    }
}