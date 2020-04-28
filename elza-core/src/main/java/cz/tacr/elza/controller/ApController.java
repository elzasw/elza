package cz.tacr.elza.controller;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApAccessPointDescriptionVO;
import cz.tacr.elza.controller.vo.ApAccessPointEditVO;
import cz.tacr.elza.controller.vo.ApAccessPointNameVO;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApAttributesInfoVO;
import cz.tacr.elza.controller.vo.ApEidTypeVO;
import cz.tacr.elza.controller.vo.ApExternalSystemSimpleVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApRecordSimple;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ApScopeWithConnectedVO;
import cz.tacr.elza.controller.vo.ApStateChangeVO;
import cz.tacr.elza.controller.vo.ApStateHistoryVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.LanguageVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.AccessPointMigrationService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.FragmentService;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.service.UserService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * REST kontroler pro registry.
 */
@RestController
@RequestMapping(value = "/api/registry", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ApController {

    private static final Logger logger = LoggerFactory.getLogger(ApController.class);

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private AccessPointMigrationService apMigrationService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private ApFactory apFactory;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private FragmentService fragmentService;

    @Autowired
    private StructObjService structObjService;

    @Autowired
    private UserService userService;

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (heslo, popis, poznámka),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null či prázdný (pak vrací vše)
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @param apTypeId   IDčka typu záznamu, může být null
     * @param versionId   id verze, podle které se budou filtrovat třídy rejstříků, null - výchozí třídy
     * @param itemSpecId   id specifikace
     * @param state             stav schválení přístupového bodu
     * @param scopeId           id scope, pokud je vyplněn vrací se jen rejstříky s tímto scope
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
	@Transactional
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public FilteredResultVO<ApAccessPointVO> findAccessPoint(@RequestParam(required = false) @Nullable final String search,
                                                             @RequestParam final Integer from,
                                                             @RequestParam final Integer count,
                                                             @RequestParam(required = false) @Nullable final Integer apTypeId,
                                                             @RequestParam(required = false) @Nullable final Integer versionId,
                                                             @RequestParam(required = false) @Nullable final Integer itemSpecId,
                                                             @RequestParam(required = false) @Nullable final Integer itemTypeId,
                                                             @RequestParam(required = false) @Nullable final ApState.StateApproval state,
                                                             @RequestParam(required = false) @Nullable final Integer scopeId,
                                                             @RequestParam(required = false) @Nullable final Integer lastRecordNr) {

        if (apTypeId != null && (itemSpecId != null || itemTypeId != null)) {
            throw new SystemException("Nelze použít více kritérií zároveň (specifikace/typ a typ rejstříku).", BaseCode.SYSTEM_ERROR);
        }

        StaticDataProvider sdp = staticDataService.getData();

        Set<Integer> apTypeIds = new HashSet<>();
        if (apTypeId != null) {
            apTypeIds.add(apTypeId);
        } else if (itemSpecId != null) {
            RulItemSpec spec = sdp.getItemSpecById(itemSpecId);
            if (spec == null) {
                throw new ObjectNotFoundException("Specification not found", ArrangementCode.ITEM_SPEC_NOT_FOUND)
                        .setId(itemSpecId);
            }
            apTypeIds.addAll(itemAptypeRepository.findApTypeIdsByItemSpec(spec));
            if (apTypeIds.size() == 0) {
                logger.error("Specification has no associated classes, itemSpecId={}", itemSpecId);
                throw new SystemException("Configuration error, specification without associated classes",
                        BaseCode.SYSTEM_ERROR).set("itemSpecId", itemSpecId);
            }
        } else
        if (itemTypeId != null) {
            ItemType itemType = sdp.getItemTypeById(itemTypeId);
            if (itemType == null) {
                throw new ObjectNotFoundException("Item type not found", ArrangementCode.ITEM_TYPE_NOT_FOUND)
                        .setId(itemTypeId);
            }
            apTypeIds.addAll(itemAptypeRepository.findApTypeIdsByItemType(itemType.getEntity()));
            if (apTypeIds.size() == 0) {
                logger.error("Item type has no associated classes, itemTypeId={}", itemTypeId);
                throw new SystemException("Configuration error, item type without associated classes",
                        BaseCode.SYSTEM_ERROR).set("itemTypeId", itemTypeId);
            }
        }

        Set<Integer> apTypeIdTree = apTypeRepository.findSubtreeIds(apTypeIds);

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
            fund = version.getFund();
        }

        Set<ApState.StateApproval> states = state != null ? EnumSet.of(state) : null;

        final long foundRecordsCount = accessPointService.findApAccessPointByTextAndTypeCount(search, apTypeIdTree, fund, scopeId, states);

        final List<ApState> foundRecords = accessPointService.findApAccessPointByTextAndType(search, apTypeIdTree, from, count, fund, scopeId, states);

        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(foundRecords.stream().map(apState -> apState.getAccessPoint()).collect(Collectors.toList()));

        return new FilteredResultVO<>(foundRecords, apState -> {
            ApAccessPointVO vo = apFactory.createVO(apState);
            vo.setPartyId(recordIdPartyIdMap.get(vo.getId()));
            return vo;
        }, foundRecordsCount);
    }


    /**
     * Najde seznam rejstříkových hesel, která jsou typu napojeného na dané relationRoleTypeId a mají třídu rejstříku
     * stejnou jako daná osoba, nebo je jejich třída navázaná na třídu dané osoby.
     *
     * @param search     hledaný řetězec
     * @param from       odkud se mají vracet výsledka
     * @param count      počet vracených výsledků
     * @param roleTypeId id typu vztahu
     * @param partyId    id osoby, ze které je načtena hledaná třída rejstříku a k ní navázané třídy
     * @return seznam rejstříkových hesel s počtem všech nalezených
     */
	@Transactional
    @RequestMapping(value = "/findRecordForRelation", method = RequestMethod.GET)
    public FilteredResultVO<ApRecordSimple> findRecordForRelation(@RequestParam(required = false) @Nullable final String search,
                                                            @RequestParam final Integer from,
                                                            @RequestParam final Integer count,
                                                            @RequestParam final Integer roleTypeId,
                                                            @RequestParam final Integer partyId) {

        ParParty party = partyRepository.getOneCheckExist(partyId);
        ApState apState = accessPointService.getState(party.getAccessPoint());

        ParRelationRoleType relationRoleType = relationRoleTypeRepository.getOneCheckExist(roleTypeId);

        Set<Integer> apTypeIds = apTypeRepository.findByRelationRoleType(relationRoleType)
                .stream().map(ApType::getApTypeId).collect(Collectors.toSet());
        apTypeIds = apTypeRepository.findSubtreeIds(apTypeIds);

        final ApScope scope = apState.getScope();
        Set<Integer> scopeIds = new HashSet<>();
        scopeIds.add(scope.getScopeId());
        scopeRepository.findConnectedByScope(scope).forEach(cs -> scopeIds.add(cs.getScopeId()));

        Collection<ApState.StateApproval> states = null;

        final long foundRecordsCount = accessPointRepository.findApAccessPointByTextAndTypeCount(search, apTypeIds, scopeIds, states);

        final List<ApState> foundRecords = accessPointRepository.findApAccessPointByTextAndType(search, apTypeIds, from, count, scopeIds, states);

        return new FilteredResultVO<>(foundRecords, ap -> apFactory.createVOSimple(ap), foundRecordsCount);
    }

    /**
     * Vytvoření přístupového bodu.
     *
     * @param accessPoint zakládaný přístupový bod
     * @return přístupový bod
     */
    @Transactional
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ApAccessPointVO createAccessPoint(@RequestBody final ApAccessPointCreateVO accessPoint) {
        Integer typeId = accessPoint.getTypeId();
        Integer scopeId = accessPoint.getScopeId();

        ApScope scope = accessPointService.getScope(scopeId);
        ApType type = accessPointService.getType(typeId);
        SysLanguage language = StringUtils.isEmpty(accessPoint.getLanguageCode()) ? null : accessPointService.getLanguage(accessPoint.getLanguageCode());

        ApState apState = accessPointService.createAccessPoint(scope, type, language, accessPoint.getPartForm());
        return apFactory.createVO(apState);
    }

	@Transactional
    @RequestMapping(value = "/specificationHasParty/{itemSpecId}", method = RequestMethod.GET)
    public boolean canParty(@PathVariable final Integer itemSpecId) {
        Assert.notNull(itemSpecId, "Identifikátor specifikace musí být vyplněn");

        RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
        List<Integer> apTypeIds = itemAptypeRepository.findApTypeIdsByItemSpec(spec);
        Set<Integer> apTypeIdTree = apTypeRepository.findSubtreeIds(apTypeIds);

        Integer byItemSpecId = apTypeRepository.findCountPartyTypeNotNullByIds(apTypeIdTree);

        return byItemSpecId > 0;
    }

    /**
     * Založení strukturovaného přístupového bodu.
     *
     * @param accessPoint zakládaný přístupový bod
     * @return založený strukturovaný přístupový bod
     */
    @Transactional
    @RequestMapping(value = "/structured", method = RequestMethod.POST)
    public ApAccessPointVO createStructuredAccessPoint(@RequestBody final ApAccessPointCreateVO accessPoint) {
        Integer typeId = accessPoint.getTypeId();
        Integer scopeId = accessPoint.getScopeId();

        ApScope scope = accessPointService.getScope(scopeId);
        ApType type = accessPointService.getType(typeId);
        SysLanguage language = StringUtils.isEmpty(accessPoint.getLanguageCode()) ? null : accessPointService.getLanguage(accessPoint.getLanguageCode());

        ApState apState = accessPointService.createStructuredAccessPoint(scope, type, language);
        return apFactory.createVO(apState, true);
    }

    /**
     * Potvrzení dočasného přístupového bodu a jeho převalidování.
     *
     * @param accessPointId identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/confirm", method = RequestMethod.POST)
    public void confirmStructuredAccessPoint(@PathVariable final Integer accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        accessPointService.confirmAccessPoint(apState);
    }

    /**
     * Provede migraci přístupového bodu na strukturovaný.
     *
     * @param accessPointId identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/migrate", method = RequestMethod.POST)
    public void migrateAccessPoint(@PathVariable final Integer accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        apMigrationService.migrateAccessPoint(apState);
    }

    /**
     * Nastaví pravidla přístupovému bodu podle typu.
     *
     * @param accessPointId identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/setRule", method = RequestMethod.POST)
    public void setRuleAccessPoint(@PathVariable final Integer accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        accessPointService.setRuleAccessPoint(apState);
    }

    /**
     * Založení strukturovaného jména přístupového bodu - dočasné, nutné potvrdit {@link #confirmAccessPointStructuredName}.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @return založené strukturované jméno
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/structured", method = RequestMethod.POST)
    public ApAccessPointNameVO createAccessPointStructuredName(@PathVariable final Integer accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApName name = accessPointService.createAccessPointStructuredName(apState);
        return apFactory.createVO(name, apState.getApType(), true);
    }

    /**
     * Potvrzení dočasného jména a převalidování celého AP.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param objectId      identifikátor objektu jména
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/{objectId}/confirm", method = RequestMethod.POST)
    public void confirmAccessPointStructuredName(@PathVariable final Integer accessPointId,
                                                 @PathVariable final Integer objectId) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notNull(objectId, "Identifikátor objektu jména musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApName name = accessPointService.getName(objectId);
        accessPointService.confirmAccessPointName(apState, name);
    }

    /**
     * Úprava hodnot těla přístupového bodu. Přidání/upravení/smazání.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param items         položky ke změně
     * @return změněné jméno
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/items", method = RequestMethod.PUT)
    public List<ApItemVO> changeAccessPointItems(@PathVariable final Integer accessPointId,
                                                 @RequestBody final List<ApUpdateItemVO> items) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        List<ApItem> itemsCreated = accessPointService.changeApItems(apState, items);
        return apFactory.createItemsVO(itemsCreated);
    }

    /**
     * Smazání hodnot fragmentu podle typu.
     *
     * @param accessPointId identifikátor identifikátor přístupového bodu
     * @param itemTypeId    identifikátor typu atributu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/type/{itemTypeId}", method = RequestMethod.DELETE)
    public void deleteAccessPointItemsByType(@PathVariable final Integer accessPointId,
                                             @PathVariable final Integer itemTypeId) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notNull(itemTypeId, "Identifikátor typu musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        StaticDataProvider data = staticDataService.getData();
        ItemType type = data.getItemTypeById(itemTypeId);
        accessPointService.deleteApItemsByType(apState, type.getEntity());
    }

    /**
     * Úprava hodnot jména přístupového bodu. Přidání/upravení/smazání.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param objectId      identifikátor objektu jména
     * @param items         položky ke změně
     * @return nové položky, které ze vytvořili při změně
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/{objectId}/items", method = RequestMethod.PUT)
    public List<ApItemVO> changeNameItems(@PathVariable final Integer accessPointId,
                                          @PathVariable final Integer objectId,
                                          @RequestBody final List<ApUpdateItemVO> items) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notNull(objectId, "Identifikátor objektu jména přístupového bodu musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApName name = accessPointService.getName(objectId);
        List<ApItem> itemsCreated = accessPointService.changeNameItems(apState, name, items);
        return apFactory.createItemsVO(itemsCreated);
    }

    /**
     * Smazání hodnot jména podle typu.
     *
     * @param accessPointId identifikátor identifikátor přístupového bodu
     * @param objectId      identifikátor objektu jména
     * @param itemTypeId    identifikátor typu atributu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/{objectId}/type/{itemTypeId}", method = RequestMethod.DELETE)
    public void deleteNameItemsByType(@PathVariable final Integer accessPointId,
                                      @PathVariable final Integer objectId,
                                      @PathVariable final Integer itemTypeId) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notNull(objectId, "Identifikátor objektu jména přístupového bodu musí být vyplněn");
        Validate.notNull(itemTypeId, "Identifikátor typu musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApName name = accessPointService.getName(objectId);
        StaticDataProvider data = staticDataService.getData();
        ItemType type = data.getItemTypeById(itemTypeId);
        accessPointService.deleteNameItemsByType(apState, name, type.getEntity());
    }

    /**
     * Vytvoření nového dočasného fragmentu. Pro potvrzení je třeba použít {@link #confirmFragment}
     *
     * @param fragmentTypeCode kód typu fragmentu
     * @return založený fragment
     */
    @Transactional
    @RequestMapping(value = "/fragment/create/{fragmentTypeCode}", method = RequestMethod.POST)
    public ApFragmentVO createFragment(@PathVariable final String fragmentTypeCode) {
        Validate.notNull(fragmentTypeCode, "Kód typu fragmentu musí být vyplněn");

        RulPartType partType = structObjService.getPartTypeByCode(fragmentTypeCode);
        ApPart part = fragmentService.createPart(partType);
        return apFactory.createVO(part, true);
    }

    /**
     * Úprava hodnot fragmentu. Přidání/upravení/smazání.
     *
     * @param fragmentId identifikátor fragmentu
     * @param items      položky ke změně
     * @return upravený fragment
     */
    @Transactional
    @RequestMapping(value = "/fragment/{fragmentId}/items", method = RequestMethod.PUT)
    public ApFragmentVO changeFragmentItems(@PathVariable final Integer fragmentId,
                                            @RequestBody final List<ApUpdateItemVO> items) {
        Validate.notNull(fragmentId, "Identifikátor fragmentu musí být vyplněn");

        ApPart fragment = fragmentService.getFragment(fragmentId);
        fragmentService.changeFragmentItems(fragment, items);
        return apFactory.createVO(fragment, true);
    }

    /**
     * Smazání hodnot fragmentu podle typu.
     *
     * @param fragmentId identifikátor fragmentu
     * @param itemTypeId identifikátor typu atributu
     */
    @Transactional
    @RequestMapping(value = "/fragment/{fragmentId}/type/{itemTypeId}", method = RequestMethod.DELETE)
    public ApFragmentVO deleteFragmentItemsByType(@PathVariable final Integer fragmentId,
                                          @PathVariable final Integer itemTypeId) {
        Validate.notNull(fragmentId, "Identifikátor fragmentu musí být vyplněn");
        Validate.notNull(itemTypeId, "Identifikátor typu musí být vyplněn");

        ApPart fragment = fragmentService.getFragment(fragmentId);
        StaticDataProvider data = staticDataService.getData();
        ItemType type = data.getItemTypeById(itemTypeId);
        fragmentService.deleteFragmentItemsByType(fragment, type.getEntity());
        return apFactory.createVO(fragment, true);
    }

    /**
     * Potvrzení fragmentu.
     *
     * @param fragmentId identifikátor fragmentu
     */
    @Transactional
    @RequestMapping(value = "/fragment/{fragmentId}/confirm", method = RequestMethod.POST)
    public void confirmFragment(@PathVariable final Integer fragmentId) {
        Validate.notNull(fragmentId, "Identifikátor fragmentu musí být vyplněn");
        ApPart fragment = fragmentService.getFragment(fragmentId);
        fragmentService.confirmFragment(fragment);
    }

    /**
     * Získání fragmentu.
     *
     * @param fragmentId identifikátor fragmentu
     */
    @Transactional
    @RequestMapping(value = "/fragment/{fragmentId}", method = RequestMethod.GET)
    public ApFragmentVO getFragment(@PathVariable final Integer fragmentId) {
        Validate.notNull(fragmentId, "Identifikátor fragmentu musí být vyplněn");
        ApPart fragment = fragmentService.getFragment(fragmentId);
        return apFactory.createVO(fragment, true);
    }

    /**
     * Smazání fragmentu.
     *
     * @param fragmentId identifikátor fragmentu
     */
    @Transactional
    @RequestMapping(value = "/fragment/{fragmentId}", method = RequestMethod.DELETE)
    public void deleteFragment(@PathVariable final Integer fragmentId) {
        Validate.notNull(fragmentId, "Identifikátor fragmentu musí být vyplněn");
        ApPart fragment = fragmentService.getFragment(fragmentId);
        fragmentService.deleteFragment(fragment);
    }

    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     * @param accessPointId      id požadovaného hesla
     * @return              heslo s vazbou na var. hesla
     */
	@Transactional
    @RequestMapping(value = "/{accessPointId}", method = RequestMethod.GET)
    public ApAccessPointVO getAccessPoint(@PathVariable final String accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor rejstříkového hesla musí být vyplněn");

        ApAccessPoint ap;
        if (accessPointId.length() == 36) {
            ap = accessPointService.getAccessPointByUuid(accessPointId);
        } else {
            Integer apId;
            try {
                apId = Integer.parseInt(accessPointId);
            } catch (NumberFormatException nfe) {
                throw new SystemException("Unrecognized ID format")
                        .set("ID", accessPointId);
            }
            ap = accessPointService.getAccessPointInternal(apId);
        }
        ApState apState = accessPointService.getState(ap);
        // check permissions
        AuthorizationRequest authRequest = AuthorizationRequest.hasPermission(UsrPermission.Permission.AP_SCOPE_RD_ALL)
                .or(UsrPermission.Permission.AP_SCOPE_RD, apState.getScopeId());
        userService.authorizeRequest(authRequest);

        ApAccessPointVO vo = getAccessPoint(apState);
        return vo;
    }

    private ApAccessPointVO getAccessPoint(ApState apState) {
        ApAccessPointVO vo = apFactory.createVO(apState, true);
        ParParty party = partyService.findParPartyByAccessPoint(apState.getAccessPoint());
        if (party != null) {
            vo.setPartyId(party.getPartyId());
        }
        return vo;
    }

    /**
     * Aktualizace přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param editVo upravovaná data přístupového bodu
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}", method = RequestMethod.PUT)
    public ApAccessPointVO updateAccessPoint(@PathVariable final Integer accessPointId,
                                             @RequestBody final ApAccessPointEditVO editVo) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notNull(editVo);

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState oldState = accessPointService.getState(accessPoint);
        ApState newState = accessPointService.changeApType(accessPointId, editVo.getTypeId());
        return getAccessPoint(newState);
    }

    /**
     * Změna popisu přístupového bodu.
     *
     * @param accessPointId          identifikátor přístupového bodu
     * @param accessPointDescription popis přístupového bodu
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/description", method = RequestMethod.PUT)
    public ApAccessPointVO changeDescription(@PathVariable final Integer accessPointId,
                                             @RequestBody final ApAccessPointDescriptionVO accessPointDescription) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(accessPointDescription, "Přístupový bod musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApAccessPoint editedAccessPoint = accessPointService.changeDescription(apState, accessPointDescription.getDescription());
        return getAccessPoint(editedAccessPoint.getAccessPointId().toString());
    }

    /**
     * Smazání přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}", method = RequestMethod.DELETE)
    public void deleteAccessPoint(@PathVariable final Integer accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        accessPointService.deleteAccessPoint(apState, true);
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    @RequestMapping(value = "/recordTypes", method = RequestMethod.GET)
	@Transactional
    public List<ApTypeVO> getApTypes() {
        List<ApType> allTypes = apTypeRepository.findAllOrderByNameAsc();

        return apFactory.createTypesWithHierarchy(allTypes);
    }

    /**
     * Vrátí seznam kořenů typů rejstříku (typů hesel) pro typ osoby. Pokud je null, pouze pro typy, které nejsou pro osoby.
     *
     * @return seznam kořenů typů rejstříku (typů hesel)
     */
	@Transactional
    @RequestMapping(value = "/recordTypesForPartyType", method = RequestMethod.GET)
    public List<ApTypeVO> getRecordTypesForPartyType(
            @RequestParam(value = "partyTypeId", required = false) @Nullable final Integer partyTypeId) {

        if (partyTypeId == null) {
            List<ApType> apTypes = apTypeRepository.findByPartyTypeIsNullAndReadOnlyFalseOrderByName();
            return apFactory.createTypesWithHierarchy(apTypes);
        }

        ParPartyType partyType = partyTypeRepository.findOne(partyTypeId);
        Assert.notNull(partyType, "Nebyl nalezen typ osoby s id " + partyTypeId);

        List<ApType> apTypes = apTypeRepository.findByPartyTypeAndReadOnlyFalseOrderByName(partyType);
        return apFactory.createTypesWithHierarchy(apTypes);
    }

    /**
     * Vytvoření jména přístupového bodu - nepreferované.
     *
     * @param accessPointId   identifikátor přístupového bodu
     * @param accessPointName data jména
     * @return vytvořené jméno
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name", method = RequestMethod.POST)
    public ApAccessPointNameVO createAccessPointName(@PathVariable final Integer accessPointId,
                                                     @RequestBody final ApAccessPointNameVO accessPointName) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(accessPointName, "Jméno přístupového bodu musí být vyplněno");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        SysLanguage language = StringUtils.isEmpty(accessPointName.getLanguageCode())
                ? null
                : accessPointService.getLanguage(accessPointName.getLanguageCode());

        ApName name = accessPointService.createAccessPointName(apState,
                accessPointName.getName(),
                accessPointName.getComplement(),
                language);
        return apFactory.createVO(name);
    }

    /**
     * Upravení jména přístupového bodu.
     *
     * @param accessPointId   identifikátor přístupového bodu
     * @param accessPointName data jména
     * @return upravené jméno
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name", method = RequestMethod.PUT)
    public ApAccessPointNameVO updateAccessPointName(@PathVariable final Integer accessPointId,
                                                     @RequestBody final ApAccessPointNameVO accessPointName) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(accessPointName, "Jméno přístupového bodu musí být vyplněno");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        SysLanguage language = StringUtils.isEmpty(accessPointName.getLanguageCode())
                ? null
                : accessPointService.getLanguage(accessPointName.getLanguageCode());

        ApName name = accessPointService.getName(accessPointName.getObjectId());
        ApName updatedName = accessPointService.updateAccessPointName(apState,
                name,
                accessPointName.getName(),
                accessPointName.getComplement(),
                language);

        return apFactory.createVO(updatedName);
    }

    /**
     * Upravení jazyk strukturovaného jména přístupového bodu.
     *
     * @param accessPointId   identifikátor přístupového bodu
     * @param accessPointName data jména
     * @return upravené jméno
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/structured", method = RequestMethod.PUT)
    public ApAccessPointNameVO updateAccessPointStructuredName(@PathVariable final Integer accessPointId,
                                                               @RequestBody final ApAccessPointNameVO accessPointName) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(accessPointName, "Jméno přístupového bodu musí být vyplněno");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);

        SysLanguage language = StringUtils.isEmpty(accessPointName.getLanguageCode())
                ? null
                : accessPointService.getLanguage(accessPointName.getLanguageCode());

        ApName name = accessPointService.getName(accessPointName.getObjectId());
        ApName updatedName = accessPointService.updateAccessPointName(apState, name, language);
        return apFactory.createVO(updatedName);
    }

    /**
     * Získání jména přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param objectId      identifikátor objektu jména
     * @return jméno
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/{objectId}", method = RequestMethod.GET)
    public ApAccessPointNameVO getAccessPointName(@PathVariable final Integer accessPointId,
                                                  @PathVariable final Integer objectId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(objectId, "Identifikátor jména přístupového bodu musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApName name = accessPointService.getName(accessPoint, objectId);
        return apFactory.createVO(name, apState.getApType(), true);
    }

    /**
     * Smazání jména přístipového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param objectId      identifikátor objektu mazaného jména
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/{objectId}", method = RequestMethod.DELETE)
    public void deleteAccessPointName(@PathVariable final Integer accessPointId,
                                      @PathVariable final Integer objectId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(objectId, "Identifikátor jména přístupového bodu musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApName name = accessPointService.getName(objectId);
        accessPointService.deleteAccessPointName(apState, name);
    }

    /**
     * Nastavení jména přístupového bodu jako preferované.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param objectId      identifikátor objektu jména, které chceme jako preferované
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/{objectId}/preferred", method = RequestMethod.POST)
    public void setPreferredAccessPointName(@PathVariable final Integer accessPointId,
                                            @PathVariable final Integer objectId) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(objectId, "Identifikátor objektu jména přístupového bodu musí být vyplněn");

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApName name = accessPointService.getName(objectId);
        accessPointService.setPreferredAccessPointName(apState, name);
    }

    /**
     * Vrací všechny jazyky.
     */
    @RequestMapping(value = "/languages", method = RequestMethod.GET)
    @Transactional
    public List<LanguageVO> getAllLanguages() {
        List<SysLanguage> languages = accessPointService.findAllLanguagesOrderByCode();
        return FactoryUtils.transformList(languages, apFactory::createVO);
    }

    /**
     * Vrací typy externích identifikátorů.
     */
    @RequestMapping(value = "/eidTypes", method = RequestMethod.GET)
    @Transactional
    public List<ApEidTypeVO> getAllExternalIdTypes() {
        StaticDataProvider data = staticDataService.getData();
        List<ApExternalIdType> types = data.getApEidTypes();
        return FactoryUtils.transformList(types, apFactory::createVO);
    }

    /**
     * Vrací všechny třídy rejstříků z databáze.
     */
    @RequestMapping(value = "/scopes", method = RequestMethod.GET)
	@Transactional
    public List<ApScopeVO> getAllScopes(){
        List<ApScope> apScopes = scopeRepository.findAllOrderByCode();
        StaticDataProvider staticData = staticDataService.getData();
        return FactoryUtils.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData));
    }

    /**
     * Vrátí třídu restříku, včetně na ni navázaných tříd.
     */
    @RequestMapping(value = "/scopes/{scopeId}/withConnected", method = RequestMethod.GET)
    @Transactional
    public ApScopeWithConnectedVO getScopeWithConnected(@PathVariable("scopeId") Integer scopeId) {
        ApScope apScope = scopeRepository.findOne(scopeId);
        Assert.notNull(apScope, "Nebyla nalezena třída rejstříku s ID=" + scopeId);
        List<ApScope> connectedScopes = scopeRepository.findConnectedByScope(apScope);
        StaticDataProvider staticData = staticDataService.getData();
        return ApScopeWithConnectedVO.newInstance(apScope, staticData, connectedScopes);
    }

    /**
     * Pokud je nastavená verze, vrací třídy napojené na verzi, jinak vrací třídy nastavené v konfiguraci elzy (YAML).
     * @param versionId id verze nebo null
     * @return seznam tříd
     */
    @RequestMapping(value = "/fundScopes", method = RequestMethod.GET)
	@Transactional
    public List<ApScopeVO> getScopeIdsByVersion(@RequestParam(required = false) @Nullable final Integer versionId) {

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.findOne(versionId);
            Assert.notNull(version, "Nebyla nalezena verze s id " + versionId);
            fund = version.getFund();
        }

        Set<Integer> scopeIdsByFund = accessPointService.getScopeIdsForSearch(fund, null);
        if (CollectionUtils.isEmpty(scopeIdsByFund)) {
            return Collections.emptyList();
        }

        List<ApScope> apScopes = scopeRepository.findAll(scopeIdsByFund);
        StaticDataProvider staticData = staticDataService.getData();
        List<ApScopeVO> result = FactoryUtils.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData));
        result.sort(Comparator.comparing(ApScopeVO::getCode));
        return result;
    }

    /**
     * Vložení nové třídy.
     *
     * @return nový objekt třídy
     */
    @Transactional
    @RequestMapping(value = "/scopes", method = RequestMethod.POST)
    public ApScopeVO createScope() {
        ApScope apScope = new ApScope();
        apScope.setCode(UUID.randomUUID().toString());
        apScope.setName("NewScope");
        apScope = accessPointService.saveScope(apScope);
        StaticDataProvider staticData = staticDataService.getData();
        return ApScopeVO.newInstance(apScope, staticData);
    }

    /**
     * Aktualizace třídy.
     *
     * @param scopeId id třídy
     * @param scopeVO objekt třídy
     * @return aktualizovaný objekt třídy
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}", method = RequestMethod.PUT)
    public ApScopeVO updateScope(@PathVariable final Integer scopeId, @RequestBody final ApScopeVO scopeVO) {
        Assert.notNull(scopeId, "Identifikátor scope musí být vyplněn");
        Assert.notNull(scopeVO, "Scope musí být vyplněn");

        Assert.isTrue(
                scopeId.equals(scopeVO.getId()),
                "V url požadavku je odkazováno na jiné ID (" + scopeId + ") než ve VO (" + scopeVO.getId() + ")."
        );

        StaticDataProvider staticData = staticDataService.getData();
        ApScope apScope = scopeVO.createEntity(staticData);
        apScope = accessPointService.saveScope(apScope);
        return ApScopeVO.newInstance(apScope, staticData);
    }

    /**
     * Provázání tříd.
     *
     * @param scopeId ID třídy ke které se bude vázat
     * @param connectedScopeId ID třídy která bude provázána
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}/connect", method = RequestMethod.POST)
    public void connectScope(@PathVariable("scopeId") final Integer scopeId, @RequestBody final Integer connectedScopeId) {
        Assert.notNull(connectedScopeId, "Identifikátor vázaného scope musí být vyplněn");

        if (scopeId.equals(connectedScopeId)) {
            throw new BusinessException("Třídu rejstříku nelze navázat sama na sebe", RegistryCode.CANT_CONNECT_SCOPE_TO_SELF);
        }

        final ApScope scope = scopeRepository.findOne(scopeId);
        final ApScope connectedScope = scopeRepository.findOne(connectedScopeId);
        accessPointService.connectScope(scope, connectedScope);
    }

    /**
     * Zrušení provázání tříd.
     *
     * @param scopeId ID třídy
     * @param connectedScopeId ID třídy k odpojení
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}/disconnect", method = RequestMethod.POST)
    public void disconnectScope(@PathVariable("scopeId") final Integer scopeId, @RequestBody final Integer connectedScopeId) {
        Assert.notNull(connectedScopeId, "Identifikátor vázané třídy musí být vyplněn");

        final ApScope scope = scopeRepository.findOne(scopeId);
        final ApScope connectedScope = scopeRepository.findOne(connectedScopeId);
        accessPointService.disconnectScope(scope, connectedScope);
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param scopeId id třídy.
     */
    @Transactional
    @RequestMapping(value = "/scopes/{scopeId}", method = RequestMethod.DELETE)
    public void deleteScope(@PathVariable final Integer scopeId) {
        ApScope scope = scopeRepository.findOne(scopeId);
        accessPointService.deleteScope(scope);
    }

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @RequestMapping(value = "/externalSystems", method = RequestMethod.GET)
	@Transactional
    public List<ApExternalSystemSimpleVO> findAllExternalSystems() {
		List<ApExternalSystem> extSystems = externalSystemService.findAllApSystem();
		return FactoryUtils.transformList(extSystems, ApExternalSystemSimpleVO::newInstance);
    }

    /**
     * Najde použití rejstříku.
     *
     * @param accessPointId identifikátor rejstříku
     *
     * @return použití rejstříku
     */
    @RequestMapping(value = "/{accessPointId}/usage", method = RequestMethod.GET)
    @Transactional
    public RecordUsageVO findUsage(@PathVariable final Integer accessPointId) {
    	ApAccessPoint apAccessPoint = accessPointRepository.getOneCheckExist(accessPointId);
    	ParParty parParty = partyService.findParPartyByAccessPoint(apAccessPoint);
    	return accessPointService.findRecordUsage(apAccessPoint, parParty);
    }

    /**
     * Nahrazení rejstříku
     *
     * @param accessPointId ID nahrazovaného rejstříku
     * @param replacedId ID rejstříku kterým budeme nahrazovat
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/replace", method = RequestMethod.POST)
    public void replace(@PathVariable final Integer accessPointId, @RequestBody final Integer replacedId) {

        final ApAccessPoint replaced = accessPointService.getAccessPointInternal(accessPointId);
        final ApAccessPoint replacement = accessPointService.getAccessPointInternal(replacedId);

        final ParParty replacedParty = partyService.findParPartyByAccessPoint(replaced);
        if (replacedParty != null) {
            final ParParty replacementParty = partyService.findParPartyByAccessPoint(replacement);
            if (replacementParty == null) {
                throw new BusinessException("Osobu lze nahradit pouze osobou.", BaseCode.INVALID_STATE);
            }
            partyService.replace(replacedParty, replacementParty);
        } else {
            ApState replacedState = accessPointService.getState(replaced);
            ApState replacementState = accessPointService.getState(replacement);
            accessPointService.replace(replacedState, replacementState);
        }
    }

    /**
     * Vyhledání historie stavů seřazené sestupně dle stáří (první je tedy aktuální).
     *
     * @param accessPointId identifikátor přístupového bodu
     * @return seznam stavů v historii
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/history", method = RequestMethod.GET)
    public List<ApStateHistoryVO> findStateHistories(@PathVariable("accessPointId") final Integer accessPointId) {
        ApAccessPoint apAccessPoint = accessPointService.getAccessPoint(accessPointId);
        List<ApState> states = accessPointService.findApStates(apAccessPoint);
        return apFactory.createStateHistoriesVO(states);
    }

    /**
     * Změna stavu přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/state", method = RequestMethod.POST)
    public void changeState(@PathVariable("accessPointId") final Integer accessPointId,
                            @RequestBody ApStateChangeVO stateChange) {
        Validate.notNull(stateChange.getState(), "AP State is null");

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);

        accessPointService.updateApState(accessPoint, stateChange.getState(), stateChange.getComment(), stateChange.getTypeId(), stateChange.getScopeId());
    }

    /**
     * Založení nové části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param apPartFormVO data pro vytvoření části
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part", method = RequestMethod.POST)
    public void createPart(@PathVariable final Integer accessPointId,
                           @RequestBody final ApPartFormVO apPartFormVO) {

    }

    /**
     * Úprava části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor upravované části
     * @param apPartFormVO data pro úpravu části
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part/{partId}", method = RequestMethod.POST)
    public void updatePart(@PathVariable final Integer accessPointId,
                           @PathVariable final Integer partId,
                           @RequestBody final ApPartFormVO apPartFormVO) {

    }


    /**
     * Smazání části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor mazané části
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part/{partId}", method = RequestMethod.DELETE)
    public void deletePart(@PathVariable final Integer accessPointId,
                           @PathVariable final Integer partId) {

    }

    /**
     * Nastavení preferovaného jména přístupového bodu.
     * Možné pouze pro části typu Označení.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor části, kterou nastavujeme jako preferovanou
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/part/{partId}/prefer-name", method = RequestMethod.PUT)
    public void setPreferName(@PathVariable final Integer accessPointId,
                              @PathVariable final Integer partId) {

    }

    /**
     * Validace přístupového bodu
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @return validační chyby přístupového bodu
     */
    @Transactional
    @RequestMapping(value = "{accessPointId}/validate", method = RequestMethod.GET)
    public ApValidationErrorsVO validateAccessPoint(@PathVariable final Integer accessPointId) {
        return new ApValidationErrorsVO();
    }

    /**
     * Zjištění povinných a možných atributů pro zakládání nového přístupového bodu nebo nové části
     *
     * @param apAccessPointCreateVO průběžná data pro založení
     * @return vyhodnocené typy a specifikace atributů, které jsou třeba pro založení přístupového bodu nebo části
     */
    @Transactional
    @RequestMapping(value = "/available/items", method = RequestMethod.POST)
    public ApAttributesInfoVO getAvailableItems(@RequestBody final ApAccessPointCreateVO apAccessPointCreateVO) {
        return new ApAttributesInfoVO();
    }
}
