import classNames from 'classnames';
import React, { FC } from 'react';
import i18n from '../../../i18n';
import './DetailPart.scss';
import { SyncState } from '../../../../api/SyncState';
import { SyncIcon } from "../sync-icon";

export const PartName:FC<{
    label: string,
    collapsed?: boolean,
    preferred?: boolean,
    newPreferred?: boolean,
    oldPreferred?: boolean,
    onClick?: (event: React.MouseEvent) => void,
    binding?: boolean;
}> = ({
    label,
    collapsed = true,
    preferred = false,
    newPreferred = false,
    oldPreferred = false,
    onClick,
    binding,
}) => {

    // label = label.replace(/^\w/, (c) => c.toUpperCase())

    const preferredText = () => {
        if (newPreferred) {
            return '(nové preferované)';
        } else if (oldPreferred) {
            return '(předchozí preferované)';
        } else if (preferred) {
            return '(preferované)';
        }
    };

    return <div
        title={collapsed ? i18n("ap.detail.expandInfo") : i18n("ap.detail.collapseInfo")}
        className="detail-part-label"
        onClick={onClick}
    >
        <span
            className={classNames({
                "preferred": preferred,
                "opened": !collapsed,
            })}
        >
            {label || <i>{i18n("ap.detail.info")}</i>}
        </span>
        {(preferred || oldPreferred || newPreferred) && (
            <span className={classNames('detail-part-label-alt', collapsed ? false : 'opened')}>
                {' '}
                {preferredText()}
            </span>
        )}
        <div className="sync-wrapper">
            {(binding != null) && (
                <SyncIcon
                    syncState={
                    binding ?
                        SyncState.SYNC_OK :
                        SyncState.LOCAL_CHANGE
                }
                    />
            )}
        </div>
    </div>
}
