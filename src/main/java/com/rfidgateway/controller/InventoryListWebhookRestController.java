/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.rfidgateway.controller;

import com.rfidgateway.inventory.InventoryListWebhookService;
import com.rfidgateway.repository.InventorySystemRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/inventory-systems/{id}/inventory-list-webhook"})
public class InventoryListWebhookRestController {
    @Autowired
    private InventorySystemRepository inventorySystemRepository;
    @Autowired
    private InventoryListWebhookService inventoryListWebhookService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> snapshot(@PathVariable UUID id) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> body = this.inventoryListWebhookService.buildSnapshot(id);
        if (body.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping(value={"/capture"}, produces={"text/html"})
    public String captureInfo(@PathVariable UUID id) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"/><title>Webhook inventario lista</title></head><body><h1>Receptor de prueba \u2014 inventario lista</h1><p>Sistema: <code>" + id + "</code></p><p>Envi\u00e1 un <strong>POST</strong> con el mismo JSON que recibir\u00eda vuestra webapp.</p><p>SYNC manual: <code>POST /api/inventory-systems/" + id + "/inventory-list-webhook/sync</code></p></body></html>";
    }

    @PostMapping(value={"/capture"})
    public ResponseEntity<Map<String, Object>> capture(@PathVariable UUID id, @RequestBody(required=false) Map<String, Object> body) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ok", true);
        out.put("systemId", id);
        out.put("received", body != null ? body : Map.of());
        return ResponseEntity.ok(out);
    }

    @PostMapping(value={"/sync"})
    public ResponseEntity<Map<String, Object>> sync(@PathVariable UUID id) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        this.inventoryListWebhookService.dispatchFullSync(id);
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ok", true);
        out.put("systemId", id);
        out.put("message", "INVENTORY_LIST_UPDATE encolado (listado completo, added=[], removed=[])");
        return ResponseEntity.accepted().body(out);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> artificialTest(@PathVariable UUID id) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            var result = this.inventoryListWebhookService.dispatchArtificialTest(id);
            LinkedHashMap<String, Object> out = new LinkedHashMap<>(result.toMap());
            out.put("systemId", id);
            return result.getWebhook().isSuccess()
                ? ResponseEntity.ok(out)
                : ResponseEntity.status(result.getWebhook().getHttpStatus() > 0
                    ? result.getWebhook().getHttpStatus() : 502).body(out);
        } catch (Exception e) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("ok", false);
            out.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        }
    }
}
