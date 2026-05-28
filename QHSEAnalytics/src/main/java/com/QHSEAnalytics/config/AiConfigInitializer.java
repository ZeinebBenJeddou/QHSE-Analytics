package com.QHSEAnalytics.config;

import com.QHSEAnalytics.shared.entity.AiConfig;
import com.QHSEAnalytics.shared.repository.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initialise la configuration IA au démarrage.
 * Idempotent : ne recrée pas les clés déjà présentes.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class AiConfigInitializer implements CommandLineRunner {

    private final AiConfigRepository aiConfigRepository;

    @Override
    public void run(String... args) {
        List<AiConfig> defaults = List.of(
            AiConfig.builder()
                .key("groq.temperature.json")
                .value("0.1")
                .description("Température Groq pour les réponses JSON (0.0–2.0)")
                .build(),
            AiConfig.builder()
                .key("groq.temperature.text")
                .value("0.5")
                .description("Température Groq pour les réponses texte (0.0–2.0)")
                .build(),
            AiConfig.builder()
                .key("groq.timeout.seconds")
                .value("30")
                .description("Timeout des appels Groq en secondes")
                .build()
        );

        int created = 0;
        for (AiConfig config : defaults) {
            if (!aiConfigRepository.existsById(config.getKey())) {
                aiConfigRepository.save(config);
                created++;
            }
        }
        log.info("Config IA initialisée : {} entrées créées", created);
    }
}
