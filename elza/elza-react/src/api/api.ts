import { AccesspointsApi, AdminApi, DaosApi, FundsApi, DefaultApi, IoApi } from 'elza-api';
import globalAxios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import i18n from '../components/i18n';
import { createException } from 'components/ExceptionUtils.jsx';
import { logout } from 'actions/global/login';
import { store } from 'stores/index.jsx';

declare module "axios" {
    export interface AxiosRequestConfig {
        overrideErrorHandler?: boolean;
    }
}

// @ts-ignore
const serverContextPath = window.serverContextPath;

const baseApiPath = '/api';
const v1ApiPath = '/v1';
const basePath = `${serverContextPath}${baseApiPath}${v1ApiPath}`;

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
    throw exception;
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
                type: 'unknown',
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
    admin: AdminApi;
    funds: FundsApi;
    daos: DaosApi;
    default: DefaultApi;
    io: IoApi;
} = {
    accesspoints: new AccesspointsApi(undefined, basePath, axios),
    admin: new AdminApi(undefined, basePath, axios),
    funds: new FundsApi(undefined, basePath, axios),
    daos: new DaosApi(undefined, basePath, axios),
    default: new DefaultApi(undefined, basePath, axios),
    io: new IoApi(undefined, basePath, axios),
};
