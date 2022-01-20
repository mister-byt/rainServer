package com.example.LeikaStartServer.security;

import com.example.LeikaStartServer.controllers.AccountController;
import com.example.LeikaStartServer.datebase.AccountRepository;
import com.example.LeikaStartServer.filter.CustomAuthentificationFilter;
import com.example.LeikaStartServer.filter.CustomAuthorizationFilter;
import com.example.LeikaStartServer.objects.AccountObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor

public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CustomAuthentificationFilter customAuthentificationFilter = new CustomAuthentificationFilter(authenticationManagerBean());
        customAuthentificationFilter.setFilterProcessesUrl("/users/login"); // переназначить ссылку логина

        http.csrf().disable();


        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.authorizeRequests().antMatchers("/users/login", "/users/token/refresh/**",
                "/users/confirm", "/users/request_password/**", "/users/change_password").permitAll();
        http.authorizeRequests().antMatchers("/users/add_account").permitAll();
        //http.authorizeRequests().antMatchers("/users/all_accounts").permitAll();
        //http.authorizeRequests().antMatchers(GET, "/user/all_accounts").hasAnyAuthority("ROLE_ADMIN");
        http.authorizeRequests().anyRequest().authenticated();
        http.addFilter(customAuthentificationFilter);
        http.addFilterBefore(new CustomAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);



        /*Optional<AccountObject> account = accountRepository.findAccountByUsername(username);
        String takenEmail = account.get().getTakenEmail();

        if(takenEmail == null) {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            return authenticationManager.authenticate(authenticationToken);
        } else {
            boolean result = AccountController.sendLetter(username, takenEmail);
            if(result) {
                throw new ResponseStatusException(HttpStatus.ACCEPTED, "Не подтверждена почта. Отправлено повторное письмо");
            } else {
                //Письмо не отправилось
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Письмо подтверждения не отправилось");
            }
        }*/
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }


}
