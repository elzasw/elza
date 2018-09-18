package cz.tacr.elza.controller.config;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.config.ConfigRules;
import cz.tacr.elza.config.ConfigView;
import cz.tacr.elza.config.rules.GroupConfiguration;
import cz.tacr.elza.config.rules.TypeInfo;
import cz.tacr.elza.config.rules.ViewConfiguration;
import cz.tacr.elza.controller.StructureExtensionFundVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFileRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemJsonTableVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemPartyRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStructureVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitidVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemTypeGroupVO;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.packageimport.ItemTypeUpdater;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.service.DaoService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.SettingsService;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;


/**
 * Tovární třída pro vytváření VO objektů a jejich seznamů.
 */
@Service
public class ClientFactoryVO {

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory mapperFactory;

    @Autowired
    private DaoService daoService;

    @Autowired
    private PartyNameRepository partyNameRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationEntityRepository relationEntityRepository;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;
    
    @Autowired
    private ApNameRepository apNameRepository;

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
    private ConfigRules elzaRules;

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ConfigView configView;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ApFactory apFactory;
    
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
     * @param initPermissions mají se plnit oprávnění?
     * @return seznam VO
     */
    public List<UsrUserVO> createUserList(final List<UsrUser> users, final boolean initPermissions) {
        if (users == null) {
            return null;
        }

        return users.stream()
                .map(x -> createUser(x, initPermissions, false))
                .collect(Collectors.toList());
    }

    /**
     * Vytvoření kolekce VO z kolekce DO.
     * @param templates vstup
     * @return seznam VO
     */
    public List<RulTemplateVO> createTemplates(final Collection<RulTemplate> templates){
        Assert.notNull(templates, "Šablony musí být vyplněny");

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
        Assert.notNull(template, "Šablona musí být vyplněna");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(template, RulTemplateVO.class);
    }

    private void nameBuilderHelper(final StringBuilder a, final String b) {
        if (b != null) {
            a.append(b);
            a.append(" ");
        }
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
        result.setPartyNames(createList(partyNameRepository.findByParty(party), ParPartyNameVO.class, this::createParPartyNameDetail));

        List<ParPartyNameVO> collect = result.getPartyNames().stream().filter(ParPartyNameVO::isPrefferedName).collect(Collectors.toList());
        ParPartyNameVO prefferedName = null;
        if (collect != null && !collect.isEmpty()) {
            prefferedName = collect.get(0);
        }

        result.getPartyNames().sort((a, b) -> {
            if (a.isPrefferedName()) {
                return Integer.MIN_VALUE;
            }
            if (b.isPrefferedName()) {
                return Integer.MAX_VALUE;
            }

            return a.getId().compareTo(b.getId());
        });

        if (prefferedName != null) {
            List<ParComplementType> all = complementTypeRepository.findAll();
            Map<Integer, ParComplementType> map = all.stream().collect(Collectors.toMap(ParComplementType::getComplementTypeId, i -> i));

            StringBuilder nameBuilder = new StringBuilder();
            nameBuilderHelper(nameBuilder, prefferedName.getDegreeBefore());
            nameBuilderHelper(nameBuilder, prefferedName.getOtherPart());
            nameBuilderHelper(nameBuilder, prefferedName.getMainPart());

            String roman = null, geoAddon = null, addon = null;

            if (prefferedName.getPartyNameComplements() != null && !prefferedName.getPartyNameComplements().isEmpty()) {
                for (ParPartyNameComplementVO b : prefferedName.getPartyNameComplements()) {
                    ParComplementType type = map.get(b.getComplementTypeId());
                    if (type != null) {
                        if (type.getCode().equals("2")) {
                            addon = b.getComplement();
                        } else if (type.getCode().equals("3")) {
                            roman = b.getComplement();
                        } else if (type.getCode().equals("4")) {
                            geoAddon = b.getComplement();
                        }
                    }
                }
            }

            nameBuilderHelper(nameBuilder, roman);
            nameBuilderHelper(nameBuilder, geoAddon);
            nameBuilderHelper(nameBuilder, addon);

            if (prefferedName.getDegreeAfter() != null && !prefferedName.getDegreeAfter().isEmpty()) {
                nameBuilder.deleteCharAt(nameBuilder.length()-1);
                nameBuilder.append(", ");
                nameBuilder.append(prefferedName.getDegreeAfter());
            }

            result.setName(nameBuilder.toString());
        }

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
            return Collections.emptyList();
        }

        //načtení dat do session
        unitdateRepository.findForFromPartyNameByParties(parties);
        unitdateRepository.findForToPartyNameByParties(parties);
        apAccessPointRepository.findByParties(parties);

        List<ParPartyName> allPartyNames = partyNameRepository.findByPartyIn(parties);
        Map<Integer, List<ParPartyName>> partyNameMap = ElzaTools
                .createGroupMap(allPartyNames, p -> p.getParty().getPartyId());

        MapperFacade mapper = mapperFactory.getMapperFacade();
        Map<Integer, ParPartyVO> partyMap = new LinkedHashMap<>();

        for (final ParParty party : parties) {
            ParPartyVO partyVO = mapper.map(party, ParPartyVO.class);

            partyMap.put(partyVO.getId(), partyVO);


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
            return Collections.emptyList();
        }
        relations.sort(new ParRelation.ParRelationComparator());

        MapperFacade mapper = mapperFactory.getMapperFacade();

