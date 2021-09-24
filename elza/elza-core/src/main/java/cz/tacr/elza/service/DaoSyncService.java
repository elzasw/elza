package cz.tacr.elza.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.google.common.collect.Lists;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.DaoSyncService.DaoDesctItemProvider;
import cz.tacr.elza.service.arrangement.DesctItemProvider;
import cz.tacr.elza.service.arrangement.MultiplItemChangeContext;
import cz.tacr.elza.ws.WsClient;
import cz.tacr.elza.ws.core.v1.WSHelper;
import cz.tacr.elza.ws.types.v1.ChecksumType;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncResponse;
import cz.tacr.elza.ws.types.v1.Daoset;
import cz.tacr.elza.ws.types.v1.Dids;
import cz.tacr.elza.ws.types.v1.File;
import cz.tacr.elza.ws.types.v1.FileGroup;
import cz.tacr.elza.ws.types.v1.Folder;
import cz.tacr.elza.ws.types.v1.FolderGroup;
import cz.tacr.elza.ws.types.v1.ItemString;
import cz.tacr.elza.ws.types.v1.Items;
import cz.tacr.elza.ws.types.v1.NonexistingDaos;
import cz.tacr.elza.ws.types.v1.ObjectFactory;
import cz.tacr.elza.ws.types.v1.UnitOfMeasure;

/**
 * Servisní metody pro synchronizaci digitizátů.
 */
