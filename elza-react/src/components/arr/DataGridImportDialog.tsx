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
import { addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import ReactTextareaAutosize from 'react-textarea-autosize';

interface ImportFormFields {
    csvFile: File;
}

interface IImportFormProps {
    onClose: () => void;
    versionId: number;
    fundId: number;
}

const DataGridImportHint = () => {
    return <div style={{ border: "var(--primary-border)", padding: "8px" }}>
        <p dangerouslySetInnerHTML={{ __html: i18n("^dataGrid.import.format.hint") }} />
        <h4>
            {i18n("dataGrid.import.format.example.title")}
        </h4>
        <p dangerouslySetInnerHTML={{ __html: i18n("dataGrid.import.format.example.message") }} />
        <div>
            {/* <ReactTextareaAutosize rows={3} style={{ width: "100%" }} value={"069f2e12-b808-4b3f-af48-35c372ae0818,ZP2015_FORMAL_TITLE,Divá Bára,ZP2015_OTHER_ID,ZP2015_OTHERID_SIG,1234/75,ZP2015_UNIT_TYPE,ZP2015_UNIT_TYPE_RKP"} /> */}
            <div style={{ lineBreak: "anywhere", border: "var(--primary-border)", padding: "8px" }}>069f2e12-b808-4b3f-af48-35c372ae0818,ZP2015_FORMAL_TITLE,Divá Bára,ZP2015_OTHER_ID,ZP2015_OTHERID_SIG,1234/75,ZP2015_UNIT_TYPE,ZP2015_UNIT_TYPE_RKP</div>
        </div>
    </div>
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
        try {
            await dispatch(fundDataGridImport(versionId, fundId, csvFile));
            dispatch(modalDialogHide());
            dispatch(addToastrSuccess(i18n("ribbon.action.arr.dataGrid.import.success")));
        }
        catch (error) {
            // error is shown in toaster
            setIsRunning(false);
        }
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
                                        <DataGridImportHint />
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
