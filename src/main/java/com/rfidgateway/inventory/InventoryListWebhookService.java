package com.rfidgateway.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rfidgateway.model.EpcPresenceState;
import com.rfidgateway.model.InventorySystem;
import com.rfidgateway.model.InventorySystemEpcState;
import com.rfidgateway.repository.InventorySystemEpcStateRepository;
import com.rfidgateway.repository.InventorySystemRepository;
import com.rfidgateway.websocket.EventWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InventoryListWebhookService {

    public static final String TYPE_UPDATE = "INVENTORY_LIST_UPDATE";
    /** Prefijo hex de EPC de prueba: ASCII "TEST". */
    private static final String TEST_EPC_HEX_PREFIX = "54455354";
    private static final String VERSION = "1";
    private static final int[] RETRY_DELAYS_MS = {0, 5_000, 30_000};
    /** Un solo intento en pruebas manuales (feedback inmediato en la UI). */
    private static final int[] MANUAL_SEND_DELAYS_MS = {0};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_INSTANT;

    @Autowired
    private RestTemplate webhookRestTemplate;

    @Autowired
    private InventorySystemRepository inventorySystemRepository;

    @Autowired
    private InventorySystemEpcStateRepository epcStateRepository;

    @Autowired(required = false)
    private EventWebSocketHandler webSocketHandler;

    public String resolveListEventId(InventorySystem system) {
        if (system.getInventoryListWebhookEventId() != null
            && !system.getInventoryListWebhookEventId().isBlank()) {
            return system.getInventoryListWebhookEventId().trim();
        }
        return InventorySystem.DEFAULT_LIST_WEBHOOK_EVENT_ID;
    }

    public Map<String, Object> buildSnapshot(UUID systemId) {
        return inventorySystemRepository.findById(systemId)
            .map(system -> buildPayload(system, List.of(), List.of()))
            .orElse(Map.of());
    }

    @Async
    public void dispatchUpdate(UUID systemId, Collection<String> added, Collection<String> removed) {
        publishListUpdate(systemId, added, removed, false, false);
    }

    @Async
    public void dispatchFullSync(UUID systemId) {
        publishListUpdate(systemId, List.of(), List.of(), true, false);
    }

    @Async
    public void dispatchFullSyncForce(UUID systemId) {
        publishListUpdate(systemId, List.of(), List.of(), true, true);
    }

    /** SYNC forzado síncrono: espera el POST y devuelve el resultado HTTP. */
    public WebhookSendResult dispatchFullSyncForceSync(UUID systemId) {
        InventorySystem system = inventorySystemRepository.findById(systemId)
            .orElseThrow(() -> new IllegalArgumentException("Sistema no encontrado"));
        if (!hasWebhookUrl(system)) {
            throw new IllegalStateException("Configurá la URL del webhook antes de forzar SYNC");
        }
        Map<String, Object> payload = buildPayload(system, List.of(), List.of());
        if (webSocketHandler != null) {
            webSocketHandler.sendInventoryListUpdate(payload);
        }
        return sendWithRetries(system, payload, MANUAL_SEND_DELAYS_MS);
    }

    /**
     * Prueba artificial: agrega/quita etiquetas TEST* al azar en BD y fuerza POST webhook.
     */
    @Transactional
    public WebhookManualDispatchResult dispatchArtificialTest(UUID systemId) {
        InventorySystem system = inventorySystemRepository.findById(systemId)
            .orElseThrow(() -> new IllegalArgumentException("Sistema no encontrado"));
        if (system.getInventoryListWebhookUrl() == null || system.getInventoryListWebhookUrl().isBlank()) {
            throw new IllegalStateException("Configurá la URL del webhook antes de probar");
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<String> testPresent = epcStateRepository
            .findBySystemIdAndPresentTrueOrderByLastSeenAtDesc(systemId)
            .stream()
            .map(InventorySystemEpcState::getEpc)
            .filter(this::isTestEpc)
            .collect(Collectors.toCollection(ArrayList::new));

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        int addCount = rnd.nextInt(1, 4);
        for (int i = 0; i < addCount; i++) {
            String epc = randomTestEpc(rnd);
            int guard = 0;
            while (epcStateRepository.findBySystemIdAndEpc(systemId, epc).isPresent() && guard++ < 20) {
                epc = randomTestEpc(rnd);
            }
            InventorySystemEpcState row = new InventorySystemEpcState();
            row.setSystemId(systemId);
            row.setEpc(epc);
            row.setFirstSeenAt(now);
            row.setLastSeenAt(now);
            row.setPresent(true);
            row.setPresenceState(EpcPresenceState.PRESENT);
            row.setMissedCycles(0);
            row.setLostCycles(0);
            row.setLastReaderId("test-webhook");
            row.setLastAntennaPort((short) 0);
            row.setLastRssi(randomTestRssi(rnd));
            epcStateRepository.save(row);
            added.add(epc);
        }

        if (!testPresent.isEmpty()) {
            Collections.shuffle(testPresent, rnd);
            int removeCount = rnd.nextInt(0, Math.min(3, testPresent.size()) + 1);
            for (int i = 0; i < removeCount; i++) {
                String epc = testPresent.get(i);
                epcStateRepository.findBySystemIdAndEpc(systemId, epc).ifPresent(row -> {
                    row.setPresent(false);
                    row.setPresenceState(EpcPresenceState.LOST);
                    epcStateRepository.save(row);
                });
                removed.add(epc);
            }
        }

        Map<String, Object> payload = buildPayload(system, added, removed);
        if (webSocketHandler != null) {
            webSocketHandler.sendInventoryListUpdate(payload);
        }
        WebhookSendResult webhook = sendWithRetries(system, payload, MANUAL_SEND_DELAYS_MS);
        return new WebhookManualDispatchResult(payload, webhook);
    }

    public void publishListUpdate(
        UUID systemId,
        Collection<String> added,
        Collection<String> removed,
        boolean forceWebhook,
        boolean testMode
    ) {
        inventorySystemRepository.findById(systemId).ifPresent(system -> {
            Map<String, Object> payload = buildPayload(system, added, removed);
            if (webSocketHandler != null) {
                webSocketHandler.sendInventoryListUpdate(payload);
            }
            boolean hasDelta = !normalizeList(added).isEmpty() || !normalizeList(removed).isEmpty();
            boolean canSend = testMode
                ? hasWebhookUrl(system)
                : isActive(system);
            if (canSend && (forceWebhook || hasDelta)) {
                sendWithRetries(system, payload); // fire-and-forget en envíos automáticos
            }
        });
    }

    public String samplePayloadJson(InventorySystem system) throws Exception {
        Map<String, Object> data = baseData(system);
        data.put("generatedAt", "2026-06-03T12:00:00.456Z");
        data.put("count", 6);
        data.put("epcs", List.of(
            "544553540031323100",
            "E2801160600002033A2B2C3D5",
            "E2801160600002033A2B2C3D6"
        ));
        data.put("tags", List.of(
            Map.of("epc", "544553540031323100", "rssi", -48.2, "proximity", "CERCA", "proximityLabel", "Cerca", "readerId", "IHT-1", "antennaPort", 1),
            Map.of("epc", "E2801160600002033A2B2C3D5", "rssi", -62.0, "proximity", "MEDIA", "proximityLabel", "Media", "readerId", "IHT-2", "antennaPort", 2),
            Map.of("epc", "E2801160600002033A2B2C3D6", "rssi", -74.5, "proximity", "LEJOS", "proximityLabel", "Lejos", "readerId", "IHT-1", "antennaPort", 1)
        ));
        data.put("added", List.of("544553540031323100"));
        data.put("removed", List.of());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(basePayload(system, data));
    }

    private Map<String, Object> buildPayload(InventorySystem system, Collection<String> added, Collection<String> removed) {
        UUID systemId = system.getId();
        List<InventorySystemEpcState> presentRows = loadPresentRows(systemId);
        List<String> epcs = presentRows.stream()
            .map(InventorySystemEpcState::getEpc)
            .collect(Collectors.toList());
        List<Map<String, Object>> tags = presentRows.stream()
            .map(RssiProximity::toTagDetail)
            .collect(Collectors.toList());
        Map<String, Object> data = baseData(system);
        data.put("generatedAt", isoFormatter.format(Instant.now()));
        data.put("count", epcs.size());
        data.put("epcs", epcs);
        data.put("tags", tags);
        data.put("added", normalizeList(added));
        data.put("removed", normalizeList(removed));
        return basePayload(system, data);
    }

    private List<InventorySystemEpcState> loadPresentRows(UUID systemId) {
        return epcStateRepository.findBySystemIdAndPresentTrueOrderByLastSeenAtDesc(systemId);
    }

    private List<String> normalizeList(Collection<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String epc : items) {
            if (epc != null && !epc.isBlank()) {
                out.add(epc.trim().toUpperCase());
            }
        }
        return out;
    }

    private Map<String, Object> baseData(InventorySystem system) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("systemId", system.getId().toString());
        data.put("systemName", system.getName());
        return data;
    }

    private Map<String, Object> basePayload(InventorySystem system, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", resolveListEventId(system));
        payload.put("type", TYPE_UPDATE);
        payload.put("version", VERSION);
        payload.put("timestamp", isoFormatter.format(Instant.now()));
        payload.put("data", data);
        return payload;
    }

    private boolean isActive(InventorySystem system) {
        return Boolean.TRUE.equals(system.getInventoryListWebhookEnabled()) && hasWebhookUrl(system);
    }

    private boolean hasWebhookUrl(InventorySystem system) {
        return system.getInventoryListWebhookUrl() != null && !system.getInventoryListWebhookUrl().isBlank();
    }

    private boolean isTestEpc(String epc) {
        return epc != null && epc.toUpperCase().startsWith(TEST_EPC_HEX_PREFIX);
    }

    private String randomTestEpc(ThreadLocalRandom rnd) {
        return asciiToHex("TEST" + rnd.nextInt(1000, 10000));
    }

    private double randomTestRssi(ThreadLocalRandom rnd) {
        int pick = rnd.nextInt(3);
        if (pick == 0) {
            return rnd.nextDouble(-50.0, RssiProximity.NEAR_RSSI_DBM);
        }
        if (pick == 1) {
            return rnd.nextDouble(RssiProximity.MEDIUM_RSSI_DBM, RssiProximity.NEAR_RSSI_DBM);
        }
        return rnd.nextDouble(-82.0, RssiProximity.MEDIUM_RSSI_DBM);
    }

    private String asciiToHex(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(String.format("%02X", (int) c));
        }
        return sb.toString();
    }

    private WebhookSendResult sendWithRetries(InventorySystem system, Map<String, Object> payload) {
        return sendWithRetries(system, payload, RETRY_DELAYS_MS);
    }

    private WebhookSendResult sendWithRetries(InventorySystem system, Map<String, Object> payload, int[] delays) {
        String url = system.getInventoryListWebhookUrl().trim();
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("No se pudo serializar webhook lista {}: {}", system.getId(), e.getMessage());
            return WebhookSendResult.fail(url, 0, 0, "No se pudo serializar payload: " + e.getMessage());
        }

        String lastDetail = null;
        int lastHttp = 0;
        int attempts = 0;

        for (int i = 0; i < delays.length; i++) {
            if (delays[i] > 0) {
                try {
                    Thread.sleep(delays[i]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return WebhookSendResult.fail(url, lastHttp, attempts, "Envío interrumpido");
                }
            }
            attempts = i + 1;
            try {
                long epochSec = Instant.now().getEpochSecond();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Timestamp", String.valueOf(epochSec));
                signIfNeeded(system, epochSec, body, headers);
                HttpEntity<String> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = webhookRestTemplate.postForEntity(url, entity, String.class);
                int code = response.getStatusCode().value();
                lastHttp = code;
                String responseBody = truncate(response.getBody(), 200);
                if (code >= 200 && code < 300) {
                    log.info("Webhook lista OK sistema={} id={} intento={}", system.getId(), payload.get("id"), attempts);
                    return WebhookSendResult.ok(url, code, attempts, responseBody);
                }
                lastDetail = responseBody != null && !responseBody.isBlank()
                    ? responseBody
                    : "Respuesta vacía";
                log.warn("Webhook lista fallo sistema={} intento={} http={}", system.getId(), attempts, code);
            } catch (Exception e) {
                lastHttp = 0;
                lastDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.warn("Webhook lista error sistema={} intento={}: {}", system.getId(), attempts, lastDetail);
            }
        }
        log.error("Webhook lista agotó reintentos sistema={} evento={}", system.getId(), payload.get("id"));
        return WebhookSendResult.fail(url, lastHttp, attempts, lastDetail != null ? lastDetail : "Agotó reintentos");
    }

    private String truncate(String text, int max) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String oneLine = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (oneLine.length() <= max) {
            return oneLine;
        }
        return oneLine.substring(0, max) + "…";
    }

    private void signIfNeeded(InventorySystem system, long epochSec, String body, HttpHeaders headers) throws Exception {
        String secret = system.getInventoryListWebhookSecret();
        if (secret == null || secret.isBlank()) {
            return;
        }
        String message = epochSec + "." + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }
        headers.set("X-Signature", "sha256=" + hex);
    }
}
