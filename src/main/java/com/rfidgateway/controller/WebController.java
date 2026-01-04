package com.rfidgateway.controller;

import com.rfidgateway.model.Antenna;
import com.rfidgateway.model.Reader;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
public class WebController {
    
    @Autowired
    private ReaderRepository readerRepository;
    
    @Autowired
    private AntennaRepository antennaRepository;
    
    @Autowired
    private ReaderManager readerManager;
    
    @Autowired
    private com.rfidgateway.repository.ReaderGroupRepository groupRepository;
    
    @GetMapping("/")
    public String index(Model model) {
        List<Reader> readers = readerRepository.findAll();
        model.addAttribute("readers", readers);
        model.addAttribute("totalReaders", readers.size());
        model.addAttribute("connectedReaders", readers.stream()
            .filter(r -> r.getIsConnected() != null && r.getIsConnected())
            .count());
        return "index";
    }
    
    @GetMapping("/readers")
    public String readers(Model model) {
        List<Reader> readers = readerRepository.findAll();
        List<com.rfidgateway.model.ReaderGroup> groups = groupRepository.findAll();
        model.addAttribute("readers", readers);
        model.addAttribute("groups", groups);
        return "readers";
    }
    
    @GetMapping("/readers/new")
    public String newReaderForm(Model model) {
        model.addAttribute("reader", new Reader());
        return "reader-form";
    }
    
    @PostMapping("/readers")
    public String createReader(@ModelAttribute Reader reader, RedirectAttributes redirectAttributes) {
        try {
            reader.setIsConnected(false);
            reader.setIsReading(false);
            
            // Establecer valores por defecto si no se proporcionaron
            if (reader.getIntermittentEnabled() == null) {
                reader.setIntermittentEnabled(false);
            }
            if (reader.getReadDurationSeconds() == null) {
                reader.setReadDurationSeconds(5);
            }
            if (reader.getPauseDurationSeconds() == null) {
                reader.setPauseDurationSeconds(5);
            }
            
            readerRepository.save(reader);
            redirectAttributes.addFlashAttribute("success", "Lector creado exitosamente");
            
            // Intentar conectar automáticamente si está habilitado
            if (reader.getEnabled()) {
                readerManager.connectReader(reader);
            }
        } catch (Exception e) {
            log.error("Error al crear lector: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al crear lector: " + e.getMessage());
        }
        return "redirect:/readers";
    }
    
    @GetMapping("/readers/{id}/edit")
    public String editReaderForm(@PathVariable String id, Model model) {
        Reader reader = readerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lector no encontrado"));
        model.addAttribute("reader", reader);
        List<Antenna> antennas = antennaRepository.findByReaderId(id);
        model.addAttribute("antennas", antennas);
        return "reader-edit";
    }
    
    @PostMapping("/readers/{id}")
    public String updateReader(@PathVariable String id, @ModelAttribute Reader reader, 
                              RedirectAttributes redirectAttributes) {
        try {
            Reader existing = readerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lector no encontrado"));
            
            existing.setName(reader.getName());
            existing.setHostname(reader.getHostname());
            existing.setEnabled(reader.getEnabled());
            
            // Actualizar configuración de lectura intermitente
            existing.setIntermittentEnabled(reader.getIntermittentEnabled());
            if (reader.getReadDurationSeconds() != null) {
                existing.setReadDurationSeconds(reader.getReadDurationSeconds());
            }
            if (reader.getPauseDurationSeconds() != null) {
                existing.setPauseDurationSeconds(reader.getPauseDurationSeconds());
            }
            
            readerRepository.save(existing);
            redirectAttributes.addFlashAttribute("success", "Lector actualizado exitosamente. Reinicia el lector para aplicar cambios en el modo de lectura.");
            
            // Reconectar si está habilitado y no está conectado
            if (existing.getEnabled() && (existing.getIsConnected() == null || !existing.getIsConnected())) {
                readerManager.connectReader(existing);
            } else if (!existing.getEnabled()) {
                readerManager.disconnectReader(id);
            }
        } catch (Exception e) {
            log.error("Error al actualizar lector: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al actualizar lector: " + e.getMessage());
        }
        return "redirect:/readers/" + id + "/edit";
    }
    
