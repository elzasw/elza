package cz.tacr.elza.service.cam;

import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import cz.tacr.elza.service.cam.SyncConfig.SynchronizationInfo;

/**
 * Časovač pro noční synchronizace přístupových bodů s CAM
 */
@Service
public class CamScheduler
        implements SchedulingConfigurer {


    private static final Logger log = LoggerFactory.getLogger(CamScheduler.class);
    @Autowired
    private CamService camService;
    
    @Autowired
    private SyncConfig syncConfig;

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

    private void configureTask(SynchronizationInfo syncConfig,
                                 ScheduledTaskRegistrar taskRegistrar) {
        if (StringUtils.isNotBlank(syncConfig.syncAt)) {
            taskRegistrar.addCronTask(() -> runSync(syncConfig.getCode()),
                                      syncConfig.resetAt);
        }
        if (syncConfig.syncDelay != null && syncConfig.syncDelay > 0) {
            taskRegistrar.addFixedDelayTask(() -> runSync(syncConfig.getCode()),
                                            syncConfig.syncDelay * 1000);
        }
    }

    private void runSync(String code) {
        if (enabled) {
            log.debug("Accesspoint synchronization started.");
            camService.synchronizeAccessPointsForExternalSystem(code);
            log.debug("Accesspoint synchronization finished.");
        }
    }

}
