package cz.tacr.elza.ws.core.v1.daoservice;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.base.Objects;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoBatchInfo;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.packageimport.xml.SettingDaoImportLevel;
import cz.tacr.elza.repository.DaoBatchInfoRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.service.DaoSyncService.DaoDesctItemProvider;
import cz.tacr.elza.service.DaoSyncService.MatchedScenario;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.DescriptionItemServiceInternal;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.arrangement.DesctItemProvider;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.dao.DaoServiceInternal;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoBatchInfo;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DaoLink;
import cz.tacr.elza.ws.types.v1.DaoLinks;
import cz.tacr.elza.ws.types.v1.DaoPackage;
import cz.tacr.elza.ws.types.v1.DaoPackages;
import cz.tacr.elza.ws.types.v1.Daoset;
import cz.tacr.elza.ws.types.v1.Did;
import cz.tacr.elza.ws.types.v1.File;
import cz.tacr.elza.ws.types.v1.FolderGroup;
import cz.tacr.elza.ws.types.v1.Items;

@Service
public class DaoCoreServiceWsImpl {

    private Logger logger = LoggerFactory.getLogger(DaoCoreServiceWsImpl.class);

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private DaoBatchInfoRepository daoBatchInfoRepository;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    @Autowired
    private cz.tacr.elza.service.DaoService daoService;

    @Autowired
    private DaoServiceInternal daoServiceInternal;

    @Autowired
    private DaoSyncService daoSyncService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private DescriptionItemServiceInternal descriptionItemServiceInternal;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private EntityManager em;

    private ArrDigitalRepository getRepository(final DaoImport daoImport) {

        String repoCode = null;
        if (daoImport.getDaoPackages() != null && !CollectionUtils.isEmpty(daoImport.getDaoPackages()
                .getDaoPackage())) {
            final List<DaoPackage> daoPackageList = daoImport.getDaoPackages().getDaoPackage();

            Set<String> repoCodes = daoPackageList.stream()
                .map(dp -> dp.getRepositoryIdentifier()).collect(Collectors.toSet());
            if (repoCodes.size() != 1) {
                throw new ObjectNotFoundException("V jednom požadavku musí být použit všude jen jeden repozitář",
                        DigitizationCode.REPOSITORY_NOT_FOUND);
            }
            repoCode = repoCodes.iterator().next();
        }

        if(daoImport.getDaoPackages()!=null&&daoImport.getDaoLinks()!=null&&
        		!CollectionUtils.isEmpty(daoImport.getDaoLinks().getDaoLink())) {
            final List<DaoLink> daoLinkList = daoImport.getDaoLinks().getDaoLink();
            Set<String> repoCodes = daoLinkList.stream()
                    .map(dl -> dl.getRepositoryIdentifier()).collect(Collectors.toSet());
            if (repoCodes.size() != 1) {
                throw new ObjectNotFoundException("V jednom požadavku musí být použit všude jen jeden repozitář",
                        DigitizationCode.REPOSITORY_NOT_FOUND);
            }
            if (repoCode == null) {
                repoCode = repoCodes.iterator().next();
            } else if (!repoCode.equals(repoCodes.iterator().next())) {
                throw new ObjectNotFoundException("V jednom požadavku musí být použit všude jen jeden repozitář",
                        DigitizationCode.REPOSITORY_NOT_FOUND);
            }
        }

        if (repoCode == null) {
            throw new ObjectNotFoundException("V jednom požadavku musí být použit alespoň jeden repozitář",
                    DigitizationCode.REPOSITORY_NOT_FOUND);
        }

        ArrDigitalRepository repository = digitalRepositoryRepository.findOneByCode(repoCode);
        if (repository == null) {
            throw new ObjectNotFoundException("Nepodařilo se dohledat digitalRepository: " + repoCode,
                    DigitizationCode.REPOSITORY_NOT_FOUND)
                            .set("code", repoCode);
        }

        return repository;
    }

