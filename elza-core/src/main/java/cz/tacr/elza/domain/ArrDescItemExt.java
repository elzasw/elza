package cz.tacr.elza.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class ArrDescItemExt extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemExt<ArrFaChange, RulDescItemType,RulDescItemSpec, ParAbstractParty, RegRecord, ArrNode> {

    private String data;
    private ParAbstractParty abstractParty;
    private RegRecord record;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ParAbstractParty getAbstractParty() {
        return abstractParty;
    }

    public void setAbstractParty(ParAbstractParty abstractParty) {
        this.abstractParty = abstractParty;
        this.data = abstractParty == null ? null : abstractParty.getRecord().getRecord();
    }

    public RegRecord getRecord() {
        return record;
    }

    public void setRecord(RegRecord record) {

        this.record = record;
        this.data = record == null ? null : record.getRecord();
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
        if (getDescItemId()== null) {
            return false;
        }

        return new EqualsBuilder()
                .append(getDescItemId(), castOther.getDescItemId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getDescItemId())
                .toHashCode();
    }
}
