package cz.tacr.elza.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.DigitizationFrontdeskRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Servisní třída pro práci s externími systémy.
 *
 * @since 05.12.2016
 */
@Service
public class ExternalSystemService {

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
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private StaticDataService staticDataService;

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
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public void deleteQueueItem(final Integer extSyncItemId) {
        extSyncsQueueItemRepository.deleteById(extSyncItemId);
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
        apBindingState.setExtUser(userName);
        apBindingState.setExtReplacedBy(replacedById == null ? null : String.valueOf(replacedById));
        apBindingState.setSyncChange(apChange);
        apBindingState.setCreateChange(apChange);
        apBindingState.setSyncOk(syncState);
        apBindingState.setPreferredPart(preferredPart);
        apBindingState.setApType(apType);
        return bindingStateRepository.save(apBindingState);
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

        ApBindingState apBindingState = new ApBindingState();
        apBindingState.setBinding(oldbindingState.getBinding());
        apBindingState.setAccessPoint(oldbindingState.getAccessPoint());
        apBindingState.setApExternalSystem(oldbindingState.getApExternalSystem());
        apBindingState.setExtState(state);
        apBindingState.setExtRevision(revisionUuid);
        apBindingState.setExtUser(user);
        apBindingState.setExtReplacedBy(extReplacedBy);
        apBindingState.setSyncChange(apChange);
        apBindingState.setCreateChange(apChange);
        apBindingState.setSyncOk(syncState);
        apBindingState.setPreferredPart(preferredPart);
        apBindingState.setApType(apType);

        return bindingStateRepository.saveAndFlush(apBindingState);
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
}
