import { Col, Form, Row } from 'react-bootstrap';
import { defineMessages, useIntl } from 'react-intl';
import { useUserSettings } from 'contexts/user';

const messages = defineMessages({
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

    return (
        <Row>
            <Col xs={12}>
                <div style={{ padding: '10px 0' }}>
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
