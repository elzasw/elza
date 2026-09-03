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
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { FluentDialogProvider } from 'components/shared/dialog/FluentModalDialog';
import { globalMessages } from 'components/shared/lang';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import * as perms from 'actions/user/Permission';
import { DisplaySettings } from './DisplaySettings';
import { ApiKeysSettings } from './ApiKeysSettings';
import { BrowserDataSettings } from './BrowserDataSettings';

enum UserSettingCategoryKey {
    Display = 'Display',
    ApiKeys = 'ApiKeys',
    BrowserData = 'BrowserData',
}

interface UserSettingCategoryConfig {
    key: UserSettingCategoryKey;
    permission?: keyof typeof perms;
}

const UserSettingCategory: Record<UserSettingCategoryKey, UserSettingCategoryConfig> = {
    [UserSettingCategoryKey.Display]: { key: UserSettingCategoryKey.Display },
    [UserSettingCategoryKey.ApiKeys]: { key: UserSettingCategoryKey.ApiKeys },
    [UserSettingCategoryKey.BrowserData]: { key: UserSettingCategoryKey.BrowserData },
};

const messages = defineMessages({
    title: {
        id: 'userSettings.title',
        defaultMessage: 'Nastavení uživatele',
    },
    categoryDisplay: {
        id: 'userSettings.category.Display',
        defaultMessage: 'Zobrazení',
    },
    categoryApiKeys: {
        id: 'userSettings.category.ApiKeys',
        defaultMessage: 'API Klíče',
    },
    categoryBrowserData: {
        id: 'userSettings.category.BrowserData',
        defaultMessage: 'Uložená data',
    },
});

const categoryMessages: Record<UserSettingCategoryKey, typeof messages.categoryDisplay> = {
    [UserSettingCategoryKey.Display]: messages.categoryDisplay,
    [UserSettingCategoryKey.ApiKeys]: messages.categoryApiKeys,
    [UserSettingCategoryKey.BrowserData]: messages.categoryBrowserData,
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

    const availableCategories = Object.values(UserSettingCategory).filter(
        ({ permission }) => !permission || hasOne(permission)
    );

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
                                <TabList
                                    className={styles.menu}
                                    vertical
                                    selectedValue={activeView}
                                    onTabSelect={(_event, data) => setActiveView(data.value as UserSettingCategoryKey)}
                                >
                                    {availableCategories.map(({ key }) => (
                                        <Tab key={key} value={key}>
                                            {formatMessage(categoryMessages[key])}
                                        </Tab>
                                    ))}
                                </TabList>
                                <div className={styles.view}>
                                    {activeView === UserSettingCategoryKey.Display && <DisplaySettings />}
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
