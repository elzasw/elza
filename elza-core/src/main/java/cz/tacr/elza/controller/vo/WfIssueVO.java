package cz.tacr.elza.controller.vo;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Připomínka
 */
public class WfIssueVO {

    // --- fields ---

    private Integer id;
    @NotNull
    private Integer issueListId;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Integer nodeId;
    @NotNull
    private Integer issueTypeId;
    private Integer issueStateId;
    @NotBlank
    private String description;
    private Integer userCreateId;

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIssueListId() {
        return issueListId;
    }

    public void setIssueListId(Integer issueListId) {
        this.issueListId = issueListId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getIssueTypeId() {
        return issueTypeId;
    }

    public void setIssueTypeId(Integer issueTypeId) {
        this.issueTypeId = issueTypeId;
    }

    public Integer getIssueStateId() {
        return issueStateId;
    }

    public void setIssueStateId(Integer issueStateId) {
        this.issueStateId = issueStateId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUserCreateId() {
        return userCreateId;
    }

    public void setUserCreateId(Integer userCreateId) {
        this.userCreateId = userCreateId;
    }
}
