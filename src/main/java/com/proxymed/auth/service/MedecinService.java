package com.proxymed.auth.service;

import com.proxymed.auth.service.model.MedecinJuniorModel;

import java.util.List;

/**
 * Consultation des medecins par un MEDECIN_SENIOR. Ne manipule que des comptes : aucune donnee medicale.
 */
public interface MedecinService {

    /** Comptes MEDECIN_JUNIOR actifs, tries par nom ; les comptes desactives sont exclus. */
    List<MedecinJuniorModel> listerJuniorsActifs();
}
