import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from '../../../shared/list/simple/SimpleListActions';
import * as DetailActions from '../../../shared/detail/DetailActions';

export const AREA_DAOS = 'daoList';
export const AREA_DAO = 'aip';

export const daDaoFetchIfNeeded = (aipId) => {
    return SimpleListActions.fetchIfNeeded(AREA_DAOS, aipId, (aipId: number) => WebApi.getDaDaoListByAipId(aipId));
}

export const setDao = (aip) => {
    return DetailActions.updateValue(AREA_DAO, aip.id, aip);
}

