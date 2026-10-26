package com.proxymed.auth.repository;

import com.proxymed.auth.entity.InscriptionEnAttente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InscriptionEnAttenteRepository extends JpaRepository<InscriptionEnAttente, UUID> {

    Optional<InscriptionEnAttente> findByEmail(String email);
}
