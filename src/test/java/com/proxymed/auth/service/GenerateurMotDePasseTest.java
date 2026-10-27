package com.proxymed.auth.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateurMotDePasseTest {

    private final GenerateurMotDePasse generateur = new GenerateurMotDePasse();

    @Test
    void generer_12CaracteresAvecChaqueFamille() {
        for (int i = 0; i < 200; i++) {
            String motDePasse = generateur.generer();
            assertThat(motDePasse).hasSize(12)
                    .containsPattern("[A-Z]")
                    .containsPattern("[a-z]")
                    .containsPattern("[0-9]")
                    .containsPattern("[#$%&*+\\-=?@]")
                    .doesNotContainPattern("[0O1lI]");
        }
    }

    @Test
    void generer_aleatoire() {
        Set<String> tires = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tires.add(generateur.generer());
        }
        assertThat(tires).hasSize(1000);
    }
}