    @Transactional
    public void daoImport(final DaoImport daoImport) {
        Validate.notNull(daoImport, "DAO import musí být vyplněn");

        ArrDigitalRepository repository = getRepository(daoImport);
        ImportContext impCtx = new ImportContext(repository, arrangementInternalService, descriptionItemService);

        if (daoImport.getDaoPackages() != null) {
            daoImportPackages(impCtx, daoImport.getDaoPackages().getDaoPackage());
        }

        // flush before dao link query
        em.flush();

        Map<Integer, String> daoNodeUuidMap = new HashMap<>();

        // založí se DaoLink bez notifikace, pokud již link existuje, tak se zruší a založí se nový (arr_change).
        if (daoImport.getDaoLinks() != null && !CollectionUtils.isEmpty(daoImport.getDaoLinks().getDaoLink())) {
            final List<DaoLink> daoLinkList = daoImport.getDaoLinks().getDaoLink();
            // Only one repocode might be used during one import
            List<String> daoCodes = daoLinkList.stream().map(dl -> dl.getDaoIdentifier()).collect(Collectors.toList());

            // Get current DAOS
            List<ArrDao> dbDaos = daoService.findDaosByRepository(impCtx.getRepository(), daoCodes);
            Map<String, ArrDao> daoMap = dbDaos.stream()
                    .collect(Collectors.toMap(dao -> dao.getCode(), Function.identity()));

            List<Integer> nodeIds = new ArrayList<>();
            for (DaoLink daoLink : daoLinkList) {
                ArrDao dao = daoMap.get(daoLink.getDaoIdentifier());
                if (dao == null) {
                    throw new ObjectNotFoundException("Digitalizát s ID=" + daoLink.getDaoIdentifier() + " nenalezen",
                            DigitizationCode.DAO_NOT_FOUND).set("code", daoLink.getDaoIdentifier());
                }
                if (dao.getDaoType() == DaoType.LEVEL) {
                    daoNodeUuidMap.put(dao.getDaoId(), daoLink.getDidIdentifier());
                    continue;
                }
                // create daolinks only for pure daos (not DaoType.Level)
                // if link exists it is updated
                final ArrDaoLink arrDaoLink = createDaoLink(impCtx, daoLink, dao);
                nodeIds.add(arrDaoLink.getNodeId());
            }
            daoService.updateNodeCacheDaoLinks(nodeIds);
        }

        // Auto create new levels
        // Daos with DaoType.LEVEL
        List<ArrDao> daos = impCtx.getDaos();
        List<ArrDaoLink> daoLinks = daoLinkRepository.findActiveByDaos(daos);
        Map<Integer, ArrDaoLink> daoLinkMap = daoLinks.stream()
                .collect(Collectors.toMap(ArrDaoLink::getDaoId, Function.identity()));

        Map<ArrFund, List<ArrDao>> levelDaos = daos.stream().filter(dao -> dao.getDaoType() == DaoType.LEVEL)
                .collect(Collectors.groupingBy(d -> d.getDaoPackage().getFund(),
                                               Collectors.toList()));
        levelDaos.forEach((fund, list) -> onImportedDaoLevels(impCtx, fund, list, daoNodeUuidMap, daoLinkMap));

        impCtx.flush();
        em.flush();

    }

