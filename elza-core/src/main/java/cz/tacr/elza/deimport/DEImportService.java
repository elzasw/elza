package cz.tacr.elza.deimport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.aop.Authorization;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.deimport.DEImportParams.ImportPositionParams;
import cz.tacr.elza.deimport.aps.context.AccessPointsContext;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;
import cz.tacr.elza.deimport.institutions.context.InstitutionsContext;
import cz.tacr.elza.deimport.parties.context.PartiesContext;
import cz.tacr.elza.deimport.reader.XmlElementReader;
import cz.tacr.elza.deimport.reader.handlers.AccessPointElementHandler;
import cz.tacr.elza.deimport.reader.handlers.EventElementHandler;
import cz.tacr.elza.deimport.reader.handlers.FamilyElementHandler;
import cz.tacr.elza.deimport.reader.handlers.FundInfoElementHandler;
import cz.tacr.elza.deimport.reader.handlers.InstitutionElementHandler;
import cz.tacr.elza.deimport.reader.handlers.PartyGroupElementHandler;
import cz.tacr.elza.deimport.reader.handlers.PersonElementHandler;
import cz.tacr.elza.deimport.reader.handlers.SectionElementHandler;
import cz.tacr.elza.deimport.reader.handlers.SectionLevelElementHandler;
import cz.tacr.elza.deimport.reader.handlers.SectionPacketElementHandler;
import cz.tacr.elza.deimport.sections.context.SectionStorageDispatcher;
import cz.tacr.elza.deimport.sections.context.SectionsContext;
import cz.tacr.elza.deimport.sections.context.SectionsContext.ImportPosition;
import cz.tacr.elza.deimport.storage.StorageManager;
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
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.utils.XmlUtils;

@Service
public class DEImportService {

    private final EntityManager em;

    private final RegRecordRepository recordRepository;

    private final ArrangementService arrangementService;

    private final RegCoordinatesRepository coordinatesRepository;

    private final RegVariantRecordRepository variantRecordRepository;

    private final PartyRepository partyRepository;

    private final UnitdateRepository unitdateRepository;

    private final PartyGroupIdentifierRepository groupIdentifierRepository;

    private final PartyNameRepository nameRepository;

    private final PartyNameComplementRepository nameComplementRepository;

    private final InstitutionRepository institutionRepository;

    private final UserService userService;

    private final StaticDataService staticDataService;

    private final NodeCacheService nodeCacheService;

    private final RegExternalSystemRepository externalSystemRepository;

    private final IEventNotificationService eventNotificationService;

    private final InstitutionTypeRepository institutionTypeRepository;

    private final GroovyScriptService groovyScriptService;

    private final ScopeRepository scopeRepository;

    private final FundVersionRepository fundVersionRepository;

    private final LevelRepository levelRepository;

    private final LevelTreeCacheService levelTreeCacheService;

