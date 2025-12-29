package com.usaid.growsmartapplication;

public class HistoryModel {
    public String cropName;
    public String date;
    public String fullResponse;

    // IMPORTANT: Firebase needs this empty constructor
    public HistoryModel() {}

    public HistoryModel(String cropName, String date, String fullResponse) {
        this.cropName = cropName;
        this.date = date;
        this.fullResponse = fullResponse;
    }
}