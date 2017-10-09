package cz.tacr.elza.domain.vo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;


/**
 * Obalovací třída pro více hodnot atributů (kvůli opakovatelným)
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 11.04.2016
 */
public class DescItemValues {

    List<DescItemValue> values = new ArrayList<>();

    public List<DescItemValue> getValues() {
        return values;
    }

    public void setValues(final List<DescItemValue> values) {
        this.values = values;
    }

    public void addValue(final DescItemValue value) {
        Assert.notNull(value, "Hodnota musí být vyplněna");

        values.add(value);
    }
}