    private void daoImportPackages(ImportContext impCtx, List<DaoPackage> receivedDaoPackages) {
        if (CollectionUtils.isEmpty(receivedDaoPackages)) {
            return;
        }
        // prepare packageIds and fundIds
        List<String> daoIds = new ArrayList<>();
        for (DaoPackage daoPackage : receivedDaoPackages) {
            // read and check fund
            impCtx.getFundVersion(daoPackage.getFundIdentifier());

            if (StringUtils.isBlank(daoPackage.getIdentifier())) {
                throw new SystemException("Nebylo vyplněno povinné pole externího identifikátoru",
                        DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER);
            }
            Daoset daoset = daoPackage.getDaos();
            if (daoset != null && daoset.getDao() != null) {
                for (Dao dao : daoset.getDao()) {
                    String daoIdent = dao.getIdentifier();
                    daoIds.add(daoIdent);
                }
            }
        }
        // Try to find daos
        List<ArrDao> daos = daoRepository.findByCodes(impCtx.getRepository(), daoIds);
        impCtx.addDaos(daos);
        if (daos.size() > 0) {
            // add dao files
            List<ArrDaoFile> daoFiles = daoService.findDaoFiles(daos);
            impCtx.addDaoFiles(daoFiles);
        }
        // Check existing links to remove active scenarios
        List<ArrDaoLink> daoLinks = daoLinkRepository.findActiveByDaos(daos);
        for(ArrDaoLink daoLink: daoLinks) {
            String currScenario = daoLink.getScenario();
            // drop items from scenario
            DaoDesctItemProvider provider = daoSyncService.createDescItemProvider(daoLink.getDao(), currScenario);
            if(provider!=null) {
                ArrFund fund = daoLink.getDao().getDaoPackage().getFund();
                ArrChange change = impCtx.getChange(fund);
                ArrFundVersion fundVersion = impCtx.getFundVersion(fund);
                MultipleItemChangeContext changeContext = impCtx.getItemsChangeContext(fundVersion);
                ArrLevel level = fundLevelService.findLevelByNode(daoLink.getNode());
                provider.remove(level, change, fundVersion, changeContext);
            }
            if (currScenario != null) {
                // reset current scenario and set is as recommended
                logger.debug("Current DAO scenarion (daoId={}): {}", daoLink.getDaoId(), currScenario);

                impCtx.setRecommendedScenario(daoLink.getDao(), currScenario);
                daoLink.setScenario(null);
            }
        }

        for (DaoPackage xmlDaoPackage : receivedDaoPackages) {
            ArrDaoPackage daoPackage = impCtx.getDaoPackage(xmlDaoPackage.getIdentifier());
            if (daoPackage == null) {
                createDaoPackage(impCtx, xmlDaoPackage);
            } else {
                updateDaoPackage(impCtx, xmlDaoPackage, daoPackage);
            }
        }
        daoRepository.flush();
    }

    /**
     * Update existing Dao
     *
     * @param impCtx
     * @param xmlDaoPackage
     * @param daoPackage
     */
    private void updateDaoPackage(ImportContext impCtx, DaoPackage xmlDaoPackage, ArrDaoPackage daoPackage) {
        ArrFundVersion fundVersion = impCtx.getFundVersion(xmlDaoPackage.getFundIdentifier());
        if (!daoPackage.getFund().getFundId().equals(fundVersion.getFundId())) {
            throw new SystemException("Změna fondu u DAO není implementována",
                    BaseCode.SYSTEM_ERROR);
        }
        //@TODO: Do batch info update

        createDaos(impCtx, xmlDaoPackage.getDaos(), daoPackage);
    }

    private ArrDaoPackage createDaoPackage(ImportContext impCtx, final DaoPackage daoPackage) {
        Validate.notNull(daoPackage, "DAO obal musí být vyplněn");

        ArrFundVersion fundVersion = impCtx.getFundVersion(daoPackage.getFundIdentifier());

        //@TODO: Rewrite to use daoService for batch info processing
        ArrDaoBatchInfo arrDaoBatchInfo;
        final DaoBatchInfo daoBatchInfo = daoPackage.getDaoBatchInfo();
        if (daoBatchInfo != null) {
            arrDaoBatchInfo = daoBatchInfoRepository.findOneByCode(daoBatchInfo.getIdentifier());
            final String label = daoBatchInfo.getLabel();
            if (arrDaoBatchInfo == null) {
                arrDaoBatchInfo = new ArrDaoBatchInfo();
                arrDaoBatchInfo.setCode(daoBatchInfo.getIdentifier());
                arrDaoBatchInfo.setLabel(label);
                arrDaoBatchInfo = daoBatchInfoRepository.save(arrDaoBatchInfo);
            } else {
                if (!StringUtils.equals(arrDaoBatchInfo.getLabel(), label)) {
                    arrDaoBatchInfo.setLabel(label);
                    arrDaoBatchInfo = daoBatchInfoRepository.save(arrDaoBatchInfo);
                }
            }
        } else {
            arrDaoBatchInfo = null;
        }

        ArrDaoPackage arrDaoPackage = daoServiceInternal.createDaoPackage(fundVersion.getFund(),
                                                                  impCtx.getRepository(),
                                                                  daoPackage.getIdentifier(),
                                                                  arrDaoBatchInfo);

        createDaos(impCtx, daoPackage.getDaos(), arrDaoPackage);

        return arrDaoPackage;
    }