        Map<Integer, ParRelationVO> relationVOMap = new LinkedHashMap<>();
        for (final ParRelation relation : relations) {
            relationVOMap.put(relation.getRelationId(), mapper.map(relation, ParRelationVO.class));
        }

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
     * Vytvoří seznam typů formy jména.
     *
     * @param types typy formy jména
     * @return seznam VO typů
     */
    public List<ParPartyNameFormTypeVO> createPartyNameFormTypes(final Collection<ParPartyNameFormType> types) {
        Assert.notNull(types, "Typy forem jmén musí být vyplněny");
        MapperFacade mapper = mapperFactory.getMapperFacade();

        List<ParPartyNameFormTypeVO> result = new LinkedList<>();

        for (final ParPartyNameFormType type : types) {
            result.add(mapper.map(type, ParPartyNameFormTypeVO.class));
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
            return Collections.emptyList();
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
     * @return nalezený nebo vytvořený VO
     */
    public <VO> VO getOrCreateVo(final Integer id,
                                                       final Object source,
                                                       final Map<Integer, VO> processedItemsMap,
                                                       final Class<VO> classType) {
        VO item = processedItemsMap.get(id);


        if (item == null) {
            item = mapperFactory.getMapperFacade().map(source, classType);
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
        Assert.notNull(fund, "AS musí být vyplněn");

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFundVO fundVO = mapper.map(fund, ArrFundVO.class);
        fundVO.setInstitutionId(fund.getInstitution().getInstitutionId());
        
        // TODO: AP scopes on fund VO object needed/used ?
        
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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFundVersionVO fundVersionVO = mapper.map(fundVersion, ArrFundVersionVO.class);
        Date createDate = Date.from(
                fundVersion.getCreateChange().getChangeDate().atZone(ZoneId.systemDefault()).toInstant());
        fundVersionVO.setCreateDate(createDate);
        ConfigView.ViewTitles viewTitles = configView.getViewTitles(fundVersion.getRuleSet().getCode(), fundVersion.getFundId());
        fundVersionVO.setStrictMode(viewTitles.getStrictMode());

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
        Assert.notNull(outputDefinitions, "Musí být vyplněny definice výstupů");

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
        Assert.notNull(outputDefinition, "Definice výstupu musí být vyplněna");
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
        Assert.notNull(outputs, "Výstupy musí být vyplněny");

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
        Assert.notNull(descItemSpec, "Specifikace atributu musí být vyplněna");
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
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
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
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
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
    public <T extends ArrItem> ArrItemVO createItem(final T item) {
        Assert.notNull(item, "Hodnota musí být vyplněna");
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ArrItemVO itemVO;
        ArrData data = item.getData();
		String code = item.getItemType().getDataType().getCode();
        if (data instanceof ArrDataText) {
			switch (code) {
                case "TEXT":
                    itemVO = new ArrItemTextVO();
                    ((ArrItemTextVO) itemVO).setValue(((ArrDataText)data).getValue());
                    break;
                case "FORMATTED_TEXT":
                    itemVO = new ArrItemFormattedTextVO();
                    ((ArrItemFormattedTextVO) itemVO).setValue(((ArrDataText)data).getValue());
                    break;
                default:
                    throw new NotImplementedException();
            }
        } else {
            itemVO = mapper.map(data, ArrItemVO.class);
        }

        if (itemVO == null) {
            switch (code) {
                case "INT": itemVO = new ArrItemIntVO(); break;
                case "STRING": itemVO = new ArrItemStringVO(); break;
                case "TEXT": itemVO = new ArrItemTextVO(); break;
                case "UNITDATE": itemVO = new ArrItemUnitdateVO(); break;
                case "UNITID": itemVO = new ArrItemUnitidVO(); break;
                case "FORMATTED_TEXT": itemVO = new ArrItemFormattedTextVO(); break;
                case "COORDINATES": itemVO = new ArrItemCoordinatesVO(); break;
                case "PARTY_REF": itemVO = new ArrItemPartyRefVO(); break;
                case "RECORD_REF": itemVO = new ArrItemRecordRefVO(); break;
                case "DECIMAL": itemVO = new ArrItemDecimalVO(); break;
                case "STRUCTURED": itemVO = new ArrItemStructureVO(); break;
                case "ENUM": itemVO = new ArrItemEnumVO(); break;
                case "FILE_REF": itemVO = new ArrItemFileRefVO(); break;
                case "JSON_TABLE": itemVO = new ArrItemJsonTableVO(); break;
                default: throw  new NotImplementedException(code);
            }
            itemVO.setUndefined(true);
        }

        BeanUtils.copyProperties(item, itemVO);
        itemVO.setId(item.getItemId());

        Integer specId = (item.getItemSpec() == null) ? null : item.getItemSpec().getItemSpecId();
        itemVO.setDescItemSpecId(specId);
        itemVO.setItemTypeId(item.getItemTypeId());

        return itemVO;
    }

    /**
     * Vytvoří seznam atributů.
     *
     * @param items seznam DO atributů
     * @return seznam VO atributů
     */
    public <T extends ArrItem> List<ArrItemVO> createItems(final List<T> items) {
        if (items == null) {
            return null;
        }
        List<ArrItemVO> result = new ArrayList<>(items.size());
        List<ApAccessPoint> apList = new ArrayList<>();
        for (T item : items) {
            ArrData data = item.getData();
            if (data instanceof ArrDataRecordRef) {
                ApAccessPoint ap = ((ArrDataRecordRef) data).getRecord();
                apList.add(ap);
            }
        }

        List<ApAccessPointVO> apListVO = apFactory.createVO(apList);
        Map<Integer, ApAccessPointVO> apMapVO = apListVO.stream()
                .collect(Collectors.toMap(ApAccessPointVO::getId, Function.identity()));

        for (T item : items) {
            ArrItemVO itemVO = createItem(item);
            ArrData data = item.getData();
            if (data instanceof ArrDataRecordRef) {
                ((ArrItemRecordRefVO) itemVO).setRecord(apMapVO.get(((ArrDataRecordRef) data).getRecord().getAccessPointId()));
            }
            result.add(itemVO);
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
        Map<RulItemType, List<ArrItemVO>> itemByType = new HashMap<>();
		if (items != null) {
        // vytvoření VO hodnot atributů
			for (T item : items) {
                List<ArrItemVO> itemList = itemByType.computeIfAbsent(item.getItemType(), k -> new ArrayList<>());
                itemList.add(createItem(item));
			}
        }

        List<ItemTypeDescItemsLiteVO> itemTypeVOList = new ArrayList<>();
        // zjištění použitých typů atributů a jejich převod do VO
        for (RulItemType descItemType : itemByType.keySet()) {
            ItemTypeDescItemsLiteVO itemTypeVO = createDescItemTypeLiteVO(descItemType);
            itemTypeVOList.add(itemTypeVO);
            itemTypeVO.setDescItems(itemByType.get(descItemType));
        }

        // mapování id na kod atributu
        Map<Integer, String> codeToId = new HashMap<>();
        itemByType.keySet().forEach(type -> codeToId.put(type.getItemTypeId(), type.getCode()));

        ViewConfiguration viewConfig = elzaRules.getViewConfiguration(ruleCode, fundId);
    	List<ItemGroupVO> allItemGroups = new ArrayList<>();
        Map<GroupConfiguration, ItemGroupVO> itemGroupVOMap = new HashMap<>();
        // prepare empty groups
        if(viewConfig!=null)
        {
        	for(GroupConfiguration groupConfig: viewConfig.getGroups())
        	{
        		ItemGroupVO groupVo = new ItemGroupVO(groupConfig.getCode());
        		groupVo.setTypes(new ArrayList<>()); // should be removed after moving logic into ItemTypeGroupVO
        		allItemGroups.add(groupVo);
        		itemGroupVOMap.put(groupConfig, groupVo);
        	}
        }
        // prepare default group for all other items
        ItemGroupVO defaultGroupVo = new ItemGroupVO(elzaRules.getDefaultGroupConfigurationCode());
        defaultGroupVo.setTypes(new ArrayList<>()); // should be removed after moving logic into ItemTypeGroupVO
        allItemGroups.add(defaultGroupVo);

        // rozřazení do skupin podle konfigurace
        for (ItemTypeDescItemsLiteVO descItemTypeVO : itemTypeVOList) {
            GroupConfiguration groupConfig = viewConfig.getGroupForType(codeToId.get(descItemTypeVO.getId()));
            ItemGroupVO itemGroupVO;
            if(groupConfig!=null) {
            	itemGroupVO = itemGroupVOMap.get(groupConfig);
            } else {
            	itemGroupVO = defaultGroupVo;
            }

            List<ItemTypeDescItemsLiteVO> itemTypeList = itemGroupVO.getTypes();
            itemTypeList.add(descItemTypeVO);
        }

        // Filter empty groups
        List<ItemGroupVO> itemGroupVOList = allItemGroups.stream().filter(g -> g.getTypes().size()>0).collect(Collectors.toList());

        // seřazení položek ve skupinách
        for (ItemGroupVO itemGroupVO : itemGroupVOList) {
        	boolean ordered = false;

            if(viewConfig!=null) {
            	GroupConfiguration groupConfig = viewConfig.getGroup(itemGroupVO.getCode());
            	if(groupConfig!=null) {
            		List<String> typeInfos = groupConfig.getTypeCodes();
                    // seřazení typů atributů podle konfigurace
                    Collections.sort(itemGroupVO.getTypes(), (left, right) -> Integer
                            .compare(typeInfos.indexOf(codeToId.get(left.getId())), typeInfos.indexOf(codeToId.get(
                                    right.getId()))));
                    ordered = true;
            	}
            }

            if (!ordered) {
                // seřazení typů atributů podle viewOrder
                Collections.sort(itemGroupVO.getTypes(), (left, right) -> Integer
                        .compare(left.getViewOrder(), right.getViewOrder()));
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
     * Vytvoření seznamu rozšířených typů, ignoruje nemožné typy.
     *
     * @param fundId    identifikátor AS
     * @param ruleCode  kód pravidel
     * @param itemTypes seznam typů hodnot atributů
     * @return seznam skupin s typy hodnot atributů
     */
    public List<ItemTypeLiteVO> createItemTypes(final String ruleCode, final Integer fundId, final List<RulItemTypeExt> itemTypes) {

        List<ItemTypeLiteVO> itemTypeExtList = createList(itemTypes, ItemTypeLiteVO.class, this::createItemTypeLite);

        Map<Integer, String> codeToId = new HashMap<>();

        itemTypes.forEach(type -> codeToId.put(type.getItemTypeId(), type.getCode()));

        // načtený globální oblíbených
        List<UISettings> favoritesItemTypes = settingsService.getGlobalSettings(UISettings.SettingsType.FAVORITE_ITEM_SPECS, UISettings.EntityType.ITEM_TYPE);

        // typeId -> list<specId>
        // naplnění mapy podle oblíbených z nastavení
        Map<Integer, List<Integer>> typeSpecsMap = new HashMap<>();
        for (UISettings favoritesItemType : favoritesItemTypes) {
            SettingFavoriteItemSpecs setting = (SettingFavoriteItemSpecs) PackageService.convertSetting(favoritesItemType, itemTypeRepository);
            if (CollectionUtils.isNotEmpty(setting.getFavoriteItems())) {
                List<RulItemSpec> itemSpecs = itemSpecRepository.findOneByCodes(setting.getFavoriteItems().stream()
                        .map(SettingFavoriteItemSpecs.FavoriteItem::getValue).collect(Collectors.toList()));
                List<Integer> itemSpecsIds = new ArrayList<>(itemSpecs.size());
                for (RulItemSpec itemSpec : itemSpecs) {
                    itemSpecsIds.add(itemSpec.getItemSpecId());
                }
                typeSpecsMap.put(favoritesItemType.getEntityId(), itemSpecsIds);
            }
        }

        // Prepare list of groups
        ViewConfiguration viewConfig = elzaRules.getViewConfiguration(ruleCode, fundId);
        Map<GroupConfiguration, ItemTypeGroupVO> itemTypeGroupVOMap = new HashMap<>();

        // prepare empty groups
        if (viewConfig != null) {
            for (GroupConfiguration groupConfig : viewConfig.getGroups()) {
                ItemTypeGroupVO groupVo = new ItemTypeGroupVO(groupConfig.getCode(), groupConfig.getName());
                groupVo.setTypes(new ArrayList<>()); // should be removed after moving logic into ItemTypeGroupVO
                itemTypeGroupVOMap.put(groupConfig, groupVo);
            }
        }
        // prepare default group for all other items
        ItemTypeGroupVO defaultGroupVo = new ItemTypeGroupVO(elzaRules.getDefaultGroupConfigurationCode(), null);
        defaultGroupVo.setTypes(new ArrayList<>()); // should be removed after moving logic into ItemTypeGroupVO

        for (ItemTypeLiteVO itemTypeVO : itemTypeExtList) {

            // získání a vyplnění oblíbených specifikací u typu
            List<Integer> favoriteSpecIds = typeSpecsMap.get(itemTypeVO.getId());
            itemTypeVO.setFavoriteSpecIds(favoriteSpecIds);

            TypeInfo typeInfo = null;
            ItemTypeGroupVO itemTypeGroupVO;

            String itemTypeCode = codeToId.get(itemTypeVO.getId());
            GroupConfiguration groupConfig = null;
            if (viewConfig != null) {
                groupConfig = viewConfig.getGroupForType(itemTypeCode);
            }
            if (groupConfig != null) {
                itemTypeGroupVO = itemTypeGroupVOMap.get(groupConfig);
                // get type info
                typeInfo = groupConfig.getTypeInfo(itemTypeCode);
            } else {
                itemTypeGroupVO = defaultGroupVo;
            }

            // set width from type info
            Integer width = 1;
            if(typeInfo != null && typeInfo.getWidth() != null) {
            	width = typeInfo.getWidth();
            }

            itemTypeVO.setWidth(width);

            List<ItemTypeLiteVO> itemTypeList = itemTypeGroupVO.getTypes();
            itemTypeList.add(itemTypeVO);
        }

        // remove empty groups and return result
        return itemTypeExtList.stream()
                .filter(s -> s.getType() > 0) // ignorují se nemožné
                .collect(Collectors.toList());
    }

    private ItemTypeLiteVO createItemTypeLite(final RulItemTypeExt itemTypeExt) {
        Assert.notNull(itemTypeExt, "Typ hodnoty atributu musí být vyplněn");
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
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RulDescItemTypeExtVO descItemTypeVO = mapper.map(descItemType, RulDescItemTypeExtVO.class);
        descItemTypeVO.setDataTypeId(descItemType.getDataType().getDataTypeId());
        descItemTypeVO.setItemSpecsTree(createTree(descItemType.getRulItemSpecList()));
        return descItemTypeVO;
    }

    /**
     * Vytvoří strom z kategorií u specifikací. Např. pro jazyky.
     *
     * @param rulItemSpecList
     * @return
     */
    private List<TreeItemSpecsItem> createTree(final List<RulItemSpecExt> rulItemSpecList) {
        List<TreeItemSpecsItem> result = new ArrayList<>();

        String[] categories;
        Integer specId;

        List<TreeItemSpecsItem> listLastTemp = new ArrayList<>();

        // procházím všechny specifikace
        for (RulItemSpecExt rulItemSpecExt : rulItemSpecList) {

            // pokud specifikace obsahuje kategorii
            if (StringUtils.isNotEmpty(rulItemSpecExt.getCategory())) {
                categories = rulItemSpecExt.getCategory().split("\\" + ItemTypeUpdater.CATEGORY_SEPARATOR);
                specId = rulItemSpecExt.getItemSpecId();

                // sestavím porovnávací vektor kategorií
                List<TreeItemSpecsItem> listTemp = new ArrayList<>();
                for (String category : categories) {
                    TreeItemSpecsItem treeItemSpecsItem = new TreeItemSpecsItem();
                    treeItemSpecsItem.setType(TreeItemSpecsItem.Type.GROUP);
                    treeItemSpecsItem.setName(category);
                    listTemp.add(treeItemSpecsItem);
                }

                // porovnám vektor s předchozí položkou a získám minimální společný index
                int index = findIndex(listTemp, listLastTemp);

                TreeItemSpecsItem parent;

                if (index < 0) { // index je záporný, nemá nic společného, založím úplně nový řádek
                    parent = listTemp.get(0);
                    result.add(parent);
                    listTemp = listTemp.subList(1, listTemp.size());
                    listLastTemp = new ArrayList<>();
                    listLastTemp.add(parent);
                } else { // vektory mají n+1 shodných položek
                    parent = listLastTemp.get(index);
                    listTemp = listTemp.subList(index + 1, listTemp.size());
                    listLastTemp = listLastTemp.subList(0, index + 1);
                }

                // procházím vektor a sestavuji nový podstrom
                for (TreeItemSpecsItem treeItemSpecsItem : listTemp) {
                    List<TreeItemSpecsItem> children = parent.getChildren();
                    if (children == null) {
                        children = new ArrayList<>();
                        parent.setChildren(children);
                    }
                    children.add(treeItemSpecsItem);
                    parent = treeItemSpecsItem;
                    listLastTemp.add(treeItemSpecsItem);
                }

                // vytvořím a zařadím list (samotnou položku)
                List<TreeItemSpecsItem> children = parent.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                    parent.setChildren(children);
                }

                TreeItemSpecsItem treeItemSpecsItem = new TreeItemSpecsItem();
                treeItemSpecsItem.setType(TreeItemSpecsItem.Type.ITEM);
                treeItemSpecsItem.setSpecId(specId);

                children.add(treeItemSpecsItem);

            }
        }

        return result;
    }

    /**
     * Vyhledá společný index dat při porovnání dvou polí.
     *
     * @param list1 první porovnávané pole
     * @param list2 druhé porovnávané pole
     * @return minimální společný index
     */
    private int findIndex(final List<TreeItemSpecsItem> list1,
                          final List<TreeItemSpecsItem> list2) {
        int min = Math.min(list1.size(), list2.size());
        for (int i = 0; i < min; i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return i - 1;
            }
        }
        return min - 1;
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
        if (node == null) {
            throw new ObjectNotFoundException("Není vyplněna JP", ArrangementCode.NODE_NOT_FOUND);
        }
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
            result.add(ArrNodeVO.valueOf(node));
        }
        return result;
    }

    /**
     * Vytvoření informace o validace stavu JP.
     * @param nodeConformity    stav validace
     * @return  VO stavu validace
     */
    public NodeConformityVO createNodeConformity(final ArrNodeConformityExt nodeConformity) {
        Assert.notNull(nodeConformity, "Musí být vyplněno");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        NodeConformityVO nodeConformityVO = mapper.map(nodeConformity, NodeConformityVO.class);
        return nodeConformityVO;
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
        Assert.notNull(bulkAction, "Nastavení hromadné akce musí být vyplněno");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(bulkAction, BulkActionVO.class);
    }

    public BulkActionRunVO createBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        Assert.notNull(bulkActionRun, "Běh hromatných akcí musí být vyplněn");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(bulkActionRun, BulkActionRunVO.class);
    }

    public BulkActionRunVO createBulkActionRunWithNodes(final ArrBulkActionRun bulkActionRun) {
        Assert.notNull(bulkActionRun, "Běh hromatných akcí musí být vyplněn");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        BulkActionRunVO bulkActionRunVO = mapper.map(bulkActionRun, BulkActionRunVO.class);
        bulkActionRunVO.setNodes(levelTreeCacheService.getNodesByIds(bulkActionNodeRepository.findNodeIdsByBulkActionRun(bulkActionRun), bulkActionRun.getFundVersionId()));
        return bulkActionRunVO;
    }

    public List<ArrNodeRegisterVO> createRegisterLinkList(final List<ArrNodeRegister> registerLinks) {
        return createList(registerLinks, ArrNodeRegisterVO.class, this::createRegisterLink);
    }

    public ArrNodeRegisterVO createRegisterLink(final ArrNodeRegister nodeRegister) {
        Assert.notNull(nodeRegister, "Rejstříkové heslo musí být vyplněno");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrNodeRegisterVO nodeRegisterVO = mapper.map(nodeRegister, ArrNodeRegisterVO.class);
        nodeRegisterVO.setNodeId(nodeRegister.getNode().getNodeId());
        nodeRegisterVO.setValue(nodeRegister.getRecord().getAccessPointId());
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
        Assert.notNull(scenarioOfNewLevel, "Scénáře musí být vyplněny");
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
        Assert.notNull(institution, "Instituce musí být vyplněny");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ParInstitutionVO institutionVO = mapper.map(institution, ParInstitutionVO.class);
        institutionVO.setPartyId(institution.getPartyId());
        ApName prefName = apNameRepository.findPreferredNameByPartyId(institution.getPartyId());
        institutionVO.setName(prefName.getFullName());
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
        Assert.notNull(policyType, "Typ oprávnění musí být vyplněno");
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
        Assert.notNull(outputType, "Typ výstupu musí být vyplněno");
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
        Assert.notNull(outputs, "Výstupy musí být vyplněny");
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

        List<ArrNodeOutput> nodes = outputServiceInternal.getOutputNodes(output.getOutputDefinition(), fundVersion.getLockChange());
        List<Integer> nodeIds = nodes.stream().map(ArrNodeOutput::getNodeId).collect(Collectors.toList());
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
    public List<UsrPermissionVO> createPermissionList(final List<UsrPermission> permissions, Class<?> targetEntity) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        Map<Object, Object> map = new HashMap<>();
        map.put("targetEntity", targetEntity);
        MappingContext context = new MappingContext(map);
        return mapper.mapAsList(permissions, UsrPermissionVO.class, context);
    }

    /**
     * Vytvoří VO uživatele s návaznými daty.
     * @param user uživatel
     * @param initPermissions mají se plnit oprávnění?
     * @param initGroups mají se plnit skuipny?
     * @return VO
     */
    public UsrUserVO createUser(final UsrUser user, final boolean initPermissions, final boolean initGroups) {
        // Hlavní objekt
        MapperFacade mapper = mapperFactory.getMapperFacade();
        UsrUserVO result = mapper.map(user, UsrUserVO.class);

        // Načtení oprávnění
        if (initPermissions) {
//        List<UsrPermission> permissions = permissionRepository.findByUserOrderByPermissionIdAsc(user);
            List<UsrPermission> permissions = permissionRepository.getAllPermissionsWithGroups(user);
            result.setPermissions(createPermissionList(permissions, UsrUser.class));
        }

        // Načtení členství ve skupinách
        if (initGroups) {
            List<UsrGroup> groups = groupRepository.findByUser(user);
            result.setGroups(createGroupList(groups, false, false));
        }

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
        if (initPermissions) {
            List<UsrPermission> permissions = permissionRepository.findByGroupOrderByPermissionIdAsc(group);
            result.setPermissions(createPermissionList(permissions, UsrGroup.class));
        }

        // Přiřazení uživatelé
        if (initUsers) {
            List<UsrUser> users = userRepository.findByGroup(group);
            result.setUsers(createUserList(users, false));
        }

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
        Assert.notNull(outputDefinition, "Definice výstupu musí být vyplněna");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrOutputDefinitionVO result = mapper.map(outputDefinition, ArrOutputDefinitionVO.class);
        if (outputDefinition.getOutputResult() != null) {
            result.setGeneratedDate(mapper.map(outputDefinition.getOutputResult().getChange().getChangeDate(), Date.class));
        }
        return result;
    }

    /**
     * Vytvoří {@link ParRelationTypeVO}. Pokud je předán {@link ParPartyTypeRelation} a obsahuje jméno,
     * tak bude nastaveno do výsledného objektu.
     *
     * @return VO
     */
    public ParRelationTypeVO createParRelationType(final ParRelationType relationType, final ParPartyTypeRelation partyTypeRelation,
            final Map<Integer, ParRelationTypeVO> relationTypeVoMap) {
        ParRelationTypeVO parRelationTypeVO = getOrCreateVo(relationType.getRelationTypeId(), relationType, relationTypeVoMap,
                ParRelationTypeVO.class);

        if (partyTypeRelation != null && StringUtils.isNotBlank(partyTypeRelation.getName())) {
            parRelationTypeVO = mapperFactory.getMapperFacade().map(relationType,  ParRelationTypeVO.class); // snad to nebude dělat neplechu když budou různá VO se stejným id
            parRelationTypeVO.setName(partyTypeRelation.getName());
        }
        return parRelationTypeVO;
    }

    public <T, R> R createSimpleEntity(final T entity, final Class<R> clazz) {
        if (entity == null) {
            return null;
        }
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(entity, clazz);
    }

    public <T, R> List<R> createSimpleEntity(final Collection<T> entity, final Class<R> clazz) {
        if (entity == null) {
            return null;
        }
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.mapAsList(entity, clazz);
    }

    public List<ArrRequestVO> createRequest(final Collection<ArrRequest> requests, final boolean detail, final ArrFundVersion fundVersion) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        List<ArrRequestVO> requestVOList = new ArrayList<>(requests.size());
        Set<ArrDigitizationRequest> requestForNodes = new HashSet<>();
        Set<ArrDaoRequest> requestForDaos = new HashSet<>();
        Set<ArrDaoLinkRequest> requestForDaoLinks = new HashSet<>();

        Map<ArrRequest, ArrRequestQueueItem> requestQueuedMap = new HashMap<>();
        List<ArrRequestQueueItem> requestQueueItems = CollectionUtils.isEmpty(requests) ? Collections.emptyList() : requestQueueItemRepository.findByRequest(requests);
        for (ArrRequestQueueItem requestQueueItem : requestQueueItems) {
            requestQueuedMap.put(requestQueueItem.getRequest(), requestQueueItem);
        }

        for (ArrRequest request : requests) {
            prepareRequest(requestForNodes, requestForDaos, requestForDaoLinks, request);
        }

        Map<ArrDigitizationRequest, Integer> countNodesRequestMap = Collections.emptyMap();
        Map<ArrDigitizationRequest, List<TreeNodeVO>> nodesRequestMap = new HashMap<>();

        if (requestForNodes.size() > 0) {
            if (detail) {
                countNodesRequestMap = fillDigitizationDetailParams(fundVersion, requestForNodes, nodesRequestMap);
            } else {
                countNodesRequestMap = digitizationRequestNodeRepository.countByRequests(requestForNodes);
            }
        }

        Map<ArrDaoRequest, Integer> countDaosRequestMap = Collections.emptyMap();
        Map<ArrDaoRequest, List<ArrDao>> daosRequestMap = new HashMap<>();

        if (requestForDaos.size() > 0) {
            if (detail) {
                countDaosRequestMap = fillDaoDetailParams(fundVersion, requestForDaos, daosRequestMap);
            } else {
                countDaosRequestMap = daoRequestDaoRepository.countByRequests(requestForDaos);
            }
        }

        Map<String, TreeNodeVO> codeTreeNodeClientMap = Collections.emptyMap();
        if (requestForDaoLinks.size() > 0) {
            if (detail) {
                codeTreeNodeClientMap = fillDaoLinkDetailParams(fundVersion, requestForDaoLinks);
            }
        }

        for (ArrRequest request : requests) {
            ArrRequestVO requestVO;
            requestVO = createRequestVO(countNodesRequestMap, nodesRequestMap, countDaosRequestMap, daosRequestMap, codeTreeNodeClientMap, request, detail, fundVersion);
            convertRequest(mapper, request, requestQueuedMap.get(request), requestVO);
            requestVOList.add(requestVO);
        }
        return requestVOList;
    }

    public ArrRequestVO createRequest(final ArrRequest request, final boolean detail, final ArrFundVersion fundVersion) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        Set<ArrDigitizationRequest> requestForNodes = new HashSet<>();
        Set<ArrDaoRequest> requestForDaos = new HashSet<>();
        Set<ArrDaoLinkRequest> requestForDaoLinks = new HashSet<>();

        ArrRequestQueueItem requestQueueItem = requestQueueItemRepository.findByRequest(request);

        prepareRequest(requestForNodes, requestForDaos, requestForDaoLinks, request);

        Map<ArrDigitizationRequest, Integer> countNodesRequestMap = Collections.emptyMap();
        Map<ArrDigitizationRequest, List<TreeNodeVO>> nodesRequestMap = new HashMap<>();

        if (requestForNodes.size() > 0) {
            if (detail) {
                countNodesRequestMap = fillDigitizationDetailParams(fundVersion, requestForNodes, nodesRequestMap);
            } else {
                countNodesRequestMap = digitizationRequestNodeRepository.countByRequests(requestForNodes);
            }
        }

        Map<ArrDaoRequest, Integer> countDaosRequestMap = Collections.emptyMap();
        Map<ArrDaoRequest, List<ArrDao>> daosRequestMap = new HashMap<>();

        if (requestForDaos.size() > 0) {
            if (detail) {
                countDaosRequestMap = fillDaoDetailParams(fundVersion, requestForDaos, daosRequestMap);
            } else {
                countDaosRequestMap = daoRequestDaoRepository.countByRequests(requestForDaos);
            }
        }

        Map<String, TreeNodeVO> codeTreeNodeClientMap = Collections.emptyMap();
        if (requestForDaoLinks.size() > 0) {
            if (detail) {
                codeTreeNodeClientMap = fillDaoLinkDetailParams(fundVersion, requestForDaoLinks);
            }
        }

        ArrRequestVO requestVO;
        requestVO = createRequestVO(countNodesRequestMap, nodesRequestMap, countDaosRequestMap, daosRequestMap, codeTreeNodeClientMap, request, detail, fundVersion);
        convertRequest(mapper, request, requestQueueItem, requestVO);

        return requestVO;
    }

    private Map<String, TreeNodeVO> fillDaoLinkDetailParams(final ArrFundVersion fundVersion, final Set<ArrDaoLinkRequest> requestForDaoLinks) {
        Map<String, TreeNodeVO> result = new HashMap<>();

        Set<String> didCodes = new HashSet<>();
        for (ArrDaoLinkRequest requestForDaoLink : requestForDaoLinks) {
            didCodes.add(requestForDaoLink.getDidCode());
        }

        List<ArrNode> nodes = nodeRepository.findByUuid(didCodes);
        Set<Integer> nodeIds = new HashSet<>();
        for (ArrNode node : nodes) {
            nodeIds.add(node.getNodeId());
        }
        Map<Integer, TreeNodeVO> treeNodeClientMap = levelTreeCacheService.getNodesByIds(nodeIds, fundVersion.getFundVersionId()).stream().collect(Collectors.toMap(TreeNodeVO::getId, Function.identity()));

        for (ArrNode node : nodes) {
            result.put(node.getUuid(), treeNodeClientMap.get(node.getNodeId()));
        }

        return result;
    }

    private Map<ArrDaoRequest, Integer> fillDaoDetailParams(final ArrFundVersion fundVersion,
                                                            final Set<ArrDaoRequest> requestForDaos,
                                                            final Map<ArrDaoRequest, List<ArrDao>> daosRequestMap) {
        Map<ArrDaoRequest, Integer> countDaosRequestMap;
        countDaosRequestMap = new HashMap<>();
        List<ArrDaoRequestDao> daoRequestDaos = daoRequestDaoRepository.findByDaoRequest(requestForDaos);

        Set<Integer> daoIds = new HashSet<>();
        for (ArrDaoRequestDao daoRequestDao : daoRequestDaos) {
            daoIds.add(daoRequestDao.getDao().getDaoId());
        }

        List<ArrDao> arrDaos = daoRepository.findAll(daoIds);
        Map<Integer, ArrDao> daosMap = new HashMap<>(arrDaos.size());
        for (ArrDao arrDao : arrDaos) {
            daosMap.put(arrDao.getDaoId(), arrDao);
        }

        for (ArrDaoRequestDao daoRequestDao : daoRequestDaos) {
            Integer daoId = daoRequestDao.getDao().getDaoId();
            List<ArrDao> daos = daosRequestMap.get(daoRequestDao.getDaoRequest());
            if (daos == null) {
                daos = new ArrayList<>();
                daosRequestMap.put(daoRequestDao.getDaoRequest(), daos);
            }
            daos.add(daosMap.get(daoId));
        }

        for (Map.Entry<ArrDaoRequest, List<ArrDao>> entry : daosRequestMap.entrySet()) {
            countDaosRequestMap.put(entry.getKey(), entry.getValue().size());
        }
        return countDaosRequestMap;
    }

    private Map<ArrDigitizationRequest, Integer> fillDigitizationDetailParams(final ArrFundVersion fundVersion, final Set<ArrDigitizationRequest> requestForNodes, final Map<ArrDigitizationRequest, List<TreeNodeVO>> nodesRequestMap) {
        Map<ArrDigitizationRequest, Integer> countNodesRequestMap;
        countNodesRequestMap = new HashMap<>();
        List<ArrDigitizationRequestNode> digitizationRequestNodes = digitizationRequestNodeRepository.findByDigitizationRequest(requestForNodes);

        Set<Integer> nodeIds = new HashSet<>();
        for (ArrDigitizationRequestNode digitizationRequestNode : digitizationRequestNodes) {
            nodeIds.add(digitizationRequestNode.getNode().getNodeId());
        }

        Map<Integer, TreeNodeVO> treeNodeClientMap = levelTreeCacheService.getNodesByIds(nodeIds, fundVersion.getFundVersionId()).stream().collect(Collectors.toMap(TreeNodeVO::getId, Function.identity()));

        for (ArrDigitizationRequestNode digitizationRequestNode : digitizationRequestNodes) {
            ArrNode node = digitizationRequestNode.getNode();
            nodeIds.add(node.getNodeId());
            List<TreeNodeVO> treeNodeClients = nodesRequestMap.get(digitizationRequestNode.getDigitizationRequest());
            if (treeNodeClients == null) {
                treeNodeClients = new ArrayList<>();
                nodesRequestMap.put(digitizationRequestNode.getDigitizationRequest(), treeNodeClients);
            }

            TreeNodeVO treeNodeClient = treeNodeClientMap.get(node.getNodeId());
            if (treeNodeClient == null) {
                treeNodeClient = new TreeNodeVO();
                treeNodeClient.setId(node.getNodeId());
                treeNodeClient.setName(node.getUuid());
                treeNodeClient.setReferenceMark(new String[0]);
                treeNodeClient.setReferenceMarkInt(new Integer[0]);
            }

            treeNodeClients.add(treeNodeClient);
        }

        for (Map.Entry<ArrDigitizationRequest, List<TreeNodeVO>> entry : nodesRequestMap.entrySet()) {
            countNodesRequestMap.put(entry.getKey(), entry.getValue().size());
        }
        return countNodesRequestMap;
    }

    private void convertRequest(final MapperFacade mapper,
                                final ArrRequest request,
                                final ArrRequestQueueItem requestQueueItem,
                                final ArrRequestVO requestVO) {
        ArrChange createChange = request.getCreateChange();
        requestVO.setCode(request.getCode());
        requestVO.setId(request.getRequestId());
        requestVO.setExternalSystemCode(request.getExternalSystemCode());
        requestVO.setState(request.getState());
        requestVO.setRejectReason(request.getRejectReason());
        requestVO.setResponseExternalSystem(mapper.map(request.getResponseExternalSystem(), Date.class));
        requestVO.setCreate(mapper.map(createChange.getChangeDate(), Date.class));
        if (requestQueueItem != null) {
            requestVO.setQueued(mapper.map(requestQueueItem.getCreateChange().getChangeDate(), Date.class));
            requestVO.setSend(mapper.map(requestQueueItem.getAttemptToSend(), Date.class));
        }
        requestVO.setUsername(createChange.getUser() == null ? null : createChange.getUser().getUsername());
    }

    private ArrRequestVO createRequestVO(final Map<ArrDigitizationRequest, Integer> countNodesRequestMap,
                                         final Map<ArrDigitizationRequest, List<TreeNodeVO>> nodesRequestMap,
                                         final Map<ArrDaoRequest, Integer> countDaosRequestMap,
                                         final Map<ArrDaoRequest, List<ArrDao>> daosRequestMap,
                                         final Map<String, TreeNodeVO> codeTreeNodeClientMap,
                                         final ArrRequest request,
                                         final boolean detail,
                                         final ArrFundVersion fundVersion) {
        ArrRequestVO requestVO;
        switch (request.getDiscriminator()) {
            case DIGITIZATION: {
                requestVO = new ArrDigitizationRequestVO();
                convertDigitizationRequest((ArrDigitizationRequest) request, (ArrDigitizationRequestVO) requestVO, countNodesRequestMap.get(request), nodesRequestMap.get(request));
                break;
            }

            case DAO: {
                requestVO = new ArrDaoRequestVO();
                convertDaoRequest((ArrDaoRequest) request, (ArrDaoRequestVO) requestVO, countDaosRequestMap.get(request), daosRequestMap.get(request), detail, fundVersion);
                break;
            }

            case DAO_LINK: {
                requestVO = new ArrDaoLinkRequestVO();
                convertDaoLinkRequest((ArrDaoLinkRequest) request, (ArrDaoLinkRequestVO) requestVO, false, fundVersion, codeTreeNodeClientMap);
                break;
            }

            default: {
                throw new IllegalStateException(String.valueOf(request.getDiscriminator()));
            }
        }
        return requestVO;
    }

    private void prepareRequest(final Set<ArrDigitizationRequest> requestForNodes, final Set<ArrDaoRequest> requestForDaos,
                                final Set<ArrDaoLinkRequest> requestForDaoLinks, final ArrRequest request) {
        switch (request.getDiscriminator()) {
            case DIGITIZATION: {
                requestForNodes.add((ArrDigitizationRequest) request);
                break;
            }

            case DAO: {
                requestForDaos.add((ArrDaoRequest) request);
                break;
            }

            case DAO_LINK: {
                requestForDaoLinks.add((ArrDaoLinkRequest) request);
                break;
            }

            default: {
                throw new IllegalStateException(String.valueOf(request.getDiscriminator()));
            }
        }
    }


    private void convertDaoLinkRequest(final ArrDaoLinkRequest request,
                                       final ArrDaoLinkRequestVO requestVO,
                                       final boolean detail,
                                       final ArrFundVersion fundVersion,
                                       final Map<String, TreeNodeVO> codeTreeNodeClientMap) {
        requestVO.setDidCode(request.getDidCode());
        requestVO.setDigitalRepositoryId(request.getDigitalRepository().getExternalSystemId());
        requestVO.setType(request.getType());
        //requestVO.setDao(createDao(request.getDao(), detail, fundVersion));
        requestVO.setNode(codeTreeNodeClientMap.get(request.getDidCode()));
    }

    private void convertDaoRequest(final ArrDaoRequest request,
                                   final ArrDaoRequestVO requestVO,
                                   final Integer daoCount,
                                   final List<ArrDao> daos,
                                   final boolean detail,
                                   final ArrFundVersion fundVersion) {
        requestVO.setDescription(request.getDescription());
        requestVO.setDaosCount(daoCount);
        requestVO.setDigitalRepositoryId(request.getDigitalRepository().getExternalSystemId());
        requestVO.setType(request.getType());
        if (daos != null) {
            requestVO.setDaos(createDaoList(daos, detail, fundVersion));
        }
    }

    private void convertDigitizationRequest(final ArrDigitizationRequest request,
                                            final ArrDigitizationRequestVO requestVO,
                                            final Integer nodeCount,
                                            final List<TreeNodeVO> treeNodeClients) {
        requestVO.setDescription(request.getDescription());
        requestVO.setNodesCount(nodeCount);
        requestVO.setDigitizationFrontdeskId(request.getDigitizationFrontdesk().getExternalSystemId());
        if (treeNodeClients != null) {
            treeNodeClients.sort((o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                }
                for (int i = 0; i < o1.getReferenceMarkInt().length; i++) {
                    if (o1.getReferenceMarkInt().length > i && o2.getReferenceMarkInt().length > i) {
                        if (o1.getReferenceMarkInt()[i] > o2.getReferenceMarkInt()[i]) {
                            return 1;
                        }
                    } else if (o1.getReferenceMarkInt().length > i) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
                return -1;
            });
            requestVO.setNodes(treeNodeClients);
        }
    }

    private ArrRequestQueueItemVO createRequestQueueItem(final MapperFacade mapper, final ArrRequestQueueItem requestQueueItem) {
        ArrRequestQueueItemVO requestQueueItemVO = new ArrRequestQueueItemVO();
        ArrChange createChange = requestQueueItem.getCreateChange();
        requestQueueItemVO.setId(requestQueueItem.getRequestQueueItemId());
        requestQueueItemVO.setCreate(mapper.map(createChange.getChangeDate(), Date.class));
        requestQueueItemVO.setAttemptToSend(mapper.map(requestQueueItem.getAttemptToSend(), Date.class));
        requestQueueItemVO.setError(requestQueueItem.getError());
        requestQueueItemVO.setUsername(createChange.getUser() == null ? null : createChange.getUser().getUsername());
        return requestQueueItemVO;
    }

    public List<ArrRequestQueueItemVO> createRequestQueueItem(final List<ArrRequestQueueItem> requestQueueItems) {
        if (requestQueueItems == null) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();

        List<ArrRequestQueueItemVO> result = new ArrayList<>(requestQueueItems.size());
        Map<Integer, ArrRequestVO> requestMap = new HashMap<>();

        Map<ArrFund, List<ArrRequest>> requestList = new HashMap<>(requestQueueItems.size());
        for (ArrRequestQueueItem requestQueueItem : requestQueueItems) {
            ArrFund fund = requestQueueItem.getRequest().getFund();
            List<ArrRequest> arrRequests = requestList.get(fund);
            if (arrRequests == null) {
                arrRequests = new ArrayList<>();
                requestList.put(fund, arrRequests);
            }

            arrRequests.add(requestQueueItem.getRequest());
        }

        for (Map.Entry<ArrFund, List<ArrRequest>> arrFundListEntry : requestList.entrySet()) {
            ArrFund key = arrFundListEntry.getKey();
            for (ArrFundVersion arrFundVersion : key.getVersions()) {
                if (arrFundVersion.getLockChange() == null) {
                    List<ArrRequestVO> request = createRequest(arrFundListEntry.getValue(), false, arrFundVersion);
                    for (ArrRequestVO requestVO : request) {
                        requestMap.put(requestVO.getId(), requestVO);
                    }
                    break;
                }
            }
        }

        for (ArrRequestQueueItem requestQueueItem : requestQueueItems) {
            ArrRequestQueueItemVO requestQueueItemVO = createRequestQueueItem(mapper, requestQueueItem);
            requestQueueItemVO.setRequest(requestMap.get(requestQueueItem.getRequest().getRequestId()));
            result.add(requestQueueItemVO);
        }

        return result;
    }

    /**
     * Vytvoření VO
     *
     * @param arrDaoList DO ke konverzi
     * @param detail příznak, zda se mají naplnit seznamy na VO, pokud ne, jsou naplněny pouze počty podřízených záznamů v DB
     * @param version
     * @return list VO
     */
    public List<ArrDaoVO> createDaoList(final List<ArrDao> arrDaoList, final boolean detail, final ArrFundVersion version) {
        List<ArrDaoVO> voList = new ArrayList<>();
        for (ArrDao arrDao : arrDaoList) {
            voList.add(createDao(arrDao, detail, version));
        }
        return voList;
    }

    /**
     * Vytvoření VO z DO.
     * @param daoFile do
     * @return VO
     */
    private ArrDaoFileVO createDaoFile(final ArrDaoFile daoFile) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrDaoFileVO result = mapper.map(daoFile, ArrDaoFileVO.class);

        ArrDigitalRepository digitalRepository = daoFile.getDao().getDaoPackage().getDigitalRepository();
        result.setUrl(daoService.getDaoFileUrl(daoFile, digitalRepository));

        return result;
    }

    /**
     * Vytvoření vo z DO
     *
     * @param arrDao DO
     * @param detail příznak, zda se mají naplnit seznamy na VO, pokud ne, jsou naplněny pouze počty podřízených záznamů v DB
     * @param version
     * @return vo
     */
    private ArrDaoVO createDao(final ArrDao arrDao, final boolean detail, final ArrFundVersion version) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrDaoVO vo = mapper.map(arrDao, ArrDaoVO.class);

        ArrDigitalRepository digitalRepository = arrDao.getDaoPackage().getDigitalRepository();

        vo.setUrl(daoService.getDaoUrl(arrDao, digitalRepository));


        final List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaoAndDeleteChangeIsNull(arrDao);
        if (CollectionUtils.isNotEmpty(daoLinkList)) {
            if (daoLinkList.size() > 1) {
                throw new SystemException("Nalezen více než jeden platný link pro arrDao ID=" + arrDao.getDaoId() + ".");
            }
            final ArrDaoLink daoLink = daoLinkList.iterator().next();

            ArrDaoLinkVO daoLinkVo = new ArrDaoLinkVO();
            daoLinkVo.setId(daoLink.getDaoLinkId());
            final List<TreeNodeVO> nodesByIds = levelTreeCacheService.getNodesByIds(Collections.singletonList(daoLink.getNode().getNodeId()), version.getFundVersionId());
            daoLinkVo.setTreeNodeClient(nodesByIds.iterator().next());

            vo.setDaoLink(daoLinkVo);
        }


        if (detail) {
            final List<ArrDaoFile> daoFileList = daoFileRepository.findByDaoAndDaoFileGroupIsNull(arrDao);
            final List<ArrDaoFileVO> daoFileVOList = daoFileList.stream().map(this::createDaoFile).collect(Collectors.toList());
            vo.addAllFile(daoFileVOList);

            final List<ArrDaoFileGroup> daoFileGroups = daoFileGroupRepository.findByDaoOrderByCodeAsc(arrDao);
            final List<ArrDaoFileGroupVO> daoFileGroupVOList = new ArrayList<>();
            for (ArrDaoFileGroup daoFileGroup : daoFileGroups) {
                final ArrDaoFileGroupVO daoFileGroupVO = mapper.map(daoFileGroup, ArrDaoFileGroupVO.class);
                final List<ArrDaoFile> arrDaoFileList = daoFileRepository.findByDaoAndDaoFileGroup(arrDao, daoFileGroup);
                final List<ArrDaoFileVO> groupDaoFileVOList = arrDaoFileList.stream().map(this::createDaoFile).collect(Collectors.toList());
                daoFileGroupVO.setFiles(groupDaoFileVOList);
                daoFileGroupVOList.add(daoFileGroupVO);
            }

            vo.addAllFileGroup(daoFileGroupVOList);
        } else {
            vo.setFileCount(daoFileRepository.countByDaoAndDaoFileGroupIsNull(arrDao));
            vo.setFileGroupCount(daoFileGroupRepository.countByDao(arrDao));
        }
        return vo;
    }

