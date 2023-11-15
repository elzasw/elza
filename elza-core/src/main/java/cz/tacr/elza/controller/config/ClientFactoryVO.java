package cz.tacr.elza.controller.config;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.config.ConfigRules;
import cz.tacr.elza.config.ConfigView;
import cz.tacr.elza.config.rules.GroupConfiguration;
import cz.tacr.elza.config.rules.TypeInfo;
import cz.tacr.elza.config.rules.ViewConfiguration;
import cz.tacr.elza.config.view.ViewTitles;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.factory.WfFactory;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApExternalSystemSimpleVO;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ArrDaoFileGroupVO;
import cz.tacr.elza.controller.vo.ArrDaoFileVO;
import cz.tacr.elza.controller.vo.ArrDaoLinkRequestVO;
import cz.tacr.elza.controller.vo.ArrDaoLinkVO;
import cz.tacr.elza.controller.vo.ArrDaoPackageVO;
import cz.tacr.elza.controller.vo.ArrDaoRequestVO;
import cz.tacr.elza.controller.vo.ArrDaoVO;
import cz.tacr.elza.controller.vo.ArrDigitizationRequestVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrOutputVO;
import cz.tacr.elza.controller.vo.ArrRequestQueueItemVO;
import cz.tacr.elza.controller.vo.ArrRequestVO;
import cz.tacr.elza.controller.vo.BulkActionRunVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.Fund;
import cz.tacr.elza.controller.vo.FundDetail;
import cz.tacr.elza.controller.vo.NodeConformityVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulExportFilterVO;
import cz.tacr.elza.controller.vo.RulOutputFilterVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.StructureExtensionFundVO;
import cz.tacr.elza.controller.vo.SysExternalSystemSimpleVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.controller.vo.TreeItemSpecsItem;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.UISettingsVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemBitVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFileRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemJsonTableVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStructureVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitidVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUriRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemTypeGroupVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDaoRequestDao;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputFilter;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.packageimport.ItemTypeUpdater;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs.FavoriteItem;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.DaoService;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.attachment.AttachmentService;
import cz.tacr.elza.ws.types.v1.Items;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;

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
    private DaoSyncService daoSyncService;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ConfigRules elzaRules;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

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
    private ConfigView configView;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ApFactory apFactory;

    @Autowired
    private WfFactory wfFactory;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private ApIndexRepository indexRepository;


    /**
     * Vytvoření nastavení.
     *
     * @param settings nastavení
     * @return
     */
    public List<UISettingsVO> createSettingsList(final List<UISettings> settings) {
        if (settings == null) {
            return null;
        }
        return settings.stream().map(UISettingsVO::newInstance).collect(Collectors.toList());
    }

    /**
     * Vytvoří seznam VO.
     *
     * @param users           vstupní seznam uživatelů
     * @param initPermissions mají se plnit oprávnění?
     * @return seznam VO
     */
    public List<UsrUserVO> createUserList(final Collection<UsrUser> users, final boolean initPermissions) {
        if (users == null) {
            return null;
        }

        return users.stream()
                .map(x -> createUser(x, initPermissions, false))
                .collect(Collectors.toList());
    }

    /**
     * Vytvoření kolekce VO z kolekce DO.
     *
     * @param templates vstup
     * @return seznam VO
     */
    public List<RulTemplateVO> createTemplates(final Collection<RulTemplate> templates) {
        Assert.notNull(templates, "Šablony musí být vyplněny");

        List<RulTemplateVO> result = new ArrayList<>();
        for (RulTemplate template : templates) {
            result.add(createTemplate(template));
        }
        return result;
    }

    /**
     * Vytvoření VO z DO.
     *
     * @param template DO
     * @return VO
     */
    public RulTemplateVO createTemplate(final RulTemplate template) {
        Assert.notNull(template, "Šablona musí být vyplněna");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(template, RulTemplateVO.class);
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
     * @param fund            archivní soubor
     * @param includeVersions true - budou do objektu donačteny všechny jeho verze, false- verze nebudou donačteny
     * @param user            přihlášený uživatel
     * @return VO
     */
    public ArrFundVO createFundVO(final ArrFund fund, final boolean includeVersions, UserDetail user) {
        Assert.notNull(fund, "AS musí být vyplněn");

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFundVO fundVO = mapper.map(fund, ArrFundVO.class);
        fundVO.setInstitutionId(fund.getInstitution().getInstitutionId());

        StaticDataProvider staticData = staticDataService.getData();

        Set<ApScope> apScopes = scopeRepository.findByFund(fund);
        fundVO.setApScopes(FactoryUtils.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData)));

        if (includeVersions) {

            List<ArrFundVersion> versions = fundVersionRepository
                    .findVersionsByFundIdOrderByCreateDateDesc(fund.getFundId());

            List<ArrFundVersionVO> versionVOs = new ArrayList<>(versions.size());
            for (ArrFundVersion version : versions) {
                ArrFundVersionVO fundVersion = createFundVersion(version, user);
                versionVOs.add(fundVersion);
            }
            fundVO.setVersions(versionVOs);

            fundVO.setValidNamedOutputs(createOutputList(outputRepository.findValidOutputByFund(fund)));
        }

        return fundVO;
    }

    public OffsetDateTime convertDateTime(LocalDateTime localDateTime) {
        ZoneOffset offset = OffsetDateTime.now().getOffset();
        return localDateTime.atOffset(offset);
    }

    /**
     * Vytvoření ArrFund a načtení verzí.
     *
     * @param arrFund archivní soubor     *
     * @param user    přihlášený uživatel
     * @return VO
     */
    public Fund createFund(final ArrFund arrFund, UserDetail user) {
        Assert.notNull(arrFund, "AS musí být vyplněn");

        Fund fund = new Fund();
        fund.setId(arrFund.getFundId());

        // get current offset
        OffsetDateTime createDateTime = convertDateTime(arrFund.getCreateDate());
        fund.setCreateDate(createDateTime);

        fund.setInstitutionIdentifier(arrFund.getInstitution().getInternalCode());
        fund.setFundNumber(arrFund.getFundNumber());
        fund.setName(arrFund.getName());
        fund.setInternalCode(arrFund.getInternalCode());
        fund.setUnitdate(arrFund.getUnitdate());
        fund.setMark(arrFund.getMark());
        return fund;
    }

    public FundDetail createFundDetail(final ArrFund arrFund, UserDetail user) {
        Assert.notNull(arrFund, "AS musí být vyplněn");

        FundDetail fundDetail = new FundDetail();
        fundDetail.setId(arrFund.getFundId());
        fundDetail.setCreateDate(convertDateTime(arrFund.getCreateDate()));
        fundDetail.setInstitutionIdentifier(arrFund.getInstitution().getInternalCode());
        fundDetail.setFundNumber(arrFund.getFundNumber());
        fundDetail.setName(arrFund.getName());
        fundDetail.setInternalCode(arrFund.getInternalCode());
        fundDetail.setUnitdate(arrFund.getUnitdate());
        fundDetail.setMark(arrFund.getMark());
        return fundDetail;
    }

    /**
     * Vytvoří verzi archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     * @param user
     * @return VO verze archivní pomůcky
     */
    public ArrFundVersionVO createFundVersion(final ArrFundVersion fundVersion, final UserDetail user) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFundVersionVO fundVersionVO = mapper.map(fundVersion, ArrFundVersionVO.class);
        Date createDate = Date.from(
                fundVersion.getCreateChange().getChangeDate().toInstant());
        fundVersionVO.setCreateDate(createDate);
        ViewTitles viewTitles = configView.getViewTitles(fundVersion.getRuleSetId(),
                fundVersion.getFundId());
        fundVersionVO.setStrictMode(viewTitles.getStrictMode());

        ArrChange lockChange = fundVersion.getLockChange();
        if (lockChange != null) {
            Date lockDate = Date.from(lockChange.getChangeDate().toInstant());
            fundVersionVO.setLockDate(lockDate);
        } else {
            fundVersionVO.setIssues(wfFactory.createSimpleIssues(fundVersion.getFund(), user));
            fundVersionVO.setConfig(wfFactory.createConfig(fundVersion));
        }
        fundVersionVO.setRuleSetId(fundVersion.getRuleSet().getRuleSetId());

        return fundVersionVO;
    }

    /**
     * Vytvoří třídy výstupů archivního souboru.
     *
     * @param outputs seznam DO
     * @return seznam VO
     */
    public List<ArrOutputVO> createOutputList(final Collection<ArrOutput> outputs) {
        Assert.notNull(outputs, "Musí být vyplněny výstupy");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        List<ArrOutputVO> result = new ArrayList<>(outputs.size());
        for (ArrOutput output : outputs) {
            result.add(createOutput(output));
        }
        return result;
    }

    /**
     * Vytvoří třídy výstupů archivního souboru.
     *
     * @param outputData DO
     * @return VO
     */
    public ArrOutputVO createOutput(final ArrOutput output) {
        Validate.notNull(output, "Výstup musí být vyplněn");

        ArrOutputVO outputVO = ArrOutputVO.newInstance(output);
        return outputVO;
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
     * <p>
     * TODO: přepsat metodu bez mapperu na klasické metody/factory
     *
     * @param item hodnota atributu
     * @return VO hodnota atributu
     */
    public <T extends ArrItem> ArrItemVO createItem(final T item) {
        Assert.notNull(item, "Hodnota musí být vyplněna");
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ArrItemVO itemVO = null;
        ArrData data = item.getData();
        DataType dataType = DataType.fromId(item.getItemType().getDataTypeId()); //.getCode();

        switch (dataType) {
            case STRING:
                return ArrItemStringVO.newInstance(item);
            case TEXT:
                return ArrItemTextVO.newInstance(item);
            case FORMATTED_TEXT:
                return ArrItemFormattedTextVO.newInstance(item);
            case FILE_REF:
                return ArrItemFileRefVO.newInstance(item, this.attachmentService);
            case ENUM:
                return ArrItemEnumVO.newInstance(item);
            case INT:
                return ArrItemIntVO.newInstance(item);
            case RECORD_REF:
                return ArrItemRecordRefVO.newInstance(item, apFactory);
        }

        // TODO: refactorize following code to the solution without mappers
        if (data != null) {
            itemVO = mapper.map(data, ArrItemVO.class);
        }
        if (itemVO == null) {
            switch (dataType) {
                case UNITDATE:
                    itemVO = new ArrItemUnitdateVO();
                    break;
                case UNITID:
                    itemVO = new ArrItemUnitidVO();
                    break;
                case COORDINATES:
                    itemVO = new ArrItemCoordinatesVO();
                    break;
                case DECIMAL:
                    itemVO = new ArrItemDecimalVO();
                    break;
                case STRUCTURED:
                    itemVO = new ArrItemStructureVO();
                    break;
                case JSON_TABLE:
                    itemVO = new ArrItemJsonTableVO();
                    break;
                case URI_REF:
                    itemVO = new ArrItemUriRefVO();
                    break;
                case DATE:
                    itemVO = new ArrItemDateVO();
                    break;
                case BIT:
                    itemVO = new ArrItemBitVO();
                    break;
                default:
                    throw new NotImplementedException(item.getItemType().getDataTypeId().toString());
            }
            itemVO.setUndefined(true);
        }

        // ignorujeme nodeId, protože přepisuje nodeId z ArrItemUriRefVO
        BeanUtils.copyProperties(item, itemVO, "nodeId");
        itemVO.setId(item.getItemId());
        itemVO.setReadOnly(item.getReadOnly());

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
        Iterator<ApAccessPointVO> apVoIt = apListVO.iterator();

        for (T item : items) {
            ArrItemVO itemVO = createItem(item);
            ArrData data = item.getData();
            if (data instanceof ArrDataRecordRef) {
                ApAccessPointVO apVo = apVoIt.next();
                ((ArrItemRecordRefVO) itemVO).setRecord(apVo);
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
        if (viewConfig != null) {
            for (GroupConfiguration groupConfig : viewConfig.getGroups()) {
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
            if (groupConfig != null) {
                itemGroupVO = itemGroupVOMap.get(groupConfig);
            } else {
                itemGroupVO = defaultGroupVo;
            }

            List<ItemTypeDescItemsLiteVO> itemTypeList = itemGroupVO.getTypes();
            itemTypeList.add(descItemTypeVO);
        }

        // Filter empty groups
        List<ItemGroupVO> itemGroupVOList = allItemGroups.stream().filter(g -> g.getTypes().size() > 0).collect(Collectors.toList());

        // seřazení položek ve skupinách
        for (ItemGroupVO itemGroupVO : itemGroupVOList) {
            boolean ordered = false;

            if (viewConfig != null) {
                GroupConfiguration groupConfig = viewConfig.getGroup(itemGroupVO.getCode());
                if (groupConfig != null) {
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
        List<UISettings> favoritesItemTypes = settingsService.getGlobalSettings(UISettings.SettingsType.FAVORITE_ITEM_SPECS.toString(), UISettings.EntityType.ITEM_TYPE);

        // typeId -> list<specId>
        // naplnění mapy podle oblíbených z nastavení
        Map<Integer, List<Integer>> typeSpecsMap = new HashMap<>();
        for (UISettings favoritesItemType : favoritesItemTypes) {
            SettingFavoriteItemSpecs setting = SettingFavoriteItemSpecs.newInstance(favoritesItemType, staticDataService);
            if (CollectionUtils.isNotEmpty(setting.getFavoriteItems())) {
                StaticDataProvider sdp = staticDataService.getData();
                List<Integer> itemSpecsIds = new ArrayList<>();
                for (FavoriteItem fi : setting.getFavoriteItems()) {
                    RulItemSpec ris = sdp.getItemSpecByCode(fi.getValue());
                    Validate.notNull(ris, "Cannot find specification: {}", fi.getValue());
                    itemSpecsIds.add(ris.getItemSpecId());
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
            if (typeInfo != null && typeInfo.getWidth() != null) {
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
     *
     * @param nodeConformity stav validace
     * @return VO stavu validace
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
        ApIndex displayName = indexRepository.findByPartAndIndexType(institution.getAccessPoint().getPreferredPart(), DISPLAY_NAME);

        ParInstitutionVO institutionVO = mapper.map(institution, ParInstitutionVO.class);
        institutionVO.setAccessPointId(institution.getAccessPointId());
        institutionVO.setName(displayName != null ? displayName.getValue() : null);
        institutionVO.setCode(institution.getInternalCode());

        return institutionVO;
    }

    /**
     * Vytvoří VO pro seznam typů oprávnění.
     *
     * @param policyTypes seznam DO
     * @return seznam VO
     */
    public List<RulPolicyTypeVO> createPolicyTypes(final List<RulPolicyType> policyTypes) {
        return createList(policyTypes, RulPolicyTypeVO.class, this::createPolicyType);
    }

    /**
     * Vytvoří VO pro typ oprávnění.
     *
     * @param policyType DO typu oprávnění
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
     * @param outputTypes seznam DO
     * @return seznam VO
     */
    public List<RulOutputTypeVO> createOutputTypes(final List<RulOutputType> outputTypes) {
        return createList(outputTypes, RulOutputTypeVO.class, this::createOutputType);
    }

    /**
     * Vytvoří VO pro typ outputů.
     *
     * @param outputType DO typu outputu
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
     * @param outputs     seznam DO výstupů
     * @param fundVersion
     * @return seznam VO výstupů
     */
    public List<ArrOutputVO> createOutputExtList(final List<ArrOutput> outputs, final ArrFundVersion fundVersion) {
        Assert.notNull(outputs, "Výstupy musí být vyplněny");
        List<ArrOutputVO> outputExtList = new ArrayList<>();
        for (ArrOutput output : outputs) {
            outputExtList.add(createOutputExt(output, fundVersion));
        }
        return outputExtList;
    }

    /**
     * Vytvoření VO rozšířeného výstupu.
     *
     * @param output      DO výstup
     * @param fundVersion
     * @return VO výstup
     */
    public ArrOutputVO createOutputExt(final ArrOutput output, final ArrFundVersion fundVersion) {    	
        ArrOutputVO outputExt = createOutput(output);
        
        // prepare template list
    	List<ArrOutputTemplate> outputTemplates = outputServiceInternal.getOutputTemplates(output);
    	if(CollectionUtils.isNotEmpty(outputTemplates)) {
    		List<Integer> templateIds = new ArrayList<>(outputTemplates.size());
    		for(ArrOutputTemplate outputTemplate: outputTemplates) {
    			templateIds.add(outputTemplate.getTemplateId());
    		}
    		outputExt.setTemplateIds(templateIds);
    	}
    	
    	// prepare result list
    	List<ArrOutputResult> outputResults = output.getOutputResults();
    	if(CollectionUtils.isNotEmpty(outputResults)) {
    		// prepare date of generation
    		outputExt.setGeneratedDate(Date.from(outputResults.get(0).getChange().getChangeDate().toInstant()));
    		
    		List<Integer> outputResultIds = new ArrayList<>(outputResults.size());
    		for(ArrOutputResult outputResult: outputResults) {
    			outputResultIds.add(outputResult.getOutputResultId());
    		}
    		outputExt.setOutputResultIds(outputResultIds);
    	}
        
        List<ArrNodeOutput> nodes = outputServiceInternal.getOutputNodes(output, fundVersion.getLockChange());
        List<Integer> nodeIds = nodes.stream().map(ArrNodeOutput::getNodeId).collect(Collectors.toList());
        outputExt.setNodes(levelTreeCacheService.getNodesByIds(nodeIds, fundVersion));
        outputExt.setScopes(outputServiceInternal.getRestrictedScopeVOs(output));
        ApAccessPoint anonymizedAp = output.getAnonymizedAp();
        if (anonymizedAp != null) {
            outputExt.setAnonymizedAp(apFactory.createVO(anonymizedAp));
        }
        return outputExt;
    }

    /**
     * Vytvoří seznam VO.
     *
     * @param groups          vstupní seznam skupin
     * @param initPermissions mají se plnit oprávnění
     * @param initUsers       mají se plnit uživatelé?
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
     * Vytvoří VO uživatele s návaznými daty.
     *
     * @param user            uživatel
     * @param initPermissions mají se plnit oprávnění?
     * @param initGroups      mají se plnit skuipny?
     * @return VO
     */
    public UsrUserVO createUser(final UsrUser user, final boolean initPermissions, final boolean initGroups) {
        // Hlavní objekt

        ApAccessPointVO accessPointVO = new ApAccessPointVO();
        if (user.getAccessPoint() != null) {
            accessPointVO.setId(user.getAccessPoint().getAccessPointId());
            accessPointVO.setUuid(user.getAccessPoint().getUuid());
            if (user.getAccessPoint().getPreferredPart() != null) {
                ApIndex displayName = indexRepository.findByPartAndIndexType(user.getAccessPoint().getPreferredPart(), DISPLAY_NAME);
                accessPointVO.setName(displayName != null ? displayName.getValue() : null);
            }
        }
        UsrUserVO result = new UsrUserVO(user, accessPointVO);
        result.setAuthTypes(authenticationRepository.findByUser(user).stream().map(UsrAuthentication::getAuthType).collect(Collectors.toList()));
        // Načtení oprávnění
        if (initPermissions) {
//        List<UsrPermission> permissions = permissionRepository.findByUserOrderByPermissionIdAsc(user);
            List<UsrPermission> permissions = permissionRepository.getAllPermissionsWithGroups(user);

            StaticDataProvider staticData = staticDataService.getData();
            List<UsrPermissionVO> permissionsVOs = permissions.stream().map(
                    // if has groupId -> it is inheritted
                    p -> UsrPermissionVO.newInstance(p,
                            p.getGroupId() != null,
                            staticData))
                    .collect(Collectors.toList());

            result.setPermissions(permissionsVOs);
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
     *
     * @param group           skupina
     * @param initPermissions mají se plnit oprávnění
     * @param initUsers       mají se plnit uživatelé?
     * @return VO
     */
    public UsrGroupVO createGroup(final UsrGroup group, final boolean initPermissions, final boolean initUsers) {
        // Hlavní objekt
        UsrGroupVO result = UsrGroupVO.newInstance(group);

        // Načtení oprávnění
        if (initPermissions) {
            List<UsrPermission> permissions = permissionRepository.findByGroupOrderByPermissionIdAsc(group);

            StaticDataProvider staticData = staticDataService.getData();
            List<UsrPermissionVO> permissionsVOs = permissions.stream().map(
                    p -> UsrPermissionVO.newInstance(p, false,
                            staticData))
                    .collect(Collectors.toList());

            result.setPermissions(permissionsVOs);
        }

        // Přiřazení uživatelé
        if (initUsers) {
            List<UsrUser> users = userRepository.findByGroup(group);
            result.setUsers(createUserList(users, false));
        }

        return result;
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

    public List<ArrRequestVO> createRequest(String contextPath,
                                            final Collection<ArrRequest> requests, final boolean detail,
                                            final ArrFundVersion fundVersion) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        List<ArrRequestVO> requestVOList = new ArrayList<>(requests.size());
        Set<ArrDigitizationRequest> requestForNodes = new HashSet<>();
        Set<ArrDaoRequest> requestForDaos = new HashSet<>();
        Set<ArrDaoLinkRequest> requestForDaoLinks = new HashSet<>();

        Map<ArrRequest, ArrRequestQueueItem> requestQueuedMap = new HashMap<>();
        List<ArrRequestQueueItem> requestQueueItems = CollectionUtils.isEmpty(requests) ? Collections.emptyList()
                : requestQueueItemRepository.findByRequests(requests);
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
            requestVO = createRequestVO(contextPath,
                                        countNodesRequestMap, nodesRequestMap, countDaosRequestMap, daosRequestMap,
                                        codeTreeNodeClientMap, request, detail, fundVersion);
            convertRequest(mapper, request, requestQueuedMap.get(request), requestVO);
            requestVOList.add(requestVO);
        }
        return requestVOList;
    }

    public ArrRequestVO createRequest(String contextPath,
                                      final ArrRequest request, final boolean detail,
                                      final ArrFundVersion fundVersion) {
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
        requestVO = createRequestVO(contextPath,
                                    countNodesRequestMap, nodesRequestMap, countDaosRequestMap, daosRequestMap,
                                    codeTreeNodeClientMap, request, detail, fundVersion);
        convertRequest(mapper, request, requestQueueItem, requestVO);

        return requestVO;
    }

    private Map<String, TreeNodeVO> fillDaoLinkDetailParams(final ArrFundVersion fundVersion, final Set<ArrDaoLinkRequest> requestForDaoLinks) {
        Map<String, TreeNodeVO> result = new HashMap<>();

        Set<String> didCodes = new HashSet<>();
        for (ArrDaoLinkRequest requestForDaoLink : requestForDaoLinks) {
            didCodes.add(requestForDaoLink.getDidCode());
        }

        List<ArrNode> nodes = nodeRepository.findAllByUuidIn(didCodes);
        Set<Integer> nodeIds = new HashSet<>();
        for (ArrNode node : nodes) {
            nodeIds.add(node.getNodeId());
        }
        Map<Integer, TreeNodeVO> treeNodeClientMap = levelTreeCacheService.getNodesByIds(nodeIds, fundVersion).stream()
                .collect(Collectors.toMap(TreeNodeVO::getId, Function.identity()));

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
        List<ArrDaoRequestDao> daoRequestDaos = daoRequestDaoRepository.findByDaoRequests(requestForDaos);

        Set<Integer> daoIds = new HashSet<>();
        for (ArrDaoRequestDao daoRequestDao : daoRequestDaos) {
            daoIds.add(daoRequestDao.getDao().getDaoId());
        }

        List<ArrDao> arrDaos = daoRepository.findAllById(daoIds);
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

        Map<Integer, TreeNodeVO> treeNodeClientMap = levelTreeCacheService.getNodesByIds(nodeIds, fundVersion).stream()
                .collect(Collectors.toMap(TreeNodeVO::getId, Function.identity()));

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

    private ArrRequestVO createRequestVO(String contextPath,
                                         final Map<ArrDigitizationRequest, Integer> countNodesRequestMap,
                                         final Map<ArrDigitizationRequest, List<TreeNodeVO>> nodesRequestMap,
                                         final Map<ArrDaoRequest, Integer> countDaosRequestMap,
                                         final Map<ArrDaoRequest, List<ArrDao>> daosRequestMap,
                                         final Map<String, TreeNodeVO> codeTreeNodeClientMap,
                                         final ArrRequest request,
                                         final boolean detail,
                                         final ArrFundVersion fundVersion) {
        ArrRequest req = HibernateUtils.unproxy(request);
        ArrRequestVO requestVO;

        switch (request.getDiscriminator()) {
            case DIGITIZATION: {
                requestVO = new ArrDigitizationRequestVO();
                convertDigitizationRequest((ArrDigitizationRequest) req, (ArrDigitizationRequestVO) requestVO,
                        countNodesRequestMap.get(request), nodesRequestMap.get(request));
                break;
            }

            case DAO: {
                requestVO = new ArrDaoRequestVO();
                convertDaoRequest(contextPath, (ArrDaoRequest) req, (ArrDaoRequestVO) requestVO,
                                  countDaosRequestMap.get(request),
                        daosRequestMap.get(request), detail, fundVersion);
                break;
            }

            case DAO_LINK: {
                requestVO = new ArrDaoLinkRequestVO();
                convertDaoLinkRequest((ArrDaoLinkRequest) req, (ArrDaoLinkRequestVO) requestVO, false, fundVersion,
                        codeTreeNodeClientMap);
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
        ArrRequest req = HibernateUtils.unproxy(request);
        switch (request.getDiscriminator()) {
            case DIGITIZATION: {
                requestForNodes.add((ArrDigitizationRequest) req);
                break;
            }

            case DAO: {
                requestForDaos.add((ArrDaoRequest) req);
                break;
            }

            case DAO_LINK: {
                requestForDaoLinks.add((ArrDaoLinkRequest) req);
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

    private void convertDaoRequest(final String contextPath,
                                   final ArrDaoRequest request,
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
            requestVO.setDaos(createDaoList(contextPath, daos, detail, fundVersion));
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

    public List<ArrRequestQueueItemVO> createRequestQueueItem(String contextPath,
                                                              final List<ArrRequestQueueItem> requestQueueItems) {
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
                    List<ArrRequestVO> request = createRequest(contextPath,
                                                               arrFundListEntry.getValue(), false, arrFundVersion);
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
     * @param detail     příznak, zda se mají naplnit seznamy na VO, pokud ne, jsou naplněny pouze počty podřízených záznamů v DB
     * @param version
     * @return list VO
     */
    public List<ArrDaoVO> createDaoList(String contextPath,
                                        final List<ArrDao> arrDaoList, final boolean detail,
                                        final ArrFundVersion version) {
        if (CollectionUtils.isEmpty(arrDaoList)) {
            return Collections.emptyList();
        }
        final List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaoInAndDeleteChangeIsNull(arrDaoList);
        // There might me more links to one DAO
        // We will send only first DaoLink
        //
        // This functionality has to be changed in future
        // Client should request existing daolinks
        Map<Integer, ArrDaoLink> daoLinkMap = new HashMap<>();
        for (ArrDaoLink daoLink : daoLinkList) {
            daoLinkMap.put(daoLink.getDaoId(), daoLink);
        }

        return createDaoList(contextPath, arrDaoList, detail, version, daoLinkMap);
    }

    public List<ArrDaoVO> createDaoList(String contextPath,
                                        final List<ArrDao> arrDaoList, final boolean detail,
                                        final ArrFundVersion version, final Map<Integer, ArrDaoLink> daoLinkMap) {
        if (CollectionUtils.isEmpty(arrDaoList)) {
            return Collections.emptyList();
        }
        List<ArrDaoVO> voList = new ArrayList<>(arrDaoList.size());
        for (ArrDao arrDao : arrDaoList) {
            voList.add(createDao(contextPath, arrDao, detail, version, daoLinkMap));
        }
        return voList;
    }

    /**
     * Vytvoření VO z DO.
     *
     * @param daoFile do
     * @return VO
     */
    private ArrDaoFileVO createDaoFile(String contextPath, final ArrDaoFile daoFile) {
        ArrDaoFileVO fileVo = ArrDaoFileVO.newInstance(daoFile);

        ArrDigitalRepository digitalRepository = daoFile.getDao().getDaoPackage().getDigitalRepository();
        fileVo.setUrl(daoService.getDaoFileUrl(contextPath, daoFile, digitalRepository));
        fileVo.setThumbnailUrl(daoService.getDaoThumbnailUrl(contextPath, daoFile, digitalRepository));

        return fileVo;
    }

    /**
     * Vytvoření vo z DO
     * 
     * @param contextPath
     *
     * @param dao
     *            DO
     * @param detail
     *            příznak, zda se mají naplnit seznamy na VO, pokud ne, jsou
     *            naplněny pouze počty podřízených záznamů v DB
     * @param version
     * @return vo
     */
    private ArrDaoVO createDao(String contextPath,
                               final ArrDao dao, final boolean detail,
                               final ArrFundVersion version,
                               final Map<Integer, ArrDaoLink> daoLinkMap) {
        // read scenarios
        Items items = daoSyncService.unmarshalItemsFromAttributes(dao);
        List<String> scenarios = null;
        if (items != null) {
            scenarios = daoSyncService.getAllScenarioNames(items);
        }

        ArrDaoVO vo = ArrDaoVO.newInstance(dao, scenarios);

        ArrDaoLink daoLink = daoLinkMap.get(dao.getDaoId());
        if (daoLink != null) {
            ArrDaoLinkVO daoLinkVo = createDaoLink(daoLink, version);
            vo.setDaoLink(daoLinkVo);
        }

        ArrDaoPackage daoPackage = dao.getDaoPackage();
        vo.setDaoPackage(ArrDaoPackageVO.newInstance(daoPackage));

        ArrDigitalRepository digitalRepository = daoPackage.getDigitalRepository();
        String url = daoService.getDaoUrl(contextPath, dao, daoLink, digitalRepository);
        vo.setUrl(url);

        if (detail) {
            final List<ArrDaoFile> daoFileList = daoFileRepository.findByDaoAndDaoFileGroupIsNull(dao);
            final List<ArrDaoFileVO> daoFileVOList = daoFileList.stream().map(f -> createDaoFile(contextPath, f))
                    .collect(Collectors.toList());
            vo.addAllFile(daoFileVOList);

            final List<ArrDaoFileGroup> daoFileGroups = daoFileGroupRepository.findByDaoOrderByCodeAsc(dao);
            final List<ArrDaoFileGroupVO> daoFileGroupVOList = new ArrayList<>();
            for (ArrDaoFileGroup daoFileGroup : daoFileGroups) {
                final ArrDaoFileGroupVO daoFileGroupVO = ArrDaoFileGroupVO.newInstance(daoFileGroup);
                final List<ArrDaoFile> arrDaoFileList = daoFileRepository.findByDaoAndDaoFileGroup(dao, daoFileGroup);
                final List<ArrDaoFileVO> groupDaoFileVOList = arrDaoFileList.stream()
                        .map(f -> createDaoFile(contextPath, f))
                        .collect(Collectors.toList());
                daoFileGroupVO.setFiles(groupDaoFileVOList);
                daoFileGroupVOList.add(daoFileGroupVO);
            }

            vo.addAllFileGroup(daoFileGroupVOList);
        } else {
            vo.setFileCount(daoFileRepository.countByDaoAndDaoFileGroupIsNull(dao));
            vo.setFileGroupCount(daoFileGroupRepository.countByDao(dao));
        }
        return vo;
    }

    public ArrDaoLinkVO createDaoLink(ArrDaoLink daoLink, ArrFundVersion version) {
        return createDaoLink(daoLink.getDaoLinkId(),
                             daoLink.getNodeId(),
                             daoLink.getScenario(),
                             version.getFundVersionId());
    }

    @Transactional
    public ArrDaoLinkVO createDaoLink(Integer daoLinkId,
                                      Integer nodeId,
                                      String scenario,
                                      Integer fundVersionId) {

        final List<TreeNodeVO> nodesByIds = levelTreeCacheService
                .getNodesByIds(Collections.singletonList(nodeId),
                               fundVersionId);

        return new ArrDaoLinkVO(daoLinkId, nodesByIds.get(0), scenario);
    }

    public List<ArrDaoPackageVO> createDaoPackageList(final List<ArrDaoPackage> arrDaoList, final Boolean unassigned) {
        if (CollectionUtils.isEmpty(arrDaoList)) {
            return Collections.emptyList();
        }
        List<ArrDaoPackageVO> result = new ArrayList<>(arrDaoList.size());
        for (ArrDaoPackage arrDaoPackage : arrDaoList) {
            ArrDaoPackageVO vo = createDaoPackage(unassigned, arrDaoPackage);
            result.add(vo);
        }

        return result;
    }

    private ArrDaoPackageVO createDaoPackage(final Boolean unassigned, final ArrDaoPackage arrDaoPackage) {
        ArrDaoPackageVO vo = ArrDaoPackageVO.newInstance(arrDaoPackage);

        long daoCount;
        if (unassigned) {
            daoCount = daoRepository.countByDaoPackageIDAndNotExistsDaoLink(arrDaoPackage.getDaoPackageId());
        } else {
            daoCount = daoRepository.countByDaoPackageID(arrDaoPackage.getDaoPackageId());
        }

        vo.setDaoCount(daoCount);
        return vo;
    }

    public List<StructureExtensionFundVO> createStructureExtensionFund(final List<RulStructuredTypeExtension> allStructureExtensions,
                                                                       final List<RulStructuredTypeExtension> structureExtensions) {
        List<StructureExtensionFundVO> result = new ArrayList<>(allStructureExtensions.size());
        allStructureExtensions.forEach(se -> {
            StructureExtensionFundVO structureExtensionFund = createStructureExtensionFund(se);
            structureExtensionFund.setActive(structureExtensions.contains(se));
            result.add(structureExtensionFund);
        });
        return result;
    }

    private StructureExtensionFundVO createStructureExtensionFund(final RulStructuredTypeExtension structureExtension) {
        StructureExtensionFundVO structureExtensionFundVO = new StructureExtensionFundVO();
        structureExtensionFundVO.setId(structureExtension.getStructuredTypeExtensionId());
        structureExtensionFundVO.setCode(structureExtension.getCode());
        structureExtensionFundVO.setName(structureExtension.getName());
        structureExtensionFundVO.setActive(false);
        return structureExtensionFundVO;
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

    public List<RulOutputFilterVO> createOutputFilterList(final List<RulOutputFilter> outputFilters) {
        return createList(outputFilters, RulOutputFilterVO.class, null);
    }

    public List<RulExportFilterVO> createExportFilterList(final List<RulExportFilter> exportFilters) {
        return createList(exportFilters, RulExportFilterVO.class, null);
    }
}
