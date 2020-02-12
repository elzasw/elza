/**
 * Akce pro typy připomínek.
 */
import {DetailActions} from 'shared/detail'
import {WebApi} from "../WebApi";

export const AREA = 'refTables.issueTypes';

export function invalidate() {
    return DetailActions.invalidate(AREA, null);
}

export function fetchIfNeeded() {
    return DetailActions.fetchIfNeeded(AREA, true, WebApi.findAllIssueTypes);
}
