package com.example.LeikaStartServer.objects;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@Document
public class AccountObject {

    @Id
    private String accountId;
    private String username;
    private String password;
    private String newPassword;
    //private Collection<LeikaObject> leikas = new ArrayList<>();
    private List<String> leikasId = new ArrayList<>();
    private String takenEmail;
    private String takenEmailNewPassword;
    //private int countDevices;
    private LocalDateTime created;
    private Collection<RoleObject> roles = new ArrayList<>();

    public AccountObject(String username, String password, String takenEmail) {
        this.username = username;
        this.password = password;
        this.takenEmail = takenEmail;
        this.created = LocalDateTime.now();
        this.roles.add(new RoleObject((long)1, "ROLE_USER"));
    }
}

