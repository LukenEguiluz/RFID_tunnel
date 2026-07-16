/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rfidgateway.repository.ReaderRepository
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Controller
 *  org.springframework.ui.Model
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.servlet.mvc.support.RedirectAttributes
 */
package com.rfidgateway.controller;

import com.rfidgateway.inventory.InventoryEpcService;
import com.rfidgateway.inventory.InventoryListWebhookService;
import com.rfidgateway.inventory.InventoryOrchestrationService;
import com.rfidgateway.inventory.InventorySystemCommandService;
import com.rfidgateway.model.InventorySystem;
import com.rfidgateway.repository.InventorySystemReaderRepository;
import com.rfidgateway.repository.InventorySystemRepository;
import com.rfidgateway.repository.ReaderRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(value={"/inventory-systems"})
public class InventorySystemWebController {
    private static final Logger log = LoggerFactory.getLogger(InventorySystemWebController.class);
    @Autowired
    private InventorySystemRepository inventorySystemRepository;
    @Autowired
    private InventorySystemReaderRepository memberRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private InventorySystemCommandService inventorySystemCommandService;
    @Autowired
    private InventoryOrchestrationService inventoryOrchestrationService;
    @Autowired
    private InventoryListWebhookService inventoryListWebhookService;
    @Autowired
    private InventoryEpcService inventoryEpcService;

    @GetMapping
    public String list(Model model) {
        try {
            model.addAttribute("systems", (Object)this.inventorySystemRepository.findAll());
        }
        catch (Exception e) {
            log.warn("Error listando sistemas: {}", (Object)e.getMessage());
            model.addAttribute("systems", Collections.emptyList());
        }
        return "inventory-systems";
    }

    @GetMapping(value={"/new"})
    public String formNew(Model model) {
        model.addAttribute("system", (Object)new InventorySystem());
        model.addAttribute("members", Collections.emptyList());
        model.addAttribute("allReaders", (Object)this.readerRepository.findAll());
        return "inventory-system-form";
    }

    @GetMapping(value={"/{id}/edit"})
    public String formEdit(@PathVariable UUID id, Model model) {
        return this.inventorySystemRepository.findById(id).map(sys -> {
            model.addAttribute("system", sys);
            model.addAttribute("members", this.memberRepository.findBySystem_IdOrderByOrderIndexAsc(id));
            model.addAttribute("allReaders", (Object)this.readerRepository.findAll());
            return "inventory-system-form";
        }).orElse("redirect:/inventory-systems");
    }

    @GetMapping(value={"/{id}/epcs"})
    public String liveEpcs(@PathVariable UUID id, Model model) {
        return this.inventorySystemRepository.findById(id).map(s -> {
            model.addAttribute("systemId", (Object)s.getId());
            model.addAttribute("systemName", (Object)s.getName());
            model.addAttribute("cyclesPaused", this.inventoryEpcService.isCyclesPaused(id));
            model.addAttribute("cyclesToLost", s.getCyclesToLost() != null ? s.getCyclesToLost() : 3);
            return "inventory-system-epcs";
        }).orElse("redirect:/inventory-systems");
    }

    @GetMapping(value={"/{id}/webhook"})
    public String webhookConfig(@PathVariable UUID id, Model model) {
        return this.inventorySystemRepository.findById(id).map(system -> {
            model.addAttribute("system", system);
            try {
                model.addAttribute("samplePayload", (Object)this.inventoryListWebhookService.samplePayloadJson((InventorySystem)system));
            }
            catch (Exception e) {
                model.addAttribute("samplePayload", (Object)"{}");
            }
            return "inventory-system-webhook";
        }).orElse("redirect:/inventory-systems");
    }

