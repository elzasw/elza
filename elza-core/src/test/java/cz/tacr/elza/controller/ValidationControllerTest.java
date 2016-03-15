package cz.tacr.elza.controller;

import org.junit.Test;
import org.springframework.util.Assert;


/**
 * Testování metod z ValidationControllerTest.
 *
 * @author Martin Šlapa
 * @author Petr Compel
 * @since 17.2.2016
 */
public class ValidationControllerTest extends AbstractControllerTest {

    @Test
    public void getFundVersionsTest() {
        this.valid(new String[]{
                "20.st.",
                "1968",
                "1968/8",
                "21.8.1698",
                "21.8.1968 8:00",
                "1968",
                "21.8.1968 0:00-27.6.1989",
                "21.8.1968-",
                "-21.8.1968",
                "(16.8.1977)",
        });

        this.invalid(new String[]{
                "19680",
                "..",
                "..dwklfhewiofle",
        });
    }

    /**
     * Kontrola zda jsou zadané unit date platné
     *
     * @param list Stringy unit date
     */
    private void valid(String[] list) {
        for (String val : list) {
            Assert.isTrue(validateUnitDate(val).isValid());
        }
    }

    /**
     * Kontrola zda jsou zadané unit date neplatené
     *
     * @param list Stringy unit date
     */
    private void invalid(String[] list) {
        for (String val : list) {
            Assert.isTrue(!validateUnitDate(val).isValid());
        }
    }

}
