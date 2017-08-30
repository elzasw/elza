package cz.tacr.elza;

import cz.tacr.elza.other.HelperTestService;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;



/**
 * Base test class
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes=ElzaCoreTest.class)
@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
public abstract class AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);

    /*
    @Autowired
    protected PackageRepository packageRepository;
    @Autowired
    protected CalendarTypeRepository calendarTypeRepository;
    @Autowired
    protected UnitdateRepository unitdateRepository;
    @Autowired
    protected ScopeRepository scopeRepository;
    */
    @Autowired
    protected HelperTestService helperTestService;

    @Before
    public void setUp() throws Exception {

    	helperTestService.loadPackage("CZ_BASE", "package-cz-base");
    	helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");

        helperTestService.deleteTables();
    }


}
