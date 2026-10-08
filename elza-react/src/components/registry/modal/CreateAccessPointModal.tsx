import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Divider,
    Dropdown,
    Field as FluentField,
    Option,
    Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { Dismiss24Regular } from '@fluentui/react-icons';
import { ApCreateTypeVO } from 'api/ApCreateTypeVO';
import { ApTypeVO } from 'api/ApTypeVO';
import { ApViewSettings } from 'api/ApViewSettings';
import { RulPartTypeVO } from 'api/RulPartTypeVO';
import { globalMessages } from 'components/shared/lang/messages';
import arrayMutators from 'final-form-arrays';
import { FC, useState } from 'react';
import { Field, Form } from 'react-final-form';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import debounce from 'shared/utils/debounce';
import storeFromArea from 'shared/utils/storeFromArea';
import { DetailStoreState } from 'types';
import { AppState, ScopeData, UserDetail } from 'typings/store';
import { hasItemValue } from 'utils/ItemInfo';
import { AP_VIEW_SETTINGS } from '../../../constants';
import { ApTypePicker } from '../ApTypePicker';
import { RevisionApPartForm } from '../part-edit/form';
import { getUpdatedForm } from '../part-edit/form/actions';
import { PartEditForm } from '../part-edit/form/PartEditForm';
import { getValueChangeMutators, handleValueUpdate } from '../part-edit/form/valueChangeMutators';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    titleMessage: {
        id: 'accesspoint.create.titleMessage',
        defaultMessage:
            'Nejprve vyberte oblast a poté podtřídu nové archivní entity. Podle vybrané podtřídy se zobrazí příslušné atributy. Po vyplnění hlavní části jména je možné archivní entitu založit.',
    },
    titleMessageSingleScope: {
        id: 'accesspoint.create.titleMessageSingleScope',
        defaultMessage:
            'Nejprve vyberte podtřídu nové archivní entity. Podle vybrané podtřídy se zobrazí příslušné atributy. Po vyplnění hlavní části jména je možné archivní entitu založit.',
    },
    addType: { id: 'registry.add.type', defaultMessage: 'Podtřída' },
    scopeClass: { id: 'registry.scopeClass', defaultMessage: 'Oblast' },
});

const useStyles = makeStyles({
    surface: {
        maxWidth: '900px',
        width: '100%',
    },
    content: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
});

export interface CreateAccessPointModalFields {
    apType?: ApTypeVO;
    scopeId?: number;
    partForm?: RevisionApPartForm;
}

export interface CreateAccessPointModalProps {
    /** Dialog title. */
    title: string;
    /** Only these class codes can be chosen (with their parents shown). */
    apTypeFilter?: string[];
    onClose: () => void;
    onSubmit: (data: CreateAccessPointModalFields) => Promise<unknown> | unknown;
}

/**
 * New archival entity: the scope first (preset when the user can write to one scope only), then a
 * class offered by the rule set of the scope, then the main name part.
 */
