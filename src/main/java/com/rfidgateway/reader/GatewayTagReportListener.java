package com.rfidgateway.reader;

import com.impinj.octane.ImpinjReader;
import com.impinj.octane.Tag;
import com.impinj.octane.TagReport;
import com.impinj.octane.TagReportListener;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.tag.TagEventService;
import com.rfidgateway.session.SessionService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GatewayTagReportListener implements TagReportListener {

    private final String readerId;
    private final TagEventService tagEventService;
    private final ReaderRepository readerRepository;
    private SessionService sessionService;
    private Double rssiMinDbm;

    public GatewayTagReportListener(String readerId, TagEventService tagEventService, ReaderRepository readerRepository) {
        this.readerId = readerId;
        this.tagEventService = tagEventService;
        this.readerRepository = readerRepository;
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void setRssiMinDbm(Double rssiMinDbm) {
        this.rssiMinDbm = rssiMinDbm;
    }

    private boolean isTunnelSessionDedupApplicable() {
        Reader r = readerRepository.findById(readerId).orElse(null);
        if (r == null || r.getOperationMode() == ReaderOperationMode.CONTINUOUS) {
            return false;
        }
        return sessionService != null && sessionService.hasActiveSession(readerId);
    }

    @Override
    public void onTagReported(ImpinjReader reader, TagReport report) {
        List<Tag> tags = report.getTags();

        log.info("onTagReported llamado - Lector: {}, Tags en reporte: {}", readerId, tags.size());

        if (tags.isEmpty()) {
            log.debug("Reporte vacío recibido del lector {}", readerId);
            return;
        }

        log.info("Lector {} reportó {} tag(s)", readerId, tags.size());

        for (Tag tag : tags) {
            try {
                String epc = tag.getEpc().toHexString();
                Short antennaPort = tag.isAntennaPortNumberPresent()
                    ? tag.getAntennaPortNumber()
                    : null;
                Double rssi = tag.isPeakRssiInDbmPresent()
                    ? tag.getPeakRssiInDbm()
                    : null;
                Double phase = tag.isRfPhaseAnglePresent()
                    ? tag.getPhaseAngleInRadians()
                    : null;

                if (rssiMinDbm != null && rssi != null && rssi < rssiMinDbm) {
                    log.debug("Tag ignorado (RSSI {} dBm < umbral {} dBm) - Lector: {}, EPC: {}",
                            rssi, rssiMinDbm, readerId, epc);
                    continue;
                }

                if (isTunnelSessionDedupApplicable()) {
                    if (sessionService.hasSeenInActiveSession(readerId, epc)) {
                        log.debug("EPC {} ya visto en esta sesión, omitido (solo uniques por lectura)", epc);
                        continue;
                    }
                }

                log.info("TAG DETECTADO - Lector: {}, EPC: {}, Antena: {}, RSSI: {} dBm",
                        readerId, epc, antennaPort, rssi);

                tagEventService.processTagEvent(
                    readerId,
                    epc,
                    antennaPort,
                    rssi,
                    phase
                );

                if (isTunnelSessionDedupApplicable()) {
                    sessionService.addEpcToSession(readerId, epc);
                    log.debug("EPC {} agregado a sesión activa del lector {}", epc, readerId);
                }

            } catch (Exception e) {
                log.error("Error al procesar tag del lector {}: {}", readerId, e.getMessage(), e);
            }
        }
    }
}
