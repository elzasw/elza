import { WebApi } from 'actions/WebApi';
import { DetailActions } from 'shared/detail';

export const AREA_AIP_STRUCTURE = 'aipStructure';

export const fetchAipStructureIfNeeded = (aipId: number, forceFetch = false) => {
    return DetailActions.fetchIfNeeded(AREA_AIP_STRUCTURE, aipId, (aipId: number) =>
        {
            return WebApi.getDaDaoListByAipId(aipId)
        },
        forceFetch
    );
}