    public ArrayList<ArrDaoPackageVO> createDaoPackageList(final List<ArrDaoPackage> arrDaoList, final Boolean unassigned) {
        ArrayList<ArrDaoPackageVO> result = new ArrayList<>();

        for (ArrDaoPackage arrDaoPackage : arrDaoList) {
            ArrDaoPackageVO vo = createDaoPackage(unassigned, arrDaoPackage);
            result.add(vo);
        }

        return result;
    }

    private ArrDaoPackageVO createDaoPackage(final Boolean unassigned, final ArrDaoPackage arrDaoPackage) {
        ArrDaoPackageVO vo = new ArrDaoPackageVO();
        vo.setId(arrDaoPackage.getDaoPackageId());
        vo.setCode(arrDaoPackage.getCode());

        final ArrDaoBatchInfo daoBatchInfo = arrDaoPackage.getDaoBatchInfo();
        if (daoBatchInfo != null) {
            vo.setBatchInfoCode(daoBatchInfo.getCode());
            vo.setBatchInfoLabel(daoBatchInfo.getLabel());
        }

        long daoCount;
        if (unassigned) {
            daoCount = daoRepository.countByDaoPackageIDAndNotExistsDaoLink(arrDaoPackage.getDaoPackageId());
        } else {
            daoCount = daoRepository.countByDaoPackageID(arrDaoPackage.getDaoPackageId());
        }

        vo.setDaoCount(daoCount);
        return vo;
    }

