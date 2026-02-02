package com.rfidgateway.repository;

import com.rfidgateway.model.ReaderGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReaderGroupRepository extends JpaRepository<ReaderGroup, String> {
    
    List<ReaderGroup> findByEnabledTrue();
    
    Optional<ReaderGroup> findByName(String name);
    
    boolean existsById(String id);
}





