--liquibase formatted sql

--changeset Mody:003-1-add-mot-de-passe-temporaire
-- true quand le mot de passe a ete genere par le serveur (compte medecin cree par un ADMIN,
-- ou mot de passe reinitialise) ; repasse a false quand l'utilisateur le change lui-meme.
ALTER TABLE utilisateur ADD COLUMN mot_de_passe_temporaire BOOLEAN NOT NULL DEFAULT FALSE;
--rollback ALTER TABLE utilisateur DROP COLUMN mot_de_passe_temporaire;
