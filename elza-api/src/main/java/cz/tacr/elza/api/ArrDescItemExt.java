package cz.tacr.elza.api;

/**
 * Rozšíření {@link ArrDescItem} o hodnotu atributu.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDescItemExt<FC extends ArrFaChange, RT extends RulDescItemType, RS extends RulDescItemSpec, PAP extends ParAbstractParty, RR extends RegRecord>
        extends ArrDescItem<FC, RT, RS> {

    /**
     * @return hodnotu atributu.
     */
    String getData();

    /**
     * Nastaví hodnotu atributu.
     * @param data
     */
    void setData(String data);

    /**
     * @return Odkaz na osobu.
     */
    PAP getAbstractParty();

    /**
     * Nastabí osobu.
     * @param abstractParty
     */
    void setAbstractParty(PAP abstractParty);

    /**
     * 
     * @return záznam v rejstříku.
     */
    RR getRecord();

    /**
     * Nastabí záznam v rejstříku.
     * @param record
     */
    void setRecord(RR record);
}
