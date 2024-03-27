import React from 'react';
import { addToastr } from 'components/shared/toastr/ToastrActions.jsx';
import LongText from './LongText';
import i18n from './i18n';
import Exception from './shared/exception/Exception';
import { urlEntity } from '../constants';
import { Link } from 'react-router-dom';

const TYPE2GROUP = {
    ArrangementCode: 'arr',
    BaseCode: 'base',
    BulkActionCode: 'ba',
    DigitizationCode: 'dig',
    ExternalCode: 'ext',
    OutputCode: 'out',
    PackageCode: 'pkg',
    RegistryCode: 'reg',
    StructObjCode: 'sobj',
    UserCode: 'usr',
};

/**
 * Sestavení výjimky.
 *
 * @param data data výjimky
 */
export function createException(data) {
    let toaster;

    // prohledání extra definovaných vyjímek
    switch (data.type) {
        case 'BaseCode': {
            toaster = resolveBase(data);
            break;
        }
        case 'ArrangementCode': {
            toaster = resolveArrangement(data);
            break;
        }
        case 'RegistryCode': {
            toaster = resolveRegistry(data);
            break;
        }
        default:
            break;
    }

    if (toaster == null) {
        toaster = resolveDefault(data);
    }

    return toaster;
}

/**
 * Vytvoření netypické vyjímky pro BaseCode.
 *
 * @param data data výjimky
 */
function resolveBase(data) {
    switch (data.code) {
        case 'INSUFFICIENT_PERMISSIONS': {
            return createToaster(i18n('exception.base.INSUFFICIENT_PERMISSIONS'), data, p => {
                return (
                    <small>
                        <b>{i18n('exception.base.INSUFFICIENT_PERMISSIONS.detail')}:</b>{' '}
                        {p.permission && p.permission.map(item => i18n('permission.' + item)).join(', ')}
                    </small>
                );
            });
        }
        case 'OPTIMISTIC_LOCKING_ERROR': {
            return createToaster(i18n('global.exception.permission.need'), data, (p, m) => {
                return <LongText text={m} />;
            });
        }
        case 'GENERATING_EXPORT_FAILED': {
            return createToaster(i18n('exception.base.GENERATING_EXPORT_FAILED'), data, (p, m) => {
                return <LongText text={m} />;
            });
        }
        default:
            break;
    }
}

/**
 * Vytvoření netypické vyjímky pro RegistryCode.
 *
 * @param data data výjimky
 */
function resolveRegistry(data) {
    switch (data.code) {
        case 'CANT_EXPORT_DELETED_AP': {
            const entityBtn = (id) => (
                <Link to={urlEntity(id)}>
                    <span>{id}</span>
                </Link>
            );
            return createToaster(i18n('exception.base.EXPORT_FAILED_DELETED_AP'), data, p => {
                return (
                    <>
                        <b>{i18n('exception.base.EXPORT_FAILED_DELETED_AP.detail')}:</b>
                        <ul>
                            {p.accessPointId && p.accessPointId.map((item) => <li key={item}>{entityBtn(item)}</li>)}
                        </ul>
                    </>
                );
            });
        }
        default:
            break;
    }
}

/**
 * Vytvoření netypické vyjímky pro ArrangementCode.
 *
 * @param data data výjimky
 */
function resolveArrangement(data) {
    console.error("Arrangement error", data);
    /*
    switch (
        data.code
       Legacy code - jen pro ukázku jak to udělat
        case 'X_DELETE_ERROR': {
            return createToaster(i18n('exception.arr.X_DELETE_ERROR'), data, (p) => {
                if (p.x) {
                    return <LongText text={i18n('exception.arr.X_DELETE_ERROR.detail', p.x.map((item)=>item).join(", "))}/>
                }
            });
        }
    ) {
    }
    */
}

/**
 * Existuje překladový text?
 *
 * @param key hledaný klíč
 * @return {boolean} existuje?
 */
function existsI18n(key) {
    return i18n('^' + key) !== null;
}

function resolveDefault(data) {
    if (TYPE2GROUP[data.type] === undefined) {
        if (data.data?.detail) {
            return createToaster(data.data.detail, {
                message: data.data.detail,
                stackTrace: JSON.stringify(data.data),
                status: data.status,
                statusText: data.statusText,
            });
        }

        return createToaster("Unknown error", {
            message: "Unknown error",
            stackTrace: JSON.stringify(data),
        });
    }

    const key = 'exception.' + TYPE2GROUP[data.type] + '.' + data.code;

    if (!existsI18n(key)) {
        console.warn("i18n('" + key + "') not found, please add to translate file");
    }

    return createToaster(i18n(key, data.properties), data);
}

/**
 * Vytvoření toastr komponenty.
 *
 * @param title        název
 * @param data         data vyjímky
 * @param textRenderer callback na vlastní renderování textu
 * @param size         velikost toastru
 * @param time         délka zobrazení - ms
 * @returns vytvořená komponenta
 */
function createToaster(title, data, textRenderer, size = 'lg', time = null) {
    const type = data.level ? data.level : 'danger';
    return addToastr(
        title,
        [<Exception key="exception-key" title={title} data={data} textRenderer={textRenderer} />],
        type,
        size,
        time,
    );
}
