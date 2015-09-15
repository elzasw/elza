package cz.tacr.elza.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * Rozšíření {@link ArrDescItem} o hodnotu atributu.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class ArrDescItemExt extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemExt<ArrFaChange, RulDescItemType,RulDescItemSpec, ParParty, RegRecord, ArrNode> {

    private String attData;
    private ParParty party;
    private RegRecord record;

    @Override
    public String getData() {
        return attData;
    }

    @Override
    public void setData(String data) {
        this.attData = data;
    }

    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(ParParty party) {
        this.party = party;
        this.attData = this.party == null ? null : this.party.getRecord().getRecord();
    }

    @Override
    public RegRecord getRecord() {
        return record;
    }

    @Override
    public void setRecord(RegRecord record) {

        this.record = record;
        this.attData = record == null ? null : record.getRecord();
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
        if (getDescItemId() == null) {
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
