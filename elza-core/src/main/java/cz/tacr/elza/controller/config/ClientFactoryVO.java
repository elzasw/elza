package cz.tacr.elza.controller.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaRules;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.ParPartyGroupIdentifierVO;
import cz.tacr.elza.controller.vo.ParPartyGroupVO;
import cz.tacr.elza.controller.vo.ParPartyNameComplementVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyTimeRangeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegRecordParentVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyTimeRange;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTimeRangeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;


/**
 * Tovární třída pro vytváření VO objektů a jejich seznamů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Service
public class ClientFactoryVO {

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory mapperFactory;

    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;

    @Autowired
    private PartyTimeRangeRepository partyTimeRangeRepository;

    @Autowired
    private PartyNameRepository partyNameRepository;

    @Autowired
    private PartyGroupIdentifierRepository partyGroupIdentifierRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationEntityRepository relationEntityRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UnitdateRepository unitdateRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private ElzaRules elzaRules;

    /**
     * Vytvoří detailní objekt osoby. Načte všechna navázaná data.
     *
     * @param party osoba
     * @return detail VO osoby
     */
    public ParPartyVO createParPartyDetail(final ParParty party) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ParPartyVO result = mapper.map(party, ParPartyVO.class);

        //donačtení group party identifiers
        if (party instanceof ParPartyGroup) {
            List<ParPartyGroupIdentifier> groupIdentifiers = partyGroupIdentifierRepository.findByPartyGroup(
                    (ParPartyGroup) party);
            ParPartyGroupVO groupResult = (ParPartyGroupVO) result;
            groupResult.setGroupIdentifiers(createList(groupIdentifiers, ParPartyGroupIdentifierVO.class, null));
        }


        //partyPreferredName
        if (party.getPreferredName() != null) {
            result.setPreferredName(createParPartyNameDetail(party.getPreferredName()));
        }
        //partyNames
        result.setPartyNames(createList(partyNameRepository.findByParty(party), ParPartyNameVO.class, (n) ->
                        createParPartyNameDetail(n)
        ));

        //partyNames
        result.setPartyNames(createList(partyNameRepository.findByParty(party), ParPartyNameVO.class,
                this::createParPartyNameDetail));

        //partyTimeRange
        result.setTimeRanges(createList(partyTimeRangeRepository.findByParty(party), ParPartyTimeRangeVO.class, null));

        result.setRelations(createPartyRelations(party));
        result.setCreators(createPartyList(partyRepository.findCreatorsByParty(party)));

        return result;
    }

    /**
     * Vytvoří seznam objektů osob. Pro osoby nejsou načítány všechna detailní data.
     *
     * @param parties seznam osob
     * @return seznam VO osob
     */
    public List<ParPartyVO> createPartyList(final List<ParParty> parties) {
        if (CollectionUtils.isEmpty(parties)) {
            return Collections.EMPTY_LIST;
        }

        //načtení dat do session
        unitdateRepository.findForFromTimeRangeByParties(parties);
        unitdateRepository.findForToTimeRangeByParties(parties);
        unitdateRepository.findForFromPartyNameByParties(parties);
        unitdateRepository.findForToPartyNameByParties(parties);
        recordRepository.findByParties(parties);

        MapperFacade mapper = mapperFactory.getMapperFacade();
        Map<Integer, ParPartyVO> partyMap = new HashMap<>();

        for (final ParParty party : parties) {
            ParPartyVO partyVO = mapper.map(party, ParPartyVO.class);
            if (party.getPreferredName() != null) {
                partyVO.setPreferredName(mapper.map(party.getPreferredName(), ParPartyNameVO.class));
            }
            partyVO.setRecord(mapper.map(party.getRecord(), RegRecordVO.class));
            partyMap.put(partyVO.getPartyId(), partyVO);
        }

        for (final ParPartyTimeRange partyTimeRange : partyTimeRangeRepository.findByParties(parties)) {
            ParPartyTimeRangeVO partyTimeRangeVO = mapper.map(partyTimeRange, ParPartyTimeRangeVO.class);
            partyMap.get(partyTimeRangeVO.getPartyId()).addPartyTimeRange(partyTimeRangeVO);
        }

        for (final ParPartyName partyName : partyNameRepository.findByParties(parties)) {
            ParPartyNameVO partyNameVO = mapper.map(partyName, ParPartyNameVO.class);
            partyMap.get(partyNameVO.getPartyId()).addPartyName(partyNameVO);
        }

        return new ArrayList<>(partyMap.values());
    }


    /**
     * Vytvoří objekt jména osoby. Jsou načteny i detailní informace.
     *
     * @param partyName jméno osoby
     * @return vo jména osoba
     */
    private ParPartyNameVO createParPartyNameDetail(final ParPartyName partyName) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ParPartyNameVO result = mapper.map(partyName, ParPartyNameVO.class);

        List<ParPartyNameComplement> nameComplements = partyNameComplementRepository.findByPartyName(partyName);
        result.setPartyNameComplements(createList(nameComplements, ParPartyNameComplementVO.class, null));

        return result;
    }

    /**
     * Vytvoří seznam vazeb osoby.
     *
     * @param party osoba
     * @return seznam vazeb osoby
     */
    public List<ParRelationVO> createPartyRelations(final ParParty party) {
        List<ParRelation> relations = relationRepository.findByParty(party);
        if (CollectionUtils.isEmpty(relations)) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();

        Map<Integer, ParRelationVO> relationVOMap = new HashMap<>();
        for (final ParRelation relation : relations) {
            relationVOMap.put(relation.getRelationId(), mapper.map(relation, ParRelationVO.class));
        }

        //načtení objektů regrecord do session
        recordRepository.findByPartyRelations(party);
        List<ParRelationEntity> partyRelations = relationEntityRepository.findByParty(party);
        List<ParRelationEntityVO> partyRelationsVo = createList(partyRelations, ParRelationEntityVO.class, null);

        for (final ParRelationEntityVO parRelationEntityVO : partyRelationsVo) {
            relationVOMap.get(parRelationEntityVO.getRelationId()).addRelationEntity(parRelationEntityVO);
        }

        return new ArrayList<>(relationVOMap.values());
    }


    /**
     * Vytvoření seznamu RegRecordVo.
     *
     * @param records            seznam rejstříkových hesel
     * @param recordIdPartyIdMap mapa id rejstříkových hesel na osobu
     * @param fillParents        příznak zda se mají načíst rodiče rejstříku
     * @return seznam rejstříkových hesel
     */
    public List<RegRecordVO> createRegRecords(final List<RegRecord> records,
                                              final Map<Integer, Integer> recordIdPartyIdMap, boolean fillParents) {
        List<RegRecordVO> result = new ArrayList<>(records.size());
        for (final RegRecord record : records) {
            Integer partyId = recordIdPartyIdMap.get(record.getRecordId());
            result.add(createRegRecord(record, partyId, fillParents));
        }

        return result;
    }

    /**
     * Vytvoří rejstříkové heslo.
     *
     * @param regRecord   rejstříkové heslo
     * @param partyId     id osoby
     * @param fillParents příznak zda se mají načíst rodiče rejstříku
     * @return rejstříkové heslo
     */
    public RegRecordVO createRegRecord(final RegRecord regRecord,
                                       @Nullable final Integer partyId,
                                       boolean fillParents) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RegRecordVO result = mapper.map(regRecord, RegRecordVO.class);
        result.setPartyId(partyId);

        if (fillParents) {
            List<RegRecordParentVO> parents = new LinkedList<>();
            RegRecord parentRecord = regRecord.getParentRecord();
            while (parentRecord != null) {
                parents.add(createRegRecordParent(parentRecord));
                parentRecord = parentRecord.getParentRecord();
            }
            result.setParents(parents);
        }

        return result;
    }

    private RegRecordParentVO createRegRecordParent(RegRecord parentRecord) {
        RegRecordParentVO parent = new RegRecordParentVO();

        parent.setId(parentRecord.getRecordId());
        parent.setRecord(parentRecord.getRecord());

        return parent;
    }

    /**
     * Vytvoření variantního rejstříkového hesla.
     *
     * @param regVariantRecord variantní rejstříkové heslo
     * @return VO variantní rejstříkové heslo
     */
    public RegVariantRecordVO createRegVariantRecord(final RegVariantRecord regVariantRecord) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regVariantRecord, RegVariantRecordVO.class);
    }

    /**
     * Vytvoření seznamu variantních rejstříkových hesel.
     *
     * @param variantRecords seznam variantních rejstříkových hesel
     * @return seznam VO variantních hesel
     */
    public List<RegVariantRecordVO> createRegVariantRecords(@Nullable final List<RegVariantRecord> variantRecords) {
        if (variantRecords == null) {
            return null;
        }

        List<RegVariantRecordVO> result = new ArrayList<>(variantRecords.size());
        variantRecords.forEach((variantRecord) ->
                        result.add(createRegVariantRecord(variantRecord))
        );

        return result;
    }

    /**
     * Vytvoří ty rejstříkového hesla.
     *
     * @param registerType typ rejstříkového hesla
     * @return VO
     */
    public RegRegisterTypeVO createRegRegisterType(final RegRegisterType registerType) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(registerType, RegRegisterTypeVO.class);
    }


    /**
     * Vytvoří seznam typů formy jména.
     *
     * @param types typy formy jména
     * @return seznam VO typů
     */
    public List<ParPartyNameFormTypeVO> createPartyNameFormTypes(final Collection<ParPartyNameFormType> types) {
        Assert.notNull(types);
        MapperFacade mapper = mapperFactory.getMapperFacade();

        List<ParPartyNameFormTypeVO> result = new LinkedList<>();

        for (final ParPartyNameFormType type : types) {
            result.add(mapper.map(type, ParPartyNameFormTypeVO.class));
        }

        return result;
    }

    /**
     * Vytváří stromovou strukturu pro všechny typy rejstříků.
     *
     * @param allTypes všechny typy rejstříků
     * @return seznam kořenových typů rejstříků
     */
    public List<RegRegisterTypeVO> createRegisterTypesTree(final List<RegRegisterType> allTypes) {
        if (CollectionUtils.isEmpty(allTypes)) {
            return Collections.EMPTY_LIST;
        }

        Map<Integer, RegRegisterTypeVO> typeMap = new HashMap<>();
        List<RegRegisterTypeVO> roots = new LinkedList<>();
        for (final RegRegisterType registerType : allTypes) {
            RegRegisterTypeVO registerTypeVO = createRegisterTypeTree(registerType, typeMap);
            if (registerTypeVO.getParentRegisterTypeId() == null) {
                roots.add(registerTypeVO);
            }
        }

        return roots;
    }

    /**
     * Vytvoří typ rejstříkového hesla a vloží jeje do mapy všech hesel.
     *
     * @param registerType typ hesla
     * @param typeMap      mapa všech typů (id typu ->typ)
     * @return typ rejstříkového hesla
     */
    private RegRegisterTypeVO createRegisterTypeTree(final RegRegisterType registerType,
                                                     final Map<Integer, RegRegisterTypeVO> typeMap) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        RegRegisterTypeVO result = typeMap.get(registerType.getRegisterTypeId());
        if (result != null) {
            return result;
        }

        result = mapper.map(registerType, RegRegisterTypeVO.class);
        typeMap.put(result.getId(), result);
        if (registerType.getParentRegisterType() != null) {
            RegRegisterTypeVO parent = createRegisterTypeTree(registerType.getParentRegisterType(), typeMap);
            parent.addChild(result);
        }

        return result;
    }

    /**
     * Vytvoří seznam VO objektů z objektů.
     *
     * @param items   seznam objektů
     * @param voTypes typ cílových objektů
     * @param factory metoda pro vytvoření VO objektu. Pokud je null, je použita výchozí tovární třída.
     * @param <VO>    typ VO objektu
     * @param <ITEM>  Typ objektu
     * @return seznam VO objektů
     */
    private <VO, ITEM> List<VO> createList(final List<ITEM> items,
                                           final Class<VO> voTypes,
                                           @Nullable final Function<ITEM, VO> factory) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.EMPTY_LIST;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();
        List<VO> result = new ArrayList<>(items.size());
        if (factory == null) {
            for (final ITEM item : items) {
                result.add(mapper.map(item, voTypes));
            }
        } else {
            for (final ITEM item : items) {
                result.add(factory.apply(item));
            }
        }

        return result;
    }

    /**
     * Najde v mapě objekt podle daného id. Pokud není objekt nalezen, přes faktory jej vytvoří a vloží do mapy.
     *
     * @param id                id objektu
     * @param source            zdrojový objekt
     * @param processedItemsMap mapa vytvořených objektů
     * @param classType         typ VO objektu
     * @param <VO>              typ VO objektu
     * @param <VOTYPE>          třída VO objektu
     * @return nalezený nebo vytvořený VO
     */
    public <VO, VOTYPE extends Class> VO getOrCreateVo(final Integer id,
                                                       final Object source,
                                                       final Map<Integer, VO> processedItemsMap,
                                                       final VOTYPE classType) {
        VO item = processedItemsMap.get(id);


        if (item == null) {
            item = (VO) mapperFactory.getMapperFacade().map(source, classType);
            processedItemsMap.put(id, item);
        }
        return item;
    }

    /**
     * Vytvoří typ výstupu.
     *
     * @param arrType typ výstupu
     * @return VO typ výstupu
     */
    public RulArrangementTypeVO createArrangementType(RulArrangementType arrType) {
        Assert.notNull(arrType);

        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulArrangementTypeVO rulArrangementTypeVO = mapper.map(arrType, RulArrangementTypeVO.class);
        rulArrangementTypeVO.setRuleSetId(arrType.getRuleSet().getRuleSetId());

        return rulArrangementTypeVO;
    }

    /**
     * Vytvoření ArrFindingAid a načtení verzí.
     *
     * @param findingAid      DO
     * @param includeVersions true - budou do objektu donačteny všechny jeho verze, false- verze nebudou donačteny
     * @return VO
     */
    public ArrFindingAidVO createArrFindingAidVO(final ArrFindingAid findingAid, final boolean includeVersions) {
        Assert.notNull(findingAid);

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFindingAidVO findingAidVO = mapper.map(findingAid, ArrFindingAidVO.class);
        if (includeVersions) {

            List<ArrFindingAidVersion> versions = findingAidVersionRepository
                    .findVersionsByFindingAidIdOrderByCreateDateAsc(findingAid.getFindingAidId());

            List<ArrFindingAidVersionVO> versionVOs = new ArrayList<>(versions.size());
            for (ArrFindingAidVersion version : versions) {
                versionVOs.add(createFindingAidVersion(version));
            }
            findingAidVO.setVersions(versionVOs);
        }
        return findingAidVO;
    }

    /**
     * Vytvoří verzi archivní pomůcky.
     *
     * @param version verze archivní pomůcky
     * @return VO verze archivní pomůcky
     */
    public ArrFindingAidVersionVO createFindingAidVersion(ArrFindingAidVersion version) {
        Assert.notNull(version);

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFindingAidVersionVO findingAidVersionVO = mapper.map(version, ArrFindingAidVersionVO.class);
        findingAidVersionVO.setCreateDate(version.getCreateChange().getChangeDate());

        ArrChange lockChange = version.getLockChange();
        if (lockChange != null) {
            findingAidVersionVO.setLockDate(lockChange.getChangeDate());
        }
        findingAidVersionVO.setArrangementType(createArrangementType(version.getArrangementType()));

        return findingAidVersionVO;
    }

    /**
     * Vytvoření specifikace hodnoty atributu.
     *
     * @param descItemSpec specifikace hodnoty atributu
     * @return VO specifikace hodnoty atributu
     */
    public RulDescItemSpecVO createDescItemSpecVO(final RulDescItemSpec descItemSpec) {
        Assert.notNull(descItemSpec);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulDescItemSpecVO descItemSpecVO = mapper.map(descItemSpec, RulDescItemSpecVO.class);
        return descItemSpecVO;
    }

    /**
     * Vytvoření typu hodnoty atributu.
     *
     * @param descItemType typ hodnoty atributu
     * @return VO typ hodnoty atributu
     */
    public RulDescItemTypeDescItemsVO createDescItemTypeVO(final RulDescItemType descItemType) {
        Assert.notNull(descItemType);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulDescItemTypeDescItemsVO descItemTypeVO = mapper.map(descItemType, RulDescItemTypeDescItemsVO.class);
        descItemTypeVO.setDataTypeId(descItemType.getDataType().getDataTypeId());
        descItemTypeVO.setDescItemSpecs(
                createList(descItemSpecRepository.findByDescItemType(descItemType), RulDescItemSpecVO.class,
                        this::createDescItemSpecVO));
        descItemTypeVO.setWidth(elzaRules.getTypeWidthByCode(descItemType.getCode()));
        return descItemTypeVO;
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItem hodnota atributu
     * @return VO hodnota atributu
     */
    public ArrDescItemVO createDescItem(final ArrDescItem descItem) {
        Assert.notNull(descItem);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrDescItemVO descItemVO = mapper.map(descItem, ArrDescItemVO.class);
        Integer specId = (descItem.getDescItemSpec() == null) ? null : descItem.getDescItemSpec().getDescItemSpecId();
        descItemVO.setDescItemSpecId(specId);
        return descItemVO;
    }

    /**
     * Vytvoření skupin, které zaobalují hodnoty atributů (typy, specifikace, apod.)
     *
     * @param descItems seznam hodnot atributů
     * @return VO skupin zabalených atributů
     */
    public List<ArrDescItemGroupVO> createDescItemGroups(final List<ArrDescItem> descItems) {
        Map<String, ArrDescItemGroupVO> descItemGroupVOMap = new HashMap<>();
        Map<RulDescItemType, List<ArrDescItemVO>> descItemByType = new HashMap<>();
        List<RulDescItemTypeDescItemsVO> descItemTypeVOList = new ArrayList<>();

        // vytvoření VO hodnot atributů
        for (ArrDescItem descItem : descItems) {
            List<ArrDescItemVO> descItemList = descItemByType.get(descItem.getDescItemType());

            if (descItemList == null) {
                descItemList = new ArrayList<>();
                descItemByType.put(descItem.getDescItemType(), descItemList);
            }

            descItemList.add(createDescItem(descItem));
        }

        // zjištění použitých typů atributů a jejich převod do VO
        for (RulDescItemType descItemType : descItemByType.keySet()) {
            RulDescItemTypeDescItemsVO descItemTypeVO = createDescItemTypeVO(descItemType);
            descItemTypeVOList.add(descItemTypeVO);
            descItemTypeVO.setDescItems(descItemByType.get(descItemType));
        }

        // rozřazení do skupin podle konfigurace
        for (RulDescItemTypeDescItemsVO descItemTypeVO : descItemTypeVOList) {
            ElzaRules.Group group = elzaRules.getGroupByType(descItemTypeVO.getCode());
            ArrDescItemGroupVO descItemGroupVO = descItemGroupVOMap.get(group.getCode());

            if (descItemGroupVO == null) {
                descItemGroupVO = new ArrDescItemGroupVO(group.getCode(), group.getName());
                descItemGroupVOMap.put(group.getCode(), descItemGroupVO);
            }

            List<RulDescItemTypeDescItemsVO> descItemTypeList = descItemGroupVO.getDescItemTypes();
            if (descItemTypeList == null) {
                descItemTypeList = new ArrayList<>();
                descItemGroupVO.setDescItemTypes(descItemTypeList);
            }

            descItemTypeList.add(descItemTypeVO);
        }

        ArrayList<ArrDescItemGroupVO> descItemGroupVOList = new ArrayList<>(descItemGroupVOMap.values());

        List<String> typeGroupCodes = elzaRules.getTypeGroupCodes();

        // seřazení skupin
        Collections.sort(descItemGroupVOList, (left, right) -> Integer
                .compare(typeGroupCodes.indexOf(left.getCode()), typeGroupCodes.indexOf(right.getCode())));

        // seřazení položek ve skupinách
        for (ArrDescItemGroupVO descItemGroupVO : descItemGroupVOList) {
            List<String> typeInfos = elzaRules.getTypeCodesByGroupCode(descItemGroupVO.getCode());
            if (typeInfos.isEmpty()) {

                // seřazení typů atributů podle viewOrder
                Collections.sort(descItemGroupVO.getDescItemTypes(), (left, right) -> Integer
                        .compare(left.getViewOrder(), right.getViewOrder()));
            } else {

                // seřazení typů atributů podle konfigurace
                Collections.sort(descItemGroupVO.getDescItemTypes(), (left, right) -> Integer
                        .compare(typeInfos.indexOf(left.getCode()), typeInfos.indexOf(right.getCode())));
            }

            descItemGroupVO.getDescItemTypes().forEach(descItemType -> {
                if (CollectionUtils.isNotEmpty(descItemType.getDescItems())) {

                    // seřazení hodnot atributů podle position
                    Collections.sort(descItemType.getDescItems(), (left, right) -> Integer
                            .compare(left.getPosition(), right.getPosition()));
                }
            });
        }

        return descItemGroupVOList;
    }

    /**
     * Vytvoření seznamu datových typů, které jsou k dispozici.
     *
     * @param dataTypes datové typy
     * @return seznam VO datových typů
     */
    public List<RulDataTypeVO> createDataTypeList(final List<RulDataType> dataTypes) {
        return createList(dataTypes, RulDataTypeVO.class, null);
    }

    /**
     * Vytvoření seznamu typů kalendářů, které jsou k dispozici.
     *
     * @param calendarTypes typy kalendářů
     * @return seznam VO typů kalendářů
     */
    public List<ArrCalendarTypeVO> createCalendarTypes(final List<ArrCalendarType> calendarTypes) {
        return createList(calendarTypes, ArrCalendarTypeVO.class, null);
    }

    /**
     * Vytvoření seznamu rozšířených typů hodnot atributů se specifikacemi.
     *
     * @param descItemTypes seznam typů hodnot atributů
     * @return seznam VO typů hodnot atributů
     */
    public List<RulDescItemTypeExtVO> createDescItemTypes(final List<RulDescItemTypeExt> descItemTypes) {
        return createList(descItemTypes, RulDescItemTypeExtVO.class, this::createDescItemTypeExt);
    }

    /**
     * Vytvoření typu hodnoty atributu se specifikacemi.
     *
     * @param descItemType typ hodnoty atributu se specifikacemi
     * @return VO typu hodnoty atributu se specifikacemi
     */
    public RulDescItemTypeExtVO createDescItemTypeExt(final RulDescItemTypeExt descItemType) {
        Assert.notNull(descItemType);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulDescItemTypeExtVO descItemTypeVO = mapper.map(descItemType, RulDescItemTypeExtVO.class);
        descItemTypeVO.setDataTypeId(descItemType.getDataType().getDataTypeId());
        return descItemTypeVO;
    }

    /**
     * Vytvoření uzlu archivní pomůcky.
     *
     * @param node uzel AP
     * @return VO uzlu AP
     */
    public ArrNodeVO createArrNode(final ArrNode node) {
        Assert.notNull(node);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrNodeVO descItemVO = mapper.map(node, ArrNodeVO.class);
        return descItemVO;
    }
}
