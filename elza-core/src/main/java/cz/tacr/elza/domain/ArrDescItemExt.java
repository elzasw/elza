package cz.tacr.elza.domain;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class ArrDescItemExt extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemExt<ArrFaChange, RulDescItemType,RulDescItemSpec, ParAbstractParty, RegRecord> {

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
    }

    public RegRecord getRecord() {
        return record;
    }

    public void setRecord(RegRecord record) {
        this.record = record;
    }

    /*@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArrDescItemExt)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        TODO: dopsat správně porovnání pro ChildComponentContainer

        return true;
    }*/
}