    private final String transformationsDirectory;

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
                           IEventNotificationService eventNotificationService,
                           InstitutionTypeRepository institutionTypeRepository,
                           GroovyScriptService groovyScriptService,
                           ScopeRepository scopeRepository,
                           FundVersionRepository fundVersionRepository,
                           LevelRepository levelRepository,
                           LevelTreeCacheService levelTreeCacheService,
                           @Value("${elza.xmlImport.transformationDir}") String transformationsDirectory) {
        this.em = em;
        this.recordRepository = recordRepository;
        this.arrangementService = arrangementService;
        this.coordinatesRepository = coordinatesRepository;
        this.variantRecordRepository = variantRecordRepository;
        this.partyRepository = partyRepository;
        this.nameRepository = nameRepository;
        this.nameComplementRepository = nameComplementRepository;
        this.groupIdentifierRepository = groupIdentifierRepository;
        this.unitdateRepository = unitdateRepository;
        this.institutionRepository = institutionRepository;
        this.userService = userService;
        this.staticDataService = staticDataService;
        this.nodeCacheService = nodeCacheService;
        this.externalSystemRepository = externalSystemRepository;
        this.eventNotificationService = eventNotificationService;
        this.institutionTypeRepository = institutionTypeRepository;
        this.groovyScriptService = groovyScriptService;
        this.scopeRepository = scopeRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.levelRepository = levelRepository;
        this.levelTreeCacheService = levelTreeCacheService;
        this.transformationsDirectory = transformationsDirectory;
    }

    public List<String> getTransformationNames() {
        return XmlUtils.getTransformationNames(transformationsDirectory);
    }

    public void validateData(InputStream is) {
        URL xsdSchema = getClass().getResource("/schema/elza-schema-v2.xsd");
        if (xsdSchema == null) {
            throw new SystemException("Import xsd schema not found");
        }
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(is);
            Schema schema = schemaFactory.newSchema(xsdSchema);
            Validator validator = schema.newValidator();
            validator.validate(new StAXSource(streamReader), null);
        } catch (SAXException e) {
            throw new DEImportException("Import is not valid", e);
        } catch (XMLStreamException | IOException e) {
            throw new SystemException("Import validation failed", e);
        }
    }

    @Transactional(TxType.REQUIRED)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ADMIN })
    public void importData(InputStream is, DEImportParams params) {

        checkScopePermissions(params.getScopeId());
        checkParameters(params);

        ImportContext context = initContext(params);
        XmlElementReader reader = prepareReader(is, context);
        FlushMode origFlushMode = configureBatchSession(context.getSession(), params.getBatchSize());
        try {
            reader.readDocument();
            context.setCurrentPhase(ImportPhase.FINISHED);
            nodeCacheService.syncCache();
        } catch (XMLStreamException e) {
            throw new SystemException(e);
        } finally {
            restoreSessionConfiguration(context.getSession(), origFlushMode);
        }
    }

    private void checkScopePermissions(int importScopeId) {
        if (!userService.hasPermission(Permission.ADMIN) && !userService.hasPermission(Permission.REG_SCOPE_WR, importScopeId)) {
            throw Authorization.createAccessDeniedException(Permission.REG_SCOPE_WR);
        }
    }

    private void checkParameters(DEImportParams params) {
        Assert.isTrue(params.getBatchSize() > 0, "Import batch size must be greater than 0");
        Assert.isTrue(params.getMemoryScoreLimit() > 0, "Import memory score limit must be greater than 0");
    }

    private FlushMode configureBatchSession(Session session, int batchSize) {
        FlushMode origFlushMode = session.getHibernateFlushMode();
        session.setHibernateFlushMode(FlushMode.COMMIT);
        session.setJdbcBatchSize(batchSize);
        return origFlushMode;
    }

    private void restoreSessionConfiguration(Session session, FlushMode origFlushMode) {
        session.setHibernateFlushMode(origFlushMode);
        session.setJdbcBatchSize(null);
    }

    private ImportContext initContext(DEImportParams params) {
        // unwrap current session
        Session session = em.unwrap(Session.class);

        // init storage manager
        StorageManager storageManager = new StorageManager(session, params.getMemoryScoreLimit(), recordRepository,
                arrangementService, coordinatesRepository, variantRecordRepository, partyRepository, nameRepository,
                nameComplementRepository, groupIdentifierRepository, unitdateRepository);

        // find import scope
        RegScope importScope = scopeRepository.findOne(params.getScopeId());
        if (importScope == null) {
            throw new SystemException("Import scope not found, id:" + params.getScopeId());
        }

        // get static data for current transaction
        StaticDataProvider staticData = staticDataService.getData();

        // initialize contexts
        AccessPointsContext accessPointsContext = new AccessPointsContext(storageManager, params.getBatchSize(), importScope,
                externalSystemRepository);
        PartiesContext partiesContext = new PartiesContext(storageManager, params.getBatchSize(), accessPointsContext,
                institutionRepository, institutionTypeRepository, groovyScriptService);
        InstitutionsContext institutionsContext = new InstitutionsContext(storageManager, params.getBatchSize(),
                institutionRepository, institutionTypeRepository);
        SectionsContext sectionsContext = createSectionsContext(storageManager, params, importScope, staticData);
        return new ImportContext(session, accessPointsContext, partiesContext, institutionsContext, sectionsContext, staticData);
    }

    private XmlElementReader prepareReader(InputStream is, ImportContext context) {
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
        reader.addElementHandler("/edx/inst/inst", new InstitutionElementHandler(context));
        reader.addElementHandler("/edx/fs/s", new SectionElementHandler(context));
        reader.addElementHandler("/edx/fs/s/fi", new FundInfoElementHandler(context));
        reader.addElementHandler("/edx/fs/s/pcks/pck", new SectionPacketElementHandler(context));
        reader.addElementHandler("/edx/fs/s/lvls/lvl", new SectionLevelElementHandler(context));
        return reader;
    }

    private SectionsContext createSectionsContext(StorageManager storageManager,
                                                  DEImportParams importParams,
                                                  RegScope importScope,
                                                  StaticDataProvider staticData) {
        // create global import change
        ArrChange createChange = arrangementService.createChange(Type.IMPORT);

        // init storage dispatcher
        SectionStorageDispatcher storageDispatcher = new SectionStorageDispatcher(storageManager, importParams.getBatchSize());

        // prepare import position
        ImportPositionParams posParams = importParams.getPositionParams();
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
        return new SectionsContext(storageDispatcher, createChange, importScope, pos, staticData, arrangementService,
                institutionRepository, eventNotificationService, levelRepository, levelTreeCacheService);
    }
}