    @Transactional
    public String addPackage(DaoPackage daoPackage) {
        DaoImport daoImport = new DaoImport();
        DaoPackages daoPkgs = new DaoPackages();
        daoImport.setDaoPackages(daoPkgs);
        daoPkgs.getDaoPackage().add(daoPackage);

        this.daoImport(daoImport);

        return daoPackage.getIdentifier();
    }

    @Transactional
    public Did getDid(String didIdentifier) {
        final ArrNode arrNode = arrangementInternalService.findNodeByUuid(didIdentifier);
        Validate.notNull(arrNode, "Node ID=" + didIdentifier + " wasn't found.");

        return groovyScriptService.createDid(arrNode);
    }

    /**
     * Založí se DaoLink bez notifikace, pokud již link existuje, tak se zruší a
     * založí se nový (arr_change).
     *
     * @param impCtx
     *
     * @param daoLink
     * @param dao
     * @see
     *      cz.tacr.elza.ws.core.v1.DaoService#_import(cz.tacr.elza.ws.types.v1.DaoImport
     *      daoImport)
     * @return nově založený link
     */
    private ArrDaoLink createDaoLink(ImportContext impCtx, final DaoLink daoLink, ArrDao dao) {
        Validate.notNull(daoLink, "Link musí být vyplněn");
        Validate.notEmpty(daoLink.getDidIdentifier(), "DID identifier musí být vyplněn");

        final String didIdentifier = daoLink.getDidIdentifier();
        final ArrNode arrNode = arrangementInternalService.findNodeByUuid(didIdentifier);
        if (arrNode == null) {
            throw new ObjectNotFoundException("JP s ID=" + didIdentifier + " nenalezena",
                    ArrangementCode.NODE_NOT_FOUND).set("Uuid", daoLink.getDidIdentifier());
        }

        // daolink.daoIdentifier a didIdentifier existují a ukazují na shodný AS
        if (!arrNode.getFund().equals(dao.getDaoPackage().getFund())) {
            throw new BusinessException("DAO a Node okazují na různý package",
                    DigitizationCode.DAO_AND_NODE_HAS_DIFFERENT_PACKAGE);
        }

        // kontrola existence linku, zrušení
        final List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaoAndNodeAndDeleteChangeIsNull(dao, arrNode);
        if (daoLinkList.size() > 0) {
            logger.debug("Nalezen existující DaoLink mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + arrNode
                    .getNodeId() + ").");
            return daoLinkList.iterator().next();
        }

        ArrChange change = impCtx.getChange(dao.getDaoPackage().getFund());
        ArrFundVersion fundVersion = impCtx.getFundVersion(dao.getDaoPackage().getFund());

        ArrDaoLink arrDaoLink = daoService.createArrDaoLink(fundVersion, change, dao, arrNode, null);
        logger.debug("Automaticky založeno nové propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + arrNode
                .getNodeId() + ").");
        return arrDaoLink;
    }

    @Transactional
    public void link(DaoLink daoLink) {
        DaoImport data = new DaoImport();
        DaoLinks daoLinks = new DaoLinks();
        daoLinks.getDaoLink().add(daoLink);
        data.setDaoLinks(daoLinks);

        daoImport(data);

    }

    /**
     * Create/update levels for daos
     *
     * @param impCtx
     *
     * @param fund
     * @param levelDaos
     * @param daoNodeUuidMap
     * @param daoLinkMap
     */
    private void onImportedDaoLevels(ImportContext impCtx,
                                     ArrFund fund,
                                     List<ArrDao> levelDaos,
                                     Map<Integer, String> daoNodeUuidMap,
                                     Map<Integer, ArrDaoLink> daoLinkMap) {
        if (CollectionUtils.isEmpty(levelDaos)) {
            return;
        }

        List<UISettings> impSettings = settingsService.getGlobalSettings(UISettings.SettingsType.DAO_LEVEL_IMPORT);
        if (CollectionUtils.isEmpty(impSettings)) {
            logger.error("Missing settings: {}", UISettings.SettingsType.DAO_LEVEL_IMPORT);
            throw new SystemException("Missing settings: DAO_LEVEL_IMPORT", BaseCode.IMPORT_FAILED);
        }
        Validate.isTrue(impSettings.size() == 1);

        UISettings s = impSettings.get(0);
        LevelImportSettings lis = SettingDaoImportLevel.newInstance(s).getLevelImportSettings();
        prepareDaoLevels(impCtx, fund, lis, levelDaos, daoNodeUuidMap, daoLinkMap);
    }


