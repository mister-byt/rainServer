package com.example.LeikaStartServer.datebase;

import com.example.LeikaStartServer.objects.AccountObject;
import com.example.LeikaStartServer.objects.LeikaObject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LeikaRepository extends MongoRepository<LeikaObject, String> {
    Optional<LeikaObject> findLeikaByName (String name); //или email
    Optional<LeikaObject> findLeikaObjectByLeikaId (String leikaId);
}