@Service
public class DaoSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DaoSyncService.class);

    /**
     * Velikost dávky pro synchronizaci.
     */
    private static final int SYNC_DAOS_BATCH_SIZE = 100;

    // --- dao ---

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    @Autowired
    DescItemRepository descItemRepository;

    @Autowired
    private ChangeRepository changeRepository;

    // --- services ---

    @Autowired
    private DaoService daoService;

    @Autowired
    private ArrangementService arrangementService;
    
    @Autowired
    ArrangementInternalService arrangementInternalService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserService userService;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    FundLevelService fundLevelService;

    @Autowired
    private WSHelper wsHelper;

    // --- fields ---

    @Autowired
    private WsClient wsClient;

    @PersistenceContext
    private EntityManager entityManager;

    static JAXBContext jaxItemsContext;

    private ObjectFactory wsObjectFactory = new ObjectFactory();

    static {
        try {
            jaxItemsContext = JAXBContext.newInstance(Items.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize JAXB Context", e);
        }
    }

    public class DaoDesctItemProvider implements DesctItemProvider {

        private Items items;
        private String scenario;

        public DaoDesctItemProvider(Items items, String scenario) {
            this.items = items;
            this.scenario = scenario;
        }

        public Items getItems() {
            return items;
        }

        @Override
        public void provide(ArrLevel level, ArrChange change, ArrFundVersion fundVersion,
                            MultiplItemChangeContext changeContext) {
            String filtredScenario = getFirstOrGivenScenario(items, scenario);
            // zadaný scenar nebyl nalezen
            if (scenario != null && filtredScenario == null) {
                logger.error("Specified scenario={} not found.", scenario);
                throw new BusinessException("Specified scenario not found", PackageCode.SCENARIO_NOT_FOUND);
            }

            for (Object item : getFiltredItems(items, filtredScenario)) {
                ArrDescItem descItem = prepare(item);
                descriptionItemService.createDescriptionItemInBatch(descItem,
                                                                    level.getNode(), fundVersion, change,
                                                                    changeContext);
            }
        }

        public void remove(ArrLevel level, ArrChange change, ArrFundVersion fundVersion,
                           MultiplItemChangeContext changeContext) {
            String filtredScenario = getFirstOrGivenScenario(items, scenario);
            // zadaný scenar nebyl nalezen
            if (scenario != null && filtredScenario == null) {
                logger.error("Specified scenario={} not found.", scenario);
                throw new BusinessException("Specified scenario not found", PackageCode.SCENARIO_NOT_FOUND);
            }

            // prepare old items
            List<ArrDescItem> prevScenarioItems = new ArrayList<>();
            for (Object item : getFiltredItems(items, filtredScenario)) {
                ArrDescItem descItem = prepare(item);
                prevScenarioItems.add(descItem);
            }

            List<ArrDescItem> dbItems = descItemRepository.findByNodeAndDeleteChangeIsNull(level.getNode());
            for (ArrDescItem dbItem : dbItems) {
                // check if item from scenario
                if (isItemFromScenario(dbItem, prevScenarioItems)) {
                    descriptionItemService.deleteDescriptionItem(dbItem, fundVersion, change, false, changeContext);
                }
            }
        }

        private boolean isItemFromScenario(ArrDescItem dbItem, List<ArrDescItem> scenarioItems) {
            for (ArrDescItem scenarioItem : scenarioItems) {
                if (scenarioItem.getItemTypeId().equals(dbItem.getItemTypeId()) &&
                        Objects.equals(scenarioItem.getItemSpecId(), dbItem.getItemSpecId())) {
                    // TODO: check value of item
                    return true;
                }
            }
            return false;
        }

        private List<Object> getFiltredItems(Items items, String scenario) {
            // items neobsahují název scénáře
            if (scenario == null) {
                return items.getStrOrLongOrEnm();
            }
            // vybereme items daného scénáře
            boolean filterOn = false;
            List<Object> filtredItems = new ArrayList<>();
            for (Object item : items.getStrOrLongOrEnm()) {
                if (isScenario(item)) {
                    filterOn = getItemStringValue(item).equals(scenario);
                } else { // název scénáře se neuloží do seznamu
                    if (filterOn) {
                        filtredItems.add(item);
                    }
                }
            }
            return filtredItems;
        }

        private String getFirstOrGivenScenario(Items items, String scenario) {
            for (Object item : items.getStrOrLongOrEnm()) {
                if (isScenario(item)) {
                    if (scenario == null) {
                        return getItemStringValue(item);
                    } else {
                        if (getItemStringValue(item).equals(scenario)) {
                            return scenario;
                        }
                    }
                }
            }
            return null;
        }

        public String getScenario() {
            return scenario;
        }

    }

    // --- methods ---

    private ArrDescItem prepare(Object item) {
        ArrDescItem di = new ArrDescItem();
        wsHelper.convertItem(di, item);
        return di;
    }

    /**
     * Změnit scénář
     * 
     * @param daoId
     * @param scenario
     */
    public void changeScenario(Integer daoId, String scenario) {
        ArrDao dao = daoRepository.getOne(daoId);
        if (dao == null) {
            throw new ObjectNotFoundException("ArrDao ID=" + daoId + " not found", DigitizationCode.DAO_NOT_FOUND).set("daoId", daoId);
        }
        List<ArrDaoLink> daoLinks = daoLinkRepository.findByDaoAndDeleteChangeIsNull(dao);
        if (daoLinks.size() != 1) {
            throw new ObjectNotFoundException("ArrDao ID=" + daoId + " missing single dao link",
                    DigitizationCode.DAO_NOT_FOUND).set("daoId", daoId);
        }
        ArrDaoLink daoLink = daoLinks.get(0);
        ArrNode node = daoLink.getNode();        
        ArrFund fund = node.getFund();
        ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(fund.getFundId());
        ArrLevel level = fundLevelService.findLevelByNode(node);
        
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CHANGE_SCENARIO_ITEMS, node);
        
        Items items = unmarshalItemsFromAttributes(dao.getAttributes(), daoId);

        MultiplItemChangeContext changeContext = descriptionItemService.createChangeContext(fundVersion.getFundVersionId());
        // odstraneni puvodnich zaznamu
        DaoDesctItemProvider daoDesctItemProviderOrig = new DaoDesctItemProvider(items, daoLink.getScenario());
        daoDesctItemProviderOrig.remove(level, change, fundVersion, changeContext);
        
        DaoDesctItemProvider daoDesctItemProviderNew = new DaoDesctItemProvider(items, scenario);
        daoDesctItemProviderNew.provide(level, change, fundVersion, changeContext);
        
        // store new scenario
        daoLink.setScenario(scenario);
        
        changeContext.flush();
    }

    /**
     * Zavolá WS pro synchronizaci digitalizátů a aktualizuje metadata pro daný node a DAO.
     *
     * @param fundVersionId verze AS
     * @param dao           DAO pro synchronizaci
     * @param node          node pro synchronizaci
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void syncDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion, ArrNode node) {

        List<ArrDaoLink> daoLinks = daoLinkRepository.findActiveByNode(node);

        if (!daoLinks.isEmpty()) {

            daoPackageRepository.findAllById(daoLinks.stream().map(daoLink -> daoLink.getDao().getDaoPackageId()).collect(toList()));  // nacist do Hibernate session

            Map<Integer, List<ArrDao>> daosByDigitalRepository = daoLinks.stream()
                    .collect(groupingBy(vo -> vo.getDao().getDaoPackage().getDigitalRepository().getExternalSystemId(), mapping(vo -> vo.getDao(), toList())));

            for (Map.Entry<Integer, List<ArrDao>> entry : daosByDigitalRepository.entrySet()) {

                Integer externalSystemId = entry.getKey();
                ArrDigitalRepository digitalRepository = digitalRepositoryRepository.getOneCheckExist(externalSystemId);

                List<ArrDao> daos = entry.getValue();
                List<DaoSyncRequest> list = daos.stream().map(dao -> createDaoSyncRequest(node, dao)).collect(toList());

                Dids dids = new Dids();
                dids.getDid().add(groovyScriptService.createDid(node));

                DaosSyncRequest daosSyncRequest = createDaosSyncRequest(fundVersion, list, dids);
                DaosSyncResponse daosSyncResponse = wsClient.syncDaos(daosSyncRequest, digitalRepository);

                processDaosSyncResponse(fundVersion.getFund(), daosSyncResponse);
            }
        }
    }

    public DaosSyncRequest createDaosSyncRequest(ArrDaoRequest request) {

        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(request.getFund().getFundId());
        if (fundVersion == null) {
            throw new ObjectNotFoundException("Nenalezena otevřená verze AS", ArrangementCode.FUND_VERSION_NOT_FOUND)
                    .setId(request.getFund().getFundId());
        }

        List<ArrDao> daos = daoRequestDaoRepository.findDaoByDaoRequest(request);

        List<ArrDaoLink> arrDaoLinks = daoLinkRepository.findActiveByDaos(daos);

        List<DaoSyncRequest> list = arrDaoLinks.stream()
                .map(arrDaoLink -> createDaoSyncRequest(arrDaoLink.getNode(), arrDaoLink.getDao()))
                .collect(toList());

        Dids dids = new Dids();
        List<ArrNode> arrNodes = arrDaoLinks.stream().map(ArrDaoLink::getNode).collect(toList());
        if (CollectionUtils.isNotEmpty(arrNodes)) {
            dids.getDid().addAll(groovyScriptService.createDids(arrNodes));
        }

        return createDaosSyncRequest(fundVersion, list, dids);
    }

    private DaosSyncRequest createDaosSyncRequest(ArrFundVersion fundVersion, List<DaoSyncRequest> list, final Dids dids) {

        DaosSyncRequest request = new DaosSyncRequest();
        request.setDids(dids);
        request.setFundIdentifier(fundVersion.getRootNode().getUuid());
        request.setUsername(getUsername());

        request.getDaoSyncRequests().addAll(list);

        return request;
    }

    private DaoSyncRequest createDaoSyncRequest(ArrNode node, ArrDao dao) {
        DaoSyncRequest daoSyncRequest = new DaoSyncRequest();
        daoSyncRequest.setDidId(node.getUuid());
        daoSyncRequest.setDaoId(dao.getCode());
        return daoSyncRequest;
    }

    /**
     * Provede aktualizaci metadat.
     * 
     * @param arrFund
     *            fund
     *
     * @param daosSyncResponse
     *            response z WS {@code syncDaos}
     */
    public void processDaosSyncResponse(ArrFund arrFund, DaosSyncResponse daosSyncResponse) {
        deleteDaos(arrFund, daosSyncResponse.getNonExistDaos());
        updateDaos(daosSyncResponse.getDaos());
    }

    private void deleteDaos(ArrFund arrFund, NonexistingDaos nonexistingDaos) {
        if (nonexistingDaos != null) {
            List<String> daoCodes = nonexistingDaos.getDaoId();
            if (!daoCodes.isEmpty()) {
                List<ArrDao> arrDaos = daoRepository.findByCodes(daoCodes);
                daoService.deleteDaos(arrFund, arrDaos, false);
            }
        }
    }

    private void updateDaos(Daoset daoset) {
        if (daoset == null) {
            return;
        }

        List<Dao> daoList = daoset.getDao();
        if (daoList.isEmpty()) {
            return;
        }

        Map<String, ArrDao> daoCache = daoRepository.findByCodes(daoList.stream().map(dao -> dao.getIdentifier()).collect(toList()))
                .stream().collect(toMap(dao -> dao.getCode(), dao -> dao));

        for (Dao dao : daoList) {

            ArrDao arrDao = daoCache.get(dao.getIdentifier());
            if (arrDao != null) {
                arrDao.setLabel(dao.getLabel());
                arrDao.setValid(true);
                arrDao = daoRepository.save(arrDao);
            } else {
                logger.warn("Neplatné DAO [code=\"" + dao.getIdentifier() + "]");
            }

            updateFiles(dao.getFiles());

            FolderGroup fg = dao.getFolders();
            if (fg != null) {
                updateRelatedFileGroup(fg.getFolder());
            }
        }
    }

    private void updateFiles(FileGroup fileGroup) {
        if (fileGroup == null) {
            return;
        }

        List<File> fileList = fileGroup.getFile();
        if (fileList.isEmpty()) {
            return;
        }

        Map<String, ArrDaoFile> fileCache = daoFileRepository.findByCodes(fileList.stream().map(file -> file.getIdentifier()).collect(toList()))
                .stream().collect(toMap(file -> file.getCode(), file -> file));

        for (File file : fileList) {
            ArrDaoFile arrDaoFile = fileCache.get(file.getIdentifier());
            if (arrDaoFile != null) {
                updateArrDaoFile(arrDaoFile, file);
                daoFileRepository.save(arrDaoFile);
            } else {
                logger.warn("Neplatný DAO file [code=\"" + file.getIdentifier() + "]");
            }
        }
    }

    private void updateRelatedFileGroup(List<Folder> relatedFileGroupList) {
        if (relatedFileGroupList == null) {
            return;
        }
        if (relatedFileGroupList.isEmpty()) {
            return;
        }

        Map<String, ArrDaoFileGroup> groupCache = daoFileGroupRepository.findByCodes(relatedFileGroupList.stream().map(group -> group.getIdentifier()).collect(toList()))
                .stream().collect(toMap(group -> group.getCode(), group -> group));

        for (Folder relatedFileGroup : relatedFileGroupList) {

            updateFiles(relatedFileGroup.getFiles());
        }
    }

    /**
     * Spustí asynchronní synchronizaci digitalizátů a aktualizuje metadata pro všechny nody z AS, které mají připojené DAO.
     *
     * @param fundVersionId verze AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void syncDaosAll(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {

        int i = 0;

        for (Map.Entry<Integer, List<Integer>> entry : groupDaoIdsByDigitalRepository(fundVersionId).entrySet()) {

            Integer externalSystemId = entry.getKey();

            for (List<Integer> daoIds : Lists.partition(entry.getValue(), SYNC_DAOS_BATCH_SIZE)) {

                syncDaos(externalSystemId, fundVersionId, daoIds);
                entityManager.flush();

                // promazat hibernate session po zpracovani vetsiho mnozstvi zaznamu
                i += daoIds.size();
                if (i > 2000) {
                    entityManager.clear();
                    i = 0;
                }
            }
        }
    }

    private Map<Integer, List<Integer>> groupDaoIdsByDigitalRepository(@NotNull Integer fundVersionId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        return daoRepository.findValidDaoExternalSystemByFund(fundVersion.getFund().getFundId()).stream()
                .collect(groupingBy(vo -> vo.getExternalSystemId(), mapping(vo -> vo.getDaoId(), toList())));
    }

    private void syncDaos(@NotNull Integer externalSystemId, @NotNull Integer fundVersionId, @NotNull List<Integer> daoIds) {

        ArrDigitalRepository digitalRepository = digitalRepositoryRepository.getOneCheckExist(externalSystemId);

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);

        List<ArrDao> daos = daoRepository.findAllById(daoIds);

        // natahnout packages do hibernate session, protoze v createDaoRequest se package z DAO pouzivaji
        daoPackageRepository.findAllById(daos.stream().map(dao -> dao.getDaoPackage().getDaoPackageId()).collect(toSet()));

        ArrDaoRequest daoRequest = requestService.createDaoRequest(daos, null, ArrDaoRequest.Type.SYNC, fundVersion, digitalRepository);

        requestService.sendRequest(daoRequest, fundVersion);
    }

    public ArrDao createArrDao(ArrDaoPackage arrDaoPackage, Dao dao) {
        if (StringUtils.isBlank(dao.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("dao.identifier", dao.getIdentifier());
        }
        ArrDao arrDao = new ArrDao();
        arrDao.setCode(dao.getIdentifier());
        arrDao.setLabel(dao.getLabel());
        arrDao.setValid(true);

        // serialize attributes
        Items items = dao.getItems();
        if (items != null) {

            JAXBElement<Items> elemAttrs = wsObjectFactory.createDescriptionItems(items);
            
            try (StringWriter sw = new StringWriter()) {
                Marshaller mar = jaxItemsContext.createMarshaller();
                mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            
                mar.marshal(elemAttrs, sw);
            
                arrDao.setAttributes(sw.toString());
            } catch (IOException | JAXBException e) {
                logger.error("Failed to serialize attributes to XML: " + e.getMessage());
                throw new SystemException("Failed to serialize attributes to XML", e,
                        BaseCode.INVALID_STATE)
                        .set("dao.identifier", dao.getIdentifier());
            
            }
        }

        DaoType daoType;
        if (dao.getDaoType() != null) {
            daoType = DaoType.valueOf(dao.getDaoType().name());
        } else {
            daoType = DaoType.ATTACHMENT;
        }
        arrDao.setDaoType(daoType);
        arrDao.setDaoPackage(arrDaoPackage);
        return daoRepository.save(arrDao);
    }

    public ArrDaoFileGroup createArrDaoFileGroup(ArrDao arrDao, Folder relatedFileGroup) {
        if (StringUtils.isBlank(relatedFileGroup.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("relatedFileGroup.identifier", relatedFileGroup.getIdentifier());
        }
        ArrDaoFileGroup arrDaoFileGroup = new ArrDaoFileGroup();
        arrDaoFileGroup.setCode(relatedFileGroup.getIdentifier());
        arrDaoFileGroup.setLabel(relatedFileGroup.getLabel());
        arrDaoFileGroup.setDao(arrDao);
        return daoFileGroupRepository.save(arrDaoFileGroup);
    }

    public ArrDaoFile createArrDaoFileGroup(ArrDaoFileGroup arrDaoFileGroup, File file) {
        if (StringUtils.isBlank(file.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("file.identifier", file.getIdentifier());
        }
        ArrDaoFile arrDaoFile = new ArrDaoFile();
        arrDaoFile.setCode(file.getIdentifier());
        arrDaoFile.setDaoFileGroup(arrDaoFileGroup);
        updateArrDaoFile(arrDaoFile, file);
        return daoFileRepository.save(arrDaoFile);
    }

    public ArrDaoFile createArrDaoFile(ArrDao arrDao, File file) {
        if (StringUtils.isBlank(file.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("file.identifier", file.getIdentifier());
        }
        ArrDaoFile arrDaoFile = new ArrDaoFile();
        arrDaoFile.setCode(file.getIdentifier());
        arrDaoFile.setDao(arrDao);
        updateArrDaoFile(arrDaoFile, file);
        return daoFileRepository.save(arrDaoFile);
    }

    public void updateArrDaoFile(ArrDaoFile arrDaoFile, File file) {
        if (file.getChecksumType() != null) {
            arrDaoFile.setChecksumType(getChecksumType(file.getChecksumType()));
        }
        arrDaoFile.setChecksum(file.getChecksum());
        if (file.getCreated() != null) {
            arrDaoFile.setCreated(file.getCreated().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
        }
        arrDaoFile.setMimetype(file.getMimetype());
        arrDaoFile.setSize(file.getSize());
        if (file.getImageHeight() != null) {
            arrDaoFile.setImageHeight(file.getImageHeight().intValueExact());
        }
        if (file.getImageHeight() != null) {
            arrDaoFile.setImageWidth(file.getImageWidth().intValueExact());
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceXDimesionUnit(getDimensionUnit(file.getSourceXDimensionUnit()));
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceXDimesionValue(file.getSourceXDimensionValue().doubleValue());
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceYDimesionUnit(getDimensionUnit(file.getSourceYDimensionUnit()));
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceYDimesionValue(file.getSourceYDimensionValue().doubleValue());
        }
        arrDaoFile.setDuration(file.getDuration());

        arrDaoFile.setDescription(file.getDescription());
        arrDaoFile.setFileName(file.getName());
    }

    private cz.tacr.elza.api.UnitOfMeasure getDimensionUnit(final UnitOfMeasure sourceDimensionUnit) {
        if (sourceDimensionUnit == null) {
            return null;
        }
        switch (sourceDimensionUnit) {
            case IN:
                return cz.tacr.elza.api.UnitOfMeasure.IN;
            case MM:
                return cz.tacr.elza.api.UnitOfMeasure.MM;
            default:
                throw new BusinessException("Neplatná jednotka: " + sourceDimensionUnit, PackageCode.PARSE_ERROR);
        }
    }

    private ArrDaoFile.ChecksumType getChecksumType(final ChecksumType checksumType) {
        if (checksumType == null) {
            return null;
        }
        switch (checksumType) {
            case MD_5:
                return ArrDaoFile.ChecksumType.MD5;
            case SHA_1:
                return ArrDaoFile.ChecksumType.SHA1;
            case SHA_256:
                return ArrDaoFile.ChecksumType.SHA256;
            case SHA_384:
                return ArrDaoFile.ChecksumType.SHA384;
            case SHA_512:
                return ArrDaoFile.ChecksumType.SHA512;
            default:
                throw new BusinessException("Neplatný checksum typ: " + checksumType, PackageCode.PARSE_ERROR);
        }
    }

    private String getUsername() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail != null && userDetail.getId() != null) {
            return userDetail.getUsername();
        }
        return null;
    }

    public DaoDesctItemProvider createDescItemProvider(ArrDao dao) {
        Items items = unmarshalItemsFromAttributes(dao.getAttributes(), dao.getDaoId());
        if (items == null) {
            return null;
        }
        List<String> scenarios = this.getAllScenarioNames(items);
        String scenario;
        if (CollectionUtils.isNotEmpty(scenarios)) {
            scenario = scenarios.get(0);
        } else {
            scenario = null;
        }
        return new DaoDesctItemProvider(items, scenario);
    }

    public Items unmarshalItemsFromAttributes(String attrs, Integer daoId) {
        if (StringUtils.isBlank(attrs)) {
            return null;
        }
        try (StringReader reader = new StringReader(attrs)) {
            Unmarshaller unmar = jaxItemsContext.createUnmarshaller();
            JAXBElement<Items> items = unmar.unmarshal(new StreamSource(reader), Items.class);
            return items.getValue();
        } catch (JAXBException e) {
            logger.error("Failed to unmarshall attributes: {}, exception: ", attrs, e);
            throw new BusinessException("Neplatné atributy dao objektu", PackageCode.PARSE_ERROR)
                    .set("attributes", attrs).set("daoId", daoId);
        }
    }

    public List<String> getAllScenarioNames(Items items) {
        List<String> scenarios = new ArrayList<>();
        for (Object item : items.getStrOrLongOrEnm()) {
            if (isScenario(item)) {
                scenarios.add(getItemStringValue(item));
            }
        }
        return scenarios;
    }

    private boolean isScenario(Object item) {
        if (item instanceof ItemString) {
            return ((ItemString) item).getType().equals("_ELZA_SCENARIO");
        }
        return false;
    }

    private String getItemStringValue(Object item) {
        if (item instanceof ItemString) {
            return ((ItemString) item).getValue();
        }
        return null;
    }

    static public class MatchedScenario {

        String scenario;

        List<ArrDescItem> readOnlyItems = new ArrayList<>();

        public MatchedScenario(final String scenario, final List<ArrDescItem> roItems) {
            this.scenario = scenario;
            this.readOnlyItems.addAll(roItems);
        }

        public String getScenario() {
            return scenario;
        }

        public List<ArrDescItem> getReadOnlyItems() {
            return readOnlyItems;
        }

    };

    public MatchedScenario matchScenario(Items items, List<ArrDescItem> descItems) {
        List<Object> itms = items.getStrOrLongOrEnm();
        String scenario = null;

        List<ArrDescItem> readOnlyItems = new ArrayList<>();

        for (Object itm : itms) {
            if (isScenario(itm)) {
                if (scenario != null) {
                    // Scenario was found
                    break;
                }
                scenario = getItemStringValue(itm);
            } else if (scenario != null) {
                ArrDescItem expectedItem = prepare(itm);
                ArrDescItem descItem = findItem(expectedItem, descItems);
                if (descItem == null) {
                    // item not found -> reset check
                    readOnlyItems.clear();
                    scenario = null;
                }
                // check readonly status
                if (expectedItem.getReadOnly() != null && expectedItem.getReadOnly()) {
                    if (descItem.getReadOnly() == null || !descItem.getReadOnly()) {
                        readOnlyItems.add(descItem);
                    }
                }
            }

        }

        if (scenario == null) {
            return null;
        }
        MatchedScenario ms = new MatchedScenario(scenario, readOnlyItems);
        return ms;
    }

    private ArrDescItem findItem(ArrDescItem expectedItem, List<ArrDescItem> descItems) {
        for (ArrDescItem descItem : descItems) {
            if (!Objects.equals(expectedItem.getItemTypeId(), descItem.getItemTypeId())) {
                continue;
            }
            if (!Objects.equals(expectedItem.getItemSpecId(), descItem.getItemSpecId())) {
                continue;
            }
        }
        return null;
    }

}