    /**
     * Automatické založení úrovní
     *
     * @param impCtx
     *
     * @param lis
     * @param levelDaos
     * @param daoLinkMap
     * @param daoLevelNodeMap
     * @return List of new links
     */
    private List<ArrDaoLink> prepareDaoLevels(ImportContext impCtx,
                                              final ArrFund fund,
                                              final LevelImportSettings lis,
                                              final List<ArrDao> levelDaoList,
                                              Map<Integer, String> daoNodeUuidMap,
                                              Map<Integer, ArrDaoLink> daoLinkMap) {
        logger.debug("Prepare DAO levels, fundId: " + fund.getFundId());
        // prepare parent level
        final ArrFundVersion fundVersion = impCtx.getFundVersion(fund);
        final ArrNode rootNode = fundVersion.getRootNode();
        final StaticDataProvider sdp = staticDataService.getData();

        ArrChange change = impCtx.getChange(fund);

        // zpracovani jiz napojenych dao
        List<ArrDao> daosWithoutLevel = new ArrayList<>();
        for (ArrDao levelDao : levelDaoList) {
            ArrDaoLink daoLink = daoLinkMap.get(levelDao.getDaoId());
            if (daoLink == null) {
                // level not found
                daosWithoutLevel.add(levelDao);
            } else {
                updateDaoLevel(daoLink, change, fundVersion, impCtx);
            }
        }

        if (daosWithoutLevel.size() == 0) {
            // noting left to create -> simply return without creation of subfolder
            return Collections.emptyList();
        }

        DesctItemProvider descProvider = new DesctItemProvider() {

            @Override
            public List<ArrDescItem> provide(ArrLevel level, ArrChange change, ArrFundVersion fundVersion,
                                MultipleItemChangeContext changeContext) {
                ArrDescItem descItem = new ArrDescItem();
                ItemType itemType = sdp.getItemTypeByCode(lis.getDescItemType());
                ArrData data = null;

                Date date = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String strDate = dateFormat.format(date);
                String value = "Importováno - " + strDate;

                switch (itemType.getDataType()) {
                case STRING:
                    ArrDataString ds = new ArrDataString();
                    ds.setStringValue(value);
                    data = ds;
                    break;
                case TEXT:
                    ArrDataText dt = new ArrDataText();
                    dt.setTextValue(value);
                    data = dt;
                    break;
                default:
                    Validate.isTrue(false, "Cannot convert string to data type: %s, item type: %s", itemType
                            .getDataType(),
                                    lis.getDescItemType());
                }
                data.setDataType(itemType.getDataType().getEntity());
                descItem.setItemType(itemType.getEntity());
                descItem.setData(data);

                ArrDescItem createdItem = descriptionItemService.createDescriptionItemInBatch(descItem,
                                                                    level.getNode(), fundVersion, change,
                                                                    changeContext);

                return Collections.singletonList(createdItem);
            }
        };

        ArrLevel parentLevel = fundLevelService.addLevelUnder(fundVersion, rootNode,
                                                              lis.getScenarioName(),
                                                              descProvider, null,
                                                              change);

        List<ArrDaoLink> daoLinks = new ArrayList<>(daosWithoutLevel.size());
        // attach to the parent
        for (ArrDao dao : daosWithoutLevel) {
        	String uuid = daoNodeUuidMap.get(dao.getDaoId());

        	ArrDaoLink daoLink = prepareDaoLevel(fundVersion, parentLevel, change, dao, uuid);
            daoLinks.add(daoLink);
        }
        return daoLinks;
    }

