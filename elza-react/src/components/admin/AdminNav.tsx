import React from 'react';
import { NavLink, useRouteMatch } from 'react-router-dom';
import { FormattedMessage, defineMessages } from 'react-intl';
import { makeStyles, tokens, mergeClasses } from '@fluentui/react-components';
import { Icon, i18n } from 'components/shared';
import * as perms from 'actions/user/Permission.jsx';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import {
    URL_ADMIN_USER,
    URL_ADMIN_GROUP,
    URL_ADMIN_FUND,
    URL_ADMIN_INSTITUTION,
} from '../../constants';

const messages = defineMessages({
    reports: {
        id: 'ribbon_action_admin_reports',
        defaultMessage: 'Přehledy',
    },
    institution: {
        id: 'ribbon.action.admin.institution',
        defaultMessage: 'Instituce',
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
    label: string | React.ReactElement;
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
        { to: URL_ADMIN_USER, glyph: 'fa-user', label: i18n('ribbon.action.admin.user'), visible: administersUser },
        { to: URL_ADMIN_GROUP, glyph: 'fa-group', label: i18n('ribbon.action.admin.group'), visible: administersGroup },
        { to: URL_ADMIN_FUND, glyph: 'fa-database', label: i18n('ribbon.action.admin.fund'), visible: administersGroup || administersUser },
        { to: URL_ADMIN_INSTITUTION, glyph: 'fa-university', label: <FormattedMessage {...messages.institution} />, visible: isSuperuser },
        { to: '/admin/reports', glyph: 'fa-line-chart', label: <FormattedMessage {...messages.reports} />, visible: canSeeReports },
        { to: '/admin/packages', glyph: 'fa-archive', label: i18n('ribbon.action.admin.packages'), visible: isSuperuser },
        { to: '/admin/extSystem', glyph: 'fa-external-link', label: i18n('ribbon.action.admin.externalSystems'), visible: isSuperuser },
        { to: '/admin/backgroundProcesses', glyph: 'fa-list-alt', label: i18n('ribbon.action.admin.backgroundProcesses'), visible: isSuperuser },
        { to: '/admin/requestsQueue', glyph: 'fa-shopping-basket', label: i18n('ribbon.action.admin.requestsQueue'), visible: isSuperuser },
        { to: '/admin/logs', glyph: 'fa-file-text-o', label: i18n('ribbon.action.admin.showLogs'), visible: isSuperuser },
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
