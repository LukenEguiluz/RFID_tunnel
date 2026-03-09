package com.rfidgateway.controller;

import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderGroup;
import com.rfidgateway.reader.ReaderManager;
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
    private ReaderGroupRepository readerGroupRepository;

    @Autowired(required = false)
    private ReaderManager readerManager;

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
        model.addAttribute("reader", new Reader());
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
            reader.setIsConnected(false);
            reader.setIsReading(false);
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
                readerRepository.save(existing);
            });
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
            if (readerManager != null) {
                readerManager.disconnectReader(id);
            }
            readerRepository.deleteById(id);
            redirect.addFlashAttribute("success", "Lector eliminado.");
        } catch (Exception e) {
            log.warn("Error eliminando lector {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo eliminar.");
        }
        return "redirect:/readers";
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
        } catch (Exception e) {
            log.warn("Error cargando api-docs: {}", e.getMessage());
            model.addAttribute("readers", Collections.emptyList());
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
}