    /**
     * Update daoLink and dao level according scenario
     *
     * @param daoLink
     * @param change
     * @param fundVersion
     * @param impCtx
     */
    private void updateDaoLevel(ArrDaoLink daoLink, ArrChange change,
                                ArrFundVersion fundVersion,
                                ImportContext impCtx) {
        // update linked node
        ArrDao dao = daoLink.getDao();
        Items items = daoSyncService.unmarshalItemsFromAttributes(dao);
        if (items == null) {
            logger.debug("Received DAO without items (daoId={}).", dao.getDaoId());
            return;
        }

        String scenario = impCtx.getRecommendedScenario(dao);
        logger.debug("Recommended scenario for DAO (daoId={}): {}", dao.getDaoId(), scenario);
        List<String> scenarios = daoSyncService.getAllScenarioNames(items);
        if (!scenarios.contains(scenario)) {
            logger.debug("Scenario was not found (daoId={}): {}, selecting first one",
                         dao.getDaoId(), scenario);
            // scenario not found -> select first one (if any)
            scenario = CollectionUtils.isNotEmpty(scenarios) ? scenarios.get(0) : null;
        } else {
            //
            logger.debug("Scenario was found (daoId={}): {}", dao.getDaoId(), scenario);
        }
        MultipleItemChangeContext itemChangeContext = impCtx.getItemsChangeContext(fundVersion);

        daoSyncService.setScenario(fundVersion, change, itemChangeContext, daoLink, scenario);
    }

    /**
     * Prepare single DAO level
     * @param fundVersion
     * @param parentLevel
     * @param change
     * @param dao
     * @param uuid
     * @return
     */
    private ArrDaoLink prepareDaoLevel(ArrFundVersion fundVersion, ArrLevel parentLevel,
    								   ArrChange change, ArrDao dao, String uuid) {
        logger.debug("Preparing DAO level, fundId: {}, daoId: {}", fundVersion.getFundId().toString(), dao.getDaoId());

        Validate.isTrue(dao.getDaoType() == DaoType.LEVEL);

        ArrNode parentNode = parentLevel.getNode();

        ArrNode linkNode = null;
        ArrLevel linkNodeLevel = null;
        if(uuid!=null) {
            // check if node and level exists
            ArrNode node = arrangementInternalService.findNodeByUuid(uuid);
            if(node!=null&&node.getFundId().equals(fundVersion.getFundId())) {
            	linkNode = node;
                // check if has active level
                ArrLevel level = fundLevelService.findLevelByNode(node);
                if (level != null) {
                    linkNodeLevel = level;
                }
            }
        }

        DaoDesctItemProvider descItemProvider = daoSyncService.createDescItemProvider(dao, null);
        String scenario = descItemProvider.getScenario();
        if (linkNodeLevel == null) {
        	if(linkNode==null) {
                ArrLevel level = fundLevelService.addLevelUnder(fundVersion, parentNode,
                                                                null, descItemProvider,
                                                                uuid, null);
                linkNode = level.getNode();
        	} else {
        		// remove previous items
        		List<ArrDescItem> descItems = descriptionItemServiceInternal.getDescItems(linkNode);
        		if(CollectionUtils.isNotEmpty(descItems)) {
        			descriptionItemService.deleteDescriptionItems(descItems, fundVersion, change, false, true);
        		}

        		linkNodeLevel = fundLevelService.addNewLevelForNode(fundVersion, parentLevel, change, linkNode, descItemProvider);
            }
        } else {
            logger.debug("Connecting DAO to existing Node, daoCode: {}, nodeId: {}",
                         dao.getCode(), linkNode.getNodeId());
            if (scenario != null) {
                scenario = updateForScenario(scenario, fundVersion, change, dao, linkNode, descItemProvider);
            }
        }

        ArrDaoLink daoLink = daoService.createOrFindDaoLink(fundVersion, change, dao, linkNode, scenario);
        return daoLink;
	}

