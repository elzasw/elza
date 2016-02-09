package cz.tacr.elza.controller.config;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.config.ConfigRules;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.BulkActionStateVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.NodeConformityVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegRecordSimple;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemTypeGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
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
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private ConfigRules elzaRules;

    /**
     * Vytvoří detailní objekt osoby. Načte všechna navázaná data.
     *
     * @param party osoba
     * @return detail VO osoby
     */
    public ParPartyVO createParPartyDetail(final ParParty party) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ParPartyVO result = mapper.map(party, ParPartyVO.class);


        //partyNames
        result.setPartyNames(createList(partyNameRepository.findByParty(party), ParPartyNameVO.class, (n) ->
                        createParPartyNameDetail(n)
        ));

        result.getPartyNames().sort((a, b) -> {
                    if (a.isPrefferedName()) {
                        return Integer.MIN_VALUE;
                    }
                    if (b.isPrefferedName()) {
                        return Integer.MAX_VALUE;
                    }

                    return a.getPartyNameId().compareTo(b.getPartyNameId());
                }
        );


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

        List<ParPartyName> allPartyNames = partyNameRepository.findByParties(parties);
        Map<Integer, List<ParPartyName>> partyNameMap = ElzaTools
                .createGroupMap(allPartyNames, p -> p.getParty().getPartyId());

        MapperFacade mapper = mapperFactory.getMapperFacade();
        Map<Integer, ParPartyVO> partyMap = new LinkedHashMap<>();

        for (final ParParty party : parties) {
            ParPartyVO partyVO = mapper.map(party, ParPartyVO.class);

            partyMap.put(partyVO.getPartyId(), partyVO);


            List<ParPartyName> partyNames = partyNameMap.get(party.getPartyId());
            List<ParPartyNameVO> partyNamesVo = new ArrayList<>(partyNames.size());

            for (ParPartyName partyName : partyNames) {
                ParPartyNameVO partyNameVo = mapper.map(partyName, ParPartyNameVO.class);
                if (partyName.equals(party.getPreferredName())) {
                    partyNameVo.setPrefferedName(true);
                }
                partyNamesVo.add(partyNameVo);
            }
            partyVO.setPartyNames(partyNamesVo);
        }

