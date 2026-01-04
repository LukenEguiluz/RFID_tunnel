package com.rfidgateway.config;

import com.rfidgateway.model.Reader;
import com.rfidgateway.model.Antenna;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.repository.AntennaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class ReaderConfig {
    
    @Autowired
    private ReaderRepository readerRepository;
    
    @Autowired
    private AntennaRepository antennaRepository;
    
    @Bean
    public CommandLineRunner initReaders() {
        return args -> {
            // Verificar si ya hay lectores configurados
            if (readerRepository.count() == 0) {
                log.info("No hay lectores configurados. Por favor, configura los lectores en la base de datos.");
                log.info("Puedes usar la API REST para agregar lectores o insertarlos directamente en la BD.");
            } else {
                log.info("Lectores encontrados en la base de datos: {}", readerRepository.count());
            }
        };
    }
}





