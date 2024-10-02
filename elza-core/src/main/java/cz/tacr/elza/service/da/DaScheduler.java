package cz.tacr.elza.service.da;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.service.ExternalSystemService;
import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DaScheduler implements SchedulingConfigurer {


    private static final Logger log = LoggerFactory.getLogger(DaScheduler.class);

    @Autowired
    private DaSyncConfig daSyncConfig;

    @Autowired
    private DaService daService;

    @Autowired
    private ExternalSystemService externalSystemService;

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
        List<ArrDigitalRepository> digitalRepositoryList = externalSystemService.findDigitalRepository();

        if (CollectionUtils.isEmpty(digitalRepositoryList)) {
            return;
        }

        List<DaSyncConfig.SynchronizationInfo> siList = daSyncConfig.getConfig();
        Map<String, DaSyncConfig.SynchronizationInfo> siMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(siList)) {
            siMap = siList.stream().collect(Collectors.toMap(DaSyncConfig.SynchronizationInfo::getCode, s -> s));
            for (DaSyncConfig.SynchronizationInfo value : siMap.values()) {
                log.info("DA configuration: {}, {}, {}, {}", value.getCode(), value.getSyncDelay(), value.getSyncAt(), value.getResetAt());
            }
        } else {
            log.info("No synchronization configuration found");
        }

        for (ArrDigitalRepository digitalRepository : digitalRepositoryList) {
            DaSyncConfig.SynchronizationInfo si = siMap.getOrDefault(digitalRepository.getCode(), null);
            configureTask(digitalRepository, si, taskRegistrar);
        }
    }

    private void configureTask(ArrDigitalRepository digitalRepository,
                               @Nullable DaSyncConfig.SynchronizationInfo syncConfig,
                               ScheduledTaskRegistrar taskRegistrar) {
        if (syncConfig == null) {
            taskRegistrar.addFixedDelayTask(() -> runSync(digitalRepository.getCode()),
                    15 * 60 * 1000);
        } else {
            if (StringUtils.isNotBlank(syncConfig.getSyncAt())) {
                taskRegistrar.addCronTask(() -> runSync(syncConfig.getCode()),
                        syncConfig.getResetAt());
            }
            if (syncConfig.getSyncDelay() != null && syncConfig.getSyncDelay() > 0) {
                taskRegistrar.addFixedDelayTask(() -> runSync(syncConfig.getCode()),
                        syncConfig.getSyncDelay() * 1000);
            }
        }
    }

    private void runSync(String code) {
        if (enabled) {
            log.info("Da repository synchronization started.");
            daService.synchronizeDaRepository(code);
            log.info("Da repository synchronization finished.");
        }
    }
}
