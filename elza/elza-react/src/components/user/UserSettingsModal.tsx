import React, { useState, useEffect, useCallback } from 'react';
import { Col, Form, Modal, Nav, Row } from 'react-bootstrap';
import { i18n, Autocomplete, FormInput, FormInputField, Icon } from 'components/shared';
import { Button } from 'components/ui';
import { Form as FinalForm, Field } from 'react-final-form';
import { Api } from 'api';
import { useSelector } from 'react-redux';
import { AppState, ApExternalSystemSimpleVO } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { apExtSystemListFetchIfNeeded } from 'actions/registry/apExtSystemList';
import { ExtSystemProperty } from 'elza-api';
import { showConfirmDialog } from 'components/shared/dialog';
// import { AP_EXT_SYSTEM_TYPE } from '../../constants';

enum UserSettingCategory {
    ApiKeys = 'ApiKeys',
}

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

interface Props {
    onClose: () => void;
}

export default function UserSettingsModal({ onClose }: Props) {
    // const externalSystems = useSelector((appState: AppState) => appState.app.apExtSystemList.rows.filter(({type}) => type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE));
    const externalSystems = useSelector((appState: AppState) => appState.app.apExtSystemList.rows);
    const userId = useSelector((appState: AppState) => appState.userDetail.id);
    const [activeView, setActiveView] = useState<string | null>(UserSettingCategory.ApiKeys);
    const [apiKeys, setApiKeys] = useState<ApiKeyValue[]>([]);
    const [availableExternalSystems, setAvailableExternalSystems] = useState<ApExternalSystemSimpleVO[]>([]);
    const dispatch = useThunkDispatch();

    const loadApiKeys = useCallback(() => {
        (async () => {
            const { data } = await Api.externalSystems.externalSystemAllProperties(undefined, userId || undefined);

            const usedExternalSystems = [...new Set(data.map((value) => value.extSystemId))];

            const newApiKeys: ApiKeyValue[] = usedExternalSystems.map((id) => {
                const apiKeyId = data.find(({ name, extSystemId, userId: _userId }) => {
                    return userId === _userId && name === "apiKeyId" && extSystemId === id
                })
                const apiKeyValue = data.find(({ name, extSystemId, userId: _userId }) => {
                    return userId === _userId && name === "apiKeyValue" && extSystemId === id
                })
                return {
                    id,
                    apiKeyId,
                    apiKeyValue,
                }
            })
            setAvailableExternalSystems(externalSystems.filter(({ id }) => id != undefined && usedExternalSystems.indexOf(id) === -1))
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
                name: "apiKeyValue",
                value: apiKeyValue,
                userId: userId || undefined,
                extSystemId: parseInt(externalSystemId.toString()),
            }, {
                name: "apiKeyId",
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
            const result = await dispatch(showConfirmDialog(i18n("userSettings.apiKeys.delete.confirm.message", extSystem?.name)))
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
                            {Object.entries(UserSettingCategory).map(([_key, value]) => {
                                return (
                                    <Nav.Item>
                                        <Nav.Link eventKey={value}>{i18n(`userSettings.category.${value}`)}</Nav.Link>
                                    </Nav.Item>
                                );
                            })}
                        </Nav>
                    </Col>
                    <Col sm={9} className="view">
                        {activeView === UserSettingCategory.ApiKeys && (
                            <Row key={UserSettingCategory.ApiKeys}>
                                <Col xs={12}>
                                    <div style={{ padding: "10px 0" }}>
                                        <div>
                                            {apiKeys.length === 0 && i18n("userSettings.apiKeys.noItems")}
                                            {apiKeys.map(({ apiKeyValue, apiKeyId, id }) => {
                                                const externalSystem = externalSystems.find(({ id: _id }) => _id === id)
                                                return <div style={{
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
                                                            <div style={{ marginRight: "10px" }}><b>{i18n("userSettings.apiKeys.item.id")}:</b> {apiKeyId?.value}</div>
                                                            <div><b>{i18n("userSettings.apiKeys.item.value")}:</b> {apiKeyValue?.value}</div>
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
                                                            as={'select'}
                                                            component={FormInputField}
                                                            label={i18n("ap.ext-syncs.ext-system")}
                                                            disabled={submitting}
                                                        >
                                                            <option />
                                                            {availableExternalSystems.map(({ id, name }) => {
                                                                return <option value={id}>{name}</option>
                                                            })}
                                                        </Field>
                                                        <Field
                                                            key={'apiKeyId'}
                                                            name="apiKeyId"
                                                            component={FormInputField}
                                                            label={i18n("admin.extSystem.apiKeyId")}
                                                        />
                                                        <Field
                                                            key={'apiKeyValue'}
                                                            name="apiKeyValue"
                                                            component={FormInputField}
                                                            label={i18n("admin.extSystem.apiKeyValue")}
                                                        />
                                                        <div style={{ display: "flex", justifyContent: "flex-end", padding: "10px 0" }}>
                                                            <Button variant="outline-secondary" onClick={handleSubmit}>{i18n("userSettings.apiKeys.save")}</Button>
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
                {/* <Button type="submit" variant="outline-secondary" > */}
                {/*     {i18n('visiblePolicy.action.save')} */}
                {/* </Button> */}
                <Button variant="link" onClick={onClose}>
                    {i18n('global.action.close')}
                </Button>
            </Modal.Footer>
        </Form>
    );
}