const CreateAccessPointModal: FC<CreateAccessPointModalProps> = ({ title, onClose, apTypeFilter, onSubmit }) => {
    const intl = useIntl();
    const styles = useStyles();
    const partTypeCode = 'PT_NAME';
    const apViewSettings = useSelector(
        (state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
    );
    const refTables = useSelector((state: AppState) => state.refTables);
    const userDetail = useSelector((state: AppState) => state.userDetail);

    const scopes = getScopes(refTables.scopesData.scopes, userDetail);
    const visibleScopes = scopes.find((scopeData) => scopeData.versionId === -1)?.scopes || [];
    const partTypeId = getPartTypeId(refTables.partTypes.items, partTypeCode) as number;

    const [values, setValues] = useState<CreateAccessPointModalFields>({
        scopeId: visibleScopes.length === 1 ? (visibleScopes[0].id ?? undefined) : undefined,
        partForm: { partTypeCode, items: [] },
    });
    const [availableAttributes, setAvailableAttributes] = useState<ApCreateTypeVO[] | undefined>();
    const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);

    const loading =
        !refTables.scopesData.scopes ||
        !refTables.partTypes.fetched ||
        !refTables.rulDataTypes.fetched ||
        !refTables.descItemTypes.fetched ||
        !apViewSettings.fetched;

    const fetchAttributes = async (data: CreateAccessPointModalFields) => {
        if (data.apType?.id == null || data.scopeId == null) {
            return;
        }
        const items = data.partForm?.items ? [...data.partForm.items] : [];
        const form = data.partForm || {
            partTypeCode,
            items: items.filter(hasItemValue),
        };
        const { attributes, errors, data: partForm } = await getUpdatedForm(
            form,
            data.apType.id,
            data.scopeId,
            apViewSettings,
            refTables,
            partTypeId,
        );
        setAvailableAttributes(attributes);
        setEditErrors(errors);
        setValues({ ...data, partForm });
    };

    const debouncedFetchAttributes = debounce(fetchAttributes, 100) as typeof fetchAttributes;

    return (
        <Dialog open modalType="modal" onOpenChange={(_event, data) => !data.open && onClose()}>
            <DialogSurface className={styles.surface}>
                {loading ? (
                    <Spinner />
                ) : (
                    <Form<CreateAccessPointModalFields>
                        initialValues={values}
                        onSubmit={onSubmit}
                        mutators={{
                            ...arrayMutators,
                            ...getValueChangeMutators(debouncedFetchAttributes),
                        }}
                    >
                        {({ submitting, values: { apType, scopeId, partForm }, handleSubmit, form }) => (
                            <form onSubmit={handleSubmit}>
                                <DialogBody>
                                    <DialogTitle
                                        action={
                                            <Button
                                                appearance="subtle"
                                                aria-label={intl.formatMessage(globalMessages.close)}
                                                icon={<Dismiss24Regular />}
                                                onClick={onClose}
                                            />
                                        }
                                    >
                                        {title}
                                    </DialogTitle>
                                    <DialogContent className={styles.content}>
                                        <p>
                                            <FormattedMessage
                                                {...(visibleScopes.length > 1
                                                    ? messages.titleMessage
                                                    : messages.titleMessageSingleScope)}
                                            />
                                        </p>
                                        {visibleScopes.length > 1 && (
                                            <Field<number> name="scopeId">
                                                {({ input }) => (
                                                    <FluentField
                                                        label={intl.formatMessage(messages.scopeClass)}
                                                        required
                                                    >
                                                        <Dropdown
                                                            disabled={submitting}
                                                            value={
                                                                visibleScopes.find((s) => s.id === input.value)?.name ?? ''
                                                            }
                                                            selectedOptions={input.value ? [String(input.value)] : []}
                                                            onOptionSelect={(_event, data) => {
                                                                input.onChange(Number(data.optionValue));
                                                                handleValueUpdate(form);
                                                            }}
                                                        >
                                                            {visibleScopes.map((scope) => (
                                                                <Option key={scope.id} value={String(scope.id)}>
                                                                    {scope.name}
                                                                </Option>
                                                            ))}
                                                        </Dropdown>
                                                    </FluentField>
                                                )}
                                            </Field>
                                        )}
                                        <Field<ApTypeVO> name="apType">
                                            {({ input }) => (
                                                <ApTypePicker
                                                    label={intl.formatMessage(messages.addType)}
                                                    required
                                                    scopeId={scopeId}
                                                    value={input.value ? input.value.id : undefined}
                                                    apTypeFilter={apTypeFilter}
                                                    disabled={submitting}
                                                    onChange={(type) => {
                                                        input.onChange(type);
                                                        handleValueUpdate(form);
                                                    }}
                                                />
                                            )}
                                        </Field>

                                        {apType?.id != null && scopeId && partForm && partTypeId !== undefined && (
                                            <>
                                                <Divider />
                                                <PartEditForm
                                                    partTypeId={partTypeId}
                                                    apTypeId={apType.id}
                                                    scopeId={scopeId}
                                                    submitting={submitting}
                                                    availableAttributes={availableAttributes}
                                                    editErrors={editErrors}
                                                    arrayName="partForm.items"
                                                />
                                            </>
                                        )}
                                    </DialogContent>
                                    <DialogActions>
                                        <Button type="submit" appearance="primary" disabled={submitting}>
                                            <FormattedMessage {...globalMessages.save} />
                                        </Button>
                                        <Button onClick={onClose} disabled={submitting}>
                                            <FormattedMessage {...globalMessages.cancel} />
                                        </Button>
                                    </DialogActions>
                                </DialogBody>
                            </form>
                        )}
                    </Form>
                )}
            </DialogSurface>
        </Dialog>
    );
};

const getPartTypeId = (partTypes: RulPartTypeVO[] = [], partTypeName: 'PT_NAME') => {
    const partType = partTypes.find((item) => item.code === partTypeName);
    return partType ? partType.id : undefined;
};

const getScopes = (scopes: ScopeData[] = [], userDetail: UserDetail) => {
    // Don't filter, when user is admin, or has permission to write to all scopes.
    if (userDetail.isAdmin() || userDetail.permissionsMap.AP_SCOPE_WR_ALL) {
        return scopes;
    }
    const userWritableScopes = userDetail.permissionsMap.AP_SCOPE_WR?.scopeIdsMap;
    // Return empty, when user doesn't have any permission to write in scopes.
    if (!userWritableScopes) {
        return [];
    }

    return [...scopes].map((scopeData) => ({
        ...scopeData,
        scopes: scopeData.scopes.filter(
            (scope) => scope.id !== undefined && scope.id !== null && userWritableScopes[scope.id] !== undefined,
        ),
    }));
};

export default CreateAccessPointModal;
