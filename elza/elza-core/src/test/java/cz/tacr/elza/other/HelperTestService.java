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

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApKeyValueRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApRevIndexRepository;
import cz.tacr.elza.repository.ApRevItemRepository;
import cz.tacr.elza.repository.ApRevPartRepository;
import cz.tacr.elza.repository.ApRevStateRepository;
import cz.tacr.elza.repository.ApRevisionRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DaoBatchInfoRepository;
import cz.tacr.elza.repository.DaoDigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoLinkRequestRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DaoRequestRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DigitizationRequestRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.GroupRepository;
import cz.tacr.elza.repository.GroupUserRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeExtensionRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.SobjVrequestRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.service.AsyncRequestService;


/**
 * Helper test service
 */
@Service
public class HelperTestService {

    private static final Logger logger = LoggerFactory.getLogger(HelperTestService.class);

    @Autowired
    protected ApRevIndexRepository revIndexRepository;
    @Autowired
    protected ApRevItemRepository revItemRepository;
    @Autowired
    protected ApRevPartRepository revPartRepository;
    @Autowired
    protected ApRevisionRepository revisionRepository;
    @Autowired
    protected ApRevStateRepository revStateRepository;
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
    private ItemAptypeRepository itemAptypeRepository;
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
    private OutputRepository outputRepository;
    @Autowired
    protected ApTypeRepository apTypeRepository;
    @Autowired
    protected InstitutionRepository institutionRepository;
    @Autowired
    protected InstitutionTypeRepository institutionTypeRepository;
    @Autowired
    protected ApAccessPointRepository apRepository;
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
    private SobjVrequestRepository sobjVrequestRepository;
    @Autowired
    private StructuredObjectRepository structureDataRepository;
    @Autowired
    private FundStructureExtensionRepository fundStructureExtensionRepository;
    @Autowired
    private ApBindingRepository bindingRepository;
    @Autowired
    private ApBindingItemRepository bindingItemRepository;
    @Autowired
    private ApBindingStateRepository bindingStateRepository;
    @Autowired
    private AsyncRequestService asyncRequestService;
    @Autowired
    private ApItemRepository apItemRepository;
    @Autowired
    private ApPartRepository partRepository;
    @Autowired
    private ApChangeRepository apChangeRepository;
    @Autowired
    private WfCommentRepository commentRepository;
    @Autowired
    private WfIssueListRepository issueListRepository;
    @Autowired
    private WfIssueRepository issueRepository;
    @Autowired
    private AuthenticationRepository authenticationRepository;
    @Autowired
    private ApStateRepository apStateRepository;
    @Autowired
    protected ArrAsyncRequestRepository asyncRequestRepository;
    @Autowired
    private DaoRepository daoRepository;
    @Autowired
    private DaoLinkRepository daoLinkRepository;
    @Autowired
    private DaoFileRepository daoFileRepository;
    @Autowired
    private DaoPackageRepository daoPackageRepository;
    @Autowired
    private DaoLinkRequestRepository daoLinkRequestRepository;
    @Autowired
    private DaoBatchInfoRepository daoBatchInfoRepository;
    @Autowired
    private DaoRequestRepository daoRequestRepository;
    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;
    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;
    @Autowired
    private DaoDigitizationRequestNodeRepository daoDigitizationRequestNodeRepository;
    @Autowired
    private DigitizationRequestRepository digitizationRequestRepository;
    @Autowired
    private ApIndexRepository indexRepository;
    @Autowired
    private ApKeyValueRepository keyValueRepository;
    @Autowired
    private ApCachedAccessPointRepository apCachedAccessPointRepository;

    @Autowired
    private PackageService packageService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    protected EntityManager em;

    @Transactional
    public void importPackage(final File file) {
        // stop services and prepare update
    	packageService.preImportPackage();

        packageService.importPackageInternal(file, true);
        // refresh static structures
        staticDataService.refreshForCurrentThread();
    }

    public List<RulPackage> getPackages() {
        return packageService.getPackages();
    }

    @Transactional
    public RulPackage getPackage(String packageCode) {
        List<RulPackage> packages = packageService.getPackages();
        for (RulPackage p : packages) {
            if (packageCode.equals(p.getCode())) {
                return p;
            }
        }
        return null;
    }

    @Transactional
    public void deleteTables(boolean stopTasks) {
        if (stopTasks) {
            packageService.stopAsyncTasks();
        }

        deleteTablesInternal();

        if (stopTasks) {
            packageService.startAsyncTasks();
        }
    }

