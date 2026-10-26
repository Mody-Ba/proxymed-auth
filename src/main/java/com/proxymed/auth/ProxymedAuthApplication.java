package com.proxymed.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * UserDetailsServiceAutoConfiguration est exclue : sans cela, Spring Boot cree un compte
 * "user" en memoire et ecrit son mot de passe genere dans les logs au demarrage.
 * Ici, seuls les comptes de la table utilisateur existent.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ProxymedAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxymedAuthApplication.class, args);
	}

}
