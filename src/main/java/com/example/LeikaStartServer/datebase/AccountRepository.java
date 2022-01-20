package com.example.LeikaStartServer.datebase;

import com.example.LeikaStartServer.objects.AccountObject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AccountRepository extends MongoRepository<AccountObject, String> {
    Optional<AccountObject> findAccountByAccountId (String accountId);
    Optional<AccountObject> findAccountByUsername(String username);
    Optional<AccountObject> findAccountByTakenEmail (String takenEmail);
    Optional<AccountObject> findAccountByTakenEmailNewPassword (String takenEmailNewPassword);
    Optional<AccountObject> deleteAccountObjectByTakenEmailNewPassword (String takenEmailNewPassword);
}
