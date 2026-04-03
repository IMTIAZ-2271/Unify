package com.Unify.model;
import java.sql.Time;

public class Bus {
    private String busNumber;
    private int groupId; // NEW: Links bus to a group
    private String routeName;
    private Time departureTime;
    private String tripType;
    private String message;

    public Bus(String busNumber, int groupId, String routeName, Time departureTime, String tripType, String message) {
        this.busNumber = busNumber;
        this.groupId = groupId;
        this.routeName = routeName;
        this.departureTime = departureTime;
        this.tripType = tripType;
        this.message = message;
    }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getBusNumber() { return busNumber; }
    public String getRouteName() { return routeName; }
    public Time getDepartureTime() { return departureTime; }
    public String getTripType() { return tripType; }

    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public void setDepartureTime(Time departureTime) { this.departureTime = departureTime; }
    public void setTripType(String tripType) { this.tripType = tripType; }
}