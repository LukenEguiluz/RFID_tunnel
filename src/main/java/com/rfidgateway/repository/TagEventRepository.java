package com.rfidgateway.repository;

import com.rfidgateway.model.TagEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TagEventRepository extends JpaRepository<TagEvent, Long> {
    Page<TagEvent> findByEpc(String epc, Pageable pageable);
    Page<TagEvent> findByReaderId(String readerId, Pageable pageable);
    Page<TagEvent> findByAntennaId(String antennaId, Pageable pageable);
    Page<TagEvent> findByDetectedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    @Query("SELECT t FROM TagEvent t WHERE t.epc = :epc AND t.readerId = :readerId ORDER BY t.detectedAt DESC")
    List<TagEvent> findLatestByEpcAndReader(@Param("epc") String epc, @Param("readerId") String readerId, Pageable pageable);
    
    Page<TagEvent> findAllByOrderByDetectedAtDesc(Pageable pageable);
    
    @Query("SELECT t FROM TagEvent t WHERE t.readerId = :readerId AND t.epc = :epc ORDER BY t.detectedAt DESC")
    List<TagEvent> findTopByReaderIdAndEpcOrderByDetectedAtDesc(@Param("readerId") String readerId, @Param("epc") String epc, Pageable pageable);
    
    @Query("SELECT t FROM TagEvent t WHERE t.readerId = :readerId ORDER BY t.detectedAt DESC")
    List<TagEvent> findTopByReaderIdOrderByDetectedAtDesc(@Param("readerId") String readerId, Pageable pageable);
    
    @Query("SELECT t FROM TagEvent t WHERE t.epc = :epc ORDER BY t.detectedAt DESC")
    List<TagEvent> findTopByEpcOrderByDetectedAtDesc(@Param("epc") String epc, Pageable pageable);
    
    @Query("SELECT t FROM TagEvent t ORDER BY t.detectedAt DESC")
    List<TagEvent> findTopOrderByDetectedAtDesc(Pageable pageable);
    
    long countByReaderIdAndDetectedAtAfter(String readerId, LocalDateTime after);
    long countByDetectedAtAfter(LocalDateTime after);
}


