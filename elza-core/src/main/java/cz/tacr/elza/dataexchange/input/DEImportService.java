package cz.tacr.elza.dataexchange.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.xml.stream.XMLStreamException;

import cz.tacr.elza.dataexchange.input.parts.context.PartsContext;
import cz.tacr.elza.dataexchange.input.reader.handlers.PartElementHandler;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.*;
import org.apache.commons.lang3.Validate;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.dataexchange.input.DEImportParams.ImportPositionParams;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionsContext;
import cz.tacr.elza.dataexchange.input.reader.XmlElementReader;
import cz.tacr.elza.dataexchange.input.reader.handlers.AccessPointElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.InstitutionElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.SectionElementHandler;
import cz.tacr.elza.dataexchange.input.sections.context.ImportPosition;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.NodeCacheService;

import static cz.tacr.elza.core.db.HibernateConfiguration.MAX_IN_SIZE;
import static cz.tacr.elza.repository.ExceptionThrow.scope;
import static cz.tacr.elza.repository.ExceptionThrow.version;

/**
 * Service for data-exchange import.
 */
@Service
public class DEImportService {

    private final ImportInitHelper initHelper;

    private final EntityManager em;

    private final UserService userService;

    private final StaticDataService staticDataService;

    private final NodeCacheService nodeCacheService;

    private final ScopeRepository scopeRepository;

    private final FundVersionRepository fundVersionRepository;

    private final LevelTreeCacheService levelTreeCacheService;

    private final AccessPointDataService apDataService;

    private final AccessPointItemService apItemService;

    private final ResourcePathResolver resourcePathResolver;

    private final DataUriRefRepository dataUriRefRepository;

    private final NodeRepository nodeRepository;

    @Autowired
    public DEImportService(EntityManager em,
                           ApAccessPointRepository apRepository,
                           ArrangementService arrangementService,
                           ApBindingRepository bindingRepository,
                           InstitutionRepository institutionRepository,
                           UserService userService,
                           StaticDataService staticDataService,
                           NodeCacheService nodeCacheService,
                           ApExternalSystemRepository externalSystemRepository,
                           InstitutionTypeRepository institutionTypeRepository,
                           GroovyScriptService groovyScriptService,
                           ScopeRepository scopeRepository,
                           FundVersionRepository fundVersionRepository,
                           LevelRepository levelRepository,
                           LevelTreeCacheService levelTreeCacheService,
                           StructObjValueService structObjService,
                           AccessPointService accessPointService,
                           AccessPointDataService apDataService,
                           ResourcePathResolver resourcePathResolver,
                           DmsService dmsService,
                           ApStateRepository apStateRepository,
                           ApPartRepository apPartRepository,
                           AccessPointItemService apItemService,
                           ApItemRepository apItemRepository,
                           ApBindingStateRepository bindingStateRepository,
                           DataUriRefRepository dataUriRefRepository,
                           NodeRepository nodeRepository) {
        this.initHelper = new ImportInitHelper(groovyScriptService, institutionRepository, institutionTypeRepository,
                arrangementService, levelRepository, apRepository, bindingRepository,
                structObjService, accessPointService,
                dmsService, apStateRepository, apPartRepository, apItemRepository, apItemService, bindingStateRepository);
        this.em = em;
        this.userService = userService;
        this.staticDataService = staticDataService;
        this.nodeCacheService = nodeCacheService;
        this.scopeRepository = scopeRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.levelTreeCacheService = levelTreeCacheService;
        this.apDataService = apDataService;
        this.resourcePathResolver = resourcePathResolver;
        this.apItemService = apItemService;
        this.dataUriRefRepository = dataUriRefRepository;
        this.nodeRepository = nodeRepository;
    }

