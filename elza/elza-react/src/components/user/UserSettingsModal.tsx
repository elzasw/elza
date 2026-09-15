import { useState } from 'react';
import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Tab,
    TabList,
    Text,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { FluentDialogProvider } from 'components/shared/dialog/FluentModalDialog';
import { globalMessages } from 'components/shared/lang';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import * as perms from 'actions/user/Permission';
import { DisplaySettings } from './DisplaySettings';
import { AccessKeysSettings } from './AccessKeysSettings';
import { ApiKeysSettings } from './ApiKeysSettings';
import { BrowserDataSettings } from './BrowserDataSettings';

enum UserSettingCategoryKey {
    Display = 'Display',
    AccessKeys = 'AccessKeys',
    ApiKeys = 'ApiKeys',
    BrowserData = 'BrowserData',
}

interface UserSettingCategoryConfig {
    key: UserSettingCategoryKey;
    permission?: keyof typeof perms;
}

const UserSettingCategory: Record<UserSettingCategoryKey, UserSettingCategoryConfig> = {
    [UserSettingCategoryKey.Display]: { key: UserSettingCategoryKey.Display },
    [UserSettingCategoryKey.AccessKeys]: { key: UserSettingCategoryKey.AccessKeys },
    [UserSettingCategoryKey.ApiKeys]: { key: UserSettingCategoryKey.ApiKeys },
    [UserSettingCategoryKey.BrowserData]: { key: UserSettingCategoryKey.BrowserData },
};

enum UserSettingGroupKey {
    General = 'General',
    ApiKeys = 'ApiKeys',
}

interface UserSettingGroupConfig {
    key: UserSettingGroupKey;
    categories: UserSettingCategoryKey[];
}

const UserSettingGroups: UserSettingGroupConfig[] = [
    {
        key: UserSettingGroupKey.General,
        categories: [UserSettingCategoryKey.Display, UserSettingCategoryKey.BrowserData],
    },
    {
        key: UserSettingGroupKey.ApiKeys,
        categories: [UserSettingCategoryKey.AccessKeys, UserSettingCategoryKey.ApiKeys],
    },
];

const messages = defineMessages({
    title: {
        id: 'userSettings.title',
        defaultMessage: 'Nastavení uživatele',
    },
    categoryDisplay: {
        id: 'userSettings.category.Display',
        defaultMessage: 'Zobrazení',
    },
    categoryAccessKeys: {
        id: 'userSettings.category.AccessKeys',
        defaultMessage: 'Elza',
    },
    categoryApiKeys: {
        id: 'userSettings.category.ApiKeys',
        defaultMessage: 'Externí systémy',
    },
    categoryBrowserData: {
        id: 'userSettings.category.BrowserData',
        defaultMessage: 'Uložená data',
    },
    groupGeneral: {
        id: 'userSettings.group.General',
        defaultMessage: 'Obecné',
    },
    groupApiKeys: {
        id: 'userSettings.group.ApiKeys',
        defaultMessage: 'API klíče',
    },
});

const categoryMessages: Record<UserSettingCategoryKey, typeof messages.categoryDisplay> = {
    [UserSettingCategoryKey.Display]: messages.categoryDisplay,
    [UserSettingCategoryKey.AccessKeys]: messages.categoryAccessKeys,
    [UserSettingCategoryKey.ApiKeys]: messages.categoryApiKeys,
    [UserSettingCategoryKey.BrowserData]: messages.categoryBrowserData,
};

const groupMessages: Record<UserSettingGroupKey, typeof messages.groupGeneral> = {
    [UserSettingGroupKey.General]: messages.groupGeneral,
    [UserSettingGroupKey.ApiKeys]: messages.groupApiKeys,
};

