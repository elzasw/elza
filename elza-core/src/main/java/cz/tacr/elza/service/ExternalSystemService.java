package cz.tacr.elza.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Servisní třída pro práci s externími systémy.
 *
 * @author Martin Šlapa
 * @since 05.12.2016
 */
@Service
public class ExternalSystemService {

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
    public List<ApExternalSystem> findAllApSystem() {
        return apExternalSystemRepository.findAll();
    }

    /**
     * Vyhledání externího systému podle kódu.
     *
     * @param code kód externího systému, který hledáme
     * @return nalezený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem findByCode(final String code) {
        return externalSystemRepository.findByCode(code);
    }

    /**
     * Vyhledání externího systému podle kódu.
     *
     * @param code kód externího systému, který hledáme
     * @return nalezený externí systém
     */
    public ApExternalSystem findApExternalSystemByCode(final String code) {
        return apExternalSystemRepository.findByCode(code);
    }

    /**
     * Vyhledání externího systému podle identifikátoru.
     *
     * @param id identifikátor externího systému
     * @return nalezený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem findOne(final Integer id) {
        return externalSystemRepository.getOneCheckExist(id);
    }

    /**
     * Vytvoření externího systému.
     *
     * @param externalSystem vytvářený externí systém
     * @return vytvořený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem create(final SysExternalSystem externalSystem) {
        validateExternalSystem(externalSystem, true);
        externalSystemRepository.save(externalSystem);
        sendCreateExternalSystemNotification(externalSystem.getExternalSystemId());
        return externalSystem;
    }

    /**
     * Smazání exteního systému.
     *
     * @param id identifikátor mazaného externího systému
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public void delete(final Integer id) {
        sendDeleteExternalSystemNotification(id);
        externalSystemRepository.delete(id);
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
     * @param externalSystem upravovaný externí systém
     * @return upravený externí systém
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public SysExternalSystem update(final SysExternalSystem externalSystem) {
        validateExternalSystem(externalSystem, false);
        sendUpdateExternalSystemNotification(externalSystem.getExternalSystemId());
        return externalSystemRepository.save(externalSystem);
    }

    /**
     * Validace externího systému.
     *
     * @param externalSystem validovaný externí systém
     * @param create         příznak, zda-li se jedná o validaci při vytvářený externího systému
     */
    private void validateExternalSystem(final SysExternalSystem externalSystem, final boolean create) {
        if (create) {
            if (externalSystem.getExternalSystemId() != null) {
                throw new SystemException("Identifikátor externího systému musí být při vytváření prázdný", BaseCode.ID_EXIST).set("id", externalSystem.getExternalSystemId());
            }
        } else {
            if (externalSystem.getExternalSystemId() == null) {
                throw new SystemException("Identifikátor externího systému musí být při editaci vyplněň", BaseCode.ID_NOT_EXIST);
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
                throw new BusinessException("Nevyplněno pole: sendNotification", BaseCode.PROPERTY_NOT_EXIST).set("property", "sendNotification");
            }
        }
    }

    /**
     * Odešle notifikaci do klienta, že se změnil externí systém.
     * @param externalSystemId id ex. systému
     */
    private void sendUpdateExternalSystemNotification(final Integer externalSystemId) {
        eventNotificationService.publishEvent(new EventId(EventType.EXTERNAL_SYSTEM_UPDATE, externalSystemId));
    }

    /**
     * Odešle notifikaci do klienta, že se vytvořil externí systém.
     * @param externalSystemId id ex. systému
     */
    private void sendCreateExternalSystemNotification(final Integer externalSystemId) {
        eventNotificationService.publishEvent(new EventId(EventType.EXTERNAL_SYSTEM_CREATE, externalSystemId));
    }

    /**
     * Odešle notifikaci do klienta, že se smazal externí systém.
     * @param externalSystemId id ex. systému
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
        return digitalRepositoryRepository.findAll(ids);
    }

    public ApBinding createApBinding(final ApScope scope,
                                     final Long eid,
                                     final ApExternalSystem apExternalSystem) {
        ApBinding apBinding = new ApBinding();
        apBinding.setScope(scope);
        apBinding.setValue(eid == null ? null : String.valueOf(eid));
        apBinding.setApExternalSystem(apExternalSystem);
        return bindingRepository.save(apBinding);
    }

    public ApBindingState createApBindingState(final ApBinding binding,
                                               final ApAccessPoint accessPoint,
                                               final ApChange apChange,
                                               final String state,
                                               final String revisionUuid,
                                               final String user,
                                               final Long replacedById) {
        ApBindingState apBindingState = new ApBindingState();
        apBindingState.setBinding(binding);
        apBindingState.setAccessPoint(accessPoint);
        apBindingState.setExtState(state);
        apBindingState.setExtRevision(revisionUuid);
        apBindingState.setExtUser(user);
        apBindingState.setExtReplacedBy(replacedById == null ? null : String.valueOf(replacedById));
        apBindingState.setSyncChange(apChange);
        apBindingState.setCreateChange(apChange);
        apBindingState.setSyncOk(SyncState.SYNC_OK);
        return bindingStateRepository.save(apBindingState);
    }

    public ApBindingState createNewApBindingState(ApBindingState oldbindingState,
                                                  ApChange apChange,
                                                  String state,
                                                  String revisionUuid,
                                                  String user,
                                                  Long replacedById) {
        ApBindingState apBindingState = new ApBindingState();
        apBindingState.setBinding(oldbindingState.getBinding());
        apBindingState.setAccessPoint(oldbindingState.getAccessPoint());
        apBindingState.setExtState(state);
        apBindingState.setExtRevision(revisionUuid);
        apBindingState.setExtUser(user);
        apBindingState.setExtReplacedBy(replacedById == null ? null : String.valueOf(replacedById));
        apBindingState.setSyncChange(apChange);
        apBindingState.setCreateChange(apChange);
        apBindingState.setSyncOk(SyncState.SYNC_OK);

        oldbindingState.setDeleteChange(apChange);
        bindingStateRepository.save(oldbindingState);

        return bindingStateRepository.save(apBindingState);
    }

    public ApBindingItem createApBindingItem(final ApBinding binding,
                                             final String uuid,
                                             final ApPart part,
                                             final ApItem item) {
        ApBindingItem apBindingItem = new ApBindingItem();
        apBindingItem.setBinding(binding);
        apBindingItem.setValue(uuid);
        apBindingItem.setPart(part);
        apBindingItem.setItem(item);
        apBindingItem.setCamIdentifier(true);
        return bindingItemRepository.save(apBindingItem);
    }

    public ApBinding findByScopeAndValueAndApExternalSystem(final ApScope scope, final Integer archiveEntityId, final String externalSystemCode) {
        return bindingRepository.findByScopeAndValueAndApExternalSystem(scope, String.valueOf(archiveEntityId), externalSystemCode);
    }

    public ApBindingState findByBinding(final ApBinding binding) {
        List<ApBindingState> bindingList = bindingStateRepository.findByBinding(binding);
        if (CollectionUtils.isNotEmpty(bindingList)) {
            return bindingList.get(0);
        }
        return null;
    }

    public ApBindingItem findByBindingAndUuid(ApBinding binding, String uuid) {
        return bindingItemRepository.findByBindingAndUuid(binding, uuid);
    }

    public ApBindingState findByAccessPointAndExternalSystem(final ApAccessPoint accessPoint, final ApExternalSystem externalSystem) {
        return bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, externalSystem);
    }
}
