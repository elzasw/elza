package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DigitizationFrontdeskRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

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
    private RegExternalSystemRepository regExternalSystemRepository;

    @Autowired
    private DigitizationFrontdeskRepository digitizationFrontdeskRepository;

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
    @AuthMethod(permission = UsrPermission.Permission.REG_SCOPE_WR_ALL)
    public List<RegExternalSystem> findAllRegSystem() {
        return regExternalSystemRepository.findAll();
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
        sendChangeExternalSystemNotification();
        return externalSystemRepository.save(externalSystem);
    }

    /**
     * Smazání exteního systému.
     *
     * @param id identifikátor mazaného externího systému
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    public void delete(final Integer id) {
        sendChangeExternalSystemNotification();
        externalSystemRepository.delete(id);
    }

    /**
     * Vyhledá všechny externí systémy typu - Digitalizační linka.
     *
     * @return seznam digitalizačních linek
     */
    public List<ArrDigitizationFrontdesk> findDigitizationFrontdesk() {
        return digitizationFrontdeskRepository.findAll();
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
        sendChangeExternalSystemNotification();
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
                throw new SystemException(BaseCode.ID_EXIST).set("id", externalSystem.getExternalSystemId());
            }
        } else {
            if (externalSystem.getExternalSystemId() == null) {
                throw new SystemException(BaseCode.ID_NOT_EXIST);
            }
        }

        if (StringUtils.isEmpty(externalSystem.getCode())) {
            throw new BusinessException(BaseCode.PROPERTY_NOT_EXIST).set("property", "code");
        }

        if (StringUtils.isEmpty(externalSystem.getName())) {
            throw new BusinessException(BaseCode.PROPERTY_NOT_EXIST).set("property", "name");
        }

        // extra validace pro ArrDigitalRepository
        if (externalSystem instanceof ArrDigitalRepository) {
            if (((ArrDigitalRepository) externalSystem).getSendNotification() == null) {
                throw new BusinessException(BaseCode.PROPERTY_NOT_EXIST).set("property", "sendNotification");
            }
        }
    }

    /**
     * Odešle notifikaci do klienta, že se změnily externí systémy.
     */
    private void sendChangeExternalSystemNotification() {
        eventNotificationService.publishEvent(new ActionEvent(EventType.EXTERNAL_SYSTEM_CHANGE));
    }

}