    public List<String> getTransformationNames() throws IOException {
        Path transformDir = resourcePathResolver.getImportXmlTrasnformDir();
        if (!Files.exists(transformDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> files = Files.list(transformDir);) {
            return files
                    .filter(p -> p.endsWith(".xslt"))
                    .map(p -> p.getFileName().toString())
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted().collect(Collectors.toList());
        }
    }

    public void validateData(InputStream is) {
        URL xsdSchema = getClass().getResource("/schema/elza-schema-v2.xsd");
        if (xsdSchema == null) {
            throw new SystemException("DataExchange XSD schema not found");
        }
        try {
            XmlUtils.validateXml(is, xsdSchema, null);
        } catch (SAXException e) {
            throw new DEImportException("XML import file is not valid", e);
        }
    }

    @Transactional(TxType.REQUIRED)
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN})
    public void importData(InputStream is, DEImportParams params) {

        checkScopePermissions(params.getScopeId());
        checkParameters(params);

        Session session = HibernateUtils.getCurrentSession(em);
        ImportContext context = initContext(params, session);
        XmlElementReader reader = prepareReader(is, context, params.isIgnoreRootNodes());
        FlushMode origFlushMode = configureBatchSession(session, params.getBatchSize());
        try {
            // read XML
            reader.readDocument();

            // finish import
            context.finish();

            // restore all uri refs
            restoreNodeUriRefs();

            // sync node cache with all new nodes
            nodeCacheService.syncCache();

            // clear level tree cache if imported to existing fund
            ImportPosition importPosition = context.getSections().getImportPostition();
            if (importPosition != null) {
                levelTreeCacheService.invalidateFundVersion(importPosition.getFundVersion());
            }
        } catch (XMLStreamException e) {
            throw new SystemException(e);
        } finally {
            restoreSessionConfiguration(session, origFlushMode);
        }
    }

    /**
     * Vyhledá všechny nepropojené odkazy na JP (typu {@value cz.tacr.elza.domain.factory.DescItemFactory#ELZA_NODE},
     * které nemají vazbu na node a pokusí je podle UUID dohledat a propojit je.
     *
     * Je třeba dohledávat všechny napříč celou DB, protože může nastat případ, kdy se importují po sobě dvě AS,
     * které mají navzájem provazbené JP.
     */
    private void restoreNodeUriRefs() {
        int page = 0;
        Page<ArrDataUriRef> dataPage;
        do {
            dataPage = dataUriRefRepository.findByUnresolvedNodeRefs(PageRequest.of(page, MAX_IN_SIZE));
            List<ArrDataUriRef> uriRefs = dataPage.getContent();
            if (uriRefs.size() == 0) {
                break;
            }
            Map<String, List<ArrDataUriRef>> uuidDataUriRefMap = createUuidDataUriRefMap(uriRefs);
            connectNodes(uuidDataUriRefMap);
            dataUriRefRepository.saveAll(uriRefs);
            page++;
        } while (page < dataPage.getTotalPages());
    }

    /**
     * Dohledá JP podle uuid a propojí je v uri refs.
     *
     * @param uuidDataUriRefMap mapa uuid -> list uri refs
     */
    private void connectNodes(final Map<String, List<ArrDataUriRef>> uuidDataUriRefMap) {
        Set<String> uuids = uuidDataUriRefMap.keySet();
        List<ArrNode> nodes = nodeRepository.findAllByUuidIn(uuids);
        for (ArrNode node : nodes) {
            List<ArrDataUriRef> refs = uuidDataUriRefMap.get(node.getUuid());
            refs.forEach(r -> r.setArrNode(node));
        }
    }

    /**
     * Sestavení mapy pro napojení JP (podle UUID) na konkrétní záznamy
     *
     * @param uriRefs seznam odkazů na JP
     * @return sestavená mapa uuid -> list uri refs
     */
    private Map<String, List<ArrDataUriRef>> createUuidDataUriRefMap(final List<ArrDataUriRef> uriRefs) {
        Map<String, List<ArrDataUriRef>> uuidDataUriRefMap = new HashMap<>();
        for (ArrDataUriRef uriRef : uriRefs) {
            URI tempUri = URI.create(uriRef.getValue()).normalize();
            String uuid = tempUri.getAuthority();
            List<ArrDataUriRef> dataUriRefs = uuidDataUriRefMap.computeIfAbsent(uuid, k -> new ArrayList<>());
            dataUriRefs.add(uriRef);
        }
        return uuidDataUriRefMap;
    }

    private void checkScopePermissions(int importScopeId) {
        if (userService.hasPermission(Permission.ADMIN)) {
            return;
        }
        if (userService.hasPermission(Permission.AP_SCOPE_WR, importScopeId)) {
            return;
        }
        throw Authorization.createAccessDeniedException(Permission.AP_SCOPE_WR);
    }

    /**
     * Check if all parameters are logically consistent
     *
     * @param params
     */
    private void checkParameters(DEImportParams params) {
        Validate.isTrue(params.getBatchSize() > 0, "Import batch size must be greater than 0");
        Validate.isTrue(params.getMemoryScoreLimit() > 0, "Import memory score limit must be greater than 0");
        // to ignore root nodes, position have to be set
        if (params.isIgnoreRootNodes()) {
            ImportPositionParams posParams = params.getPositionParams();
            Validate.notNull(posParams);
        }
    }

    /**
     * Prepare ImportContext object
     *
     * @param params
     * @param session
     * @return
     */
    private ImportContext initContext(DEImportParams params, Session session) {
        // create AP change holder
        ApChangeHolder apChangeHolder = new ApChangeHolder(apDataService, session);
        ObjectIdHolder objectIdHolder = new ObjectIdHolder(apItemService, session);

        // init storage manager
        StorageManager storageManager = new StorageManager(params.getMemoryScoreLimit(), session, apChangeHolder,
                initHelper);

        // find import scope
        ApScope importScope = scopeRepository.findById(params.getScopeId())
                .orElseThrow(scope(params.getScopeId()));

        // get static data for current transaction
        StaticDataProvider staticData = staticDataService.getData();

        // initialize phase contexts
        AccessPointsContext apContext = new AccessPointsContext(storageManager, params.getBatchSize(), importScope,
                apChangeHolder, staticData, initHelper, objectIdHolder);
        InstitutionsContext institutionsContext = new InstitutionsContext(storageManager, params.getBatchSize(),
                initHelper);
        SectionsContext sectionsContext = initSectionsContext(storageManager, params, importScope, staticData);
        PartsContext partsContext = new PartsContext(storageManager, params.getBatchSize(), apContext, staticData, initHelper, objectIdHolder);

        // initialize context
        ImportContext context = new ImportContext(session, staticData, apContext, institutionsContext,
                sectionsContext, partsContext, storageManager);
        context.init(params.getImportPhaseChangeListeners());

        return context;
    }

    private SectionsContext initSectionsContext(StorageManager storageManager,
                                                DEImportParams params,
                                                ApScope importScope,
                                                StaticDataProvider staticData) {
        ArrangementService arrangementService = initHelper.getArrangementService();

        // create global import change
        ArrChange createChange = arrangementService.createChange(Type.IMPORT);

        // prepare import position
        ImportPositionParams posParams = params.getPositionParams();
        ImportPosition pos = null;
        if (posParams != null) {
            // find fund version
            ArrFundVersion fundVersion = fundVersionRepository.findById(posParams.getFundVersionId())
                    .orElseThrow(version(posParams.getFundVersionId()));
            // find & lock parent level
            ArrLevel parentLevel = arrangementService.lockLevel(posParams.getParentNode(), fundVersion);
            // find & lock target level
            ArrLevel targetLevel = null;
            if (posParams.getTargetNode() != null) {
                targetLevel = arrangementService.lockLevel(posParams.getTargetNode(), fundVersion);
            }
            pos = new ImportPosition(fundVersion, parentLevel, targetLevel, posParams.getDirection());
        }

        return new SectionsContext(storageManager, params.getBatchSize(), createChange, importScope, pos, staticData,
                initHelper);
    }

    private static XmlElementReader prepareReader(InputStream is, ImportContext context, boolean ignoreRootNodes) {
        XmlElementReader reader;
        try {
            reader = XmlElementReader.create(is);
        } catch (XMLStreamException e) {
            throw new SystemException("Failed to prepare import source", e);
        }
        reader.addElementHandler("/edx/aps/ap", new AccessPointElementHandler(context));
        reader.addElementHandler("/edx/pars/pg", new PartElementHandler(context));
        reader.addElementHandler("/edx/pars/per", new PartElementHandler(context));
        reader.addElementHandler("/edx/pars/evnt", new PartElementHandler(context));
        reader.addElementHandler("/edx/inss/inst", new InstitutionElementHandler(context));
        reader.addElementHandler("/edx/fs/s", new SectionElementHandler(context, reader, ignoreRootNodes));
        return reader;
    }

    private static FlushMode configureBatchSession(Session session, int batchSize) {
        FlushMode origFlushMode = session.getHibernateFlushMode();
        session.setHibernateFlushMode(FlushMode.COMMIT);
        session.setJdbcBatchSize(batchSize);
        return origFlushMode;
    }

    private static void restoreSessionConfiguration(Session session, FlushMode origFlushMode) {
        session.setHibernateFlushMode(origFlushMode);
        session.setJdbcBatchSize(null);
    }
}
