package cz.tacr.elza.controller.config;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static org.aspectj.runtime.internal.Conversions.intValue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.DaDaoFile;
import cz.tacr.elza.domain.DaDaoFileFolder;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.da.DaAipActionService;
import cz.tacr.elza.service.da.DaService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriUtils;

import cz.tacr.elza.service.dao.FileSystemRepoBrowser;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
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
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDaoRequestDao;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.GisExternalSystem;
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
import cz.tacr.elza.domain.converter.UnitDateConverter;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.ItemTypeUpdater;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs.FavoriteItem;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.DaoService;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.attachment.AttachmentService;
import cz.tacr.elza.ws.types.v1.Items;

/**
 * Tovární třída pro vytváření VO objektů a jejich seznamů.
 */
@Service
public class ClientFactoryVO {

	static Map<cz.tacr.elza.core.data.DataType, Function<ArrData, ItemData>> dataConvertors = new HashMap<>();
	static {
		dataConvertors.put(cz.tacr.elza.core.data.DataType.INT, ClientFactoryVO::convertInt);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.STRING, ClientFactoryVO::convertString);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.TEXT, ClientFactoryVO::convertText);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.UNITDATE, ClientFactoryVO::convertUnitdate);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.UNITID, ClientFactoryVO::convertUnitid);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.FORMATTED_TEXT, ClientFactoryVO::convertFormattedText);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.COORDINATES, ClientFactoryVO::convertCoordinates);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.RECORD_REF, ClientFactoryVO::convertRecordRef);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.DECIMAL, ClientFactoryVO::convertDecimal);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.STRUCTURED, ClientFactoryVO::convertStructureRef);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.ENUM, ClientFactoryVO::convertNull);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.FILE_REF, ClientFactoryVO::convertFileRef);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.JSON_TABLE, ClientFactoryVO::convertJsonTable);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.DATE, ClientFactoryVO::convertDate);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.URI_REF, ClientFactoryVO::convertUriRef);
		dataConvertors.put(cz.tacr.elza.core.data.DataType.BIT, ClientFactoryVO::convertBit);
    }

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
    private ArrLegacyDaoLinkRepository legacyDaoLinkRepository;

    @Autowired
    private ArrDaLinkRepository daLinkRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

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

    @Autowired
    private AccessPointService accessPointService;
    @Autowired
    private AipStateRepository aipStateRepository;

    
    @Autowired
    private DaSyncQueueItemRepository daSyncQueueItemRepository;

    @Autowired
    private DaAipActionService daAipActionService;

    @Autowired
    private InstitutionRepository institutionRepository;

    /**
     * Konvertor pro INT data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertInt(ArrData arrData) {
    	DataInteger data = new DataInteger(((ArrDataInteger) arrData).getIntegerValue(), DataType.INT);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro STRING data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertString(ArrData arrData) {
    	DataString data = new DataString(((ArrDataString) arrData).getStringValue(), DataType.STRING);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro TEXT data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertText(ArrData arrData) {
    	if (arrData instanceof ArrDataString) {
    		return convertString(arrData);
    	}
    	DataText data = new DataText(((ArrDataText) arrData).getTextValue(), DataType.TEXT);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro UNITDATE data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertUnitdate(ArrData arrData) {
    	DataUnitdate data = new DataUnitdate(UnitDateConverter.convertToString(((ArrDataUnitdate) arrData)), DataType.UNITDATE);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro UNITID data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertUnitid(ArrData arrData) {
    	DataUnitid data = new DataUnitid(((ArrDataUnitid) arrData).getUnitId(), DataType.UNITID);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro FORMATTED_TEXT data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertFormattedText(ArrData arrData) {
    	DataFormattedText data = new DataFormattedText(((ArrDataText) arrData).getTextValue(), DataType.FORMATTED_TEXT);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro COORDINATES data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertCoordinates(ArrData arrData) {
    	DataCoordinates data = new DataCoordinates(((ArrDataCoordinates) arrData).getFulltextValue(), DataType.COORDINATES);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro RECORD_REF data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertRecordRef(ArrData arrData) {
    	DataRecordRef data = new DataRecordRef(((ArrDataRecordRef) arrData).getRecordId(), DataType.RECORD_REF);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro DECIMAL data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertDecimal(ArrData arrData) {
    	DataDecimal data = new DataDecimal(((ArrDataDecimal) arrData).getValue(), DataType.DECIMAL);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro STRUCTURED data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertStructureRef(ArrData arrData) {
    	DataStructureRef data = new DataStructureRef(((ArrDataStructureRef) arrData).getStructuredObjectId(), DataType.STRUCTURED);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro ENUM data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertNull(ArrData arrData) {
    	DataEnum data = new DataEnum(DataType.ENUM);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro FILE_REF data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertFileRef(ArrData arrData) {
    	DataFileRef data = new DataFileRef(((ArrDataFileRef) arrData).getFileId(), DataType.FILE_REF);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro JSON_TABLE data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertJsonTable(ArrData arrData) {
    	DataJsonTable data = new DataJsonTable(((ArrDataJsonTable) arrData).getJsonValue(), DataType.JSON_TABLE);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro DATE data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertDate(ArrData arrData) {
    	DataDate data = new DataDate(((ArrDataDate) arrData).getValue(), DataType.DATE);
        data.setDataId(arrData.getDataId());
        return data;
    }

    /**
     * Konvertor pro URI_REF data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertUriRef(ArrData arrData) {
    	ArrDataUriRef adur = (ArrDataUriRef) arrData;
    	DataUriRef data = new DataUriRef(adur.getUriRefValue(), DataType.URI_REF);
        data.setDataId(arrData.getDataId());
        data.setDescription(adur.getDescription());
        data.setRefTemplateId(adur.getRefTemplateId());
        data.setNodeId(adur.getNodeId());
        return data;
    }

    /**
     * Konvertor pro BIT data
     * 
     * @param arrData
     * @return
     */
    private static ItemData convertBit(ArrData arrData) {
    	DataBit data = new DataBit(((ArrDataBit) arrData).isBitValue(), DataType.BIT);
        data.setDataId(arrData.getDataId());
        return data;
    }

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
        return RulTemplateVO.newInstance(template);
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

        ArrFundVO fundVO = ArrFundVO.newInstance(fund);
        fundVO.setInstitutionId(fund.getInstitution().getInstitutionId());

        StaticDataProvider staticData = staticDataService.getData();

        Set<ApScope> apScopes = scopeRepository.findByFund(fund);
        fundVO.setApScopes(FactoryUtils.transformList(apScopes, s -> ApScopeVO.newInstance(s, staticData)));
        fundVO.setUnitdate(fund.getUnitdate());

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

    public ArrFundGen createFundGen(final ArrFund fund) {
        Assert.notNull(fund, "AS musí být vyplněn");

        ArrFundGen fundGen = new ArrFundGen();
        fundGen.setFundNumber(fund.getFundNumber());
        fundGen.setCreateDate(fund.getCreateDate().toString());
        fundGen.setId(fund.getFundId());
        fundGen.setInternalCode(fund.getInternalCode());
        fundGen.setInstitutionId(fund.getInstitution().getInstitutionId());
        fundGen.setUnitdate(fund.getUnitdate());
        fundGen.setName(fund.getName());

        return fundGen;
    }

    public ParInstitutionGen createParInstitutionGen(final ParInstitution institution, final ApIndex displayName) {
        cz.tacr.elza.controller.vo.BaseCode type = new cz.tacr.elza.controller.vo.BaseCode();
        if (institution.getInstitutionType() != null) {
	        type.setName(institution.getInstitutionType().getName());
	        type.setCode(institution.getInstitutionType().getCode());
	        type.setId(institution.getInstitutionType().getInstitutionTypeId());
        }

        ParInstitutionGen result = new ParInstitutionGen();
        result.setId(institution.getInstitutionId());
        result.setAccessPointId(institution.getAccessPointId());
        result.setCode(institution.getInternalCode());
        result.setInstitutionType(type);
        result.setName(displayName != null? displayName.getIndexValue() : null);

        return result;
    }

    /**
     * Vytvoření Fund z ArrFundVersion
     * 
     * @param arrFundVersion
     * @return Fund
     */
    public Fund createFund(final ArrFundVersion arrFundVersion) {
    	return createFund(arrFundVersion.getFund(), arrFundVersion.getRootNode().getUuid());
    }

    /**
     * Vytvoření Fund z ArrFund.
     *
     * @param arrFund archivní soubor
     * @param uuid
     * @return Fund
     */
    public Fund createFund(final ArrFund arrFund, String uuid) {
        Assert.notNull(arrFund, "AS musí být vyplněn");
        Assert.notNull(uuid, "UUID musí být vyplněn");

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
        fund.setUuid(uuid);
        return fund;
    }

    public FundDetail createFundDetail(final ArrFund arrFund, String uuid) {
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
        fundDetail.setUuid(uuid);
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

        ArrFundVersionVO fundVersionVO = ArrFundVersionVO.newInstance(fundVersion);
        ViewTitles viewTitles = configView.getViewTitles(fundVersion.getRuleSetId(), fundVersion.getFundId());
        fundVersionVO.setStrictMode(viewTitles.getStrictMode());

        ArrChange lockChange = fundVersion.getLockChange();
        if (lockChange != null) {
            Date lockDate = Date.from(lockChange.getChangeDate().toInstant());
            fundVersionVO.setLockDate(lockDate);
        } else {
            fundVersionVO.setIssues(wfFactory.createSimpleIssues(fundVersion.getFund(), user));
            fundVersionVO.setConfig(wfFactory.createConfig(fundVersion));
        }

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
    @Deprecated
    public ArrOutputVO createOutput(final ArrOutput output) {
        Validate.notNull(output, "Výstup musí být vyplněn");

        ArrOutputVO outputVO = ArrOutputVO.newInstance(output);
        return outputVO;
    }

    /**
     * Vytvoření specifikace hodnoty atributu.
     *
     * @param itemSpec specifikace hodnoty atributu
     * @return VO specifikace hodnoty atributu
     */
    public RulDescItemSpecVO createDescItemSpecVO(final RulItemSpec itemSpec) {
        Assert.notNull(itemSpec, "Specifikace atributu musí být vyplněna");
        return RulDescItemSpecVO.newInstance(itemSpec);
    }

    /**
     * Vytvoření typu hodnoty atributu.
     *
     * @param itemType typ hodnoty atributu
     * @return VO typ hodnoty atributu
     */
    public RulDescItemTypeDescItemsVO createDescItemTypeVO(final RulItemType itemType) {
        Assert.notNull(itemType, "Typ atributu musí být vyplněn");
        return RulDescItemTypeDescItemsVO.newInstance(itemType);
    }

    /**
     * Vytvoření typu hodnoty atributu.
     *
     * @param itemType typ hodnoty atributu
     * @return VO typ hodnoty atributu
     */
    public ItemTypeDescItemsLiteVO createDescItemTypeLiteVO(final RulItemType itemType) {
        Assert.notNull(itemType, "Typ atributu musí být vyplněn");
        return ItemTypeDescItemsLiteVO.newInstance(itemType);
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param item hodnota atributu
     * @return VO hodnota atributu
     */
    public <T extends ArrItem> ArrItemVO createItem(final T item) {
        Assert.notNull(item, "Hodnota musí být vyplněna");

        cz.tacr.elza.core.data.DataType dataType = cz.tacr.elza.core.data.DataType.fromId(item.getItemType().getDataTypeId()); //.getCode();

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
            case UNITDATE:
            	return ArrItemUnitdateVO.newInstance(item);
            case UNITID:
            	return ArrItemUnitidVO.newInstance(item);
            case COORDINATES:
                return ArrItemCoordinatesVO.newInstance(item);
            case DECIMAL:
                return ArrItemDecimalVO.newInstance(item);
            case STRUCTURED:
            	return ArrItemStructureVO.newInstance(item);
            case JSON_TABLE:
            	return ArrItemJsonTableVO.newInstance(item);
            case URI_REF:
            	return ArrItemUriRefVO.newInstance(item);
            case DATE:
            	return ArrItemDateVO.newInstance(item);
            case BIT:
            	return ArrItemBitVO.newInstance(item);
            default:
                throw new NotImplementedException(item.getItemType().getDataTypeId().toString());
        }
    }

    /**
     * Vytvoření hodnoty atributu (nová).
     *
     * @param item hodnota atributu
     * @return VO hodnota atributu
     */
    public <T extends ArrItem> StructuredObjectItem createStructuredObjectItem(final T item) {
        Assert.notNull(item, "Hodnota musí být vyplněna");

        StructuredObjectItem soItem = new StructuredObjectItem();
        soItem.setId(item.getItemId());
        soItem.setItemTypeId(item.getItemTypeId());
        soItem.setItemSpecId(item.getItemSpecId());
        soItem.setItemObjectId(item.getDescItemObjectId());
        soItem.setPosition(item.getPosition());
        soItem.setReadOnly(item.getReadOnly());

//        ArrNode node = item.getNode();
//        if (node != null) {
//            soItem.setNodeId(item.getNodeId());
//            soItem.setNodeVersion(node.getVersion());
//        }

        ArrData arrData = HibernateUtils.unproxy(item.getData());
        if (arrData == null) {
            soItem.setUndefined(true);
        } else {
            StaticDataProvider sdp = staticDataService.getData();
            RulItemType itemType = sdp.getItemType(item.getItemTypeId());
            cz.tacr.elza.core.data.DataType dataType = cz.tacr.elza.core.data.DataType.fromId(itemType.getDataTypeId());
            Function<ArrData, ItemData> dataConvertor = dataConvertors.get(dataType);
            Objects.requireNonNull(dataConvertor);
            ItemData data = dataConvertor.apply(arrData);
            soItem.setData(data);
        }

        return soItem;
    }

    /**
     * Vytvoření hodnoty atributu (nová).
     *
     * @param item hodnota atributu
     * @return VO hodnota atributu
     */
    public <T extends ArrItem> NodeItem createNodeItem(final T item) {
        Assert.notNull(item, "Hodnota musí být vyplněna");

        NodeItem nodeItem = new NodeItem();
        nodeItem.setId(item.getItemId());
        nodeItem.setItemTypeId(item.getItemTypeId());
        nodeItem.setItemSpecId(item.getItemSpecId());
        nodeItem.setItemObjectId(item.getDescItemObjectId());
        nodeItem.setPosition(item.getPosition());
        nodeItem.setReadOnly(item.getReadOnly());

        ArrNode node = item.getNode();
        if (node != null) {
            nodeItem.setNodeId(item.getNodeId());
            nodeItem.setNodeVersion(node.getVersion());
        }

        ArrData arrData = HibernateUtils.unproxy(item.getData());
        if (arrData == null) {
            nodeItem.setUndefined(true);
        } else {
            StaticDataProvider sdp = staticDataService.getData();
            RulItemType itemType = sdp.getItemType(item.getItemTypeId());
            cz.tacr.elza.core.data.DataType dataType = cz.tacr.elza.core.data.DataType.fromId(itemType.getDataTypeId());
            Function<ArrData, ItemData> dataConvertor = dataConvertors.get(dataType);
            Objects.requireNonNull(dataConvertor);
            ItemData data = dataConvertor.apply(arrData);
            nodeItem.setData(data);
        }

        return nodeItem;
    }

    public ItemData convertData(final ArrData arrData, final cz.tacr.elza.core.data.DataType dataType) {
        Function<ArrData, ItemData> dataConvertor = dataConvertors.get(dataType);
        Objects.requireNonNull(dataConvertor);
        return dataConvertor.apply(arrData);
    }    

    /**
     * Vytvoření OpenAPI hodnoty výstupního prvku z doménové entity.
     *
     * @param item hodnota atributu
     * @return OpenAPI hodnota atributu
     */
    public <T extends ArrItem> OutputItem createOutputItem(final T item) {
        Assert.notNull(item, "Hodnota musí být vyplněna");

        OutputItem outputItem = new OutputItem();
        outputItem.setId(item.getItemId());
        outputItem.setItemTypeId(item.getItemTypeId());
        outputItem.setItemSpecId(item.getItemSpecId());
        outputItem.setItemObjectId(item.getDescItemObjectId());
        outputItem.setPosition(item.getPosition());
        outputItem.setReadOnly(item.getReadOnly());

        ArrData arrData = HibernateUtils.unproxy(item.getData());
        if (arrData == null) {
            outputItem.setUndefined(true);
        } else {
            StaticDataProvider sdp = staticDataService.getData();
            RulItemType itemType = sdp.getItemType(item.getItemTypeId());
            cz.tacr.elza.core.data.DataType dataType = cz.tacr.elza.core.data.DataType.fromId(itemType.getDataTypeId());
            Function<ArrData, ItemData> dataConvertor = dataConvertors.get(dataType);
            Objects.requireNonNull(dataConvertor);
            ItemData data = dataConvertor.apply(arrData);
            outputItem.setData(data);
        }
        return outputItem;
    }

    /**
     * Vytvoří seznam atributů z seznamu ArrDescItem.
     *
     * @param nodeId
     * @param items seznam DO atributů
     * @param inhibitedDescItemObjectIds
     * @return seznam NodeItem
     */
    public List<NodeItem> createNodeItems(final Integer nodeId, final List<ArrDescItem> items, final Set<Integer> inhibitedDescItemObjectIds) {
        if (items == null) {
            return null;
        }
        List<NodeItem> result = new ArrayList<>(items.size());

//        List<ApAccessPoint> apList = new ArrayList<>();
//        for (ArrDescItem item : items) {
//            ArrData data = HibernateUtils.unproxy(item.getData());
//            if (data instanceof ArrDataRecordRef) {
//                ApAccessPoint ap = ((ArrDataRecordRef) data).getRecord();
//                apList.add(ap);
//            }
//        }
//
//        List<ApAccessPointVO> apListVO = apFactory.createVO(apList);
//        Iterator<ApAccessPointVO> apVoIt = apListVO.iterator();

        int inhPos = -items.size();
        for (ArrDescItem item : items) {
        	NodeItem nodeItem = createNodeItem(item);
            if (!nodeItem.getNodeId().equals(nodeId)) {
            	nodeItem.setNodeId(item.getNodeId());
            	nodeItem.setPosition(inhPos++);
            }
            if (inhibitedDescItemObjectIds.contains(item.getDescItemObjectId())) {
            	nodeItem.setInhibited(true);
            }
//            ArrData data = HibernateUtils.unproxy(item.getData());
//            if (data instanceof ArrDataRecordRef) {
//                ApAccessPointVO apVo = apVoIt.next();
//                ((ArrItemRecordRefVO) nodeItem).setRecord(apVo);
//            }
            result.add(nodeItem);
        }

        // řazení záznamů podle position
        Collections.sort(result, (o1, o2) -> o1.getPosition() - o2.getPosition());

        return result;
    }

    public StructuredObject createStructuredObject(final ArrStructuredObject aso) {
    	StructuredObject so = new StructuredObject();
        so.setId(aso.getStructuredObjectId());
        so.setTypeCode(aso.getStructuredType().getCode());
        so.setValue(aso.getValue());
        so.setComplement(aso.getComplement());
        so.setErrorDescription(aso.getErrorDescription());
        so.setAssignable(aso.getAssignable());
        if (aso.getState() != null) {
            so.setState(StructuredObject.StateEnum.valueOf(aso.getState().name()));
        }
        return so;
    }

    /**
     * Vytvoří seznam atributů z seznamu ArrDescItem.
     *
     * @param nodeId
     * @param items seznam DO atributů
     * @param inhibitedDescItemObjectIds
     * @return seznam VO atributů
     */
    @Deprecated
    public List<ArrItemVO> createItems(final Integer nodeId, final List<ArrDescItem> items, final Set<Integer> inhibitedDescItemObjectIds) {
        if (items == null) {
            return null;
        }
        List<ArrItemVO> result = new ArrayList<>(items.size());
        List<ApAccessPoint> apList = new ArrayList<>();
        for (ArrDescItem item : items) {
            ArrData data = HibernateUtils.unproxy(item.getData());
            if (data instanceof ArrDataRecordRef) {
                ApAccessPoint ap = ((ArrDataRecordRef) data).getRecord();
                apList.add(ap);
            }
        }

        List<ApAccessPointVO> apListVO = apFactory.createVO(apList);
        Iterator<ApAccessPointVO> apVoIt = apListVO.iterator();

        int inhPos = -items.size();
        for (ArrDescItem item : items) {
            ArrItemVO itemVO = createItem(item);
            if (!item.getNodeId().equals(nodeId)) {
            	itemVO.setFromNodeId(item.getNodeId());
            	itemVO.setPosition(inhPos++);
            }
            if (inhibitedDescItemObjectIds.contains(item.getDescItemObjectId())) {
            	itemVO.setInhibited(true);
            }
            ArrData data = HibernateUtils.unproxy(item.getData());
            if (data instanceof ArrDataRecordRef) {
                ApAccessPointVO apVo = apVoIt.next();
                ((ArrItemRecordRefVO) itemVO).setRecord(apVo);
            }
            result.add(itemVO);
        }

        // řazení záznamů podle position
        Collections.sort(result, (o1, o2) -> o1.getPosition() - o2.getPosition());
        return result;
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
            ArrData data = HibernateUtils.unproxy(item.getData());
            if (data instanceof ArrDataRecordRef) {
                ApAccessPoint ap = ((ArrDataRecordRef) data).getRecord();
                apList.add(ap);
            }
        }

        List<ApAccessPointVO> apListVO = apFactory.createVO(apList);
        Iterator<ApAccessPointVO> apVoIt = apListVO.iterator();

        for (T item : items) {
            ArrItemVO itemVO = createItem(item);
            ArrData data = HibernateUtils.unproxy(item.getData());
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
        return dataTypes.stream().map(i -> RulDataTypeVO.newInstance(i)).collect(Collectors.toList());
    }

    /**
     * Převedení seznamu RulItemTypeExt -> FormItemType
     * 
     * @param itemTypes
     * @return
     */
    public List<FormItemType> createFormItemTypes(final List<RulItemTypeExt> itemTypes) {
    	return itemTypes.stream()
    			.map(i -> {
    				FormItemType fit = new FormItemType();
    		        List<RulItemSpecExt> rulItemSpecs = i.getRulItemSpecList();
    		        List<FormItemSpec> specs = rulItemSpecs.stream()
    		        		.filter(s -> s.getType() != RulItemSpec.Type.IMPOSSIBLE)
    		        		.map(s -> {
    		        			FormItemSpec spec = new FormItemSpec();
    		        			spec.setItemSpecId(s.getItemSpecId());
    		        			spec.setRepeatable(s.getRepeatable());
    		        			spec.setType(MandatoryType.valueOf(s.getType().name()));
    		        			return spec;
    		        		})
    		        		.toList();

    		        fit.setItemTypeId(i.getItemTypeId());
    		        fit.setType(MandatoryType.valueOf(i.getType().name()));
    		        fit.setRepeatable(i.getRepeatable());
    		        fit.setCal(i.getCalculable());
    		        fit.setCalSt(i.getCalculableState());
    		        fit.setUndefinable(i.getIndefinable());
    		        fit.setSpecs(specs);
    		        fit.setFavoriteSpecIds(null); // TODO
    				return fit;
    			})
    			.toList();
    }
    
    /**
     * Vytvoření seznamu rozšířených typů, ignoruje nemožné typy.
     *
     * @param fundId    identifikátor AS
     * @param ruleCode  kód pravidel
     * @param itemTypes seznam typů hodnot atributů
     * @return seznam skupin s typy hodnot atributů
     */
    public List<FormItemType> createFormItemTypes(final String ruleCode, final Integer fundId, final List<RulItemTypeExt> itemTypes) {

    	List<FormItemType> formItemTypes = createFormItemTypes(itemTypes);

    	Map<Integer, String> codeToId = itemTypes.stream().collect(Collectors.toMap(RulItemTypeExt::getItemTypeId, RulItemTypeExt::getCode));
    	
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
                    Validate.notNull(ris, "Cannot find specification: %s", fi.getValue());
                    itemSpecsIds.add(ris.getItemSpecId());
                }
                typeSpecsMap.put(favoritesItemType.getEntityId(), itemSpecsIds);
            }
        }

        // Prepare list of groups
        ViewConfiguration viewConfig = elzaRules.getViewConfiguration(ruleCode, fundId);
        Map<GroupConfiguration, ItemTypeGroup> itemTypeGroupMap = new HashMap<>();

        // prepare empty groups
        if (viewConfig != null) {
            for (GroupConfiguration groupConfig : viewConfig.getGroups()) {
                ItemTypeGroup group = new ItemTypeGroup(groupConfig.getCode(), groupConfig.getName());
                itemTypeGroupMap.put(groupConfig, group);
            }
        }

        // prepare default group for all other items
        ItemTypeGroup defaultGroup = new ItemTypeGroup(elzaRules.getDefaultGroupConfigurationCode(), null);

        for (FormItemType itemType: formItemTypes) {

            // získání a vyplnění oblíbených specifikací u typu
            List<Integer> favoriteSpecIds = typeSpecsMap.get(itemType.getItemTypeId());
            itemType.setFavoriteSpecIds(favoriteSpecIds);

            TypeInfo typeInfo = null;
            ItemTypeGroup itemTypeGroup;

            String itemTypeCode = codeToId.get(itemType.getItemTypeId());
            GroupConfiguration groupConfig = null;
            if (viewConfig != null) {
                groupConfig = viewConfig.getGroupForType(itemTypeCode);
            }
            if (groupConfig != null) {
                itemTypeGroup = itemTypeGroupMap.get(groupConfig);
                // get type info
                typeInfo = groupConfig.getTypeInfo(itemTypeCode);
            } else {
                itemTypeGroup = defaultGroup;
            }

//            // set width from type info
//            Integer width = 1;
//            if (typeInfo != null && typeInfo.getWidth() != null) {
//                width = typeInfo.getWidth();
//            }
//
//            itemType.setWidth(width);

            List<FormItemType> formItemType = itemTypeGroup.getTypes();
            formItemType.add(itemType);
        }

        // remove empty groups and return result
        return formItemTypes.stream()
                .filter(s -> s.getType() != MandatoryType.IMPOSSIBLE) // ignorují se nemožné
                .collect(Collectors.toList());
    }

    /**
     * Vytvoření seznamu rozšířených typů, ignoruje nemožné typy.
     *
     * @param fundId    identifikátor AS
     * @param ruleCode  kód pravidel
     * @param itemTypes seznam typů hodnot atributů
     * @return seznam skupin s typy hodnot atributů
     */
    @Deprecated
    public List<ItemTypeLiteVO> createItemTypes(final String ruleCode, final Integer fundId, final List<RulItemTypeExt> itemTypes) {

        List<ItemTypeLiteVO> itemTypeExtList = itemTypes.stream().map(i -> ItemTypeLiteVO.newInstance(i)).collect(Collectors.toList());

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
                    Validate.notNull(ris, "Cannot find specification: %s", fi.getValue());
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

    /**
     * Vytvoření typu hodnoty atributu se specifikacemi.
     *
     * @param descItemType typ hodnoty atributu se specifikacemi
     * @return VO typu hodnoty atributu se specifikacemi
     */
    public RulDescItemTypeExtVO createDescItemTypeExt(final RulItemTypeExt descItemType) {
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        RulDescItemTypeExtVO descItemTypeVO = RulDescItemTypeExtVO.newInstance(descItemType);
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
        return descItemTypes.stream().map(i -> RulDescItemTypeExtVO.newInstance(i)).collect(Collectors.toList());
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
        return ArrNodeVO.valueOf(node);
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
        return NodeConformityVO.newInstance(nodeConformity);
    }

    /**
     * Vytvoření seznamu hromadných akcí
     *
     * @param bulkActions seznam DO hromadných akcí
     * @return seznam VO hromadných akcí
     */
    public List<BulkActionVO> createBulkActionList(final List<BulkActionConfig> bulkActions) {
        return bulkActions.stream().map(i -> BulkActionVO.newInstance(i)).collect(Collectors.toList());
    }

    /**
     * Vytvoří hromadnou akci
     *
     * @param bulkAction DO hromadné akce
     * @return hromadná akce VO
     */
    public BulkActionVO createBulkAction(final BulkActionConfig bulkAction) {
        Assert.notNull(bulkAction, "Nastavení hromadné akce musí být vyplněno");
        return BulkActionVO.newInstance(bulkAction);
    }

    public BulkActionRunVO createBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        Assert.notNull(bulkActionRun, "Běh hromatných akcí musí být vyplněn");
        return BulkActionRunVO.newInstance(bulkActionRun);
    }

    public BulkActionRunVO createBulkActionRunWithNodes(final ArrBulkActionRun bulkActionRun) {
        Assert.notNull(bulkActionRun, "Běh hromatných akcí musí být vyplněn");
        BulkActionRunVO bulkActionRunVO = BulkActionRunVO.newInstance(bulkActionRun);
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
        ScenarioOfNewLevelVO scenarioVO = ScenarioOfNewLevelVO.newInstance(scenarioOfNewLevel);
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
    	Collection<ApAccessPoint> accessPoints = institutions.stream().map(i -> i.getAccessPoint()).collect(Collectors.toList());
    	Map<Integer, ApIndex> indexMap = accessPointService.findPreferredPartIndexMap(accessPoints);
        return institutions.stream().map(i -> ParInstitutionVO.newInstance(i, indexMap.get(i.getAccessPointId()))).collect(Collectors.toList());
    }

    /**
     * Vytvoří VO pro seznam typů oprávnění.
     *
     * @param policyTypes seznam DO
     * @return seznam VO
     */
    public List<RulPolicyTypeVO> createPolicyTypes(final List<RulPolicyType> policyTypes) {
        return policyTypes.stream().map(i -> RulPolicyTypeVO.newInstance(i)).collect(Collectors.toList());
    }

    /**
     * Vytvoří VO pro typ oprávnění.
     *
     * @param policyType DO typu oprávnění
     * @return seznam VO typu oprávnění
     */
    public RulPolicyTypeVO createPolicyType(final RulPolicyType policyType) {
        Assert.notNull(policyType, "Typ oprávnění musí být vyplněno");
        return RulPolicyTypeVO.newInstance(policyType);
    }

    /**
     * Vytvoří VO pro seznam typů outputů.
     *
     * @param outputTypes seznam DO
     * @return seznam VO
     */
    public List<RulOutputTypeVO> createOutputTypes(final List<RulOutputType> outputTypes) {
        return outputTypes.stream().map(i -> RulOutputTypeVO.newInstance(i)).collect(Collectors.toList());
    }

    /**
     * Vytvoří VO pro typ outputů.
     *
     * @param outputType DO typu outputu
     * @return seznam VO typu outputu
     */
    public RulOutputTypeVO createOutputType(final RulOutputType outputType) {
        Assert.notNull(outputType, "Typ výstupu musí být vyplněno");
        return RulOutputTypeVO.newInstance(outputType);
    }

    public List<BulkActionRunVO> createBulkActionsList(final List<ArrBulkActionRun> bulkActions) {
        return bulkActions.stream().map(i -> BulkActionRunVO.newInstance(i)).collect(Collectors.toList());
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
                accessPointVO.setName(displayName != null ? displayName.getIndexValue() : null);
            }
        }
        UsrUserVO result = new UsrUserVO(user, accessPointVO);
        result.setAuthTypes(authenticationRepository.findByUser(user).stream().map(UsrAuthentication::getAuthType).collect(Collectors.toList()));
        // Načtení oprávnění
        if (initPermissions) {
            List<UsrPermission> permissions = new ArrayList<>(permissionRepository.findByUser(user));
            permissions.addAll(permissionRepository.findByUserGroups(user));

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

    public List<ArrRequestVO> createRequest(String contextPath,
                                            final Collection<ArrRequest> requests, final boolean detail,
                                            final ArrFundVersion fundVersion) {
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
            convertRequest(request, requestQueuedMap.get(request), requestVO);
            requestVOList.add(requestVO);
        }
        return requestVOList;
    }

    public ArrRequestVO createRequest(String contextPath,
                                      final ArrRequest request, final boolean detail,
                                      final ArrFundVersion fundVersion) {
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
        convertRequest(request, requestQueueItem, requestVO);

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

    private void convertRequest(final ArrRequest request,
                                final ArrRequestQueueItem requestQueueItem,
                                final ArrRequestVO requestVO) {
        ArrChange createChange = request.getCreateChange();
        requestVO.setCode(request.getCode());
        requestVO.setId(request.getRequestId());
        requestVO.setExternalSystemCode(request.getExternalSystemCode());
        requestVO.setState(request.getState());
        requestVO.setRejectReason(request.getRejectReason());
        requestVO.setResponseExternalSystem(Date.from(request.getResponseExternalSystem().atZone(ZoneId.systemDefault()).toInstant()));
        requestVO.setCreate(Date.from(createChange.getChangeDate().toInstant()));
        if (requestQueueItem != null) {
            requestVO.setQueued(Date.from(requestQueueItem.getCreateChange().getChangeDate().toInstant()));
            requestVO.setSend(Date.from(requestQueueItem.getAttemptToSend().atZone(ZoneId.systemDefault()).toInstant()));
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

    public List<ArrRequestQueueItemVO> createRequestQueueItem(String contextPath,
                                                              final List<ArrRequestQueueItem> requestQueueItems) {
        if (requestQueueItems == null) {
            return null;
        }

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
            ArrRequestQueueItemVO requestQueueItemVO = ArrRequestQueueItemVO.newInstance(requestQueueItem);
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
        final List<ArrLegacyDaoLink> daoLinkList = legacyDaoLinkRepository.findByDaoInAndDeleteChangeIsNull(arrDaoList);
        // There might me more links to one DAO
        // We will send only first DaoLink
        //
        // This functionality has to be changed in future
        // Client should request existing daolinks
        Map<Integer, ArrLegacyDaoLink> daoLinkMap = new HashMap<>();
        for (ArrLegacyDaoLink daoLink : daoLinkList) {
            daoLinkMap.put(daoLink.getDaoId(), daoLink);
        }

        return createDaoList(contextPath, arrDaoList, detail, version, daoLinkMap);
    }

    public List<ArrDaoVO> createDaoList(String contextPath,
                                        final Collection<ArrDao> arrDaoList, final boolean detail,
                                        final ArrFundVersion version, final Map<Integer, ArrLegacyDaoLink> daoLinkMap) {
        if (CollectionUtils.isEmpty(arrDaoList)) {
            return Collections.emptyList();
        }
        List<ArrDaoVO> voList = new ArrayList<>(arrDaoList.size());
        // synthetic ids for live-listed filesystem files, unique within this response
        AtomicInteger fsFileIdSeq = new AtomicInteger();
        for (ArrDao arrDao : arrDaoList) {
            voList.add(createDao(contextPath, arrDao, detail, version, daoLinkMap, fsFileIdSeq));
        }
        return voList;
    }

    /**
     * Builds panel VOs for the filesystem links of one node. A filesystem link
     * carries no {@link ArrDao} — the VO is synthesized from the link itself:
     * negative wire id derived from the link id, the repository-relative path
     * as code and label, files listed live from the repository.
     */
    public List<ArrDaoVO> createFsDaoList(String contextPath,
                                          final List<ArrFsLink> fsLinks, final boolean detail,
                                          final ArrFundVersion version) {
        if (CollectionUtils.isEmpty(fsLinks)) {
            return Collections.emptyList();
        }
        List<ArrDaoVO> voList = new ArrayList<>(fsLinks.size());
        // synthetic ids for live-listed filesystem files, unique within this response
        AtomicInteger fsFileIdSeq = new AtomicInteger();
        for (ArrFsLink fsLink : fsLinks) {
            voList.add(createFsDao(contextPath, fsLink, detail, version, fsFileIdSeq));
        }
        return voList;
    }

    private ArrDaoVO createFsDao(String contextPath,
                                 final ArrFsLink fsLink, final boolean detail,
                                 final ArrFundVersion version,
                                 final AtomicInteger fsFileIdSeq) {
        ArrDaoVO vo = new ArrDaoVO();
        // no persistent ArrDao exists; negative wire id so the row cannot
        // collide with real dao ids
        vo.setId(-fsLink.getDaoLinkId());
        vo.setValid(true);
        vo.setCode(fsLink.getPath());
        vo.setLabel(fsLink.getPath());
        vo.setDaoType(ArrDao.DaoType.ATTACHMENT);
        vo.setExistInArrDaoRequest(false);
        vo.setDaoLink(createDaoLink(fsLink.getDaoLinkId(), fsLink.getNodeId(), null,
                                    version.getFundVersionId()));

        ArrDigitalRepository repo = fsLink.getDigitalRepository();
        Integer fundId = fsLink.getNode().getFundId();
        FileSystemRepoBrowser.FsDaoListing listing = daoService.listFsLinkFiles(fsLink);
        if (detail) {
            for (FileSystemRepoBrowser.FsDaoFile file : listing.files()) {
                ArrDaoFileVO fileVo = new ArrDaoFileVO();
                fileVo.setId(-fsFileIdSeq.incrementAndGet());
                fileVo.setCode(file.relatPath());
                fileVo.setFileName(file.fileName());
                fileVo.setSize(file.size());
                fileVo.setMimetype(file.mimetype());
                String url = createFsItemDataUrl(contextPath, fundId, repo, file.relatPath());
                fileVo.setUrl(url);
                fileVo.setThumbnailUrl(url);
                vo.addFile(fileVo);
            }
        } else {
            vo.setFileCount(listing.files().size());
        }
        if (listing.truncated()) {
            vo.setTruncated(Boolean.TRUE);
        }
        return vo;
    }

    private String createFsItemDataUrl(String contextPath, Integer fundId,
                                       ArrDigitalRepository repo, String relatPath) {
        if (contextPath == null || contextPath.equals("/")) {
            contextPath = "";
        } else if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        String encodedPath = StringUtils.isNotBlank(relatPath)
                ? UriUtils.encodeQueryParam(relatPath, StandardCharsets.UTF_8)
                : "";
        return contextPath + "/api/v1/fund/" + fundId
                + "/fsrepo/" + repo.getExternalSystemId()
                + "/item-data?path=" + encodedPath;
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
                               final Map<Integer, ArrLegacyDaoLink> daoLinkMap,
                               final AtomicInteger fsFileIdSeq) {
        // read scenarios
        Items items = daoSyncService.unmarshalItemsFromAttributes(dao);
        List<String> scenarios = null;
        if (items != null) {
            scenarios = daoSyncService.getAllScenarioNames(items);
        }

        ArrDaoVO vo = ArrDaoVO.newInstance(dao, scenarios);

        ArrLegacyDaoLink daoLink = daoLinkMap.get(dao.getDaoId());
        if (daoLink != null) {
            ArrDaoLinkVO daoLinkVo = createDaoLink(daoLink, version);
            vo.setDaoLink(daoLinkVo);
        }

        ArrDaoPackage daoPackage = dao.getDaoPackage();
        vo.setDaoPackage(ArrDaoPackageVO.newInstance(daoPackage));

        ArrDigitalRepository digitalRepository = daoPackage.getDigitalRepository();
        String url = daoService.getDaoUrl(contextPath, dao, daoLink, digitalRepository);
        vo.setUrl(url);

        DaoService.DaoFileListing listing = daoService.getDaoFileListing(dao);
        if (detail) {
            for (ArrDaoFile daoFile : listing.files()) {
                ArrDaoFileVO fileVo = createDaoFile(contextPath, daoFile);
                if (fileVo.getId() == null) {
                    // live-listed filesystem files have no persistent id; assign a
                    // negative wire id, unique within this response, so the client
                    // can address the row without colliding with persisted ids
                    fileVo.setId(-fsFileIdSeq.incrementAndGet());
                }
                vo.addFile(fileVo);
            }
        } else {
            vo.setFileCount(listing.total());
        }
        vo.setTruncated(listing.truncated());
        return vo;
    }

    public ArrDaoLinkVO createDaoLink(ArrLegacyDaoLink daoLink, ArrFundVersion version) {
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

    public List<SdoExtensionFund> createStructureExtensionFund(final List<RulStructuredTypeExtension> allStructureExtensions,
                                                               final List<RulStructuredTypeExtension> structureExtensions) {
        List<SdoExtensionFund> result = new ArrayList<>(allStructureExtensions.size());
        allStructureExtensions.forEach(se -> {
        	SdoExtensionFund sdoExtensionFund = new SdoExtensionFund();
        	sdoExtensionFund.setId(se.getStructuredTypeExtensionId());
        	sdoExtensionFund.setCode(se.getCode());
        	sdoExtensionFund.setName(se.getName());
        	sdoExtensionFund.setActive(structureExtensions.contains(se));
            result.add(sdoExtensionFund);
        });
        return result;
    }

    @Deprecated
    public List<StructureExtensionFundVO> createStructureExtensionFundDeprecated(final List<RulStructuredTypeExtension> allStructureExtensions,
                                                                       final List<RulStructuredTypeExtension> structureExtensions) {
        List<StructureExtensionFundVO> result = new ArrayList<>(allStructureExtensions.size());
        allStructureExtensions.forEach(se -> {
            StructureExtensionFundVO structureExtensionFund = createStructureExtensionFund(se);
            structureExtensionFund.setActive(structureExtensions.contains(se));
            result.add(structureExtensionFund);
        });
        return result;
    }

    @Deprecated
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
        if (extSystem instanceof GisExternalSystem) {
            return GisExternalSystemVO.newInstance((GisExternalSystem) extSystem);
        }
        if (extSystem instanceof AiExternalSystem) {
            return AiExternalSystemVO.newInstance((AiExternalSystem) extSystem);
        }
        if (extSystem instanceof ArrDigitalRepository) {
        	return ArrDigitalRepositoryVO.newInstance((ArrDigitalRepository) extSystem);
        }
        if (extSystem instanceof ArrDigitizationFrontdesk) {
        	return ArrDigitizationFrontdeskVO.newInstance((ArrDigitizationFrontdesk) extSystem);
        }

        throw new BusinessException("Unrecognized external system", BaseCode.INVALID_STATE).set("type", extSystem.getClass());
    }

    public SysExternalSystemSimpleVO createExtSystemSimple(SysExternalSystem extSystem) {
        // AP external system is newly created through factory without mapper
        if (extSystem instanceof ApExternalSystem) {
            return ApExternalSystemSimpleVO.newInstance((ApExternalSystem) extSystem);
        }
        if (extSystem instanceof GisExternalSystem) {
            return GisExternalSystemSimpleVO.newInstance((GisExternalSystem) extSystem);
        }
        if (extSystem instanceof AiExternalSystem) {
            return AiExternalSystemSimpleVO.newInstance((AiExternalSystem) extSystem);
        }
        if (extSystem instanceof ArrDigitizationFrontdesk) {
            return ArrDigitizationFrontdeskSimpleVO.newInstance((ArrDigitizationFrontdesk) extSystem);
        }
        if (extSystem instanceof ArrDigitalRepository) {
            return ArrDigitalRepositorySimpleVO.newInstance((ArrDigitalRepository) extSystem);
        }

        throw new BusinessException("Unrecognized external system", BaseCode.INVALID_STATE).set("type", extSystem.getClass());
    }

    public List<RulOutputFilterVO> createOutputFilterList(final List<RulOutputFilter> outputFilters) {
        return outputFilters.stream().map(i -> new RulOutputFilterVO(i)).collect(Collectors.toList());
    }

    public List<RulExportFilterVO> createExportFilterList(final List<RulExportFilter> exportFilters) {
        return exportFilters.stream().map(i -> new RulExportFilterVO(i)).collect(Collectors.toList());
    }

    public List<AipDetailVO> createAips(List<DaAip> aips) {
        List<AipDetailVO> aipVOList = new ArrayList<>();

        for (DaAip aip : aips) {
           aipVOList.add(createAipDetail(aip));
        }
        return aipVOList;
    }

    public AipDetailVO createAipDetail(DaAip daAip) {
        DaAipState state = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(Collections.singletonList(daAip)).get(0);
        ParInstitution originator = null;
        if(state.getOriginatorAccessPoint() != null) {
            originator = institutionRepository.findByAccessPoint(state.getOriginatorAccessPoint());
        }
        List<ArrDaoLink> daoLinks = null;
        Map<Integer, TreeNodeVO> treeNodeMap = null;
        if (state.getFund() != null) {
            daoLinks = new ArrayList<>(daLinkRepository.findByAipIdAndDeleteChangeIsNull(daAip.getAipId()));
            Set<Integer> nodeIds = daoLinks.stream()
                    .map(ArrDaoLink::getNodeId)
                    .collect(Collectors.toSet());

            ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(state.getFund().getFundId());
            treeNodeMap = levelTreeCacheService.getNodesByIds(nodeIds, fundVersion.getFundVersionId()).stream()
                    .collect(Collectors.toMap(TreeNodeVO::getId, v -> v));
        }

        return createAipDetailVO(daAip, state, originator, daoLinks, treeNodeMap);
    }

    private AipDetailVO createAipDetailVO(DaAip src, DaAipState state, ParInstitution originator, List<ArrDaoLink> daoLinks, Map<Integer, TreeNodeVO> treeNodeMap) {
        AipDetailVO vo = new AipDetailVO();
        vo.setAipId(src.getAipId());
        vo.setCode(src.getCode());
        vo.setDigitalRepositoryId(src.getDigitalRepository().getExternalSystemId());

        if (state != null) {
            vo.setAipVersion(state.getAipVersion());

            if (state.getFund() != null) {
                vo.setFund(createFundGen(state.getFund()));
            }
            vo.setFundCode(state.getFundCode());
            if (state.getUnitdateFrom() != null) {
                vo.setUnitdateFrom(state.getUnitdateFrom().toString());
            }
            if (state.getUnitdateTo() != null) {
                vo.setUnitdateTo(state.getUnitdateTo().toString());
            }
            vo.setInstitutionCode(state.getInstitutionCode());
            if (state.getInstitution() != null) {
                ApIndex displayName = indexRepository.findByPartAndIndexType(state.getInstitution().getAccessPoint().getPreferredPart(), DISPLAY_NAME);
                vo.setInstitution(createParInstitutionGen(state.getInstitution(), displayName));
            }
            if (originator != null) {
                ApIndex displayName = indexRepository.findByPartAndIndexType(state.getOriginatorAccessPoint().getPreferredPart(), DISPLAY_NAME);
                vo.setOriginatorInstitution(createParInstitutionGen(originator, displayName));
            }
            vo.setOriginator(state.getOriginator());
            vo.setIngestionCode(state.getIngestionCode());
            vo.setReferenceNumber(state.getReferenceNumber());
            vo.setNadChangeCode(state.getNadChangeCode());
            vo.setAipSize(state.getAipSize());
            vo.setMetadataLoad(state.getMetadataLoad());
            vo.setCompleteAipLoad(state.getCompleteAipLoad());
            if (state.getProblemType() != null) {
                vo.setProblemType(AipProblemType.fromValue(state.getProblemType().name()));
            }
            vo.setProblemDescription(state.getProblemDescription());
            vo.setProblemFile(state.getProblemFile());
            vo.setAipVersionMetadata(state.getAipVersionMetadata());
        }

        DaSyncQueueItem importSyncQueueItem = daSyncQueueItemRepository.findByAipAndStateInAndActiveIsTrue(src, DaService.getQueueImportStates());
        if (importSyncQueueItem != null) {
            vo.setImportState(mapQueueItemState(importSyncQueueItem.getState()));
            vo.setImportStateMessage(importSyncQueueItem.getStateMessage());
            vo.setImportStateDate(importSyncQueueItem.getDate());
        }

        DaSyncQueueItem exportSyncQueueItem = daSyncQueueItemRepository.findByAipAndStateInAndActiveIsTrue(src, DaService.getQueueExportStates());
        if (exportSyncQueueItem != null) {
            vo.setExportState(mapQueueItemState(exportSyncQueueItem.getState()));
            vo.setExportStateMessage(exportSyncQueueItem.getStateMessage());
            vo.setExportStateDate(exportSyncQueueItem.getDate());
        }

        if (CollectionUtils.isNotEmpty(daoLinks)) {
            vo.setLinkedNodes(createLinkedNodeVOs(daoLinks, treeNodeMap));
        }

        return vo;
    }

    /**
     * Akce nad AIPy i s výsledkem po jednotlivých AIPech. Stav akce se odvozuje z položek,
     * neukládá se - položky se dokončují nezávisle na sobě.
     */
    public DaAipActionVO createAipAction(DaAipAction action) {
        DaAipActionVO vo = new DaAipActionVO();
        vo.setId(action.getAipActionId());
        vo.setActionType(DaAipActionType.fromValue(action.getActionType().name()));
        vo.setState(DaAipActionState.fromValue(daAipActionService.stateOf(action).name()));
        vo.setCreateDate(action.getCreateDate());
        vo.setFinishDate(action.getFinishDate());
        vo.setItems(action.getItems().stream().map(this::createAipActionItem).toList());
        return vo;
    }

    private DaAipActionItemVO createAipActionItem(DaAipActionItem item) {
        DaAipActionItemVO vo = new DaAipActionItemVO();
        vo.setAipId(item.getAip().getAipId());
        vo.setAipCode(item.getAip().getCode());
        vo.setState(DaAipActionItemState.fromValue(item.getState().name()));
        vo.setMessage(item.getMessage());
        vo.setFinishDate(item.getFinishDate());
        return vo;
    }

    private List<LinkedNodeVO> createLinkedNodeVOs(List<ArrDaoLink> daoLinks, Map<Integer, TreeNodeVO> treeNodeMap) {
        List<LinkedNodeVO> linkedNodeList = new ArrayList<>();
        for (ArrDaoLink daoLink : daoLinks) {
            TreeNodeVO treeNode = treeNodeMap.get(daoLink.getNodeId());
            linkedNodeList.add(new LinkedNodeVO(daoLink.getDaoLinkId(), treeNode.getId(), treeNode.getName()));
        }
        return linkedNodeList;
    }

    private LinkType mapArrDaoLink(ArrDaoLink.LinkType type) {
        switch (type) {
            case AIP -> {return LinkType.AIP;}
            case PART_AIP -> {return LinkType.PART_AIP;}
            case COMPONENT_AIP -> {return LinkType.COMPONENT_AIP;}
            default -> throw new IllegalArgumentException("Unknown link type: " + type);
        }
    }

    private QueueItemState mapQueueItemState(
            cz.tacr.elza.domain.DaSyncQueueItem.QueueItemState state) {
        switch (state) {
            case IMPORT_ERROR -> { return QueueItemState.IMPORT_ERROR; }
            case UPDATE -> { return QueueItemState.UPDATE; }
            case IMPORT_OK -> { return QueueItemState.IMPORT_OK; }
            case IMPORT_NEW -> { return QueueItemState.IMPORT_NEW; }
            case EXPORT_ERROR -> { return QueueItemState.EXPORT_ERROR; }
            case EXPORT_OK -> { return QueueItemState.EXPORT_OK; }
            case EXPORT_NEW -> { return QueueItemState.EXPORT_NEW; }
            default -> throw new BusinessException("Unrecognized queue item state", BaseCode.INVALID_STATE);
        }
    }

    public ExplorerTreeNodeFile createExplorerTreeNodeFile(DaDaoFile daDaoFile, List<ArrDaoLink> daoLinks, Map<Integer, TreeNodeVO> treeNodeMap) {
        ExplorerTreeNodeFile result = new ExplorerTreeNodeFile();
        result.setUuid(daDaoFile.getDao().getCode());
        result.setDaoFileId(daDaoFile.getDaoFileId());
        result.setChecksum(daDaoFile.getChecksum());
        result.setChecksumType(daDaoFile.getChecksumType());
        result.setMimeType(daDaoFile.getMimeType());
        if(daDaoFile.getSize() != null) {
            result.setSize(intValue(daDaoFile.getSize()));
        }
        result.setImageHeight(daDaoFile.getImageHeight());
        result.setImageWidth(daDaoFile.getImageWidth());
        result.setSourceXDimensionUnit(daDaoFile.getSourceXDimensionUnit());
        result.setSourceXDimensionValue(daDaoFile.getSourceXDimensionValue());
        result.setSourceYDimensionUnit(daDaoFile.getSourceYDimensionUnit());
        result.setSourceYDimensionValue(daDaoFile.getSourceYDimensionValue());
        result.setDuration(daDaoFile.getDuration());
        result.setDescription(daDaoFile.getDescription());
        result.setFilename(daDaoFile.getDao().getLabel());
        result.setDaoId(daDaoFile.getDao().getDaoId());
        if(daDaoFile.getDaoFileFolder() != null) {
            result.setDaoFileFolderId(daDaoFile.getDaoFileFolder().getDaoFileFolderId());
        }
        result.setLinkedNodes(createLinkedNodes(daoLinks, treeNodeMap));
        return result;
    }

    public List<LinkedNodeVO> createLinkedNodes(List<ArrDaoLink> daoLinks, Map<Integer, TreeNodeVO> treeNodeMap) {
        if (CollectionUtils.isNotEmpty(daoLinks)) {
            List<LinkedNodeVO> linkedNodeList = new ArrayList<>();
            for (ArrDaoLink daoLink : daoLinks) {
                TreeNodeVO treeNode = treeNodeMap.get(daoLink.getNodeId());
                if(treeNode != null) {
                    linkedNodeList.add(new LinkedNodeVO(daoLink.getDaoLinkId(), treeNode.getId(), treeNode.getName()));
                }
            }
            return linkedNodeList;
        }
        return null;
    }

    public ExplorerTreeNodeFile copyFile(ExplorerTreeNodeFile file) {
        ExplorerTreeNodeFile result = new ExplorerTreeNodeFile();
        result.setUuid(file.getUuid());
        result.setDaoId(file.getDaoId());
        result.setDaoFileFolderId(file.getDaoFileFolderId());
        result.setDaoFileId(file.getDaoFileId());
        result.setChecksum(file.getChecksum());
        result.setChecksumType(file.getChecksumType());
        result.setMimeType(file.getMimeType());
        result.setSize(file.getSize());
        result.setImageHeight(file.getImageHeight());
        result.setImageWidth(file.getImageWidth());
        result.setSourceXDimensionUnit(file.getSourceXDimensionUnit());
        result.setSourceXDimensionValue(file.getSourceXDimensionValue());
        result.setSourceYDimensionUnit(file.getSourceYDimensionUnit());
        result.setSourceYDimensionValue(file.getSourceYDimensionValue());
        result.setDuration(file.getDuration());
        result.setDescription(file.getDescription());
        result.setFilename(file.getFilename());
        if(file.getParentFolderLogical() != null) {
            result.setParentFolderLogical(file.getParentFolderLogical());
        }
        if(file.getParentFolder() != null) {
            result.setParentFolder(file.getParentFolder());
        }
        result.setLinkedNodes(file.getLinkedNodes());
        return result;
    }

    public ExplorerTreeNode createExplorerTreeNode(DaDaoFileFolder daDaoFileFolder, Map<Integer, List<ArrDaoLink>> daoLinkMap, Map<Integer, TreeNodeVO> treeNodeMap) {
        ExplorerTreeNode result = new ExplorerTreeNode();
        result.setUuid(UUID.nameUUIDFromBytes(daDaoFileFolder.getDaoFileFolderId().toString().getBytes()).toString());
        result.setDaoId(daDaoFileFolder.getRepresentationDao().getDaoId());
        result.setLinkedNodes(createLinkedNodes(daoLinkMap.getOrDefault(daDaoFileFolder.getRepresentationDao().getDaoId(), new ArrayList<>()), treeNodeMap));
        if(daDaoFileFolder.getParentFileFolder() != null) {
            result.setParentFolder(createExplorerTreeNode(daDaoFileFolder.getParentFileFolder(), daoLinkMap, treeNodeMap));
        }
        result.setLabel(daDaoFileFolder.getLabel());
        return result;
    }



	public NodeBase createNode(ArrNode arrNode) {
		NodeBase node = new NodeBase(arrNode.getNodeId(), arrNode.getVersion(), arrNode.getUuid());
		return node;
	}
}
