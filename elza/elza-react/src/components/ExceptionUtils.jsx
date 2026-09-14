import React from 'react';
import { addToastr } from 'components/shared/toastr/ToastrActions.jsx';
import LongText from './LongText';
import { FormattedMessage } from 'react-intl';
import { getIntl } from './shared/lang/intlInstance';
import { exceptionMessages, permissionNameMessages } from './exception/messages';
import { ExceptionTitle } from './exception/ExceptionTitle';
import { TYPE2GROUP } from './exception/exceptionKey';
import Exception from './shared/exception/Exception';
import { urlEntity } from '../constants';
import { Link } from 'react-router-dom';


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
            return createToaster(<FormattedMessage {...exceptionMessages["exception.base.INSUFFICIENT_PERMISSIONS"]} />, data, p => {
                return (
                    <small>
                        <b>{<FormattedMessage {...exceptionMessages["exception.base.INSUFFICIENT_PERMISSIONS.detail"]} />}:</b>{' '}
                        {p.permission &&
                            p.permission
                                .map(item => {
                                    const message = permissionNameMessages['permission.' + item];
                                    return message ? getIntl().formatMessage(message) : item;
                                })
                                .join(', ')}
                    </small>
                );
            });
        }
        case 'OPTIMISTIC_LOCKING_ERROR': {
            return createToaster(<FormattedMessage {...exceptionMessages["exception.base.OPTIMISTIC_LOCKING_ERROR"]} />, data, (p, m) => {
                return <LongText text={m} />;
            });
        }
        case 'GENERATING_EXPORT_FAILED': {
            return createToaster(<FormattedMessage {...exceptionMessages["exception.base.GENERATING_EXPORT_FAILED"]} />, data, (p, m) => {
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
            return createToaster(<FormattedMessage {...exceptionMessages["exception.base.EXPORT_FAILED_DELETED_AP"]} />, data, p => {
                return (
                    <>
                        <b>{<FormattedMessage {...exceptionMessages["exception.base.EXPORT_FAILED_DELETED_AP.detail"]} />}:</b>
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

    // Titulek se formátuje až při renderu (ExceptionTitle), takže se přepíše
    // i při přepnutí jazyka; chybějící hlášku ohlásí do konzole sám.
    return createToaster(<ExceptionTitle data={data} />, data);
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
