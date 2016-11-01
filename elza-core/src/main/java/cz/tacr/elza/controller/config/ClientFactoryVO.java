package cz.tacr.elza.controller.config;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.config.ConfigRules;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFileVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrOutputDefinitionVO;
import cz.tacr.elza.controller.vo.ArrOutputExtVO;
import cz.tacr.elza.controller.vo.ArrOutputFileVO;
import cz.tacr.elza.controller.vo.ArrOutputVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.BulkActionRunVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.DmsFileVO;
import cz.tacr.elza.controller.vo.ItemSpecsCategory;
import cz.tacr.elza.controller.vo.NodeConformityVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegCoordinatesVO;
import cz.tacr.elza.controller.vo.RegRecordSimple;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.UISettingsVO;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemTypeGroupVO;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.packageimport.ItemTypeUpdater;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.SettingsService;
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
    private PermissionRepository permissionRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UnitdateRepository unitdateRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private ConfigRules elzaRules;

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

    /**
     * Vytvoří objekt informací o přihlášeném uživateli.
     * @param userDetail detail objekt
     * @return detail VO objekt
     */
    public UserInfoVO createUserInfo(final UserDetail userDetail) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        UserInfoVO result = mapper.map(userDetail, UserInfoVO.class);
        UsrUser user = userRepository.findByUsername(userDetail.getUsername());
        if (user != null) {
            List<UISettings> settings = settingsService.getSettings(user);
            result.setSettings(mapper.mapAsList(settings, UISettingsVO.class));
        }
        return result;
    }

    /**
     * Vytvoření nastavení.
     *
     * @param settings nastavení
     * @return
     */
    public List<UISettingsVO> createSettingsList(final List<UISettings> settings) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.mapAsList(settings, UISettingsVO.class);
    }

    /**
     * Vytvoří seznam VO.
     * @param users vstupní seznam uživatelů
     * @return seznam VO
     */
    public List<UsrUserVO> createUserList(final List<UsrUser> users) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.mapAsList(users, UsrUserVO.class);
    }

    /**
     * Vytvoření kolekce VO z kolekce DO.
     * @param templates vstup
     * @return seznam VO
     */
    public List<RulTemplateVO> createTemplates(final Collection<RulTemplate> templates){
        Assert.notNull(templates);

        List<RulTemplateVO> result = new ArrayList<>();
        for (RulTemplate template : templates) {
            result.add(createTemplate(template));
        }
        return result;
    }

    /**
     * Vytvoření VO z DO.
     * @param template DO
     * @return VO
     */
    public RulTemplateVO createTemplate(final RulTemplate template){
        Assert.notNull(template);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(template, RulTemplateVO.class);
    }

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
            if (partyNames != null) {
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
                                              final Map<Integer, Integer> recordIdPartyIdMap, final boolean fillParents,
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
                                       final boolean fillParents, final RegRecord fillToParent) {
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
        parentTypeNames.add(new RegRecordVO.RecordParent(recordType.getRegisterTypeId(), recordType.getName()));
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
     * Vytvoření souřadnice rejsříkového hesla
     *
     * @param regCoordinates souřadnice rejsříkového hesla
     * @return VO souřadnice rejstříkového hesla
     */
    public RegCoordinatesVO createRegCoordinates(final RegCoordinates regCoordinates) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regCoordinates, RegCoordinatesVO.class);
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
     * Vytvoření seznamu souřadnic rejstříkových hesel
     *
     * @param coordinatesList seznam souřadnic rejstříkových hesel
     * @return seznam VO seznam souřadnic
     */
    public List<RegCoordinatesVO> createRegCoordinates(@Nullable final List<RegCoordinates> coordinatesList) {
        if (coordinatesList == null) {
            return null;
        }

        List<RegCoordinatesVO> result = new ArrayList<>(coordinatesList.size());
        coordinatesList.forEach((coordinates) ->
                result.add(createRegCoordinates(coordinates))
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
     * Vytvoření ArrFund a načtení verzí.
     *
     * @param fund      DO
     * @param includeVersions true - budou do objektu donačteny všechny jeho verze, false- verze nebudou donačteny
     * @return VO
     */
    public ArrFundVO createFundVO(final ArrFund fund, final boolean includeVersions) {
        Assert.notNull(fund);

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFundVO fundVO = mapper.map(fund, ArrFundVO.class);
        fundVO.setInstitutionId(fund.getInstitution().getInstitutionId());
        if (includeVersions) {

            List<ArrFundVersion> versions = fundVersionRepository
                    .findVersionsByFundIdOrderByCreateDateDesc(fund.getFundId());

            List<ArrFundVersionVO> versionVOs = new ArrayList<>(versions.size());
            for (ArrFundVersion version : versions) {
                versionVOs.add(createFundVersion(version));
            }
            fundVO.setVersions(versionVOs);

            fundVO.setValidNamedOutputs(createOutputDefinitions(outputDefinitionRepository.findValidOutputDefinitionByFund(fund), false));
            fundVO.setHistoricalNamedOutputs(createOutputDefinitions(outputDefinitionRepository.findHistoricalOutputDefinitionByFund(fund), true));
        }

        return fundVO;
    }

    /**
     * Vytvoří verzi archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     * @return VO verze archivní pomůcky
     */
    public ArrFundVersionVO createFundVersion(final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion);

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFundVersionVO fundVersionVO = mapper.map(fundVersion, ArrFundVersionVO.class);
        Date createDate = Date.from(
                fundVersion.getCreateChange().getChangeDate().atZone(ZoneId.systemDefault()).toInstant());
        fundVersionVO.setCreateDate(createDate);

        ArrChange lockChange = fundVersion.getLockChange();
        if (lockChange != null) {
            Date lockDate = Date.from(lockChange.getChangeDate().atZone(ZoneId.systemDefault()).toInstant());
            fundVersionVO.setLockDate(lockDate);
        }
        fundVersionVO.setDateRange(fundVersion.getDateRange());
        fundVersionVO.setRuleSetId(fundVersion.getRuleSet().getRuleSetId());

        return fundVersionVO;
    }

    /**
     * Vytvoří třídy výstupů archivního souboru.
     *
     * @param outputDefinitions seznam DO
     * @param loadOutputs  mají se do objektu načíst verze? (arr_output)
     * @return seznam VO
     */
    public List<ArrOutputDefinitionVO> createOutputDefinitions(final Collection<ArrOutputDefinition> outputDefinitions,
                                                               final boolean loadOutputs) {
        Assert.notNull(outputDefinitions);

        MapperFacade mapper = mapperFactory.getMapperFacade();

        List<ArrOutputDefinitionVO> result = new ArrayList<>(outputDefinitions.size());
        for (ArrOutputDefinition outputDefinition : outputDefinitions) {

            ArrOutputDefinitionVO outputDefinitionVO = mapper.map(outputDefinition, ArrOutputDefinitionVO.class);

            if (loadOutputs) {
                outputDefinitionVO.setOutputs(createOutputsVO(outputDefinition.getOutputs()));
            }

            result.add(outputDefinitionVO);
        }
        return result;
    }

    /**
     * Vytvoří třídy výstupů archivního souboru.
     *
     * @param outputDefinition DO
     * @return VO
     */
    public ArrOutputDefinitionVO createOutputDefinition(final ArrOutputDefinition outputDefinition) {
        Assert.notNull(outputDefinition);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrOutputDefinitionVO outputDefinitionVO = mapper.map(outputDefinition, ArrOutputDefinitionVO.class);
        return outputDefinitionVO;
    }

    /**
     * Vytvoření objektů verzí výstupu archivního souboru.
     *
     * @param outputs verze výstupu archivního souboru
     * @return seznam verzí seřazený od nejstarší po nejmladší
     */
    private List<ArrOutputVO> createOutputsVO(final List<ArrOutput> outputs) {
        Assert.notNull(outputs);

        List<ArrOutputVO> result = new ArrayList<>(outputs.size());
        MapperFacade mapper = mapperFactory.getMapperFacade();

        for (ArrOutput output : outputs) {
            result.add(mapper.map(output, ArrOutputVO.class));
        }

        Collections.sort(result, (lock1, lock2) -> {
            if (lock1 != null && lock2 != null) {
                return lock1.getLockDate().compareTo(lock2.getLockDate());
            } else if (lock1 == null) {
                return 1;
            } else {
                return -1;
            }
        });

        return result;
    }

    /**
     * Vytvoření specifikace hodnoty atributu.
     *
     * @param descItemSpec specifikace hodnoty atributu
     * @return VO specifikace hodnoty atributu
     */
    public RulDescItemSpecVO createDescItemSpecVO(final RulItemSpec descItemSpec) {
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
    public RulDescItemTypeDescItemsVO createDescItemTypeVO(final RulItemType descItemType) {
        Assert.notNull(descItemType);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulDescItemTypeDescItemsVO descItemTypeVO = mapper.map(descItemType, RulDescItemTypeDescItemsVO.class);
        descItemTypeVO.setDataTypeId(descItemType.getDataType().getDataTypeId());
        return descItemTypeVO;
    }

    /**
     * Vytvoření typu hodnoty atributu.
     *
     * @param descItemType typ hodnoty atributu
     * @return VO typ hodnoty atributu
     */
    public ItemTypeDescItemsLiteVO createDescItemTypeLiteVO(final RulItemType descItemType) {
        Assert.notNull(descItemType);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ItemTypeDescItemsLiteVO descItemTypeVO = mapper.map(descItemType, ItemTypeDescItemsLiteVO.class);
        return descItemTypeVO;
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param item hodnota atributu
     * @return VO hodnota atributu
     */
    public <T extends ArrItem> ArrItemVO createDescItem(final T item) {
        Assert.notNull(item);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrItemVO itemVO = mapper.map(item.getItem(), ArrItemVO.class);
        BeanUtils.copyProperties(item, itemVO);
        itemVO.setId(item.getItemId());

        Integer specId = (item.getItemSpec() == null) ? null : item.getItemSpec().getItemSpecId();
        itemVO.setDescItemSpecId(specId);
        return itemVO;
    }

    /**
     * Vytvoří seznam atributů.
     *
     * @param descItems seznam DO atributů
     * @return seznam VO atributů
     */
    public List<ArrItemVO> createDescItems(final List<ArrDescItem> descItems) {
        List<ArrItemVO> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            result.add(createDescItem(descItem));
        }
        return result;
    }

    /**
     * Vytvoření skupin, které zaobalují hodnoty atributů (typy, specifikace, apod.)
     *
     * @param items seznam hodnot atributů
     * @return VO skupin zabalených atributů
     */
    public <T extends ArrItem> List<ItemGroupVO> createItemGroupsNew(final String ruleCode, final Integer fundId, final List<T> items) {
        Map<String, ItemGroupVO> itemGroupVOMap = new HashMap<>();
        Map<RulItemType, List<ArrItemVO>> itemByType = new HashMap<>();
        List<ItemTypeDescItemsLiteVO> itemTypeVOList = new ArrayList<>();

        // vytvoření VO hodnot atributů
        for (T item : items) {
            List<ArrItemVO> itemList = itemByType.get(item.getItemType());

            if (itemList == null) {
                itemList = new ArrayList<>();
                itemByType.put(item.getItemType(), itemList);
            }

            itemList.add(createItem(item));
        }

        // zjištění použitých typů atributů a jejich převod do VO
        for (RulItemType descItemType : itemByType.keySet()) {
            ItemTypeDescItemsLiteVO itemTypeVO = createDescItemTypeLiteVO(descItemType);
            itemTypeVOList.add(itemTypeVO);
            itemTypeVO.setDescItems(itemByType.get(descItemType));
        }

        Map<Integer, String> codeToId = new HashMap<>();

        itemByType.keySet().forEach(type -> codeToId.put(type.getItemTypeId(), type.getCode()));

        // rozřazení do skupin podle konfigurace
        for (ItemTypeDescItemsLiteVO descItemTypeVO : itemTypeVOList) {
            ConfigRules.Group group = elzaRules.getGroupByType(ruleCode, fundId, codeToId.get(descItemTypeVO.getId()));
            ItemGroupVO itemGroupVO = itemGroupVOMap.get(group.getCode());

            if (itemGroupVO == null) {
                itemGroupVO = new ItemGroupVO(group.getCode());
                itemGroupVOMap.put(group.getCode(), itemGroupVO);
            }

            List<ItemTypeDescItemsLiteVO> itemTypeList = itemGroupVO.getTypes();
            if (itemTypeList == null) {
                itemTypeList = new ArrayList<>();
                itemGroupVO.setTypes(itemTypeList);
            }

            itemTypeList.add(descItemTypeVO);
        }

        ArrayList<ItemGroupVO> itemGroupVOList = new ArrayList<>(itemGroupVOMap.values());

        List<String> typeGroupCodes = elzaRules.getTypeGroupCodes(ruleCode, fundId);

        // seřazení skupin
        Collections.sort(itemGroupVOList, (left, right) -> Integer
                .compare(typeGroupCodes.indexOf(left.getCode()), typeGroupCodes.indexOf(right.getCode())));

        // seřazení položek ve skupinách
        for (ItemGroupVO itemGroupVO : itemGroupVOList) {
            List<String> typeInfos = elzaRules.getTypeCodesByGroupCode(ruleCode, fundId, itemGroupVO.getCode());
            if (typeInfos.isEmpty()) {

                // seřazení typů atributů podle viewOrder
                Collections.sort(itemGroupVO.getTypes(), (left, right) -> Integer
                        .compare(left.getViewOrder(), right.getViewOrder()));
            } else {

                // seřazení typů atributů podle konfigurace
                Collections.sort(itemGroupVO.getTypes(), (left, right) -> Integer
                        .compare(typeInfos.indexOf(codeToId.get(left.getId())), typeInfos.indexOf(codeToId.get(
                                right.getId()))));
            }

            itemGroupVO.getTypes().forEach(descItemType -> {
                if (CollectionUtils.isNotEmpty(descItemType.getDescItems())) {

                    // seřazení hodnot atributů podle position
                    Collections.sort(descItemType.getDescItems(), (left, right) -> Integer
                            .compare(left.getPosition(), right.getPosition()));
                }
            });
        }

        return itemGroupVOList;
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
     * @param itemTypes seznam typů hodnot atributů
     * @return seznam skupin s typy hodnot atributů
     */
    public List<ItemTypeGroupVO> createItemTypeGroupsNew(final String ruleCode, final Integer fundId, final List<RulItemTypeExt> itemTypes) {

        List<ItemTypeLiteVO> itemTypeExtList = createList(itemTypes, ItemTypeLiteVO.class, this::createItemTypeLite);

        Map<Integer, String> codeToId = new HashMap<>();

        itemTypes.forEach(type -> codeToId.put(type.getItemTypeId(), type.getCode()));

        // načtený globální oblíbených
        List<UISettings> favoritesItemTypes = settingsService.getGlobalSettings(UISettings.SettingsType.FAVORITE_ITEM_SPECS, UISettings.EntityType.ITEM_TYPE);

        // typeId -> list<specId>
        // naplnění mapy podle oblíbených z nastavení
        Map<Integer, List<Integer>> typeSpecsMap = new HashMap<>();
        for (UISettings favoritesItemType : favoritesItemTypes) {
            String value = favoritesItemType.getValue();
            if (value != null) {
                String[] specIdsString = value.split("\\|");
                Integer[] specIds = new Integer[specIdsString.length];
                for (int i = 0; i < specIdsString.length; i++) {
                    specIds[i] = Integer.valueOf(specIdsString[i]);
                }
                typeSpecsMap.put(favoritesItemType.getEntityId(), Arrays.asList(specIds));
            }
        }

        Map<String, ItemTypeGroupVO> itemTypeGroupVOMap = new HashMap<>();

        for (ItemTypeLiteVO itemTypeVO : itemTypeExtList) {

            // získání a vyplnění oblíbených specifikací u typu
            List<Integer> favoriteSpecIds = typeSpecsMap.get(itemTypeVO.getId());
            itemTypeVO.setFavoriteSpecIds(favoriteSpecIds);

            ConfigRules.Group group = elzaRules.getGroupByType(ruleCode, fundId, codeToId.get(itemTypeVO.getId()));
            ItemTypeGroupVO itemTypeGroupVO = itemTypeGroupVOMap.get(group.getCode());

            if (itemTypeGroupVO == null) {
                itemTypeGroupVO = new ItemTypeGroupVO(group.getCode());
                itemTypeGroupVOMap.put(group.getCode(), itemTypeGroupVO);
            }

            List<ItemTypeLiteVO> itemTypeList = itemTypeGroupVO.getTypes();
            if (itemTypeList == null) {
                itemTypeList = new ArrayList<>();
                itemTypeGroupVO.setTypes(itemTypeList);
            }

            itemTypeVO.setWidth(elzaRules.getTypeWidthByCode(ruleCode, fundId, codeToId.get(itemTypeVO.getId())));
            itemTypeList.add(itemTypeVO);
        }

        return new ArrayList<>(itemTypeGroupVOMap.values());
    }

    private ItemTypeLiteVO createItemTypeLite(final RulItemTypeExt itemTypeExt) {
        Assert.notNull(itemTypeExt);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ItemTypeLiteVO itemTypeVO = mapper.map(itemTypeExt, ItemTypeLiteVO.class);
        return itemTypeVO;
    }

    /**
     * Vytvoření typu hodnoty atributu se specifikacemi.
     *
     * @param descItemType typ hodnoty atributu se specifikacemi
     * @return VO typu hodnoty atributu se specifikacemi
     */
    public RulDescItemTypeExtVO createDescItemTypeExt(final RulItemTypeExt descItemType) {
        Assert.notNull(descItemType);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulDescItemTypeExtVO descItemTypeVO = mapper.map(descItemType, RulDescItemTypeExtVO.class);
        descItemTypeVO.setDataTypeId(descItemType.getDataType().getDataTypeId());
        descItemTypeVO.setItemSpecsTree(new ArrayList<>(1));
        for (RulItemSpecExt rulItemSpecExt : descItemType.getRulItemSpecList()) {
            if (StringUtils.isNotEmpty(rulItemSpecExt.getCategory())) {
                String[] categories = rulItemSpecExt.getCategory().split("\\" + ItemTypeUpdater.CATEGORY_SEPARATOR);
                recursiveAddCategory(categories, 0, descItemTypeVO.getItemSpecsTree(), rulItemSpecExt.getItemSpecId());
            }
        }
        return descItemTypeVO;
    }

    /**
     * Rekurzivní sestavení stromu kategorií.
     *
     * @param categories    cesta kategorií stromem
     * @param depthIndex    index aktuální pozice cesty stromem
     * @param itemSpecsTree seznam kategorií na aktuální pozici cesty stromem
     * @param itemSpecId    přidávaný identifikátor specifikace
     */
    private void recursiveAddCategory(final String[] categories,
                                      final int depthIndex,
                                      final List<ItemSpecsCategory> itemSpecsTree,
                                      final Integer itemSpecId) {
        if (itemSpecsTree == null || depthIndex >= categories.length) {
            return;
        }

        String categoryName = categories[depthIndex];

        ItemSpecsCategory category = null;

        // vyhledám kategorii
        for (ItemSpecsCategory itemSpecsCategory : itemSpecsTree) {
            if (itemSpecsCategory.getName().equals(categoryName)) {
                category = itemSpecsCategory;
                break;
            }
        }

        // pokud ještě neexistuje, vytvořím a přidám jí do seznamu
        if (category == null) {
            category = new ItemSpecsCategory();
            category.setName(categoryName);
            itemSpecsTree.add(category);
        }

        // pokud jsme na poslední úrovni, přidáme id do seznamu identifikátorů specifikace,
        // pokud nejsme, procházíme do hlubší kategorie
        if (depthIndex + 1 == categories.length) {
            List<Integer> specIds = category.getSpecIds();
            if (specIds == null) {
                specIds = new ArrayList<>(1);
                category.setSpecIds(specIds);
            }
            specIds.add(itemSpecId);
        } else {
            if (category.getChildren() == null) {
                category.setChildren(new ArrayList<>(1));
            }
            recursiveAddCategory(categories, depthIndex + 1, category.getChildren(), itemSpecId);
        }

    }

    /**
     * Vytvoření seznamu typů hodnot se specifikacemi.
     *
     * @param descItemTypes DO typů hodnot
     * @return VO typů hodnot
     */
    public List<RulDescItemTypeExtVO> createDescItemTypeExtList(final List<RulItemTypeExt> descItemTypes) {
        return createList(descItemTypes, RulDescItemTypeExtVO.class, this::createDescItemTypeExt);
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
        return mapper.map(bulkAction, BulkActionVO.class);
    }

    public BulkActionRunVO createBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        Assert.notNull(bulkActionRun);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(bulkActionRun, BulkActionRunVO.class);
    }

    public BulkActionRunVO createBulkActionRunWithNodes(final ArrBulkActionRun bulkActionRun) {
        Assert.notNull(bulkActionRun);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        BulkActionRunVO bulkActionRunVO = mapper.map(bulkActionRun, BulkActionRunVO.class);
        bulkActionRunVO.setNodes(levelTreeCacheService.getNodesByIds(bulkActionNodeRepository.findNodeIdsByBulkActionRun(bulkActionRun), bulkActionRun.getFundVersionId()));
        return bulkActionRunVO;
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
     * @param withGroups          zda-li se má ke scénářům přidat i seznam hodnot atributů k založení
     * @return list scénářů nového levelu VO
     */
    public List<ScenarioOfNewLevelVO> createScenarioOfNewLevelList(final List<ScenarioOfNewLevel> scenarioOfNewLevels,
                                                                   final Boolean withGroups,
                                                                   final String ruleCode,
                                                                   final Integer fundId) {
        List<ScenarioOfNewLevelVO> scenarios = new ArrayList<>(scenarioOfNewLevels.size());
        scenarioOfNewLevels.forEach(scenario -> scenarios.add(createScenarioOfNewLevel(scenario, withGroups, ruleCode, fundId)));
        return scenarios;
    }

    /**
     * Vytvoří scénář nového levelu.
     *
     * @param scenarioOfNewLevel scénář nového levelu DO
     * @param withGroups         zda-li se má ke scénářům přidat i seznam hodnot atributů k založení
     * @return scénář nového levelu VO
     */
    public ScenarioOfNewLevelVO createScenarioOfNewLevel(final ScenarioOfNewLevel scenarioOfNewLevel,
                                                         final Boolean withGroups,
                                                         final String ruleCode,
                                                         final Integer fundId) {
        Assert.notNull(scenarioOfNewLevel);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ScenarioOfNewLevelVO scenarioVO = mapper.map(scenarioOfNewLevel, ScenarioOfNewLevelVO.class);
        if (BooleanUtils.isTrue(withGroups)) {
            scenarioVO.setGroups(createItemGroupsNew(ruleCode, fundId, scenarioOfNewLevel.getDescItems()));
        }
        return scenarioVO;
    }

    /**
     * Vytvořit VO pro seznam rule set.
     *
     * @param ruleSets seznam DO
     * @return seznam VO
     */
    public List<RulRuleSetVO> createRuleSetList(final List<RulRuleSet> ruleSets) {
        return createList(ruleSets, RulRuleSetVO.class, null);
    }

    /**
     * Vytvořit VO pro seznam institucí.
     *
     * @param institutions seznam DO
     * @return seznam VO
     */
    public List<ParInstitutionVO> createInstitutionList(final List<ParInstitution> institutions) {
        return createList(institutions, ParInstitutionVO.class, this::createInstitution);
    }

    /**
     * Vytvoří VO instituce.
     *
     * @param institution instituce DO
     * @return instituce VO
     */
    public ParInstitutionVO createInstitution(final ParInstitution institution) {
        Assert.notNull(institution);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ParInstitutionVO institutionVO = mapper.map(institution, ParInstitutionVO.class);
        institutionVO.setName(institution.getParty().getRecord().getRecord());
        institutionVO.setPartyId(institution.getParty().getPartyId());
        return institutionVO;
    }

    /**
     * Vytvoří VO pro seznam typů oprávnění.
     *
     * @param policyTypes   seznam DO
     * @return seznam VO
     */
    public List<RulPolicyTypeVO> createPolicyTypes(final List<RulPolicyType> policyTypes) {
        return createList(policyTypes, RulPolicyTypeVO.class, this::createPolicyType);
    }

    /**
     * Vytvoří VO pro typ oprávnění.
     *
     * @param policyType   DO typu oprávnění
     * @return seznam VO typu oprávnění
     */
    public RulPolicyTypeVO createPolicyType(final RulPolicyType policyType) {
        Assert.notNull(policyType);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulPolicyTypeVO policyTypeVO = mapper.map(policyType, RulPolicyTypeVO.class);
        policyTypeVO.setRuleSetId(policyType.getRuleSet().getRuleSetId());
        return policyTypeVO;
    }

    /**
     * Vytvoří VO pro seznam typů outputů.
     *
     * @param outputTypes   seznam DO
     * @return seznam VO
     */
    public List<RulOutputTypeVO> createOutputTypes(final List<RulOutputType> outputTypes) {
        return createList(outputTypes, RulOutputTypeVO.class, this::createOutputType);
    }

    /**
     * Vytvoří VO pro typ outputů.
     *
     * @param outputType   DO typu outputu
     * @return seznam VO typu outputu
     */
    public RulOutputTypeVO createOutputType(final RulOutputType outputType) {
        Assert.notNull(outputType);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(outputType, RulOutputTypeVO.class);
    }

    public List<BulkActionRunVO> createBulkActionsList(final List<ArrBulkActionRun> allBulkActions) {
        return createList(allBulkActions, BulkActionRunVO.class, this::createBulkActionRun);
    }

    /**
     * Vytvoření VO rozšířeného výstupu.
     *
     * @param outputs seznam DO výstupů
     * @param fundVersion
     * @return seznam VO výstupů
     */
    public List<ArrOutputExtVO> createOutputExtList(final List<ArrOutput> outputs, final ArrFundVersion fundVersion) {
        Assert.notNull(outputs);
        List<ArrOutputExtVO> outputExtList = new ArrayList<>();
        for (ArrOutput output : outputs) {
            ArrOutputExtVO outputExt = createOutputExt(output, fundVersion);
            outputExtList.add(outputExt);
        }
        return outputExtList;
    }

    /**
     * Vytvoření VO rozšířeného výstupu.
     *
     * @param output DO výstup
     * @param fundVersion
     * @return VO výstup
     */
    public ArrOutputExtVO createOutputExt(final ArrOutput output, final ArrFundVersion fundVersion) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrOutputExtVO outputExt = mapper.map(output, ArrOutputExtVO.class);
        outputExt.setOutputDefinition(createArrOutputDefinition(output.getOutputDefinition()));
        outputExt.setCreateDate(mapper.map(output.getCreateChange().getChangeDate(), Date.class));
        outputExt.setLockDate(output.getLockChange() != null ? mapper.map(output.getLockChange().getChangeDate(), Date.class) : null);

        List<ArrNode> nodes = outputService.getNodesForOutput(output);
        List<Integer> nodeIds = nodes.stream().map(ArrNode::getNodeId).collect(Collectors.toList());
        outputExt.getOutputDefinition().setNodes(levelTreeCacheService.getNodesByIds(nodeIds, fundVersion.getFundVersionId()));
        return outputExt;
    }

    /**
     * Vytvoří seznam VO.
     * @param groups vstupní seznam skupin
     * @param initPermissions mají se plnit oprávnění
     * @param initUsers mají se plnit uživatelé?
     * @return seznam VO
     */
    public List<UsrGroupVO> createGroupList(final List<UsrGroup> groups, final boolean initPermissions, final boolean initUsers) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        List<UsrGroupVO> result = new ArrayList<>();
        for (UsrGroup group : groups) {
            result.add(createGroup(group, initPermissions, initUsers));
        }
        return result;
    }

    /**
     * Vytvoří seznam VO.
     * @param permissions vstupní seznam oprávnění
     * @return seznam VO
     */
    public List<UsrPermissionVO> createPermissionList(final List<UsrPermission> permissions) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.mapAsList(permissions, UsrPermissionVO.class);
    }

    /**
     * Vytvoří VO uživatele s návaznými daty.
     * @param user uživatel
     * @return VO
     */
    public UsrUserVO createUser(final UsrUser user) {
        // Hlavní objekt
        MapperFacade mapper = mapperFactory.getMapperFacade();
        UsrUserVO result = mapper.map(user, UsrUserVO.class);

        // Načtení oprávnění
        List<UsrPermission> permissions = permissionRepository.findByUserOrderByPermissionIdAsc(user);
        result.setPermissions(createPermissionList(permissions));

        // Načtení členství ve skupinách
        List<UsrGroup> groups = groupRepository.findByUser(user);
        result.setGroups(createGroupList(groups, false, false));

        return result;
    }

    /**
     * Vytvoří VO skupiny s návaznými daty.
     * @param group skupina
     * @param initPermissions mají se plnit oprávnění
     * @param initUsers mají se plnit uživatelé?
     * @return VO
     */
    public UsrGroupVO createGroup(final UsrGroup group, final boolean initPermissions, final boolean initUsers) {
        // Hlavní objekt
        MapperFacade mapper = mapperFactory.getMapperFacade();
        UsrGroupVO result = mapper.map(group, UsrGroupVO.class);

        // Načtení oprávnění
        List<UsrPermission> permissions = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
        result.setPermissions(createPermissionList(permissions));

        // Přiřazení uživatelé
        List<UsrUser> users = userRepository.findByGroup(group);
        result.setUsers(createUserList(users));

        return result;
    }

    /**
     * Vytvoří VO DMS File
     * @param file DO
     * @return VO
     */
    public DmsFileVO createDmsFile(final DmsFile file) {
        return mapperFactory.getMapperFacade().map(file, DmsFileVO.class);
    }

    /**
     * Vytvoří List VO DMS File
     * @param filesList List DO
     * @return List VO
     */
    public List<DmsFileVO> createDmsFilesList(final List<DmsFile> filesList) {
        return createList(filesList, DmsFileVO.class, this::createDmsFile);
    }

    /**
     * Vytvoří VO Arr File
     * @param file DO
     * @return VO
     */
    public ArrFileVO createArrFile(final ArrFile file) {
        return mapperFactory.getMapperFacade().map(file, ArrFileVO.class);
    }

    /**
     * Vytvoří List VO Arr File
     * @param filesList List DO
     * @return List VO
     */
    public List<ArrFileVO> createArrFilesList(final List<ArrFile> filesList) {
        return createList(filesList, ArrFileVO.class, this::createArrFile);
    }

    /**
     * Vytvoří VO Arr Output
     * @param file DO
     * @return VO
     */
    public ArrOutputFileVO createArrOutputFile(final ArrOutputFile file) {
        return mapperFactory.getMapperFacade().map(file, ArrOutputFileVO.class);
    }

    /**
     * Vytvoří List VO Arr Output
     * @param filesList List DO
     * @return List VO
     */
    public List<ArrOutputFileVO> createArrOutputFilesList(final List<ArrOutputFile> filesList) {
        return createList(filesList, ArrOutputFileVO.class, this::createArrOutputFile);
    }

    public ArrOutputDefinitionVO createArrOutputDefinition(final ArrOutputDefinition outputDefinition) {
        Assert.notNull(outputDefinition);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrOutputDefinitionVO result = mapper.map(outputDefinition, ArrOutputDefinitionVO.class);
        if (outputDefinition.getOutputResult() != null) {
            result.setGeneratedDate(mapper.map(outputDefinition.getOutputResult().getChange().getChangeDate(), Date.class));
        }
        return result;
    }

    public <T extends ArrItem> ArrItemVO createItem(final T item) {

        Assert.notNull(item);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrItemVO descItemVO = mapper.map(item.getItem(), ArrItemVO.class);
        BeanUtils.copyProperties(item, descItemVO);
        descItemVO.setId(item.getItemId());

        Integer specId = (item.getItemSpec() == null) ? null : item.getItemSpec().getItemSpecId();
        descItemVO.setDescItemSpecId(specId);
        return descItemVO;

    }
}
