package cz.tacr.elza;

import java.io.File;
import java.net.URL;

import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.DmsService;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ExportRepository;
import cz.tacr.elza.repository.ExportTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InhibitedItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;

/**
 * Base test class
 */
@ContextConfiguration(classes=ElzaCoreMain.class)
@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
public abstract class AbstractTest {

    // import institucí
    protected final static String XML_INSTITUTION = "institution-import.xml";

    // import fund
    protected final static String XML_FUND = "fund-filter-nodes.xml";

    // item type code for title
    protected final static String SRD_TITLE = "SRD_TITLE";

    // item type code serial number
    protected final static String SRD_SERIAL_NUMBER = "SRD_SERIAL_NUMBER";

    // item type code for unit-date
    protected final static String SRD_UNIT_DATE = "SRD_UNIT_DATE";

    // item type code for other-id - Jiná označení
    protected final static String SRD_OTHER_ID = "SRD_OTHER_ID";

    // item spec code for other-id
    protected final static String SRD_OTHERID_CJ = "SRD_OTHERID_CJ";

    // item type for enum 
    protected final static String SRD_LANGUAGE = "SRD_LANGUAGE";

    // item spec for SRD_LANGUAGE enum
    protected final static String SRD_LANGUAGE_1 = "SRD_LANGUAGE_1";

    // item type for record_ref
    protected final static String SRD_ENTITY_ROLE = "SRD_ENTITY_ROLE";

    // item spec for record_ref
    protected final static String SRD_ENTITY_ROLE_1 = "SRD_ENTITY_ROLE_1";

    @Autowired
    protected ClientFactoryVO clientFactoryVO;
    @Autowired
	protected DataRepository dataRepository;
    @Autowired
    protected DataTypeRepository dataTypeRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    protected CachedNodeRepository cachedNodeRepository;
    @Autowired
    protected InhibitedItemRepository inhibitedItemRepository;
    @Autowired
    protected ItemTypeRepository itemTypeRepository;
    @Autowired
    protected ItemSpecRepository itemSpecRepository;
    @Autowired
    protected ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;
    @Autowired
    protected FundVersionRepository fundVersionRepository;
    @Autowired
    protected NodeRepository nodeRepository;
    @Autowired
    protected HelperTestService helperTestService;
    @Autowired
    protected StartupService startupService;
    @Autowired
    protected DescriptionItemService descItemService;
    @Autowired
    protected LevelRepository levelRepository;
	@Autowired
	protected AsyncRequestService asyncRequestService;
    @Autowired
    protected ExportRepository exportRepository;
    @Autowired
    protected ExportTypeRepository exportTypeRepository;
	@Autowired
	protected UserService userService;
	@Autowired
	protected DmsService dmsService;

	@Autowired
	protected ResourcePathResolver resourcePathResolver;
	
    @Autowired
    protected ElzaLocale elzaLocale;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected EntityManager em;
    
    @Autowired
    protected PlatformTransactionManager tm;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @BeforeEach
    public void setUp() throws Exception {
        // startup service have to be initialized
        Assertions.assertFalse(startupService.isRunning());
        helperTestService.deleteTables(false);

        startupService.startNow();

    	helperTestService.loadPackage("CZ_BASE", "package-cz-base");
    	// helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        helperTestService.loadPackage("SIMPLE-DEV", "rules-simple-dev");
    }

    @AfterEach
    public void tearDown() {
        startupService.stop();
    }

    public static File getResourceFile(String resourcePath) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        Assertions.assertNotNull(url);
        return new File(url.getPath());
    }
}
