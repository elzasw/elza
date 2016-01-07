package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ParComplementTypeVO;
import cz.tacr.elza.controller.vo.ParDynastyEditVO;
import cz.tacr.elza.controller.vo.ParEventEditVO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyGroupEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPartyWithCount;
import cz.tacr.elza.controller.vo.ParPersonEditVO;
import cz.tacr.elza.controller.vo.ParRelationRoleTypeVO;
import cz.tacr.elza.controller.vo.ParRelationTypeVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeComplementType;
import cz.tacr.elza.domain.ParPartyTypeRelation;
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
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.service.PartyService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    private PartyService partyService;



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

    @RequestMapping(value = "/insertParty", method = RequestMethod.PUT)
    @Transactional
    public ParPartyVO insertParty(@RequestBody final ParPartyEditVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        //CHECK
        chekParty(partyVO);

        //TYPES
        ParPartyType partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        // hledám typ rejstříku, který má přiřazen zde použitý typ osoby
        RegRegisterType registerType = registerTypeRepository.findRegisterTypeByPartyType(partyType).get(0);

        //PARTY
        ParParty party = partyService.createParty(partyVO, partyType, registerType);

        List<ParPartyVO> partyList = factoryVo.createPartyList(Arrays.asList(party));
        return partyList.get(0);
    }

    @RequestMapping(value = "/updateParty", method = RequestMethod.PUT)
    @Transactional
    public ParPartyVO updateParty(@RequestBody final ParPartyEditVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        //CHECK
        chekPartyUpdate(partyVO);

        //TYPES
        ParPartyType partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        // hledám typ rejstříku, který má přiřazen zde použitý typ osoby
        RegRegisterType registerType = registerTypeRepository.findRegisterTypeByPartyType(partyType).get(0);

        //PARTY
        ParParty party = partyService.updateParty(partyVO, partyType, registerType);

        List<ParPartyVO> partyList = factoryVo.createPartyList(Arrays.asList(party));
        return partyList.get(0);
    }

    private void chekParty(final ParPartyEditVO partyVO) {
        ParPartyType partyType;
        if (partyVO.getPartyTypeId() != null) {
            partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        } else {
            throw new IllegalArgumentException("Nenalezen typ osoby s id: " + partyVO.getPartyTypeId());
        }

        // object type dle party type ?
        if (partyVO instanceof ParDynastyEditVO
                && !ParPartyType.PartyTypeEnum.DYNASTY.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParPersonEditVO
                && !ParPartyType.PartyTypeEnum.PERSON.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParEventEditVO
                && !ParPartyType.PartyTypeEnum.EVENT.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParPartyGroupEditVO
                && !ParPartyType.PartyTypeEnum.PARTY_GROUP.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        if (partyVO.getPartyNames() != null) {
            checkPreferredNameExist(partyVO.getPartyNames());
        } else {
            throw new IllegalArgumentException("Je povinné alespoň jedno preferované jméno.");
        }
        // end CHECK
    }

    private void chekPartyUpdate(final ParPartyEditVO partyVO) {
        ParPartyType partyType;
        if (partyVO.getPartyTypeId() != null) {
            partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        } else {
            throw new IllegalArgumentException("Nenalezen typ osoby s id: " + partyVO.getPartyTypeId());
        }

        if (partyVO.getPartyId() == null) {
            throw new IllegalArgumentException("Není vyplněno id existující entity pro update.");
        }

        ParParty parParty = partyRepository.getOne(partyVO.getPartyId());
        if (!parParty.getPartyType().getPartyTypeId().equals(partyVO.getPartyTypeId())) {
            throw new IllegalArgumentException("Nelze měnit typ osoby.");
        }

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        if (partyVO.getPartyNames() != null) {
            checkPreferredNameExist(partyVO.getPartyNames());
        }
    }

    private void checkPreferredNameExist(final List<ParPartyNameEditVO> partyNameEditVOs) {
        boolean isPreferred = false;
        for (final ParPartyNameEditVO partyName : partyNameEditVOs) {
            if (partyName.isPreferredName()) {
                isPreferred = true;
            }
        }
        if (!isPreferred) {
            throw new IllegalArgumentException("Není přítomno žádné preferované jméno osoby.");
        }
    }

}
