package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.WfIssueType;

public class WfIssueTypeVO extends BaseCodeVo {

    public static WfIssueTypeVO newInstance(final WfIssueType issueType) {
        WfIssueTypeVO fragment = new WfIssueTypeVO();
        fragment.setId(issueType.getIssueTypeId());
        fragment.setCode(issueType.getCode());
        fragment.setName(issueType.getName());
        return fragment;
    }

}
