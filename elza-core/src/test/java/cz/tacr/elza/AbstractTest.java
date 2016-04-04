package cz.tacr.elza;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.other.UtilsTest;
import cz.tacr.elza.repository.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.net.URL;
import java.util.List;


/**
 * @author Martin Šlapa
 * @since 16.2.2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaCoreTest.class)
@IntegrationTest("server.port:0") // zvoli volny port, lze spustit i s aktivni Elzou
@WebAppConfiguration
public abstract class AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);

    @Autowired
    protected ChangeRepository changeRepository;
    @Autowired
    protected LevelRepository levelRepository;
    @Autowired
    protected FundRepository fundRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    protected DescItemTypeRepository descItemTypeRepository;
    @Autowired
    protected DescItemSpecRepository descItemSpecRepository;
    @Autowired
    private DescItemSpecRegisterRepository descItemSpecRegisterRepository;
    @Autowired
    protected DataTypeRepository dataTypeRepository;
    @Autowired
    private DataRepository arrDataRepository;
    @Autowired
    protected RegisterTypeRepository registerTypeRepository;
    @Autowired
    protected PartyRepository partyRepository;
    @Autowired
    private VariantRecordRepository variantRecordRepository;
    @Autowired
    protected RegRecordRepository recordRepository;
    @Autowired
    protected NodeRepository nodeRepository;
    @Autowired
    protected PartyNameRepository partyNameRepository;
    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;
    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;
    @Autowired
    private PacketRepository packetRepository;
    @Autowired
    private BulkActionRunRepository faBulkActionRepository;
    @Autowired
    protected NodeConformityRepository nodeConformityInfoRepository;
    @Autowired
    protected NodeConformityErrorRepository nodeConformityErrorsRepository;
    @Autowired
    protected NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    protected PackageRepository packageRepository;
    @Autowired
    protected PartyTypeRelationRepository partyTypeRelationRepository;
    @Autowired
    protected RelationRepository relationRepository;
    @Autowired
    protected RelationEntityRepository relationEntityRepository;
    @Autowired
    protected RelationTypeRepository relationTypeRepository;
    @Autowired
    protected RelationRoleTypeRepository relationRoleTypeRepository;
    @Autowired
    protected RelationTypeRoleTypeRepository relationTypeRoleTypeRepository;
    @Autowired
    protected PartyTypeComplementTypeRepository partyTypeComplementTypeRepository;
    @Autowired
    protected ComplementTypeRepository complementTypeRepository;
    @Autowired
    protected PartyNameFormTypeRepository partyNameFormTypeRepository;
    @Autowired
    protected CalendarTypeRepository calendarTypeRepository;
    @Autowired
    protected UnitdateRepository unitdateRepository;
    @Autowired
    protected ScopeRepository scopeRepository;
    @Autowired
    protected FundRegisterScopeRepository fundRegisterScopeRepository;
    @Autowired
    protected FundVersionRepository fundVersionRepository;
    @Autowired
    protected PartyCreatorRepository partyCreatorRepository;
    @Autowired
    protected PartyGroupIdentifierRepository partyGroupIdentifierRepository;
    @Autowired
    protected InstitutionRepository institutionRepository;
    @Autowired
    protected InstitutionTypeRepository institutionTypeRepository;
    @Autowired
    protected DataRepository dataRepository;
    @Autowired
    protected ClientFactoryVO clientFactoryVO;
    @Autowired
    protected BulkActionNodeRepository bulkActionNodeRepository;

    @Autowired
    private UtilsTest utilsTest;

    public final static String PACKAGE_FILE = "package-test.zip";

    private RulPackage rulPackage;

    @Before
    public void setUp() {

        List<RulPackage> packages = utilsTest.getPackages();
        rulPackage = packages.size() > 0 ? packages.get(0) : null;
        if (rulPackage == null) {
            logger.info("Loading package for tests...");
            URL url = Thread.currentThread().getContextClassLoader().getResource(PACKAGE_FILE);
            File file = new File(url.getPath());
            utilsTest.importPackage(file);
            rulPackage = utilsTest.getPackages().get(0);
        }

        deleteTables();
    }

    protected void deleteTables() {
        // TODO: dopsat vsechny potrebne tabulky
        arrDataRepository.deleteAll();
        bulkActionNodeRepository.deleteAll();
        faBulkActionRepository.deleteAll();
        packetRepository.deleteAll();
        partyNameComplementRepository.deleteAll();
        partyRepository.unsetAllPreferredName();
        relationEntityRepository.deleteAll();
        relationRepository.deleteAll();
        partyGroupIdentifierRepository.deleteAll();
        partyCreatorRepository.deleteAll();
        partyNameRepository.deleteAll();
        partyRepository.deleteAll();
        variantRecordRepository.deleteAll();
        nodeRegisterRepository.deleteAll();
        recordRepository.deleteAll();
        nodeConformityErrorsRepository.deleteAll();
        nodeConformityMissingRepository.deleteAll();
        nodeConformityInfoRepository.deleteAll();
        fundVersionRepository.deleteAll();
        fundRegisterScopeRepository.deleteAll();
        levelRepository.deleteAll();
        descItemRepository.deleteAll();
        descItemSpecRegisterRepository.deleteAll();
        nodeRepository.deleteAll();
        changeRepository.deleteAll();
        fundRepository.deleteAll();
        institutionRepository.deleteAll();
    }

    /**
     * Vrací balík se kterým testy běží.
     *
     * @return balík
     */
    public RulPackage getPackage() {
        return rulPackage;
    }

}
