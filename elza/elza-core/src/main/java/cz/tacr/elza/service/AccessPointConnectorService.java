package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import cz.tacr.elza.cam.ApiCamConnector;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.event.ApExternalSystemEvent;
import jakarta.transaction.Transactional;

@Service
public class AccessPointConnectorService {

    static private final Logger log = LoggerFactory.getLogger(AccessPointConnectorService.class);

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private StaticDataService staticDataService;

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

	@Autowired
	private ApplicationContext appCtx;

	@Autowired
    private cz.tacr.elza.cam.v1.CamConnector camConnectorV1;

    @Autowired
    private cz.tacr.elza.cam.v2.CamConnector camConnectorV2;

    @EventListener
    public void onExternalSystemChanged(ApExternalSystemEvent event) {
        ApExternalSystem extSys = event.getExternalSystem();
        if (extSys.getType().getVersionApi() == 1) {
            camConnectorV1.invalidate(extSys);
        } else {
            camConnectorV2.invalidate(extSys);
        }
    }

    /**
     * Výběr verze konektoru API CAM 
     * 
     * @param extSystem
     * @return
     */
	public ApiCamConnector getConnector(ApExternalSystem extSystem) {
		return extSystem.getType().getVersionApi() == 2 ? camConnectorV2 : camConnectorV1;
	}

	/**
     * Výběr verze konektoru API CAM 
     * 
     * @param extSysCode
     * @return
     */
	@Transactional
	public ApiCamConnector getConnector(String extSysCode) {
		ApExternalSystem extSystem = staticDataService.getData().getApExternalSystemByCode(extSysCode);
		return getConnector(extSystem);
	}

	/**
	 * Vynucené odeslání AP nebo odmítnutí
	 * 
	 * @param queueItemId
	 * @param body
	 */
	@Transactional
	public void exportForceOrNo(Integer queueItemId, boolean force) {
		ExtSyncsQueueItem queueItem = extSyncsQueueItemRepository.findById(queueItemId).orElse(null);
		if (queueItem == null) {
			throw new BusinessException("Queue item not found in the queue.", BaseCode.ID_NOT_EXIST).set("queueItemId", queueItemId);
		}
		if (!force) {
			// User declined to force-send the entity despite warnings. Record as CANCELLED
			// (not ERROR) so no ACCESS_POINT_EXPORT_FAILED event is fired — see the switch
			// in setQueueItemState, which has no case for EXPORT_CANCELLED.
			setQueueItemStateTA(queueItem, ExtAsyncQueueState.EXPORT_CANCELLED, queueItem.getStateMessage());
			return;
		}
		ApExternalSystem extSystem = staticDataService.getData().getApExternalSystemById(queueItem.getExternalSystemId());
		ApiCamConnector connector = getConnector(extSystem);
		connector.exportApForce(queueItem);
	}

	// method is synchronized with synchronizeAccessPointsForExternalSystem
    // only one of then can run due to manipulation with queue
    @AuthMethod(permission = { UsrPermission.Permission.AP_EXTERNAL_WR })
    synchronized public void disconnectAccessPoint(ApAccessPoint accessPoint, String externalSystemCode) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
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

