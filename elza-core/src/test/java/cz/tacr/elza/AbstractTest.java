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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


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
    protected ItemRepository itemRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    protected ItemTypeRepository itemTypeRepository;
    @Autowired
    protected ItemSpecRepository itemSpecRepository;
    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;
    @Autowired
    protected DataTypeRepository dataTypeRepository;
    @Autowired
    private DataRepository arrDataRepository;
    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;
    @Autowired
    private OutputRepository outputRepository;
    @Autowired
    protected RegisterTypeRepository registerTypeRepository;
    @Autowired
    protected PartyRepository partyRepository;
    @Autowired
    private VariantRecordRepository variantRecordRepository;
    @Autowired
    protected RegCoordinatesRepository regCoordinatesRepository;
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

    private RulPackage rulPackage;

    @Before
    public void setUp() throws Exception {

        List<RulPackage> packages = utilsTest.getPackages();
        rulPackage = packages.size() > 0 ? packages.get(0) : null;
        if (rulPackage == null) {
            logger.info("Loading package for tests...");
            File file = buildPackageFileZip();
            utilsTest.importPackage(file);
            file.delete();
            rulPackage = utilsTest.getPackages().get(0);
        }

        deleteTables();
    }

    /**
     * Vytvoří balíček pro import pravidel a hromadných akcí.
     *
     * @return zip soubor
     */
    protected File buildPackageFileZip() throws Exception {
        byte[] buffer = new byte[1024];
        URL url = Thread.currentThread().getContextClassLoader().getResource("zp");
        File tmpFile = File.createTempFile("package-test_", ".zip");
        String sourceDirectory = url.getPath();
        FileOutputStream fout = new FileOutputStream(tmpFile);
        ZipOutputStream zout = new ZipOutputStream(fout);
        File dir = new File(sourceDirectory);
        recurseAdd(buffer, zout, dir, "");
        zout.close();
        return tmpFile;
    }

    /**
     * Rekurzivní přidávání souborů do ZIPu.
     *
     * @param buffer buffer pro kopírování
     * @param zout  výstupní zip stream
     * @param dir   adresář k prohledání
     * @param path  relativní cesta v zip
     */
    private void recurseAdd(final byte[] buffer, final ZipOutputStream zout, final File dir, final String path) throws IOException {
        File[] files = dir.listFiles();
        for(int i=0; i < files.length ; i++)
        {
            if(files[i].isDirectory())
            {
                recurseAdd(buffer, zout, files[i], path + files[i].getName() + "/");
                continue;
            }

            FileInputStream fin = new FileInputStream(files[i]);
            zout.putNextEntry(new ZipEntry(path + files[i].getName()));

            int length;
            while((length = fin.read(buffer)) > 0)
            {
                zout.write(buffer, 0, length);
            }
            zout.closeEntry();
            fin.close();
        }
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
        regCoordinatesRepository.deleteAll();
        recordRepository.deleteAll();
        nodeConformityErrorsRepository.deleteAll();
        nodeConformityMissingRepository.deleteAll();
        nodeConformityInfoRepository.deleteAll();
        fundVersionRepository.deleteAll();
        fundRegisterScopeRepository.deleteAll();
        levelRepository.deleteAll();
        outputRepository.deleteAll();
        outputDefinitionRepository.deleteAll();
        descItemRepository.deleteAll();
        itemRepository.deleteAll();
        itemSpecRegisterRepository.deleteAll();
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
