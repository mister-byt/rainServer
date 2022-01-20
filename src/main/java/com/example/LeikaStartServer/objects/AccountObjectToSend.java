package com.example.LeikaStartServer.objects;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class AccountObjectToSend {

    private Collection<LeikaObject> leikas = new ArrayList<>();
    private LocalDateTime created;

    public AccountObjectToSend(LocalDateTime created, Collection<LeikaObject> leikas) {
        this.created = created;
        this.leikas = leikas;
    }
}

