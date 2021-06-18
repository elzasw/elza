package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.List;

import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;

/**
 * VO výstup z archivního souboru.
 *
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

    private List<Integer> templateIds;

    private List<Integer> outputResultIds;

    /**
     * Datum generovani vystupu
     * 
     * Prevezme se z prvniho vystupu
     */
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

    private List<ApScopeVO> scopes;

    private ApAccessPointVO anonymizedAp;

    private Integer outputFilterId;
    
    public ArrOutputVO() {
    	
    }

    public ArrOutputVO(ArrOutput output) {
		id = output.getOutputId();
		internalCode = output.getInternalCode();
		name = output.getName();
		state = output.getState();
		error = output.getError();
		outputTypeId = output.getOutputType().getOutputTypeId();
		createDate = (Date.from(output.getCreateChange().getChangeDate().toInstant()));
		version = output.getVersion();
		if(output.getDeleteChange()!=null) {
          deleteDate = Date.from(output.getDeleteChange().getChangeDate().toInstant());
		}
		if (output.getOutputFilter() != null) {
		    outputFilterId = output.getOutputFilter().getOutputFilterId();
        }
	}

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

    public List<Integer> getTemplateIds() {
        return templateIds;
    }
    
    public void setTemplateIds(final List<Integer> templateIds) {
    	this.templateIds = templateIds;
    }

    public List<Integer> getOutputResultIds() {
        return outputResultIds;
    }

    public void setOutputResultIds(final List<Integer> outputResultIds) {
        this.outputResultIds = outputResultIds;
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

    public List<ApScopeVO> getScopes() {
        return scopes;
    }

    public void setScopes(List<ApScopeVO> scopes) {
        this.scopes = scopes;
    }

    public ApAccessPointVO getAnonymizedAp() {
        return anonymizedAp;
    }

    public void setAnonymizedAp(ApAccessPointVO anonymizedAp) {
        this.anonymizedAp = anonymizedAp;
    }

    public Integer getOutputFilterId() {
        return outputFilterId;
    }

    public void setOutputFilterId(Integer outputFilterId) {
        this.outputFilterId = outputFilterId;
    }

    /**
     * Create basic vo object
     * @param outputData
     * @return
     */
    public static ArrOutputVO newInstance(ArrOutput output) {
		ArrOutputVO outputVo = new ArrOutputVO(output);
		return outputVo;
    }

}
