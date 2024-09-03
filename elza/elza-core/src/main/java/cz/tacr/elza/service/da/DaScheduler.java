package cz.tacr.elza.service.da;

import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DaScheduler implements SchedulingConfigurer {


    private static final Logger log = LoggerFactory.getLogger(DaScheduler.class);

    @Autowired
    private DaSyncConfig daSyncConfig;

    @Autowired
    private DaService daService;

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
        List<DaSyncConfig.SynchronizationInfo> siList = daSyncConfig.getConfig();
        if (CollectionUtils.isEmpty(siList)) {
            return;
        }
        for (DaSyncConfig.SynchronizationInfo si : siList) {
            configureTask(si, taskRegistrar);
        }
    }

    private void configureTask(DaSyncConfig.SynchronizationInfo syncConfig,
                               ScheduledTaskRegistrar taskRegistrar) {
        if (StringUtils.isNotBlank(syncConfig.getSyncAt())) {
            taskRegistrar.addCronTask(() -> runSync(syncConfig.getCode()),
                    syncConfig.getResetAt());
        }
        if (syncConfig.getSyncDelay() != null && syncConfig.getSyncDelay() > 0) {
            taskRegistrar.addFixedDelayTask(() -> runSync(syncConfig.getCode()),
                    syncConfig.getSyncDelay() * 1000);
        }
    }

    private void runSync(String code) {
        if (enabled) {
            log.debug("Da repository synchronization started.");
            daService.synchronizeDaRepository(code);
            log.debug("Da repository synchronization finished.");
        }
    }
}
