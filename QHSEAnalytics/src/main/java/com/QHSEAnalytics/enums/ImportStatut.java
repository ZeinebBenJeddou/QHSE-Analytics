package com.QHSEAnalytics.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum ImportStatut {
    EN_ATTENTE,
    EN_TRAITEMENT,
    @Deprecated
    IMPORTED,
    CALCULATED,
    READY_FOR_AI,
    TRAITE,
    ERREUR;

    public static ImportStatut initial() {
        return EN_ATTENTE;
    }

    public static ImportStatut processing() {
        return EN_TRAITEMENT;
    }

    public static boolean isValid(String statut) {
        if (statut == null || statut.isBlank()) {
            return false;
        }
        try {
            valueOf(statut);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static Set<String> valuesAsStrings() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.toSet());
    }

    public static String valuesForCheckConstraint() {
        return Arrays.stream(values())
                .map(Enum::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));
    }

    public Set<ImportStatut> nextStates() {
        return switch (this) {
            case EN_ATTENTE -> Set.of(EN_TRAITEMENT, ERREUR);
            case EN_TRAITEMENT -> Set.of(CALCULATED, ERREUR);
            case CALCULATED -> Set.of(READY_FOR_AI, ERREUR);
            case READY_FOR_AI -> Set.of(TRAITE, ERREUR);
            case TRAITE -> Set.of();
            case ERREUR -> Set.of();
            case IMPORTED -> Set.of(EN_ATTENTE, EN_TRAITEMENT, ERREUR);
        };
    }

    public boolean canTransitionTo(ImportStatut next) {
        return next != null && nextStates().contains(next);
    }

    public boolean isTerminal() {
        return this == TRAITE || this == ERREUR;
    }

    public boolean isComplete() {
        return this == CALCULATED || this == READY_FOR_AI || this == TRAITE;
    }

    public boolean canViewComparatif() {
        return isComplete();
    }

    public boolean canViewGraphiques() {
        return isComplete();
    }

    public boolean canViewAnalysesIa() {
        return this == READY_FOR_AI || this == TRAITE;
    }

    public boolean isFailed() {
        return this == ERREUR;
    }

    public boolean isProcessing() {
        return this == EN_ATTENTE || this == EN_TRAITEMENT;
    }

    public String toUserMessage() {
        return switch (this) {
            case EN_ATTENTE, EN_TRAITEMENT -> "Import en cours de traitement.";
            case ERREUR -> "Import échoué.";
            case CALCULATED -> "Données prêtes mais analyse IA non disponible.";
            case READY_FOR_AI, TRAITE -> "Analyse disponible.";
            default -> this.name();
        };
    }

    public boolean isReadyForAi() {
        return canViewAnalysesIa();
    }
}
