import {AccesspointsApi, FundsApi} from "elza-api";
import globalAxios, {AxiosRequestConfig, AxiosResponse, AxiosError} from "axios";
import i18n from '../components/i18n';
import {createException} from 'components/ExceptionUtils.jsx';
import {logout} from "actions/global/login";
import {store} from 'stores/index.jsx';

// @ts-ignore
const serverContextPath = window.serverContextPath;

const baseApiPath = "/api";
const v1ApiPath = "/v1";
const basePath = `${serverContextPath}${baseApiPath}${v1ApiPath}`;

let pendingRequests: (() => void)[] = [];

/*
globalAxios.interceptors.request.use((config) => {
    return config;
})
*/

globalAxios.interceptors.response.use(undefined, (error) => {
    const exception = resolveException(error);
    if(exception.unauthorized && !error.config.noPending){
        return createPendingPromise(error.config);
    }
    return exception;
})

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

function resolveException(error: AxiosError) {
    let result: IError = {
        type: "unknown"
    };

    if(error.response){
        const {status, data, statusText} = error.response;

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
    console.error("ERROR", result)

    if(store){
        if (result.createToaster) {
            store.dispatch(createException(result));
        }

        if(result.unauthorized){
            store.dispatch(logout());
        }
    }

    return result;
}

/**
 * Creates a new promise and stores it as a funcion in pending requests array
 */
const createPendingPromise = (config: AxiosRequestConfig): Promise<AxiosResponse<any>> => {
    console.log("create pending promise");
    return new Promise((resolve, reject) => {
        pendingRequests.push(() => {
            globalAxios.request(config).then((response) => {
                resolve(response);
            }).catch((error) => {
                reject(error)
            })
        });
    })
}

/**
 * Resumes all promises in pending requests array
 */
export const continueRequests = () => {
    console.log("continue requests", pendingRequests.length);
    pendingRequests.forEach((resolve) => { resolve() })
    pendingRequests = [];
}

export const Api = {
    accesspoints: new AccesspointsApi({ basePath }, undefined, globalAxios),
    funds: new FundsApi({ basePath }, undefined, globalAxios),
}
