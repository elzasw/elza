package cz.tacr.elza;

import java.io.File;
import java.net.URL;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.StartupService;



/**
 * Base test class
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes=ElzaCoreMain.class)
@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
public abstract class AbstractTest {

    // Import institucí
    protected final static String XML_INSTITUTION = "institution-import.xml";

    @Autowired
    protected ClientFactoryVO clientFactoryVO;
    @Autowired
	protected DataRepository dataRepository;
    @Autowired
    protected DataTypeRepository dataTypeRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    protected ItemTypeRepository itemTypeRepository;
    @Autowired
    protected ItemSpecRepository itemSpecRepository;
    @Autowired
    protected ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;
    @Autowired
    protected NodeRepository nodeRepository;
    @Autowired
    protected HelperTestService helperTestService;
    @Autowired
    protected StartupService startupService;

    @Autowired
    protected ElzaLocale elzaLocale;
    
    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected EntityManager em;

    @Before
    public void setUp() throws Exception {
        // startup service have to be initialized
        Assert.assertTrue(!startupService.isRunning());
        helperTestService.deleteTables(false);

        if (!startupService.isRunning()) {
            startupService.startNow();
        }

    	helperTestService.loadPackage("CZ_BASE", "package-cz-base");
    	// helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        helperTestService.loadPackage("SIMPLE-DEV", "rules-simple-dev");
    }

    @After
    public void tearDown() {
        startupService.stop();
    }

    public static File getResourceFile(String resourcePath) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        Assert.assertNotNull(url);
        return new File(url.getPath());
    }

}
