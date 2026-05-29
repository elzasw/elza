package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.BindingSyncInfo;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.ExtHistory;
import cz.tacr.elza.controller.vo.ExtIssue;
import cz.tacr.elza.controller.vo.ExtIssueSeverity;
import cz.tacr.elza.controller.vo.ExtIssueStatus;
import cz.tacr.elza.controller.vo.ExtParticipant;
import cz.tacr.elza.controller.vo.ExtParticipantRole;
import cz.tacr.elza.controller.vo.ExtRevision;
import cz.tacr.elza.controller.vo.ExtSystemProperty;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingIssue;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingParticipant;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApBindingSync;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.GisExternalSystem;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.domain.SysExternalSystemProperty;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingIssueRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingParticipantRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApBindingSyncRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.DigitizationFrontdeskRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
import cz.tacr.elza.repository.GisExternalSystemRepository;
import cz.tacr.elza.repository.SysExternalSystemPropertyRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.event.ApExternalSystemEvent;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Servisní třída pro práci s externími systémy.
 *
 * @since 05.12.2016
 */
@Service
public class ExternalSystemService {

    static private final Logger log = LoggerFactory.getLogger(ExternalSystemService.class);

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private ExternalSystemRepository externalSystemRepository;

    @Autowired
    private ApExternalSystemRepository apExternalSystemRepository;

    @Autowired
    private GisExternalSystemRepository gisExternalSystemRepository;

    @Autowired
    private DigitizationFrontdeskRepository digitizationFrontdeskRepository;

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    @Autowired
    private SysExternalSystemPropertyRepository sysExtSysPropertyRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingSyncRepository bindingSyncRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private ApBindingIssueRepository bindingIssueRepository;

    @Autowired
    private ApBindingParticipantRepository bindingParticipantRepository;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Vyhledá všechny externí systémy.
     * 
     * Pokud uživatel nemá oprávnění správce, je vrácena jen kopie
     * entit bez hesel a jiných důvěrných informací.
     *
     * @return seznam externích systémů
     */
    public List<SysExternalSystem> findAll() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if(userDetail==null) {
            throw new AccessDeniedException("User not authorized.", Collections.emptyList());
        }
        var extSystems = externalSystemRepository.findAll();
        
