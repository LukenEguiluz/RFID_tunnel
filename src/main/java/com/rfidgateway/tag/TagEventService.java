package com.rfidgateway.tag;

import com.rfidgateway.controller.RealtimeEventController;
import com.rfidgateway.inventory.InventoryEpcService;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.model.TagEvent;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.repository.TagEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
public class TagEventService {

    @Autowired
    private TagEventRepository tagEventRepository;

    @Autowired
    private AntennaRepository antennaRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private WebSocketEventService webSocketEventService;

    @Autowired
    private InventoryEpcService inventoryEpcService;

    @Autowired(required = false)
    private RealtimeEventController realtimeEventController;

    public void processTagEvent(String readerId, String epc, Short antennaPort,
                                Double rssi, Double phase) {
        try {
            Reader readerEntity = readerRepository.findById(readerId).orElse(null);
            if (readerEntity != null
                && readerEntity.getOperationMode() == ReaderOperationMode.CONTINUOUS
                && readerEntity.getInventorySystemId() != null) {
                inventoryEpcService.recordTag(
                    readerEntity.getInventorySystemId(),
                    readerId,
                    epc,
                    antennaPort,
                    rssi,
                    phase
                );
                return;
            }

            String antennaId = antennaRepository
                .findByReaderIdAndPortNumber(readerId, antennaPort)
                .map(antenna -> antenna.getId())
                .orElse(readerId + "-antenna-" + antennaPort);

            TagEvent event = new TagEvent();
            event.setEpc(epc);
            event.setReaderId(readerId);
            event.setAntennaId(antennaId);
            event.setAntennaPort(antennaPort);
            event.setRssi(rssi);
            event.setPhase(phase);
            event.setDetectedAt(LocalDateTime.now());

            tagEventRepository.save(event);

            webSocketEventService.notifyTagDetected(readerId, epc, antennaId, antennaPort, rssi, phase);

            if (realtimeEventController != null) {
                realtimeEventController.broadcastEvent(event);
            }

        } catch (Exception e) {
            log.error("Error al procesar evento de tag: {}", e.getMessage(), e);
        }
    }
}
