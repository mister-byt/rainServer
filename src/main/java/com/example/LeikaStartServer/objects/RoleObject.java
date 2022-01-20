package com.example.LeikaStartServer.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
@AllArgsConstructor
public class RoleObject {

    @Id
    private Long roleId;
    private String name;

    public RoleObject(){
        this.roleId = roleId;
        this.name = name;
    }
}