    @Transactional
    public ItemSyncProcessor nextItemSyncProcessor(int pageSize) {
    	Iterable<ExtSyncsQueueItem> itemPage;

    	// upload confirmation ELZA->CAM
        itemPage = externalSystemService.getNextItems(1, ExtAsyncQueueState.EXPORT_PROCESSING);
        if (itemPage.iterator().hasNext()) {
        	ExtSyncsQueueItem queueItem = itemPage.iterator().next();
        	return appCtx.getBean(cz.tacr.elza.cam.v2.ItemSyncExportConfirmProcessor.class, queueItem);
        }

        // update CAM->ELZA
        itemPage = externalSystemService.getNextItems(pageSize, ExtAsyncQueueState.UPDATE, ExtAsyncQueueState.IMPORT_NEW);
        if(itemPage.iterator().hasNext()) {
            // prepare download processor
            return createDownloadProcessor(itemPage);
        }

        // send item ELZA->CAM
        itemPage = externalSystemService.getNextItems(1, ExtAsyncQueueState.EXPORT_NEW, ExtAsyncQueueState.EXPORT_START);
        if (itemPage.iterator().hasNext()) {
            ExtSyncsQueueItem queueItem = itemPage.iterator().next();
            StaticDataProvider sdp = staticDataService.getData();
            ApExternalSystem extSystem = sdp.getApExternalSystemById(queueItem.getExternalSystemId());
            if (extSystem.getType().getVersionApi()	 == 1) {
            	return appCtx.getBean(cz.tacr.elza.cam.v1.ItemSyncExportProcessor.class, queueItem);
            }
            return appCtx.getBean(cz.tacr.elza.cam.v2.ItemSyncExportProcessor.class, queueItem);
        }

        return null;
    }

    private ItemSyncProcessor createDownloadProcessor(Iterable<ExtSyncsQueueItem> itemPage) {
        ExtSyncsQueueItem firstItem = itemPage.iterator().next();
        ApExternalSystem externalSystem = firstItem.getExternalSystem();

        if (externalSystem.getType().getVersionApi() == 1) {
            cz.tacr.elza.cam.v1.ItemSyncImportProcessor isiProc = appCtx.getBean(cz.tacr.elza.cam.v1.ItemSyncImportProcessor.class, externalSystem.getExternalSystemId());
            fillDownloadProcessor(itemPage, externalSystem, isiProc::addQueueItem, isiProc::addBindingValue);
            return isiProc;
        }
        cz.tacr.elza.cam.v2.ItemSyncImportProcessor isiProc = appCtx.getBean(cz.tacr.elza.cam.v2.ItemSyncImportProcessor.class, externalSystem.getExternalSystemId());
        fillDownloadProcessor(itemPage, externalSystem, isiProc::addQueueItem, isiProc::addBindingValue);
        return isiProc;
    }

    private void fillDownloadProcessor(Iterable<ExtSyncsQueueItem> itemPage,
                                       ApExternalSystem externalSystem,
                                       Consumer<ExtSyncsQueueItem> addQueueItem,
                                       Consumer<String> addBindingValue) {
        List<Integer> bindingIds = new ArrayList<>(), apIds = new ArrayList<>();

        // read binding values
        for (ExtSyncsQueueItem queueItem : itemPage) {
            // do not mix data from different external systems
            if (!externalSystem.getExternalSystemId().equals(queueItem.getExternalSystemId())) {
                break;
            }
            addQueueItem.accept(queueItem);
            if (queueItem.getBindingId() != null) {
                bindingIds.add(queueItem.getBindingId());
            } else if (queueItem.getAccessPointId() != null) {
                apIds.add(queueItem.getAccessPointId());
            }
        }
        if (CollectionUtils.isNotEmpty(apIds)) {
            List<ApBindingState> bindingStates = bindingStateRepository.findByAccessPointIdsAndExternalSystem(apIds, externalSystem);
            bindingStates.forEach(bs -> addBindingValue.accept(bs.getBinding().getValue()));
        }
        if (CollectionUtils.isNotEmpty(bindingIds)) {
            List<ApBinding> bindings = bindingRepository.findAllById(bindingIds);
            bindings.forEach(b -> addBindingValue.accept(b.getValue()));
        }
    }
    
    /**
     * Set state of single item inside transaction
     *
     * @param itemId
     * @param state
     * @param dateTime
     * @param message
     */
    @Transactional
    public void setQueueItemStateTA(Integer itemId, 
    		                        ExtAsyncQueueState state,
                                    String message) {
        ExtSyncsQueueItem queueItem = extSyncsQueueItemRepository.findById(itemId)
        		.orElseThrow(() -> new ObjectNotFoundException("Prvek fronty neexistuje", BaseCode.ID_NOT_EXIST).setId(itemId));

        setQueueItemState(Collections.singletonList(queueItem), state, message, null, null, null);
    }

