package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import cz.tacr.elza.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.security.UserDetail;


/**
 * Datová třída pro přístupové body.
 *
 * @since 24.07.2018
 */
@Service
public class AccessPointDataService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointDataService.class);

    private final EntityManager em;
    private final UserService userService;
    private final ApChangeRepository apChangeRepository;
    private final PartyService partyService;

    @Autowired
    public AccessPointDataService(final EntityManager em,
                                  final UserService userService,
                                  final ApChangeRepository apChangeRepository,
                                  final PartyService partyService) {
        this.em = em;
        this.userService = userService;
        this.apChangeRepository = apChangeRepository;
        this.partyService = partyService;
    }

    /**
     * Sestavení celého jména z jména a doplňku.
     *
     * @param name       jméno
     * @param complement doplněk
     * @return celé jméno
     */
    @Nullable
    public static String generateFullName(@Nullable final String name, @Nullable final String complement) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(name.trim());
        if (StringUtils.isNotEmpty(complement)) {
            sb.append(" (").append(complement.trim()).append(')');
        }
        return sb.toString();
    }

    /**
     * Vytvoření změny daného typu.
     *
     * @param type typ změny
     * @return změna
     */
    public ApChange createChange(@Nullable final ApChange.Type type) {
        return createChange(type, null);
    }

    /**
     * Vytvoření změny s externím systémem.
     *
     * @param type           typ změny
     * @param externalSystem externí systém
     * @return změna
     */
    public ApChange createChange(@Nullable final ApChange.Type type, @Nullable ApExternalSystem externalSystem) {
        ApChange change = new ApChange();
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(OffsetDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
            change.setUser(user);
        }

        change.setType(type);
        change.setExternalSystem(externalSystem);

        return apChangeRepository.save(change);
    }

    /**
     * Změna popisu přístupového bodu.
     * Podle vstupních a aktuálních dat se rozhodne, zda-li se bude popis mazat, vytvářet nebo jen upravovat - verzovaně.
     *
     * @param apState     přístupový bod
     * @param description popis přístupového bodu
     * @param change      změna pod kterou se provádí změna (pokud null, volí se individuelně)
     * @return přístupový bod
     */
    public ApAccessPoint changeDescription(final ApState apState,
                                           @Nullable final String description,
                                           @Nullable final ApChange change) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        validationNotDeleted(apState);

        ApAccessPoint accessPoint = apState.getAccessPoint();

        //TODO : smazáno - změna popisu

        return accessPoint;
    }

    /**
     * Validace přístupového bodu, že není smazaný.
     *
     * @param state stav přístupového bodu
     */
    public void validationNotDeleted(final ApState state) {
        if (state.getDeleteChange() != null) {
            throw new BusinessException("Nelze upravit přístupový bod", RegistryCode.CANT_CHANGE_DELETED_AP)
                    .set("accessPointId", state.getAccessPointId())
                    .set("uuid", state.getAccessPoint().getUuid());
        }
    }

    /**
     * Validace typu, že se nejedná o struturovaně popisovaný.
     *
     * @param type typ
     */
    public void validateNotStructureType(final ApType type) {
        if (type.getRuleSystem() != null) {
            throw new SystemException("Neplatná operace pro založení strukturovaného popisu přístupového bodu", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Validace typu, že se jedná o struturovaně popisovaný.
     *
     * @param type typ
     */
    public void validateStructureType(final ApType type) {
        if (type.getRuleSystem() == null) {
            throw new SystemException("Neplatná operace pro založení nestrukturovaného popisu přístupového bodu", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Validace ap, že se musí strukturovaně popisovat.
     *
     * @param accessPoint přístupový bod
     */
    private void validateAccessPointStructure(final ApAccessPoint accessPoint) {
        if (!isDescribeStructure(accessPoint)) {
            throw new SystemException("Přístupový bod není vázán na pravidla pro strukturovaný popis", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Je přístupový bod popisován strukturovaně?
     *
     * @param accessPoint přístupový bod
     * @return true pokud je
     */
    private boolean isDescribeStructure(final ApAccessPoint accessPoint) {
        return accessPoint.getRuleSystem() != null;
    }

    /**
     * Validace přístupového bodu pro migraci.
     *
     * @param apState přístupový bod
     */
    public void validationMigrateAp(final ApState apState) {
        validationNotDeleted(apState);
        validateStructureType(apState.getApType());
        ApAccessPoint accessPoint = apState.getAccessPoint();
        if (accessPoint.getRuleSystem() != null) {
            throw new BusinessException("Nelze migrovat přístupový bod", RegistryCode.CANT_MIGRATE_AP)
                    .set("accessPointId", accessPoint.getAccessPointId())
                    .set("uuid", accessPoint.getUuid());
        }
    }
}
