package com.proxymed.auth.service.model;

import java.util.List;

/**
 * Page de resultats renvoyee par les listes paginees.
 */
public record PageResponse<T>(
        List<T> contenu,
        int page,
        int taille,
        long totalElements,
        int totalPages
) {
}
