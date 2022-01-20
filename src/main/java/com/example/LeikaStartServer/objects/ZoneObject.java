package com.example.LeikaStartServer.objects;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;

@Data
public class ZoneObject implements Comparable{
    private boolean zoneActive;
    private int zoneNum;
    private String name;
    int icon;
    boolean onOff;
    int armTime;
    int schTime;

    public ZoneObject(boolean zoneActive, int zoneNum, String name, int icon, boolean onOff, int armTime, int schTime) {

        this.zoneActive = zoneActive;
        this.zoneNum = zoneNum;
        this.name = name;
        this.icon = icon;
        this.onOff = onOff;
        this.armTime = armTime;
        this.schTime = schTime;
    }



    @Override
    public int compareTo(Object o) {
        ZoneObject zone = (ZoneObject) o;
        return this.zoneNum - zone.zoneNum ;
    }
}
