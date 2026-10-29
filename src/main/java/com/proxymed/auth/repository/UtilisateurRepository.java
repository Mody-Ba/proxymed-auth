package com.proxymed.auth.repository;

import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Page<Utilisateur> findByRole(Role role, Pageable pageable);

    List<Utilisateur> findByRoleAndActifTrue(Role role, Sort sort);
}
