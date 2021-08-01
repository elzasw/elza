import React, { FC } from "react";
import { SyncState } from '../../../../api/SyncState';
import i18n from '../../../i18n';
import { Icon } from '../../../index';
import "./SyncIcon.scss";
import classnames from "classnames";

export const SyncIcon:FC<{
    syncState: SyncState;
}> = ({
    syncState = SyncState.SYNC_OK
}) => {
    const classname = classnames("sync", {
        "not-synced": syncState === SyncState.NOT_SYNCED,
        "sync-ok": syncState === SyncState.SYNC_OK,
        "local-change": syncState === SyncState.LOCAL_CHANGE,
    })
    return <div className="sync-icon">
        <Icon
            glyph="fa-circle"
            title={i18n('ap.binding.syncState.' + syncState)}
            className={classname}
            />
    </div>
}

