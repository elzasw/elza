import { importForm } from 'actions/global/global.jsx';
import { WebApi } from 'actions/index.jsx';
import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet.jsx';
import { humanFileSize } from 'components/Utils.jsx';
import React, { useEffect, useState } from 'react';
import { Form as BsForm, Modal } from 'react-bootstrap';
import { Field, Form } from 'react-final-form';
import { useSelector } from 'react-redux';
import { FormErrors } from 'redux-form';
import { AppState, Scope } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { exceedsUploadLimit, useUploadLimits } from 'utils/hooks/useUploadLimits';
import * as perms from '../../actions/user/Permission.jsx';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    registryScope: { id: 'import.registryScope', defaultMessage: 'Oblast entit' },
    file: { id: 'import.file', defaultMessage: 'Soubor' },
    running: { id: 'import.running', defaultMessage: 'Import probíhá' },
    importAction: { id: 'global.action.import', defaultMessage: 'Importovat' },
    maxFileSize: { id: 'import.maxFileSize', defaultMessage: 'Maximální velikost souboru: {size}' },
    selectedFileSize: { id: 'import.selectedFileSize', defaultMessage: 'Velikost vybraného souboru: {size}' },
});
import Autocomplete from '../shared/autocomplete/Autocomplete';
import FileInput from '../shared/form/FileInput';
import Icon from '../shared/icon/Icon';
import { Button } from '../ui';

/**
 * Formulář importu rejstříkových hesel
 * <ImportForm fund onSubmit={this.handleCallImportRegistry} />
 */

interface ImportFormFields {
    recordScope: Scope;
    xmlFile: File;
}

interface IImportFormProps {
    record?: boolean;
    fund?: boolean;
    onClose: () => void;
}
export const ImportForm = ({ record, fund, onClose }: IImportFormProps) => {
    const intl = useIntl();
    const [scopes, setScopes] = useState<Scope[]>([]);
    const [isRunning, setIsRunning] = useState<boolean>(false);
    const uploadLimits = useUploadLimits();
    const maxFileSize = uploadLimits?.maxFileSize ?? -1;
    const dispatch = useThunkDispatch();
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);

    const initialRecordScope = scopes.length === 1 ? scopes[0] : undefined;

    const hasSizeLimit = maxFileSize >= 0;
    const isTooLarge = (file?: File) => !!file && exceedsUploadLimit(file, maxFileSize);

    const validate = (values: ImportFormFields) => {
        const errors: FormErrors<ImportFormFields> = {};

        if (!values.recordScope || !values.recordScope.id) {
            errors.recordScope = intl.formatMessage(globalMessages.validationRequired);
        }
        if (!values.xmlFile || values.xmlFile == null) {
            errors.xmlFile = intl.formatMessage(globalMessages.validationRequired);
        } else if (isTooLarge(values.xmlFile)) {
            errors.xmlFile = intl.formatMessage(messages.maxFileSize, { size: humanFileSize(maxFileSize) });
        }
        return errors;
    };

    useEffect(() => {
        dispatch(refRuleSetFetchIfNeeded());
        WebApi.getAllScopes().then((scopes: Scope[]) => {
            const availableScopes = scopes.filter((scope: Scope) => {
                return userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
                    type: perms.AP_SCOPE_WR,
                    scopeId: scope.id || undefined,
                });
            });
            setScopes(availableScopes);
        });
    }, []);

    const save = (values: ImportFormFields) => {
        setIsRunning(true);

        const data: { xmlFile: File; scopeId?: number } = {
            xmlFile: values.xmlFile,
        };

        if (values.recordScope && values.recordScope.id) {
            data.scopeId = values.recordScope.id;
        }

        const formData = new FormData();
        for (const key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        const messageType = fund ? 'Fund' : record ? 'Record' : 'Unknown';
        dispatch(importForm(formData, messageType));
    };

    return (
        <div>
            {!isRunning && (
                <div>
                    <Form onSubmit={save} validate={validate} initialValues={{ recordScope: initialRecordScope }}>
                        {({ handleSubmit, valid }) => {
                            return (
                                <>
                                    <Modal.Body>
                                        <Field<string> name="recordScope">
                                            {({ input, meta }) => (
                                                <Autocomplete
                                                    {...input}
                                                    {...meta}
                                                    label={intl.formatMessage(messages.registryScope)}
                                                    items={scopes}
                                                    getItemId={(item: Scope) => (item ? item.id : null)}
                                                    getItemName={(item: Scope) => (item ? item.name : '')}
                                                />
                                            )}
                                        </Field>
                                        <Field<File> name={'xmlFile'}>
                                            {({ input, meta }) => (
                                                <>
                                                    <FileInput
                                                        {...input}
                                                        {...meta}
                                                        label={intl.formatMessage(messages.file)}
                                                        type="file"
                                                    />
                                                    {input.value instanceof File && (
                                                        <BsForm.Text
                                                            className={`d-block ${isTooLarge(input.value) ? 'text-danger' : 'text-muted'}`}
                                                        >
                                                            <FormattedMessage
                                                                {...messages.selectedFileSize}
                                                                values={{ size: humanFileSize(input.value.size) }}
                                                            />
                                                        </BsForm.Text>
                                                    )}
                                                    {hasSizeLimit && (
                                                        <BsForm.Text
                                                            className={`d-block ${isTooLarge(input.value) ? 'text-danger' : 'text-muted'}`}
                                                        >
                                                            <FormattedMessage
                                                                {...messages.maxFileSize}
                                                                values={{ size: humanFileSize(maxFileSize) }}
                                                            />
                                                        </BsForm.Text>
                                                    )}
                                                </>
                                            )}
                                        </Field>
                                    </Modal.Body>
                                    <Modal.Footer>
                                        <Button
                                            disabled={!valid}
                                            variant="outline-secondary"
                                            type="submit"
                                            onClick={handleSubmit}
                                        >
                                            <FormattedMessage {...messages.importAction} />
                                        </Button>
                                        <Button variant="link" onClick={onClose}>
                                            <FormattedMessage {...globalMessages.cancel} />
                                        </Button>
                                    </Modal.Footer>
                                </>
                            );
                        }}
                    </Form>
                </div>
            )}
            {isRunning && (
                <div>
                    <Modal.Body>
                        <Icon className="fa-spin" glyph="fa-refresh" /> <FormattedMessage {...messages.running} />
                    </Modal.Body>
                </div>
            )}
        </div>
    );
};

export default ImportForm;
