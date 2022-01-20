package com.example.LeikaStartServer.service;

import com.example.LeikaStartServer.Settings;
import com.example.LeikaStartServer.controllers.AccountController;
import com.example.LeikaStartServer.datebase.AccountRepository;
import com.example.LeikaStartServer.objects.AccountObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountService implements UserDetailsService {


    //Добавить всевозможные операции по добавлению т тзменентю всех объектов (addLeika, saveLeika и тд). Возможно, стоит их разнести по разным сервисам
    private final AccountRepository accountRepository;



    public UserDetails loadUserByUsername(String username) /*throws UsernameNotFoundException*/ {
        Optional<AccountObject> accountOptional = accountRepository.findAccountByUsername(username);
        if(accountOptional.isEmpty()) {
            log.error("Account {} not found in database", username);
            //throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Аккаунта с таким логином нет");
            //throw new UsernameNotFoundException("Account not found in database");
            //UsernameNotFoundException usernameNotFoundException = new UsernameNotFoundException("Аккаунта с таким логином нет");
            //usernameNotFoundException.addSuppressed(new Throwable(String.valueOf(HttpStatus.NOT_FOUND)));
            //throw usernameNotFoundException;

            //throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Аккаунта с таким логином нет");
            throw new UsernameNotFoundException("Account not found in database", new Throwable("В базе данных не найдено")); //основное


            //throw new AuthenticationServiceException("Account not found in database");

            //org.springframework.security.authentication.AuthenticationServiceException
            //org.springframework.security.authentication.Exc

        } else {
            log.error("Account found in database: {}", username);
            String takenEmail = accountOptional.get().getTakenEmail();
            if(takenEmail != null){
                log.error("Account {} has not confirm email", username);

                String subject = "Подтверждение регистрации приложения 'Лейка'";
                String text = "Перейдите по данной ссылке для подтверждения регистрации в приложении 'Лейка'" +
                        " https://"+ Settings.HOST + "/users/confirm?token=" + takenEmail;
                boolean result = AccountController.sendLetter(username, subject, text);

                //throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, "Аккаунт не подтвердил email. На почту повторно отправлено письмо");
                throw new UsernameNotFoundException("Аккаунт не подтвердил email. На почту повторно отправлено письмо",  new Throwable("Account has not confirmed email")); //основное
            }
        }
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        AccountObject account = accountOptional.get();
        account.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getName())));
        return new org.springframework.security.core.userdetails.User(account.getUsername(), account.getPassword(), authorities);
    }

    public AccountObject saveAccount(AccountObject account) {
        log.info("Saving of account with id {}", account.getAccountId());

        return accountRepository.save(account);
    }

    /*public AccountObject addAccount(AccountObject account) {
        log.info("Adding of new account with email {}", account.getUsername());
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        return accountRepository.insert(account);
    }*/

    public AccountObject getAccount(String id) {
        log.info("Getting of  account with username {}", id);
        return accountRepository.findAccountByAccountId(id).get();
    }

    public List<AccountObject> getAccounts() {
        log.info("Taking of all accounts");
        return accountRepository.findAll();
    }

    /*@Override
    public RoleObject addRole(RoleObject role) {
        log.info("Adding role with id {}", role.getRoleId());
        return roleRepository.save(role);
    }

    @Override
    public void addRoleToAccount(String email, String role) {
        log.info("Adding role {} to account with id {}", role, email);
        AccountObject account = accountRepository.findAccountByEmail(email).get();
        RoleObject roleObject = roleRepository.findRoleByName(role).get();
        account.getRoles().add(roleObject);
        saveAccount(account);
    }*/


}
