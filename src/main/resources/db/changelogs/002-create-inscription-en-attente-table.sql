--liquibase formatted sql

--changeset Mody:002-1-create-inscription-en-attente
-- Inscription d'un UTILISATEUR en attente de validation du code recu par email.
-- Le compte (table utilisateur) n'est cree qu'apres validation du code ; la ligne est alors supprimee.
-- Le mot de passe et le code sont haches avec BCrypt, jamais stockes en clair.
CREATE TABLE inscription_en_attente
(
    id             UUID                     NOT NULL,
    email          VARCHAR(255)             NOT NULL,
    nom_complet    VARCHAR(255)             NOT NULL,
    mot_de_passe   VARCHAR(100)             NOT NULL,
    code_hash      VARCHAR(100)             NOT NULL,
    code_expire_le TIMESTAMP WITH TIME ZONE NOT NULL,
    essais_echoues INTEGER                  NOT NULL DEFAULT 0,
    dernier_envoi  TIMESTAMP WITH TIME ZONE NOT NULL,
    date_creation  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT inscription_en_attente_pkey PRIMARY KEY (id),
    CONSTRAINT inscription_en_attente_email_key UNIQUE (email)
);
--rollback DROP TABLE inscription_en_attente;
