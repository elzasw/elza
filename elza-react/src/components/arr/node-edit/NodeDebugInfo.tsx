import { makeStyles, mergeClasses, Tooltip } from '@fluentui/react-components';
import {
    AddRegular,
    ArrowDownRegular,
    ArrowSortRegular,
    CircleFilled,
    CircleHalfFillRegular,
    CircleOffRegular,
    CircleRegular,
    DatabaseRegular,
    EyeOffRegular,
    KeyRegular,
    ProhibitedRegular,
} from '@fluentui/react-icons';
import { FormItemType, MandatoryType, NodeItem } from 'elza-api';
import { DescItemTypeRef } from 'typings/store/RefTables.types';
import { useUserSettings } from 'contexts/user';

const mandatoryTypeIndicator: Record<string, { icon: typeof CircleFilled; color: string; bg: string }> = {
    [MandatoryType.Required]: { icon: CircleFilled, color: 'var(--contrast-color-fg)', bg: 'var(--color-blue)' },
    [MandatoryType.Recommended]: {
        icon: CircleHalfFillRegular,
        color: 'var(--contrast-color-fg)',
        bg: 'var(--color-green)',
    },
    [MandatoryType.Possible]: { icon: CircleRegular, color: 'inherit', bg: 'var(--shade-2)' },
    [MandatoryType.Impossible]: { icon: CircleOffRegular, color: 'var(--contrast-color-fg)', bg: 'var(--color-red)' },
};

const useStyles = makeStyles({
    root: {
        display: 'inline-flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: '2px 4px',
        fontSize: '11px',
        fontFamily: 'monospace',
        lineHeight: '1.6em',
        flexSrink: 0,
    },
    pill: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        height: '20px',
        padding: '0 6px',
        borderRadius: '4px',
        background: 'var(--shade-2)',
        border: '1px solid var(--border-color)',
    },
    typePill: {
        padding: '4px',
        gap: '4px',
        overflow: 'hidden',
    },
    flagsPill: {
        background: 'var(--contrast-color)',
        color: 'var(--contrast-color-fg)',
    },
    typeSegment: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '3px',
        padding: '0 6px',
        height: '100%',
    },
    specSegment: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '3px',
        padding: '0 6px',
        height: '100%',
        borderLeft: '1px solid var(--border-color)',
    },
    infoItem: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '2px',
    },
    flagItem: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    icon: {
        fontSize: '14px',
    },
    iconMuted: {
        fontSize: '14px',
        color: 'var(--fg-color)',
    },
    iconSmall: {
        fontSize: '10px',
    },
});

interface MandatoryTypeIndicatorProps {
    type: MandatoryType;
    id?: string | number;
}

export function MandatoryTypeIndicator({ type, id }: MandatoryTypeIndicatorProps) {
    const styles = useStyles();
    const ti = mandatoryTypeIndicator[type];
    const Icon = ti?.icon;

    if (!ti || !Icon) {
        return null;
    }

    return (
        <span style={{ background: ti.bg, color: ti.color }} className={mergeClasses(styles.pill, styles.typePill)}>
            <Tooltip relationship="label" appearance="inverted" content={type}>
                    <Icon className={styles.iconSmall} />
            </Tooltip>
            {id != undefined && (
                <b>{id}</b>
            )}
        </span>
    );
}

interface DescItemTypeFlagsProps {
    repeatable?: boolean;
    undefinable?: boolean;
}

export function DescItemTypeFlags({ repeatable, undefinable }: DescItemTypeFlagsProps) {
    const styles = useStyles();

    if (!repeatable && !undefinable) {
        return null;
    }

    return (
        <span className={mergeClasses(styles.pill, styles.flagsPill)}>
            {repeatable && (
                <Tooltip relationship="label" appearance="inverted" content="Repeatable">
                    <span className={styles.flagItem}>
                        <AddRegular className={styles.icon} />
                    </span>
                </Tooltip>
            )}
            {undefinable && (
                <Tooltip relationship="label" appearance="inverted" content="Undefinable">
                    <span className={styles.flagItem}>
                        <EyeOffRegular className={styles.icon} />
                    </span>
                </Tooltip>
            )}
        </span>
    );
}

interface DescItemTypeDebugInfoProps {
    typeRef: DescItemTypeRef;
    typeForm?: FormItemType;
}

export function DescItemTypeDebugInfo({ typeRef, typeForm }: DescItemTypeDebugInfoProps) {
    const styles = useStyles();
    const { settings } = useUserSettings();

    if (!settings.showDebugInfo) {
        return null;
    }

    const type = (typeForm?.type ?? MandatoryType.Impossible) as MandatoryType;
    const id = typeForm?.itemTypeId ?? typeRef.id;
    const repeatable = typeForm?.repeatable ?? false;
    const undefinable = typeForm?.undefinable ?? false;

    return (
        <div className={styles.root} style={{margin: "0 4px"}}>
            <MandatoryTypeIndicator type={type} id={id} />
            <DescItemTypeFlags repeatable={repeatable} undefinable={undefinable} />
        </div>
    );
}

interface DescItemInfoProps {
    item: NodeItem;
    typeForm?: FormItemType;
    localId: string | number;
    nodeId: number;
}

export function DescItemInfo({ item, typeForm, localId, nodeId }: DescItemInfoProps) {
    const styles = useStyles();
    const { settings } = useUserSettings();

    if (!settings.showDebugInfo) {
        return null;
    }

    const spec = typeForm?.specs?.find(({ itemSpecId }) => itemSpecId === item.itemSpecId);

    return (
        <div className={styles.root}>
            {/* Spec pill */}
            {spec && <MandatoryTypeIndicator type={spec.type} id={item.itemSpecId} />}
            {/* Identity pill */}
            <span className={styles.pill}>
                <Tooltip relationship="label" appearance="inverted" content="Local ID">
                    <span className={styles.infoItem}>
                        <KeyRegular className={styles.iconMuted} />
                        {localId}
                    </span>
                </Tooltip>
                <Tooltip relationship="label" appearance="inverted" content="Position">
                    <span className={styles.infoItem}>
                        <ArrowSortRegular className={styles.iconMuted} />
                        {item.position}
                    </span>
                </Tooltip>
                {item.itemObjectId != undefined && (
                    <Tooltip relationship="label" appearance="inverted" content="Object ID">
                        <span className={styles.infoItem}>
                            <DatabaseRegular className={styles.iconMuted} />
                            {item.itemObjectId}
                        </span>
                    </Tooltip>
                )}
            </span>
            {/* Flags pill */}
            {(item.nodeId !== nodeId || item.inhibited) && (
                <span className={mergeClasses(styles.pill, styles.flagsPill)}>
                    {item.nodeId !== nodeId && (
                        <Tooltip relationship="label" appearance="inverted" content="Inherited">
                            <span className={styles.flagItem}>
                                <ArrowDownRegular className={styles.icon} />
                            </span>
                        </Tooltip>
                    )}
                    {item.inhibited && (
                        <Tooltip relationship="label" appearance="inverted" content="Inhibited">
                            <span className={styles.flagItem}>
                                <ProhibitedRegular className={styles.icon} />
                            </span>
                        </Tooltip>
                    )}
                </span>
            )}
        </div>
    );
}
