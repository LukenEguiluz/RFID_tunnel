package com.rfidgateway.controller;

import com.rfidgateway.inventory.InventoryOrchestrationService;
import com.rfidgateway.model.Antenna;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderBrand;
import com.rfidgateway.model.ReaderGroup;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.reader.AntennaRfOptions;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.InventorySystemReaderRepository;
import com.rfidgateway.repository.InventorySystemRepository;
import com.rfidgateway.repository.ReaderGroupRepository;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
public class WebController {

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private AntennaRepository antennaRepository;

    @Autowired
    private ReaderGroupRepository readerGroupRepository;

    @Autowired
    private InventorySystemRepository inventorySystemRepository;

    @Autowired
    private InventorySystemReaderRepository inventorySystemReaderRepository;

    @Autowired(required = false)
    private ReaderManager readerManager;

    @Autowired(required = false)
    private InventoryOrchestrationService inventoryOrchestrationService;

    @GetMapping("/")
    public String index(Model model) {
        try {
            List<Reader> readers = readerRepository.findAll();
            if (readers == null) {
                readers = new ArrayList<>();
            }
            long total = readers.size();
            long connected = readers.stream().filter(r -> Boolean.TRUE.equals(r.getIsConnected())).count();
            model.addAttribute("totalReaders", total);
            model.addAttribute("connectedReaders", connected);
        } catch (Exception e) {
            log.warn("Error cargando índice: {}", e.getMessage());
            model.addAttribute("totalReaders", 0);
            model.addAttribute("connectedReaders", 0);
        }
        return "index";
    }

