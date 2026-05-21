import React, { FC } from "react";
import { SyncState } from 'elza-api';
import i18n from '../../../i18n';
import { Icon } from '../../../index';
import "./SyncIcon.scss";
import classnames from "classnames";

const getIconForState = (state: SyncState) => {
    if(state === SyncState.SyncOk) {return "fa-circle"}
    return "fa-square";
}

export const SyncIcon:FC<{
    syncState: SyncState;
}> = ({
    syncState = SyncState.SyncOk
}) => {
    const classname = classnames("sync", {
        "not-synced": syncState === SyncState.NotSynced,
        "sync-ok": syncState === SyncState.SyncOk,
        "local-change": syncState === SyncState.LocalChange,
    })
    return <div className="sync-icon">
        <Icon
            glyph={getIconForState(syncState)}
            title={i18n('ap.binding.syncState.' + syncState)}
            className={classname}
            />
    </div>
}

