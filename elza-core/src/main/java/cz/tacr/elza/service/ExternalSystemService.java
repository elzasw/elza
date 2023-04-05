package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import cz.tacr.cam.schema.cam.EntityRecordRevInfoXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApBindingSync;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApBindingSyncRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.DigitizationFrontdeskRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
import cz.tacr.elza.service.cam.BindingSyncInfo;
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
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private ExternalSystemRepository externalSystemRepository;

    @Autowired
    private ApExternalSystemRepository apExternalSystemRepository;

    @Autowired
    private DigitizationFrontdeskRepository digitizationFrontdeskRepository;

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ApBindingRepository bindingRepository;
    
    @Autowired
    private ApBindingSyncRepository bindingSyncRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private UserService userService;

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public List<SysExternalSystem> findAll() {
        return externalSystemRepository.findAll();
    }

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @Transactional
    public List<ApExternalSystem> findAllApSystem() {
        return apExternalSystemRepository.findAll();
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
    public void delete(final Integer id) {
        sendDeleteExternalSystemNotification(id);
        externalSystemRepository.deleteById(id);

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

            if (queueItem.get().getState() != ExtAsyncQueueState.EXPORT_START) {
                extSyncsQueueItemRepository.deleteById(extSyncItemId);
            }
        }
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

        if (StringUtils.isEmpty(externalSystem.getCode())) {
            throw new BusinessException("Nevyplněno pole: code", BaseCode.PROPERTY_NOT_EXIST).set("property", "code");
        }

        if (StringUtils.isEmpty(externalSystem.getName())) {
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
        Validate.notNull(value);
        Validate.notNull(apExternalSystem);

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
        apBindingState.setSyncChange(apChange);
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
                                             ApChange apChange, final String value,
                                             final ApPart part,
                                             final ApItem item) {
        Validate.notNull(binding);
        Validate.notNull(apChange);
        Validate.notNull(value);
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

    /**
     * Prepare entities for synchronization
     * 
     * @param bindingSyncId
     * @param entityRecordRevInfoXmls entity info list
     * @param lastTransaction
     * @param toTransaction
     * @param page
     * @param count
     */
    @Transactional
    public void prepareApsForSync(Integer bindingSyncId, List<EntityRecordRevInfoXml> entityRecordRevInfoXmls, 
                                  String lastTransaction, String toTransaction, 
                                  Integer page, Integer count) {
        log.debug("Preparing APs for synchronization from external system, count: {}", entityRecordRevInfoXmls.size());

        // Prepare keys
        ApBindingSync bindingSync = bindingSyncRepository.getOneCheckExist(bindingSyncId);
        ApExternalSystem externalSystem = bindingSync.getApExternalSystem();
        List<String> keyList = new ArrayList<>(entityRecordRevInfoXmls.size());
        Map<String, EntityRecordRevInfoXml> recordCodesMap = new HashMap<>();
        Function<EntityRecordRevInfoXml, String> idGetter;
        if (externalSystem.getType().equals(ApExternalSystemType.CAM_UUID)) {
            idGetter = (x) -> x.getEuid().getValue();
        } else {
            idGetter = (x) -> Long.toString(x.getEid().getValue());
        }
        for (EntityRecordRevInfoXml entityRecordRevInfoXml : entityRecordRevInfoXmls) {
            String id = idGetter.apply(entityRecordRevInfoXml);
            keyList.add(id);
            EntityRecordRevInfoXml prevInfo = recordCodesMap.put(id, entityRecordRevInfoXml);
            Validate.isTrue(prevInfo == null, "Record with same key already process, %s", id);
        }
        
        List<ApBinding> bindings = findBindings(keyList, externalSystem);
        final Map<String, ApBinding> bindingMap = bindings.stream().collect(Collectors.toMap(p -> p.getValue(), p -> p));

        Map<Integer, ApBindingState> bindingStateMap;
        if (bindings.size() > 0) {
            List<ApBindingState> bindingStateList = findBindingStates(bindings);
            bindingStateMap = bindingStateList.stream().collect(Collectors.toMap(p -> p.getBindingId(), p -> p));
        } else {
            bindingStateMap = Collections.emptyMap();
        }

        int recNo = 0;

        UsrUser user = userService.getLoggedUser();
        for (String recordCode : keyList) {
            recNo++;
            if (log.isDebugEnabled()) {
                if (recNo%100 == 0) {
                    log.debug("Prepared records for sync: [{}-{}]", ((recNo+99)/100-1)*100+1, recNo);
                }
            }

            ApBinding binding = bindingMap.get(recordCode);
            ApAccessPoint ap = null;
            if (binding == null) {
                // prepare binding for CAM Complete
                if (externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
                    // we are creating all bindings at once 
                    // - will be flush to the DB at the end of this method
                    binding = createApBinding(recordCode, externalSystem, false);
                }
            } else {
                ApBindingState bindingState = bindingStateMap.get(binding.getBindingId());
                EntityRecordRevInfoXml xmlRecordInfo = recordCodesMap.get(recordCode);
                if (bindingState != null) {
                    ap = bindingState.getAccessPoint();
                    // kontrola uuid revizi, pokud se rovná extRevizion(), pak aktualizace není potřeba
                    String uuidRev = xmlRecordInfo.getRev().getValue();
                    if (bindingState.getExtRevision().equals(uuidRev)) {
                        continue;
                    }
                }
                // entita mohla být smazána, hledáme ji jinak
                if (ap == null) {
                    String uuid = xmlRecordInfo.getEuid().getValue();
                    ap = apAccessPointRepository.findAccessPointByUuid(uuid);
                }
            }
            // update or add new items from CAM_COMPLETE
            if (ap != null || externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
                createExtSyncsQueueItem(ap, externalSystem, binding, null,
                                        ap != null? ExtAsyncQueueState.UPDATE : ExtAsyncQueueState.IMPORT_NEW,
                                        OffsetDateTime.now(),
                                        user);
            }
        }
        if (log.isDebugEnabled()) {
            if (recNo%100 != 0) {
                log.debug("Prepared records for sync: [{}-{}]", ((recNo+99)/100-1)*100+1, recNo);
            }
        }
        log.debug("APs prepared for synchronization from external system");
        log.info("To queue ext_syncs_queue_item added {} records for sync.", recNo);

        // aktualizace dat
        bindingSync.setLastTransaction(lastTransaction);
        bindingSync.setToTransaction(toTransaction);
        bindingSync.setPage(page);
        bindingSync.setCount(count);
        bindingSyncRepository.saveAndFlush(bindingSync);
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
 }
