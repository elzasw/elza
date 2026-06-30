import { makeStyles, tokens } from '@fluentui/react-components';

export const useTableStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        overflow: 'hidden',
    },
    tableWrapper: {
        flex: '1',
        overflowX: 'auto',
        overflowY: 'auto',
    },
    header: {
        fontWeight: '600',
    },
    actionCol: {
        width: '40px',
        minWidth: '40px',
    },
    tableRow: {
        cursor: 'pointer',
    },
});

export const useToolbarStyles = makeStyles({
    root: {
        display: 'flex',
        alignItems: 'center',
        padding: '4px 8px',
        gap: '8px',
    },
});

export const useDialogStyles = makeStyles({
    surface: {
        width: "800px",
        maxWidth: "95vw",
        maxHeight: "90vh",
    },
    body: {
        height: "100%",
        overflow: "hidden",
    },
    content: {
        display: "flex",
        flex: "1",
        gap: "0",
        overflow: "hidden",
        padding: "0",
    },
    left: {
        width: "280px",
        flexShrink: "0",
        borderRight: `1px solid ${tokens.colorNeutralStroke1}`,
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
    },
    list: {
        flex: "1",
        overflowY: "auto",
        padding: "4px",
    },
    listItem: {
        cursor: "pointer",
        borderRadius: tokens.borderRadiusMedium,
        padding: "6px 8px",
        display: "flex",
        alignItems: "center",
        ":hover": {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    listItemSelected: {
        backgroundColor: tokens.colorBrandBackground2,
        ":hover": {
            backgroundColor: tokens.colorBrandBackground2Hover,
        },
    },
    listItemName: {
        flex: "1",
    },
    listItemInactive: {
        color: tokens.colorNeutralForeground3,
        fontStyle: "italic",
    },
    inactiveIcon: {
        color: tokens.colorNeutralForeground3,
        flexShrink: "0",
        fontSize: tokens.fontSizeBase400,
        marginRight: tokens.spacingHorizontalXS,
    },
    addButton: {
        margin: "8px",
    },
    right: {
        flex: "1",
        overflow: "hidden",
        padding: "0 16px",
    },
    empty: {
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        height: "100%",
        color: tokens.colorNeutralForeground3,
    },
});

export const useDetailStyles = makeStyles({
    root: {
        display: "flex",
        flexDirection: "column",
        height: "100%",
        overflow: "hidden",
    },
    form: {
        flex: "1",
        overflowY: "auto",
        display: "flex",
        flexDirection: "column",
        gap: "16px",
        padding: "8px 4px",
    },
    section: {
        display: "flex",
        flexDirection: "column",
        gap: "8px",
    },
    filterRow: {
        display: "flex",
        alignItems: "center",
        gap: "8px",
    },
    sectionTitle: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase300,
        color: tokens.colorNeutralForeground2,
        marginBottom: "4px",
    },
    footer: {
        display: "flex",
        gap: "8px",
        paddingTop: "12px",
        // borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    dangerBtn: {
        ":hover": {
            backgroundColor: tokens.colorStatusDangerBackground3Hover,
            color: tokens.colorNeutralForegroundOnBrand,
        },
    },
});
