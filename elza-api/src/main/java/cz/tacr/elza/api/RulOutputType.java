package cz.tacr.elza.api;

/**
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public interface RulOutputType<P extends RulPackage> {

    Integer getOutputTypeId();


    void setOutputTypeId(final Integer dataTypeId);


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
}
