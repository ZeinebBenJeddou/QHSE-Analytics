package com.QHSEAnalytics.importer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ImportProgressService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String clientId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            emitter.complete();
        });
        emitter.onError(e -> emitters.remove(clientId));
        emitters.put(clientId, emitter);
        log.debug("[ImportProgress] Emitter registered for clientId={}", clientId);
        return emitter;
    }

    public void push(String clientId, String stage, int percent, String message) {
        if (clientId == null || clientId.isBlank()) return;
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(Map.of("stage", stage, "percent", percent, "message", message)));
            if (percent >= 100) {
                emitter.complete();
                emitters.remove(clientId);
            }
        } catch (IOException e) {
            log.debug("[ImportProgress] Emitter closed for clientId={}", clientId);
            emitters.remove(clientId);
        }
    }

    public void pushError(String clientId, String message) {
        if (clientId == null || clientId.isBlank()) return;
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("import-error")
                    .data(Map.of("stage", "ERREUR", "percent", 0, "message", message)));
            emitter.complete();
        } catch (IOException e) {
            log.debug("[ImportProgress] Emitter closed on error for clientId={}", clientId);
        } finally {
            emitters.remove(clientId);
        }
    }
}
