import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {Api} from 'api';
import {ApiKeyState} from 'elza-api';
import {i18n} from 'components/shared';

const stateOrder = (state) => {
    switch (state) {
        case ApiKeyState.Active: return 0;
        case ApiKeyState.Expired: return 1;
        case ApiKeyState.Revoked: return 2;
        default: return 3;
    }
};

const stateLabel = (state) => {
    switch (state) {
        case ApiKeyState.Active: return i18n('admin.perms.tabs.accessKeys.state.active');
        case ApiKeyState.Expired: return i18n('admin.perms.tabs.accessKeys.state.expired');
        case ApiKeyState.Revoked: return i18n('admin.perms.tabs.accessKeys.state.revoked');
        default: return state;
    }
};

const formatDate = (iso) => iso ? new Date(iso).toLocaleDateString() : '';

/**
 * Read-only overview of a user's personal API keys with a revoke action.
 * Creation is reserved for the key owner, so this panel has no "new key" button.
 */
function AccessKeysPanel({userId}) {
    const [keys, setKeys] = useState([]);

    const load = useCallback(async () => {
        const {data} = await Api.admin.adminListUserApiKeys(userId);
        setKeys(data);
    }, [userId]);

    useEffect(() => { load(); }, [load]);

    const sorted = useMemo(
        () => [...keys].sort((a, b) => {
            const so = stateOrder(a.state) - stateOrder(b.state);
            return so !== 0 ? so : b.createDate.localeCompare(a.createDate);
        }),
        [keys]
    );

    const handleRevoke = async (key) => {
        const msg = i18n('admin.perms.tabs.accessKeys.revoke.confirm', {name: key.name});
        if (!window.confirm(msg)) return;
        await Api.admin.adminRevokeUserApiKey(userId, key.id);
        load();
    };

    if (sorted.length === 0) {
        return <div className="access-keys-panel">{i18n('admin.perms.tabs.accessKeys.empty')}</div>;
    }

    return (
        <div className="access-keys-panel">
            <table className="table table-sm">
                <thead>
                    <tr>
                        <th>{i18n('admin.perms.tabs.accessKeys.column.state')}</th>
                        <th>Název</th>
                        <th>ID</th>
                        <th>{i18n('admin.perms.tabs.accessKeys.column.created')}</th>
                        <th>{i18n('admin.perms.tabs.accessKeys.column.expires')}</th>
                        <th>{i18n('admin.perms.tabs.accessKeys.column.lastUsed')}</th>
                        <th />
                    </tr>
                </thead>
                <tbody>
                    {sorted.map((k) => (
                        <tr key={k.id} style={k.state !== ApiKeyState.Active ? {opacity: 0.6} : undefined}>
                            <td>{stateLabel(k.state)}</td>
                            <td>{k.name}</td>
                            <td><code>{k.keyId}</code></td>
                            <td>{formatDate(k.createDate)}</td>
                            <td>{formatDate(k.expireDate)}</td>
                            <td>{k.lastUsedDate ? formatDate(k.lastUsedDate) : i18n('admin.perms.tabs.accessKeys.lastUsedNever')}</td>
                            <td>
                                {k.state === ApiKeyState.Active && (
                                    <button className="btn btn-sm btn-link" onClick={() => handleRevoke(k)}>
                                        {i18n('admin.perms.tabs.accessKeys.revoke')}
                                    </button>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export default AccessKeysPanel;
