import * as SimpleListActions from '../../shared/list/simple/SimpleListActions';
import {WebApi} from '../WebApi';

export const MIME_TYPES_AREA = 'mimeTypesList';

export function mimeTypesFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(MIME_TYPES_AREA, '', (parent, filter) => {
        return WebApi.getMimeTypes().then(json => ({rows: json, count: 0}));
    });
}