    @GetMapping("/readers")
    public String readers(Model model) {
        try {
            model.addAttribute("readers", readerRepository.findAll() != null ? readerRepository.findAll() : Collections.emptyList());
            model.addAttribute("groups", readerGroupRepository.findAll() != null ? readerGroupRepository.findAll() : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Error cargando lectores: {}", e.getMessage());
            model.addAttribute("readers", Collections.emptyList());
            model.addAttribute("groups", Collections.emptyList());
        }
        return "readers";
    }

    @GetMapping("/readers/new")
    public String readerNew(Model model) {
        Reader r = new Reader();
        r.setBrand(ReaderBrand.IMPINJ_OCTANE);
        model.addAttribute("reader", r);
        return "reader-form";
    }

    @PostMapping("/readers")
    public String readerCreate(Reader reader, RedirectAttributes redirect) {
        try {
            if (reader.getId() == null || reader.getId().isBlank()) {
                redirect.addFlashAttribute("error", "El ID es obligatorio.");
                return "redirect:/readers/new";
            }
            if (reader.getEnabled() == null) {
                reader.setEnabled(true);
            }
            if (reader.getBrand() == null) {
                reader.setBrand(ReaderBrand.IMPINJ_OCTANE);
            }
            reader.setIsConnected(false);
            reader.setIsReading(false);
            if (reader.getOperationMode() == null) {
                reader.setOperationMode(ReaderOperationMode.TUNNEL);
            }
            reader.setInventorySystemId(null);
            readerRepository.save(reader);
            redirect.addFlashAttribute("success", "Lector agregado correctamente.");
            if (readerManager != null && Boolean.TRUE.equals(reader.getEnabled())) {
                try {
                    readerManager.connectReader(reader);
                } catch (Exception e) {
                    log.warn("Lector guardado pero no se pudo conectar: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Error creando lector: {}", e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo guardar: " + e.getMessage());
            return "redirect:/readers/new";
        }
        return "redirect:/readers";
    }

    @GetMapping("/readers/{id}/edit")
    public String readerEdit(@PathVariable String id, Model model) {
        try {
            return readerRepository.findById(id)
                .map(reader -> {
                    model.addAttribute("reader", reader);
                    model.addAttribute("inventorySystems", inventorySystemRepository.findAll());
                    return "reader-edit";
                })
                .orElse("redirect:/readers");
        } catch (Exception e) {
            log.warn("Error editando lector {}: {}", id, e.getMessage());
            return "redirect:/readers";
        }
    }

    @PostMapping("/readers/{id}/edit")
    public String readerUpdate(@PathVariable String id, Reader reader, RedirectAttributes redirect) {
        try {
            readerRepository.findById(id).ifPresent(existing -> {
                existing.setName(reader.getName());
                existing.setHostname(reader.getHostname());
                existing.setEnabled(reader.getEnabled() != null ? reader.getEnabled() : true);
                if (reader.getOperationMode() != null) {
                    existing.setOperationMode(reader.getOperationMode());
                }
                if (reader.getBrand() != null) {
                    existing.setBrand(reader.getBrand());
                }
                if (existing.getOperationMode() == ReaderOperationMode.TUNNEL) {
                    existing.setInventorySystemId(null);
                    inventorySystemReaderRepository.findByReader_Id(id).ifPresent(inventorySystemReaderRepository::delete);
                } else {
                    existing.setInventorySystemId(reader.getInventorySystemId());
                }
                readerRepository.save(existing);
            });
            if (inventoryOrchestrationService != null) {
                inventoryOrchestrationService.reload();
            }
            redirect.addFlashAttribute("success", "Lector actualizado.");
        } catch (Exception e) {
            log.warn("Error actualizando lector {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo actualizar.");
        }
        return "redirect:/readers";
    }

    @PostMapping("/readers/{id}/connect")
    public String readerConnect(@PathVariable String id, RedirectAttributes redirect) {
        try {
            if (readerManager != null) {
                readerRepository.findById(id).ifPresent(readerManager::connectReader);
                redirect.addFlashAttribute("success", "Conectando lector...");
            } else {
                redirect.addFlashAttribute("error", "Servicio no disponible.");
            }
        } catch (Exception e) {
            log.warn("Error conectando lector {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo conectar: " + e.getMessage());
        }
        return "redirect:/readers";
    }

    @PostMapping("/readers/{id}/disconnect")
    public String readerDisconnect(@PathVariable String id, RedirectAttributes redirect) {
        try {
            if (readerManager != null) {
                readerManager.disconnectReader(id);
                redirect.addFlashAttribute("success", "Lector desconectado.");
            }
        } catch (Exception e) {
            log.warn("Error desconectando lector {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo desconectar.");
        }
        return "redirect:/readers";
    }

    @PostMapping("/readers/{id}/delete")
    public String readerDelete(@PathVariable String id, RedirectAttributes redirect) {
        try {
            inventorySystemReaderRepository.findByReader_Id(id).ifPresent(inventorySystemReaderRepository::delete);
            if (readerManager != null) {
                readerManager.disconnectReader(id);
            }
            readerRepository.deleteById(id);
            if (inventoryOrchestrationService != null) {
                inventoryOrchestrationService.reload();
            }
            redirect.addFlashAttribute("success", "Lector eliminado.");
        } catch (Exception e) {
            log.warn("Error eliminando lector {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo eliminar.");
        }
        return "redirect:/readers";
    }

    @GetMapping("/readers/{id}/antennas")
    public String readerAntennas(@PathVariable String id, Model model) {
        return readerRepository.findById(id)
            .map(reader -> {
                model.addAttribute("reader", reader);
                model.addAttribute("antennas", antennaRepository.findByReaderIdOrderByPortNumberAsc(id));
                if (readerManager != null) {
                    readerManager.queryHardwareCapabilities(id).ifPresent(h -> model.addAttribute("hardware", h));
                    model.addAttribute("rfOptions", readerManager.getAntennaRfOptions(id));
                } else {
                    model.addAttribute("rfOptions", AntennaRfOptions.fallback());
                }
                return "reader-antennas";
            })
            .orElse("redirect:/readers");
    }

    @PostMapping("/readers/{id}/antennas/discover")
    public String readerAntennasDiscover(@PathVariable String id, RedirectAttributes redirect) {
        try {
            if (readerManager == null) {
                redirect.addFlashAttribute("error", "ReaderManager no disponible.");
            } else {
                int n = readerManager.discoverAndSyncAntennas(id);
                redirect.addFlashAttribute("success", "Detectadas y sincronizadas " + n + " antena(s) desde el lector.");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/readers/" + id + "/antennas";
    }

    @PostMapping("/readers/{readerId}/antennas")
    public String readerAntennaAdd(@PathVariable String readerId,
                                   @RequestParam Short portNumber,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) String txPowerDbm,
                                   @RequestParam(required = false) String rxSensitivityDbm,
                                   @RequestParam(defaultValue = "true") boolean enabled,
                                   RedirectAttributes redirect) {
        if (!readerRepository.existsById(readerId)) {
            return "redirect:/readers";
        }
        try {
            String aid = readerId + "-antenna-" + portNumber;
            Antenna a = antennaRepository.findByReaderIdAndPortNumber(readerId, portNumber).orElseGet(() -> {
                Antenna na = new Antenna();
                na.setId(aid);
                na.setReaderId(readerId);
                na.setPortNumber(portNumber);
                na.setEnabled(true);
                return na;
            });
            if (name != null && !name.isBlank()) {
                a.setName(name.trim());
            } else if (a.getName() == null || a.getName().isBlank()) {
                a.setName("Puerto " + portNumber);
            }
            a.setEnabled(enabled);
            a.setTxPowerDbm(parseOptionalDbm(txPowerDbm));
            a.setRxSensitivityDbm(parseOptionalDbm(rxSensitivityDbm));
            antennaRepository.save(a);
            if (readerManager != null) {
                readerManager.resetAntennas(readerId);
            }
            redirect.addFlashAttribute("success", "Antena guardada. Si el lector estaba conectado, se reaplicó la configuración.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "No se pudo guardar la antena: " + e.getMessage());
        }
        return "redirect:/readers/" + readerId + "/antennas";
    }

    @GetMapping("/tags")
    public String tags(Model model) {
        try {
            model.addAttribute("readers", readerRepository.findAll() != null ? readerRepository.findAll() : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Error cargando tags: {}", e.getMessage());
            model.addAttribute("readers", Collections.emptyList());
        }
        return "tags";
    }

    @GetMapping("/api-docs")
    public String apiDocs(Model model) {
        try {
            model.addAttribute("readers", readerRepository.findAll() != null ? readerRepository.findAll() : Collections.emptyList());
            model.addAttribute("groups", readerGroupRepository.findAll() != null ? readerGroupRepository.findAll() : Collections.emptyList());
            model.addAttribute("inventorySystems", inventorySystemRepository.findAll() != null ? inventorySystemRepository.findAll() : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Error cargando api-docs: {}", e.getMessage());
            model.addAttribute("readers", Collections.emptyList());
            model.addAttribute("groups", Collections.emptyList());
            model.addAttribute("inventorySystems", Collections.emptyList());
        }
        return "api-docs";
    }

    @GetMapping("/groups")
    public String groups(Model model) {
        try {
            model.addAttribute("groups", readerGroupRepository.findAll() != null ? readerGroupRepository.findAll() : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Error cargando grupos: {}", e.getMessage());
            model.addAttribute("groups", Collections.emptyList());
        }
        return "groups";
    }

    @GetMapping("/groups/new")
    public String groupNew(Model model) {
        try {
            model.addAttribute("group", new ReaderGroup());
            model.addAttribute("readers", readerRepository.findAll() != null ? readerRepository.findAll() : Collections.emptyList());
            model.addAttribute("selectedReaderIds", Collections.emptyList());
        } catch (Exception e) {
            log.warn("Error cargando formulario grupo: {}", e.getMessage());
            model.addAttribute("group", new ReaderGroup());
            model.addAttribute("readers", Collections.emptyList());
            model.addAttribute("selectedReaderIds", Collections.emptyList());
        }
        return "group-form";
    }

    @GetMapping("/groups/{id}/edit")
    public String groupEdit(@PathVariable String id, Model model) {
        try {
            return readerGroupRepository.findById(id)
                .map(group -> {
                    model.addAttribute("group", group);
                    model.addAttribute("readers", readerRepository.findAll() != null ? readerRepository.findAll() : Collections.emptyList());
                    List<String> selectedIds = group.getReaders() != null ? group.getReaders().stream().map(Reader::getId).collect(java.util.stream.Collectors.toList()) : Collections.emptyList();
                    model.addAttribute("selectedReaderIds", selectedIds);
                    return "group-form";
                })
                .orElse("redirect:/groups");
        } catch (Exception e) {
            log.warn("Error editando grupo {}: {}", id, e.getMessage());
            return "redirect:/groups";
        }
    }

    @PostMapping("/groups")
    public String groupCreate(@RequestParam String id, @RequestParam String name,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) List<String> readerIds,
                             @RequestParam(required = false) Boolean enabled,
                             RedirectAttributes redirect) {
        try {
            if (id == null || id.isBlank()) {
                redirect.addFlashAttribute("error", "El ID del grupo es obligatorio.");
                return "redirect:/groups/new";
            }
            if (readerGroupRepository.existsById(id)) {
                redirect.addFlashAttribute("error", "Ya existe un grupo con ese ID.");
                return "redirect:/groups/new";
            }
            ReaderGroup group = new ReaderGroup();
            group.setId(id.trim());
            group.setName(name != null ? name.trim() : id);
            group.setDescription(description != null ? description.trim() : null);
            group.setEnabled(enabled != null ? enabled : true);
            if (readerIds != null && !readerIds.isEmpty()) {
                List<Reader> list = new ArrayList<>();
                for (String rid : readerIds) {
                    readerRepository.findById(rid).ifPresent(list::add);
                }
                group.setReaders(list);
            }
            readerGroupRepository.save(group);
            redirect.addFlashAttribute("success", "Grupo creado correctamente.");
        } catch (Exception e) {
            log.warn("Error creando grupo: {}", e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo crear el grupo.");
            return "redirect:/groups/new";
        }
        return "redirect:/groups";
    }

    @PostMapping("/groups/{id}/edit")
    public String groupUpdate(@PathVariable String id,
                              @RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) List<String> readerIds,
                              @RequestParam(required = false) Boolean enabled,
                              RedirectAttributes redirect) {
        try {
            readerGroupRepository.findById(id).ifPresent(group -> {
                group.setName(name != null ? name.trim() : group.getName());
                group.setDescription(description != null ? description.trim() : null);
                group.setEnabled(enabled != null ? enabled : true);
                if (readerIds != null) {
                    List<Reader> list = new ArrayList<>();
                    for (String rid : readerIds) {
                        readerRepository.findById(rid).ifPresent(list::add);
                    }
                    group.setReaders(list);
                }
                readerGroupRepository.save(group);
            });
            redirect.addFlashAttribute("success", "Grupo actualizado.");
        } catch (Exception e) {
            log.warn("Error actualizando grupo {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo actualizar.");
        }
        return "redirect:/groups";
    }

    @PostMapping("/groups/{id}/delete")
    public String groupDelete(@PathVariable String id, RedirectAttributes redirect) {
        try {
            readerGroupRepository.deleteById(id);
            redirect.addFlashAttribute("success", "Grupo eliminado.");
        } catch (Exception e) {
            log.warn("Error eliminando grupo {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo eliminar el grupo.");
        }
        return "redirect:/groups";
    }

    private static Double parseOptionalDbm(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Double.valueOf(raw.trim().replace(',', '.'));
    }
}
