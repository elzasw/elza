package cz.tacr.elza.ws.core.v1.daoservice;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoBatchInfo;
import cz.tacr.elza.domain.ArrDaoFileGroup;
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
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.packageimport.xml.SettingDaoImportLevel;
import cz.tacr.elza.repository.DaoBatchInfoRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.service.DaoSyncService.DaoDesctItemProvider;
import cz.tacr.elza.service.DaoSyncService.MatchedScenario;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.arrangement.DesctItemProvider;
import cz.tacr.elza.service.arrangement.MultiplItemChangeContext;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
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
import cz.tacr.elza.ws.types.v1.Folder;
import cz.tacr.elza.ws.types.v1.FolderGroup;

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
    private StaticDataService staticDataService;

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
        
        if(daoImport.getDaoPackages()!=null&&!CollectionUtils.isEmpty(daoImport.getDaoLinks().getDaoLink())) {
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

        ImportContext impCtx = new ImportContext(getRepository(daoImport));
        getRepository(daoImport);

        if (daoImport.getDaoPackages() != null && !CollectionUtils.isEmpty(daoImport.getDaoPackages()
                .getDaoPackage())) {
            List<DaoPackage> daoPackageList = daoImport.getDaoPackages().getDaoPackage();
            for (DaoPackage daoPackage : daoPackageList) {
                createArrDaoPackage(impCtx, daoPackage);
            }
            daoRepository.flush();
        }

        // Daos with DaoType.LEVEL 
        List<ArrDao> daos = impCtx.getDaos();

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
                // create daolinks only for pure daos (not DaoType.LeveL)
                final ArrDaoLink arrDaoLink = createArrDaoLink(daoLink, dao);
                nodeIds.add(arrDaoLink.getNodeId());
            }
            daoService.updateNodeCacheDaoLinks(nodeIds);
        }

        // Auto create new levels        
        Map<ArrFund, List<ArrDao>> levelDaos = daos.stream().filter(dao -> dao.getDaoType() == DaoType.LEVEL)
                .collect(Collectors.groupingBy(d -> d.getDaoPackage().getFund(),
                                               Collectors.toList()));
        levelDaos.forEach((fund, list) -> onImportedDaoLevels(fund, list, daoNodeUuidMap));

    }

    private ArrDaoPackage createArrDaoPackage(ImportContext impCtx, final DaoPackage daoPackage) {
        Validate.notNull(daoPackage, "DAO obal musí být vyplněn");

        ArrFund fund = arrangementService.getFundByString(daoPackage.getFundIdentifier());

        if (StringUtils.isBlank(daoPackage.getIdentifier())) {
            throw new SystemException("Nebylo vyplněno povinné pole externího identifikátoru",
                    DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER);
        }

        ArrDaoPackage arrDaoPackage = new ArrDaoPackage();
        arrDaoPackage.setFund(fund);
        arrDaoPackage.setDigitalRepository(impCtx.getRepository());

        arrDaoPackage.setCode(daoPackage.getIdentifier());

        final DaoBatchInfo daoBatchInfo = daoPackage.getDaoBatchInfo();
        if (daoBatchInfo != null) {
            ArrDaoBatchInfo arrDaoBatchInfo = daoBatchInfoRepository.findOneByCode(daoBatchInfo.getIdentifier());
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
            arrDaoPackage.setDaoBatchInfo(arrDaoBatchInfo);
        }

        arrDaoPackage = daoPackageRepository.save(arrDaoPackage);

        impCtx.addPackage(arrDaoPackage);

        List<ArrDao> daos = createDaos(daoPackage.getDaos(), arrDaoPackage);
        impCtx.addDaos(daos);

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
        Assert.notNull(arrNode, "Node ID=" + didIdentifier + " wasn't found.");

        return groovyScriptService.createDid(arrNode);
    }

    /**
     * Založí se DaoLink bez notifikace, pokud již link existuje, tak se zruší a
     * založí se nový (arr_change).
     *
     * @param daoLink
     * @param dao
     * @see
     *      cz.tacr.elza.ws.core.v1.DaoService#_import(cz.tacr.elza.ws.types.v1.DaoImport
     *      daoImport)
     * @return nově založený link
     */
    private ArrDaoLink createArrDaoLink(final DaoLink daoLink, ArrDao dao) {
        Assert.notNull(daoLink, "Link musí být vyplněn");
        Assert.hasText(daoLink.getDidIdentifier(), "DID identifier musí být vyplněn");

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

        ArrDaoLink arrDaoLink = createArrDaoLink(dao, arrNode);
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

    private void onImportedDaoLevels(ArrFund fund, List<ArrDao> levelDaos, Map<Integer, String> daoNodeUuidMap) {
        if (CollectionUtils.isEmpty(levelDaos)) {
            return;
        }

        List<UISettings> impSettings = settingsService.getGlobalSettings(UISettings.SettingsType.DAO_LEVEL_IMPORT);
        if (CollectionUtils.isEmpty(impSettings)) {
            logger.error("Missing settings: {}", UISettings.SettingsType.DAO_LEVEL_IMPORT);
            return;
        }
        Validate.isTrue(impSettings.size() == 1);

        UISettings s = impSettings.get(0);
        LevelImportSettings lis = SettingDaoImportLevel.newInstance(s).getLevelImportSettings();
        prepareDaoLevels(fund, lis, levelDaos, daoNodeUuidMap);
    }

    /**
     * Automatické založení úrovní
     * 
     * @param lis
     * @param levelDaos
     * @param daoLevelNodeMap
     * @return
     */
    private List<ArrDaoLink> prepareDaoLevels(final ArrFund fund,
                                              final LevelImportSettings lis,
                                              final List<ArrDao> levelDaos,
                                              Map<Integer, String> daoNodeUuidMap) {
        logger.debug("Prepare DAO levels, fundId: " + fund.getFundId());
        // prepare parent level
        final ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fund.getFundId());
        final ArrNode rootNode = fundVersion.getRootNode();
        final StaticDataProvider sdp = staticDataService.getData();

        DesctItemProvider descProvider = new DesctItemProvider() {

            @Override
            public void provide(ArrLevel level, ArrChange change, ArrFundVersion fundVersion,
                                MultiplItemChangeContext changeContext) {
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

                descriptionItemService.createDescriptionItemInBatch(descItem,
                                                                    level.getNode(), fundVersion, change,
                                                                    changeContext);
            }
        };
        List<ArrLevel> levels = fundLevelService.addNewLevel(fundVersion, rootNode, rootNode,
                                                      AddLevelDirection.CHILD,
                                                      lis.getScenarioName(), Collections.emptySet(),
                                                      descProvider, null);
        ArrChange change = levels.get(0).getCreateChange();
        ArrNode parentNode = levels.get(0).getNode();

        List<ArrDaoLink> daoLinks = new ArrayList<>(levelDaos.size());
        // attach to the parent
        for (ArrDao dao : levelDaos) {
            logger.debug("Preparing DAO level, fundId: {}, daoId: {}", fund.getFundId().toString(), dao.getDaoId());

            Validate.isTrue(dao.getDaoType() == DaoType.LEVEL);

            ArrNode linkNode = null;
            ArrLevel linkNodeLevel = null;
            String uuid = daoNodeUuidMap.get(dao.getDaoId());
            if(uuid!=null) {
                // check if node and level exists
                ArrNode node = arrangementInternalService.findNodeByUuid(uuid);
                if(node!=null&&node.getFundId().equals(fundVersion.getFundId())) {
                    // check if has active level                    
                    ArrLevel level = fundLevelService.findLevelByNode(node);
                    if (level != null) {
                        linkNode = node;
                        linkNodeLevel = level;
                    } else {
                        // Node exists but level is missing
                        // New level has to be created for the existing node
                        logger.error("Unsupported scenario, node with given UUID already exists, uuid: {}", uuid);
                        Validate.isTrue(false, "Unsupported scenario, node with given UUID already exists, uuid: "
                                + uuid);
                    }
                }
            }

            DaoDesctItemProvider descItemProvider = daoSyncService.createDescItemProvider(dao);
            String scenario = descItemProvider.getScenario();
            if (linkNodeLevel == null) {
                levels = fundLevelService.addNewLevel(fundVersion, parentNode, parentNode,
                                                      AddLevelDirection.CHILD, null, null,
                                                      descItemProvider, null);
                linkNode = levels.get(0).getNode();
            } else {
                if (scenario != null) {
                    logger.debug("Connecting DAO to existing Node, daoCode: {}, nodeId: {}",
                                 dao.getCode(), linkNode.getNodeId());
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
                    List<ArrDescItem> readOnlyItems = ms.getReadOnlyItems();
                    if(CollectionUtils.isNotEmpty(readOnlyItems)) {
                        logger.debug("Changing selected items to readonly, nodeId: {}, items: {}",
                                     linkNode.getNodeId(), readOnlyItems);

                        List<ArrDescItem> updatedItems = new ArrayList<>(readOnlyItems.size());
                        for(ArrDescItem src: readOnlyItems) {
                            ArrDescItem di = new ArrDescItem(src);
                            di.setReadOnly(true);
                            updatedItems.add(di);
                        }
                        this.descriptionItemService.updateDescriptionItems(updatedItems, fundVersion, change);
                    }
                    List<ArrDescItem> missingItems = ms.getMissingItems();
                    if (!CollectionUtils.isEmpty(missingItems)) {
                        logger.debug("Adding items to , nodeId: {}, items: {}",
                                     linkNode.getNodeId(), missingItems);
                        this.descriptionItemService.createDescriptionItems(missingItems, linkNode, fundVersion, change);
                    }
                }
            }

            ArrDaoLink daoLink = daoService.createOrFindDaoLink(fundVersion, dao, linkNode, scenario);
            daoLinks.add(daoLink);
        }
        return daoLinks;
    }

    private List<ArrDao> createDaos(Daoset daoset, ArrDaoPackage arrDaoPackage) {
        if (daoset == null) {
            return Collections.emptyList();
        }
        List<ArrDao> result = new ArrayList<>(daoset.getDao().size());
        for (Dao dao : daoset.getDao()) {

            ArrDao arrDao = daoSyncService.createArrDao(arrDaoPackage, dao);
            result.add(arrDao);

            if (dao.getFiles() != null) {
                for (File file : dao.getFiles().getFile()) {
                    daoSyncService.createArrDaoFile(arrDao, file);
                }
            }

            FolderGroup fg = dao.getFolders();
            if (fg != null) {
                for (Folder folder : fg.getFolder()) {

                    ArrDaoFileGroup arrDaoFileGroup = daoSyncService.createArrDaoFileGroup(arrDao,
                                                                                           folder);

                    if (folder.getFiles() != null) {
                        for (File file : folder.getFiles().getFile()) {
                            daoSyncService.createArrDaoFileGroup(arrDaoFileGroup, file);
                        }
                    }
                }
            }
        }
        return result;
    }

    private ArrDaoLink createArrDaoLink(ArrDao arrDao, ArrNode arrNode) {

        // vytvořit změnu
        final ArrChange createChange = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);

        // vytvořit připojení
        ArrDaoLink arrDaoLink = new ArrDaoLink();
        arrDaoLink.setCreateChange(createChange);
        arrDaoLink.setDao(arrDao);
        arrDaoLink.setNode(arrNode);

        return daoLinkRepository.save(arrDaoLink);
    }

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
    }

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

        daoService.deleteDaosWithoutLinks(fund, Collections.singletonList(arrDao));

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

        // původní zneplatnění - nahrazeno skutečným kaskádovým smazáním - výslovně požadováno 10.1. LightCompem
        // final List<ArrDao> arrDaoList = daoService.deleteDaosWithoutLinks(arrDaos);

        daoService.deleteDaoPackageWithCascade(arrDaoPackage);

    }

}
