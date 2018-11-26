import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'
import {WebApi} from "../WebApi";

export const AREA_LIST = "issueList";
export const AREA_PROTOCOLS = "issueProtocols";
export const AREA_DETAIL = "issueDetail";
export const AREA_COMMENTS = "issueComments";

const dataToRowsHelper = data => ({rows: data, count: data.length});

export const protocols = {
    fetchIfNeeded: (parent, force = false) => SimpleListActions.fetchIfNeeded(AREA_PROTOCOLS, parent, (parent, filter) => WebApi.findIssueListByFund(parent, filter.open).then(dataToRowsHelper), force),
    filter: (filter) => SimpleListActions.filter(AREA_PROTOCOLS, filter)
};

export const list = {
    fetchIfNeeded: (parent, force = false) => SimpleListActions.fetchIfNeeded(AREA_LIST, parent, (parent, filter) => WebApi.findIssueByIssueList(parent, filter.state, filter.type).then(dataToRowsHelper), force),
    filter:(filter) => SimpleListActions.filter(AREA_LIST, filter)
};


export const detail = {
    fetchIfNeeded: (id, force = false) => DetailActions.fetchIfNeeded(AREA_DETAIL, id, id => WebApi.getIssue(id), force),
};

export const comments = {
    fetchIfNeeded: (id, force = false) => SimpleListActions.fetchIfNeeded(AREA_COMMENTS, id, id => WebApi.findIssueCommentByIssue(id).then(dataToRowsHelper), force),
    invalidate: (id) => SimpleListActions.invalidate(AREA_COMMENTS, id),
};
