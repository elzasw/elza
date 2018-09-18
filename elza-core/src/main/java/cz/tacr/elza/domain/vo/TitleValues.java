package cz.tacr.elza.domain.vo;

import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Obalovací třída pro více hodnot atributů (kvůli opakovatelným).
 */
public class TitleValues {

    private static final Comparator<TitleValue> TITLE_VALUE_COMPARATOR = Comparator.comparing(TitleValue::getPosition);

    private final TreeSet<TitleValue> values = new TreeSet<>(TITLE_VALUE_COMPARATOR);

    public TreeSet<TitleValue> getValues() {
        return values;
    }

    public void addValue(final TitleValue value) {
        values.add(value);
    }
    
    public DescItemValues toDescItemValues() {
        DescItemValues div = new DescItemValues();
        div.setValues(Collections.unmodifiableCollection(values));
        return div;
    }
}
