import {
    Button,
    Card,
    Menu,
    MenuItem,
    MenuItemRadio,
    MenuList,
    MenuPopover,
    MenuTrigger,
    Spinner,
    Tag,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    AddRegular,
    ArrowSortRegular,
    ArrowUpRegular,
    ArrowDownRegular,
    GridRegular,
    ListRegular,
} from '@fluentui/react-icons';
import { Api } from 'api';
import { Ribbon } from 'components/index.jsx';
import { showConfirmDialog } from 'components/shared/dialog';
import { Institution, InstitutionType } from 'elza-api';
import { ReactNode, useCallback, useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import { useHistory, useParams } from 'react-router';
import * as perms from 'actions/user/Permission.jsx';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { useUserSettings } from 'contexts/user';
import { urlAdminInstitution } from '../../constants';
import { AdminLayout } from '../shared/layout/AdminLayout';
import { InstitutionDetail } from 'components/admin/institution/InstitutionDetail';

const NEW_ROUTE_ID = 'new';

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
    viewGrid: {
        id: 'admin.institution.view.grid',
        defaultMessage: 'Dlaždice',
    },
    viewList: {
        id: 'admin.institution.view.list',
        defaultMessage: 'Seznam',
    },
    sortName: {
        id: 'admin.institution.sort.name',
        defaultMessage: 'Název',
    },
    sortInternalCode: {
        id: 'admin.institution.sort.internalCode',
        defaultMessage: 'Interní kód',
    },
    sortLabel: {
        id: 'admin.institution.sort.label',
        defaultMessage: 'Řazení',
    },
});

type ViewMode = 'grid' | 'list';
type SortField = 'name' | 'internalCode';

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
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
    },
    viewMenuButton: {
        marginLeft: 'auto',
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
    tileChips: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: tokens.spacingHorizontalXS,
        marginTop: 'auto',
        paddingTop: tokens.spacingVerticalS,
    },
    list: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalXS,
    },
    listRow: {
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'baseline',
        columnGap: tokens.spacingHorizontalM,
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
    },
    listName: {
        fontWeight: tokens.fontWeightSemibold,
        flexGrow: 1,
    },
    listChips: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: tokens.spacingHorizontalXS,
    },
    tileName: {
        fontWeight: tokens.fontWeightSemibold,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
    },
    sortItem: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
        width: '100%',
    },
    sortItemIcon: {
        marginLeft: 'auto',
        display: 'flex',
        width: '16px',
    },
});

