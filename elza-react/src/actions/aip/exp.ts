import { DetailActions } from 'shared/detail';

export const AREA_EXPLORER_ITEM = 'explorerItem';

export const setExplorerItem = (id, item) => {
    return DetailActions.updateValue(AREA_EXPLORER_ITEM, id, item);
}