        AuthorizationRequest adminPermission = AuthorizationRequest.hasPermission(Permission.ADMIN);
        if(adminPermission.matches(userDetail)) {
	        return extSystems;
        }
    	// authorized user but not admin -> we have to return a copy
        // with minimum information        
        return extSystems.stream().map(es -> {
        	SysExternalSystem ses = HibernateUtils.unproxy(es);
        	SysExternalSystem copy;
        	if(ses instanceof ApExternalSystem) {
        		ApExternalSystem aes = (ApExternalSystem)ses;
        		var aesCopy = new ApExternalSystem(aes);
        		copy = aesCopy;
        	} else 
        	if(ses instanceof ArrDigitalRepository) {
        		ArrDigitalRepository ardr = (ArrDigitalRepository)ses;
				var ardCopy = new ArrDigitalRepository(ardr);
				copy = ardCopy;
        	} else 
        	if(ses instanceof ArrDigitizationFrontdesk) {
        		ArrDigitizationFrontdesk adf = (ArrDigitizationFrontdesk)ses;
				var adfCopy = new ArrDigitizationFrontdesk(adf);
				copy = adfCopy;
        	} else 
			if(ses instanceof GisExternalSystem) {
				GisExternalSystem ges = (GisExternalSystem)ses;
				var gesCopy = new GisExternalSystem(ges);
				copy = gesCopy;
			} else {
				throw new SystemException("Unknown external system type: "+es.getClass().getName());
			}
        	// anonymize
			copy.setPassword(null);
			copy.setUsername(null);
			copy.setApiKeyId(null);
			copy.setApiKeyValue(null);
			return copy;
        }).collect(Collectors.toList());
    }

    /**
     * Vyhledá všechny externí systémy pro přistupové body.
     *
     * @return seznam externích systémů
     */
    @Transactional
    public List<ApExternalSystem> findAllApSystem() {
        return apExternalSystemRepository.findAll();
    }

    /**
     * Vyhledá všechny externí systémy pro přistupové body.
     *
     * @return seznam externích systémů
     */
    @Transactional
    public List<GisExternalSystem> findAllGisSystem() {
        return gisExternalSystemRepository.findAll();
    }

    /**
     * Vyhledání externího systému podle kódu.
     *
     * @param code
     *            kód externího systému, který hledáme
     * @return nalezený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem findByCode(final String code) {
        return externalSystemRepository.findByCode(code);
    }

    /**
     * Vyhledání externího systému podle kódu nebo id.
     * 
     * @param code 
     *            kód externího systému, který hledáme nebo id
     * @return nalezený externí systém nebo null
     */
    public ApExternalSystem findExternalSystemByCodeOrId(final String code) {
    	ApExternalSystem extSystem = apExternalSystemRepository.findByCode(code);
    	if (extSystem == null) {
			Integer id = Integer.parseInt(code);
    		return apExternalSystemRepository.findById(id).orElse(null);
    	}
    	return extSystem;
    }

    /**
     * Vyhledání externího systému podle kódu.
     *
     * @param code
     *            kód externího systému, který hledáme
     * @return nalezený externí systém
     */
    public ApExternalSystem findApExternalSystemByCode(final String code) {
        ApExternalSystem extSystem = apExternalSystemRepository.findByCode(code);
        if (extSystem == null) {
            throw new BusinessException("External system not found, code: " + code, BaseCode.ID_NOT_EXIST)
                    .set("code", code);
        }
        return extSystem;
    }

    /**
     * Vyhledání externího systému podle id.
     *
     * @param id
     *            identifikátor externího systému, který hledáme
     * @return nalezený externí systém
     */
    public ApExternalSystem findApExternalSystemById(final Integer id) {
        Optional<ApExternalSystem> extSystem = apExternalSystemRepository.findById(id);
        if (!extSystem.isPresent()) {
            throw new BusinessException("External system not found, id: " + id, BaseCode.ID_NOT_EXIST)
                    .set("id", id);
        }
        return extSystem.get();
    }

    /**
     * Vyhledání externího systému podle identifikátoru bez kontroly práv.
     *
     * @param id
     *            identifikátor externího systému
     * @return nalezený externí systém
     */
    public ApExternalSystem getExternalSystemInternal(final Integer id) {
        return apExternalSystemRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Externí systém neexistuje", BaseCode.ID_NOT_EXIST)
                        .setId(id));
    }

    /**
     * Vyhledání externího systému podle identifikátoru.
     *
     * @param id
     *            identifikátor externího systému
     * @return nalezený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem findOne(final Integer id) {
        return externalSystemRepository.getOneCheckExist(id);
    }

    /**
     * Vytvoření externího systému.
     *
     * @param externalSystem
     *            vytvářený externí systém
     * @return vytvořený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem create(final SysExternalSystem externalSystem) {

        validateExternalSystem(externalSystem, true);
        externalSystemRepository.save(externalSystem);
        sendCreateExternalSystemNotification(externalSystem.getExternalSystemId());

        staticDataService.reloadOnCommit();
        return externalSystem;
    }

    /**
     * Smazání exteního systému.
     *
     * @param id
     *            identifikátor mazaného externího systému
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public void delete(final SysExternalSystem externalSystem) {
        sendDeleteExternalSystemNotification(externalSystem.getExternalSystemId());
        externalSystemRepository.deleteById(externalSystem.getExternalSystemId());

        if (externalSystem instanceof ApExternalSystem extSys) {
            eventPublisher.publishEvent(new ApExternalSystemEvent(this, extSys));
        }

        staticDataService.reloadOnCommit();
    }

    /**
     * Smazání záznamu z tabulky ExtSyncsQueueItem
     *
     * @param extSyncItemId
     */
    public void deleteQueueItem(final Integer extSyncItemId) {
        Optional<ExtSyncsQueueItem> queueItem = extSyncsQueueItemRepository.findById(extSyncItemId);

        if (queueItem.isPresent()) {

            ExtSyncsQueueItem item = queueItem.get();
            UsrUser loggedUser = userService.getLoggedUser();

            // smazat záznam může pouze Admin nebo autor
            if (!userService.hasPermission(Permission.ADMIN)) {
                if (loggedUser == null || !Objects.equals(loggedUser.getUserId(), item.getUserId())) {
                    throw new SystemException("Uživatel nemá oprávnění na vymazávání přístupového bodu ve frontě", BaseCode.INSUFFICIENT_PERMISSIONS)
                    .set("accessPointId", item.getAccessPointId())
                    .set("userId", item.getUserId());
                }
            }

            if (item.getState() != ExtAsyncQueueState.EXPORT_START) {
                extSyncsQueueItemRepository.deleteById(extSyncItemId);
            }
        }
    }

    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public void deleteBindingSync(final ApExternalSystem externalSystem) {
        bindingSyncRepository.deleteByApExternalSystem(externalSystem);
    }

    /**
     * Externí systém typu - Digitalizační linka.
     *
     * @return digitalizační linka
     * @param digitizationFrontdeskId
     */
    public ArrDigitizationFrontdesk findDigitizationFrontdesk(final Integer digitizationFrontdeskId) {
        return digitizationFrontdeskRepository.getOneCheckExist(digitizationFrontdeskId);
    }

    /**
     * @return digitalizační linky
     */
    public List<SysExternalSystem> findAllWithoutPermission() {
        return externalSystemRepository.findAll();
    }

    /**
     * Upravení externího systému.
     *
     * @param externalSystem
     *            upravovaný externí systém
     * @return upravený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem update(final SysExternalSystem externalSystem) {
        staticDataService.reloadOnCommit();

        SysExternalSystem original = externalSystemRepository.getOneCheckExist(externalSystem.getExternalSystemId());

        // if type changed in ApExternalSystem
        if (externalSystem instanceof ApExternalSystem extSys 
        		&& original instanceof ApExternalSystem origExtSys
        		&& origExtSys.getType() != extSys.getType()) {
        	
        	ApExternalSystemType extSysType = extSys.getType();
        	ApExternalSystemType origExtSysType = origExtSys.getType();

        	// if it's a switch between versions
        	if (!extSysType.isSameType(origExtSysType)) {
                throw new SystemException("Změna typu Externího Systému není možná", BaseCode.INVALID_STATE)
	                .set("extSystemId", origExtSys.getExternalSystemId())
	                .set("extSystemType", origExtSysType)
	            	.set("newExtSystemType", extSysType);
        	}

        	eventPublisher.publishEvent(new ApExternalSystemEvent(this, origExtSys));
        }

        validateExternalSystem(externalSystem, false);
        sendUpdateExternalSystemNotification(externalSystem.getExternalSystemId());

        return externalSystemRepository.save(externalSystem);
    }

    /**
     * Validace externího systému.
     *
     * @param externalSystem
     *            validovaný externí systém
     * @param create
     *            příznak, zda-li se jedná o validaci při vytvářený externího
     *            systému
     */
    private void validateExternalSystem(final SysExternalSystem externalSystem, final boolean create) {
        if (create) {
            if (externalSystem.getExternalSystemId() != null) {
                throw new SystemException("Identifikátor externího systému musí být při vytváření prázdný",
                        BaseCode.ID_EXIST).set("id", externalSystem.getExternalSystemId());
            }
        } else {
            if (externalSystem.getExternalSystemId() == null) {
                throw new SystemException("Identifikátor externího systému musí být při editaci vyplněň",
                        BaseCode.ID_NOT_EXIST);
            }
        }

        if (ObjectUtils.isEmpty(externalSystem.getCode())) {
            throw new BusinessException("Nevyplněno pole: code", BaseCode.PROPERTY_NOT_EXIST).set("property", "code");
        }

        if (ObjectUtils.isEmpty(externalSystem.getName())) {
            throw new BusinessException("Nevyplněno pole: name", BaseCode.PROPERTY_NOT_EXIST).set("property", "name");
        }

        // extra validace pro ArrDigitalRepository
        if (externalSystem instanceof ArrDigitalRepository) {
            if (((ArrDigitalRepository) externalSystem).getSendNotification() == null) {
                throw new BusinessException("Nevyplněno pole: sendNotification", BaseCode.PROPERTY_NOT_EXIST).set(
                                                                                                                  "property",
                                                                                                                  "sendNotification");
            }
        }
    }

    /**
     * Odešle notifikaci do klienta, že se změnil externí systém.
     *
     * @param externalSystemId
     *            id ex. systému
     */
    private void sendUpdateExternalSystemNotification(final Integer externalSystemId) {
        eventNotificationService.publishEvent(new EventId(EventType.EXTERNAL_SYSTEM_UPDATE, externalSystemId));
    }

    /**
     * Odešle notifikaci do klienta, že se vytvořil externí systém.
     *
     * @param externalSystemId
     *            id ex. systému
     */
    private void sendCreateExternalSystemNotification(final Integer externalSystemId) {
        eventNotificationService.publishEvent(new EventId(EventType.EXTERNAL_SYSTEM_CREATE, externalSystemId));
    }

    /**
     * Odešle notifikaci do klienta, že se smazal externí systém.
     *
     * @param externalSystemId
     *            id ex. systému
     */
    private void sendDeleteExternalSystemNotification(final Integer externalSystemId) {
        eventNotificationService.publishEvent(new EventId(EventType.EXTERNAL_SYSTEM_DELETE, externalSystemId));
    }

    /**
     * Vyhledá všechny externí systémy typu - Uložiště digitalizátů.
     *
     * @return seznam uložišť digitalizátů
     */
    public List<ArrDigitalRepository> findDigitalRepository() {
        return digitalRepositoryRepository.findAll();
    }

    public List<ArrDigitalRepository> findDigitalRepositoryByIds(@NotNull Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return digitalRepositoryRepository.findAllById(ids);
    }

    /**
     * Create binding based on external system code
     *
     * @param scope
     * @param value
     * @param externalSystemCode
     * @return
     */
    public ApBinding createBinding(final String value,
                                   final String externalSystemCode) {
    	log.debug("creating binding, externalSystemCode: {}, value: {}",externalSystemCode, value);
    	
        ApExternalSystem apExternalSystem = apExternalSystemRepository.findByCode(externalSystemCode);
        if (apExternalSystem == null) {
            throw new BusinessException("External system not exists, code: " + externalSystemCode,
                    BaseCode.INVALID_STATE)
                            .set("code", externalSystemCode);
        }

        return createApBinding(value, apExternalSystem, true);
    }

    /**
     * Create AP Binding in DB (saveAndFlush)
     *
     * Method will flush new binding immediately to the DB
     * to prevent duplicated bindings.
     *
     * @param value
     *            Binding value
     * @param apExternalSystem
     *            Binded system
     * @param flush
     *            Flag if binding should be immediately flushed to DB
     * @return saved binding
     */
    public ApBinding createApBinding(final String value,
                                     final ApExternalSystem apExternalSystem,
                                     final boolean flush) {
    	log.debug("Creating binding, extSystem: {}, value: {}, flush: {}", apExternalSystem.getName(), value, flush);
    	
    	Objects.requireNonNull(value);
    	Objects.requireNonNull(apExternalSystem);

        ApBinding apBinding = new ApBinding();
        apBinding.setValue(value);
        apBinding.setApExternalSystem(apExternalSystem);
        if (flush) {
            return bindingRepository.saveAndFlush(apBinding);
        } else {
            return bindingRepository.save(apBinding);
        }
    }

    /**
     * Create new binding state
     *
     * @param binding
     * @param accessPoint
     * @param apChange
     * @param state
     * @param revisionUuid
     * @param userName
     * @param replacedById
     * @param syncState
     * @param preferredPart
     * @param apType
     * @return
     */
    public ApBindingState createBindingState(final ApBinding binding,
                                             final ApAccessPoint accessPoint,
                                             final ApChange apChange,
                                             final String state,
                                             final String revisionUuid,
                                             final String userName,
                                             final Long replacedById,
                                             final SyncState syncState,
                                             @Nullable final ApPart preferredPart,
                                             @Nullable final ApType apType) {
        ApBindingState apBindingState = new ApBindingState();
        apBindingState.setBinding(binding);
        apBindingState.setAccessPoint(accessPoint);
        apBindingState.setApExternalSystem(binding.getApExternalSystem());
        apBindingState.setExtState(state);
        apBindingState.setExtRevision(revisionUuid);
        Validate.isTrue(userName == null || userName.length() <= StringLength.LENGTH_250, "UserName length exceeds the limit");
        apBindingState.setExtUser(userName);
        apBindingState.setExtReplacedBy(replacedById == null ? null : String.valueOf(replacedById));
        apBindingState.setCreateChange(apChange);
        apBindingState.setSyncOk(syncState);
        apBindingState.setPreferredPart(preferredPart);
        apBindingState.setApType(apType);

        return bindingStateRepository.saveAndFlush(apBindingState);
    }

    /**
     * Create new binding state based on current state
     *
     * @param oldbindingState
     * @param apChange
     * @param state
     * @param revisionUuid
     * @param user
     * @param extReplacedBy
     * @param syncState
     * @return
     */
    public ApBindingState createBindingState(ApBindingState oldbindingState,
                                             ApChange apChange,
                                             String state,
                                             String revisionUuid,
                                             String user,
                                             String extReplacedBy,
                                             final SyncState syncState,
                                             @Nullable final ApPart preferredPart,
                                             @Nullable final ApType apType) {
        // check if new state is needed
        if (Objects.equals(state, oldbindingState.getExtState()) &&
                Objects.equals(revisionUuid, oldbindingState.getExtRevision()) &&
                Objects.equals(user, oldbindingState.getExtUser()) &&
                Objects.equals(extReplacedBy, oldbindingState.getExtReplacedBy()) &&
                Objects.equals(syncState, oldbindingState.getSyncOk())) {
            // we can use old state only if not synced
            if (syncState == SyncState.NOT_SYNCED) {
                return oldbindingState;
            }
            // if item is synced -> new sync state has to be created
        }

        oldbindingState.setDeleteChange(apChange);
        bindingStateRepository.saveAndFlush(oldbindingState);

        return createBindingState(oldbindingState.getBinding(),
                                  oldbindingState.getAccessPoint(),
                                  apChange,
                                  state,
                                  revisionUuid,
                                  user,
                                  extReplacedBy == null? null : Long.valueOf(extReplacedBy),
                                  syncState,
                                  preferredPart,
                                  apType);
    }

    public ApBindingItem createApBindingItem(final ApBinding binding,
                                             ApChange apChange, 
                                             final String value,
                                             final ApPart part,
                                             final ApItem item) {
    	Objects.requireNonNull(binding);
    	Objects.requireNonNull(apChange);
    	Objects.requireNonNull(value);
        Validate.isTrue(part == null ^ item == null);

        ApBindingItem apBindingItem = new ApBindingItem();
        apBindingItem.setBinding(binding);
        apBindingItem.setValue(value);
        apBindingItem.setPart(part);
        apBindingItem.setItem(item);
        apBindingItem.setCreateChange(apChange);
        return bindingItemRepository.save(apBindingItem);
    }

    public ApBinding findByValueAndExternalSystemCode(final String archiveEntityId,
                                                      final String externalSystemCode) {
        return bindingRepository.findByValueAndExternalSystemCode(archiveEntityId, externalSystemCode);
    }

    public ApBinding findByValueAndExternalSystem(final String archiveEntityId,
                                                  final ApExternalSystem externalSystem) {
        return bindingRepository.findByValueAndExternalSystem(archiveEntityId, externalSystem);
    }

    public Optional<ApBindingState> getBindingState(final ApBinding binding) {
        return bindingStateRepository.findActiveByBinding(binding);
    }

    public ApBindingState getBindingState(final ApAccessPoint accessPoint, final ApExternalSystem externalSystem) {
        return bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, externalSystem);
    }

    public ApBindingState getBindingState(final Integer accessPointId, final Integer externalSystemId) {
        return bindingStateRepository.findByAccessPointIdAndExternalSystemId(accessPointId, externalSystemId);
    }

    @Transactional
    public BindingSyncInfo getBindingSync(final String extSystemCode, final String transactionUuid) {
        ApExternalSystem externalSystem = findApExternalSystemByCode(extSystemCode);

        ApBindingSync bindingSync = bindingSyncRepository.findByApExternalSystem(externalSystem);
        if (bindingSync == null) {
            bindingSync = new ApBindingSync();
            bindingSync.setApExternalSystem(externalSystem);
            bindingSync.setLastTransaction(transactionUuid);
            bindingSync = bindingSyncRepository.save(bindingSync);
        }
        return new BindingSyncInfo(bindingSync.getBindingSyncId(),
                                   externalSystem.getExternalSystemId(),
                                   bindingSync.getLastTransaction(), bindingSync.getToTransaction(),
                                   bindingSync.getPage(), bindingSync.getCount());
    }

    @Transactional
    public void resetTransaction(final Integer bindingSyncId, final String transactionUuid) {
        ApBindingSync bindingSync = bindingSyncRepository.getOneCheckExist(bindingSyncId);
        bindingSync.setLastTransaction(transactionUuid);
        bindingSync.setPage(null);
        bindingSyncRepository.save(bindingSync);
    }

    public ApBindingItem findByBindingAndUuid(ApBinding binding, String uuid) {
        return bindingItemRepository.findByBindingAndUuid(binding, uuid);
    }

    public List<ApBindingItem> getBindingItems(final ApBinding binding) {
        return bindingItemRepository.findByBinding(binding);
    }

    public List<ApBindingItem> findItemsForSync(final ApBinding binding, final Integer syncChangeId) {
        return bindingItemRepository.findItemsForSync(binding, syncChangeId);
    }

    /**
     * Return active binding state
     *
     * Binding is also fetched.
     *
     * @param accessPoint
     * @param externalSystem
     * @return
     */
    public ApBindingState findByAccessPointAndExternalSystem(final ApAccessPoint accessPoint,
                                                             final ApExternalSystem externalSystem) {
        return bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, externalSystem);
    }

    public List<ApBinding> findBindings(List<String> recordCodes, ApExternalSystem externalSystem) {
        return ObjectListIterator.findIterable(recordCodes, recs -> {
            return bindingRepository.findByValuesAndExternalSystem(recs, externalSystem);
        });
    }

    public List<ApBindingState> findBindingStates(List<ApBinding> bindings) {
        return ObjectListIterator.findIterable(bindings,
                                               bs -> bindingStateRepository.findByBindings(bs));
    }

    /**
     * Return CAM-side issues attached to a binding, mapped to the
     * wire VO. The list is not versioned — only the current snapshot
     * from the last CAM sync is returned.
     *
     * @param bindingId id of the binding
     * @return issues for the binding (empty list when none)
     * @throws ObjectNotFoundException when no binding with the given id exists
     */
	public List<ExtIssue> findBindingIssues(Integer bindingId) {
	    ApBinding binding = bindingRepository.findById(bindingId)
	    		.orElseThrow(() -> new ObjectNotFoundException("Binding not found", BaseCode.ID_NOT_EXIST).setId(bindingId));

	    // access control by binding
	    checkBindingReadAccess(binding);

	    return bindingIssueRepository.findByBindingIdFetchRelated(bindingId).stream()
	            .map(ExternalSystemService::toExtIssue)
	            .toList();
    }

	/**
	 * Access control via binding
	 *  
	 * @param binding
	 */
	private void checkBindingReadAccess(ApBinding binding) {
	    UserDetail userDetail = userService.getLoggedUserDetail();
	    if (userDetail == null) {
	        throw new AccessDeniedException("User not authorized.", Collections.emptyList());
	    }
	    // fast path — global reader doesn't need a scope at allS
	    if (userDetail.hasPermission(Permission.AP_SCOPE_RD_ALL)) {
	        return;
	    }
	    Integer scopeId = null;
	    ApBindingState activeState = bindingStateRepository.findActiveByBinding(binding).orElse(null);
	    if (activeState != null) {
	        ApState lastState = stateRepository.findLastByAccessPointId(activeState.getAccessPointId());
	        if (lastState != null) {
	            scopeId = lastState.getScopeId();
	        }
	    }
	    AuthorizationRequest authRequest = AuthorizationRequest
	    		.hasPermission(Permission.AP_SCOPE_RD_ALL)
	            .or(Permission.AP_SCOPE_RD, scopeId);
	    if (!authRequest.matches(userDetail)) {
	        throw new AccessDeniedException("Read permission required for binding issues", authRequest.getPermissions());
	    }
	}

	private static ExtIssue toExtIssue(final ApBindingIssue bi) {
	    ExtIssue ei = new ExtIssue();
	    ei.setId(bi.getBindingIssueId());
	    ei.setUuid(bi.getUuid());
	    ei.setSeverity(ExtIssueSeverity.valueOf(bi.getSeverity().name()));
	    if (bi.getStatus() != null) {
	        ei.setStatus(ExtIssueStatus.valueOf(bi.getStatus().name()));
	    }
	    ei.setRuleCode(bi.getRuleCode());
	    ei.setIssueCode(bi.getIssueCode());
	    ei.setMessage(bi.getMessage());
	    ei.setSource(bi.getSource());
	    ei.setDetail(bi.getDetail());
	    ei.setNote(bi.getNote());
	    ei.setIssueFrom(bi.getIssueFrom());
	    ei.setExtFromRev(bi.getExtFromRev());
	    ei.setPartId(bi.getPartId());
	    ei.setItemId(bi.getItemId());
	    ei.setRelatedBindingId(bi.getRelatedBindingId());
	    if (bi.getRelatedBinding() != null) {
	        ei.setRelatedBindingExtValue(bi.getRelatedBinding().getValue());
	    }
	    return ei;
	}

	private static final int HISTORY_MAX_LIMIT = 100;

	/**
	 * Paginated revision history of a binding, newest first, with embedded
	 * participants. Returns {@code totalCount} (all revisions for the binding)
	 * and {@code incomplete} flag — true when the extPrevRevision chain points
	 * to a revision Elza does not have.
	 *
	 * @param bindingId id of the binding
	 * @param offset    page offset; null or negative coerced to 0
	 * @param limit     page size; null or non-positive coerced to {@value #HISTORY_MAX_LIMIT}, capped at the same value
	 * @throws ObjectNotFoundException when no binding with the given id exists
	 */
	public ExtHistory findBindingHistory(final Integer bindingId,
	                                     final Integer offset,
	                                     final Integer limit) {
	    ApBinding binding = bindingRepository.findById(bindingId)
	    		.orElseThrow(() -> new ObjectNotFoundException("Binding not found", BaseCode.ID_NOT_EXIST).setId(bindingId));

	    // access control by binding
	    checkBindingReadAccess(binding);

	    int effOffset = (offset == null || offset < 0) ? 0 : offset;
	    int effLimit = (limit == null || limit <= 0) ? HISTORY_MAX_LIMIT : Math.min(limit, HISTORY_MAX_LIMIT);

	    long totalCount = bindingStateRepository.countByBindingId(bindingId);

	    // Spring Data Pageable is page-index based — convert offset.
	    // Non-aligned offsets are uncommon for UI pagination; we still
	    // produce a correct slice by adjusting the page index, accepting
	    // that the returned slice is aligned to the next lower page.
	    Pageable pageable = PageRequest.of(effOffset / effLimit, effLimit);

	    List<ApBindingState> states = bindingStateRepository.findRevisionsByBindingId(bindingId, pageable);

	    Map<Integer, List<ApBindingParticipant>> participantsByState;
	    if (states.isEmpty()) {
	        participantsByState = Map.of();
	    } else {
	        List<Integer> stateIds = states.stream()
	                .map(ApBindingState::getBindingStateId)
	                .toList();
	        participantsByState = bindingParticipantRepository
	                .findByBindingStateIdInOrderByLastChange(stateIds).stream()
	                .collect(Collectors.groupingBy(ApBindingParticipant::getBindingStateId));
	    }

	    boolean incomplete = isHistoryIncomplete(bindingId);

	    ExtHistory result = new ExtHistory();
	    result.setRevisions(states.stream()
	            .map(s -> toExtRevision(s,
	                    participantsByState.getOrDefault(s.getBindingStateId(), List.of())))
	            .toList());
	    result.setTotalCount(Math.toIntExact(totalCount));
	    result.setIncomplete(incomplete);
	    return result;
	}

	private boolean isHistoryIncomplete(final Integer bindingId) {
	    List<Object[]> links = bindingStateRepository.findRevisionLinksByBindingId(bindingId);
	    Set<String> knownExtRevisions = new HashSet<>();
	    List<String> prevRefs = new ArrayList<>();
	    for (Object[] row : links) {
	        String extRev = (String) row[0];
	        String extPrev = (String) row[1];
	        if (extRev != null) {
	            knownExtRevisions.add(extRev);
	        }
	        if (extPrev != null) {
	            prevRefs.add(extPrev);
	        }
	    }
	    for (String prev : prevRefs) {
	        if (!knownExtRevisions.contains(prev)) {
	            return true;
	        }
	    }
	    return false;
	}

	private static ExtRevision toExtRevision(final ApBindingState bs, final List<ApBindingParticipant> participants) {
	    ExtRevision er = new ExtRevision();
	    er.setBindingStateId(bs.getBindingStateId());
	    er.setExtRevision(bs.getExtRevision());
	    er.setExtPrevRevision(bs.getExtPrevRevision());
	    er.setExtMetadataRevision(bs.getExtMetadataRevision());
	    er.setExtCreatedAt(bs.getExtCreatedAt());
	    if (bs.getExtCreatedAt() == null && bs.getCreateChange() != null) {
	        er.setCreateChangeAt(bs.getCreateChange().getChangeDate());
	    }
	    er.setSender(bs.getExtUser());
	    er.setParticipants(participants.stream()
	            .sorted(Comparator.comparing(ApBindingParticipant::getLastChange))
	            .map(ExternalSystemService::toExtParticipant)
	            .toList());
	    return er;
	}

	private static ExtParticipant toExtParticipant(final ApBindingParticipant bp) {
	    ExtParticipant ep = new ExtParticipant();
	    ep.setId(bp.getBindingParticipantId());
	    ep.setRole(ExtParticipantRole.valueOf(bp.getRole().name()));
	    ep.setName(bp.getName());
	    ep.setInstitutionCode(bp.getInstitutionCode());
	    ep.setLastChange(bp.getLastChange());
	    return ep;
	}

    /**
     * Vytvoření záznamu ve frontě zpracování
     *
     * @param accessPoint
     * @param apExternalSystem
     * @param stateMessage
     * @param state
     * @param date
     * @param user
     * @return
     */
    public ExtSyncsQueueItem createExtSyncsQueueItem(final ApAccessPoint accessPoint,
                                                     final ApExternalSystem apExternalSystem,
                                                     final ApBinding binding,
                                                     final String stateMessage,
                                                     final ExtSyncsQueueItem.ExtAsyncQueueState state,
                                                     final OffsetDateTime date,
                                                     final UsrUser user) {
         ExtSyncsQueueItem extSyncsQueueItem = new ExtSyncsQueueItem();
         extSyncsQueueItem.setAccessPoint(accessPoint);
         extSyncsQueueItem.setExternalSystem(apExternalSystem);
         extSyncsQueueItem.setBinding(binding);
         extSyncsQueueItem.setStateMessage(stateMessage);
         extSyncsQueueItem.setState(state);
         extSyncsQueueItem.setDate(date);
         extSyncsQueueItem.setUser(user);

         return extSyncsQueueItemRepository.save(extSyncsQueueItem);
     }

     /**
      * Return list of first items to process in given states
      *
      * @param pageSize
      * @param states
      * @return
      */
     @Transactional(value = TxType.MANDATORY)
     public Iterable<ExtSyncsQueueItem> getNextItems(int pageSize, ExtAsyncQueueState... states) {
         Pageable pageable = PageRequest.of(0, pageSize);

         return extSyncsQueueItemRepository.findByStates(Arrays.asList(states), pageable);
     }

     public List<ExtSyncsQueueItem> getQueueItems(Integer accessPointId, Integer externalSystemId,
                                                  ExtAsyncQueueState... states) {
         return extSyncsQueueItemRepository.findByApExtSystAndState(accessPointId, externalSystemId, Arrays.asList(
                                                                                                                   states));
     }

     public ArrDigitalRepository getDigitalRepository(Integer digiRepId) {
         return digitalRepositoryRepository.getOneCheckExist(digiRepId);
     }

     public List<ExtSystemProperty> findUserProperties(Integer extSystemId, Integer userId) {
         // pokud userId == null, získáme hodnoty pro všechny uživatele
         List<SysExternalSystemProperty> properties;
         if (extSystemId == null) {
             properties = sysExtSysPropertyRepository.findByUserId(userId);
         } else {
             properties = sysExtSysPropertyRepository.findByExternalSystemIdAndUserId(extSystemId, userId);
         }
         List<ExtSystemProperty> result = new ArrayList<>(properties.size());
         properties.forEach(i -> {
             ExtSystemProperty p = new ExtSystemProperty();
             p.setId(i.getExternalSystemPropertyId());
             p.setUserId(i.getUserId());
             p.setExtSystemId(i.getExternalSystemId());
             p.setName(i.getName());
             p.setValue(i.getValue());
             result.add(p);
         });
         return result;
     }

     public List<ExtSystemProperty> findAllProperties(Integer extSystemId) {
		// pokud userId == null, získáme hodnoty pro všechny uživatele
        List<SysExternalSystemProperty> properties;
        if (extSystemId == null) {
            properties = sysExtSysPropertyRepository.findAll();
        } else {
            properties = sysExtSysPropertyRepository.findByExternalSystemId(extSystemId);
        }
		List<ExtSystemProperty> result = new ArrayList<>(properties.size());
		properties.forEach(i -> {
			ExtSystemProperty p = new ExtSystemProperty();
            p.setId(i.getExternalSystemPropertyId());
			p.setUserId(i.getUserId());
            p.setExtSystemId(i.getExternalSystemId());
			p.setName(i.getName());
			p.setValue(i.getValue());
			result.add(p);
		});
		return result;
	}

    /**
     * Add or update property
     *
     * @param extSystem
     * @param user
     * @param extSystemProperty
     * @return
     */
    public SysExternalSystemProperty storeProperty(ApExternalSystem extSystem, UsrUser user,
                                                 ExtSystemProperty extSystemProperty) {
		List<SysExternalSystemProperty> properties = sysExtSysPropertyRepository.findByExternalSystemAndUser(extSystem, user);
		SysExternalSystemProperty property = null;
		for (SysExternalSystemProperty p : properties) {
			if (p.getName().equals(extSystemProperty.getName())) {
				property = p;
				break;
			}
		}
		if (property == null) {
			property = new SysExternalSystemProperty();
			property.setExternalSystem(extSystem);
			property.setUser(user);
			property.setName(extSystemProperty.getName());
		}
		property.setValue(extSystemProperty.getValue());
        return sysExtSysPropertyRepository.save(property);
	}

	public void deleteProperty(Integer extSysPropertyId) {
		sysExtSysPropertyRepository.deleteById(extSysPropertyId);
	}

    public SysExternalSystemProperty getProperty(Integer extSysPropertyId) {
        return sysExtSysPropertyRepository.findById(extSysPropertyId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "SysExternalSystemProperty not found, id: " + extSysPropertyId));
    }

    public ArrDigitalRepository findDigitalRepositoryByCode(String code) {
        ArrDigitalRepository digitalRepository = digitalRepositoryRepository.findOneByCode(code);
        if (digitalRepository == null) {
            throw new EntityNotFoundException("ArrDigitalRepository not found, code: " + code);
        } else {
            return digitalRepository;
        }
    }

    @Transactional
    public void replaceBindingIssues(ApBinding binding, List<ApBindingIssue> newIssues) {
        bindingIssueRepository.deleteByBindingId(binding.getBindingId());
        if (!newIssues.isEmpty()) {
            bindingIssueRepository.saveAll(newIssues);
        }
    }

    @Transactional
    public void saveBindingStateParticipants(List<ApBindingParticipant> participants) {
        if (!participants.isEmpty()) {
            bindingParticipantRepository.saveAll(participants);
        }
    }
}