    @PostMapping("/readers/{id}/delete")
    public String deleteReader(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            readerManager.disconnectReader(id);
            readerRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Lector eliminado exitosamente");
        } catch (Exception e) {
            log.error("Error al eliminar lector: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al eliminar lector: " + e.getMessage());
        }
        return "redirect:/readers";
    }
    
    @GetMapping("/readers/{id}/antennas/new")
    public String newAntennaForm(@PathVariable String id, Model model) {
        Reader reader = readerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lector no encontrado"));
        Antenna antenna = new Antenna();
        antenna.setReaderId(id);
        model.addAttribute("antenna", antenna);
        model.addAttribute("reader", reader);
        return "antenna-form";
    }
    
    @PostMapping("/readers/{readerId}/antennas")
    public String createAntenna(@PathVariable String readerId, @ModelAttribute Antenna antenna,
                                RedirectAttributes redirectAttributes) {
        try {
            antenna.setReaderId(readerId);
            if (antenna.getId() == null || antenna.getId().isEmpty()) {
                antenna.setId(readerId + "-antenna-" + antenna.getPortNumber());
            }
            antennaRepository.save(antenna);
            updateConnectedAntennasCount(readerId);
            redirectAttributes.addFlashAttribute("success", "Antena creada exitosamente");
        } catch (Exception e) {
            log.error("Error al crear antena: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al crear antena: " + e.getMessage());
        }
        return "redirect:/readers/" + readerId + "/edit";
    }
    
    @PostMapping("/antennas/{id}/delete")
    public String deleteAntenna(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            Antenna antenna = antennaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Antena no encontrada"));
            String readerId = antenna.getReaderId();
            antennaRepository.deleteById(id);
            updateConnectedAntennasCount(readerId);
            redirectAttributes.addFlashAttribute("success", "Antena eliminada exitosamente");
            return "redirect:/readers/" + readerId + "/edit";
        } catch (Exception e) {
            log.error("Error al eliminar antena: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al eliminar antena: " + e.getMessage());
            return "redirect:/readers";
        }
    }
    
    private void updateConnectedAntennasCount(String readerId) {
        try {
            Reader reader = readerRepository.findById(readerId).orElse(null);
            if (reader != null) {
                // Contar antenas habilitadas (enabled = true)
                int count = antennaRepository.findByReaderIdAndEnabledTrue(readerId).size();
                reader.setConnectedAntennasCount(count);
                readerRepository.save(reader);
            }
        } catch (Exception e) {
            log.error("Error al actualizar contador de antenas para lector {}: {}", readerId, e.getMessage());
        }
    }
    
    @PostMapping("/readers/{id}/connect")
    public String connectReader(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lector no encontrado"));
            readerManager.connectReader(reader);
            redirectAttributes.addFlashAttribute("success", "Conectando al lector...");
        } catch (Exception e) {
            log.error("Error al conectar lector: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al conectar: " + e.getMessage());
        }
        return "redirect:/readers";
    }
    
    @PostMapping("/readers/{id}/disconnect")
    public String disconnectReader(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            readerManager.disconnectReader(id);
            redirectAttributes.addFlashAttribute("success", "Lector desconectado");
        } catch (Exception e) {
            log.error("Error al desconectar lector: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al desconectar: " + e.getMessage());
        }
        return "redirect:/readers";
    }
    
    @PostMapping("/readers/{id}/start")
    public String startReader(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lector no encontrado"));
            
            if (!reader.getEnabled()) {
                redirectAttributes.addFlashAttribute("error", "El lector debe estar habilitado para iniciar lectura");
                return "redirect:/readers/" + id + "/edit";
            }
            
            readerManager.startReader(id);
            redirectAttributes.addFlashAttribute("success", "Lectura iniciada");
        } catch (Exception e) {
            log.error("Error al iniciar lectura: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al iniciar lectura: " + e.getMessage());
        }
        return "redirect:/readers/" + id + "/edit";
    }
    
    @PostMapping("/readers/{id}/stop")
    public String stopReader(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            readerManager.stopReader(id);
            redirectAttributes.addFlashAttribute("success", "Lectura detenida");
        } catch (Exception e) {
            log.error("Error al detener lectura: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al detener lectura: " + e.getMessage());
        }
        return "redirect:/readers/" + id + "/edit";
    }
    
    @PostMapping("/readers/{id}/reset")
    public String resetReader(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            readerManager.resetReader(id);
            redirectAttributes.addFlashAttribute("success", "Lector reiniciado. Reconectando...");
        } catch (Exception e) {
            log.error("Error al reiniciar lector: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al reiniciar lector: " + e.getMessage());
        }
        return "redirect:/readers/" + id + "/edit";
    }
    
    @GetMapping("/api-docs")
    public String apiDocs(Model model) {
        List<Reader> readers = readerRepository.findAll();
        model.addAttribute("readers", readers);
        return "api-docs";
    }
    
    @GetMapping("/tags")
    public String tags(Model model) {
        List<Reader> readers = readerRepository.findAll();
        model.addAttribute("readers", readers);
        return "tags";
    }
    
    // ========== GRUPOS DE LECTORES ==========
    
    @GetMapping("/groups/new")
    public String newGroupForm(Model model) {
        List<Reader> readers = readerRepository.findAll();
        model.addAttribute("readers", readers);
        model.addAttribute("group", new com.rfidgateway.model.ReaderGroup());
        return "group-form";
    }
    
    @PostMapping("/groups")
    public String createGroup(@RequestParam String id,
                             @RequestParam String name,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) List<String> readerIds,
                             @RequestParam(required = false, defaultValue = "true") Boolean enabled,
                             RedirectAttributes redirectAttributes) {
        try {
            com.rfidgateway.model.ReaderGroup group = new com.rfidgateway.model.ReaderGroup();
            group.setId(id);
            group.setName(name);
            group.setDescription(description);
            group.setEnabled(enabled);
            
            // Agregar lectores al grupo
            if (readerIds != null && !readerIds.isEmpty()) {
                var readers = readerIds.stream()
                    .map(readerRepository::findById)
                    .filter(opt -> opt.isPresent())
                    .map(opt -> opt.get())
                    .collect(java.util.stream.Collectors.toList());
                group.setReaders(readers);
            }
            
            groupRepository.save(group);
            redirectAttributes.addFlashAttribute("success", "Grupo creado exitosamente");
        } catch (Exception e) {
            log.error("Error al crear grupo: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al crear grupo: " + e.getMessage());
        }
        return "redirect:/readers";
    }
    
    @GetMapping("/groups/{id}/edit")
    public String editGroupForm(@PathVariable String id, Model model) {
        com.rfidgateway.model.ReaderGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
        List<Reader> readers = readerRepository.findAll();
        model.addAttribute("group", group);
        model.addAttribute("readers", readers);
        return "group-form";
    }
    
    @PostMapping("/groups/{id}")
    public String updateGroup(@PathVariable String id,
                             @RequestParam String name,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) List<String> readerIds,
                             @RequestParam(required = false, defaultValue = "true") Boolean enabled,
                             RedirectAttributes redirectAttributes) {
        try {
            com.rfidgateway.model.ReaderGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
            
            group.setName(name);
            group.setDescription(description);
            group.setEnabled(enabled);
            
            // Actualizar lectores del grupo
            if (readerIds != null) {
                var readers = readerIds.stream()
                    .map(readerRepository::findById)
                    .filter(opt -> opt.isPresent())
                    .map(opt -> opt.get())
                    .collect(java.util.stream.Collectors.toList());
                group.setReaders(readers);
            } else {
                group.setReaders(new java.util.ArrayList<>());
            }
            
            groupRepository.save(group);
            redirectAttributes.addFlashAttribute("success", "Grupo actualizado exitosamente");
        } catch (Exception e) {
            log.error("Error al actualizar grupo: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al actualizar grupo: " + e.getMessage());
        }
        return "redirect:/readers";
    }
    
    @PostMapping("/groups/{id}/delete")
    public String deleteGroup(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            groupRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Grupo eliminado exitosamente");
        } catch (Exception e) {
            log.error("Error al eliminar grupo: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al eliminar grupo: " + e.getMessage());
        }
        return "redirect:/readers";
    }
}



