package com.example.LeikaStartServer.objects;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Collection;


@Data
@Document
public class LeikaObject {
    @Id
    private String leikaId;
    private String accountId;
    private String mac;
    private String name;
    private ArrayList<ZoneObject>  zones = new ArrayList<ZoneObject>();
    private int priborClock; //время на устройстве
    private long priborOnline; //последнее подключение
    private int wateringType;
    private Collection<String> messages = new ArrayList<>();
    private int timer;
    private Collection<Integer> wateringTime = new ArrayList<>();
    private Collection<Integer[]> wateringZones = new ArrayList<>();

    public LeikaObject(String leikaId, String accountId, String mac, String name, ArrayList<ZoneObject> zones, int priborClock,
                       long priborOnline, int wateringType, Collection<String> messages, int timer,
                       Collection<Integer> wateringTime, Collection<Integer[]> wateringZones) {

        this.leikaId = leikaId;
        this.accountId = accountId;
        this.mac = mac;
        this.name = name;
        this.zones = zones;
        this.priborClock = priborClock;
        this.priborOnline = priborOnline;
        this.wateringType = wateringType;
        this.messages = messages;
        this.timer = timer;
        this.wateringTime = wateringTime;
        this.wateringZones = wateringZones;
    }
}
