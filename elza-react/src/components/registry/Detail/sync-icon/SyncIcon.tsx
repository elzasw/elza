import React, { FC } from "react";
import { SyncState } from 'elza-api';
import { useIntl } from 'react-intl';
import { messageFor } from 'components/shared/lang/dynamicMessage';
import { apDetailMessages, syncStateMessages } from '../messages';
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
    const intl = useIntl();
    const classname = classnames("sync", {
        "not-synced": syncState === SyncState.NotSynced,
        "sync-ok": syncState === SyncState.SyncOk,
        "local-change": syncState === SyncState.LocalChange,
    })
    return <div className="sync-icon">
        <Icon
            glyph={getIconForState(syncState)}
            title={intl.formatMessage(messageFor(syncStateMessages, syncState, apDetailMessages.bindingSyncStateSYNC_OK))}
            className={classname}
            />
    </div>
}

