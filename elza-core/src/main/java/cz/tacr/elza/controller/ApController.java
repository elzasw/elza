package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.ap.ApFragmentTypeVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.interpi.service.InterpiService;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.FragmentService;
import cz.tacr.elza.service.PartyService;
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
import java.util.*;
import java.util.stream.Collectors;


/**
 * REST kontroler pro registry.
 */
@RestController
@RequestMapping(value = "/api/registry", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ApController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private ClientFactoryVO factoryVo;

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
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private InterpiService interpiService;

    @Autowired
    private ApFactory apFactory;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private FragmentService fragmentService;

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
                                                             @RequestParam(required = false) @Nullable final Integer scopeId,
                                                             @RequestParam(required = false) @Nullable final Integer lastRecordNr) {

        Set<Integer> apTypeIdTree = Collections.emptySet();

        if (itemSpecId != null && apTypeId != null) {
            throw new SystemException("Nelze použít specifikaci a typ rejstříku zároveň.", BaseCode.SYSTEM_ERROR);
        } else if (itemSpecId != null || apTypeId != null) {
            Set<Integer> apTypeIds = new HashSet<>();
            if (itemSpecId != null) {
                RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
                apTypeIds.addAll(itemSpecRegisterRepository.findIdsByItemSpecId(spec));
            } else {
                apTypeIds.add(apTypeId);
            }
            apTypeIdTree = apTypeRepository.findSubtreeIds(apTypeIds);
        }

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
            fund = version.getFund();
        }

        final long foundRecordsCount = accessPointService.findApAccessPointByTextAndTypeCount(search, apTypeIdTree, fund, scopeId);

        List<ApAccessPoint> foundRecords = accessPointService.findApAccessPointByTextAndType(search, apTypeIdTree, from,
                count, fund, scopeId);

        Map<Integer, Integer> recordIdPartyIdMap = partyService.findParPartyIdsByRecords(foundRecords);

        List<ApAccessPointVO> foundRecordVOList = new ArrayList<>(foundRecords.size());
        for (ApAccessPoint ap : foundRecords) {
            ApAccessPointVO vo = apFactory.createVO(ap);
            vo.setPartyId(recordIdPartyIdMap.get(vo.getId()));
            foundRecordVOList.add(vo);
        }
        return new FilteredResultVO<>(foundRecordVOList, foundRecordsCount);
    }


    /**
     * Najde seznam rejstříkových hesel, která jsou typu napojeného na dané relationRoleTypeId a mají třídu rejstříku
     * stejnou jako daná osoba.
     *
     * @param search     hledaný řetězec
     * @param from       odkud se mají vracet výsledka
     * @param count      počet vracených výsledků
     * @param roleTypeId id typu vztahu
     * @param partyId    id osoby, ze které je načtena hledaná třída rejstříku
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

        ParRelationRoleType relationRoleType = relationRoleTypeRepository.getOneCheckExist(roleTypeId);


        Set<Integer> apTypeIds = apTypeRepository.findByRelationRoleType(relationRoleType)
                .stream().map(ApType::getApTypeId).collect(Collectors.toSet());
        apTypeIds = apTypeRepository.findSubtreeIds(apTypeIds);

        Set<Integer> scopeIds = new HashSet<>();
        scopeIds.add(party.getAccessPoint().getScope().getScopeId());

        final long foundRecordsCount = accessPointRepository.findApAccessPointByTextAndTypeCount(search, apTypeIds,
                scopeIds);

        final List<ApAccessPoint> foundRecords = accessPointRepository.findApAccessPointByTextAndType(search, apTypeIds,
                from, count, scopeIds);

        List<ApRecordSimple> foundRecordsVO = new ArrayList<>(foundRecords.size());
        foundRecords.forEach(ap -> foundRecordsVO.add(apFactory.createVOSimple(ap)));

        return new FilteredResultVO<>(foundRecordsVO, foundRecordsCount);
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
        String name = StringUtils.isEmpty(accessPoint.getName()) ? null : accessPoint.getName();
        String description = StringUtils.isEmpty(accessPoint.getDescription()) ? null : accessPoint.getDescription();
        String complement = StringUtils.isEmpty(accessPoint.getComplement()) ? null : accessPoint.getComplement();

        ApAccessPoint createdAccessPoint = accessPointService.createAccessPoint(scope, type, name, complement, language, description);
        return apFactory.createVO(createdAccessPoint);
    }

	@Transactional
    @RequestMapping(value = "/specificationHasParty/{itemSpecId}", method = RequestMethod.GET)
    public boolean canParty(@PathVariable final Integer itemSpecId) {
        Assert.notNull(itemSpecId, "Identifikátor specifikace musí být vyplněn");

        RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
        Set<Integer> apTypeIds = itemSpecRegisterRepository.findIdsByItemSpecId(spec);
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

        ApAccessPoint createdAccessPoint = accessPointService.createStructuredAccessPoint(scope, type, language);
        return apFactory.createVO(createdAccessPoint, true);
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
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        accessPointService.confirmAccessPoint(accessPoint);
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
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        accessPointService.setRuleAccessPoint(accessPoint);
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);

        ApName name = accessPointService.createAccessPointStructuredName(accessPoint);
        return apFactory.createVO(name, true);
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApName name = accessPointService.getName(objectId);
        accessPointService.confirmAccessPointName(accessPoint, name);
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
    public void changeAccessPointItems(@PathVariable final Integer accessPointId,
                                              @RequestBody final List<ApUpdateItemVO> items) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        accessPointService.changeApItems(accessPoint, items);
    }

    /**
     * Úprava hodnot jména přístupového bodu. Přidání/upravení/smazání.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param objectId      identifikátor objektu jména
     * @param items         položky ke změně
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}/name/{objectId}/items", method = RequestMethod.PUT)
    public void changeNameItems(@PathVariable final Integer accessPointId,
                                @PathVariable final Integer objectId,
                                @RequestBody final List<ApUpdateItemVO> items) {
        Validate.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Validate.notNull(objectId, "Identifikátor objektu jména přístupového bodu musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApName name = accessPointService.getName(objectId);
        accessPointService.changeNameItems(accessPoint, name, items);
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

        ApFragmentType fragmentType = fragmentService.getFragmentType(fragmentTypeCode);
        ApFragment fragment = fragmentService.createFragment(fragmentType);
        return apFactory.createVO(fragment, true);
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

        ApFragment fragment = fragmentService.getFragment(fragmentId);
        fragmentService.changeFragmentItems(fragment, items);
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
        ApFragment fragment = fragmentService.getFragment(fragmentId);
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
        ApFragment fragment = fragmentService.getFragment(fragmentId);
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
        ApFragment fragment = fragmentService.getFragment(fragmentId);
        fragmentService.deleteFragment(fragment);
    }

    /**
     * Získání všech typů fragmentů, které jsou k dispozici.
     *
     * @return typy fragmentů
     */
    @RequestMapping(value = "/fragment/types", method = RequestMethod.GET)
    public List<ApFragmentTypeVO> findFragmentTypes() {
        List<ApFragmentType> fragmentTypes = fragmentService.findFragmentTypes();
        return ApFactory.transformList(fragmentTypes, apFactory::createVO);
    }

    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     * @param accessPointId      id požadovaného hesla
     * @return              heslo s vazbou na var. hesla
     */
	@Transactional
    @RequestMapping(value = "/{accessPointId}", method = RequestMethod.GET)
    public ApAccessPointVO getAccessPoint(@PathVariable final Integer accessPointId) {
        Assert.notNull(accessPointId, "Identifikátor rejstříkového hesla musí být vyplněn");

        ApAccessPoint ap = accessPointService.getAccessPoint(accessPointId);
        ApAccessPointVO vo = apFactory.createVO(ap, true);

        ParParty party = partyService.findParPartyByAccessPoint(ap);
        if (party != null) {
            vo.setPartyId(party.getPartyId());
        }
        return vo;
    }

    /**
     * Aktualizace přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param accessPoint upravovaná data přístupového bodu
     * @return aktualizovaný záznam
     */
    @Transactional
    @RequestMapping(value = "/{accessPointId}", method = RequestMethod.PUT)
    public ApAccessPointVO updateAccessPoint(@PathVariable final Integer accessPointId,
                                             @RequestBody final ApAccessPointEditVO accessPoint) {
        Assert.notNull(accessPointId, "Identifikátor přístupového bodu musí být vyplněn");
        Assert.notNull(accessPoint, "Přístupový bod musí být vyplněn");

        ApAccessPoint accessPointEdit = accessPointService.getAccessPoint(accessPointId);
        ApType type = accessPointService.getType(accessPoint.getTypeId());
        ApAccessPoint editedAccessPoint = accessPointService.updateAccessPoint(accessPointEdit, type);
        return getAccessPoint(editedAccessPoint.getAccessPointId());
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApAccessPoint editedAccessPoint = accessPointService.changeDescription(accessPoint, accessPointDescription.getDescription());
        return getAccessPoint(editedAccessPoint.getAccessPointId());
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
        accessPointService.deleteAccessPoint(accessPointId, true);
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        SysLanguage language = StringUtils.isEmpty(accessPointName.getLanguageCode())
                ? null
                : accessPointService.getLanguage(accessPointName.getLanguageCode());

        ApName name = accessPointService.createAccessPointName(accessPoint,
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        SysLanguage language = StringUtils.isEmpty(accessPointName.getLanguageCode())
                ? null
                : accessPointService.getLanguage(accessPointName.getLanguageCode());

        ApName name = accessPointService.getName(accessPointName.getObjectId());
        ApName updatedName = accessPointService.updateAccessPointName(accessPoint,
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        SysLanguage language = StringUtils.isEmpty(accessPointName.getLanguageCode())
                ? null
                : accessPointService.getLanguage(accessPointName.getLanguageCode());

        ApName name = accessPointService.getName(accessPointName.getObjectId());
        ApName updatedName = accessPointService.updateAccessPointName(accessPoint, name, language);
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
        ApName name = accessPointService.getName(accessPoint, objectId);
        return apFactory.createVO(name, true);
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApName name = accessPointService.getName(objectId);
        accessPointService.deleteAccessPointName(accessPoint, name);
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

        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApName name = accessPointService.getName(objectId);
        accessPointService.setPreferredAccessPointName(accessPoint, name);
    }

    /**
     * Vrací všechny jazyky.
     */
    @RequestMapping(value = "/languages", method = RequestMethod.GET)
    @Transactional
    public List<LanguageVO> getAllLanguages() {
        List<SysLanguage> languages = accessPointService.findAllLanguagesOrderByCode();
        return ApFactory.transformList(languages, apFactory::createVO);
    }

    /**
     * Vrací typy externích identifikátorů.
     */
    @RequestMapping(value = "/eidTypes", method = RequestMethod.GET)
    @Transactional
    public List<ApEidTypeVO> getAllExternalIdTypes() {
        StaticDataProvider data = staticDataService.getData();
        List<ApExternalIdType> types = data.getApEidTypes();
        return ApFactory.transformList(types, apFactory::createVO);
    }

    /**
     * Vrací všechny třídy rejstříků z databáze.
     */
    @RequestMapping(value = "/scopes", method = RequestMethod.GET)
	@Transactional
    public List<ApScopeVO> getAllScopes(){
        List<ApScope> apScopes = scopeRepository.findAllOrderByCode();
        StaticDataProvider staticData = staticDataService.getData();
        return ApFactory.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData));
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
        List<ApScopeVO> result = ApFactory.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData));
        result.sort(Comparator.comparing(ApScopeVO::getCode));
        return result;
    }

    /**
     * Vložení nové třídy.
     *
     * @param scopeVO objekt třídy
     * @return nový objekt třídy
     */
    @Transactional
    @RequestMapping(value = "/scopes", method = RequestMethod.POST)
    public ApScopeVO createScope(@RequestBody final ApScopeVO scopeVO) {
        Assert.notNull(scopeVO, "Scope musí být vyplněn");
        Assert.isNull(scopeVO.getId(), "Identifikátor scope musí být vyplněn");

        StaticDataProvider staticData = staticDataService.getData();
        ApScope apScope = scopeVO.createEntity(staticData);
        apScope = accessPointService.saveScope(apScope);
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
		return ApFactory.transformList(extSystems, ApExternalSystemSimpleVO::newInstance);
    }

    /**
     * Aktualizace rejstříku z externího systému.
     * @param accessPointId id rejstříku
     * @param recordImportVO data rejstříku
     */
    @RequestMapping(value = "/interpi/import/{accessPointId}", method = RequestMethod.PUT)
    @Transactional
    public ApAccessPointVO updateAccessPoint(@PathVariable final Integer accessPointId, @RequestBody final RecordImportVO recordImportVO) {
        Assert.notNull(accessPointId, "Identifikátor rejstříkového hesla musí být vyplněn");
        Assert.notNull(recordImportVO, "Struktura importu hesla musí být vyplněna");
        Assert.notNull(recordImportVO.getInterpiRecordId(), "Identifikátor interpi musí být vyplněn");
        Assert.notNull(recordImportVO.getScopeId(), "Identifikátor scope musí být vyplněn");
        Assert.notNull(recordImportVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        interpiService.importRecord(accessPointId, recordImportVO.getInterpiRecordId(), recordImportVO.getScopeId(),
                recordImportVO.getSystemId(), recordImportVO.getOriginator(), recordImportVO.getMappings());

        return getAccessPoint(accessPointId);
    }

    /**
     * Založení rejstříku z externího systému.
     * @param recordImportVO data rejstříku
     */
    @Transactional
    @RequestMapping(value = "/interpi/import", method = RequestMethod.POST)
    public ApAccessPointVO importRecord(@RequestBody final RecordImportVO recordImportVO) {
        Assert.notNull(recordImportVO, "Struktura importu hesla musí být vyplněna");
        Assert.notNull(recordImportVO.getInterpiRecordId(), "Identifikátor interpi musí být vyplněn");
        Assert.notNull(recordImportVO.getScopeId(), "Identifikátor scope musí být vyplněn");
        Assert.notNull(recordImportVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        ApAccessPoint apRecord = interpiService.importRecord(null, recordImportVO.getInterpiRecordId(), recordImportVO.getScopeId(),
                recordImportVO.getSystemId(), recordImportVO.getOriginator(), recordImportVO.getMappings());

        return getAccessPoint(apRecord.getAccessPointId());
    }

    /**
     * Vyhledá rejstříky v externím systému.
     *
     * @param interpiSearchVO vyhledávací kritéria
     *
     * @return rejstřík z externího systému
     */
    @Transactional
    @RequestMapping(value = "/interpi", method = RequestMethod.POST)
    public List<ExternalRecordVO> findInterpiRecords(@RequestBody final InterpiSearchVO interpiSearchVO) {
        Assert.notNull(interpiSearchVO, "Struktura pro vyhledání musí být vyplněna");
        Assert.notNull(interpiSearchVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        long start = System.currentTimeMillis();
        List<ExternalRecordVO> records = interpiService.findRecords(interpiSearchVO.isParty(), interpiSearchVO.getConditions(),
                interpiSearchVO.getCount(), interpiSearchVO.getSystemId());
        long end = System.currentTimeMillis();
        logger.debug("Nalezení " + records.size() + " záznamů, trvalo " + (end - start) + " ms.");

        return records;
    }

    /**
     * Načte vztahy daného záznamu.
     *
     * @param interpiRecordId id rejstříku v INTERPI
     * @param relationSearchVO vyhledávávací kriteria
     *
     * @return vztahy a jejich mapování
     */
	@Transactional
    @RequestMapping(value = "/interpi/{interpiRecordId}/relations", method = RequestMethod.POST)
    public InterpiMappingVO findInterpiRecordRelations(@PathVariable final String interpiRecordId, @RequestBody final RelationSearchVO relationSearchVO) {
        Assert.notNull(interpiRecordId, "Identifikátor systému interpi musí být vyplněn");
        Assert.notNull(relationSearchVO, "Struktura importu hesla musí být vyplněna");
        Assert.notNull(relationSearchVO.getScopeId(), "Identifikátor scope musí být vyplněn");
        Assert.notNull(relationSearchVO.getSystemId(), "Identifikátor systému musí být vyplněn");

        return interpiService.findInterpiRecordRelations(interpiRecordId, relationSearchVO.getSystemId(), relationSearchVO.getScopeId());
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
        final ApAccessPoint replaced = accessPointService.getAccessPoint(accessPointId);
        final ApAccessPoint replacement = accessPointService.getAccessPoint(replacedId);

        final ParParty replacedParty = partyService.findParPartyByAccessPoint(replaced);

        if (replacedParty != null) {
            final ParParty replacementParty = partyService.findParPartyByAccessPoint(replacement);
            if (replacementParty == null) {
                throw new BusinessException("Osobu lze nahradit pouze osobou.", BaseCode.INVALID_STATE);
            }
            partyService.replace(replacedParty, replacementParty);
        } else {
            accessPointService.replace(replaced, replacement);
        }
    }
}
