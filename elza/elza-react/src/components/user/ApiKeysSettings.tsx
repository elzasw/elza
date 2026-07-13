import { useState, useEffect, useCallback, useMemo } from 'react';
import { Col, Row } from 'react-bootstrap';
import { FormInputField, Icon } from 'components/shared';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { Button } from 'components/ui';
import { Form as FinalForm, Field } from 'react-final-form';
import { Api } from 'api';
import { useSelector } from 'react-redux';
import { AppState, RefExternalSystemSimpleVO } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { ExtSystemProperty } from 'elza-api';
import { showConfirmDialog } from 'components/shared/dialog';
import { refExternalSystemsFetchIfNeeded } from 'actions/refTables/externalSystems';
import { usePermissions } from 'contexts/user';
import * as perms from 'actions/user/Permission';

const APIKEY_ID = 'apiKeyId';
const APIKEY_VALUE = 'apiKeyValue';

// External systems that support personal API keys (from the ref-tables simple list).
const API_KEY_EXT_SYSTEM_CLASSES = ['.AiExternalSystemSimpleVO', '.ApExternalSystemSimpleVO'];

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

export default function ApiKeysSettings() {
    const allExternalSystems = useSelector(
        (appState: AppState) => appState.refTables.externalSystems.items ?? []
    );
    const { hasOne } = usePermissions();
    const canWriteApExtSystems = hasOne(perms.AP_EXTERNAL_WR);
    const externalSystems = useMemo(
        () => allExternalSystems.filter((system) => {
            if (!API_KEY_EXT_SYSTEM_CLASSES.includes(system['@class'])) return false;
            // AP external systems additionally require the ext-system write permission.
            const isApExtSystem = system['@class'] === '.ApExternalSystemSimpleVO';
            return !isApExtSystem || canWriteApExtSystems;
        }),
        [allExternalSystems, canWriteApExtSystems]
    );
    const { id: userId } = useSelector((appState: AppState) => appState.userDetail);
    const [apiKeys, setApiKeys] = useState<ApiKeyValue[]>([]);
    const [availableExternalSystems, setAvailableExternalSystems] = useState<RefExternalSystemSimpleVO[]>([]);
    const dispatch = useThunkDispatch();
    const { formatMessage } = useIntl();

    const loadApiKeys = useCallback(() => {
        (async () => {
            const { data } = await Api.externalSystems.externalSystemAllProperties(undefined, userId || undefined);

            const extsysMap = new Map<number, ApiKeyValue>();
            data.forEach((prop) => {
                if (userId !== prop.userId) {
                    return;
                }
                // check if known property type
                var keyValueObj = extsysMap.get(prop.extSystemId);
                if (prop.name === APIKEY_ID) {
                    if (keyValueObj === undefined) {
                        keyValueObj = { id: prop.extSystemId, apiKeyId: prop };
                        extsysMap.set(prop.extSystemId, keyValueObj);
                    } else {
                        keyValueObj.apiKeyId = prop;
                    }
                } else if (prop.name === APIKEY_VALUE) {
                    if (keyValueObj === undefined) {
                        keyValueObj = { id: prop.extSystemId, apiKeyValue: prop };
                        extsysMap.set(prop.extSystemId, keyValueObj);
                    } else {
                        keyValueObj.apiKeyValue = prop;
                    }
                }
            });

            setAvailableExternalSystems(externalSystems.filter(({ id }) => id != undefined && !extsysMap.has(id)));
            setApiKeys([...extsysMap.values()]);
        })();
    }, [userId, externalSystems]);

    useEffect(() => {
        dispatch(refExternalSystemsFetchIfNeeded());
    }, [dispatch]);

    useEffect(() => {
        loadApiKeys();
    }, [loadApiKeys]);

    const handleSubmit = async ({ externalSystemId, apiKeyId, apiKeyValue }: ApiKeyValueFields) => {
        if (externalSystemId && parseInt(externalSystemId.toString())) {
            await Api.externalSystems.externalSystemStoreProperties([
                {
                    name: APIKEY_VALUE,
                    value: apiKeyValue,
                    userId: userId || undefined,
                    extSystemId: parseInt(externalSystemId.toString()),
                },
                {
                    name: APIKEY_ID,
                    value: apiKeyId,
                    userId: userId || undefined,
                    extSystemId: parseInt(externalSystemId.toString()),
                },
            ]);
            loadApiKeys();
        }
    };

    const handleDelete = (extSystemId?: string | number) => {
        return async () => {
            const extSystem = externalSystems.find(({ id }) => extSystemId === id);
            const result = await dispatch(
                showConfirmDialog(formatMessage(messages.apiKeysDeleteConfirm, { name: extSystem?.name }))
            );
            if (!result) {
                return;
            }

            const apiKey = apiKeys.find(({ id }) => id === extSystemId);
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
        };
    };

    return (
        <Row>
            <Col xs={12}>
                <div style={{ padding: '10px 0' }}>
                    <div>
                        {apiKeys.length === 0 && <FormattedMessage {...messages.apiKeysNoItems} />}
                        {apiKeys.map(({ apiKeyValue, apiKeyId, id }) => {
                            const externalSystem = externalSystems.find(({ id: _id }) => _id === id);
                            return (
                                <div
                                    key={String(id)}
                                    style={{
                                        border: 'var(--primary-border)',
                                        padding: '10px',
                                        borderRadius: '10px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        margin: '5px 0',
                                    }}
                                >
                                    <div style={{ flexGrow: 1 }}>
                                        <div>
                                            <b>{externalSystem?.name}</b>
                                        </div>
                                        <div style={{ display: 'flex', flexWrap: 'wrap' }}>
                                            <div style={{ marginRight: '10px' }}>
                                                <b>
                                                    <FormattedMessage {...messages.apiKeysItemId} />:
                                                </b>{' '}
                                                {apiKeyId?.value}
                                            </div>
                                            <div>
                                                <b>
                                                    <FormattedMessage {...messages.apiKeysItemValue} />:
                                                </b>{' '}
                                                {apiKeyValue?.value}
                                            </div>
                                        </div>
                                    </div>
                                    <div style={{ padding: '5px' }}>
                                        <Button onClick={handleDelete(id)}>
                                            <Icon glyph="fa-trash" />
                                        </Button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                    {availableExternalSystems.length > 0 && (
                        <FinalForm<ApiKeyValueFields>
                            onSubmit={handleSubmit}
                            initialValues={{
                                externalSystemId:
                                    availableExternalSystems.length === 1 ? availableExternalSystems[0].id : undefined,
                            }}
                        >
                            {({ handleSubmit, submitting }) => (
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
                                        {availableExternalSystems.map(({ id, name }) => (
                                            <option value={id} key={id}>
                                                {name}
                                            </option>
                                        ))}
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
                                    <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '10px 0' }}>
                                        <Button variant="outline-secondary" onClick={handleSubmit}>
                                            <FormattedMessage {...messages.apiKeysSave} />
                                        </Button>
                                    </div>
                                </>
                            )}
                        </FinalForm>
                    )}
                </div>
            </Col>
        </Row>
    );
}
