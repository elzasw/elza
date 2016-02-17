package cz.tacr.elza.controller;

import org.junit.Test;
import org.springframework.util.Assert;


/**
 * @author Martin Šlapa
 * @since 16.2.2016
 */
public class ValidationControllerTest extends AbstractControllerTest {

    @Test
    public void getFindingAidVersionsTest() {
        this.valid(new String[]{
                "20.st.",
                "1968",
                "1968/8",
                "21.8.1698",
                "21.8.1968 8:00",
                "1968",
                "21.8.1968 0:00-27.6.1989",
                ///"21.8.1968-", TODO Odkomentovat šlapa po opravě polointervalu
                "(16.8.1977)",
        });

        this.invalid(new String[]{
                "..",
                "..dwklfhewiofle",
        });
    }

    private void valid(String[] list) {
        for (String val : list) {
            Assert.isTrue(validateUnitDate(val).isValid());
        }
    }

    private void invalid(String[] list) {
        for (String val : list) {
            Assert.isTrue(!validateUnitDate(val).isValid());
        }
    }

}
