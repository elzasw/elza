package cz.tacr.elza.api;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDescItemExt<FC extends ArrFaChange, RT extends RulDescItemType, RS extends RulDescItemSpec, PAP extends ParAbstractParty, RR extends RegRecord>
        extends ArrDescItem<FC, RT, RS> {

    String getData();

    void setData(String data);

    PAP getAbstractParty();

    void setAbstractParty(PAP abstractParty);

    RR getRecord();

    void setRecord(RR record);
}
