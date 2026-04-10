import React, { useState, useEffect, useCallback } from 'react';
import { Col, Form, Modal, Nav, Row } from 'react-bootstrap';
import { FormInputField, Icon } from 'components/shared';
import { globalMessages } from 'components/shared/lang';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { Button } from 'components/ui';
import { Form as FinalForm, Field } from 'react-final-form';
import { Api } from 'api';
import { useSelector } from 'react-redux';
import { AppState, ApExternalSystemSimpleVO } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { apExtSystemListFetchIfNeeded } from 'actions/registry/apExtSystemList';
import { ExtSystemProperty } from 'elza-api';
import { showConfirmDialog } from 'components/shared/dialog';
import { useUserSettings } from 'contexts/user';
// import { AP_EXT_SYSTEM_TYPE } from '../../constants';

enum UserSettingCategory {
    Display = 'Display',
    ApiKeys = 'ApiKeys',
}

const APIKEY_ID = "apiKeyId";
const APIKEY_VALUE = "apiKeyValue";

interface ApiKeyValueFields {
    externalSystemId?: string | number;
    apiKeyId: string;
    apiKeyValue: string;
}

interface ApiKeyValue {
    id?: number | string;
    apiKeyId?: ExtSystemProperty;
    apiKeyValue?: ExtSystemProperty;
}

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
    categoryDisplay: {
        id: 'userSettings.category.Display',
        defaultMessage: 'Zobrazení',
    },
    categoryApiKeys: {
        id: 'userSettings.category.ApiKeys',
        defaultMessage: 'API Klíče',
    },
    apiKeysNoItems: {
        id: 'userSettings.apiKeys.noItems',
        defaultMessage: 'Žádné uložené osobní API klíče',
    },
    apiKeysItemId: {
        id: 'userSettings.apiKeys.item.id',
        defaultMessage: 'id',
    },
    apiKeysItemValue: {
        id: 'userSettings.apiKeys.item.value',
        defaultMessage: 'hodnota',
    },
    apiKeysSave: {
        id: 'userSettings.apiKeys.save',
        defaultMessage: 'Uložit',
    },
    apiKeysDeleteConfirm: {
        id: 'userSettings.apiKeys.delete.confirm.message',
        defaultMessage: 'Přejete si smazat nastavený API klíč pro externí systém {name}?',
    },
    extSystem: {
        id: 'ap.ext-syncs.ext-system',
        defaultMessage: 'Externí systém',
    },
    apiKeyId: {
        id: 'admin.extSystem.apiKeyId',
        defaultMessage: 'ApiKey - ID',
    },
    apiKeyValue: {
        id: 'admin.extSystem.apiKeyValue',
        defaultMessage: 'ApiKey - hodnota',
    },
});

const categoryMessages: Record<UserSettingCategory, typeof messages.categoryDisplay> = {
    [UserSettingCategory.Display]: messages.categoryDisplay,
    [UserSettingCategory.ApiKeys]: messages.categoryApiKeys,
};

