import { useState, useEffect, useCallback, useMemo } from 'react';
import {
    Button,
    Card,
    CardHeader,
    Field,
    Input,
    Select,
    Text,
    Tooltip,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { AddRegular, DeleteRegular } from '@fluentui/react-icons';
import { MaskedValue } from 'components/shared/MaskedValue';
import { globalMessages } from 'components/shared/lang';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { Form as FinalForm, Field as FinalField } from 'react-final-form';
import { Api } from 'api';
import { RefExternalSystemSimpleVO } from 'typings/store';
import { useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { ExtSystemProperty } from 'elza-api';
import { useConfirmModal } from 'components/shared/dialog/useConfirmModal';
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

function isFilledIn({ externalSystemId, apiKeyId, apiKeyValue }: Partial<ApiKeyValueFields>) {
    return !!externalSystemId && !!apiKeyId?.trim() && !!apiKeyValue?.trim();
}

const messages = defineMessages({
    sectionHint: {
        id: 'userSettings.apiKeys.sectionHint',
        defaultMessage:
            'Osobní klíč se použije místo klíče nastaveného pro celou instanci, když ELZA volá daný externí systém za vás.',
    },
    apiKeysAdd: {
        id: 'userSettings.apiKeys.add',
        defaultMessage: 'Přidat API klíč',
    },
    apiKeysDelete: {
        id: 'userSettings.apiKeys.delete',
        defaultMessage: 'Smazat klíč',
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

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
        paddingTop: tokens.spacingVerticalM,
        paddingBottom: tokens.spacingVerticalM,
    },
    hint: {
        color: tokens.colorNeutralForeground3,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
        fontStyle: 'italic',
    },
    list: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    properties: {
        display: 'grid',
        gridTemplateColumns: 'auto 1fr',
        columnGap: tokens.spacingHorizontalM,
        rowGap: tokens.spacingVerticalXXS,
        alignItems: 'baseline',
    },
    propertyLabel: {
        color: tokens.colorNeutralForeground3,
    },
    propertyValue: {
        fontFamily: tokens.fontFamilyMonospace,
        wordBreak: 'break-all',
    },
    addButton: {
        alignSelf: 'flex-start',
    },
    formFields: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    formActions: {
        display: 'flex',
        justifyContent: 'flex-end',
        gap: tokens.spacingHorizontalS,
        paddingTop: tokens.spacingVerticalXS,
    },
});

export function ApiKeysSettings() {
    const allExternalSystems = useAppSelector(({ refTables }) => refTables.externalSystems.items ?? []);
    const { hasOne } = usePermissions();
    const canWriteApExtSystems = hasOne(perms.AP_EXTERNAL_WR);
    const externalSystems = useMemo(
        () =>
            allExternalSystems.filter((system) => {
                if (!API_KEY_EXT_SYSTEM_CLASSES.includes(system['@class'])) return false;
                // AP external systems additionally require the ext-system write permission.
                const isApExtSystem = system['@class'] === '.ApExternalSystemSimpleVO';
                return !isApExtSystem || canWriteApExtSystems;
            }),
        [allExternalSystems, canWriteApExtSystems]
    );
    const { id: userId } = useAppSelector(({ userDetail }) => userDetail);
    const [apiKeys, setApiKeys] = useState<ApiKeyValue[]>([]);
    const [availableExternalSystems, setAvailableExternalSystems] = useState<RefExternalSystemSimpleVO[]>([]);
    const [isAddingKey, setIsAddingKey] = useState(false);
    const dispatch = useAppThunkDispatch();
    const confirm = useConfirmModal();
    const { formatMessage } = useIntl();
    const styles = useStyles();

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
        const extSystemId = externalSystemId ? parseInt(externalSystemId.toString()) : NaN;
        if (!extSystemId) {
            return;
        }

        await Api.externalSystems.externalSystemStoreProperties([
            {
                name: APIKEY_VALUE,
                value: apiKeyValue,
                userId: userId || undefined,
                extSystemId,
            },
            {
                name: APIKEY_ID,
                value: apiKeyId,
                userId: userId || undefined,
                extSystemId,
            },
        ]);
        loadApiKeys();
        setIsAddingKey(false);
    };

    const handleDelete = (extSystemId?: string | number) => {
        return async () => {
            const extSystem = externalSystems.find(({ id }) => extSystemId === id);
            const result = await confirm({
                title: formatMessage(messages.apiKeysDelete),
                message: formatMessage(messages.apiKeysDeleteConfirm, { name: extSystem?.name }),
                destructive: true,
            });
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

    const canAddKey = availableExternalSystems.length > 0;

    return (
        <div className={styles.root}>
            <Text size={200} className={styles.hint}>
                {formatMessage(messages.sectionHint)}
            </Text>
            {canAddKey && !isAddingKey && (
                <Button
                    className={styles.addButton}
                    appearance="primary"
                    icon={<AddRegular />}
                    onClick={() => setIsAddingKey(true)}
                >
                    {formatMessage(messages.apiKeysAdd)}
                </Button>
            )}
            {canAddKey && isAddingKey && (
                <FinalForm<ApiKeyValueFields>
                    onSubmit={handleSubmit}
                    initialValues={{
                        externalSystemId:
                            availableExternalSystems.length === 1 ? availableExternalSystems[0].id : undefined,
                    }}
                >
                    {({ handleSubmit, submitting, values }) => (
                        <Card className={styles.formFields} appearance="outline">
                            <FinalField
                                name="externalSystemId"
                                render={({ input }) => (
                                    <Field label={formatMessage(messages.extSystem)}>
                                        <Select
                                            disabled={submitting}
                                            value={input.value == undefined ? '' : String(input.value)}
                                            onChange={(_event, data) => input.onChange(data.value)}
                                        >
                                            <option value="" />
                                            {availableExternalSystems.map(({ id, name }) => (
                                                <option value={id} key={id}>
                                                    {name}
                                                </option>
                                            ))}
                                        </Select>
                                    </Field>
                                )}
                            />
                            <FinalField
                                name="apiKeyId"
                                render={({ input }) => (
                                    <Field label={formatMessage(messages.apiKeyId)}>
                                        <Input
                                            disabled={submitting}
                                            value={input.value ?? ''}
                                            onChange={(_event, data) => input.onChange(data.value)}
                                        />
                                    </Field>
                                )}
                            />
                            <FinalField
                                name="apiKeyValue"
                                render={({ input }) => (
                                    <Field label={formatMessage(messages.apiKeyValue)}>
                                        <Input
                                            disabled={submitting}
                                            value={input.value ?? ''}
                                            onChange={(_event, data) => input.onChange(data.value)}
                                        />
                                    </Field>
                                )}
                            />
                            <div className={styles.formActions}>
                                <Button
                                    appearance="secondary"
                                    disabled={submitting}
                                    onClick={() => setIsAddingKey(false)}
                                >
                                    <FormattedMessage {...globalMessages.cancel} />
                                </Button>
                                <Button
                                    appearance="primary"
                                    disabled={submitting || !isFilledIn(values)}
                                    onClick={handleSubmit}
                                >
                                    <FormattedMessage {...messages.apiKeysSave} />
                                </Button>
                            </div>
                        </Card>
                    )}
                </FinalForm>
            )}
            {apiKeys.length === 0 ? (
                <Text size={200} className={styles.empty}>
                    <FormattedMessage {...messages.apiKeysNoItems} />
                </Text>
            ) : (
                <div className={styles.list}>
                    {apiKeys.map(({ apiKeyValue, apiKeyId, id }) => {
                        const externalSystem = externalSystems.find(({ id: _id }) => _id === id);
                        return (
                            <Card key={String(id)} appearance="outline">
                                <CardHeader
                                    header={<Text weight="semibold">{externalSystem?.name}</Text>}
                                    action={
                                        <Tooltip content={formatMessage(messages.apiKeysDelete)} relationship="label">
                                            <Button
                                                appearance="subtle"
                                                icon={<DeleteRegular />}
                                                onClick={handleDelete(id)}
                                            />
                                        </Tooltip>
                                    }
                                />
                                <div className={styles.properties}>
                                    <Text size={200} className={styles.propertyLabel}>
                                        <FormattedMessage {...messages.apiKeysItemId} />
                                    </Text>
                                    <Text size={200} className={styles.propertyValue}>
                                        {apiKeyId?.value}
                                    </Text>
                                    <Text size={200} className={styles.propertyLabel}>
                                        <FormattedMessage {...messages.apiKeysItemValue} />
                                    </Text>
                                    <MaskedValue value={apiKeyValue?.value} />
                                </div>
                            </Card>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