    private String updateForScenario(String scenario, ArrFundVersion fundVersion, ArrChange change,
                                     ArrDao dao,
                                     ArrNode linkNode, DaoDesctItemProvider descItemProvider) {
        // get current data
        RestoredNode rn = nodeCacheService.getNode(linkNode.getNodeId());
        List<ArrDescItem> descItems = rn.getDescItems();
        MatchedScenario ms = daoSyncService.matchScenario(descItemProvider.getItems(), descItems);
        if (ms == null) {
            logger.error("Failed to match scenario to current data, daoCode: {}, nodeId: {}",
                         dao.getCode(), linkNode.getNodeId());
            throw new ObjectNotFoundException("Digitalizát s ID=" + dao.getCode() +
                    " nelze napojit na uzel nodeId = " + linkNode.getNodeId() +
                    " z důvodu nenalezení vhodného scénáře.",
                    DigitizationCode.DAO_NOT_FOUND).set("code", dao.getCode());
        }
        scenario = ms.getScenario();
        logger.debug("Found matching scenario: {}", scenario);
        List<ArrDescItem> readOnlyItems = ms.getReadOnlyItems();
        if (CollectionUtils.isNotEmpty(readOnlyItems)) {
            logger.debug("Changing selected items to readonly, nodeId: {}, items: {}",
                         linkNode.getNodeId(), readOnlyItems);

            List<ArrDescItem> updatedItems = new ArrayList<>(readOnlyItems.size());
            for (ArrDescItem src : readOnlyItems) {
                ArrDescItem di = new ArrDescItem(src);
                di.setReadOnly(true);
                updatedItems.add(di);
            }
            descriptionItemService.updateDescriptionItems(updatedItems, fundVersion, change, true);
        }
        List<ArrDescItem> missingItems = ms.getMissingItems();
        if (!CollectionUtils.isEmpty(missingItems)) {
            logger.debug("Adding items to , nodeId: {}, items: {}",
                         linkNode.getNodeId(), missingItems);
            descriptionItemService.createDescriptionItems(missingItems, linkNode, fundVersion, change);
        }
        return scenario;
    }

    private void createDaos(ImportContext impCtx, Daoset daoset, ArrDaoPackage dbDaoPackage) {
        if (daoset == null) {
            return;
        }
        for (Dao xmlDao : daoset.getDao()) {

            ArrDao dbDao = impCtx.getDao(xmlDao.getIdentifier());
            if (dbDao == null) {
                dbDao = daoSyncService.createDao(dbDaoPackage, xmlDao);
                impCtx.addDao(dbDao);
            } else {
                dbDao = updateDao(impCtx, dbDaoPackage, dbDao, xmlDao);
            }

            FolderGroup fg = xmlDao.getFolders();
            if (fg != null && fg.getFolder().size() > 0) {
                // Folders are not fully implemented
                throw new SystemException("Folders in DAOs are not supported", DigitizationCode.DAO_NOT_FOUND);

                /*
                for (Folder folder : fg.getFolder()) {

                    ArrDaoFileGroup arrDaoFileGroup = daoSyncService.createArrDaoFileGroup(dbDao,
                                                                                           folder);

                    if (folder.getFiles() != null) {
                        for (File file : folder.getFiles().getFile()) {
                            daoSyncService.createArrDaoFileGroup(arrDaoFileGroup, file);
                        }
                    }
                }*/
            }

            List<File> xmlFileList = xmlDao.getFiles()!=null?
                        xmlDao.getFiles().getFile():
                        Collections.emptyList();
            syncFiles(impCtx, dbDao, xmlFileList);
        }
    }

    /**
     * Synchronize files
     *
     * @param impCtx
     * @param dbDao
     * @param xmlFileList
     */
    private void syncFiles(final ImportContext impCtx,
                           final ArrDao dbDao,
                           final List<File> xmlFileList) {

        // get current files
        final List<ArrDaoFile> daoFiles = impCtx.getFiles(dbDao);
        final Map<String, ArrDaoFile> codeFileMap = new HashMap<>();
        daoFiles.forEach(df -> codeFileMap.put(df.getCode(), df));

        // compare with newly received files
        for (File xmlFile : xmlFileList) {
            String fileIdent = xmlFile.getIdentifier();
            ArrDaoFile dbDaoFile = codeFileMap.remove(fileIdent);
            if (dbDaoFile == null) {
                // file not exists -> create
                dbDaoFile = daoSyncService.createDaoFile(dbDao, xmlFile);
                impCtx.addDaoFile(dbDaoFile);
            } else {
                // update existing file
                dbDaoFile = daoSyncService.updateArrDaoFile(dbDaoFile, xmlFile);
            }
        }

        // Delete remaining files
        Collection<ArrDaoFile> daoFilesToDelete = codeFileMap.values();
        if (CollectionUtils.isNotEmpty(daoFilesToDelete)) {
            impCtx.removeDaoFiles(daoFilesToDelete);
            daoServiceInternal.deleteDaoFiles(daoFilesToDelete);
        }

    }

