package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.List;

import cz.tacr.elza.domain.ArrOutput.OutputState;

/**
 * VO výstup z archivního souboru.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.04.2016
 */
public class ArrOutputVO {

    // --- fields ---

    private Integer id;

    private String internalCode;

    private String name;

    /**
     * Stav outputu
     */
    private OutputState state;

    private String error;

    private List<TreeNodeVO> nodes;

    private Integer outputTypeId;

    private Integer templateId;

    private Integer outputResultId;

    private Date generatedDate;

    private Integer version;

    private String outputSettings;

    /**
     * Změna vytvoření verze.
     */
    private Date createDate;

    /**
     * Změna smazání verze.
     */
    private Date deleteDate;

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public OutputState getState() {
        return state;
    }

    public void setState(final OutputState state) {
        this.state = state;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public List<TreeNodeVO> getNodes() {
        return nodes;
    }

    public void setNodes(final List<TreeNodeVO> nodes) {
        this.nodes = nodes;
    }

    public Integer getOutputTypeId() {
        return outputTypeId;
    }

    public void setOutputTypeId(final Integer outputTypeId) {
        this.outputTypeId = outputTypeId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Integer templateId) {
        this.templateId = templateId;
    }

    public Integer getOutputResultId() {
        return outputResultId;
    }

    public void setOutputResultId(final Integer outputResultId) {
        this.outputResultId = outputResultId;
    }

    public Date getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(final Date generatedDate) {
        this.generatedDate = generatedDate;
    }

    public String getOutputSettings() {
        return outputSettings;
    }

    public void setOutputSettings(String outputSettings) {
        this.outputSettings = outputSettings;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(Date deleteDate) {
        this.deleteDate = deleteDate;
    }
}
