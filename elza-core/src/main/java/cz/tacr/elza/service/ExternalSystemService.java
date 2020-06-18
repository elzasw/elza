package cz.tacr.elza.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.DigitizationFrontdeskRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
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
}
