package cz.tacr.elza.service.cam;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.service.ExternalSystemService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Časovač pro noční synchronizace přístupových bodů s CAM
 */
@Service
public class CamScheduler {

    @Autowired
    private CamService camService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Scheduled(cron = "${elza.synchronizeAP.cam:0 0 2 * * *}")
    public void synchronizeAccessPoints() {
        List<ApExternalSystem> externalSystems = externalSystemService.findAllApSystem();
        if (CollectionUtils.isNotEmpty(externalSystems)) {
            for (ApExternalSystem externalSystem : externalSystems) {
                if (externalSystem.getType() == ApExternalSystemType.CAM ||
                        externalSystem.getType() == ApExternalSystemType.CAM_UUID ||
                        externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
                    camService.synchronizeAccessPointsForExternalSystem(externalSystem);
                }
            }
        }
    }

}
