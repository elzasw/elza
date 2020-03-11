import React from 'react';
import {addToastr} from 'components/shared/toastr/ToastrActions.jsx';
import LongText from './LongText';
import i18n from './i18n';
import Exception from './shared/exception/Exception';

const TYPE2GROUP = {
    'ArrangementCode': 'arr',
    'BaseCode': 'base',
    'BulkActionCode': 'ba',
    'DigitizationCode': 'dig',
    'ExternalCode': 'ext',
    'OutputCode': 'out',
    'PackageCode': 'pkg',
    'RegistryCode': 'reg',
    'StructObjCode': 'sobj',
    'UserCode': 'usr',
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
            return createToaster(i18n('exception.base.INSUFFICIENT_PERMISSIONS'), data, (p) => {
                return <small><b>{i18n('exception.base.INSUFFICIENT_PERMISSIONS.detail')}:</b> {p.permission && p.permission.map((item) => i18n('permission.' + item)).join(', ')}
                </small>;
            });
        }
        case 'OPTIMISTIC_LOCKING_ERROR': {
            return createToaster(i18n('global.exception.permission.need'), data, (p, m) => {
                return <LongText text={m}/>;
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
    switch (data.code) {
        /*
       Legacy code - jen pro ukázku jak to udělat
        case 'X_DELETE_ERROR': {
            return createToaster(i18n('exception.arr.X_DELETE_ERROR'), data, (p) => {
                if (p.x) {
                    return <LongText text={i18n('exception.arr.X_DELETE_ERROR.detail', p.x.map((item)=>item).join(", "))}/>
                }
            });
        }
        */
    }
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
        return null;
    }

    const key = 'exception.' + TYPE2GROUP[data.type] + '.' + data.code;

    if (!existsI18n(key)) {
        console.warn('i18n(\'' + key + '\') not found, please add to translate file');
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
    return addToastr(title, [<Exception key="exception-key" title={title} data={data}
                                        textRenderer={textRenderer}/>], type, size, time);
}

