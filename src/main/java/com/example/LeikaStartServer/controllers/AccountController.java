package com.example.LeikaStartServer.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.LeikaStartServer.Settings;
import com.example.LeikaStartServer.datebase.AccountRepository;
import com.example.LeikaStartServer.datebase.LeikaRepository;
import com.example.LeikaStartServer.objects.AccountObject;
import com.example.LeikaStartServer.objects.AccountObjectToSend;
import com.example.LeikaStartServer.objects.LeikaObject;
import com.example.LeikaStartServer.objects.RoleObject;
import com.example.LeikaStartServer.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    private final AccountRepository accountRepository;
    private final LeikaRepository leikaRepository;

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/all_accounts") //получить все аккаунты
    public ResponseEntity<List<AccountObject>> getUsers(Authentication authentication, Principal principal) {
        String username = authentication.getName();
        return ResponseEntity.ok().body(accountService.getAccounts());
    }

    /*@GetMapping("/find_account") //получить аккаунт по email
    public AccountObject findAccount(@RequestParam String username) {
        log.info("Getting of account with username {}", username);
        return accountRepository.findAccountByUsername(username).get();
    }*/

    @GetMapping("/get_account") //получить аккаунт по id
    public /*AccountObject*/ AccountObjectToSend getAccount(Authentication authentication, Principal principal) {
        String id = authentication.getName();
        log.info("Getting of account with id {}", id);
        if(accountRepository.findAccountByAccountId(id).isPresent()){
            AccountObject account = accountRepository.findAccountByAccountId(id).get();
            List<String> leikasId  = account.getLeikasId();
            Collection<LeikaObject> leikas = new ArrayList<>();
            for(int i = 0; i < leikasId.size(); i++){
                LeikaObject leika = leikaRepository.findLeikaObjectByLeikaId(leikasId.get(i)).get();
                leikas.add(leika);
            }
            AccountObjectToSend accountToSend = new AccountObjectToSend(account.getCreated(), leikas);
            //return accountRepository.findAccountByAccountId(id).get();
            return accountToSend;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Такого аккаунта не найдено");
        }
    }

    @PostMapping("/add_account") //добавить новый аккаунт  //можно без токена
    public void addAccount(@RequestParam String username, String password) {
        if(username != null && password != null) {
            if(accountRepository.findAccountByUsername(username).isPresent()) {
                Optional<AccountObject> account = accountRepository.findAccountByUsername(username);
                String takenEmail = account.get().getTakenEmail();
                if(takenEmail == null) {
                    throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, "Такой аккаунт уже существует");
                } else {
                    String subject = "Подтверждение регистрации приложения 'Лейка'";
                    String text = "Перейдите по данной ссылке для подтверждения регистрации в приложении 'Лейка'" +
                            " https://"+ Settings.HOST + "/users/confirm?token=" + takenEmail;
                    boolean result = sendLetter(username, subject, text);
                    if(result){
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Не подтверждена почта. Отправлено повторное письмо");
                    } else {
                        //Письмо не отправилось
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Письмо подтверждения не отправилось");
                    }
                }
            } else {
                String takenEmail = createEmailToken();
                String subject = "Подтверждение регистрации приложения 'Лейка'";
                String text = "Перейдите по данной ссылке для подтверждения регистрации в приложении 'Лейка'" +
                        " https://"+ Settings.HOST + "/users/confirm?token=" + takenEmail;
                boolean result = sendLetter(username, subject, text); //сделать отдельным потоком
                if(result){
                    AccountObject account = new AccountObject(username, password + Settings.PASSWORD_STATIC_VALUE, takenEmail);
                    log.info("Adding of new account with email {}", account.getUsername());
                    account.setPassword(passwordEncoder.encode(account.getPassword()));
                    accountRepository.insert(account);
                    throw new ResponseStatusException(HttpStatus.CREATED, "Аккаунт создан. Письмо для подтверждения отправлено на почту");
                }  else {
                    //Письмо не отправилось
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Письмо подтверждения не отправилось");
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не хватает данных");
        }
    }

    @PostMapping("/request_password") //запрос на смену пароля если забыл через почту (доступно без access taken) //можно без токена
    public void requestPasswordForgot(@RequestParam String username, String newPassword) {
        Optional<AccountObject> accountOptional = accountRepository.findAccountByUsername(username);
        log.info("Request to change password of account {}", username);
        creatingNewPassword(accountOptional, newPassword);
    }

    @PostMapping("/new_password") //запрос на смену пароля если решил просто поменять (не доступно без access taken)
    public void requestPasswordChange(Authentication authentication, Principal principal, @RequestParam String newPassword) { //Запрашивать еще и старый пароль
        String id = authentication.getName();
        log.info("Request to change password of account {}", id);
        Optional<AccountObject> accountOptional = accountRepository.findAccountByAccountId(id);
        AccountObject account = accountOptional.get();
        creatingNewPassword(accountOptional, newPassword);
    }

    public void creatingNewPassword (Optional<AccountObject> accountOptional, String newPassword) {
        if(accountOptional.isEmpty()) {
            log.error("Account not found in database");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Аккаунта с данной почтой не существует");
        } else {
            AccountObject account = accountOptional.get();
            account.setNewPassword(passwordEncoder.encode(newPassword + Settings.PASSWORD_STATIC_VALUE));
            String takenEmail = createEmailToken();
            account.setTakenEmailNewPassword(takenEmail);

            String subject = "Восстановление пароля от приложения 'Лейка'";
            String text = "Перейдите по данной ссылке для подтверждения нового пароля от приложения 'Лейка'." +
                    " https://"+ Settings.HOST + "/users/change_password?token=" + takenEmail;
            boolean result = sendLetter(account.getUsername(), subject, text);

            if(result){
                accountRepository.save(account);
                throw new ResponseStatusException(HttpStatus.CREATED, "Письмо для подтверждения смены пароля отправлено на почту");
            }  else {
                //Письмо не отправилось
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Письмо подтверждения не отправилось");
            }
        }
    }

    @GetMapping("/change_password")  //Подтверждение смены пароля через почту //можно без токена
    public String passwordChange(@RequestParam String token)  {
        Optional<AccountObject> accountOptional = accountRepository.findAccountByTakenEmailNewPassword(token);
        if(accountOptional.isPresent()) {
            AccountObject account = accountOptional.get();
            String newPassword = account.getNewPassword();
            account.setPassword(newPassword);
            account.setTakenEmailNewPassword(null);
            account.setNewPassword(null);


            //Удаление и создание нового аккаунта при смене пароля для того чтобы сменить id. Токены станут недействительными
            account.setAccountId(null);
            accountRepository.deleteAccountObjectByTakenEmailNewPassword(token);
            accountRepository.insert(account);
            //accountRepository.save(account);
            throw new ResponseStatusException(HttpStatus.ACCEPTED, "Пароль успешно изменен");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Неверный или устаревший токен");
        }

    }

    /*@PostMapping("/add_role") //добавить роль
    public ResponseEntity<RoleObject> addRole(@RequestBody RoleObject role) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/users/add_role").toUriString());
        return ResponseEntity.created(uri).body(accountService.addRole(role));
    }

    @PostMapping("/role_to_user")
    public ResponseEntity<?> addRoleToAccount(@RequestBody RoleToAccountForm form) {
        accountService.addRoleToAccount(form.getIdAccount(), form.getRoleName());
        return ResponseEntity.ok().build();
    }*/

    @PostMapping("/taken/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256(Settings.KEY.getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refresh_token);
                String id = decodedJWT.getSubject();

                AccountObject account = accountService.getAccount(id);
                String access_taken = JWT.create()
                        .withSubject(account.getAccountId())
                        .withExpiresAt(new Date((System.currentTimeMillis()) + Settings.ACCESS_TAKEN_MIN * 60 * 1000))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("roles", account.getRoles().stream().map(RoleObject::getName).collect(Collectors.toList()))
                        .sign(algorithm);
                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", access_taken);
                //tokens.put("refresh_token", refresh_token);
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            } catch (Exception exception) {
                response.setHeader("error", exception.getMessage());
                response.setStatus(FORBIDDEN.value());
                //response.sendError(FORBIDDEN.value());

                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }

        } else {
            throw new RuntimeException("Refresh token is missing");
        }
    }

    @GetMapping("/confirm")
    public String post(@RequestParam String token)  {  //подтверждение почты можно //без токена
        Optional<AccountObject> account = accountRepository.findAccountByTakenEmail(token);
        if(account.isPresent()){
            account.get().setTakenEmail(null);
            accountRepository.save(account.get());
            throw new ResponseStatusException(HttpStatus.ACCEPTED, "Аккаунт успешно подтвержден");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Аккаунта, привязанного к данной почте не найдено");
        }

    }

    private String createEmailToken(){
        final SecureRandom secureRandom = new SecureRandom();
        final Base64.Encoder base64Encoder = Base64.getUrlEncoder();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token2 = base64Encoder.encodeToString(randomBytes);
        String token1 = token2.replace("=", "");
        return token1.replace("$", "");
    }

    public static boolean sendLetter(String email, String subject, String text)  {
        boolean result = true;

        final Properties properties = new Properties();
        try {
            properties.load(AccountController.class.getClassLoader().getResourceAsStream("mail.properties"));
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        }

        Session mailSession = Session.getDefaultInstance(properties);
        MimeMessage message = new MimeMessage(mailSession);
        try {
            message.setFrom(Settings.EMAIL);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject(subject);
            message.setText(text);
           /* message.setFrom("leika.confirmation@gmail.com");
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject("Подтверждение регистрации приложения 'Лейка'");
            message.setText("Перейдите по данной ссылке для подтверждения регистрации в приложении 'Лейка'" +
                    " https://"+ Settings.HOST + "/users/confirm?token=" + token);*/
        } catch (MessagingException e) {
            result = false;
            e.printStackTrace();
        }

        try {
            Transport tr = mailSession.getTransport();
            tr.connect(null, Settings.EMAIL_PASSWORD);
            tr.sendMessage(message, message.getAllRecipients());
            tr.close();
        } catch (NoSuchProviderException e) {
            result = false;
            e.printStackTrace();
        } catch (MessagingException e) {
            result = false;
            e.printStackTrace();
        }

        return result;
    }

    @PostMapping(value = "/addLeika", consumes = "application/json", produces = "application/json") //добавление нового прибора к аккаунту
    public void addLeika(Authentication authentication, Principal principal, @RequestBody LeikaObject leika) {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaByName(leika.getName());
        log.info("Попытка добавить привязать прибор {}", leika.getName());
        leika.setAccountId(authentication.getName());
        if(leikaOptional.isPresent()) {
            String saveAccountId = leikaOptional.get().getAccountId();
            if(saveAccountId.equals(leika.getAccountId())){
                log.info("Данный прибор уже привязан к этому аккаунту");
                throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, "Данный прибор уже привязан к этому аккаунту");
            } else {
                log.info("Данный прибор уже привязан к другому аккаунту");
                throw new ResponseStatusException(HttpStatus.FOUND, "Данный прибор уже привязан к другому аккаунту");
            }
        } else {
            leikaRepository.insert(leika);
            String newLeikaId = leika.getLeikaId();

            Optional<AccountObject> accountOptional = accountRepository.findAccountByAccountId(authentication.getName());
            accountOptional.get().getLeikasId().add(newLeikaId);
            accountRepository.save(accountOptional.get());

            log.info("Данный прибор успешно привязан к аккаунту {}", authentication.getName());
            throw new ResponseStatusException(HttpStatus.CREATED, "Данный прибор успешно привязан к аккаунту");
        }
    }
}

/*@Data
class RoleToAccountForm {
    private String idAccount;
    private String roleName;
}*/
