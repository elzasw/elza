import { Button, Card, Spinner, makeStyles, tokens } from '@fluentui/react-components';
import { AddRegular } from '@fluentui/react-icons';
import { Api } from 'api';
import { Ribbon } from 'components/index.jsx';
import { showConfirmDialog } from 'components/shared/dialog';
import { Institution, InstitutionType } from 'elza-api';
import { useCallback, useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import * as perms from 'actions/user/Permission.jsx';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { AdminLayout } from '../shared/layout/AdminLayout';
import { InstitutionDialog } from 'components/admin/institution/InstitutionDialog';

const messages = defineMessages({
    add: {
        id: 'admin.institution.action.add',
        defaultMessage: 'Přidat instituci',
    },
    empty: {
        id: 'admin.institution.empty',
        defaultMessage: 'Žádné instituce',
    },
    deleteConfirm: {
        id: 'admin.institution.delete.confirm',
        defaultMessage: 'Opravdu chcete smazat tuto instituci?',
    },
    noType: {
        id: 'admin.institution.tile.noType',
        defaultMessage: 'Bez typu',
    },
});

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        padding: tokens.spacingVerticalL,
        rowGap: tokens.spacingVerticalM,
        overflow: 'auto',
    },
    toolbar: {
        display: 'flex',
    },
    grid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
        gap: tokens.spacingHorizontalM,
    },
    tile: {
        cursor: 'pointer',
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalXXS,
        padding: tokens.spacingVerticalM,
    },
    tileName: {
        fontWeight: tokens.fontWeightSemibold,
    },
    tileMeta: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase200,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
    },
});

export function AdminInstitutionPage() {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();
    const splitter = useSelector(({ splitter }: AppState) => splitter);
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    const isAdmin = userDetail.hasOne(perms.ADMIN);

    const [institutions, setInstitutions] = useState<Institution[]>([]);
    const [types, setTypes] = useState<InstitutionType[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [editing, setEditing] = useState<Institution>();

    const reload = useCallback(async () => {
        setIsLoading(true);
        try {
            const [{ data: institutionData }, { data: typeData }] = await Promise.all([
                Api.institution.instGetAll(),
                Api.institution.instGetTypes(),
            ]);
            setInstitutions(institutionData);
            setTypes(typeData);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        reload();
    }, [reload]);

    const typesById = new Map(types.map(type => [type.id, type]));

    const openCreate = () => {
        setEditing(undefined);
        setDialogOpen(true);
    };

    const openEdit = (institution: Institution) => {
        setEditing(institution);
        setDialogOpen(true);
    };

    const handleSubmit = async (values: Institution) => {
        if (editing) {
            await Api.institution.instUpdate(editing.id, values);
        } else {
            await Api.institution.instCreate(values);
        }
        setDialogOpen(false);
        await reload();
    };

    const handleDelete = async () => {
        if (!editing) {
            return;
        }
        const confirmed = await dispatch(showConfirmDialog(formatMessage(messages.deleteConfirm)));
        if (!confirmed) {
            return;
        }
        await Api.institution.instDelete(editing.id);
        setDialogOpen(false);
        await reload();
    };

    const centerPanel = (
        <div className={styles.container}>
            {isAdmin && (
                <div className={styles.toolbar}>
                    <Button appearance="primary" icon={<AddRegular />} onClick={openCreate}>
                        <FormattedMessage {...messages.add} />
                    </Button>
                </div>
            )}
            {isLoading ? (
                <Spinner />
            ) : institutions.length === 0 ? (
                <div className={styles.empty}>
                    <FormattedMessage {...messages.empty} />
                </div>
            ) : (
                <div className={styles.grid}>
                    {institutions.map(institution => {
                        const typeName =
                            institution.institutionTypeId != null
                                ? typesById.get(institution.institutionTypeId)?.name
                                : formatMessage(messages.noType);
                        return (
                            <Card
                                key={institution.id}
                                className={styles.tile}
                                onClick={() => openEdit(institution)}
                            >
                                <span className={styles.tileName}>
                                    {institution.name || institution.shortName || institution.internalCode}
                                </span>
                                <span className={styles.tileMeta}>{institution.internalCode}</span>
                                <span className={styles.tileMeta}>{typeName}</span>
                            </Card>
                        );
                    })}
                </div>
            )}
            <InstitutionDialog
                open={dialogOpen}
                institution={editing}
                types={types}
                onSubmit={handleSubmit}
                onDelete={isAdmin ? handleDelete : undefined}
                onClose={() => setDialogOpen(false)}
            />
        </div>
    );

    return <AdminLayout splitter={splitter} ribbon={<Ribbon />} centerPanel={centerPanel} />;
}

export default AdminInstitutionPage;
