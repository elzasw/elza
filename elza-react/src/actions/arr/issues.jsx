import {SimpleListActions} from 'shared/list';
import {DetailActions} from 'shared/detail';
import {WebApi} from '../WebApi';
import {addToastrInfo} from '../../components/shared/toastr/ToastrActions';
import {fundSelectSubNode} from './node';
import {i18n} from '../../components/shared';
import {createFundRoot} from '../../components/arr/ArrUtils';

export const AREA_LIST = 'issueList';
export const AREA_PROTOCOL = 'issueProtocol';
export const AREA_PROTOCOLS = 'issueProtocols';
export const AREA_PROTOCOLS_CONFIG = 'issueProtocolsConfig';
export const AREA_DETAIL = 'issueDetail';
export const AREA_COMMENTS = 'issueComments';

const dataToRowsHelper = data => ({rows: data, count: data.length});

// Seznam protokolů
export const protocols = {
    fetchIfNeeded: (parent, force = false) =>
        SimpleListActions.fetchIfNeeded(
            AREA_PROTOCOLS,
            parent,
            (parent, filter) => WebApi.findIssueListByFund(parent, filter.open).then(dataToRowsHelper),
            force,
        ),
    filter: filter => SimpleListActions.filter(AREA_PROTOCOLS, filter),
    invalidate: id => SimpleListActions.invalidate(AREA_PROTOCOLS, id),
};

// Seznam protokolů pro nastavení
export const protocolsConfig = {
    fetchIfNeeded: (parent, force = false) =>
        SimpleListActions.fetchIfNeeded(
            AREA_PROTOCOLS_CONFIG,
            parent,
            (parent, filter) => WebApi.findIssueListByFund(parent, filter.open).then(dataToRowsHelper),
            force,
        ),
    filter: filter => SimpleListActions.filter(AREA_PROTOCOLS_CONFIG, filter),
    invalidate: id => SimpleListActions.invalidate(AREA_PROTOCOLS_CONFIG, id),
};

// Detail protokolu
export const protocol = {
    fetchIfNeeded: (id, force = false) =>
        DetailActions.fetchIfNeeded(AREA_PROTOCOL, id, id => WebApi.getIssueList(id), force),
    invalidate: id => DetailActions.invalidate(AREA_PROTOCOL, id),
};

// List připomínek
export const list = {
    fetchIfNeeded: (parent, force = false) =>
        SimpleListActions.fetchIfNeeded(
            AREA_LIST,
            parent,
            (parent, filter) => WebApi.findIssueByIssueList(parent, filter.state, filter.type).then(dataToRowsHelper),
            force,
        ),
    filter: filter => SimpleListActions.filter(AREA_LIST, filter),
    invalidate: id => SimpleListActions.invalidate(AREA_LIST, id),
    reset: () => SimpleListActions.reset(AREA_LIST),
};

// Detail připomínky
export const detail = {
    fetchIfNeeded: (id, force = false) =>
        DetailActions.fetchIfNeeded(AREA_DETAIL, id, id => WebApi.getIssue(id), force),
    invalidate: id => DetailActions.invalidate(AREA_DETAIL, id),
    select: id => DetailActions.select(AREA_DETAIL, id),
    reset: () => DetailActions.reset(AREA_DETAIL),
};

// Komentáře připomínky
export const comments = {
    fetchIfNeeded: (id, force = false) =>
        SimpleListActions.fetchIfNeeded(
            AREA_COMMENTS,
            id,
            id => WebApi.findIssueCommentByIssue(id).then(dataToRowsHelper),
            force,
        ),
    invalidate: id => SimpleListActions.invalidate(AREA_COMMENTS, id),
    reset: () => SimpleListActions.reset(AREA_COMMENTS),
};

export function nodeWithIssueByFundVersion(fund, nodeId, direction) {
    return dispatch => {
        const fundVersionId = fund.versionId;
        WebApi.nextIssueByFundVersion(fundVersionId, nodeId, direction).then(function(data) {
            if (data.node !== null && data.nodeCount > 0) {
                const node = data.node;
                if (node.parentNode == null) {
                    node.parentNode = createFundRoot(fund);
                }
                dispatch(fundSelectSubNode(fundVersionId, node.id, node.parentNode));
            } else {
                dispatch(addToastrInfo(i18n('toast.arr.validation.issues.notFound')));
            }
        });
    };
}
