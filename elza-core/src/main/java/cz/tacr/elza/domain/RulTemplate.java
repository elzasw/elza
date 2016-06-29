package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;


/**
 * Implementace třídy {@link cz.tacr.elza.api.RulTemplate}
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 16.6.2016
 */
@Entity(name = "rul_template")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulTemplate implements cz.tacr.elza.api.RulTemplate<RulPackage, RulOutputType> {

    @Id
    @GeneratedValue
    private Integer templateId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Engine engine;

    @Column(length = 250, nullable = false)
    private String directory;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulOutputType.class)
    @JoinColumn(name = "outputTypeId", nullable = false)
    private RulOutputType outputType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    /* Konstanty pro vazby a fieldy. */
    public static final String NAME = "name";

    @Override
    public Integer getTemplateId() {
        return templateId;
    }

    @Override
    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Engine getEngine() {
        return engine;
    }

    @Override
    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    @Override
    public String getDirectory() {
        return directory;
    }

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public RulOutputType getOutputType() {
        return outputType;
    }

    @Override
    public void setOutputType(RulOutputType outputType) {
        this.outputType = outputType;
    }
}
