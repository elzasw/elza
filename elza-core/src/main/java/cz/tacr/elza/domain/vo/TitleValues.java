package cz.tacr.elza.domain.vo;

import java.util.Comparator;
import java.util.TreeSet;


/**
 * Obalovací třída pro více hodnot atributů (kvůli opakovatelným)
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 08.04.2016
 */
public class TitleValues {

    private static final Comparator<TitleValue> TITLE_VALUE_COMPARATOR = Comparator.comparing(TitleValue::getPosition);


    private TreeSet<TitleValue> values = new TreeSet<>(TITLE_VALUE_COMPARATOR);


    public TreeSet<TitleValue> getValues() {
        return values;
    }

    public void setValues(final TreeSet<TitleValue> values) {
        this.values = values;
    }

    public void addValue(final TitleValue value) {
        values.add(value);
    }
}
