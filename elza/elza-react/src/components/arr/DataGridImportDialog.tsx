import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet.jsx';
import { useEffect, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { Field, Form } from 'react-final-form';
import { FormErrors } from 'redux-form';
import { defineMessages, useIntl } from 'react-intl';
import { useThunkDispatch } from 'utils/hooks';
import i18n from '../i18n';
import FileInput from '../shared/form/FileInput';
import Icon from '../shared/icon/Icon';
import { Button } from '../ui';
import { fundDataGridImport } from 'actions/arr/fundDataGrid';
import { modalDialogHide } from 'actions/global/modalDialog';
import { addToastrSuccess } from 'components/shared/toastr/ToastrActions';

const DEFAULT_SEPARATOR = ';';

const messages = defineMessages({
    separatorLabel: {
        id: 'dataGrid.import.separator.label',
        defaultMessage: 'Oddělovač polí CSV',
    },
    separatorSemicolon: {
        id: 'dataGrid.import.separator.semicolon',
        defaultMessage: '; (středník)',
    },
    separatorComma: {
        id: 'dataGrid.import.separator.comma',
        defaultMessage: ', (čárka)',
    },
    separatorTab: {
        id: 'dataGrid.import.separator.tab',
        defaultMessage: '\\t (tabulátor)',
    },
    separatorPipe: {
        id: 'dataGrid.import.separator.pipe',
        defaultMessage: '| (svislítko)',
    },
});

interface ImportFormFields {
    csvFile: File;
    separator: string;
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
            <div style={{ lineBreak: "anywhere", border: "var(--primary-border)", padding: "8px" }}>069f2e12-b808-4b3f-af48-35c372ae0818,ZP2015_FORMAL_TITLE,Divá Bára,ZP2015_OTHER_ID,ZP2015_OTHERID_SIG,1234/75,ZP2015_UNIT_TYPE,ZP2015_UNIT_TYPE_RKP</div>
        </div>
    </div>
}

export const DataGridImportDialog = ({ onClose, versionId, fundId }: IImportFormProps) => {
    const [isRunning, setIsRunning] = useState<boolean>(false);
    const dispatch = useThunkDispatch();
    const intl = useIntl();

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

    const save = async ({ csvFile, separator }: ImportFormFields) => {
        setIsRunning(true);
        try {
            await dispatch(fundDataGridImport(versionId, fundId, csvFile, separator || DEFAULT_SEPARATOR));
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
                    <Form<ImportFormFields> onSubmit={save} validate={validate} initialValues={{ separator: DEFAULT_SEPARATOR } as ImportFormFields}>
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
                                        <Field<string> name={'separator'}>
                                            {({ input }) => (
                                                <div style={{ marginTop: '8px' }}>
                                                    <label htmlFor="csv-separator-select" style={{ display: 'block', marginBottom: '4px' }}>
                                                        {intl.formatMessage(messages.separatorLabel)}
                                                    </label>
                                                    <select
                                                        id="csv-separator-select"
                                                        {...input}
                                                    >
                                                        <option value=";">{intl.formatMessage(messages.separatorSemicolon)}</option>
                                                        <option value=",">{intl.formatMessage(messages.separatorComma)}</option>
                                                        <option value={'\t'}>{intl.formatMessage(messages.separatorTab)}</option>
                                                        <option value="|">{intl.formatMessage(messages.separatorPipe)}</option>
                                                    </select>
                                                </div>
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
