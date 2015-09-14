package cz.tacr.elza.api;

/**
 * Rozšíření {@link ArrDescItem} o hodnotu atributu.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDescItemExt<FC extends ArrFaChange, RT extends RulDescItemType, RS extends RulDescItemSpec, PAP extends ParParty, RR extends RegRecord, N extends ArrNode>
        extends ArrDescItem<FC, RT, RS, N> {

    /**
     * @return hodnotu atributu.
     */
    String getData();

    /**
     * Nastaví hodnotu atributu.
     * @param data hodnota atributu.
     */
    void setData(String data);

    /**
     * @return Odkaz na osobu.
     */
    PAP getAbstractParty();

    /**
     * Nastaví osobu.
     * @param abstractParty osoba
     */
    void setAbstractParty(PAP abstractParty);

    /**
     * 
     * @return záznam v rejstříku.
     */
    RR getRecord();

    /**
     * Nastabí záznam v rejstříku.
     * @param record záznam v rejstříku.
     */
    void setRecord(RR record);
}
