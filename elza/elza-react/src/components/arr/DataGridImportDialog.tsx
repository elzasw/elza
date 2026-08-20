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
const DEFAULT_ENCODING = 'windows-1250';
const PREVIEW_HEAD_BYTES = 8 * 1024;
const PREVIEW_MAX_LINES = 12;

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
    encodingLabel: {
        id: 'dataGrid.import.encoding.label',
        defaultMessage: 'Kódování souboru',
    },
    previewTitle: {
        id: 'dataGrid.import.preview.title',
        defaultMessage: 'Náhled (prvních {count} řádků)',
    },
    previewEmpty: {
        id: 'dataGrid.import.preview.empty',
        defaultMessage: 'Vyberte soubor pro zobrazení náhledu.',
    },
    previewReading: {
        id: 'dataGrid.import.preview.reading',
        defaultMessage: 'Načítám náhled…',
    },
});

const ENCODING_OPTIONS = [
    { value: 'windows-1250', label: 'Windows-1250 (středoevropské)' },
    { value: 'utf-8', label: 'UTF-8' },
    { value: 'iso-8859-2', label: 'ISO-8859-2 (Latin-2)' },
    { value: 'windows-1252', label: 'Windows-1252 (západoevropské)' },
];

interface ImportFormFields {
    csvFile: File;
    separator: string;
    encoding: string;
}

interface IImportFormProps {
    onClose: () => void;
    versionId: number;
    fundId: number;
}

const DataGridImportHint = () => {
    return <div style={{ border: "var(--primary-border)", padding: "8px", maxHeight: "140px", overflowY: "auto" }}>
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

interface CsvPreviewProps {
    file: File | undefined;
    separator: string;
    encoding: string;
}

function CsvPreview({ file, separator, encoding }: CsvPreviewProps) {
    const intl = useIntl();
    const [headBytes, setHeadBytes] = useState<ArrayBuffer | null>(null);
    const [loading, setLoading] = useState<boolean>(false);

    useEffect(() => {
        if (!file) {
            setHeadBytes(null);
            return;
        }
        let cancelled = false;
        setLoading(true);
        // Only read the head — Blob.slice does not load the whole file.
        file.slice(0, PREVIEW_HEAD_BYTES).arrayBuffer().then(buf => {
            if (cancelled) return;
            setHeadBytes(buf);
            setLoading(false);
        }).catch(() => {
            if (cancelled) return;
            setHeadBytes(null);
            setLoading(false);
        });
        return () => {
            cancelled = true;
        };
    }, [file]);

    if (!file) {
        return <div style={{ marginTop: '8px', fontStyle: 'italic', opacity: 0.7 }}>
            {intl.formatMessage(messages.previewEmpty)}
        </div>;
    }
    if (loading || !headBytes) {
        return <div style={{ marginTop: '8px', fontStyle: 'italic' }}>
            {intl.formatMessage(messages.previewReading)}
        </div>;
    }

    // Non-fatal decoder replaces incomplete trailing bytes with U+FFFD; we then drop
    // the last line to hide any resulting mojibake on the boundary of the head slice.
    let text: string;
    try {
        text = new TextDecoder(encoding).decode(headBytes);
    } catch {
        text = new TextDecoder('utf-8').decode(headBytes);
    }
    const rawLines = text.split(/\r?\n/);
    const safeLines = rawLines.length > 1 ? rawLines.slice(0, -1) : rawLines;
    const lines = safeLines.slice(0, PREVIEW_MAX_LINES);
    const rows = lines.map(line => line.split(separator));
    const columnCount = rows.reduce((max, row) => Math.max(max, row.length), 0);

    return <div style={{ marginTop: '8px' }}>
        <div style={{ marginBottom: '4px', fontWeight: 600 }}>
            {intl.formatMessage(messages.previewTitle, { count: rows.length })}
        </div>
        <div style={{ overflowX: 'auto', border: 'var(--primary-border)' }}>
            <table style={{ borderCollapse: 'collapse', fontSize: '12px', width: '100%' }}>
                <tbody>
                    {rows.map((cells, rowIdx) => (
                        <tr key={rowIdx}>
                            <td style={{
                                padding: '2px 6px',
                                borderRight: 'var(--primary-border)',
                                background: 'var(--fill-100, #f5f5f5)',
                                textAlign: 'right',
                                color: '#888',
                                userSelect: 'none',
                            }}>{rowIdx + 1}</td>
                            {Array.from({ length: columnCount }).map((_, colIdx) => (
                                <td key={colIdx} style={{
                                    padding: '2px 6px',
                                    borderRight: colIdx < columnCount - 1 ? 'var(--primary-border)' : undefined,
                                    whiteSpace: 'nowrap',
                                    maxWidth: '240px',
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                }} title={cells[colIdx] ?? ''}>
                                    {cells[colIdx] ?? ''}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    </div>;
}

async function toUtf8File(file: File, encoding: string): Promise<File> {
    if (encoding === 'utf-8') return file;
    const buf = await file.arrayBuffer();
    const text = new TextDecoder(encoding).decode(buf);
    const utf8 = new TextEncoder().encode(text);
    return new File([utf8], file.name, { type: file.type || 'text/csv' });
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

    const save = async ({ csvFile, separator, encoding }: ImportFormFields) => {
        setIsRunning(true);
        try {
            const utf8File = await toUtf8File(csvFile, encoding || DEFAULT_ENCODING);
            await dispatch(fundDataGridImport(versionId, fundId, utf8File, separator || DEFAULT_SEPARATOR));
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
                    <Form<ImportFormFields> onSubmit={save} validate={validate} initialValues={{ separator: DEFAULT_SEPARATOR, encoding: DEFAULT_ENCODING } as ImportFormFields}>
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
                                        <div style={{ display: 'flex', gap: '16px', marginTop: '8px' }}>
                                            <Field<string> name={'encoding'}>
                                                {({ input }) => (
                                                    <div>
                                                        <label htmlFor="csv-encoding-select" style={{ display: 'block', marginBottom: '4px' }}>
                                                            {intl.formatMessage(messages.encodingLabel)}
                                                        </label>
                                                        <select
                                                            id="csv-encoding-select"
                                                            {...input}
                                                        >
                                                            {ENCODING_OPTIONS.map(opt => (
                                                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                            ))}
                                                        </select>
                                                    </div>
                                                )}
                                            </Field>
                                            <Field<string> name={'separator'}>
                                                {({ input }) => (
                                                    <div>
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
                                        </div>
                                        <CsvPreview
                                            file={values?.csvFile}
                                            separator={values?.separator || DEFAULT_SEPARATOR}
                                            encoding={values?.encoding || DEFAULT_ENCODING}
                                        />
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
