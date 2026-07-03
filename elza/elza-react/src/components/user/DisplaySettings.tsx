import { Col, Form, Row } from 'react-bootstrap';
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
});

export default function DisplaySettings() {
    const { settings, update } = useUserSettings();
    const { formatMessage } = useIntl();

    const experimentalFeaturesEnabled = !!settings.showExperimentalFeatures;

    return (
        <Row>
            <Col xs={12}>
                <div style={{ padding: '10px 0' }}>
                    {experimentalFeaturesEnabled && (
                        <Form.Group controlId="language" className="mb-3">
                            <Form.Label>{formatMessage(messages.language)}</Form.Label>
                            <Form.Select
                                value={settings.language ?? 'cs'}
                                onChange={(e) => update({ language: e.target.value as Language })}
                            >
                                {languageOptions.map(({ value, nativeName }) => (
                                    <option key={value} value={value}>
                                        {nativeName}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    )}
                    <Form.Check
                        type="checkbox"
                        id="darkMode"
                        label={formatMessage(messages.darkMode)}
                        checked={!!settings.darkMode}
                        onChange={(e) => update({ darkMode: e.target.checked })}
                    />
                    <Form.Check
                        type="checkbox"
                        id="showExperimentalFeatures"
                        label={formatMessage(messages.showExperimentalFeatures)}
                        checked={!!settings.showExperimentalFeatures}
                        onChange={(e) => update({ showExperimentalFeatures: e.target.checked })}
                    />
                    <Form.Check
                        type="checkbox"
                        id="showDebugInfo"
                        label={formatMessage(messages.showDebugInfo)}
                        checked={!!settings.showDebugInfo}
                        onChange={(e) => update({ showDebugInfo: e.target.checked })}
                    />
                </div>
            </Col>
        </Row>
    );
}