    private void deleteTablesInternal() {

        logger.debug("Cleaning table contents...");

        daoDigitizationRequestNodeRepository.deleteAll();
        digitizationRequestRepository.deleteAll();
        daoRequestDaoRepository.deleteAll();
        daoRequestRepository.deleteAll();
        daoLinkRequestRepository.deleteAll();
        daoBatchInfoRepository.deleteAll();
        daoPackageRepository.deleteAll();
        daoFileGroupRepository.deleteAll();
        daoFileRepository.deleteAll();
        daoLinkRepository.deleteAll();
        daoRepository.deleteAll();

        revIndexRepository.deleteAll();
        revItemRepository.deleteAll();
        revPartRepository.deleteAll();
        revStateRepository.deleteAll();
        revisionRepository.deleteAll();

        apCachedAccessPointRepository.deleteAll();
        asyncRequestRepository.deleteAll();
        commentRepository.deleteAll();
        issueRepository.deleteAll();
        issueListRepository.deleteAll();
        cachedNodeRepository.deleteAll();
        permissionRepository.deleteAll();
        groupUserRepository.deleteAll();
        groupRepository.deleteAll();
        authenticationRepository.deleteAll();
        userRepository.deleteAll();
        nodeConformityErrorsRepository.deleteAll();
        nodeConformityMissingRepository.deleteAll();
        nodeConformityInfoRepository.deleteAll();
        descItemRepository.deleteAll();
        itemRepository.deleteAll();
        dataRepository.deleteAll();
        sobjVrequestRepository.deleteAll();
        structureDataRepository.deleteAll();
        fundStructureExtensionRepository.deleteAll();
        bulkActionNodeRepository.deleteAll();
        faBulkActionRepository.deleteAll();
        indexRepository.deleteAll();
        bindingItemRepository.deleteAll();
        bindingStateRepository.deleteAll();
        apItemRepository.deleteAll();
        partRepository.deleteAll();
        keyValueRepository.deleteAll();
        fundVersionRepository.deleteAll();
        fundRegisterScopeRepository.deleteAll();
        levelRepository.deleteAll();
        nodeOutputRepository.deleteAll();
        outputRepository.deleteAll();
        itemAptypeRepository.deleteAll();
        nodeExtensionRepository.deleteAll();
        changeRepository.deleteAll();
        nodeRepository.deleteAll();
        fundRepository.deleteAll();
        institutionRepository.deleteAll();
        bindingRepository.deleteAll();
        apStateRepository.deleteAll();
        apRepository.deleteAll();
        apChangeRepository.deleteAll();
        externalSystemRepository.deleteAll();

        // DB has to be flushed before start
        em.flush();

        logger.info("All tables cleaned.");
    }

    public FundRepository getFundRepository() {
        return fundRepository;
    }

    public OutputRepository getOutputRepository() {
        return outputRepository;
    }

    // Each package have to be loaded in separate transaction
    // this allows to commit package and reload static data
    @Transactional(value = TxType.REQUIRES_NEW)
    public void loadPackage(String packageCode, String packageDir) {
        RulPackage rulPackage = getPackage(packageCode);
        if (rulPackage == null || rulPackage.getVersion() <= 0) {
            logger.info("Loading package '" + packageCode + "' for tests...");
            File file = null;
            try {
                file = buildPackageFileZip(packageDir);
                Assert.assertNotNull(file);
                importPackage(file);
            } catch (Exception e) {
                logger.info("Exception while importing package: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                if (file != null) {
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
     * @param zout   výstupní zip stream
     * @param dir    adresář k prohledání
     * @param path   relativní cesta v zip
     */
    static private void recurseAdd(final byte[] buffer, final ZipOutputStream zout, final File dir, final String path) throws IOException {
        File[] files = dir.listFiles();
        logger.info("recurseAdd: path: " + path + ", dir: " + dir + ", files: " + files);
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                recurseAdd(buffer, zout, files[i], path + files[i].getName() + "/");
                continue;
            }

            FileInputStream fin = new FileInputStream(files[i]);
            zout.putNextEntry(new ZipEntry(path + files[i].getName()));

            int length;
            while ((length = fin.read(buffer)) > 0) {
                zout.write(buffer, 0, length);
            }
            zout.closeEntry();
            fin.close();
        }
    }

    /**
     * Function will wait for all workers
     */
    // This method is not running in transaction
    public void waitForWorkers() {
        asyncRequestService.waitForFinishAll();
    }

}
