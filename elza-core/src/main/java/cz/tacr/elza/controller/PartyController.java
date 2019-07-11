package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.ParComplementTypeVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParRelationRoleTypeVO;
import cz.tacr.elza.controller.vo.ParRelationTypeVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.UIPartyGroupVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeComplementType;
import cz.tacr.elza.domain.ParPartyTypeRelation;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParRelationTypeRoleType;
import cz.tacr.elza.domain.UIPartyGroup;
import cz.tacr.elza.exception.DeleteException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRelationRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.UIPartyGroupRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.service.UserService;


/**
 * Kontrolér pro osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@RestController
@RequestMapping(value = "/api/party", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class PartyController {

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private PartyTypeRelationRepository partyTypeRelationRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private RelationTypeRoleTypeRepository relationTypeRoleTypeRepository;

    @Autowired
    private PartyTypeComplementTypeRepository partyTypeComplementTypeRepository;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private PartyService partyService;

    @Autowired
    private ValidationVOService validationVOService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UIPartyGroupRepository uiPartyGroupRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private ApFactory apFactory;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * Uložení nové osoby
     * @param partyVO data osoby
     * @return uložený objekt osoby
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @Transactional
    public ParPartyVO createParty(@RequestBody final ParPartyVO partyVO) {
        Assert.notNull(partyVO, "Osoba musí být vyplněna");

        if (partyVO.getId() != null) {
            throw new SystemException("Nová osoba nesmí mít nastaveno ID", BaseCode.ID_EXIST).set("id", partyVO.getId());
        }

        //CHECK
        if (partyVO.getAccessPoint() == null) {
            throw new SystemException("Není vyplněn přístupový bod", BaseCode.ID_NOT_EXIST);
        }
        ApState apState = validationVOService.checkAccessPoint(partyVO.getAccessPoint().getId());
        if (apState == null) {
            // zalozit novy
            apState = apFactory.create(partyVO.getAccessPoint());
        }
        validationVOService.checkParty(partyVO);

        ParParty party = factoryDO.createParty(partyVO, apState);

        ParParty savedParty = partyService.saveParty(party, apState);
        return factoryVo.createParPartyDetail(savedParty);
    }

    /**
     * Načte osobu podle id.
     * @param partyId id osoby
     * @return data osoby
     */
	@Transactional
    @RequestMapping(value = "/{partyId}", method = RequestMethod.GET)
    public ParPartyVO getParty(@PathVariable final Integer partyId) {
        Assert.notNull(partyId, "Identifikátor osoby musí být vyplněna");
        ParParty party = partyService.getParty(partyId);
        return factoryVo.createParPartyDetail(party);
    }

    /**
     * Aktualizace osoby.
     * @param partyId id osoby
     * @param partyVO data osoby
     * @return aktualizovaný objekt osoby
     */
    @RequestMapping(value = "/{partyId}", method = RequestMethod.PUT)
    @Transactional
    public ParPartyVO updateParty(@PathVariable final Integer partyId, @RequestBody final ParPartyVO partyVO) {

        Assert.notNull(partyId, "Identifikátor osoby musí být vyplněna");
        Assert.notNull(partyVO, "Osoba musí být vyplněna");

        Assert.isTrue(
                partyId.equals(partyVO.getId()),
                "V url požadavku je odkazováno na jiné ID (" + partyId + ") než ve VO (" + partyVO.getId() + ")."
        );

        ApState apState = validationVOService.checkPartyUpdate(partyVO);
        if (partyVO.getAccessPoint() != null) {
            if (partyVO.getAccessPoint().getId() != null) {
                ApAccessPoint accessPoint = accessPointService.getAccessPoint(partyVO.getAccessPoint().getId());
                apState = accessPointService.getState(accessPoint);
                if (apState.getDeleteChange() != null) {
                    throw new IllegalStateException("Zneplatněné osoby není možno upravovat");
                }
            } else {
                apState = apFactory.create(partyVO.getAccessPoint());
            }
        }

        ParParty party = factoryDO.createParty(partyVO, apState);

        ParParty savedParty = partyService.saveParty(party, apState);
        return factoryVo.createParPartyDetail(savedParty);
    }

    /**
     * Smazání osoby a navázaných entit.
     *
     * @param partyId id osoby
     */
    @Transactional
    @RequestMapping(value = "/{partyId}", method = RequestMethod.DELETE)
    public void deleteParty(@PathVariable final Integer partyId) {
        Assert.notNull(partyId, "Identifikátor osoby musí být vyplněna");

        ParParty party = partyRepository.getOneCheckExist(partyId);

        if (!userService.findUsersByParty(party).isEmpty()) {
            throw new DeleteException("Osobu nelze smazat, kvůli navázaným uživatelům", UserCode.USER_DELETE_ERROR).level(Level.WARNING);
        }

        partyService.deleteParty(party);
    }

    /**
     * Načte stránkovaný seznam osob.
     *
     * @param search      hledaný řetězec
     * @param from        počáteční záznam
     * @param count       počet vrácených záznamů
     * @param partyTypeId id typu osoby
     * @param versionId   id verze, podle které se budou filtrovat třídy rejstříků, null - výchozí třídy
     * @param scopeId     id scope, pokud je vyplněn vrací se jen osoby s tímto scope
     * @return seznam osob s počtem všech osob
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<ParPartyVO> findParty(@Nullable @RequestParam(required = false) final String search,
                                       @RequestParam final Integer from,
                                       @RequestParam final Integer count,
                                       @Nullable @RequestParam(required = false) final Integer partyTypeId,
                                       @Nullable @RequestParam(required = false) final Integer itemSpecId,
                                       @RequestParam(required = false) @Nullable final Integer versionId,
                                       @RequestParam(required = false) @Nullable final Integer scopeId) {

        ArrFund fund;
        if (versionId == null) {
            fund = null;
        } else {
            ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
            fund = version.getFund();
        }

        List<ParParty> partyList = partyService.findPartyByTextAndType(search, partyTypeId, itemSpecId, from, count,
                fund, scopeId);
        List<ParPartyVO> resultVo = factoryVo.createPartyList(partyList);

        long countAll = partyService.findPartyByTextAndTypeCount(search, partyTypeId, itemSpecId, fund, scopeId);
        return new FilteredResultVO<>(resultVo, countAll);
    }

    /**
     * Načte stránkovaný seznam osob.
     *
     * @param search      hledaný řetězec
     * @param from        počáteční záznam
     * @param count       počet vrácených záznamů
     * @param partyTypeId id typu osoby
     * @param partyId     id osoby, v jejíž scopeid budou filtrovány výsledky
     * @return seznam osob s počtem všech osob
     */
    @RequestMapping(value = "/findPartyForParty", method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<ParPartyVO> findPartyForParty(
            @Nullable @RequestParam(required = false) final String search,
            @RequestParam final Integer from,
            @RequestParam final Integer count,
            @Nullable @RequestParam(required = false) final Integer partyTypeId,
            @RequestParam final Integer partyId) {

        ParParty party = partyRepository.getOneCheckExist(partyId);
        ApState apState = accessPointService.getState(party.getAccessPoint());

        Set<Integer> scopeIds = new HashSet<>();
        scopeIds.add(apState.getScope().getScopeId());

        List<ParParty> partyList = partyRepository.findPartyByTextAndType(search, partyTypeId, null, from, count, scopeIds);

        List<ParPartyVO> resultVo = factoryVo.createPartyList(partyList);

        long countAll = partyRepository.findPartyByTextAndTypeCount(search, partyTypeId, null, scopeIds);
        return new FilteredResultVO<>(resultVo, countAll);
    }

    /**
     * Vložení vztahu spolu s vazbami.
     *
     * @param relationVO vztah s vazvami
     * @return vložený objekt
     */
    @Transactional
    @RequestMapping(value = "/relation", method = RequestMethod.POST)
    public ParRelationVO createRelation(@RequestBody final ParRelationVO relationVO) {

        Assert.isNull(relationVO.getId(), "Identifikátor vztahu musí být vyplněn");

        validationVOService.checkRelation(relationVO);


        ParRelation relation = factoryDO.createRelation(relationVO);
        List<ParRelationEntity> relationEntities = factoryDO.createRelationEntities(relationVO.getRelationEntities());
        ParRelation relationSaved = partyService.saveRelation(relation, relationEntities);


        return factoryVo.createRelation(relationSaved);
    }

    /**
     * Aktualizace vztahu s vazbami. Obsahuje stav, tzn. chybějící vazby budou smazány.
     *
     * @param relationId id vztahu
     * @param relationVO objekt vztahu s vazbami
     * @return aktualizovaný objekt vztahu
     */
    @Transactional
    @RequestMapping(value = "/relation/{relationId}", method = RequestMethod.PUT)
    public ParRelationVO updateRelation(
            @PathVariable(value = "relationId") final Integer relationId,
            @RequestBody final ParRelationVO relationVO) {


        relationVO.setId(relationId);
        validationVOService.checkRelation(relationVO);


        ParRelation relation = factoryDO.createRelation(relationVO);
        List<ParRelationEntity> relationEntities = factoryDO.createRelationEntities(relationVO.getRelationEntities());
        ParRelation relationSaved = partyService.saveRelation(relation, relationEntities);

        return factoryVo.createRelation(relationSaved);
    }

    /**
     * Provede smazání vztahu a jeho vazeb
     *
     * @param relationId id vztahu
     */
    @Transactional
    @RequestMapping(value = "/relation/{relationId}", method = RequestMethod.DELETE)
    public void deleteRelation(@PathVariable(value = "relationId") final Integer relationId) {

        ParRelation relation = relationRepository.findOne(relationId);
        if (relation != null) {
            partyService.deleteRelationAndSync(relation);
        }
    }

    /**
     * Vrátí všechny typy osob včetně podtypů.
     *
     * @return typy osob včetně navázaných podtypů
     */
    @RequestMapping(value = "/partyTypes", method = RequestMethod.GET)
	@Transactional
    public List<ParPartyTypeVO> getPartyTypes() {
        //načteme všechny záznamy, aby nedocházelo k samostatným dotazům v cyklech
        //noinspection unused
        List<ParRelationType> relationTypes = relationTypeRepository.findAll();

        Map<Integer, ParPartyTypeVO> partyTypeVoMap = new HashMap<>();

        for (ParPartyType partyType : partyTypeRepository.findAll()) {
            factoryVo.getOrCreateVo(partyType.getPartyTypeId(), partyType, partyTypeVoMap, ParPartyTypeVO.class);
        }

        //načtení ParRelationTypeVO
        //noinspection unused
        List<ParRelationRoleType> relationRoleTypes = relationRoleTypeRepository.findAll();

        //načtení ParRelationTypeVO
        Map<Integer, ParRelationTypeVO> relationTypeVoMap = new HashMap<>();
        for (ParPartyTypeRelation partyTypeRelation : partyTypeRelationRepository.findAllByOrderByPartyTypeAscViewOrderAsc()) {
            ParPartyType partyType = partyTypeRelation.getPartyType();
            ParPartyTypeVO partyTypeVO = factoryVo
                    .getOrCreateVo(partyType.getPartyTypeId(), partyType, partyTypeVoMap, ParPartyTypeVO.class);


            ParRelationType relationType = partyTypeRelation.getRelationType();
            ParRelationTypeVO relationTypeVO = factoryVo.createParRelationType(relationType, partyTypeRelation,
                    relationTypeVoMap);
            partyTypeVO.addRelationType(relationTypeVO);

            Map<Integer, ParRelationRoleTypeVO> relationRoleTypeVoMap = new HashMap<>();
            for (ParRelationTypeRoleType parRelationTypeRoleType : relationTypeRoleTypeRepository.findAll()) {

                if (!parRelationTypeRoleType.getRelationType().getRelationTypeId().equals(relationType.getRelationTypeId())) {
                    continue;
                }

                ParRelationRoleType relationRoleType = parRelationTypeRoleType.getRoleType();
                ParRelationRoleTypeVO relationRoleTypeVO = factoryVo
                        .getOrCreateVo(relationRoleType.getRoleTypeId(), relationRoleType, relationRoleTypeVoMap,
                                ParRelationRoleTypeVO.class);
                relationRoleTypeVO.setRepeatable(parRelationTypeRoleType.getRepeatable());
                relationTypeVO.addRelationRoleType(relationRoleTypeVO);
            }
        }

        //načtení ParComplementTypeVO
        //noinspection unused
        List<ParComplementType> complementTypes = complementTypeRepository.findAll();
        Map<Integer, ParComplementTypeVO> complementTypeVOMap = new HashMap<>();
        for (ParPartyTypeComplementType partyTypeComplementType : partyTypeComplementTypeRepository.findAll()) {
            ParPartyType partyType = partyTypeComplementType.getPartyType();
            ParPartyTypeVO partyTypeVO = factoryVo
                    .getOrCreateVo(partyType.getPartyTypeId(), partyType, partyTypeVoMap, ParPartyTypeVO.class);


            ParComplementType complementType = partyTypeComplementType.getComplementType();
            ParComplementTypeVO complementTypeVO = factoryVo
                    .getOrCreateVo(complementType.getComplementTypeId(), complementType, complementTypeVOMap,
                            ParComplementTypeVO.class);
            partyTypeVO.addComplementType(complementTypeVO);
        }


        //načtení ParPartyTypeVO
        StaticDataProvider staticData = staticDataService.getData();
        for (ApType apType : apTypeRepository.findTypesForPartyTypes()) {
            ParPartyType partyType = apType.getPartyType();
            ParPartyTypeVO partyTypeVO = factoryVo
                    .getOrCreateVo(partyType.getPartyTypeId(), partyType, partyTypeVoMap, ParPartyTypeVO.class);

            ApTypeVO apTypeVO = ApTypeVO.newInstance(apType, staticData);
            partyTypeVO.addApType(apTypeVO);
        }

        // načtení UIPartyGroup
        List<UIPartyGroup> uiPartyGroups = uiPartyGroupRepository.findAll();
        Map<Integer, UIPartyGroupVO> uiPartyGroupVOMap = new HashMap<>();
        Map<String, List<UIPartyGroupVO>> partyTypeCodeToUIPartyGroupsVOMap = new HashMap<>();
        for (UIPartyGroup uiPartyGroup : uiPartyGroups) {
            UIPartyGroupVO uiPartyGroupVO = factoryVo.getOrCreateVo(uiPartyGroup.getPartyGroupId(), uiPartyGroup,
                    uiPartyGroupVOMap, UIPartyGroupVO.class);

            ParPartyTypeVO partyTypeVO = uiPartyGroupVO.getPartyType();
            String partyTypeCode = null;
            if (partyTypeVO != null) {
                partyTypeCode = partyTypeVO.getCode();
            }

            List<UIPartyGroupVO> uiPartyGroupVOList = partyTypeCodeToUIPartyGroupsVOMap.get(partyTypeCode);
            if (uiPartyGroupVOList == null) {
                uiPartyGroupVOList = new LinkedList<>();
                partyTypeCodeToUIPartyGroupsVOMap.put(partyTypeCode, uiPartyGroupVOList);
            }
            uiPartyGroupVOList.add(uiPartyGroupVO);
        }

        for (ParPartyTypeVO partyTypeVO : partyTypeVoMap.values()) {
            ParPartyType partyType = partyTypeRepository.findOne(partyTypeVO.getId());
            List<ApType> partyApTypes = apTypeRepository.findByPartyTypeAndReadOnlyFalseOrderByName(partyType);

            partyTypeVO.setApTypes(apFactory.createTypesWithHierarchy(partyApTypes));

            List<UIPartyGroupVO> uiGroups = new LinkedList<>();
            List<UIPartyGroupVO> commonUIGroups = partyTypeCodeToUIPartyGroupsVOMap.get(null);
            List<UIPartyGroupVO> typeUIGroups = partyTypeCodeToUIPartyGroupsVOMap.get(partyTypeVO.getCode());
            if (commonUIGroups != null) {
                uiGroups.addAll(commonUIGroups);
            }
            if (typeUIGroups != null) {
                uiGroups.addAll(typeUIGroups);
            }
            if (!uiGroups.isEmpty()) {
                uiGroups.sort((g1, g2) -> {
                    if (g1.getViewOrder().equals(g2.getViewOrder())) {
                        return g1.getId().compareTo(g2.getId());
                    } else {
                        return g1.getViewOrder().compareTo(g2.getViewOrder());
                    }
                });
                partyTypeVO.setPartyGroups(uiGroups);
            }
        }


        return new ArrayList<>(partyTypeVoMap.values());
    }

    /**
     * Načte typy formy jména.
     *
     * @return seznam typů formy jména
     */
    @RequestMapping(value = "/partyNameFormTypes", method = RequestMethod.GET)
	@Transactional
    public List<ParPartyNameFormTypeVO> getPartyNameFormType() {
        List<ParPartyNameFormType> types = partyNameFormTypeRepository.findAll();

        return factoryVo.createPartyNameFormTypes(types);
    }

    /**
     * Načte seznam institucí
     * @return seznam institucí
     */
    @RequestMapping(value = "/institutions", method = RequestMethod.GET)
	@Transactional
    public List<ParInstitutionVO> getInstitutions() {
        //findAll()
        List<ParInstitution> instsFromDB = institutionRepository.findAllWithFetch();
        return factoryVo.createInstitutionList(instsFromDB);
    }

    /**
     * Najde použití osoby.
     *
     * @param partyId identifikátor osoby
     *
     * @return použití osoby
     */
    @RequestMapping(value = "/{partyId}/usage", method = RequestMethod.GET)
	@Transactional
    public RecordUsageVO findUsage(@PathVariable final Integer partyId) {
        ParParty parParty = partyRepository.getOneCheckExist(partyId);
        ApAccessPoint accessPoint = parParty.getAccessPoint();
        return accessPointService.findRecordUsage(accessPoint, parParty);
    }

    /**
     * Nahrazení osoby
     *
     * @param partyId ID nahrazované osoby
     * @param replacedId ID osoby pomocí které budeme nahrazovat
     */
    @Transactional
    @RequestMapping(value = "/{partyId}/replace", method = RequestMethod.POST)
    public void replace(@PathVariable final Integer partyId, @RequestBody final Integer replacedId) {
        final ParParty replaced = partyService.getParty(partyId);
        final ParParty replacement = partyService.getParty(replacedId);
        partyService.replace(replaced, replacement);
    }
}
