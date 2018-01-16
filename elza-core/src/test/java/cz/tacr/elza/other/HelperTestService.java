package cz.tacr.elza.other;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.NodeExtensionRepository;
import cz.tacr.elza.repository.StructureDataRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionRepository;
import cz.tacr.elza.repository.StructureTypeRepository;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.GroupUserRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRelationRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegVariantRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.UserRepository;


/**
 * Helper test service
 */
@Service
public class HelperTestService {

	private static final Logger logger = LoggerFactory.getLogger(HelperTestService.class);

    @Autowired
    protected BulkActionNodeRepository bulkActionNodeRepository;
    @Autowired
    private CachedNodeRepository cachedNodeRepository;
    @Autowired
    protected ChangeRepository changeRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    protected DataTypeRepository dataTypeRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    protected FundRepository fundRepository;
    @Autowired
    protected FundRegisterScopeRepository fundRegisterScopeRepository;
    @Autowired
    protected FundVersionRepository fundVersionRepository;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected ItemSpecRepository itemSpecRepository;
    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;
    @Autowired
    protected ItemTypeRepository itemTypeRepository;
    @Autowired
    protected LevelRepository levelRepository;
    @Autowired
    protected NodeConformityRepository nodeConformityInfoRepository;
    @Autowired
    protected NodeConformityErrorRepository nodeConformityErrorsRepository;
    @Autowired
    protected NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    protected NodeRepository nodeRepository;
    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;
    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;
    @Autowired
    private OutputRepository outputRepository;
    @Autowired
    private RegVariantRecordRepository variantRecordRepository;
    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;
    @Autowired
    protected RegisterTypeRepository registerTypeRepository;
    @Autowired
    protected PartyRepository partyRepository;
    @Autowired
    protected PartyTypeRelationRepository partyTypeRelationRepository;
    @Autowired
    protected PartyCreatorRepository partyCreatorRepository;
    @Autowired
    protected PartyGroupIdentifierRepository partyGroupIdentifierRepository;
    @Autowired
    protected InstitutionRepository institutionRepository;
    @Autowired
    protected InstitutionTypeRepository institutionTypeRepository;
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
    protected RegCoordinatesRepository regCoordinatesRepository;
    @Autowired
    protected RegRecordRepository recordRepository;
    @Autowired
    protected PartyNameRepository partyNameRepository;
    @Autowired
    private BulkActionRunRepository faBulkActionRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PermissionRepository permissionRepository;
    @Autowired
    protected GroupUserRepository groupUserRepository;
    @Autowired
    protected GroupRepository groupRepository;
    @Autowired
    protected ExternalSystemRepository externalSystemRepository;
    @Autowired
    private NodeOutputRepository nodeOutputRepository;
    @Autowired
    private NodeExtensionRepository nodeExtensionRepository;
    @Autowired
    private StructureDataRepository structureDataRepository;
    @Autowired
    private FundStructureExtensionRepository fundStructureExtensionRepository;

    @Autowired
    private PackageService packageService;

    @Autowired
    private StaticDataService staticDataService;

    @Transactional
    public void importPackage(final File file) {
        packageService.importPackage(file);
        // refresh static structures
        staticDataService.refreshForCurrentThread();
    }

    public List<RulPackage> getPackages() {
        return packageService.getPackages();
    }

    @Transactional
	public RulPackage getPackage(String packageCode) {
    	List<RulPackage> packages = packageService.getPackages();
    	for(RulPackage p: packages) {
    		if(packageCode.equals(p.getCode())) {
    			return p;
    		}
    	}
		return null;
	}

    @Transactional
    public void deleteTables() {
    	logger.info("Cleaning table contents...");

        cachedNodeRepository.deleteAll();
        permissionRepository.deleteAll();
        groupUserRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
        nodeConformityErrorsRepository.deleteAll();
        nodeConformityMissingRepository.deleteAll();
        nodeConformityInfoRepository.deleteAll();
        descItemRepository.deleteAll();
        itemRepository.deleteAll();
        dataRepository.deleteAll();
        structureDataRepository.deleteAll();
        fundStructureExtensionRepository.deleteAll();
        bulkActionNodeRepository.deleteAll();
        faBulkActionRepository.deleteAll();
        partyNameComplementRepository.deleteAll();
        partyRepository.unsetAllPreferredName();
        relationEntityRepository.deleteAll();
        relationRepository.deleteAll();
        partyGroupIdentifierRepository.deleteAll();
        partyCreatorRepository.deleteAll();
        partyNameRepository.deleteAll();
        variantRecordRepository.deleteAll();
        nodeRegisterRepository.deleteAll();
        regCoordinatesRepository.deleteAll();
        fundVersionRepository.deleteAll();
        fundRegisterScopeRepository.deleteAll();
        levelRepository.deleteAll();
        nodeOutputRepository.deleteAll();
        outputRepository.deleteAll();
        outputDefinitionRepository.deleteAll();
        itemSpecRegisterRepository.deleteAll();
        nodeExtensionRepository.deleteAll();
        changeRepository.deleteAll();
        nodeRepository.deleteAll();
        fundRepository.deleteAll();
        institutionRepository.deleteAll();
        partyRepository.deleteAll();
        recordRepository.deleteAll();
        externalSystemRepository.deleteAll();

        logger.info("All tables cleaned.");
    }

	public FundRepository getFundRepository() {
		return fundRepository;
	}

    // Each package have to be loaded in separate transaction
    // this allows to commit package and reload static data
    @Transactional(value = TxType.REQUIRES_NEW)
	public void loadPackage(String packageCode, String packageDir) {
    	RulPackage rulPackage = getPackage(packageCode);
        if (rulPackage == null || rulPackage.getVersion()<=0 ) {
            logger.info("Loading package '"+packageCode+"' for tests...");
            File file = null;
            try {
                file = buildPackageFileZip(packageDir);
                Assert.assertNotNull(file);
            	importPackage(file);
            } catch(Exception e) {
            	logger.info("Exception while importing package: " + e.getMessage());
            	e.printStackTrace();
            	throw new RuntimeException(e);
            }
            finally {
            	if(file!=null) {
                    file.delete();
                }
            }

            rulPackage = getPackage(packageCode);
            Assert.assertNotNull(rulPackage);
            logger.info("Package loaded.");
        }

	}

    /**
     * Vytvoří balíček pro import pravidel a hromadných akcí.
     *
     * @return zip soubor
     */
    static public File buildPackageFileZip(String resourceDir) throws Exception {
        byte[] buffer = new byte[1024];
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceDir);
        File tmpFile = File.createTempFile("package-test_", ".zip");
        String sourceDirectory = URLDecoder.decode(url.getPath(), "UTF-8");
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
    static private void recurseAdd(final byte[] buffer, final ZipOutputStream zout, final File dir, final String path) throws IOException {
        File[] files = dir.listFiles();
        logger.info("recurseAdd: path: " + path + ", dir: " + dir + ", files: " + files);
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

}
