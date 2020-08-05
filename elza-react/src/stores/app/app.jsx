import * as types from 'actions/constants/ActionTypes.js';
import DetailReducer from 'shared/detail/DetailReducer';
import SimpleListReducer from 'shared/list/simple/SimpleListReducer';
import processAreaStores from 'shared/utils/processAreaStores';
import registryDetail from 'stores/app/registry/registryDetail';
import SharedReducer from '../../shared/shared/SharedReducer';

const initialState = {
    registryDetail: registryDetail(),
    apValidation: DetailReducer(),
    apViewSettings: DetailReducer(),
    registryDetailHistory: SimpleListReducer(),
    preparedRequestList: SimpleListReducer(), // seznam neodeslaných požadavků - sdíleno pro celou aplikaci
    requestInQueueList: SimpleListReducer(), // seznam požadavků ve frontě
    apExtSystemList: SimpleListReducer(), // seznam externích systémů
    extSystemDetail: DetailReducer(),
    extSystemList: SimpleListReducer(), // seznam externích systémů
    mimeTypesList: SimpleListReducer(), // seznam mime typů pro editaci systémů
    issueDetail: DetailReducer(), // Detail připomínky
    issueComments: SimpleListReducer(), // Komentáře připomínky
    issueList: SimpleListReducer(undefined, undefined, {filter: {type: '', state: '', protocol: ''}}), // Seznam připomínek
    issueProtocol: DetailReducer(), // Detail protokolu přípomínek
    issueProtocols: SimpleListReducer(), // Seznam protokolů
    issueProtocolsConfig: SimpleListReducer(), // Seznam protokolů v konfiguraci
    registryList: SimpleListReducer(undefined, undefined, {
        filter: {
            text: null,
            registryTypeId: null,
            versionId: null,
            itemSpecId: null,
            parents: [],
            typesToRoot: null,
            scopeId: null,
            from: 0,
            excludeInvalid: true,
        },
    }),
    arrStructure: SimpleListReducer(undefined, undefined, {
        filter: {
            text: '',
            fundVersionId: null,
            structureCode: null,
            from: 0,
            state: '',
        },
    }),
    scopeList: SimpleListReducer(), // Seznam oblastí
    scopeDetail: DetailReducer(), // Detail oblasti
    languageList: SimpleListReducer(), // Seznam jazyků
    shared: SharedReducer(),
};

export default function app(state = initialState, action) {
    if (action.area && typeof action.area === 'string') {
        return processAreaStores(state, action);
    }

    if (action.type === types.STORE_SAVE) {
        return {
            registryDetail: DetailReducer(state.registryDetail, action),
        };
    }

    if (action.type === types.STORE_LOAD && action.store === 'app') {
        const newState = {...state};

        if (action.registryDetail) {
            newState.registryDetail = DetailReducer(state.registryDetail, {
                ...action.registryDetail,
                type: types.STORE_LOAD,
                store: 'app',
            });
        }

        return newState;
    }

    return state;
}
