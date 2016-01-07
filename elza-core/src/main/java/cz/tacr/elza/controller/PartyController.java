package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ParComplementTypeVO;
import cz.tacr.elza.controller.vo.ParDynastyEditVO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPartyWithCount;
import cz.tacr.elza.controller.vo.ParRelationRoleTypeVO;
import cz.tacr.elza.controller.vo.ParRelationTypeVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeComplementType;
import cz.tacr.elza.domain.ParPartyTypeRelation;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParRelationTypeRoleType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRelationRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
    private PartyNameRepository partyNameRepository;

    @Autowired
    private UnitdateRepository unitdateRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;


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

        // TODO kuzel check na min. vyplneni entit


        //CHECK & types
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

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        RegRegisterType registerType = regRegisterTypes.get(0);

        boolean isPreferred = false;
        for (final ParPartyNameEditVO partyName : partyVO.getPartyNames()) {
            if (partyName.isPreferredName()) {
                isPreferred = true;
            }
        }
        if (!isPreferred) {
            throw new IllegalArgumentException("Není přítomno žádné preferované jméno osoby.");
        }
        // end CHECK

        ParParty party = factoryDO.createParty(partyVO);
        party.setPartyType(partyType);

        // Record
        // hledám typ rejstříku, který má přiřazen zde použitý typ osoby
        RegRecord regRecord = genRegRecordsFromPartyNames(partyVO.getPartyNames(), registerType);
        party.setRecord(regRecord);

        // Party
        partyRepository.save(party);

        // Names
        ParPartyName preferredName = null;
        for (final ParPartyNameEditVO partyNameVO : partyVO.getPartyNames()) {
            ParPartyNameFormType nameFormType = partyNameFormTypeRepository.getOne(partyNameVO.getNameFormTypeId());

            ParPartyName partyName = factoryDO.createParPartyName(partyNameVO);
            partyName.setNameFormType(nameFormType);
            partyName.setParty(party);

            partyNameRepository.save(partyName);

            if (partyNameVO.isPreferredName()) {
                preferredName = partyName;
            }
        }

        Assert.notNull(preferredName);
        party.setPreferredName(preferredName);
        partyRepository.save(party);

//        List<ParPartyVO> partyList = factoryVo.createPartyList(Arrays.asList(party));
//        return partyList.get(0);
        return null;
    }

    /**
     * Nageneruje rejstříkové heslo dle preferovaného jména osoby. Ostatní jména jako variantní hesla k tomuto.
     * @param partyNamesVO    jména osob
     * @param registerType    typ rejstříku
     * @return      rejstříkoví heslo s variantními hesly daného typu
     */
    private RegRecord genRegRecordsFromPartyNames(final List<ParPartyNameEditVO> partyNamesVO, final RegRegisterType registerType) {
        if (partyNamesVO == null) {
            return null;
        }

        RegRecord result = null;
        for (final ParPartyNameEditVO pn : partyNamesVO) {
            if (pn.isPreferredName()) {
                RegRecord regRecord = new RegRecord();
                regRecord.setRegisterType(registerType);
                regRecord.setRecord(pn.getMainPart() + StringUtils.defaultString(pn.getOtherPart()));
                regRecord.setLocal(true);

                recordRepository.save(regRecord);
                result = regRecord;
            }
        }

        for (final ParPartyNameEditVO pn : partyNamesVO) {
            if (!pn.isPreferredName()) {
                RegVariantRecord regVariantRecord = new RegVariantRecord();
                regVariantRecord.setRegRecord(result);
                regVariantRecord.setRecord(pn.getMainPart() + StringUtils.defaultString(pn.getOtherPart()));

                variantRecordRepository.save(regVariantRecord);
            }
        }

        return result;
    }
}
