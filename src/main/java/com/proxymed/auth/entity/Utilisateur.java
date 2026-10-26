package com.proxymed.auth.entity;

import com.proxymed.auth.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Compte PROXYMED. Pas de @ToString/@Data : le hash du mot de passe ne doit jamais
 * se retrouver dans un log par accident.
 */
@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Toujours stocke en minuscules (voir AuthServiceImpl#normaliserEmail). */
    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt, jamais le mot de passe en clair. */
    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean actif;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;
}