    public ArrStructureDataVO createStructureData(final ArrStructuredObject structureData) {
        ArrStructureDataVO structureDataVO = new ArrStructureDataVO();
        structureDataVO.id = structureData.getStructuredObjectId();
        structureDataVO.typeCode = structureData.getStructuredType().getCode();
        structureDataVO.value = structureData.getValue();
        structureDataVO.errorDescription = structureData.getErrorDescription();
        structureDataVO.assignable = structureData.getAssignable();
        structureDataVO.state = structureData.getState();
        return structureDataVO;
    }

    public List<ArrStructureDataVO> createStructureDataList(final List<ArrStructuredObject> structureDataList) {
        if (structureDataList == null) {
            return null;
        }
        return structureDataList.stream().map(this::createStructureData).collect(Collectors.toList());
    }

    public List<StructureExtensionFundVO> createStructureExtensionFund(final List<RulStructuredTypeExtension> allStructureExtensions,
                                                                       final List<RulStructuredTypeExtension> structureExtensions) {
        List<StructureExtensionFundVO> result = new ArrayList<>(allStructureExtensions.size());
        allStructureExtensions.forEach(se -> {
            StructureExtensionFundVO structureExtensionFund = createStructureExtensionFund(se);
            structureExtensionFund.active = structureExtensions.contains(se);
            result.add(structureExtensionFund);
        });
        return result;
    }

