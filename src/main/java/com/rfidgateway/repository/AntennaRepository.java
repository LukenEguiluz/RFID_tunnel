package com.rfidgateway.repository;

import com.rfidgateway.model.Antenna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AntennaRepository extends JpaRepository<Antenna, String> {
    List<Antenna> findByReaderId(String readerId);
    List<Antenna> findByReaderIdAndEnabledTrue(String readerId);
    Optional<Antenna> findByReaderIdAndPortNumber(String readerId, Short portNumber);
}

