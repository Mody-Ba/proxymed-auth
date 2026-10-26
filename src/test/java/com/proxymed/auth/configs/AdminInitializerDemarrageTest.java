package com.proxymed.auth.configs;

import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demarrage reel avec les deux variables renseignees (donnees de test) et une base H2 dediee,
 * vide au depart : l'administrateur doit exister une fois l'application demarree.
 * Les autres tests demarrent sans ces variables, ce qui prouve que leur absence ne bloque pas.
 */
@SpringBootTest(properties = {
        "PROXYMED_ADMIN_EMAIL=admin.demarrage@proxymed.test",
        "PROXYMED_ADMIN_PASSWORD=Demarrage#Test-2026",
        "spring.datasource.url=jdbc:h2:mem:admin_initializer;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class AdminInitializerDemarrageTest {

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void auDemarrage_creeLePremierAdministrateur() {
        assertThat(utilisateurRepository.count()).isEqualTo(1);
        Utilisateur admin = utilisateurRepository.findByEmail("admin.demarrage@proxymed.test").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isActif()).isTrue();
        assertThat(passwordEncoder.matches("Demarrage#Test-2026", admin.getMotDePasse())).isTrue();
    }
}