//        for (final ParPartyName partyName : partyNames) {
//            ParPartyNameVO partyNameVO = mapper.map(partyName, ParPartyNameVO.class);
//            partyMap.get(partyNameVO.getPartyId()).addPartyName(partyNameVO);
//        }

        return new ArrayList<>(partyMap.values());
    }


    /**
     * Vytvoří objekt jména osoby. Jsou načteny i detailní informace.
     *
     * @param partyName jméno osoby
     * @return vo jména osoba
     */
    private ParPartyNameVO createParPartyNameDetail(final ParPartyName partyName) {

        if(partyName.getPartyNameComplements() != null){
            partyName.getPartyNameComplements().sort(new ParPartyNameComplement.ParPartyNameComplementComparator());
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ParPartyNameVO result = mapper.map(partyName, ParPartyNameVO.class);

        if(partyName == partyName.getParty().getPreferredName()){
            result.setPrefferedName(true);
        }
//        List<ParPartyNameComplement> nameComplements = partyNameComplementRepository.findByPartyName(partyName);
//        result.setPartyNameComplements(createList(nameComplements, ParPartyNameComplementVO.class, null));

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
        relations.sort(new ParRelation.ParRelationComparator());

        MapperFacade mapper = mapperFactory.getMapperFacade();

        Map<Integer, ParRelationVO> relationVOMap = new LinkedHashMap<>();
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
     * Vytvoří VO objekt vztahu z DO.
     *
     * @param relation VO objekt vztahu
     * @return DO objekt vztahu
     */
    public ParRelationVO createRelation(final ParRelation relation) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ParRelationVO relationVO = mapper.map(relation, ParRelationVO.class);

        for (ParRelationEntity relationEntity : relationEntityRepository.findByRelation(relation)) {
            relationVO.addRelationEntity(mapper.map(relationEntity, ParRelationEntityVO.class));
        }

        return relationVO;
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
                                              final Map<Integer, Integer> recordIdPartyIdMap, boolean fillParents,
                                              @Nullable final RegRecord fillToParent) {
        List<RegRecordVO> result = new ArrayList<>(records.size());
        for (final RegRecord record : records) {
            Integer partyId = recordIdPartyIdMap.get(record.getRecordId());
            result.add(createRegRecord(record, partyId, fillParents, fillToParent));
        }

        return result;
    }

    /**
     * Vytvoří rejstříkové heslo.
     *
     * @param regRecord   rejstříkové heslo
     * @param partyId     id osoby
     * @param fillParents příznak zda se mají načíst rodiče rejstříku
     * @param fillToParent
     * @return rejstříkové heslo
     */
    public RegRecordVO createRegRecord(final RegRecord regRecord,
                                       @Nullable final Integer partyId,
                                       boolean fillParents, final RegRecord fillToParent) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RegRecordVO result = mapper.map(regRecord, RegRecordVO.class);
        result.setPartyId(partyId);

        if (fillParents) {
            List<RegRecordVO.RecordParent> parents = new LinkedList<>();
            RegRecord parentRecord = regRecord.getParentRecord();
            while (parentRecord != null ) {
                parents.add(new RegRecordVO.RecordParent(parentRecord.getRecordId(), parentRecord.getRecord()));
                parentRecord = parentRecord.getParentRecord();
            }
            result.setParents(parents);
        }

        return result;
    }

    /**
     * Pro heslo vytvoří seznam typů až po kořen typů nebo po typ v seznamu.
     *  @param record        heslo
     */
    public void fillRegisterTypeNamesToParents(final RegRecordVO record) {

        List<RegRecordVO.RecordParent> parentTypeNames = new ArrayList<>();

        RegRegisterType recordType = registerTypeRepository.findOne(record.getRegisterTypeId());
        parentTypeNames.add(new RegRecordVO.RecordParent(recordType.getRegisterTypeId(),recordType.getName()));
        record.setTypesToRoot(parentTypeNames);


        RegRegisterType parentType = recordType.getParentRegisterType();
        while (parentType != null) {
            parentTypeNames.add(new RegRecordVO.RecordParent(parentType.getRegisterTypeId(), parentType.getName()));
            parentType = parentType.getParentRegisterType();
        }

    }

    /**
     * Vytvoří seznam jedhoduchých rejstříkových hesel.
     *
     * @param records seznam rej. hesel
     * @return seznam jednoduchých rejs. hesel
     */
    public List<RegRecordSimple> createRegRecordsSimple(final Collection<RegRecord> records) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        List<RegRecordSimple> result = new ArrayList<>(records.size());

        for (RegRecord record : records) {
            result.add(mapper.map(record, RegRecordSimple.class));
        }

        return result;
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
     * @param checkPartyType true -> bude nastaven parametr addRecord podle toho, jestli se partytype rovná typu osoby v rejstříku
     * @param  partyType typ osoby, který musí mít nastaven typ rejstříku, jinak nelze vkládat nové záznamy
     * @return seznam kořenových typů rejstříků
     */
    public List<RegRegisterTypeVO> createRegisterTypesTree(final List<RegRegisterType> allTypes,
                                                           final boolean checkPartyType,
                                                           @Nullable final ParPartyType partyType) {
        if (CollectionUtils.isEmpty(allTypes)) {
            return Collections.EMPTY_LIST;
        }

        Map<Integer, RegRegisterTypeVO> typeMap = new HashMap<>();
        List<RegRegisterTypeVO> roots = new LinkedList<>();
        for (final RegRegisterType registerType : allTypes) {
            if (checkPartyType) {
                createRegisterTypeTreeForPartyType(registerType, partyType, typeMap, roots);
            }else{
                createRegisterTypeTree(registerType, typeMap, roots);
            }
        }

        return roots;
    }

    /**
     * Vytvoří typ rejstříkového hesla a vloží jeje do mapy všech hesel.
     *
     * @param registerType typ hesla
     * @param typeMap      mapa všech typů (id typu ->typ)
     * @param roots     kořeny stromu
     * @return typ rejstříkového hesla
     */
    private RegRegisterTypeVO createRegisterTypeTree(final RegRegisterType registerType,
                                                     final Map<Integer, RegRegisterTypeVO> typeMap,
                                                     final List<RegRegisterTypeVO> roots) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        RegRegisterTypeVO result = typeMap.get(registerType.getRegisterTypeId());
        if (result != null) {
            return result;
        }

        result = mapper.map(registerType, RegRegisterTypeVO.class);
        typeMap.put(result.getId(), result);
        if (registerType.getParentRegisterType() == null) {
            roots.add(result);
        }else{
            RegRegisterTypeVO parent = createRegisterTypeTree(registerType.getParentRegisterType(), typeMap, roots);
            parent.addChild(result);
            result.addParent(parent.getName());
            result.addParents(parent.getParents());
        }

        return result;
    }

    /**
     * Vytvoří typ rejstříkového hesla a vloží jeje do mapy všech hesel.
     *
     * @param registerType typ hesla
     * @param parPartyType typ osoby, který musí mít nastaven typ rejstříku, jinak nelze vkládat nové záznamy
     * @param typeMap      mapa všech typů (id typu ->typ)
     * @param roots        kořeny stromu
     * @return typ rejstříkového hesla
     */
    private RegRegisterTypeVO createRegisterTypeTreeForPartyType(final RegRegisterType registerType,
                                                                 @Nullable final ParPartyType parPartyType,
                                                                 final Map<Integer, RegRegisterTypeVO> typeMap,
                                                                 final List<RegRegisterTypeVO> roots) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        RegRegisterTypeVO result = typeMap.get(registerType.getRegisterTypeId());
        if (result != null) {
            return result;
        }
        boolean addRecord = registerType.getAddRecord() && ObjectUtils.equals(registerType.getPartyType(), parPartyType);
        result = mapper.map(registerType, RegRegisterTypeVO.class);
        result.setAddRecord(addRecord);

        typeMap.put(result.getId(), result);
        if (registerType.getParentRegisterType() == null) {
            roots.add(result);
        }else{
            RegRegisterTypeVO parent = createRegisterTypeTreeForPartyType(registerType.getParentRegisterType(),
                    parPartyType, typeMap, roots);
            parent.addChild(result);

            result.addParent(parent.getName());
            result.addParents(parent.getParents());
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
        Date createDate = Date.from(
                version.getCreateChange().getChangeDate().atZone(ZoneId.systemDefault()).toInstant());
        findingAidVersionVO.setCreateDate(createDate);

        ArrChange lockChange = version.getLockChange();
        if (lockChange != null) {
            Date lockDate = Date.from(lockChange.getChangeDate().atZone(ZoneId.systemDefault()).toInstant());
            findingAidVersionVO.setLockDate(lockDate);
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
     * Vytvoří seznam atributů.
     *
     * @param descItems seznam DO atributů
     * @return seznam VO atributů
     */
    public List<ArrDescItemVO> createDescItems(final List<ArrDescItem> descItems) {
        List<ArrDescItemVO> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            result.add(createDescItem(descItem));
        }
        return result;
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
            ConfigRules.Group group = elzaRules.getGroupByType(descItemTypeVO.getCode());
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
     * Vytvoření seznamu rozšířených typů hodnot atributů se specifikacemi ve skupinách.
     *
     * @param descItemTypes seznam typů hodnot atributů
     * @return seznam skupin s typy hodnot atributů
     */
    public List<ArrDescItemTypeGroupVO> createDescItemTypeGroups(final List<RulDescItemTypeExt> descItemTypes) {

        List<RulDescItemTypeExtVO> descItemTypeExtList = createList(descItemTypes, RulDescItemTypeExtVO.class,
                this::createDescItemTypeExt);

        Map<String, ArrDescItemTypeGroupVO> descItemTypeGroupVOMap = new HashMap<>();

        for (RulDescItemTypeExtVO descItemTypeVO : descItemTypeExtList) {
            ConfigRules.Group group = elzaRules.getGroupByType(descItemTypeVO.getCode());
            ArrDescItemTypeGroupVO descItemTypeGroupVO = descItemTypeGroupVOMap.get(group.getCode());

            if (descItemTypeGroupVO == null) {
                descItemTypeGroupVO = new ArrDescItemTypeGroupVO(group.getCode(), group.getName());
                descItemTypeGroupVOMap.put(group.getCode(), descItemTypeGroupVO);
            }

            List<RulDescItemTypeExtVO> descItemTypeList = descItemTypeGroupVO.getDescItemTypes();
            if (descItemTypeList == null) {
                descItemTypeList = new ArrayList<>();
                descItemTypeGroupVO.setDescItemTypes(descItemTypeList);
            }

            descItemTypeVO.setWidth(elzaRules.getTypeWidthByCode(descItemTypeVO.getCode()));
            descItemTypeList.add(descItemTypeVO);
        }

        return new ArrayList<>(descItemTypeGroupVOMap.values());
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
        ArrNodeVO result = mapper.map(node, ArrNodeVO.class);
        return result;
    }

    /**
     * Vytvoří seznam vo uzlů.
     *
     * @param nodes uzly
     * @return seznam vo uzlů
     */
    public List<ArrNodeVO> createArrNodes(final Collection<ArrNode> nodes) {
        List<ArrNodeVO> result = new LinkedList<>();
        for (ArrNode node : nodes) {
            result.add(createArrNode(node));
        }
        return result;
    }

    /**
     * Vytvoření seznamu typů obalů, které jsou k dispozici.
     *
     * @param packetTypes seznam DO typů obalů
     * @return seznam VO typů obalů
     */
    public List<RulPacketTypeVO> createPacketTypeList(final List<RulPacketType> packetTypes) {
        return createList(packetTypes, RulPacketTypeVO.class, null);
    }

    /**
     * Vytvoření seznamu obalů.
     *
     * @param packets seznam DO obalů
     * @return seznam VO obalů
     */
    public List<ArrPacketVO> createPacketList(final List<ArrPacket> packets) {
        return createList(packets, ArrPacketVO.class, null);
    }

    /**
     * Vytvoření obalu.
     *
     * @param packet DO obalu
     * @return VO obalu
     */
    public ArrPacketVO createPacket(final ArrPacket packet) {
        Assert.notNull(packet);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrPacketVO packetVO = mapper.map(packet, ArrPacketVO.class);
        return packetVO;
    }

    /**
     * Vytvoření informace o validace stavu JP.
     * @param nodeConformity    stav validace
     * @return  VO stavu validace
     */
    public NodeConformityVO createNodeConformity(final ArrNodeConformityExt nodeConformity) {
        Assert.notNull(nodeConformity);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        NodeConformityVO nodeConformityVO = mapper.map(nodeConformity, NodeConformityVO.class);
        return nodeConformityVO;
    }

    /**
     * Vytvoří třídu rejstříku.
     *
     * @param scope třída rejstříku
     * @return třída rejstříku
     */
    public RegScopeVO createScope(final RegScope scope) {
        Assert.notNull(scope);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(scope, RegScopeVO.class);
    }

    public List<RegScopeVO> createScopes(final Collection<RegScope> scopes) {
        Assert.notNull(scopes);
        List<RegScopeVO> result = new ArrayList<>(scopes.size());
        scopes.forEach(s -> result.add(createScope(s)));

        return result;
    }

    /**
     * Vytvoření seznamu stavu hromadných akcí
     *
     * @param bulkActionStates seznam DO stavu hromadných akcí
     * @return seznam VO stavu hromadných akcí
     */
    public List<BulkActionStateVO> createBulkActionStateList(final List<BulkActionState> bulkActionStates) {
        return createList(bulkActionStates, BulkActionStateVO.class, null);
    }


    /**
     * Vytvoření seznamu hromadných akcí
     *
     * @param bulkActions seznam DO hromadných akcí
     * @return seznam VO hromadných akcí
     */
    public List<BulkActionVO> createBulkActionList(final List<BulkActionConfig> bulkActions) {
        return createList(bulkActions, BulkActionVO.class, this::createBulkAction);
    }

    /**
     * Vytvoří hromadnou akci
     *
     * @param bulkAction DO hromadné akce
     * @return hromadná akce VO
     */
    public BulkActionVO createBulkAction(final BulkActionConfig bulkAction) {
        Assert.notNull(bulkAction);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        BulkActionVO bulkActionVO = mapper.map(bulkAction, BulkActionVO.class);
        bulkActionVO.setDescription((String) bulkAction.getProperty("description"));
        return bulkActionVO;
    }

    /**
     * Vytvoří stav hromadné akce
     *
     * @param bulkActionState stav hromadné akce DO
     * @return stav hromadné akce VO
     */
    public BulkActionStateVO createBulkActionState(final BulkActionState bulkActionState) {
        Assert.notNull(bulkActionState);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(bulkActionState, BulkActionStateVO.class);
    }

    public List<ArrNodeRegisterVO> createRegisterLinkList(final List<ArrNodeRegister> registerLinks) {
        return createList(registerLinks, ArrNodeRegisterVO.class, this::createRegisterLink);
    }

    public ArrNodeRegisterVO createRegisterLink(final ArrNodeRegister nodeRegister) {
        Assert.notNull(nodeRegister);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrNodeRegisterVO nodeRegisterVO = mapper.map(nodeRegister, ArrNodeRegisterVO.class);
        nodeRegisterVO.setNodeId(nodeRegister.getNode().getNodeId());
        nodeRegisterVO.setValue(nodeRegister.getRecord().getRecordId());
        return nodeRegisterVO;
    }

    /**
     * Vytvoří list scénářů nového levelu
     *
     * @param scenarioOfNewLevels list scénářů nového levelu DO
     * @return list scénářů nového levelu VO
     */
    public List<ScenarioOfNewLevelVO> createScenarioOfNewLevelList(final List<ScenarioOfNewLevel> scenarioOfNewLevels) {
        return createList(scenarioOfNewLevels, ScenarioOfNewLevelVO.class, null);
    }
}
