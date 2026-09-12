import React from 'react';
import { NavLink, useRouteMatch } from 'react-router-dom';
import { FormattedMessage, defineMessages } from 'react-intl';
import { makeStyles, tokens, mergeClasses } from '@fluentui/react-components';
import { Icon } from 'components/shared';
import * as perms from 'actions/user/Permission.jsx';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import {
    URL_ADMIN_USER,
    URL_ADMIN_GROUP,
    URL_ADMIN_FUND,
    URL_ADMIN_INSTITUTION,
    URL_ADMIN_IMPORT,
} from '../../constants';

// Ids are kept exactly as the legacy catalog had them: renaming an id drops its
// translations on the next locale:merge, so migration must preserve them verbatim.
const messages = defineMessages({
    user: {
        id: 'ribbon.action.admin.user',
        defaultMessage: 'Uživatelé',
    },
    group: {
        id: 'ribbon.action.admin.group',
        defaultMessage: 'Skupiny',
    },
    fund: {
        id: 'ribbon.action.admin.fund',
        defaultMessage: 'Archivní soubory',
    },
    institution: {
        id: 'ribbon.action.admin.institution',
        defaultMessage: 'Instituce',
    },
    import: {
        id: 'admin.nav.import',
        defaultMessage: 'Import',
    },
    reports: {
        id: 'ribbon_action_admin_reports',
        defaultMessage: 'Přehledy',
    },
    packages: {
        id: 'ribbon.action.admin.packages',
        defaultMessage: 'Správa balíčků',
    },
    externalSystems: {
        id: 'ribbon.action.admin.externalSystems',
        defaultMessage: 'Externí systémy',
    },
    backgroundProcesses: {
        id: 'ribbon.action.admin.backgroundProcesses',
        defaultMessage: 'Úlohy na pozadí',
    },
    requestsQueue: {
        id: 'ribbon.action.admin.requestsQueue',
        defaultMessage: 'Fronta požadavků',
    },
    showLogs: {
        id: 'ribbon.action.admin.showLogs',
        defaultMessage: 'Zobrazení logu',
    },
});

const useStyles = makeStyles({
    nav: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'stretch',
        height: '100%',
        rowGap: tokens.spacingVerticalXXS,
        borderRightWidth: tokens.strokeWidthThin,
        borderRightStyle: 'solid',
        borderRightColor: tokens.colorNeutralStroke1,
    },
    tab: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
        paddingTop: tokens.spacingVerticalS,
        paddingBottom: tokens.spacingVerticalS,
        paddingLeft: tokens.spacingHorizontalM,
        paddingRight: tokens.spacingHorizontalL,
        whiteSpace: 'nowrap',
        color: tokens.colorNeutralForeground1,
        textDecorationLine: 'none',
        borderLeftWidth: tokens.strokeWidthThick,
        borderLeftStyle: 'solid',
        borderLeftColor: 'transparent',
        fontSize: tokens.fontSizeBase300,
        ':hover': {
            backgroundColor: tokens.colorNeutralBackground1Hover,
            color: tokens.colorNeutralForeground1,
            textDecorationLine: 'none',
        },
    },
    tabActive: {
        borderLeftColor: tokens.colorBrandStroke1,
        color: tokens.colorBrandForeground1,
    },
});

interface NavItem {
    to: string;
    glyph: string;
    label: React.ReactNode;
    visible: boolean;
}

export function AdminNav() {
    const styles = useStyles();
    const userDetail = useAppSelector(state => state.userDetail);

    const isSuperuser = userDetail.hasOne(perms.ADMIN);
    const administersUser =
        userDetail.hasOne(perms.GROUP_CONTROL_ENTITY, perms.USR_PERM) ||
        userDetail.hasOne(perms.USER_CONTROL_ENTITY, perms.USR_PERM);
    const administersGroup = userDetail.hasOne(perms.GROUP_CONTROL_ENTITY, perms.USR_PERM);
    const canSeeReports = userDetail.hasOne(perms.REPORT_ALL);

    const items: NavItem[] = [
        { to: URL_ADMIN_USER, glyph: 'fa-user', label: <FormattedMessage {...messages.user} />, visible: administersUser },
        { to: URL_ADMIN_GROUP, glyph: 'fa-group', label: <FormattedMessage {...messages.group} />, visible: administersGroup },
        { to: URL_ADMIN_FUND, glyph: 'fa-database', label: <FormattedMessage {...messages.fund} />, visible: administersGroup || administersUser },
        { to: URL_ADMIN_INSTITUTION, glyph: 'fa-university', label: <FormattedMessage {...messages.institution} />, visible: isSuperuser },
        { to: URL_ADMIN_IMPORT, glyph: 'fa-upload', label: <FormattedMessage {...messages.import} />, visible: isSuperuser },
        { to: '/admin/reports', glyph: 'fa-line-chart', label: <FormattedMessage {...messages.reports} />, visible: canSeeReports },
        { to: '/admin/packages', glyph: 'fa-archive', label: <FormattedMessage {...messages.packages} />, visible: isSuperuser },
        { to: '/admin/extSystem', glyph: 'fa-external-link', label: <FormattedMessage {...messages.externalSystems} />, visible: isSuperuser },
        { to: '/admin/backgroundProcesses', glyph: 'fa-list-alt', label: <FormattedMessage {...messages.backgroundProcesses} />, visible: isSuperuser },
        { to: '/admin/requestsQueue', glyph: 'fa-shopping-basket', label: <FormattedMessage {...messages.requestsQueue} />, visible: isSuperuser },
        { to: '/admin/logs', glyph: 'fa-file-text-o', label: <FormattedMessage {...messages.showLogs} />, visible: isSuperuser },
    ];

    return (
        <nav className={styles.nav}>
            {items
                .filter(item => item.visible)
                .map(item => (
                    <AdminNavTab key={item.to} item={item} styles={styles} />
                ))}
        </nav>
    );
}

interface AdminNavTabProps {
    item: NavItem;
    styles: ReturnType<typeof useStyles>;
}

function AdminNavTab({ item, styles }: AdminNavTabProps) {
    const match = useRouteMatch(item.to);
    const isActive = match != null;

    return (
        <NavLink to={item.to} className={mergeClasses(styles.tab, isActive && styles.tabActive)}>
            <Icon glyph={item.glyph} />
            <span>{item.label}</span>
        </NavLink>
    );
}

export type AdminNavProps = Record<string, never>;
