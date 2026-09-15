import { Field, Select, Switch, makeStyles, tokens } from '@fluentui/react-components';
import { defineMessages, useIntl } from 'react-intl';
import { Language, useUserSettings } from 'contexts/user';

const languageOptions: Array<{ value: Language; nativeName: string }> = [
    { value: 'cs', nativeName: 'Čeština' },
    { value: 'en', nativeName: 'English' },
];

const messages = defineMessages({
    language: {
        id: 'userSettings.display.language',
        defaultMessage: 'Jazyk',
    },
    darkMode: {
        id: 'userSettings.display.darkMode',
        defaultMessage: 'Tmavý režim',
    },
    showDebugInfo: {
        id: 'userSettings.display.showDebugInfo',
        defaultMessage: 'Zobrazit ladící informace',
    },
    showExperimentalFeatures: {
        id: 'userSettings.display.showExperimentalFeatures',
        defaultMessage: 'Zobrazit experimentální funkce',
    },
    outputColumnLayout: {
        id: 'userSettings.display.outputColumnLayout',
        defaultMessage: 'Sloupcové rozvržení detailu výstupu',
    },
});

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
        paddingTop: tokens.spacingVerticalM,
        paddingBottom: tokens.spacingVerticalM,
    },
    languageField: {
        maxWidth: '260px',
    },
});

export function DisplaySettings() {
    const { settings, update } = useUserSettings();
    const { formatMessage } = useIntl();
    const styles = useStyles();

    const experimentalFeaturesEnabled = !!settings.showExperimentalFeatures;

    return (
        <div className={styles.root}>
            {experimentalFeaturesEnabled && (
                <Field label={formatMessage(messages.language)} className={styles.languageField}>
                    <Select
                        value={settings.language ?? 'cs'}
                        onChange={(_event, data) => update({ language: data.value as Language })}
                    >
                        {languageOptions.map(({ value, nativeName }) => (
                            <option key={value} value={value}>
                                {nativeName}
                            </option>
                        ))}
                    </Select>
                </Field>
            )}
            <Switch
                label={formatMessage(messages.darkMode)}
                checked={!!settings.darkMode}
                onChange={(_event, data) => update({ darkMode: data.checked })}
            />
            <Switch
                label={formatMessage(messages.outputColumnLayout)}
                checked={!!settings.outputColumnLayout}
                onChange={(_event, data) => update({ outputColumnLayout: data.checked })}
            />
            <Switch
                label={formatMessage(messages.showExperimentalFeatures)}
                checked={experimentalFeaturesEnabled}
                onChange={(_event, data) => update({ showExperimentalFeatures: data.checked })}
            />
            <Switch
                label={formatMessage(messages.showDebugInfo)}
                checked={!!settings.showDebugInfo}
                onChange={(_event, data) => update({ showDebugInfo: data.checked })}
            />
        </div>
    );
}
