import * as types from 'actions/constants/actionTypes';

import {Toastr, i18n} from 'components';

export function changeConformityInfo(findingAidVersionId, nodeId) {
    return {
        type: types.CHANGE_CONFORMITY_INFO,
        findingAidVersionId: findingAidVersionId,
        nodeId: nodeId
    }
}

export function changeIndexingFinished() {

    Toastr.Actions.success({
        title: i18n("admin.fulltext.message.success"),
    });

    return {
        type: types.CHANGE_INDEXING_FINISHED
    }
}