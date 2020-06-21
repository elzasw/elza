package cz.tacr.elza.controller;

import cz.tacr.cam._2019.Entity;
import cz.tacr.cam._2019.QueryResult;
import cz.tacr.cam.client.ApiException;
import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.factory.SearchFilterFactory;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.drools.model.ItemSpec;
import cz.tacr.elza.drools.model.ModelAvailable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.ArrayList;
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
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private ApFactory apFactory;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private PartService partService;

    @Autowired
    private StructObjService structObjService;

    @Autowired
    private UserService userService;

    @Autowired
    private SearchFilterFactory searchFilterFactory;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private CamConnector camConnector;

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
                                                             @RequestParam(required = false) @Nullable final Integer lastRecordNr,
                                                             @RequestParam(required = false) @Nullable final SearchType searchTypeName,
                                                             @RequestParam(required = false) @Nullable final SearchType searchTypeUsername) {

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

        SearchType searchTypeNameFinal = searchTypeName != null ? searchTypeName : SearchType.FULLTEXT;
        SearchType searchTypeUsernameFinal = searchTypeUsername != null ? searchTypeUsername : SearchType.DISABLED;

        final long foundRecordsCount = accessPointService.findApAccessPointByTextAndTypeCount(search, apTypeIdTree, fund, scopeId, states, searchTypeNameFinal, searchTypeUsernameFinal);

        final List<ApState> foundRecords = accessPointService.findApAccessPointByTextAndType(search, apTypeIdTree, from, count, fund, scopeId, states, searchTypeNameFinal, searchTypeUsernameFinal);

        return new FilteredResultVO<>(foundRecords, apState -> apFactory.createVO(apState), foundRecordsCount);
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
        return apFactory.createVO(apState);
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
        //TODO : smazána převalidace
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
        ApPart part = partService.createPart(partType);
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

        ApPart fragment = partService.getPart(fragmentId);
        partService.changeFragmentItems(fragment, items);
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

        ApPart fragment = partService.getPart(fragmentId);
        StaticDataProvider data = staticDataService.getData();
        ItemType type = data.getItemTypeById(itemTypeId);
        partService.deleteFragmentItemsByType(fragment, type.getEntity());
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
        ApPart fragment = partService.getPart(fragmentId);
        partService.confirmFragment(fragment);
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
        ApPart fragment = partService.getPart(fragmentId);
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
        ApPart fragment = partService.getPart(fragmentId);
        partService.deleteFragment(fragment);
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

        ApAccessPointVO vo = apFactory.createVO(apState);
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
        return apFactory.createVO(newState);
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
    	return accessPointService.findRecordUsage(apAccessPoint);
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

        ApState replacedState = accessPointService.getState(replaced);
        ApState replacementState = accessPointService.getState(replacement);
        accessPointService.replace(replacedState, replacementState);
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
        ApAccessPoint apAccessPoint = accessPointRepository.findOne(accessPointId);
        ApPart apPart = partService.createPart(apAccessPoint, apPartFormVO);
        accessPointService.updatePartValue(apPart);
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
        ApAccessPoint apAccessPoint = accessPointRepository.findOne(accessPointId);
        ApPart apPart = partService.getPart(partId);
        accessPointService.updatePart(apAccessPoint, apPart, apPartFormVO);
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
        ApAccessPoint apAccessPoint = accessPointRepository.findOne(accessPointId);
        partService.deletePart(apAccessPoint, partId);
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
        ApAccessPoint apAccessPoint = accessPointRepository.findOne(accessPointId);
        ApPart apPart = partService.getPart(partId);
        accessPointService.setPreferName(apAccessPoint, apPart);
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
        return ruleService.executeValidation(accessPointId);
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
        if (true) {
            boolean hasHlavniCast = false;
            for (ApItemVO item : apAccessPointCreateVO.getPartForm().getItems()) {
                if (item.getTypeId() == 11) {
                    hasHlavniCast = true;
                    break;
                }
            }

            final boolean hasHlavniCast2 = hasHlavniCast;
            List<ApCreateTypeVO> list = staticDataService.getData().getItemTypes().stream()
                    .map(x -> {
                        ApCreateTypeVO vo = new ApCreateTypeVO();
                        vo.setItemTypeId(x.getItemTypeId());
                        vo.setRequiredType(RequiredType.POSSIBLE);
                        vo.setRepeatable(false);

                        if (hasHlavniCast2 && x.getItemTypeId() < 10) {
                            vo.setRequiredType(RequiredType.REQUIRED);
                        }
                        return vo;
                    })
                    .collect(Collectors.toList());
            ApAttributesInfoVO aeAttributesInfoVO = new ApAttributesInfoVO();
            aeAttributesInfoVO.setAttributes(list);
            aeAttributesInfoVO.setErrors(Collections.emptyList());
            return aeAttributesInfoVO;
        }

        List<ApCreateTypeVO> result = new ArrayList<>();
        ModelAvailable modelAvailable = ruleService.executeAvailable(apAccessPointCreateVO);
        for (cz.tacr.elza.drools.model.ItemType itemType : modelAvailable.getItemTypes()) {
            if (itemType.getRequiredType() != cz.tacr.elza.drools.model.RequiredType.IMPOSSIBLE) {
                ApCreateTypeVO createTypeVO = new ApCreateTypeVO();
                createTypeVO.setItemTypeId(itemType.getId());
                createTypeVO.setRepeatable(itemType.isRepeatable());
                createTypeVO.setRequiredType(RequiredType.fromValue(itemType.getRequiredType().name()));
                List<Integer> specIds = itemType.getSpecs().stream()
                        .filter(s -> s.getRequiredType() != cz.tacr.elza.drools.model.RequiredType.IMPOSSIBLE)
                        .map(ItemSpec::getId)
                        .collect(Collectors.toList());
                createTypeVO.setItemSpecIds(specIds);
                result.add(createTypeVO);
            }
        }

        List<String> errors = ruleService.validateAvailableItems(modelAvailable);

        ApAttributesInfoVO apAttributesInfoVO = new ApAttributesInfoVO();
        apAttributesInfoVO.setAttributes(result);
        apAttributesInfoVO.setErrors(errors);

        return apAttributesInfoVO;
    }

    @Transactional
    @RequestMapping(value = "/external/search", method = RequestMethod.POST)
    public ArchiveEntityResultListVO findArchiveEntitiesInExternalSystem(@RequestParam(name = "from", defaultValue = "0", required = false) final Integer from,
                                                                         @RequestParam(name = "max", defaultValue = "50", required = false) final Integer max,
                                                                         @RequestParam(name = "externalSystemCode") final String externalSystemCode,
                                                                         @RequestBody final SearchFilterVO filter) {
        if (from < 0) {
            throw new SystemException("Parametr from musí být >=0", BaseCode.PROPERTY_IS_INVALID);
        }
        int fromPage = from / max;
        QueryResult result;
        try {
            result = camConnector.search(fromPage + 1, max, searchFilterFactory.createQueryParamsDef(filter), externalSystemCode);
        } catch (ApiException e) {
            throw new SystemException("Došlo k chybě při komunikaci s externím systémem.", e);
        }
        return searchFilterFactory.createArchiveEntityVoListResult(result);
    }

    @Transactional
    @RequestMapping(value = "/external/{archiveEntityId}/take", method = RequestMethod.POST)
    public Integer takeArchiveEntity(@PathVariable("archiveEntityId") final Integer archiveEntityId,
                                     @RequestParam final Integer scopeId,
                                     @RequestParam final String externalSystemCode) {
        Entity entity;
        try {
            entity = camConnector.getEntityById(archiveEntityId, externalSystemCode);
        } catch (ApiException e) {
            throw new SystemException("Došlo k chybě při komunikaci s externím systémem.");
        }
        ApScope scope = accessPointService.getScope(scopeId);
        ApState apState = accessPointService.createAccessPoint(scope, entity);
        return apState.getAccessPointId();
    }

    @Transactional
    @RequestMapping(value = "/external/{archiveEntityId}/connect/{accessPointId}", method = RequestMethod.POST)
    public void connectArchiveEntity(@PathVariable("archiveEntityId") final Integer archiveEntityId,
                                     @PathVariable("accessPointId") final Integer accessPointId,
                                     @RequestParam final String externalSystemCode) {
    }
}