    private StructureExtensionFundVO createStructureExtensionFund(final RulStructuredTypeExtension structureExtension) {
        StructureExtensionFundVO structureExtensionFundVO = new StructureExtensionFundVO();
        structureExtensionFundVO.id = structureExtension.getStructuredTypeExtensionId();
        structureExtensionFundVO.code = structureExtension.getCode();
        structureExtensionFundVO.name = structureExtension.getName();
        structureExtensionFundVO.active = false;
        return structureExtensionFundVO;
    }

    public List<LanguageVO> createLanguages(final Collection<SysLanguage> languages) {
        if (languages == null) {
            return null;
        }
        return languages.stream().map(this::createLanguage).collect(Collectors.toList());
    }

    public LanguageVO createLanguage(final SysLanguage language) {
        if (language == null) {
            return null;
        }
        LanguageVO languageVO = new LanguageVO();
        languageVO.setId(language.getLanguageId());
        languageVO.setCode(language.getCode());
        languageVO.setName(language.getName());
        return languageVO;
    }

    public SysExternalSystemVO createExtSystem(SysExternalSystem extSystem) {
        // AP external system is newly created through factory without mapper
        if (extSystem instanceof ApExternalSystem) {
            return ApExternalSystemVO.newInstance((ApExternalSystem) extSystem);
        }
        return createSimpleEntity(extSystem, SysExternalSystemVO.class);
    }

    public SysExternalSystemSimpleVO createExtSystemSimple(SysExternalSystem extSystem) {
        // AP external system is newly created through factory without mapper
        if (extSystem instanceof ApExternalSystem) {
            return ApExternalSystemSimpleVO.newInstance((ApExternalSystem) extSystem);
        }
        return createSimpleEntity(extSystem, SysExternalSystemSimpleVO.class);
    }
}
