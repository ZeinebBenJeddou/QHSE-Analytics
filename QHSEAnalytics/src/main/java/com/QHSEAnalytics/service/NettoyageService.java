package com.QHSEAnalytics.service;

import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.entity.StagingDonnee;
import com.QHSEAnalytics.entity.UniteKpi;
import com.QHSEAnalytics.enums.StatutNettoyage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class NettoyageService {

    public StagingDonnee nettoyerDonnee(ExcelParserService.DonneeExtraite donnee, ImportSession session, Kpi kpi) {
        CleanedValue n1 = cleanRawValue(donnee.valeurBruteN1(), kpi.getUnite());
        CleanedValue n = cleanRawValue(donnee.valeurBruteN(), kpi.getUnite());

        StatutNettoyage finalStatus = mostSevere(n1.status(), n.status());
        String note = mergeNotes(n1.note(), n.note());

        return StagingDonnee.builder()
                .importSession(session)
                .kpi(kpi)
                .valeurBruteN1(donnee.valeurBruteN1())
                .valeurBruteN(donnee.valeurBruteN())
                .valeurN1(n1.value())
                .valeurN(n.value())
                .statutNettoyage(finalStatus)
                .noteNettoyage(note)
                .build();
    }

    private CleanedValue cleanRawValue(String rawInput, UniteKpi unite) {
        String raw = rawInput == null ? null : rawInput.trim();
        if (raw == null || raw.isBlank()) {
            return new CleanedValue(null, StatutNettoyage.MANQUANT, "valeur manquante");
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.equals("n/a") || lower.equals("-") || lower.equals("nd") || lower.equals("non disponible")) {
            return new CleanedValue(null, StatutNettoyage.MANQUANT, "valeur manquante");
        }

        String value = raw;
        StatutNettoyage status = StatutNettoyage.OK;
        List<String> notes = new ArrayList<>();

        String withoutSymbols = value.replace("%", "")
                .replace("€", "")
                .replace("$", "")
                .replace("\u00A0", "")
                .replace(" ", "");
        if (!withoutSymbols.equals(value)) {
            value = withoutSymbols;
            status = StatutNettoyage.CORRIGE;
            notes.add("symbole supprimé");
        }

        String noThousands = removeThousandsSeparators(value);
        if (!noThousands.equals(value)) {
            value = noThousands;
            status = StatutNettoyage.CORRIGE;
            notes.add("séparateur milliers supprimé");
        }

        String dotDecimal = value.replace(',', '.');
        if (!dotDecimal.equals(value)) {
            value = dotDecimal;
            status = StatutNettoyage.CORRIGE;
            notes.add("virgule -> point");
        }

        boolean scientific = value.toUpperCase(Locale.ROOT).contains("E");
        Double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return new CleanedValue(null, StatutNettoyage.INVALIDE, "valeur non numérique");
        }

        if (scientific) {
            status = StatutNettoyage.CORRIGE;
            notes.add("notation scientifique convertie");
        }

        if (unite == UniteKpi.POURCENTAGE && parsed >= 0.0d && parsed <= 1.0d) {
            parsed = parsed * 100.0d;
            status = StatutNettoyage.CORRIGE;
            notes.add("décimal -> pourcentage");
        }

        if (isNegativeInvalid(parsed, unite)) {
            return new CleanedValue(null, StatutNettoyage.INVALIDE, "valeur négative impossible");
        }

        if (unite == UniteKpi.POURCENTAGE && parsed > 100.0d) {
            parsed = roundByUnit(parsed, unite);
            notes.add("valeur > 100% à vérifier");
            return new CleanedValue(parsed, StatutNettoyage.SUSPECT, String.join(" | ", notes));
        }

        parsed = roundByUnit(parsed, unite);
        String note = notes.isEmpty() ? null : String.join(" | ", notes);
        return new CleanedValue(parsed, status, note);
    }

    private String removeThousandsSeparators(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace(" ", "");
        if (cleaned.matches("^-?\\d{1,3}(\\.\\d{3})+(,\\d+)?$")) {
            cleaned = cleaned.replace(".", "");
        }
        return cleaned;
    }

    private boolean isNegativeInvalid(Double value, UniteKpi unite) {
        if (value == null) {
            return false;
        }
        return value < 0.0d && (unite == UniteKpi.POURCENTAGE || unite == UniteKpi.NOMBRE || unite == UniteKpi.KWH || unite == UniteKpi.KG);
    }

    private Double roundByUnit(Double value, UniteKpi unite) {
        if (value == null) {
            return null;
        }

        if (unite == UniteKpi.NOMBRE) {
            return (double) Math.round(value);
        }

        if (unite == UniteKpi.POURCENTAGE || unite == UniteKpi.KWH || unite == UniteKpi.KG) {
            return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return value;
    }

    private String mergeNotes(String note1, String note2) {
        if (note1 == null) {
            return note2;
        }
        if (note2 == null || note1.equals(note2)) {
            return note1;
        }
        return note1 + " | " + note2;
    }

    private StatutNettoyage mostSevere(StatutNettoyage a, StatutNettoyage b) {
        if (a == StatutNettoyage.INVALIDE || b == StatutNettoyage.INVALIDE) {
            return StatutNettoyage.INVALIDE;
        }
        if (a == StatutNettoyage.MANQUANT || b == StatutNettoyage.MANQUANT) {
            return StatutNettoyage.MANQUANT;
        }
        if (a == StatutNettoyage.SUSPECT || b == StatutNettoyage.SUSPECT) {
            return StatutNettoyage.SUSPECT;
        }
        if (a == StatutNettoyage.CORRIGE || b == StatutNettoyage.CORRIGE) {
            return StatutNettoyage.CORRIGE;
        }
        if (a == StatutNettoyage.IGNORE || b == StatutNettoyage.IGNORE) {
            return StatutNettoyage.IGNORE;
        }
        return StatutNettoyage.OK;
    }

    private record CleanedValue(Double value, StatutNettoyage status, String note) {}
}
