import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet.jsx';
import { useEffect, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { Field, Form } from 'react-final-form';
import { FormErrors } from 'redux-form';
import { useThunkDispatch } from 'utils/hooks';
import i18n from '../i18n';
import FileInput from '../shared/form/FileInput';
import Icon from '../shared/icon/Icon';
import { Button } from '../ui';
import { fundDataGridImport } from 'actions/arr/fundDataGrid';
import { modalDialogHide } from 'actions/global/modalDialog';

interface ImportFormFields {
    csvFile: File;
}

interface IImportFormProps {
    onClose: () => void;
    versionId: number;
    fundId: number;
}

export const DataGridImportDialog = ({ onClose, versionId, fundId }: IImportFormProps) => {
    const [isRunning, setIsRunning] = useState<boolean>(false);
    const dispatch = useThunkDispatch();

    const validate = (values: ImportFormFields) => {
        const errors: FormErrors<ImportFormFields> = {};

        if (!values.csvFile || values.csvFile == null) {
            errors.csvFile = i18n('global.validation.required');
        }
        return errors;
    };

    useEffect(() => {
        dispatch(refRuleSetFetchIfNeeded());
    }, []);

    const save = async ({ csvFile }: ImportFormFields) => {
        setIsRunning(true);
        await dispatch(fundDataGridImport(versionId, fundId, csvFile));
        dispatch(modalDialogHide());
    };

    return (
        <div>
            {!isRunning && (
                <div>
                    <Form onSubmit={save} validate={validate}>
                        {({ handleSubmit, valid, values }) => {
                            return (
                                <>
                                    <Modal.Body>
                                        {values?.csvFile?.name}
                                        <Field<File> name={'csvFile'}>
                                            {({ input, meta }) => (
                                                <FileInput
                                                    {...input}
                                                    {...meta}
                                                    label={i18n('import.file')}
                                                    type="file"
                                                />
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
                                            {i18n('global.action.import')}
                                        </Button>
                                        <Button variant="link" onClick={onClose}>
                                            {i18n('global.action.cancel')}
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
                        <Icon className="fa-spin" glyph="fa-refresh" /> {i18n('import.running')}
                    </Modal.Body>
                </div>
            )}
        </div>
    );
};

export default DataGridImportDialog;
