package com.example.safeguardapp.Map;

public class LocationResponse {

    private double latitude;
    private double longitude;
    private double battery;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getBattery() { return battery; }

    public void setBattery(double battery) { this.battery = battery; }
}
