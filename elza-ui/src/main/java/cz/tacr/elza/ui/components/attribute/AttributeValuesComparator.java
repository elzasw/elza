package cz.tacr.elza.ui.components.attribute;

import java.util.Comparator;

import org.apache.commons.lang.builder.CompareToBuilder;

import cz.tacr.elza.domain.ArrDescItemExt;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.9.2015
 */
public class AttributeValuesComparator implements Comparator<ArrDescItemExt> {

    @Override
    public int compare(final ArrDescItemExt o1, final ArrDescItemExt o2) {

        Integer specOrder1 = (o1.getDescItemSpec() == null) ? null : o1.getDescItemSpec().getViewOrder();
        Integer specOrder2 = (o2.getDescItemSpec() == null) ? null : o2.getDescItemSpec().getViewOrder();
        return new CompareToBuilder()
                .append(o1.getDescItemType().getViewOrder(), o2.getDescItemType().getViewOrder())
                .append(o1.getDescItemType().getName(), o2.getDescItemType().getName())
                .append(specOrder1, specOrder2)
                .append(o1.getPosition(), o2.getPosition())
                .toComparison();
    }
}
