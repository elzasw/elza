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
    errorLink: {
        maxWidth: '100%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        textAlign: 'start',
        color: tokens.colorPaletteRedForeground1,
    },
    errorTooltip: {
        maxWidth: '600px',
        whiteSpace: 'pre-wrap',
        overflowWrap: 'anywhere',
    },
    errorTooltipHint: {
        marginTop: tokens.spacingVerticalXS,
        fontStyle: 'italic',
        opacity: '0.8',
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

export const useDetailStyles = makeStyles({
    root: {
        display: "flex",
        flexDirection: "column",
    },
    form: {
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
    hint: {
        display: "flex",
        alignItems: "center",
        gap: tokens.spacingHorizontalXS,
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