export function AdminInstitutionPage() {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    const isAdmin = userDetail.hasOne(perms.ADMIN);

    const history = useHistory();
    const { id: routeId } = useParams<{ id?: string }>();

    const { settings, update } = useUserSettings();
    const viewMode = settings.institutionViewMode ?? 'list';
    const setViewMode = (mode: ViewMode) => update({ institutionViewMode: mode });
    const sortField = settings.institutionSortField ?? 'name';
    const sortDir = settings.institutionSortDir ?? 'asc';

    const selectSort = (field: SortField) => {
        if (field === sortField) {
            update({ institutionSortDir: sortDir === 'asc' ? 'desc' : 'asc' });
        } else {
            update({ institutionSortField: field, institutionSortDir: 'asc' });
        }
    };

    const [institutions, setInstitutions] = useState<Institution[]>([]);
    const [types, setTypes] = useState<InstitutionType[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    const isCreating = routeId === NEW_ROUTE_ID;
    const editing = routeId != null && !isCreating
        ? institutions.find(institution => institution.id.toString() === routeId)
        : undefined;
    const showDetail = isCreating || editing != null;

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
        history.push(`${urlAdminInstitution()}/${NEW_ROUTE_ID}`);
    };

    const openEdit = (institution: Institution) => {
        history.push(urlAdminInstitution(institution.id));
    };

    const goBack = () => {
        history.push(urlAdminInstitution());
    };

    const handleSubmit = async (values: Institution) => {
        if (editing) {
            await Api.institution.instUpdate(editing.id, values);
        } else {
            await Api.institution.instCreate(values);
        }
        await reload();
        goBack();
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
        await reload();
        goBack();
    };

    const typeName = (institution: Institution) =>
        institution.institutionTypeId != null
            ? typesById.get(institution.institutionTypeId)?.name
            : undefined;

    const displayName = (institution: Institution) =>
        institution.name || institution.shortName || institution.internalCode;

    const sortKey = (institution: Institution) =>
        sortField === 'internalCode' ? institution.internalCode : displayName(institution);

    const sortItemContent = (field: SortField, label: ReactNode) => (
        <span className={styles.sortItem}>
            {label}
            <span className={styles.sortItemIcon}>
                {field === sortField &&
                    (sortDir === 'asc' ? <ArrowUpRegular /> : <ArrowDownRegular />)}
            </span>
        </span>
    );

    const sortedInstitutions = [...institutions].sort((a, b) => {
        const result = sortKey(a).localeCompare(sortKey(b), undefined, { sensitivity: 'base', numeric: true });
        return sortDir === 'desc' ? -result : result;
    });

    const renderChips = (institution: Institution, className: string) => {
        const type = typeName(institution);
        return (
            <div className={className}>
                <Tag size="small" appearance="brand">
                    {institution.internalCode}
                </Tag>
                {type && <Tag size="small">{type}</Tag>}
            </div>
        );
    };

    const listPanel = (
        <div className={styles.container}>
            <div className={styles.toolbar}>
                {isAdmin && (
                    <Button appearance="primary" icon={<AddRegular />} onClick={openCreate}>
                        <FormattedMessage {...messages.add} />
                    </Button>
                )}
                <Menu>
                    <MenuTrigger disableButtonEnhancement>
                        <Button
                            className={styles.viewMenuButton}
                            appearance="subtle"
                            icon={<ArrowSortRegular />}
                            aria-label={formatMessage(messages.sortLabel)}
                            title={formatMessage(messages.sortLabel)}
                        />
                    </MenuTrigger>
                    <MenuPopover>
                        <MenuList>
                            <MenuItem onClick={() => selectSort('name')}>
                                {sortItemContent('name', <FormattedMessage {...messages.sortName} />)}
                            </MenuItem>
                            <MenuItem onClick={() => selectSort('internalCode')}>
                                {sortItemContent('internalCode', <FormattedMessage {...messages.sortInternalCode} />)}
                            </MenuItem>
                        </MenuList>
                    </MenuPopover>
                </Menu>
                <Menu
                    checkedValues={{ view: [viewMode] }}
                    onCheckedValueChange={(_event, data) => setViewMode(data.checkedItems[0] as ViewMode)}
                >
                    <MenuTrigger disableButtonEnhancement>
                        <Button
                            appearance="subtle"
                            icon={viewMode === 'grid' ? <GridRegular /> : <ListRegular />}
                            aria-label={formatMessage(viewMode === 'grid' ? messages.viewGrid : messages.viewList)}
                            title={formatMessage(viewMode === 'grid' ? messages.viewGrid : messages.viewList)}
                        />
                    </MenuTrigger>
                    <MenuPopover>
                        <MenuList>
                            <MenuItemRadio name="view" value="list" icon={<ListRegular />}>
                                <FormattedMessage {...messages.viewList} />
                            </MenuItemRadio>
                            <MenuItemRadio name="view" value="grid" icon={<GridRegular />}>
                                <FormattedMessage {...messages.viewGrid} />
                            </MenuItemRadio>
                        </MenuList>
                    </MenuPopover>
                </Menu>
            </div>
            {isLoading ? (
                <Spinner />
            ) : institutions.length === 0 ? (
                <div className={styles.empty}>
                    <FormattedMessage {...messages.empty} />
                </div>
            ) : viewMode === 'grid' ? (
                <div className={styles.grid}>
                    {sortedInstitutions.map(institution => (
                        <Card
                            key={institution.id}
                            className={styles.tile}
                            onClick={() => openEdit(institution)}
                        >
                            <span className={styles.tileName}>{displayName(institution)}</span>
                            {renderChips(institution, styles.tileChips)}
                        </Card>
                    ))}
                </div>
            ) : (
                <div className={styles.list}>
                    {sortedInstitutions.map(institution => (
                        <Card
                            key={institution.id}
                            className={styles.listRow}
                            onClick={() => openEdit(institution)}
                        >
                            <span className={styles.listName}>{displayName(institution)}</span>
                            {renderChips(institution, styles.listChips)}
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );

    const centerPanel = showDetail ? (
        <div className={styles.container}>
            <InstitutionDetail
                key={routeId}
                institution={editing}
                types={types}
                canEdit={isAdmin}
                onSubmit={handleSubmit}
                onDelete={isAdmin ? handleDelete : undefined}
                onBack={goBack}
            />
        </div>
    ) : (
        listPanel
    );

    return <AdminLayout ribbon={<Ribbon />} centerPanel={centerPanel} />;
}

export default AdminInstitutionPage;
