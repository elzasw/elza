package cz.tacr.elza.controller;

import org.junit.Test;

import java.io.File;
import java.net.URL;


/**
 * @author Petr Compel
 * @since 17.2.2016
 */
public class RuleControllerTest extends AbstractControllerTest {

    @Test
    public void getDataTypesTest() {
        getDataTypes();
    }

    /*@Test
    public void getDescItemTypesTest() {
        getDescItemTypes();
    }*/

    @Test
    public void getPackagesTest() {
        getPackages();
    }

    @Test
    public void deleteImportExportPackageTest() {

    }

    public void packageTest() {

    }

    public void importPackageRestTest() {
        URL url = Thread.currentThread().getContextClassLoader().getResource(PACKAGE_FILE);
        File file = new File(url.getPath());
    }

    public void deletePackageTest() {

    }

}
