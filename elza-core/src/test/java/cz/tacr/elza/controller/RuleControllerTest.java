package cz.tacr.elza.controller;

import org.junit.Test;


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

    /*public void packageTest() {

    }

    @Test
    public void importPackageRestTest() {
        Assert.isInstanceOf(List.class, getPackages());
    }

    @Test
    public void exportPackageRestTest() {
        Assert.isInstanceOf(List.class, getPackages());
    }

    @Test
    public void deletePackageTest() {
        Assert.isInstanceOf(List.class, getPackages());
    }*/

}
