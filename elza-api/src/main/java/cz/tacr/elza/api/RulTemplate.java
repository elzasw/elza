package cz.tacr.elza.api;

/**
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public interface RulTemplate<P extends RulPackage, O extends RulOutputType> {

    /**
     * Výčet template enginů
     */
    enum Engine {
        JASPER,
        FREEMARKER,
        DOCX
    }

    Integer getTemplateId();

    void setTemplateId(final Integer templateId);

    String getCode();

    void setCode(final String code);

    String getName();

    void setName(final String name);

    Engine getEngine();

    void setEngine(Engine engine);

    String getDirectory();

    void setDirectory(String directory);

    /**
     * @return balíček
     */
    P getPackage();

    /**
     * @param rulPackage balíček
     */
    void setPackage(P rulPackage);

    /**
     * @return typ outputu
     */
    O getOutputType();

    /**
     * @param outputType typ outputu
     */
    void setOutputType(O outputType);

    String getMimeType();

    void setMimeType(String mimeType);

    String getExtension();

    void setExtension(String extension);

    Boolean getDeleted();

    void setDeleted(Boolean deleted);
}
