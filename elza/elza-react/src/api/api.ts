import {
    AccesspointsApi,
    AccesspointInternalApi,
    AdminApi,
    AiproviderApi,
    DaosApi,
    FundsApi,
    DefaultApi,
    IoApi,
    ExternalsystemsApi,
    DescitemsApi,
    AipsApi,
    ReportApi,
    NodeApi,
    RulesApi,
    TasksApi,
    StructureApi,
    PublicationInternalApi,
} from 'elza-api';
import globalAxios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import i18n from '../components/i18n';
import { createException } from 'components/ExceptionUtils.jsx';
import { logout } from 'actions/global/login';
import { store } from 'stores/index.jsx';

interface WindowEx extends Window {
    serverContextPath?: string;
}

declare module "axios" {
    export interface AxiosRequestConfig {
        overrideErrorHandler?: boolean;
    }
}

export const serverContextPath = (window as WindowEx).serverContextPath || "";
export function getServerContextPath() { return (window as WindowEx).serverContextPath || ""; }

const baseApiPath = '/api';
const v1ApiPath = '/v1';
export const basePath = `${serverContextPath}${baseApiPath}${v1ApiPath}`;

export const getFullPath = (path: string) => {
    if (path.startsWith('/')) {
        path = path.replace('/', '');
    }
    return `${basePath}/${path}`;
}

let pendingRequests: (() => void)[] = [];

/*
globalAxios.interceptors.request.use((config) => {
    return config;
})
*/

const axios = globalAxios.create();

axios.interceptors.response.use(undefined, error => {
    if (error.config.overrideErrorHandler) { throw error; }

    const exception = resolveException(error);
    if (exception.unauthorized && !error.config.noPending) {
        return createPendingPromise(error.config);
    }
    throw {
        ...exception,
        processed: true // mark exception as processed
    };
});

interface IError {
    type: string;
    data?: any;
    validation?: boolean;
    unauthorized?: boolean;
    createToaster?: boolean;
    message?: string;
    statusText?: string;
    level?: string;
    code?: string;
    properties?: any;
    stackTrace?: any;
    status?: number;
}

interface Error {
    type: string,
    code: string,
    level: string,
    properties: object,
    message: string,
    stackTrace: string,
}

function resolveException(error: AxiosError<Error>) {
    let result: IError = {
        type: 'unknown',
    };

    if (error.response) {
        const { status, data, statusText } = error.response;

        if (status == 422) {
            // pro validaci
            result = {
                type: 'validation',
                validation: true,
                data: data,
            };
        } else if (status == 400) {
            result = {
                createToaster: true,
                type: 'BaseCode',
                code: 'BAD_REQUEST',
                level: 'danger',
                message: i18n('global.exception.bad.request.tech'),
                status: status,
                statusText: statusText,
            };
        } else if (status == 401) {
            result = {
                type: 'unauthorized',
                unauthorized: true,
                data: data,
            };
        } else if (data) {
            // other errors containing data
            result = {
                createToaster: true,
                type: data.type,
                code: data.code,
                level: data.level,
                properties: data.properties,
                message: data.message,
                stackTrace: data.stackTrace,
                status: status,
                statusText: statusText,
            };
        } else {
            // other unknown errors
            result = {
                createToaster: true,
                type: 'unknown',
                status: status,
                statusText: statusText,
            };
        }
    }
    console.error('ERROR', result);

    if (store) {
        if (result.createToaster) {
            store.dispatch(createException(result));
        }

        if (result.unauthorized) {
            store.dispatch(logout());
        }
    }

    return result;
}

/**
 * Creates a new promise and stores it as a funcion in pending requests array
 */
const createPendingPromise = (config: AxiosRequestConfig): Promise<AxiosResponse<any>> => {
    console.log('create pending promise');
    return new Promise((resolve, reject) => {
        pendingRequests.push(() => {
            axios
                .request(config)
                .then(response => {
                    resolve(response);
                })
                .catch(error => {
                    reject(error);
                });
        });
    });
};

/**
 * Resumes all promises in pending requests array
 */
export const continueRequests = () => {
    console.log('continue requests', pendingRequests.length);
    pendingRequests.forEach(resolve => {
        resolve();
    });
    pendingRequests = [];
};

// Diagnosticky log
try {
    console.log("Axios basePath:", basePath);
    console.log("Parsed url:", new URL(basePath, window.location.origin));
} catch (e) {
    console.error("BasePath error:", e);
}

export const Api: {
    accesspoints: AccesspointsApi;
    accesspointInternal: AccesspointInternalApi;
    admin: AdminApi;
    aiprovider: AiproviderApi;
    funds: FundsApi;
    daos: DaosApi;
    default: DefaultApi;
    io: IoApi;
    externalSystems: ExternalsystemsApi;
    descItems: DescitemsApi;
    aips: AipsApi;
    node: NodeApi;
    rules: RulesApi;
    report: ReportApi;
    tasks: TasksApi;
    structure: StructureApi;
    publication: PublicationInternalApi;
} = {
    accesspoints: new AccesspointsApi(undefined, basePath, axios),
    accesspointInternal: new AccesspointInternalApi(undefined, basePath, axios),
    admin: new AdminApi(undefined, basePath, axios),
    aiprovider: new AiproviderApi(undefined, basePath, axios),
    funds: new FundsApi(undefined, basePath, axios),
    daos: new DaosApi(undefined, basePath, axios),
    default: new DefaultApi(undefined, basePath, axios),
    io: new IoApi(undefined, basePath, axios),
    externalSystems: new ExternalsystemsApi(undefined, basePath, axios),
    descItems: new DescitemsApi(undefined, basePath, axios),
    aips: new AipsApi(undefined, basePath, axios),
    node: new NodeApi(undefined, basePath, axios),
    rules: new RulesApi(undefined, basePath, axios),
    report: new ReportApi(undefined, basePath, axios),
    tasks: new TasksApi(undefined, basePath, axios),
    structure: new StructureApi(undefined, basePath, axios),
    publication: new PublicationInternalApi(undefined, basePath, axios),
};
