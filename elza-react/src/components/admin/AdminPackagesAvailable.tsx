import { useCallback, useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import {
    Button,
    MessageBar,
    MessageBarActions,
    MessageBarBody,
    Table,
    TableBody,
    TableCell,
    TableHeader,
    TableHeaderCell,
    TableRow,
    tokens,
} from '@fluentui/react-components';

import { Api } from 'api';
import { AvailablePackage, AvailablePackageState, AvailablePackages } from 'elza-api';
import { showConfirmDialog } from 'components/shared/dialog';
import { addToastrInfo } from 'components/shared/toastr/ToastrActions.jsx';
import { useThunkDispatch } from 'utils/hooks';

// The column labels share their ids with the list of installed packages.
const messages = defineMessages({
    title: {
        id: 'admin.packages.available.title',
        defaultMessage: 'Balíčky v adresáři dpkg, které nejsou načteny',
    },
    empty: {
        id: 'admin.packages.available.empty',
        defaultMessage: 'Všechny balíčky z adresáře dpkg jsou načteny.',
    },
    code: { id: 'admin_packages_label_code', defaultMessage: 'Kód' },
    name: { id: 'admin_packages_label_name', defaultMessage: 'Název' },
    version: { id: 'admin_packages_label_version', defaultMessage: 'Verze' },
    description: {
        id: 'admin_packages_label_description',
        defaultMessage: 'Popis',
        description: 'Popis balicku',
    },
    dependencies: {
        id: 'admin.packages.available.dependencies',
        defaultMessage: 'Požadované balíčky (minimální verze)',
    },
    file: { id: 'admin.packages.available.file', defaultMessage: 'Soubor' },
    state: { id: 'admin.packages.available.state', defaultMessage: 'Při příštím startu' },
    action: { id: 'admin_packages_label_action', defaultMessage: 'Akce' },
    stateConfigured: {
        id: 'admin.packages.available.state.configured',
        defaultMessage: 'Načte se (podle konfigurace)',
    },
    stateMarked: {
        id: 'admin.packages.available.state.marked',
        defaultMessage: 'Načte se (označeno)',
    },
    stateNotLoaded: {
        id: 'admin.packages.available.state.notLoaded',
        defaultMessage: 'Nenačte se',
    },
    mark: { id: 'admin.packages.available.mark', defaultMessage: 'Načíst při příštím startu' },
    unmark: { id: 'admin.packages.available.unmark', defaultMessage: 'Zrušit označení' },
    restartMarked: {
        id: 'admin.packages.available.restart.marked',
        defaultMessage: 'Označené balíčky se načtou při příštím startu aplikace.',
    },
    restartImported: {
        id: 'admin.packages.available.restart.imported',
        defaultMessage:
            'Balíček byl naimportován za běhu aplikace; vyhledávací index zaregistruje jeho nové prvky až po restartu.',
    },
    restartManual: {
        id: 'admin.packages.available.restart.manual',
        defaultMessage: 'Restartujte aplikaci.',
    },
    restart: { id: 'admin.packages.available.restart', defaultMessage: 'Restartovat aplikaci' },
    restartConfirm: {
        id: 'admin.packages.available.restart.confirm',
        defaultMessage:
            'Aplikace se ukončí a správce služeb ji znovu spustí; všichni uživatelé budou odpojeni. Pokračovat?',
    },
    restartRequested: {
        id: 'admin.packages.available.restart.requested',
        defaultMessage: 'Restart aplikace byl spuštěn. Aplikace se za chvíli ukončí a znovu spustí.',
    },
});

const stateMessages = {
    [AvailablePackageState.Configured]: messages.stateConfigured,
    [AvailablePackageState.Marked]: messages.stateMarked,
    [AvailablePackageState.NotLoaded]: messages.stateNotLoaded,
};

/**
 * Packages of the dpkg directory that are not loaded: the administrator marks them for the import
 * at the next start, and restarts the application when the server offers it.
 */
export function AdminPackagesAvailable() {
    const intl = useIntl();
    const dispatch = useThunkDispatch();
    const [data, setData] = useState<AvailablePackages | null>(null);
    // an import or a delete of an installed package changes whether a restart is needed
    const installedDirty = useSelector(
        (state: { adminRegion: { packages: { dirty: boolean } } }) => state.adminRegion.packages.dirty,
    );

    const load = useCallback(async () => {
        const response = await Api.packages.packagesListAvailablePackages();
        setData(response.data);
    }, []);

    useEffect(() => {
        load();
    }, [load, installedDirty]);

    const handleMark = async (code: string) => {
        await Api.packages.packagesMarkPackage(code);
        await load();
    };

    const handleUnmark = async (code: string) => {
        await Api.packages.packagesUnmarkPackage(code);
        await load();
    };

    const handleRestart = async () => {
        const confirmed = await dispatch(showConfirmDialog(intl.formatMessage(messages.restartConfirm)));
        if (!confirmed) {
            return;
        }
        await Api.admin.adminRestart();
        dispatch(addToastrInfo(intl.formatMessage(messages.restart), intl.formatMessage(messages.restartRequested)));
    };

    if (!data) {
        return null;
    }

    const marked = data.items.some(item => item.state === AvailablePackageState.Marked);

    return (
        <div style={{ padding: tokens.spacingHorizontalM }}>
            {data.restartRequired && (
                <MessageBar intent="warning" layout="multiline">
                    <MessageBarBody>
                        <FormattedMessage {...(marked ? messages.restartMarked : messages.restartImported)} />{' '}
                        {!data.restartAvailable && <FormattedMessage {...messages.restartManual} />}
                    </MessageBarBody>
                    {data.restartAvailable && (
                        <MessageBarActions>
                            <Button onClick={handleRestart}>
                                <FormattedMessage {...messages.restart} />
                            </Button>
                        </MessageBarActions>
                    )}
                </MessageBar>
            )}
            <h3>
                <FormattedMessage {...messages.title} />
            </h3>
            {data.items.length === 0 ? (
                <p>
                    <FormattedMessage {...messages.empty} />
                </p>
            ) : (
                <Table size="small" aria-label={intl.formatMessage(messages.title)}>
                    <TableHeader>
                        <TableRow>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.code} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.name} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.version} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.description} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.dependencies} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.file} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.state} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.action} />
                            </TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {data.items.map(item => (
                            <AvailablePackageRow
                                key={item.code}
                                item={item}
                                onMark={handleMark}
                                onUnmark={handleUnmark}
                            />
                        ))}
                    </TableBody>
                </Table>
            )}
        </div>
    );
}

interface RowProps {
    item: AvailablePackage;
    onMark: (code: string) => void;
    onUnmark: (code: string) => void;
}

function AvailablePackageRow({ item, onMark, onUnmark }: RowProps) {
    const marked = item.state === AvailablePackageState.Marked;
    return (
        <TableRow>
            <TableCell>{item.code}</TableCell>
            <TableCell>{item.name}</TableCell>
            <TableCell>{item.version}</TableCell>
            <TableCell>{item.description}</TableCell>
            <TableCell>
                {item.dependencies.map(dependency => (
                    <span key={dependency.code}>
                        {dependency.code} ({dependency.minVersion})
                        <br />
                    </span>
                ))}
            </TableCell>
            <TableCell>{item.fileName}</TableCell>
            <TableCell>
                <FormattedMessage {...stateMessages[item.state]} />
            </TableCell>
            <TableCell>
                {marked ? (
                    <Button size="small" onClick={() => onUnmark(item.code)}>
                        <FormattedMessage {...messages.unmark} />
                    </Button>
                ) : (
                    <Button size="small" onClick={() => onMark(item.code)}>
                        <FormattedMessage {...messages.mark} />
                    </Button>
                )}
            </TableCell>
        </TableRow>
    );
}
