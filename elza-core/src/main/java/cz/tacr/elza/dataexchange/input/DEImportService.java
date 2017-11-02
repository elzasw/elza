package cz.tacr.elza.dataexchange.input;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.Validate;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.aop.Authorization;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.input.DEImportParams.ImportPositionParams;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionsContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartiesContext;
import cz.tacr.elza.dataexchange.input.reader.XmlElementReader;
import cz.tacr.elza.dataexchange.input.reader.handlers.AccessPointElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.EventElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.FamilyElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.FundInfoElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.InstitutionElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.PartyGroupElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.PersonElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.SectionElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.SectionLevelElementHandler;
import cz.tacr.elza.dataexchange.input.reader.handlers.SectionPacketElementHandler;
import cz.tacr.elza.dataexchange.input.sections.context.SectionStorageDispatcher;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext.ImportPosition;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegVariantRecordRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.utils.HibernateUtils;
import cz.tacr.elza.utils.XmlUtils;

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

    private final String transformationsDir;

    @Autowired
    public DEImportService(EntityManager em,
                           RegRecordRepository recordRepository,
                           ArrangementService arrangementService,
                           RegCoordinatesRepository coordinatesRepository,
                           RegVariantRecordRepository variantRecordRepository,
                           PartyRepository partyRepository,
                           PartyNameRepository nameRepository,
                           PartyNameComplementRepository nameComplementRepository,
                           PartyGroupIdentifierRepository groupIdentifierRepository,
                           UnitdateRepository unitdateRepository,
                           InstitutionRepository institutionRepository,
                           UserService userService,
                           StaticDataService staticDataService,
                           NodeCacheService nodeCacheService,
                           RegExternalSystemRepository externalSystemRepository,
                           InstitutionTypeRepository institutionTypeRepository,
                           GroovyScriptService groovyScriptService,
                           ScopeRepository scopeRepository,
                           FundVersionRepository fundVersionRepository,
                           LevelRepository levelRepository,
                           LevelTreeCacheService levelTreeCacheService,
                           @Value("${elza.xmlImport.transformationDir}") String transformationsDir) {

        this.initHelper = new ImportInitHelper(externalSystemRepository, groovyScriptService, institutionRepository,
                institutionTypeRepository, arrangementService, levelRepository, recordRepository, coordinatesRepository,
                variantRecordRepository, partyRepository, nameRepository, nameComplementRepository, groupIdentifierRepository,
                unitdateRepository);
        this.em = em;
        this.userService = userService;
        this.staticDataService = staticDataService;
        this.nodeCacheService = nodeCacheService;
        this.scopeRepository = scopeRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.levelTreeCacheService = levelTreeCacheService;
        this.transformationsDir = transformationsDir;
    }

    public List<String> getTransformationNames() {
        return XmlUtils.getXsltFileNames(transformationsDir);
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
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ADMIN })
    public void importData(InputStream is, DEImportParams params) {

        checkScopePermissions(params.getScopeId());
        checkParameters(params);

        Session session = HibernateUtils.getCurrentSession(em);
        ImportContext context = initContext(params, session);
        XmlElementReader reader = prepareReader(is, context);
        FlushMode origFlushMode = configureBatchSession(session, params.getBatchSize());
        try {
            reader.readDocument();
            context.setCurrentPhase(ImportPhase.FINISHED);
            nodeCacheService.syncCache();
            // clear lever tree cache
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

    private void checkScopePermissions(int importScopeId) {
        if (!userService.hasPermission(Permission.ADMIN) && !userService.hasPermission(Permission.REG_SCOPE_WR, importScopeId)) {
            throw Authorization.createAccessDeniedException(Permission.REG_SCOPE_WR);
        }
    }

    private void checkParameters(DEImportParams params) {
        Validate.isTrue(params.getBatchSize() > 0, "Import batch size must be greater than 0");
        Validate.isTrue(params.getMemoryScoreLimit() > 0, "Import memory score limit must be greater than 0");
    }

    private ImportContext initContext(DEImportParams params, Session session) {
        // init storage manager
        StorageManager storageManager = new StorageManager(params.getMemoryScoreLimit(), session, initHelper);

        // find import scope
        RegScope importScope = scopeRepository.findOne(params.getScopeId());
        if (importScope == null) {
            throw new SystemException("Import scope not found, id:" + params.getScopeId());
        }

        // get static data for current transaction
        StaticDataProvider staticData = staticDataService.getData();

        // initialize contexts
        AccessPointsContext apContext = new AccessPointsContext(storageManager, params.getBatchSize(), importScope, initHelper);
        PartiesContext partiesContext = new PartiesContext(storageManager, params.getBatchSize(), apContext, session, initHelper);
        InstitutionsContext institutionsContext = new InstitutionsContext(storageManager, params.getBatchSize(), initHelper);
        SectionsContext sectionsContext = initSectionsContext(storageManager, params, importScope, staticData);

        ImportContext context = new ImportContext(session, staticData, apContext, partiesContext, institutionsContext,
                sectionsContext);

        // register listeners
        params.getImportPhaseChangeListeners().forEach(context::registerPhaseChangeListener);

        return context;
    }

    private SectionsContext initSectionsContext(StorageManager storageManager,
                                                DEImportParams params,
                                                RegScope scope,
                                                StaticDataProvider staticData) {
        ArrangementService arrangementService = initHelper.getArrangementService();

        // create global import change
        ArrChange createChange = arrangementService.createChange(Type.IMPORT);

        // init storage dispatcher
        SectionStorageDispatcher storageDispatcher = new SectionStorageDispatcher(storageManager, params.getBatchSize());

        // prepare import position
        ImportPositionParams posParams = params.getPositionParams();
        ImportPosition pos = null;
        if (posParams != null) {
            // find fund version
            ArrFundVersion fundVersion = fundVersionRepository.findOne(posParams.getFundVersionId());
            // find & lock parent level
            ArrLevel parentLevel = arrangementService.lockLevel(posParams.getParentNode(), fundVersion);
            // find & lock target level
            ArrLevel targetLevel = null;
            if (posParams.getTargetNode() != null) {
                targetLevel = arrangementService.lockLevel(posParams.getTargetNode(), fundVersion);
            }
            pos = new ImportPosition(fundVersion, parentLevel, targetLevel, posParams.getDirection());
        }

        return new SectionsContext(storageDispatcher, createChange, scope, pos, staticData, initHelper);
    }

    private static XmlElementReader prepareReader(InputStream is, ImportContext context) {
        XmlElementReader reader;
        try {
            reader = XmlElementReader.create(is);
        } catch (XMLStreamException e) {
            throw new SystemException("Failed to prepare import source", e);
        }
        reader.addElementHandler("/edx/aps/ap", new AccessPointElementHandler(context));
        reader.addElementHandler("/edx/pars/per", new PersonElementHandler(context));
        reader.addElementHandler("/edx/pars/famy", new FamilyElementHandler(context));
        reader.addElementHandler("/edx/pars/pg", new PartyGroupElementHandler(context));
        reader.addElementHandler("/edx/pars/evnt", new EventElementHandler(context));
        reader.addElementHandler("/edx/inss/inst", new InstitutionElementHandler(context));
        reader.addElementHandler("/edx/fs/s", new SectionElementHandler(context));
        reader.addElementHandler("/edx/fs/s/fi", new FundInfoElementHandler(context));
        reader.addElementHandler("/edx/fs/s/pcks/pck", new SectionPacketElementHandler(context));
        reader.addElementHandler("/edx/fs/s/lvls/lvl", new SectionLevelElementHandler(context));
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