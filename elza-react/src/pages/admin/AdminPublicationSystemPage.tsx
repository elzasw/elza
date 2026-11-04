import {
    Button,
    Card,
    Spinner,
    Tag,
    Title3,
    Toolbar,
    ToolbarButton,
    makeStyles,
    mergeClasses,
    tagClassNames,
    tokens,
} from '@fluentui/react-components';
import { AddRegular, ArrowLeftRegular, ChevronRightRegular, EyeOffRegular } from '@fluentui/react-icons';
import { Api } from 'api';
import { Ribbon } from 'components/index.jsx';
import { PublicationSystemDetail } from 'components/arr/publication/PublicationSystemDetail';
import { connectionTypeMessages } from 'components/arr/publication/messages';
import { ConnectionType, PublicationType } from 'elza-api';
import { useCallback, useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { useHistory, useParams } from 'react-router';
import { urlAdminPublicationSystem } from '../../constants';
import { AdminLayout } from '../shared/layout/AdminLayout';

const NEW_ROUTE_ID = 'new';

const messages = defineMessages({
    title: {
        id: 'admin.publicationSystem.title',
        defaultMessage: 'Publikační systémy',
    },
    add: {
        id: 'admin.publicationSystem.action.add',
        defaultMessage: 'Přidat publikační systém',
    },
    empty: {
        id: 'admin.publicationSystem.empty',
        defaultMessage: 'Žádné publikační systémy',
    },
    inactive: {
        id: 'admin.publicationSystem.inactive',
        defaultMessage: 'Neaktivní',
    },
    newSystem: {
        id: 'publication.systems.new',
        defaultMessage: 'Nový publikační systém',
    },
    back: {
        id: 'admin.institution.detail.back',
        defaultMessage: 'Zpět',
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
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
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
    inactiveName: {
        color: tokens.colorNeutralForeground3,
    },
    inactiveTag: {
        backgroundColor: tokens.colorStatusDangerBackground1,
        color: tokens.colorStatusDangerForeground1,
        borderTopColor: tokens.colorStatusDangerBorder1,
        borderRightColor: tokens.colorStatusDangerBorder1,
        borderBottomColor: tokens.colorStatusDangerBorder1,
        borderLeftColor: tokens.colorStatusDangerBorder1,
        [`& .${tagClassNames.icon}`]: {
            color: tokens.colorStatusDangerForeground1,
        },
    },
    listChips: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: tokens.spacingHorizontalXS,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalS,
    },
    crumb: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalXS,
        minWidth: 0,
    },
    crumbRoot: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase300,
    },
    crumbSeparator: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase300,
        display: 'flex',
    },
    title: {
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    form: {
        maxWidth: '480px',
        padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalXXL}`,
    },
});

function createEmptySystem(): PublicationType {
    return {
        name: '',
        code: '',
        retentionCount: 5,
        exportFilterCode: undefined,
        active: true,
        allowPermExport: false,
        allowPermPublication: false,
        connectionType: ConnectionType.Development,
    };
}

function isActive(system: PublicationType) {
    return system.active ?? true;
}

export function AdminPublicationSystemPage() {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const history = useHistory();
    const { id: routeId } = useParams<{ id?: string }>();

    const [systems, setSystems] = useState<PublicationType[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [newSystem, setNewSystem] = useState<PublicationType>(createEmptySystem);

    const isCreating = routeId === NEW_ROUTE_ID;
    const editing = routeId != null && !isCreating
        ? systems.find(system => system.id?.toString() === routeId)
        : undefined;
    const detailValue = isCreating ? newSystem : editing;

    const reload = useCallback(async () => {
        setIsLoading(true);
        try {
            const { data } = await Api.publication.publicationTypeAdminListPublicationTypes();
            setSystems(data);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        reload();
    }, [reload]);

    const goBack = () => {
        history.push(urlAdminPublicationSystem());
    };

    const openCreate = () => {
        setNewSystem(createEmptySystem());
        history.push(urlAdminPublicationSystem(NEW_ROUTE_ID));
    };

    const openEdit = (system: PublicationType) => {
        history.push(urlAdminPublicationSystem(system.id));
    };

    const handleSave = async (values: PublicationType) => {
        if (values.id === undefined) {
            await Api.publication.publicationTypeAdminCreatePublicationType(values);
        } else {
            await Api.publication.publicationTypeAdminUpdatePublicationType(values.id, values);
        }
        await reload();
        goBack();
    };

    const handleToggleActive = async () => {
        if (!detailValue) {
            return;
        }
        const toggled = { ...detailValue, active: !isActive(detailValue) };
        if (toggled.id === undefined) {
            setNewSystem(toggled);
            return;
        }
        await Api.publication.publicationTypeAdminUpdatePublicationType(toggled.id, toggled);
        await reload();
    };

    const handleRemove = async () => {
        if (detailValue?.id !== undefined) {
            await Api.publication.publicationTypeAdminDeletePublicationType(detailValue.id);
            await reload();
        }
        goBack();
    };

    const sortedSystems = [...systems].sort((a, b) =>
        (a.name ?? '').localeCompare(b.name ?? '', undefined, { sensitivity: 'base', numeric: true }),
    );

    const listPanel = (
        <div className={styles.container}>
            <div className={styles.toolbar}>
                <Button appearance="primary" icon={<AddRegular />} onClick={openCreate}>
                    <FormattedMessage {...messages.add} />
                </Button>
            </div>
            {isLoading ? (
                <Spinner />
            ) : systems.length === 0 ? (
                <div className={styles.empty}>
                    <FormattedMessage {...messages.empty} />
                </div>
            ) : (
                <div className={styles.list}>
                    {sortedSystems.map(system => {
                        const isInactive = !isActive(system);
                        return (
                            <Card key={system.id} className={styles.listRow} onClick={() => openEdit(system)}>
                                <span className={mergeClasses(styles.listName, isInactive && styles.inactiveName)}>
                                    {system.name}
                                </span>
                                <div className={styles.listChips}>
                                    {isInactive && (
                                        <Tag size="small" icon={<EyeOffRegular />} className={styles.inactiveTag}>
                                            <FormattedMessage {...messages.inactive} />
                                        </Tag>
                                    )}
                                    <Tag size="small" appearance="brand">
                                        {system.code}
                                    </Tag>
                                    {system.connectionType && (
                                        <Tag size="small">
                                            <FormattedMessage {...connectionTypeMessages[system.connectionType]} />
                                        </Tag>
                                    )}
                                </div>
                            </Card>
                        );
                    })}
                </div>
            )}
        </div>
    );

    const detailTitle = isCreating ? formatMessage(messages.newSystem) : editing?.name || editing?.code;

    const centerPanel = detailValue ? (
        <div className={styles.container}>
            <Toolbar className={styles.header}>
                <ToolbarButton
                    icon={<ArrowLeftRegular />}
                    onClick={goBack}
                    aria-label={formatMessage(messages.back)}
                />
                <div className={styles.crumb}>
                    <span className={styles.crumbRoot}>
                        <FormattedMessage {...messages.title} />
                    </span>
                    <ChevronRightRegular className={styles.crumbSeparator} />
                    <Title3 className={styles.title}>{detailTitle}</Title3>
                </div>
            </Toolbar>
            <div className={styles.form}>
                <PublicationSystemDetail
                    key={routeId}
                    value={detailValue}
                    onSave={handleSave}
                    onToggleActive={handleToggleActive}
                    onRemove={handleRemove}
                />
            </div>
        </div>
    ) : (
        listPanel
    );

    return <AdminLayout ribbon={<Ribbon />} centerPanel={centerPanel} />;
}
