package cz.tacr.elza.ui.components.attribute;

import cz.tacr.elza.domain.ArrNodeRegister;
import org.apache.commons.lang.builder.CompareToBuilder;

import java.util.Comparator;


/**
 * Pořadí propojení uzlů na hesla rejstříku.
 */
public class NodeRegisterLinkValuesComparator implements Comparator<ArrNodeRegister> {

    @Override
    public int compare(final ArrNodeRegister o1, final ArrNodeRegister o2) {

        return new CompareToBuilder()
                .append(o1.getRecord().getRecord(), o2.getRecord().getRecord())
                .toComparison();
    }
}
