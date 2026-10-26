--liquibase formatted sql

--changeset Mody:001-1-create-utilisateur
-- Les comptes de demonstration ne sont pas inseres ici : leurs mots de passe sont chiffres
-- avec BCrypt au demarrage (ComptesDemoService), a partir de application.properties.
CREATE TABLE utilisateur
(
    id             UUID                     NOT NULL,
    email          VARCHAR(255)             NOT NULL,
    mot_de_passe   VARCHAR(100)             NOT NULL,
    nom_complet    VARCHAR(255)             NOT NULL,
    role           VARCHAR(30)              NOT NULL,
    actif          BOOLEAN                  NOT NULL DEFAULT TRUE,
    date_creation  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT utilisateur_pkey PRIMARY KEY (id),
    CONSTRAINT utilisateur_email_key UNIQUE (email),
    CONSTRAINT utilisateur_role_check CHECK (role IN ('UTILISATEUR', 'MEDECIN_JUNIOR', 'MEDECIN_SENIOR', 'ADMIN'))
);
--rollback DROP TABLE utilisateur;
