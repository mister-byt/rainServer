package com.example.LeikaStartServer.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.LeikaStartServer.Settings;
import com.example.LeikaStartServer.datebase.AccountRepository;
import com.example.LeikaStartServer.objects.AccountObject;
import com.example.LeikaStartServer.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.client.*;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MappedDocument;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import static com.example.LeikaStartServer.controllers.AccountController.sendLetter;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class CustomAuthentificationFilter extends UsernamePasswordAuthenticationFilter {


    private AccountRepository accountRepository;

    private AccountService accountService;

    private final AuthenticationManager authenticationManager;
    public CustomAuthentificationFilter(AuthenticationManager authenticationManager){
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String username = request.getParameter("username");
        String passwordIn = request.getParameter("password");
        String password = passwordIn + Settings.PASSWORD_STATIC_VALUE;
        log.info("Email is: {}", username); log.info("Password is: {} + PASSWORD_STATIC_VALUE", passwordIn);

        //Optional<AccountObject> account = accountRepository.findAccountByUsername(username);
        //Получение taken для проверки
        MongoClient mongoClient = MongoClients.create();
        MongoDatabase database = mongoClient.getDatabase("accounts");
        MongoCollection<Document> collection = database.getCollection("accountObject");


        Object myDoc = collection.find(new Document("username", new Document("$regex", username))).first();
        if(myDoc == null){
            throw new AuthenticationCredentialsNotFoundException(/*HttpStatus.NOT_FOUND, */"Аккаунта с такой почтой не найдено");
        } else {
            Object myDoc1 = collection.find(new Document("username", new Document("$regex", username))).first().getString("takenEmail");
            if(myDoc1 == null){
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
                return authenticationManager.authenticate(authenticationToken);
            } else {
                String subject = "Подтверждение регистрации приложения 'Лейка'";
                String text = "Перейдите по данной ссылке для подтверждения регистрации в приложении 'Лейка'" +
                        " https://"+ Settings.HOST + "/users/confirm?token=" + myDoc1;
                boolean result = sendLetter(username, subject, text);
                if(result){
                    throw new AuthenticationCredentialsNotFoundException(/*HttpStatus.ALREADY_REPORTED, */"Не подтверждена почта. Отправлено повторное письмо");
                } else {
                    //Письмо не отправилось
                    throw new AuthenticationCredentialsNotFoundException(/*HttpStatus.INTERNAL_SERVER_ERROR,*/ "Письмо подтверждения не отправилось");
                }
            }
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentification) throws IOException, ServletException {
        User user = (User) authentification.getPrincipal();
        Algorithm algorithm = Algorithm.HMAC256(Settings.KEY.getBytes());


        //Получение Id для вставки его в JWT вместо username
        MongoClient mongoClient = MongoClients.create();
        MongoDatabase database = mongoClient.getDatabase("accounts");
        MongoCollection<Document> collection = database.getCollection("accountObject");
        Object myDoc = collection.find(new Document("username", new Document("$regex", user.getUsername()))).first().getObjectId("_id");
        String id = String.valueOf(myDoc);
        log.info("Попытка аутентификации аккаунта с id {}", id);

        String access_taken = JWT.create()
                //.withSubject(user.getUsername()) чтобы токен был с почтой
                .withSubject(id)
                .withExpiresAt(new Date((System.currentTimeMillis()) + Settings.ACCESS_TAKEN_MIN * 60 * 1000))  //время жизни аксесс
                .withIssuer(request.getRequestURL().toString())
                .withClaim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .sign(algorithm);
        String refresh_taken = JWT.create()
                //.withSubject(user.getUsername()) //чтобы токен был с почтой
                .withSubject(id)
                .withExpiresAt(new Date((System.currentTimeMillis()) + Settings.REFRESH_TAKEN_MIN * 60 * 1000))   //время жизни рефреша
                .withIssuer(request.getRequestURL().toString())
                .withClaim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())) //мб убрать
                .sign(algorithm);

        /*response.setHeader("access_token", access_taken);
        response.setHeader("refresh_token", refresh_taken);*/


        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", access_taken);
        tokens.put("refresh_token", refresh_taken);
        tokens.put("id", id);
        response.setContentType(APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getOutputStream(), tokens);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse res, AuthenticationException failed)
            throws IOException, ServletException {

        int inside = res.getStatus();
        String inside1 = failed.getLocalizedMessage();

        String messageResponse = "";
        String id = "";
        int status = 0;
        if(inside1.equals("Аккаунта с такой почтой не найдено")){
            messageResponse = "Account is not found";
            res.setStatus(404);
            status = 404;
        } else if(inside1.equals("Не подтверждена почта. Отправлено повторное письмо")){
            messageResponse = "Email has not confirmed";
            res.setStatus(208);
            status = 208;
        } else if(inside1.equals("Письмо подтверждения не отправилось")) {
            res.setStatus(500);
            messageResponse = "Email error";
            status = 500;
        } else {
            res.setStatus(403);
            messageResponse = "Incorrect password";
            status = 403;
        }
        //org.springframework.security.authentication.AuthenticationServiceException
        res.addHeader("Access-Control-Allow-Origin", "*");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode message = mapper.createObjectNode();
        message.put("success", false);
        message.put("status", status);
        message.put("message", messageResponse);
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        String error = failed.getMessage().split(" ")[0];

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);

        PrintWriter out = res.getWriter();
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        out.print(json);
        out.flush();
    }
}
