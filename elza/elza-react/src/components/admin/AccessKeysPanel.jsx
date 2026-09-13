import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {Api} from 'api';
import {ApiKeyState} from 'elza-api';
import {} from 'components/shared';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny; nové id dostal jen sloupec
// "Název", který byl v JSX natvrdo česky.
const messages = defineMessages({
    stateActive: { id: 'admin.perms.tabs.accessKeys.state.active', defaultMessage: 'Aktivní' },
    stateExpired: { id: 'admin.perms.tabs.accessKeys.state.expired', defaultMessage: 'Expirovaný' },
    stateRevoked: { id: 'admin.perms.tabs.accessKeys.state.revoked', defaultMessage: 'Zrušený' },
    revokeConfirm: {
        id: 'admin.perms.tabs.accessKeys.revoke.confirm',
        defaultMessage: 'Přejete si zrušit klíč "{name}"? Tato akce je nevratná.',
    },
    filterShowAll: {
        id: 'admin.perms.tabs.accessKeys.filter.showAll',
        defaultMessage: 'Zobrazit všechny klíče, včetně neaktivních',
    },
    empty: { id: 'admin.perms.tabs.accessKeys.empty', defaultMessage: 'Uživatel nemá žádné přístupové klíče.' },
    columnState: { id: 'admin.perms.tabs.accessKeys.column.state', defaultMessage: 'Stav' },
    columnName: { id: 'admin.perms.tabs.accessKeys.column.name', defaultMessage: 'Název' },
    columnCreated: { id: 'admin.perms.tabs.accessKeys.column.created', defaultMessage: 'Vytvořeno' },
    columnExpires: { id: 'admin.perms.tabs.accessKeys.column.expires', defaultMessage: 'Platnost do' },
    columnLastUsed: { id: 'admin.perms.tabs.accessKeys.column.lastUsed', defaultMessage: 'Naposledy použito' },
    lastUsedNever: { id: 'admin.perms.tabs.accessKeys.lastUsedNever', defaultMessage: 'nikdy' },
    revoke: { id: 'admin.perms.tabs.accessKeys.revoke', defaultMessage: 'Zrušit' },
});

const stateOrder = (state) => {
    switch (state) {
        case ApiKeyState.Active: return 0;
        case ApiKeyState.Expired: return 1;
        case ApiKeyState.Revoked: return 2;
        default: return 3;
    }
};

/** Vrací deskriptor, ne text - formátuje se až při renderu buňky. */
const stateMessage = (state) => {
    switch (state) {
        case ApiKeyState.Active: return messages.stateActive;
        case ApiKeyState.Expired: return messages.stateExpired;
        case ApiKeyState.Revoked: return messages.stateRevoked;
        default: return null;
    }
};

const formatDate = (iso) => iso ? new Date(iso).toLocaleDateString() : '';

/**
 * Read-only overview of a user's personal API keys with a revoke action.
 * Creation is reserved for the key owner, so this panel has no "new key" button.
 */
function AccessKeysPanel({userId}) {
    const intl = useIntl();
    const [keys, setKeys] = useState([]);
    const [showAll, setShowAll] = useState(false);

    const load = useCallback(async () => {
        const {data} = await Api.admin.adminListUserApiKeys(userId, showAll || undefined);
        setKeys(data);
    }, [userId, showAll]);

    useEffect(() => { load(); }, [load]);

    const sorted = useMemo(
        () => [...keys].sort((a, b) => {
            const so = stateOrder(a.state) - stateOrder(b.state);
            return so !== 0 ? so : b.createDate.localeCompare(a.createDate);
        }),
        [keys]
    );

    const handleRevoke = async (key) => {
        const msg = intl.formatMessage(messages.revokeConfirm, { name: key.name });
        if (!window.confirm(msg)) return;
        await Api.admin.adminRevokeUserApiKey(userId, key.id);
        load();
    };

    const filterCheckbox = (
        <label style={{marginBottom: '0.5rem', display: 'inline-block'}}>
            <input
                type="checkbox"
                checked={showAll}
                onChange={(e) => setShowAll(e.target.checked)}
            />
            {' '}<FormattedMessage {...messages.filterShowAll} />
        </label>
    );

    if (sorted.length === 0) {
        return (
            <div className="access-keys-panel">
                {filterCheckbox}
                <div><FormattedMessage {...messages.empty} /></div>
            </div>
        );
    }

    return (
        <div className="access-keys-panel">
            {filterCheckbox}
            <table className="table table-sm">
                <thead>
                    <tr>
                        <th><FormattedMessage {...messages.columnState} /></th>
                        <th><FormattedMessage {...messages.columnName} /></th>
                        <th>ID</th>
                        <th><FormattedMessage {...messages.columnCreated} /></th>
                        <th><FormattedMessage {...messages.columnExpires} /></th>
                        <th><FormattedMessage {...messages.columnLastUsed} /></th>
                        <th />
                    </tr>
                </thead>
                <tbody>
                    {sorted.map((k) => (
                        <tr key={k.id} style={k.state !== ApiKeyState.Active ? {opacity: 0.6} : undefined}>
                            <td>{stateMessage(k.state) ? <FormattedMessage {...stateMessage(k.state)} /> : k.state}</td>
                            <td>{k.name}</td>
                            <td><code>{k.keyId}</code></td>
                            <td>{formatDate(k.createDate)}</td>
                            <td>{formatDate(k.expireDate)}</td>
                            <td>{k.lastUsedDate ? formatDate(k.lastUsedDate) : <FormattedMessage {...messages.lastUsedNever} />}</td>
                            <td>
                                {k.state === ApiKeyState.Active && (
                                    <button className="btn btn-sm btn-link" onClick={() => handleRevoke(k)}>
                                        <FormattedMessage {...messages.revoke} />
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
