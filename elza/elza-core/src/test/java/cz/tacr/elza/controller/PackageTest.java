package cz.tacr.elza.controller;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import cz.tacr.elza.controller.vo.PackageVO;
import cz.tacr.elza.other.HelperTestService;


/**
 * @since 22.11.2016
 */
public class PackageTest extends AbstractControllerTest {

    private static final String IMPORT_PACKAGE = RULE_CONTROLLER_URL + "/importPackage";
    private static final String DELETE_PACKAGE = RULE_CONTROLLER_URL + "/deletePackage/{code}";
    private static final String EXPORT_PACKAGE = RULE_CONTROLLER_URL + "/exportPackage/{code}";

    @Override
    @Before
    public void setUp() throws Exception {
        loadInstitutions = false;
        super.setUp();
    }

    @Test
    public void deleteImportExportPackageTest() throws Exception {
        List<PackageVO> packages = getPackages();
        PackageVO packageItem = null;
        for (PackageVO item : packages) {
            if (item.getCode().equals("SIMPLE-DEV")) {
                packageItem = item;
                break;
            }
        }
        Assert.assertNotNull(packageItem);

        deletePackage(packageItem.getCode());
        importPackage();
        exportPackage(packageItem.getCode());
    }

    private void importPackage() throws Exception {
        File file = HelperTestService.buildPackageFileZip("rules-simple-dev");

        multipart(spec -> spec.multiPart("file", file), IMPORT_PACKAGE);
        file.delete();
    }

    private void deletePackage(final String code) {
        get(spec -> spec.pathParam("code", code), DELETE_PACKAGE);
    }

    private void exportPackage(final String code) {
        get(spec -> spec.pathParam("code", code), EXPORT_PACKAGE);
    }
}
