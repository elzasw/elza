import { describe, expect, it } from 'vitest';
import { AnyAction } from 'redux';
import { BulkActionRunState, BulkActionType } from 'elza-api';
// loads the store first, as the app does: the actions and the reducers import each other
import 'test/test-utils';
import * as types from 'actions/constants/ActionTypes';
import { AppState } from 'typings/store';
import { bulkActionStateChanged, BulkActionStateChangeMessage } from './bulkActionStateChanged';

/**
 * A finished, interrupted or failed run of the persistent sort updates the tree of the fund of the
 * run and tells the user - whatever the rule set calls the action.
 */

const openFund = (versionId: number) => ({
    versionId,
    nodes: { activeIndex: null as number | null, nodes: [] as unknown[] },
    fundTree: { expandedIds: [] as unknown[] },
});

/** Two open funds, the first one shown. */
const state = {
    arrRegion: { activeIndex: 0, funds: [openFund(1), openFund(2)] },
} as unknown as AppState;

function run(message: Partial<BulkActionStateChangeMessage>) {
    const actions: AnyAction[] = [];
    const dispatch = (action: AnyAction) => {
        actions.push(action);
        return action;
    };
    bulkActionStateChanged({ versionId: 2, entityId: 7, ...message })(dispatch as never, () => state);
    return actions;
}

const sort = { code: 'ISADG_PERSISTENT_SORT', actionType: BulkActionType.PersistentSort };

describe('bulkActionStateChanged', () => {
    it('refreshes the tree of the fund of the run, not of the shown fund, when the sort finishes', () => {
        const actions = run({ ...sort, state: BulkActionRunState.Finished });

        expect(actions).toContainEqual(expect.objectContaining({ type: types.FUND_FUND_TREE_INVALIDATE, versionId: 2 }));
        expect(actions).toContainEqual(expect.objectContaining({ type: types.TOASTR_ADD, style: 'success' }));
        expect(actions).toContainEqual(expect.objectContaining({ type: types.CHANGE_FUND_ACTION, versionId: 2 }));
    });

    it('reports a failed sort, which the server sends as ERROR', () => {
        const actions = run({ ...sort, state: BulkActionRunState.Error });

        expect(actions).toContainEqual(expect.objectContaining({ type: types.TOASTR_ADD, style: 'danger' }));
        expect(actions.map(a => a.type)).not.toContain(types.FUND_FUND_TREE_INVALIDATE);
    });

    it('does not take an action of another kind for the sort', () => {
        const actions = run({ code: 'PERZISTENTNI_RAZENI', actionType: BulkActionType.FundValidation, state: BulkActionRunState.Finished });

        expect(actions.map(a => a.type)).toEqual([types.CHANGE_FUND_ACTION]);
    });

    it('only records the change for a fund that is not open', () => {
        const actions = run({ ...sort, versionId: 3, state: BulkActionRunState.Finished });

        expect(actions.map(a => a.type)).toEqual([types.CHANGE_FUND_ACTION]);
    });
});