    @PostMapping(value={"/{id}/inventory-list-webhook"})
    public String inventoryListWebhookSave(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "false") boolean inventoryListWebhookEnabled,
        @RequestParam(required = false) String inventoryListWebhookUrl,
        @RequestParam(required = false) String inventoryListWebhookSecret,
        @RequestParam(required = false) String inventoryListWebhookEventId,
        @RequestParam(defaultValue = "false") boolean sendSyncOnSave,
        RedirectAttributes redirect
    ) {
        if (inventoryListWebhookEnabled && (inventoryListWebhookUrl == null || inventoryListWebhookUrl.isBlank())) {
            redirect.addFlashAttribute("error", "Indicá la URL del webhook de inventario lista.");
            return "redirect:/inventory-systems/" + id + "/webhook";
        }
        this.inventorySystemRepository.findById(id).ifPresent(system -> {
            system.setInventoryListWebhookEnabled(inventoryListWebhookEnabled);
            system.setInventoryListWebhookUrl(inventoryListWebhookUrl != null ? inventoryListWebhookUrl.trim() : null);
            if (inventoryListWebhookSecret != null && !inventoryListWebhookSecret.isBlank()) {
                system.setInventoryListWebhookSecret(inventoryListWebhookSecret.trim());
            }
            if (inventoryListWebhookEventId != null && !inventoryListWebhookEventId.isBlank()) {
                system.setInventoryListWebhookEventId(inventoryListWebhookEventId.trim());
            }
            this.inventorySystemRepository.save(system);
            if (sendSyncOnSave && inventoryListWebhookEnabled) {
                this.inventoryListWebhookService.dispatchFullSync(id);
            }
        });
        redirect.addFlashAttribute("success", "Webhook inventario lista guardado.");
        return "redirect:/inventory-systems/" + id + "/webhook";
    }

    @PostMapping("/{id}/inventory-list-webhook/test")
    public String inventoryListWebhookTest(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            var result = this.inventoryListWebhookService.dispatchArtificialTest(id);
            Object data = result.getPayload().get("data");
            String added = data instanceof Map ? String.valueOf(((Map<?, ?>) data).get("added")) : "[]";
            String removed = data instanceof Map ? String.valueOf(((Map<?, ?>) data).get("removed")) : "[]";
            String tagsInfo = "Tags TEST: added=" + added + " removed=" + removed + ". ";
            if (result.getWebhook().isSuccess()) {
                redirect.addFlashAttribute("success", tagsInfo + result.getWebhook().toUserMessage());
            } else {
                redirect.addFlashAttribute("error", tagsInfo + result.getWebhook().toUserMessage());
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Prueba fallida: " + e.getMessage());
        }
        return "redirect:/inventory-systems/" + id + "/webhook";
    }

    @PostMapping("/{id}/inventory-list-webhook/force")
    public String inventoryListWebhookForce(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            var webhook = this.inventoryListWebhookService.dispatchFullSyncForceSync(id);
            if (webhook.isSuccess()) {
                redirect.addFlashAttribute("success", webhook.toUserMessage());
            } else {
                redirect.addFlashAttribute("error", webhook.toUserMessage());
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "SYNC forzado fallido: " + e.getMessage());
        }
        return "redirect:/inventory-systems/" + id + "/webhook";
    }

    @PostMapping("/{id}/demo/pause-cycles")
    public String demoPauseCycles(@PathVariable UUID id, RedirectAttributes redirect) {
        this.inventoryEpcService.setCyclesPaused(id, true);
        redirect.addFlashAttribute("success", "Ciclos pausados. No se evaluarán removes automáticos hasta reanudar.");
        return "redirect:/inventory-systems/" + id + "/epcs";
    }

    @PostMapping("/{id}/demo/resume-cycles")
    public String demoResumeCycles(@PathVariable UUID id, RedirectAttributes redirect) {
        this.inventoryEpcService.setCyclesPaused(id, false);
        redirect.addFlashAttribute("success", "Ciclos reanudados.");
        return "redirect:/inventory-systems/" + id + "/epcs";
    }

    @PostMapping("/{id}/demo/mark-removed")
    public String demoMarkRemoved(@PathVariable UUID id, @RequestParam String epc, RedirectAttributes redirect) {
        try {
            this.inventoryEpcService.manualMarkRemoved(id, epc);
            redirect.addFlashAttribute("success", "Salida manual registrada (removed): " + epc.trim().toUpperCase());
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory-systems/" + id + "/epcs";
    }

    @PostMapping("/{id}/demo/mark-returned")
    public String demoMarkReturned(@PathVariable UUID id, @RequestParam String epc, RedirectAttributes redirect) {
        try {
            this.inventoryEpcService.manualMarkReturned(id, epc);
            redirect.addFlashAttribute("success", "Regreso manual registrado (added): " + epc.trim().toUpperCase());
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory-systems/" + id + "/epcs";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam int globalCycleSeconds, @RequestParam(defaultValue="3") int cyclesToLost, @RequestParam(defaultValue="false") boolean enabled, @RequestParam(required=false) List<String> memberReaderId, @RequestParam(required=false) List<Integer> memberOrder, @RequestParam(required=false) List<Integer> memberSlotSeconds, RedirectAttributes redirect) {
        try {
            this.inventorySystemCommandService.createSystem(name, globalCycleSeconds, cyclesToLost, enabled, memberReaderId, memberOrder, memberSlotSeconds);
            this.inventoryOrchestrationService.reload();
            redirect.addFlashAttribute("success", (Object)"Sistema creado. Activa el sistema para iniciar ciclos.");
        }
        catch (Exception e) {
            log.warn("Error creando sistema: {}", (Object)e.getMessage());
            redirect.addFlashAttribute("error", (Object)("No se pudo crear: " + e.getMessage()));
            return "redirect:/inventory-systems/new";
        }
        return "redirect:/inventory-systems";
    }

    @PostMapping(value={"/{id}/edit"})
    public String update(@PathVariable UUID id, @RequestParam String name, @RequestParam int globalCycleSeconds, @RequestParam(defaultValue="3") int cyclesToLost, @RequestParam(defaultValue="false") boolean enabled, @RequestParam(required=false) List<String> memberReaderId, @RequestParam(required=false) List<Integer> memberOrder, @RequestParam(required=false) List<Integer> memberSlotSeconds, RedirectAttributes redirect) {
        try {
            this.inventorySystemCommandService.updateSystem(id, name, globalCycleSeconds, cyclesToLost, enabled, memberReaderId, memberOrder, memberSlotSeconds);
            this.inventoryOrchestrationService.reload();
            redirect.addFlashAttribute("success", (Object)"Sistema actualizado.");
        }
        catch (Exception e) {
            log.warn("Error actualizando sistema {}: {}", (Object)id, (Object)e.getMessage());
            redirect.addFlashAttribute("error", (Object)("No se pudo actualizar: " + e.getMessage()));
        }
        return "redirect:/inventory-systems";
    }

    @PostMapping(value={"/{id}/delete"})
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            this.inventorySystemCommandService.deleteSystem(id);
            this.inventoryOrchestrationService.reload();
            redirect.addFlashAttribute("success", (Object)"Sistema eliminado.");
        }
        catch (Exception e) {
            log.warn("Error eliminando sistema {}: {}", (Object)id, (Object)e.getMessage());
            redirect.addFlashAttribute("error", (Object)"No se pudo eliminar.");
        }
        return "redirect:/inventory-systems";
    }
}
