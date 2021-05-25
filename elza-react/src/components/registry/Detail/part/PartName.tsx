import classNames from 'classnames';
import React, { FC } from 'react';
import i18n from '../../../i18n';
import './DetailPart.scss';

export const PartName:FC<{
    label: string,
    collapsed?: boolean,
    preferred?: boolean,
    onClick?: (event: React.MouseEvent) => void,
}> = ({
    label, 
    collapsed = true, 
    preferred = false,
    onClick,
}) => {

    // label = label.replace(/^\w/, (c) => c.toUpperCase())

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
        {preferred && (
            <span className={classNames('detail-part-label-alt', collapsed ? false : 'opened')}>
                {' '}
                (preferované)
            </span>
        )}
    </div>
}
