package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.WfIssueList;

/**
 * Základní údaje o protokolu (viz seznam oprávnění).
 */
public class WfIssueListBaseVO {

    // --- fields ---

    /**
     * Indentifikátor protokolu
     */
    private Integer id;

    /**
     * Název protokolu
     */
    private String name;

    public WfIssueListBaseVO() {
    }

    public WfIssueListBaseVO(WfIssueList issueList) {
        id = issueList.getIssueListId();
        name = issueList.getName();
    }

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WfIssueList createEntity() {
        WfIssueList entity = new WfIssueList();
        entity.setIssueListId(id);
        entity.setName(name);
        return entity;
    }

    public static WfIssueListBaseVO newInstance(WfIssueList issueList) {
        WfIssueListBaseVO vo = new WfIssueListBaseVO(issueList);
        return vo;
    }
}