const useStyles = makeStyles({
    surface: {
        width: '840px',
        maxWidth: '95vw',
    },
    layout: {
        display: 'flex',
        gap: tokens.spacingHorizontalL,
        alignItems: 'flex-start',
    },
    menu: {
        flexShrink: 0,
        // Fixed rail: keeps the panel from shifting when the selected label turns semibold.
        width: '180px',
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    groupLabel: {
        display: 'block',
        color: tokens.colorNeutralForeground3,
        // Nudge matches the vertical Tab's own horizontal padding, so label and tab text align.
        paddingLeft: tokens.spacingHorizontalMNudge,
        paddingBottom: tokens.spacingVerticalXS,
    },
    view: {
        flexGrow: 1,
        minWidth: 0,
        // Height, not maxHeight: the dialog must keep its size when a shorter tab is selected.
        height: '40vh',
        minHeight: '600px',
        overflowY: 'auto',
        paddingLeft: tokens.spacingHorizontalL,
        borderLeftWidth: tokens.strokeWidthThin,
        borderLeftStyle: 'solid',
        borderLeftColor: tokens.colorNeutralStroke2,
    },
});

interface Props {
    open: boolean;
    onClose: () => void;
}

export type UserSettingsModalProps = Props;

export function UserSettingsModal({ open, onClose }: Props) {
    const { hasOne } = useAppSelector(({ userDetail }) => userDetail);
    const [activeView, setActiveView] = useState<UserSettingCategoryKey>(UserSettingCategoryKey.Display);
    const { formatMessage } = useIntl();
    const styles = useStyles();

    const availableGroups = UserSettingGroups.map(({ key, categories }) => ({
        key,
        categories: categories.filter((categoryKey) => {
            const { permission } = UserSettingCategory[categoryKey];
            const isAllowed = !permission || hasOne(permission);
            return isAllowed;
        }),
    })).filter(({ categories }) => categories.length > 0);

    return (
        <Dialog
            open={open}
            onOpenChange={(_event, data) => {
                if (!data.open) {
                    onClose();
                }
            }}
        >
            <DialogSurface className={styles.surface}>
                <DialogBody>
                    <DialogTitle>{formatMessage(messages.title)}</DialogTitle>
                    {/* The bootstrap modal stack renders outside the app-wide provider, so the
                        panels get their own to be able to open Fluent confirmations. */}
                    <FluentDialogProvider hidden={false}>
                        <DialogContent>
                            <div className={styles.layout}>
                                {/* One TabList per group: TabList has no group headings of its own,
                                    and only the list owning activeView renders a selected tab. */}
                                <div className={styles.menu}>
                                    {availableGroups.map(({ key: groupKey, categories }) => (
                                        <div key={groupKey}>
                                            <Text size={200} weight="semibold" className={styles.groupLabel}>
                                                {formatMessage(groupMessages[groupKey])}
                                            </Text>
                                            <TabList
                                                vertical
                                                // Without this every unselected tab also renders a
                                                // hidden semibold copy of its label in a second grid
                                                // column, so tab widths differ by label length.
                                                reserveSelectedTabSpace={false}
                                                selectedValue={activeView}
                                                onTabSelect={(_event, data) =>
                                                    setActiveView(data.value as UserSettingCategoryKey)
                                                }
                                            >
                                                {categories.map((categoryKey) => (
                                                    <Tab key={categoryKey} value={categoryKey}>
                                                        {formatMessage(categoryMessages[categoryKey])}
                                                    </Tab>
                                                ))}
                                            </TabList>
                                        </div>
                                    ))}
                                </div>
                                <div className={styles.view}>
                                    {activeView === UserSettingCategoryKey.Display && <DisplaySettings />}
                                    {activeView === UserSettingCategoryKey.AccessKeys && <AccessKeysSettings />}
                                    {activeView === UserSettingCategoryKey.ApiKeys && <ApiKeysSettings />}
                                    {activeView === UserSettingCategoryKey.BrowserData && <BrowserDataSettings />}
                                </div>
                            </div>
                        </DialogContent>
                    </FluentDialogProvider>
                    <DialogActions>
                        <Button appearance="secondary" onClick={onClose}>
                            <FormattedMessage {...globalMessages.close} />
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}
