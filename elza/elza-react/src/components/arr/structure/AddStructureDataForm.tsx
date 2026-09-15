import { useEffect, useMemo, useRef, useState } from 'react';
import { Form, FormCheck, Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import {} from 'components/shared';
import { FormattedMessage, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { templateMessages } from '../templateMessages';
import { Form as FinalForm, Field } from 'react-final-form';
import { FORM_ERROR } from 'final-form';
import FormInputField from '../../shared/form/FormInputField';
import { useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { WebApi } from 'actions';
import { DataType } from 'elza-api';
import { modalDialogHide } from 'actions/global/modalDialog';
import { structureTypeInvalidate } from 'actions/arr/structureType';
import { StructureEdit } from './StructureEdit';

export interface FormValues {
    count: string;
    incrementedTypeIds: number[];
}

interface StructureData {
    id: number;
}

interface Props {
    multiple?: boolean;
    fundVersionId: number;
    fundId: number;
    structureTypeCode: string;
    initialQuery?: string;
    onConfirm?: (structureId: number) => void | Promise<void>;
    onClose?: () => void;
}

function AddStructureDataForm({
    multiple = false,
    fundVersionId,
    fundId,
    structureTypeCode,
    initialQuery = '',
    onConfirm,
    onClose,
}: Props) {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);

    // Creates a temp structure on mount, deletes on unmount (unless confirmed).
    // Ref + cancelled flag ensure correct cleanup even if unmount races with the API call.
    const [structureData, setStructureData] = useState<StructureData | null>(null);
    const structureDataRef = useRef<StructureData | null>(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            const data = await WebApi.createStructureData(fundVersionId, structureTypeCode, initialQuery);
            if (!cancelled) {
                structureDataRef.current = data;
                setStructureData(data);
            } else {
                WebApi.deleteStructureData(fundVersionId, data.id);
            }
        })();
        return () => {
            cancelled = true;
            if (structureDataRef.current) {
                WebApi.deleteStructureData(fundVersionId, structureDataRef.current.id);
            }
        };
    }, [fundVersionId, structureTypeCode, initialQuery]);

    const isLoading = !structureData;

    // Which INT item types should auto-increment when duplicating.
    const [incrementedTypeIds, setIncrementedTypeIds] = useState<number[]>([]);

    // Stable reference prevents react-final-form from resetting fields on re-render.
    const initialValues = useMemo<FormValues>(() => ({ count: '', incrementedTypeIds: [] }), []);

    const validate = (values: FormValues) => {
        const errors: Record<string, string> = {};
        if (multiple) {
            if (!values.count || parseInt(values.count) < 2) {
                errors.count = intl.formatMessage(templateMessages.structureModalAddMultipleErrorCountTooSmall);
            }
            if (incrementedTypeIds.length < 1) {
                errors[FORM_ERROR] = intl.formatMessage(templateMessages.structureModalAddMultipleErrorItemTypeIdsRequired);
            }
        }
        return errors;
    };

    const handleFormSubmit = async (values: FormValues) => {
        const id = structureData!.id;
        if (multiple) {
            await WebApi.duplicateStructureDataBatch(fundVersionId, id, {
                count: values.count,
                incrementedTypeIds,
            });
        } else {
            const structure = await WebApi.confirmStructureData(fundVersionId, id);
            await onConfirm?.(structure.id);
        }
        structureDataRef.current = null; // Clear ref so cleanup doesn't delete the confirmed/duplicated structure
        dispatch(modalDialogHide());
        dispatch(structureTypeInvalidate());
    };

    return (
        <FinalForm<FormValues> initialValues={initialValues} onSubmit={handleFormSubmit} validate={validate}>
            {({ handleSubmit, submitting, submitError, error }) => (
                    <Form onSubmit={handleSubmit}>
                        <Modal.Body>
                            {(submitError || error) && <p>{submitError || error}</p>}
                            {isLoading ? (
                                <div style={{ display: 'flex', justifyContent: 'center', padding: '10px' }}>
                                    {<FormattedMessage {...templateMessages.globalDataLoading} />}
                                </div>
                            ) : (
                                <StructureEdit
                                    fundId={fundId}
                                    fundVersionId={fundVersionId}
                                    structureObjectId={structureData!.id}
                                    plain={true}
                                    renderExtraActions={multiple ? (typeRef) => {
                                        const dataType = dataTypeRefs[typeRef.dataTypeId];
                                        if (dataType?.code !== DataType.Int) { return null; }
                                        const checked = incrementedTypeIds.includes(typeRef.id);
                                        return (
                                            <FormCheck
                                                key="increment"
                                                checked={checked}
                                                onChange={() => {
                                                    if (checked) {
                                                        setIncrementedTypeIds((ids) => ids.filter((id) => id !== typeRef.id));
                                                    } else {
                                                        setIncrementedTypeIds((ids) => [...ids, typeRef.id]);
                                                    }
                                                }}
                                                label={<FormattedMessage {...templateMessages.structureModalIncrement} />}
                                            />
                                        );
                                    } : undefined}
                                />
                            )}
                            {multiple && (
                                <Field
                                    name="count"
                                    component={FormInputField}
                                    min="2"
                                    type="number"
                                    label={<FormattedMessage {...templateMessages.structureModalAddMultipleCount} />}
                                />
                            )}
                        </Modal.Body>
                        <Modal.Footer>
                            <Button type="submit" variant="outline-secondary" disabled={submitting || isLoading}>
                                {<FormattedMessage {...globalMessages.add} />}
                            </Button>
                            <Button
                                type="button"
                                variant="link"
                                disabled={submitting}
                                // Prevents blur → validation flash when clicking cancel.
                                onMouseDown={(e) => e.preventDefault()}
                                onClick={onClose}
                            >
                                {<FormattedMessage {...globalMessages.cancel} />}
                            </Button>
                        </Modal.Footer>
                    </Form>
            )}
        </FinalForm>
    );
}

export default AddStructureDataForm;