    public void setQueueItemState(ExtSyncsQueueItem queueItem, 
    		                      ExtAsyncQueueState state,
                                  String message) {
        setQueueItemState(Collections.singletonList(queueItem), state, message, null, null, null);
    }

    @Transactional
    public void setQueueItemStateTA(ExtSyncsQueueItem item,
    		                        ExtAsyncQueueState state,
                                    String message,
                                    String batchId,
                                    String data,
                                    String forceKey) {
        setQueueItemState(Collections.singletonList(item), state, message, batchId, data, forceKey, null);
    }

    /**
     * Overload that additionally persists the upload payload onto the queue item.
     * Pass {@code null} for {@code uploadMap} to leave the existing value unchanged
     * (which is what all other overloads do).
     */
    @Transactional
    public void setQueueItemStateTA(ExtSyncsQueueItem item,
                                    ExtAsyncQueueState state,
                                    String message,
                                    String batchId,
                                    String data,
                                    String forceKey,
                                    String uploadMap) {
        setQueueItemState(Collections.singletonList(item), state, message, batchId, data, forceKey, uploadMap);
    }

    @Transactional
    public void setQueueItemStateTA(ExtSyncsQueueItem item,
    		                        ExtAsyncQueueState state,
                                    String message) {
        setQueueItemState(Collections.singletonList(item), state, message, null, null, null, null);
    }

    @Transactional
    public void setQueueItemStateTA(ExtSyncsQueueItem item,
    		                        ExtAsyncQueueState state) {
        setQueueItemState(Collections.singletonList(item), state, null, null, null, null, null);
    }

    public void setQueueItemState(List<ExtSyncsQueueItem> items,
                                  ExtAsyncQueueState state,
                                  String message,
                                  String batchId,
                                  String data,
                                  String forceKey) {
        setQueueItemState(items, state, message, batchId, data, forceKey, null);
    }

    public void setQueueItemState(List<ExtSyncsQueueItem> items,
                                  ExtAsyncQueueState state,
                                  String message,
                                  String batchId,
                                  String data,
                                  String forceKey,
                                  String uploadMap) {
		// check message length
		if (StringUtils.isNotEmpty(message)) {
			if(message.length()>StringLength.LENGTH_4000) {
				log.error("Received very long error message, original message: {}", message);
				message = message.substring(0, StringLength.LENGTH_4000-1);
			}
		}
		for (ExtSyncsQueueItem item : items) {
			if (state != null) {
				item.setState(state);
				item.setDate(OffsetDateTime.now());
				item.setStateMessage(message);
				item.setBatchId(batchId);
				item.setData(data);
				item.setForceKey(forceKey);
				// upload_map lifecycle: clear on terminal states (info no longer useful);
				// otherwise, overwrite only if the caller supplied a new payload — null means
				// "leave as is" so NEED_CONFIRM transitions don't wipe the upload-time payload.
				switch (state) {
				case EXPORT_OK:
				case EXPORT_CANCELLED:
				case ERROR:
					item.setUploadMap(null);
					break;
				default:
					if (uploadMap != null) {
						item.setUploadMap(uploadMap);
					}
					break;
				}
				switch (state) {
				case EXPORT_START:
					accessPointService.publishExtQueueProcessStartedEvent(item);
					break;
				case EXPORT_NEED_CONFIRM:
					accessPointService.publishExtQueueProcessNeedConfirmEvent(item);
					break;
				case EXPORT_OK:
					accessPointService.publishExtQueueProcessCompletedEvent(item);
					break;
				case ERROR:
					accessPointService.publishExtQueueProcessFailedEvent(item);
					break;
				}
			}
		}
		extSyncsQueueItemRepository.saveAll(items);
	}
}
