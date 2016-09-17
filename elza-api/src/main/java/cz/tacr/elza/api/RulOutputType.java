package cz.tacr.elza.api;

/**
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public interface RulOutputType<P extends RulPackage, R extends RulRule> {

    Integer getOutputTypeId();


    void setOutputTypeId(final Integer outputTypeId);


    String getCode();


    void setCode(final String code);


    String getName();


    void setName(final String name);


    /**
     * @return balíček
     */
    P getPackage();


    /**
     * @param rulPackage balíček
     */
    void setPackage(P rulPackage);


    /**
     * @return pravidla (drl soubor)
     */
    R getRule();


    /**
     * @param rule pravidla (drl soubor)
     */
    void setRule(R rule);
}