function DisplaySettings() {
    const { settings, update } = useUserSettings();
    const { formatMessage } = useIntl();

    return (
        <Row>
            <Col xs={12}>
                <div style={{ padding: "10px 0" }}>
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

interface Props {
    onClose: () => void;
}

export default function UserSettingsModal({ onClose }: Props) {
    // const externalSystems = useSelector((appState: AppState) => appState.app.apExtSystemList.rows.filter(({type}) => type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE));
    const externalSystems = useSelector((appState: AppState) => appState.app.apExtSystemList.rows);
    const userId = useSelector((appState: AppState) => appState.userDetail.id);
    const [activeView, setActiveView] = useState<string | null>(UserSettingCategory.Display);
    const [apiKeys, setApiKeys] = useState<ApiKeyValue[]>([]);
    const [availableExternalSystems, setAvailableExternalSystems] = useState<ApExternalSystemSimpleVO[]>([]);
    const dispatch = useThunkDispatch();
    const { formatMessage } = useIntl();

    const loadApiKeys = useCallback(() => {
        (async () => {
            const { data } = await Api.externalSystems.externalSystemAllProperties(undefined, userId || undefined);

            // prepare map of properties for each ext system
            const extsysMap = new Map<number, ApiKeyValue>();
            data.forEach(prop => {
                if(userId !== prop.userId) {
                    return;
                }
                // check if known property type
                var keyValueObj = extsysMap.get(prop.extSystemId);
                if(prop.name===APIKEY_ID) {
                    if(keyValueObj === undefined) {
                        keyValueObj = { id: prop.extSystemId, apiKeyId: prop };
                        extsysMap.set(prop.extSystemId, keyValueObj);
                    } else {
                        keyValueObj.apiKeyId = prop;
                    }
                } else
                if(prop.name===APIKEY_VALUE) {
                    if(keyValueObj === undefined) {
                        keyValueObj = { id: prop.extSystemId, apiKeyValue: prop };
                        extsysMap.set(prop.extSystemId, keyValueObj);
                    } else {
                        keyValueObj.apiKeyValue = prop;
                    }
                }
            });

            const newApiKeys: ApiKeyValue[] = [ ...extsysMap.values() ];

            setAvailableExternalSystems(externalSystems.filter(({ id }) => id != undefined && !extsysMap.has(id) ))
            setApiKeys(newApiKeys);
        })()
    }, [userId, externalSystems])

    useEffect(() => {
        dispatch(apExtSystemListFetchIfNeeded());
    }, [dispatch])

    useEffect(() => {
        (async function() {
            loadApiKeys()
        })()
    }, [externalSystems, loadApiKeys])

    const handleSubmit = async ({ externalSystemId, apiKeyId, apiKeyValue }: ApiKeyValueFields) => {
        if (externalSystemId && parseInt(externalSystemId.toString())) {
            await Api.externalSystems.externalSystemStoreProperties([{
                name: APIKEY_VALUE,
                value: apiKeyValue,
                userId: userId || undefined,
                extSystemId: parseInt(externalSystemId.toString()),
            }, {
                name: APIKEY_ID,
                value: apiKeyId,
                userId: userId || undefined,
                extSystemId: parseInt(externalSystemId.toString()),
            }])
            loadApiKeys();
        }
    };

    const handleDelete = (extSystemId?: string | number) => {
        return async () => {
            const extSystem = externalSystems.find(({ id }) => extSystemId === id);
            const result = await dispatch(showConfirmDialog(formatMessage(messages.apiKeysDeleteConfirm, { name: extSystem?.name })))
            if (!result) { return; }

            const apiKey = apiKeys.find(({ id }) => id === extSystemId)
            const idsToDelete: number[] = [];

            if (apiKey?.apiKeyId && apiKey.apiKeyId.id != undefined) {
                idsToDelete.push(apiKey.apiKeyId.id);
            }
            if (apiKey?.apiKeyValue && apiKey.apiKeyValue.id != undefined) {
                idsToDelete.push(apiKey.apiKeyValue.id);
            }
            if (idsToDelete.length > 0) {
                await Api.externalSystems.externalSystemDeleteProperties(idsToDelete);
                loadApiKeys();
            }
        }
    }

    return (
        <Form className="node-settings-form">
            <Modal.Body>
                <Row>
                    <Col sm={3} className="menu">
                        <Nav variant="pills" activeKey={activeView} onSelect={view => setActiveView(view)}>
                            {Object.values(UserSettingCategory).map((value) => {
                                return (
                                    <Nav.Item key={value}>
                                        <Nav.Link eventKey={value}>{formatMessage(categoryMessages[value])}</Nav.Link>
                                    </Nav.Item>
                                );
                            })}
                        </Nav>
                    </Col>
                    <Col sm={9} className="view">
                        {activeView === UserSettingCategory.Display && (
                            <DisplaySettings />
                        )}
                        {activeView === UserSettingCategory.ApiKeys && (
                            <Row key={UserSettingCategory.ApiKeys}>
                                <Col xs={12}>
                                    <div style={{ padding: "10px 0" }}>
                                        <div>
                                            {apiKeys.length === 0 && <FormattedMessage {...messages.apiKeysNoItems} />}
                                            {apiKeys.map(({ apiKeyValue, apiKeyId, id }) => {
                                                const externalSystem = externalSystems.find(({ id: _id }) => _id === id)
                                                return <div key={String(id)} style={{
                                                    border: "var(--primary-border)",
                                                    padding: "10px",
                                                    borderRadius: "10px",
                                                    display: "flex",
                                                    alignItems: "center",
                                                    margin: "5px 0"
                                                }}>
                                                    <div style={{ flexGrow: 1 }}>
                                                        <div><b>{externalSystem?.name}</b></div>
                                                        <div style={{ display: "flex", flexWrap: "wrap" }}>
                                                            <div style={{ marginRight: "10px" }}><b><FormattedMessage {...messages.apiKeysItemId} />:</b> {apiKeyId?.value}</div>
                                                            <div><b><FormattedMessage {...messages.apiKeysItemValue} />:</b> {apiKeyValue?.value}</div>
                                                        </div>
                                                    </div>
                                                    <div style={{ padding: "5px" }}>
                                                        <Button onClick={handleDelete(id)}><Icon glyph="fa-trash" /></Button>
                                                    </div>
                                                </div>
                                            })}
                                        </div>
                                        {availableExternalSystems.length > 0 && <FinalForm<ApiKeyValueFields>
                                            onSubmit={handleSubmit}
                                            initialValues={{ externalSystemId: availableExternalSystems.length === 1 ? availableExternalSystems[0].id : undefined }}
                                        >
                                            {({ handleSubmit, submitting }) => {
                                                return (
                                                    <>
                                                        <Field
                                                            key={'externalSystemId'}
                                                            name="externalSystemId"
                                                            type={'select'}
                                                            component={FormInputField}
                                                            label={formatMessage(messages.extSystem)}
                                                            disabled={submitting}
                                                        >
                                                            <option />
                                                            {availableExternalSystems.map(({ id, name }) => {
                                                                return <option value={id} key={id}>{name}</option>
                                                            })}
                                                        </Field>
                                                        <Field
                                                            key={'apiKeyId'}
                                                            name="apiKeyId"
                                                            component={FormInputField}
                                                            label={formatMessage(messages.apiKeyId)}
                                                        />
                                                        <Field
                                                            key={'apiKeyValue'}
                                                            name="apiKeyValue"
                                                            component={FormInputField}
                                                            label={formatMessage(messages.apiKeyValue)}
                                                        />
                                                        <div style={{ display: "flex", justifyContent: "flex-end", padding: "10px 0" }}>
                                                            <Button variant="outline-secondary" onClick={handleSubmit}><FormattedMessage {...messages.apiKeysSave} /></Button>
                                                        </div>
                                                    </>
                                                );
                                            }}
                                        </FinalForm>}
                                    </div>
                                </Col>
                            </Row>
                        )}
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
