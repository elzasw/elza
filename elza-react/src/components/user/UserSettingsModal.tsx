import React, { useState } from 'react';
import { Col, Form, Modal, Nav, Row } from 'react-bootstrap';
import { globalMessages } from 'components/shared/lang';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { Button } from 'components/ui';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import * as perms from 'actions/user/Permission';
import DisplaySettings from './DisplaySettings';
import ApiKeysSettings from './ApiKeysSettings';

enum UserSettingCategoryKey {
    Display = 'Display',
    ApiKeys = 'ApiKeys',
}

interface UserSettingCategoryConfig {
    key: UserSettingCategoryKey;
    permission?: keyof typeof perms;
}

const UserSettingCategory: Record<UserSettingCategoryKey, UserSettingCategoryConfig> = {
    [UserSettingCategoryKey.Display]: { key: UserSettingCategoryKey.Display },
    [UserSettingCategoryKey.ApiKeys]: { key: UserSettingCategoryKey.ApiKeys, permission: perms.AP_EXTERNAL_WR },
};

const messages = defineMessages({
    categoryDisplay: {
        id: 'userSettings.category.Display',
        defaultMessage: 'Zobrazení',
    },
    categoryApiKeys: {
        id: 'userSettings.category.ApiKeys',
        defaultMessage: 'API Klíče',
    },
});

const categoryMessages: Record<UserSettingCategoryKey, typeof messages.categoryDisplay> = {
    [UserSettingCategoryKey.Display]: messages.categoryDisplay,
    [UserSettingCategoryKey.ApiKeys]: messages.categoryApiKeys,
};

interface Props {
    onClose: () => void;
}

export default function UserSettingsModal({ onClose }: Props) {
    const { hasOne } = useSelector((appState: AppState) => appState.userDetail);
    const [activeView, setActiveView] = useState<UserSettingCategoryKey | null>(UserSettingCategoryKey.Display);
    const { formatMessage } = useIntl();

    return (
        <Form className="node-settings-form">
            <Modal.Body>
                <Row>
                    <Col sm={3} className="menu">
                        <Nav variant="pills" activeKey={activeView} onSelect={view => setActiveView(view as UserSettingCategoryKey | null)}>
                            {Object.values(UserSettingCategory)
                                .filter(({ permission }) => !permission || hasOne(permission))
                                .map(({ key }) => (
                                    <Nav.Item key={key}>
                                        <Nav.Link eventKey={key}>{formatMessage(categoryMessages[key])}</Nav.Link>
                                    </Nav.Item>
                                ))}
                        </Nav>
                    </Col>
                    <Col sm={9} className="view">
                        {activeView === UserSettingCategoryKey.Display && <DisplaySettings />}
                        {activeView === UserSettingCategoryKey.ApiKeys && <ApiKeysSettings />}
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...globalMessages.close} />
                </Button>
            </Modal.Footer>
        </Form>
    );
}
