package cz.tacr.elza.ws.core.v1.daoservice;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.arrangement.DesctItemProvider;
import cz.tacr.elza.service.arrangement.MultiplItemChangeContext;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoBatchInfo;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DaoLink;
import cz.tacr.elza.ws.types.v1.DaoPackage;
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
    private NodeRepository nodeRepository;

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
    private ArrangementService arrangementService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private StaticDataService staticDataService;

    @Transactional
    public void daoImport(final DaoImport daoImport) {
        Validate.notNull(daoImport, "DAO import musí být vyplněn");
        Validate.notNull(daoImport.getDaoPackages(), "DAO obaly musí být vyplněny");
        Validate.notNull(daoImport.getDaoLinks(), "DAO linky musí být vyplněny");

        ImportContext impCtx = new ImportContext();

        final List<DaoPackage> daoPackageList = daoImport.getDaoPackages().getDaoPackage();
        if (CollectionUtils.isNotEmpty(daoPackageList)) {
            for (DaoPackage daoPackage : daoPackageList) {
                createArrDaoPackage(impCtx, daoPackage);
            }
        }

        // založí se DaoLink bez notifikace, pokud již link existuje, tak se zruší a založí se nový (arr_change).
        if (daoImport.getDaoLinks() != null) {
            List<Integer> nodeIds = new ArrayList<>();
            final List<DaoLink> daoLinkList = daoImport.getDaoLinks().getDaoLink();
            if (CollectionUtils.isNotEmpty(daoLinkList)) {
                for (DaoLink daoLink : daoLinkList) {
                    final ArrDaoLink arrDaoLink = createArrDaoLink(daoLink);
                    nodeIds.add(arrDaoLink.getNodeId());
                }
            }
            daoService.updateNodeCacheDaoLinks(nodeIds);
        }

        // Auto create new levels
        List<ArrDao> daos = impCtx.getDaos();
        Map<ArrFund, List<ArrDao>> levelDaos = daos.stream().filter(dao -> dao.getDaoType() == DaoType.LEVEL)
                .collect(Collectors.groupingBy(d -> d.getDaoPackage().getFund(),
                                               Collectors.toList()));
        levelDaos.forEach((fund, list) -> onImportedDaoLevels(fund, list));

    }

    private ArrDaoPackage createArrDaoPackage(ImportContext impCtx, final DaoPackage daoPackage) {
        Validate.notNull(daoPackage, "DAO obal musí být vyplněn");

        ArrFund fund = arrangementService.getFundByString(daoPackage.getFundIdentifier());

        ArrDigitalRepository repository = digitalRepositoryRepository.findOneByCode(daoPackage
                .getRepositoryIdentifier());
        if (repository == null) {
            throw new ObjectNotFoundException("Nepodařilo se dohledat digitalRepository: " + daoPackage
                    .getRepositoryIdentifier(), DigitizationCode.REPOSITORY_NOT_FOUND).set("code", daoPackage
                            .getRepositoryIdentifier());
        }
        if (StringUtils.isBlank(daoPackage.getIdentifier())) {
            throw new SystemException("Nebylo vyplněno povinné pole externího identifikátoru",
                    DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER);
        }

        ArrDaoPackage arrDaoPackage = new ArrDaoPackage();
        arrDaoPackage.setFund(fund);
        arrDaoPackage.setDigitalRepository(repository);

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
        ImportContext impCtx = new ImportContext();
        final ArrDaoPackage arrDaoPackage = createArrDaoPackage(impCtx, daoPackage);
        return arrDaoPackage.getCode();
    }

    @Transactional
    public Did getDid(String didIdentifier) {
        final ArrNode arrNode = nodeRepository.findOneByUuid(didIdentifier);
        Assert.notNull(arrNode, "Node ID=" + didIdentifier + " wasn't found.");

        return groovyScriptService.createDid(arrNode);
    }

    /**
     * Založí se DaoLink bez notifikace, pokud již link existuje, tak se zruší a
     * založí se nový (arr_change).
     *
     * @param daoLink
     * @see
     *      cz.tacr.elza.ws.core.v1.DaoService#_import(cz.tacr.elza.ws.types.v1.DaoImport
     *      daoImport)
     * @return nově založený link
     */
    private ArrDaoLink createArrDaoLink(final DaoLink daoLink) {
        Assert.notNull(daoLink, "Link musí být vyplněn");
        Assert.hasText(daoLink.getDaoIdentifier(), "DAO identifier musí být vyplněn");
        Assert.hasText(daoLink.getDidIdentifier(), "DID identifier musí být vyplněn");

        final String daoIdentifier = daoLink.getDaoIdentifier();
        final String didIdentifier = daoLink.getDidIdentifier();
        final ArrDao arrDao = daoRepository.findOneByCode(daoIdentifier);
        final ArrNode arrNode = nodeRepository.findOneByUuid(didIdentifier);

        // daolink.daoIdentifier a didIdentifier existují a ukazují na shodný AS
        if (arrDao == null) {
            throw new ObjectNotFoundException("Digitalizát s ID=" + daoIdentifier + " nenalezen",
                    DigitizationCode.DAO_NOT_FOUND).set("code", daoLink.getDaoIdentifier());
        }
        if (arrNode == null) {
            throw new ObjectNotFoundException("JP s ID=" + didIdentifier + " nenalezena",
                    ArrangementCode.NODE_NOT_FOUND).set("Uuid", daoLink.getDidIdentifier());
        }
        if (!arrNode.getFund().equals(arrDao.getDaoPackage().getFund())) {
            throw new BusinessException("DAO a Node okazují na různý package",
                    DigitizationCode.DAO_AND_NODE_HAS_DIFFERENT_PACKAGE);
        }

        deleteArrDaoLink(arrDao, arrNode);

        ArrDaoLink arrDaoLink = createArrDaoLink(arrDao, arrNode);
        logger.debug("Automaticky založeno nové propojení mezi DAO(ID=" + arrDao.getDaoId() + ") a node(ID=" + arrNode
                .getNodeId() + ").");
        return arrDaoLink;
    }

    @Transactional
    public void link(DaoLink daoLink) {
        final ArrDaoLink arrDaoLink = createArrDaoLink(daoLink);

        daoService.updateNodeCacheDaoLinks(Collections.singletonList(arrDaoLink.getNodeId()));
    }

    private void onImportedDaoLevels(ArrFund fund, List<ArrDao> levelDaos) {
        if (CollectionUtils.isEmpty(levelDaos)) {
            return;
        }

        List<UISettings> impSettings = settingsService.getGlobalSettings(UISettings.SettingsType.DAO_LEVEL_IMPORT);
        if (CollectionUtils.isEmpty(impSettings)) {
            return;
        }
        Validate.isTrue(impSettings.size() == 1);

        UISettings s = impSettings.get(0);
        LevelImportSettings lis = SettingDaoImportLevel.newInstance(s).getLevelImportSettings();
        prepareDaoLevels(fund, lis, levelDaos);
    }

    /**
     * Automatické založení úrovní
     * 
     * @param lis
     * @param levelDaos
     * @return
     */
    private List<ArrDaoLink> prepareDaoLevels(final ArrFund fund, final LevelImportSettings lis,
                                  final List<ArrDao> levelDaos) {
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
                    ds.setValue(value);
                    data = ds;
                    break;
                case TEXT:
                    ArrDataText dt = new ArrDataText();
                    dt.setValue(value);
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
        ArrLevel level = fundLevelService.addNewLevel(fundVersion, rootNode, rootNode,
                                                      AddLevelDirection.CHILD,
                                                      lis.getScenarioName(), Collections.emptySet(),
                                                      descProvider);

        List<ArrDaoLink> daoLinks = new ArrayList<>(levelDaos.size());
        // attach to the parent
        for (ArrDao dao : levelDaos) {
            Validate.isTrue(dao.getDaoType() == DaoType.LEVEL);

            DesctItemProvider descItemProvider = daoSyncService.createDescItemProvider(dao);
            ArrLevel daoLevel = fundLevelService.addNewLevel(fundVersion, level.getNode(), level.getNode(),
                                                             AddLevelDirection.CHILD, null, null,
                                                             descItemProvider);
            ArrDaoLink daoLink = daoService.createOrFindDaoLink(fundVersion, dao, daoLevel.getNode());
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
        final ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);

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
            final ArrChange deleteChange = arrangementService.createChange(ArrChange.Type.DELETE_DAO_LINK, arrNode);

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
