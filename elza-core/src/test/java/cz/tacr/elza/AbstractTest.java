package cz.tacr.elza;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeRepository;



/**
 * Base test class
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes=ElzaCoreTest.class)
@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
public abstract class AbstractTest {

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
    protected NodeRepository nodeRepository;
    @Autowired
    protected HelperTestService helperTestService;

    @Before
    public void setUp() throws Exception {

    	helperTestService.loadPackage("CZ_BASE", "package-cz-base");
    	helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");

        helperTestService.deleteTables();
    }
}