    private ArrDao updateDao(ImportContext impCtx, ArrDaoPackage dbDaoPackage, ArrDao dbDao, Dao xmlDao) {
        // check if dao parent match dbDaoPackage
        if (!dbDao.getDaoPackageId().equals(dbDaoPackage.getDaoPackageId())) {
            throw new SystemException("Same Dao but different DaoPackage", DigitizationCode.PACKAGE_NOT_FOUND);
        }
        // check dao type
        DaoType daoType = DaoSyncService.getDaoType(xmlDao.getDaoType());
        if (!dbDao.getDaoType().equals(daoType)) {
            throw new SystemException("Updated Dao has different type", DigitizationCode.DAO_NOT_FOUND);
        }
        if (!Objects.equal(dbDao.getLabel(), xmlDao.getLabel())) {
            dbDao.setLabel(xmlDao.getLabel());
        }
        String attrs = DaoSyncService.getDaoAttributes(xmlDao);
        if (!Objects.equal(dbDao.getAttributes(), attrs)) {
            dbDao.setAttributes(attrs);
        }
        return daoRepository.save(dbDao);
    }

    /*
    private void deleteArrDaoLink(ArrDao arrDao, ArrNode arrNode) {

        // kontrola existence linku, zrušení
        final List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaoAndNodeAndDeleteChangeIsNull(arrDao, arrNode);

        for (ArrDaoLink arrDaoLink : daoLinkList) {

            // vytvořit změnu
            final ArrChange deleteChange = arrangementInternalService.createChange(ArrChange.Type.DELETE_DAO_LINK, arrNode);

            // nastavit připojení na neplatné
            arrDaoLink.setDeleteChange(deleteChange);
            logger.debug("Propojení arrDaoLink(ID=" + arrDaoLink.getDaoLinkId()
                    + ") bylo automaticky zneplatněno novou změnou.");
            final ArrDaoLink resultDaoLink = daoLinkRepository.save(arrDaoLink);
        }
    }*/

    @Transactional
    public void removeDao(String daoIdentifier) {

        Assert.hasText(daoIdentifier, "Označení obalu musí být vyplněno");

        final ArrDao arrDao = daoRepository.findOneByCode(daoIdentifier);
        if (arrDao == null) {
            throw new ObjectNotFoundException("Digitalizát s ID=" + daoIdentifier + " nenalezen",
                    DigitizationCode.DAO_NOT_FOUND).set("code", daoIdentifier);
        }

        ArrDaoPackage daoPackage = arrDao.getDaoPackage();
        ArrFund fund = daoPackage.getFund();

        ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fund.getFundId());

        daoService.deleteDaosWithoutLinks(fundVersion, Collections.singletonList(arrDao));

        /* TODO: Maji se automaticky mazat daoPackage?
        // check if whole package can be deleted and delete it
        List<ArrDao> daos = daoService.findDaosByPackage(fund.getFundId(), daoPackage, null,
                                                         null,
                                                         false);
        if (!daos.stream().anyMatch(dao -> dao.getValid() == true)) {
            // all are invalid -> remove whole package
            daoService.deleteDaoPackageWithCascade(daoPackage);
        }*/

    }

    public void removePackage(String packageIdentifier) {
        Assert.hasText(packageIdentifier, "Označení obalu musí být vyplněno");

        final ArrDaoPackage arrDaoPackage = daoPackageRepository.findOneByCode(packageIdentifier);
        if (arrDaoPackage == null) {
            throw new ObjectNotFoundException("Balíček digitalizátů s ID=" + packageIdentifier + " nenalezen",
                    DigitizationCode.PACKAGE_NOT_FOUND).set("code", packageIdentifier);
        }

        ArrFund fund = arrDaoPackage.getFund();
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFund(fund);

        daoService.deleteDaoPackageWithCascade(fundVersion, arrDaoPackage);

    }

}
