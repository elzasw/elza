package cz.tacr.elza.service;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;

@Service
public class AccessPointConnectorService {

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private UserService userService;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    // method is synchronized with synchronizeAccessPointsForExternalSystem
    // only one of then can run due to manipulation with queue
    @AuthMethod(permission = { UsrPermission.Permission.AP_EXTERNAL_WR })
    synchronized public void disconnectAccessPoint(ApAccessPoint accessPoint, String externalSystemCode) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint,
                                                                                                apExternalSystem);
        ApBinding binding = bindingState.getBinding();
        // Odstraneni ze synchronizacni fronty
        int numDeleted = extSyncsQueueItemRepository.deleteByAccessPoint(accessPoint);
        numDeleted += extSyncsQueueItemRepository.deleteByBinding(binding);
        if (numDeleted > 0) {
            extSyncsQueueItemRepository.flush();
        }

        dataRecordRefRepository.disconnectBinding(binding);
        bindingItemRepository.deleteByBinding(binding);
        bindingStateRepository.deleteByBinding(binding);
        bindingRepository.delete(binding);
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
    }

    @AuthMethod(permission = {UsrPermission.Permission.AP_EXTERNAL_WR})
    public ExtSyncsQueueItem createExtSyncsQueueItem(Integer accessPointId, String externalSystemCode) {
    	ApExternalSystem extSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        // check AP state
        ApState apState = accessPointService.getApState(accessPointId);
        switch(apState.getStateApproval()) {
        case APPROVED:
        	break;
        case NEW:
        case TO_AMEND:
        	// Kontrola pripustnosti stavu
        	if(extSystem.getPublishOnlyApproved()==null||
        		!extSystem.getPublishOnlyApproved()) {
        		// pokud neni omezeni definovano nebo neni nastaveno
        		//  -> lze publikovat
        		break;
        	}
        default:
        	throw new BusinessException("Entita v tomto stavu nemůže být předána do externího systému.", BaseCode.INVALID_STATE)
                .set("accessPointId", apState.getAccessPointId())
                .set("state", apState.getStateApproval());
        }

        ApAccessPoint accessPoint = apState.getAccessPoint();

        // check ext_sync_queue
        if (extSyncsQueueItemRepository.countByAccesPointAndExternalSystemAndState(accessPoint, extSystem, ExtAsyncQueueState.EXPORT_NEW) != 0) {
            throw new BusinessException("Entita již čeká na zpracování ve frontě.", BaseCode.INVALID_STATE)
                .set("accessPointId", apState.getAccessPointId())
                .set("externalSystemCode", externalSystemCode);
        }

        UsrUser user = userService.getLoggedUser();
        ExtSyncsQueueItem item = externalSystemService.createExtSyncsQueueItem(accessPoint, extSystem, null, null,
                                                      ExtAsyncQueueState.EXPORT_NEW,
                                                      OffsetDateTime.now(),
                                                      user);
        accessPointService.publishExtQueueAddEvent(item);
        return item;
    }
}
