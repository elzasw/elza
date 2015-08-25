package cz.tacr.elza.domain;

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
}
