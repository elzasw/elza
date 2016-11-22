package cz.tacr.elza.controller;

import cz.tacr.elza.domain.RulPackage;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;


/**
 * @author Martin Šlapa
 * @since 22.11.2016
 */
public class PackageTest extends AbstractControllerTest {

    private static final String IMPORT_PACKAGE = RULE_CONTROLLER_URL + "/importPackage";
    private static final String DELETE_PACKAGE = RULE_CONTROLLER_URL + "/deletePackage/{code}";
    private static final String EXPORT_PACKAGE = RULE_CONTROLLER_URL + "/exportPackage/{code}";

    @Before
    public void setUp() throws Exception {
        loadInstitutions = false;
        super.setUp();
        loadInstitutions = true;
    }

    @Test
    public void deleteImportExportPackageTest() throws Exception {
        List<RulPackage> packages = getPackages();
        RulPackage packageItem = null;
        for (RulPackage item : packages) {
            if (!item.getCode().equals("CZ_BASE")) {
                packageItem = item;
                break;
            }
        }

        deletePackage(packageItem.getCode());
        importPackage();
        exportPackage(packageItem.getCode());
    }

    private void importPackage() throws Exception {
        File file = buildPackageFileZip();
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
