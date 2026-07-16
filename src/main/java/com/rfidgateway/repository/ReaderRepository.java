package com.rfidgateway.repository;

import com.rfidgateway.model.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReaderRepository extends JpaRepository<Reader, String> {
    List<Reader> findByEnabledTrue();
    Optional<Reader> findByName(String name);
    Optional<Reader> findByHostname(String hostname);
}







