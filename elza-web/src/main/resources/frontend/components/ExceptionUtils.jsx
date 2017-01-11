import React from "react";
import {i18n, Toastr, LongText, Exception} from "components/index.jsx";
import {addToastr} from "components/shared/toastr/ToastrActions.jsx";

/**
 * Sestavení výjimky.
 *
 * @param data data výjimky
 */
export function createException(data) {

    let toaster;

    switch (data.type) {
        case 'BaseCode': {
            toaster = resolveBase(data);
            break;
        }
        case 'ArrangementCode': {
            toaster = resolveArrangement(data);
            break;
        }
    }

    if (toaster == null) {
        toaster = resolveUndefined(data);
    }

    return toaster;
}

function resolveBase(data) {
    switch (data.code) {
        case 'BAD_REQUEST': {
            return createToaster(i18n('global.exception.bad.request'), data, "danger");
        }
        case 'INSUFFICIENT_PERMISSIONS': {
            return createToaster(i18n('global.exception.permission.need'), data, "danger", (p) => {
                return <small><b>{i18n('global.exception.permission.need')}:</b> {p.permission && p.permission.map((item)=>i18n('permission.' + item)).join(", ")}</small>
            });
        }
        case 'OPTIMISTIC_LOCKING_ERROR': {
            return createToaster(i18n('global.exception.permission.need'), data, "danger", (p, m) => {
                return <LongText text={m} />
            });
        }
    }
}

function resolveArrangement(data) {
    switch (data.code) {
        case 'PACKET_DELETE_ERROR': {
            return createToaster(i18n('arr.fund.packets.action.delete.problem'), data, "warning", (p) => {
                if (p.packets) {
                    return <LongText text={i18n('arr.exception.delete.packets', p.packets.map((item)=>item).join(", "))}/>
                }
            });
        }
        case 'VERSION_ALREADY_CLOSED': {
            return createToaster(i18n('arr.exception.version.already.closed'), data, "warning");
        }
        case 'FUND_NOT_FOUND': {
            return createToaster(i18n('arr.exception.fund.not.found'), data, "danger");
        }
        case 'FUND_VERSION_NOT_FOUND': {
            return createToaster(i18n('arr.exception.fund.version.not.found'), data, "danger");
        }
        case 'NODE_NOT_FOUND': {
            return createToaster(i18n('arr.exception.node.not.found'), data, "warning");
        }
        case 'VERSION_CANNOT_CLOSE_ACTION': {
            return createToaster(i18n('arr.exception.version.cannot.close.action'), data, "info");
        }
        case 'VERSION_CANNOT_CLOSE_VALIDATION': {
            return createToaster(i18n('arr.exception.version.cannot.close.validation'), data, "info");
        }
        case 'EXISTS_NEWER_CHANGE': {
            return createToaster(i18n('arr.exception.exists.newer.change'), data, "warning");
        }
        case 'EXISTS_BLOCKING_CHANGE': {
            return createToaster(i18n('arr.exception.exists.blocking.change'), data, "warning");
        }
        case 'ALREADY_ADDED': {
            return createToaster(i18n('arr.exception.already.added'), data, "warning");
        }
        case 'ALREADY_REMOVED': {
            return createToaster(i18n('arr.exception.already.removed'), data, "warning");
        }
        case 'ILLEGAL_COUNT_EXTERNAL_SYSTEM': {
            return createToaster(i18n('arr.exception.illegal.count.external.system'), data, "warning");
        }
    }
}

function resolveUndefined(data) {
    return createToaster(i18n('global.exception.undefined'), data, "danger", (p, m) => {
        return <LongText text={m} />
    });
}

function createToaster(title, data, type, textRenderer, size = "lg", time = null) {
    return addToastr(title, [<Exception title={title} data={data} textRenderer={textRenderer} />], type, size, time);
}

