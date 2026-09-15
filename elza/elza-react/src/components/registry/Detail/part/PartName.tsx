import classNames from 'classnames';
import React, { FC } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { apDetailMessages } from '../messages';
import './DetailPart.scss';
import { SyncState } from 'elza-api';
import { SyncIcon } from "../sync-icon";

export const PartName:FC<{
    label: string,
    collapsed?: boolean,
    preferred?: boolean,
    onClick?: (event: React.MouseEvent) => void,
    binding?: boolean;
}> = ({
    label,
    collapsed = true,
    preferred = false,
    onClick,
    binding,
}) => {
    const intl = useIntl();

    // label = label.replace(/^\w/, (c) => c.toUpperCase())

    return <div
        title={collapsed ? intl.formatMessage(apDetailMessages.detailExpandInfo) : intl.formatMessage(apDetailMessages.detailCollapseInfo)}
        className="detail-part-label"
        onClick={onClick}
    >
        <span
            className={classNames({
                "preferred": preferred,
                "opened": !collapsed,
            })}
        >
            {label || <i>{<FormattedMessage {...apDetailMessages.detailInfo} />}</i>}
        </span>
        <div className="sync-wrapper">
            {(binding != null) && (
                <SyncIcon
                    syncState={
                    binding ?
                        SyncState.SyncOk :
                        SyncState.LocalChange
                }
                    />
            )}
        </div>
    </div>
}
