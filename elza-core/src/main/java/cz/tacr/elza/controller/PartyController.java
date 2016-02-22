package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.PartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.*;


/**
 * Kontrolér pro osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@RestController
@RequestMapping("/api/partyManagerV2")
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
    private RegisterTypeRepository registerTypeRepository;

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
    private FindingAidVersionRepository findingAidVersionRepository;

    /**
     * Načte osobu podle id.
     * @param partyId id osoby
     * @return data osoby
     */
    @RequestMapping(value = "/getParty", method = RequestMethod.GET)
    public ParPartyVO getParty(@RequestParam("partyId") final Integer partyId) {
        Assert.notNull(partyId);

        ParParty party = partyRepository.findOne(partyId);
        return factoryVo.createParPartyDetail(party);
    }

    /**
     * Vrátí všechny typy osob včetně podtypů.
     *
     * @return typy osob včetně navázaných podtypů
     */
    @RequestMapping(value = "/getPartyTypes", method = RequestMethod.GET)
    public List<ParPartyTypeVO> getPartyTypes() {
        //načteme všechny záznamy, aby nedocházelo k samostatným dotazům v cyklech
        List<ParRelationType> relationTypes = relationTypeRepository.findAll();

        Map<Integer, ParPartyTypeVO> partyTypeVoMap = new HashMap<>();

        for (ParPartyType partyType : partyTypeRepository.findAll()) {
            factoryVo.getOrCreateVo(partyType.getPartyTypeId(), partyType, partyTypeVoMap, ParPartyTypeVO.class);
        }


        //načtení ParRelationTypeVO
        Map<Integer, ParRelationTypeVO> relationTypeVoMap = new HashMap<>();
        for (ParPartyTypeRelation partyTypeRelation : partyTypeRelationRepository.findAll()) {
            ParPartyType partyType = partyTypeRelation.getPartyType();
            ParPartyTypeVO partyTypeVO = factoryVo
                    .getOrCreateVo(partyType.getPartyTypeId(), partyType, partyTypeVoMap, ParPartyTypeVO.class);


            ParRelationType relationType = partyTypeRelation.getRelationType();
            ParRelationTypeVO relationTypeVO = factoryVo
                    .getOrCreateVo(relationType.getRelationTypeId(), relationType, relationTypeVoMap,
                            ParRelationTypeVO.class);

            partyTypeVO.addRelationType(relationTypeVO);
        }


        //načtení ParRelationTypeVO
        List<ParRelationRoleType> relationRoleTypes = relationRoleTypeRepository.findAll();
        Map<Integer, ParRelationRoleTypeVO> relationRoleTypeVoMap = new HashMap<>();
        for (ParRelationTypeRoleType parRelationTypeRoleType : relationTypeRoleTypeRepository.findAll()) {

            ParRelationType relationType = parRelationTypeRoleType.getRelationType();
            ParRelationTypeVO relationTypeVO = factoryVo
                    .getOrCreateVo(relationType.getRelationTypeId(), relationType, relationTypeVoMap,
                            ParRelationTypeVO.class);


            ParRelationRoleType relationRoleType = parRelationTypeRoleType.getRoleType();
            ParRelationRoleTypeVO relationRoleTypeVO = factoryVo
                    .getOrCreateVo(relationRoleType.getRoleTypeId(), relationRoleType, relationRoleTypeVoMap,
                            ParRelationRoleTypeVO.class);
            relationTypeVO.addRelationRoleType(relationRoleTypeVO);
        }

        //načtení ParComplementTypeVO
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
        for (RegRegisterType registerType : registerTypeRepository.findTypesForPartyTypes()) {
            ParPartyType partyType = registerType.getPartyType();
            ParPartyTypeVO partyTypeVO = factoryVo
                    .getOrCreateVo(partyType.getPartyTypeId(), partyType, partyTypeVoMap, ParPartyTypeVO.class);

            RegRegisterTypeVO regRegisterTypeVO = factoryVo.createRegRegisterType(registerType);
            partyTypeVO.addRegisterType(regRegisterTypeVO);
        }


        for (ParPartyTypeVO partyTypeVO : partyTypeVoMap.values()) {
            ParPartyType partyType = partyTypeRepository.findOne(partyTypeVO.getPartyTypeId());
            List<RegRegisterType> partyRegisterTypes = registerTypeRepository
                    .findByPartyTypeEnableAdding(partyType);

            partyTypeVO.setRegisterTypes(factoryVo.createRegisterTypesTree(partyRegisterTypes, true, partyType));
        }


        return new ArrayList<>(partyTypeVoMap.values());
    }

    /**
     * Načte typy formy jména.
     *
     * @return seznam typů formy jména
     */
    @RequestMapping(value = "/getPartyNameFormTypes", method = RequestMethod.GET)
    public List<ParPartyNameFormTypeVO> getPartyNameFormType() {
        List<ParPartyNameFormType> types = partyNameFormTypeRepository.findAll();

        return factoryVo.createPartyNameFormTypes(types);
    }


    /**
     * Načte stránkovaný seznam osob.
     *
     * @param search      hledaný řetězec
     * @param from        počáteční záznam
     * @param count       počet vrácených záznamů
     * @param partyTypeId id typu osoby
     * @param versionId   id verze, podle které se budou filtrovat třídy rejstříků, null - výchozí třídy
     * @return seznam osob s počtem všech osob
     */
    @RequestMapping(value = "/findParty", method = RequestMethod.GET)
    public ParPartyWithCount findParty(@Nullable @RequestParam(value = "search", required = false) final String search,
                                       @RequestParam("from") final Integer from,
                                       @RequestParam("count") final Integer count,
                                       @Nullable @RequestParam(value = "partyTypeId", required = false) final Integer partyTypeId,
                                       @RequestParam(required = false) @Nullable final Integer versionId) {

        ArrFindingAid findingAid;
        if(versionId == null){
            findingAid = null;
        }else{
            ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
            Assert.notNull(version, "Nebyla nalezena verze archivní pomůcky s id "+versionId);
            findingAid = version.getFindingAid();
        }


        List<ParParty> partyList = partyService.findPartyByTextAndType(search, partyTypeId, from, count,
                findingAid);
        List<ParPartyVO> resultVo = factoryVo.createPartyList(partyList);

        long countAll = partyService.findPartyByTextAndTypeCount(search, partyTypeId, findingAid);
        return new ParPartyWithCount(resultVo, countAll);
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
    public ParPartyWithCount findPartyForParty(
            @Nullable @RequestParam(value = "search", required = false) final String search,
            @RequestParam("from") final Integer from,
            @RequestParam("count") final Integer count,
            @Nullable @RequestParam(value = "partyTypeId", required = false) final Integer partyTypeId,
            @RequestParam(value = "partyId", required = true) final Integer partyId) {

        ParParty party = partyRepository.getOne(partyId);
        Assert.notNull(party, "Nebyla nalezena osoba s id " + partyId);
        Set<Integer> scopeIds = new HashSet<>();
        scopeIds.add(party.getRecord().getScope().getScopeId());


        List<ParParty> partyList = partyRepository.findPartyByTextAndType(search, partyTypeId, from, count, scopeIds);

        List<ParPartyVO> resultVo = factoryVo.createPartyList(partyList);

        long countAll = partyRepository.findPartyByTextAndTypeCount(search, partyTypeId, scopeIds);
        return new ParPartyWithCount(resultVo, countAll);
    }


    /**
     * Uložení nové osoby
     * @param partyVO data osoby
     * @return uložený objekt osoby
     */
    @RequestMapping(value = "/insertParty", method = RequestMethod.POST)
    @Transactional
    public ParPartyVO insertParty(@RequestBody final ParPartyVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        if(partyVO.getPartyId() != null){
            throw new IllegalArgumentException("Nová osoba nesmí mít nastaveno ID");
        }

        //CHECK
        validationVOService.checkParty(partyVO);

        ParParty party = factoryDO.createParty(partyVO);

        ParParty savedParty = partyService.saveParty(party);
        return factoryVo.createParPartyDetail(savedParty);
    }

    /**
     * Aktualizace osoby.
     * @param partyId id osoby
     * @param partyVO data osoby
     * @return aktualizovaný objekt osoby
     */
    @RequestMapping(value = "/updateParty/{partyId}", method = RequestMethod.PUT)
    @Transactional
    public ParPartyVO updateParty(
            @PathVariable(value = "partyId") final Integer partyId,
            @RequestBody final ParPartyVO partyVO) {

        if (partyVO == null) {
            return null;
        }

        //CHECK
        Assert.isTrue(partyVO.getPartyId().equals(partyId), "V url požadavku je odkazováno na jiné ID (" + partyId
                + ") než ve VO (" + partyVO.getPartyId() + ").");
        validationVOService.checkPartyUpdate(partyVO);

        ParParty party = factoryDO.createParty(partyVO);

        ParParty savedParty = partyService.saveParty(party);
        return factoryVo.createParPartyDetail(savedParty);
    }

    /**
     * Smazání osoby a navázaných entit.
     *
     * @param partyId id osoby
     */
    @Transactional
    @RequestMapping(value = "/deleteParty", method = RequestMethod.DELETE)
    public void deleteParty(@RequestParam("partyId") final Integer partyId) {
        Assert.notNull(partyId);
        ParParty party = partyRepository.findOne(partyId);
        if (party == null) {
            return;
        }

        partyService.deleteParty(party);
    }


    /**
     * Vložení vztahu spolu s vazbami.
     *
     * @param relationVO vztah s vazvami
     * @return vložený objekt
     */
    @Transactional
    @RequestMapping(value = "/relations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ParRelationVO insertRelation(@RequestBody final ParRelationVO relationVO) {

        Assert.isNull(relationVO.getRelationId());

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
    @RequestMapping(value = "/relations/{relationId}", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ParRelationVO updateRelation(
            @PathVariable(value = "relationId") final Integer relationId,
            @RequestBody final ParRelationVO relationVO) {


        relationVO.setRelationId(relationId);
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
    @RequestMapping(value = "/relations/{relationId}", method = RequestMethod.DELETE)
    public void deleteRelation(@PathVariable(value = "relationId") final Integer relationId) {

        ParRelation relation = relationRepository.findOne(relationId);
        if (relation != null) {
            partyService.deleteRelation(relation);
        }
    }


}
