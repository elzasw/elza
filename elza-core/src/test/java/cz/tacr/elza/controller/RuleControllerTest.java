package cz.tacr.elza.controller;

import org.junit.Test;

import java.io.File;
import java.net.URL;


/**
 * @author Petr Compel
 * @since 17.2.2016
 */
public class RuleControllerTest extends AbstractControllerTest {

    private static final String IMPORT_PACKAGE = RULE_CONTROLLER_URL + "/importPackage";
    private static final String DELETE_PACKAGE = RULE_CONTROLLER_URL + "/deletePackage/{code}";
    private static final String EXPORT_PACKAGE = RULE_CONTROLLER_URL + "/exportPackage/{code}";

    @Test
    public void getDataTypesTest() {
        getDataTypes();
    }

    @Test
    public void getDescItemTypesTest() {
        getDescItemTypes();
    }

    @Test
    public void getPackagesTest() {
        getPackages();
    }

    @Test
    public void getRuleSetsTest() {
        getRuleSets();
    }

    @Test
    public void deleteImportExportPackageTest() {
        deletePackage(getPackages().get(0).getCode());
        importPackage();
        exportPackage(getPackages().get(0).getCode());
    }

    private void importPackage() {
        URL url = Thread.currentThread().getContextClassLoader().getResource(PACKAGE_FILE);
        File file = new File(url.getPath());
        multipart(spec -> spec.multiPart("file", file), IMPORT_PACKAGE);
    }

    private void deletePackage(final String code) {
        get(spec -> spec.pathParam("code", code), DELETE_PACKAGE);
    }

    private void exportPackage(final String code) {
        get(spec -> spec.pathParam("code", code), EXPORT_PACKAGE);
    }
}
