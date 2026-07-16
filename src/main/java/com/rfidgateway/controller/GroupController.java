package com.rfidgateway.controller;

import com.rfidgateway.model.ReaderGroup;
import com.rfidgateway.repository.ReaderGroupRepository;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/groups")
public class GroupController {
    
    @Autowired
    private ReaderGroupRepository groupRepository;
    
    @Autowired
    private ReaderRepository readerRepository;
    
    /**
     * Listar todos los grupos
     */
    @GetMapping
    public ResponseEntity<List<ReaderGroup>> getAllGroups() {
        List<ReaderGroup> groups = groupRepository.findAll();
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Obtener un grupo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReaderGroup> getGroup(@PathVariable String id) {
        return groupRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Crear un nuevo grupo
     */
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request) {
        String id = (String) request.get("id");
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> readerIds = (List<String>) request.get("readerIds");
        Boolean enabled = request.get("enabled") != null ? 
            Boolean.valueOf(request.get("enabled").toString()) : true;
        
        if (id == null || id.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "id es requerido"));
        }
        
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "name es requerido"));
        }
        
        if (groupRepository.existsById(id)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Ya existe un grupo con id: " + id));
        }
        
        try {
            ReaderGroup group = new ReaderGroup();
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
                    .collect(Collectors.toList());
                group.setReaders(readers);
            }
            
            groupRepository.save(group);
            
            return ResponseEntity.status(201).body(group);
            
        } catch (Exception e) {
            log.error("Error al crear grupo: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al crear grupo: " + e.getMessage()));
        }
    }
    
    /**
     * Actualizar un grupo
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroup(@PathVariable String id, @RequestBody Map<String, Object> request) {
        var groupOpt = groupRepository.findById(id);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ReaderGroup group = groupOpt.get();
        
        if (request.containsKey("name")) {
            group.setName((String) request.get("name"));
        }
        
        if (request.containsKey("description")) {
            group.setDescription((String) request.get("description"));
        }
        
        if (request.containsKey("enabled")) {
            group.setEnabled(Boolean.valueOf(request.get("enabled").toString()));
        }
        
        if (request.containsKey("readerIds")) {
            @SuppressWarnings("unchecked")
            List<String> readerIds = (List<String>) request.get("readerIds");
            var readers = readerIds.stream()
                .map(readerRepository::findById)
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .collect(Collectors.toList());
            group.setReaders(readers);
        }
        
        try {
            groupRepository.save(group);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            log.error("Error al actualizar grupo: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al actualizar grupo: " + e.getMessage()));
        }
    }
    
    /**
     * Eliminar un grupo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable String id) {
        if (!groupRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            groupRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Grupo eliminado exitosamente"));
        } catch (Exception e) {
            log.error("Error al eliminar grupo: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al eliminar grupo: " + e.getMessage()));
        }
    }
    
    /**
     * Obtener estadísticas de un grupo
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getGroupStats(@PathVariable String id) {
        var groupOpt = groupRepository.findById(id);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ReaderGroup group = groupOpt.get();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("groupId", group.getId());
        stats.put("groupName", group.getName());
        stats.put("totalReaders", group.getReaders().size());
        stats.put("enabledReaders", group.getEnabledReaders().size());
        stats.put("connectedReaders", group.getConnectedReaders().size());
        
        return ResponseEntity.ok(stats);
    }
}





