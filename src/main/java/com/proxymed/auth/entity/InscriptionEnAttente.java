package com.proxymed.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Inscription en attente de validation du code envoye par email. Le compte Utilisateur n'est
 * cree qu'apres validation. Pas de @ToString/@Data : les hashes ne doivent jamais etre logges.
 */
@Entity
@Table(name = "inscription_en_attente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscriptionEnAttente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Toujours stocke en minuscules (voir AuthServiceImpl#normaliserEmail). */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    /** Hash BCrypt du mot de passe choisi, recopie tel quel dans Utilisateur a la validation. */
    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    /** Hash BCrypt du code a 6 chiffres. */
    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "code_expire_le", nullable = false)
    private Instant codeExpireLe;

    @Column(name = "essais_echoues", nullable = false)
    private int essaisEchoues;

    @Column(name = "dernier_envoi", nullable = false)
    private Instant dernierEnvoi;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;
}
