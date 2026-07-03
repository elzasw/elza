package cz.tacr.elza.cam;

import java.time.Duration;
import java.util.List;

import jakarta.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import cz.tacr.elza.cam.SyncConfig.SynchronizationInfo;
import cz.tacr.elza.metrics.ElzaMonitoringMetrics;
import cz.tacr.elza.service.AccessPointConnectorService;

/**
 * Časovač pro noční synchronizace přístupových bodů s CAM
 */
@Service
public class CamScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CamScheduler.class);

    @Autowired
    private AccessPointConnectorService apConnectService;

    @Autowired
    private SyncConfig syncConfig;

    @Autowired
    private ElzaMonitoringMetrics monitoringMetrics;

    private boolean enabled = false;

    public void start() {
        enabled = true;
    }

    public void stop() {
        enabled = false;
    }

    @Override
    @Transactional
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        List<SynchronizationInfo> siList = syncConfig.getConfig();
        if (CollectionUtils.isEmpty(siList)) {
            return;
        }
        for (SynchronizationInfo si : siList) {
            configureTask(si, taskRegistrar);
        }
    }

    private void configureTask(SynchronizationInfo syncConfig, ScheduledTaskRegistrar taskRegistrar) {
        if (StringUtils.isNotBlank(syncConfig.getSyncAt())) {
            taskRegistrar.addCronTask(() -> runSync(syncConfig.getCode()), syncConfig.getResetAt());
        }
        if (syncConfig.getSyncDelay() != null && syncConfig.getSyncDelay() > 0) {
            taskRegistrar.addFixedDelayTask(() -> runSync(syncConfig.getCode()), Duration.ofSeconds(syncConfig.getSyncDelay()));
        }
    }

    private void runSync(String extSysCode) {
        if (enabled) {
            log.debug("Accesspoint synchronization started.");
            apConnectService.getConnector(extSysCode).synchronizeAccessPointsForExternalSystem(extSysCode);
            // Poll completed without error (including "nothing changed") — record successful CAM communication.
            monitoringMetrics.recordCamPollSuccess(extSysCode);
            log.debug("Accesspoint synchronization finished.");
        }
    }
}
