package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApNameRepository;
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
    private final ApNameRepository apNameRepository;
    private final ApDescriptionRepository descriptionRepository;
    private final PartyService partyService;

    @Autowired
    public AccessPointDataService(final EntityManager em,
                                  final UserService userService,
                                  final ApChangeRepository apChangeRepository,
                                  final ApNameRepository apNameRepository,
                                  final ApDescriptionRepository descriptionRepository,
                                  final PartyService partyService) {
        this.em = em;
        this.userService = userService;
        this.apChangeRepository = apChangeRepository;
        this.apNameRepository = apNameRepository;
        this.descriptionRepository = descriptionRepository;
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

        // aktuálně platný popis přístupového bodu
        ApDescription apDescription = descriptionRepository.findByAccessPoint(accessPoint);

        if (StringUtils.isBlank(description)) {
            if (apDescription != null) {
                apDescription.setDeleteChange(change == null ? createChange(ApChange.Type.DESC_DELETE) : change);
                descriptionRepository.save(apDescription);
            } else {
                // nic se nezakládá ani nemaže
                logger.debug("Charakteristika přístupového bodu neexistuje a nebude ani zakládána. (accessPointId: {})", accessPoint.getAccessPointId());
            }
        } else {
            if (apDescription != null) {
                ApDescription apDescriptionNew = new ApDescription(apDescription);
                ApChange updateChange = change == null ? createChange(ApChange.Type.DESC_UPDATE) : change;
                apDescription.setDeleteChange(updateChange);
                descriptionRepository.save(apDescription);

                apDescriptionNew.setCreateChange(updateChange);
                apDescriptionNew.setDescription(description);
                descriptionRepository.save(apDescriptionNew);
            } else {
                createDescription(accessPoint, description, change == null ? createChange(ApChange.Type.DESC_CREATE) : change);
            }
        }

        return accessPoint;
    }

    /**
     * Aktualizace jména přístupového bodu - verzovaně.
     *
     * @param apState    přístupový bod
     * @param apName     upravované jméno přístupového bodu
     * @param name       jméno přístupového bodu
     * @param complement doplněk přístupového bodu
     * @param language   jazyk jména
     * @param change     změna
     * @return upravený jméno
     */
    public ApName updateAccessPointName(final ApState apState,
                                        final ApName apName,
                                        final @Nullable String name,
                                        final @Nullable String complement,
                                        final @Nullable String fullName,
                                        final @Nullable SysLanguage language,
                                        final ApChange change,
                                        final boolean validateUnique) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");
        Validate.notNull(apName, "Upravované jméno musí být vyplněno");
        Validate.notNull(change, "Změna musí být vyplněna");
        validationNotDeleted(apState);
        validationNotDeleted(apName);

        if (apName.getCreateChangeId().equals(change.getChangeId())) {
            apName.setName(name);
            apName.setComplement(complement);
            apName.setFullName(fullName);
            apName.setLanguage(language);
            apNameRepository.save(apName);
            if (validateUnique) {
                validationNameUnique(apState.getScope(), apName.getFullName());
            }
            return apName;
        } else {
            ApName apNameNew = new ApName(apName);

            // zneplatnění původní verze jména
            apName.setDeleteChange(change);
            apNameRepository.save(apName);

            // založení nové verze jména
            apNameNew.setCreateChange(change);
            apNameNew.setName(name);
            apNameNew.setComplement(complement);
            apNameNew.setFullName(fullName);
            apNameNew.setLanguage(language);

            apNameRepository.save(apNameNew);
            if (validateUnique) {
                validationNameUnique(apState.getScope(), apNameNew.getFullName());
            }
            return apNameNew;
        }
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
     * Validace jména, že není smazaný.
     *
     * @param name jméno
     */
    public void validationNotDeleted(final ApName name) {
        if (name.getDeleteChange() != null) {
            throw new BusinessException("Nelze upravit jméno přístupového bodu", RegistryCode.CANT_CHANGE_DELETED_NAME)
                    .set("nameId", name.getNameId());
        }
    }

    /**
     * Validace přístupového bodu, že nemá vazbu na osobu.
     *
     * @param accessPoint přístupový bod
     */
    public void validationNotParty(final ApAccessPoint accessPoint) {
        ParParty parParty = partyService.findParPartyByAccessPoint(accessPoint);
        if (parParty != null) {
            throw new BusinessException("Existuje vazba z osoby, nelze měnit přístupový bod", RegistryCode.EXIST_FOREIGN_PARTY);
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
     * Validace unikátnosti jména v daném scope.
     *
     * @param scope    třída
     * @param fullName validované jméno
     */
    public void validationNameUnique(final ApScope scope, final String fullName) {
        if (!isNameUnique(scope, fullName)) {
            throw new BusinessException("Celé jméno není unikátní v rámci třídy", RegistryCode.NOT_UNIQUE_FULL_NAME)
                    .set("fullName", fullName)
                    .set("scopeId", scope.getScopeId());
        }
    }

    /**
     * Kontrola, zdali je jméno unikátní v daném scope.
     *
     * @param scope    třída
     * @param fullName validované jméno
     * @return true pokud je
     */
    public boolean isNameUnique(final ApScope scope, final String fullName) {
        Validate.notNull(scope, "Přístupový bod musí být vyplněn");
        Validate.notNull(fullName, "Plné jméno musí být vyplněno");
        int count = apNameRepository.countUniqueName(fullName, scope);
        return count <= 1;
    }

    /**
     * Založení popisu.
     *
     * @param accessPoint přístupový bod
     * @param description popis přístupového bodu
     * @param change      změna
     */
    public void createDescription(final ApAccessPoint accessPoint,
                                  final String description,
                                  @Nullable final ApChange change) {
        Validate.notNull(accessPoint, "Přístupový bod musí být vyplněn");
        Validate.notNull(description, "Popis musí být vyplněn");

        ApChange createChange = change == null ? createChange(ApChange.Type.DESC_CREATE) : change;
        ApDescription apDescription = new ApDescription();
        apDescription.setDescription(description);
        apDescription.setCreateChange(createChange);
        apDescription.setAccessPoint(accessPoint);

        descriptionRepository.save(apDescription);
    }

    /**
     * Porovnání obsahů jmen.
     *
     * @param name1 první jméno
     * @param name2 druhé jméno
     * @return true pokud se shodují
     */
    public boolean equalsNames(final ApName name1, final ApName name2) {
        return equalsNames(name1, name2.getName(), name2.getComplement(), name2.getFullName(), name2.getLanguageId());
    }

    /**
     * Porovnání obsahů jmen.
     *
     * @param apName jméno
     * @return true pokud se shodují
     */
    public boolean equalsNames(final ApName apName, final String name, final String complement, final String fullName, final Integer languageId) {
        return Objects.equals(apName.getComplement(), complement)
                && Objects.equals(apName.getName(), name)
                && Objects.equals(apName.getFullName(), fullName)
                && Objects.equals(apName.getLanguageId(), languageId);
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
