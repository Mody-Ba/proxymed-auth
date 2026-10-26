package com.proxymed.auth.configs;

import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class AdminInitializerTest {

    private static final String EMAIL = "Premier.Admin@Proxymed.Test";
    private static final String MOT_DE_PASSE = "Admin#Test-2026";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UtilisateurRepository repository;
    private AdminInitializer initializer;

    @BeforeEach
    void setUp() {
        repository = mock(UtilisateurRepository.class);
        when(repository.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));
        initializer = new AdminInitializer(repository, passwordEncoder);
    }

    @Test
    void aucunAdminEtVariablesPresentes_creeLAdministrateur(CapturedOutput output) {
        variables(EMAIL, MOT_DE_PASSE);

        initializer.run();

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(repository).save(captor.capture());
        Utilisateur admin = captor.getValue();
        assertThat(admin.getEmail()).isEqualTo("premier.admin@proxymed.test");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isActif()).isTrue();
        assertThat(admin.getDateCreation()).isNotNull();
        assertThat(admin.getMotDePasse()).startsWith("$2a$").isNotEqualTo(MOT_DE_PASSE);
        assertThat(passwordEncoder.matches(MOT_DE_PASSE, admin.getMotDePasse())).isTrue();
        assertThat(output.getAll()).doesNotContain(MOT_DE_PASSE);
    }

    @Test
    void variablesAbsentes_neCreeRienEtAvertit(CapturedOutput output) {
        variables(null, null);

        initializer.run();

        verify(repository, never()).save(any());
        assertThat(output.getAll()).contains("WARN").contains("aucun administrateur cree");
    }

    @Test
    void uneSeuleVariable_neCreeRienEtAvertit(CapturedOutput output) {
        variables(EMAIL, "");

        initializer.run();

        verify(repository, never()).save(any());
        assertThat(output.getAll()).contains("aucun administrateur cree");
    }

    @Test
    void adminDejaPresent_neCreeRien(CapturedOutput output) {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(true);
        variables(EMAIL, MOT_DE_PASSE);

        initializer.run();

        verify(repository, never()).save(any());
        assertThat(output.getAll()).doesNotContain(MOT_DE_PASSE);
    }

    @Test
    void emailDejaPrisParUnAutreCompte_neCreeRien() {
        when(repository.existsByEmail("premier.admin@proxymed.test")).thenReturn(true);
        variables(EMAIL, MOT_DE_PASSE);

        initializer.run();

        verify(repository, never()).save(any());
    }

    @Test
    void motDePasseTropLong_neCreeRien(CapturedOutput output) {
        String tropLong = "x".repeat(73);
        variables(EMAIL, tropLong);

        initializer.run();

        verify(repository, never()).save(any());
        assertThat(output.getAll()).doesNotContain(tropLong);
    }

    /** Simule les variables d'environnement PROXYMED_ADMIN_EMAIL et PROXYMED_ADMIN_PASSWORD. */
    private void variables(String email, String motDePasse) {
        ReflectionTestUtils.setField(initializer, "adminEmail", email);
        ReflectionTestUtils.setField(initializer, "adminPassword", motDePasse);
    }
}
