package com.example.LeikaStartServer;

import com.example.LeikaStartServer.objects.AccountObject;
import com.example.LeikaStartServer.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class LeikaStartServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeikaStartServerApplication.class, args);
	}

	@Bean
	PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}

	@Bean
	CommandLineRunner run(AccountService accountService){  //Что нужно сделать сразу после запуска
		return args -> {
			System.out.println("Сервер запущен");

			//accountService.addAccount(new AccountObject("mister_byt@mail.ru", "Ceram1443"));
			/*accountService.addRole(new RoleObject((long)12365, "ROLE_USER"));
			//accountService.addRole(new RoleObject(null, "ROLE_MANAGER"));
			accountService.addRole(new RoleObject((long)456, "ROLE_ADMIN"));
			//accountService.addRole(new RoleObject(null, "ROLE_SUPER_ADMIN"));

			accountService.addAccount(new AccountObject(null, "kekich@lol.ru", "sdojpdfbfdg", "sdsdfgd", new ArrayList<>()));
			accountService.addAccount(new AccountObject(null,"mister_byt@mail.ru", "Ceram1443", "sdsdfgd", new ArrayList<>()));
			accountService.addAccount(new AccountObject(null,"dad@lol.ru", "sdojpdfbfdg", "sdsdfgd", new ArrayList<>()));

			accountService.addRoleToAccount("kekich@lol.ru", "ROLE_USER");
			accountService.addRoleToAccount("mister_byt@mail.ru", "ROLE_ADMIN");
			accountService.addRoleToAccount("dad@lol.ru", "ROLE_USER");
			accountService.addRoleToAccount("dad@lol.ru", "ROLE_ADMIN");*/
		};
	}

}
