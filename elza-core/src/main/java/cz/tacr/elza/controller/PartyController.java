package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import cz.tacr.elza.controller.vo.ParComplementTypeVO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPartyWithCount;
import cz.tacr.elza.controller.vo.ParRelationRoleTypeVO;
import cz.tacr.elza.controller.vo.ParRelationTypeVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.domain.ParComplementType;
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
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRelationRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.service.PartyService;


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
     * @return seznam osob s počtem všech osob
     */
    @RequestMapping(value = "/findParty", method = RequestMethod.GET)
    public ParPartyWithCount findParty(@Nullable @RequestParam(value = "search", required = false) final String search,
                                       @RequestParam("from") final Integer from,
                                       @RequestParam("count") final Integer count,
                                       @Nullable @RequestParam(value = "partyTypeId", required = false) final Integer partyTypeId) {

        boolean onlyLocal = false;

        List<ParParty> partyList = partyRepository
                .findPartyByTextAndType(search, partyTypeId, from, count, onlyLocal);
        List<ParPartyVO> resultVo = factoryVo.createPartyList(partyList);

        long countAll = partyRepository.findPartyByTextAndTypeCount(search, partyTypeId, onlyLocal);
        return new ParPartyWithCount(resultVo, countAll);
    }

    @RequestMapping(value = "/insertParty", method = RequestMethod.POST)
    @Transactional
    public ParPartyVO insertParty(@RequestBody final ParPartyEditVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        //CHECK
        validationVOService.checkParty(partyVO);

        //TYPES
        ParPartyType partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        // hledám typ rejstříku, který má přiřazen zde použitý typ osoby
        RegRegisterType registerType = registerTypeRepository.findRegisterTypeByPartyType(partyType).get(0);

        //PARTY
        ParParty party = partyService.createParty(partyVO, partyType, registerType);

        List<ParPartyVO> partyList = factoryVo.createPartyList(Arrays.asList(party));
        return partyList.get(0);
    }

    @RequestMapping(value = "/updateParty/{partyId}", method = RequestMethod.PUT)
    @Transactional
    public ParPartyVO updateParty(
            @PathVariable(value = "partyId") final Integer partyId,
            @RequestBody final ParPartyEditVO partyVO) {

        if (partyVO == null) {
            return null;
        }

        //CHECK
        Assert.isTrue(partyVO.getPartyId().equals(partyId), "V url požadavku je odkazováno na jiné ID (" + partyId
                + ") než ve VO (" + partyVO.getPartyId() + ").");
        validationVOService.checkPartyUpdate(partyVO);

        //TYPES
        ParPartyType partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        // hledám typ rejstříku, který má přiřazen zde použitý typ osoby
        RegRegisterType registerType = registerTypeRepository.findRegisterTypeByPartyType(partyType).get(0);

        //PARTY
        ParParty party = partyService.updateParty(partyVO, partyType);

        List<ParPartyVO> partyList = factoryVo.createPartyList(Arrays.asList(party));
        return partyList.get(0);
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
