package cz.tacr.elza.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class ArrDescItemExt extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemExt<ArrFaChange, RulDescItemType,RulDescItemSpec> {

    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArrDescItemExt)) {
            return false;
        }
        ArrDescItemExt castOther = (ArrDescItemExt) other;
        if (getId() == null) {
            return false;
        }

        return new EqualsBuilder()
                .append(getId(), castOther.getId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getId())
                .toHashCode();
    }
}
