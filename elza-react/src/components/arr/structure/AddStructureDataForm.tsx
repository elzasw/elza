import { useEffect, useMemo, useRef, useState } from 'react';
import { Form, FormCheck, Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import { i18n } from 'components/shared';
import { Form as FinalForm, Field } from 'react-final-form';
import { FORM_ERROR } from 'final-form';
import FormInputField from '../../shared/form/FormInputField';
import { useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { Api } from 'api/api';
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
    const dispatch = useAppThunkDispatch();
    const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);

    // Creates a temp structure on mount, deletes on unmount (unless confirmed).
    // Ref + cancelled flag ensure correct cleanup even if unmount races with the API call.
    const [structureData, setStructureData] = useState<StructureData | null>(null);
    const structureDataRef = useRef<StructureData | null>(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            const { data } = await Api.structure.sdoCreateObject(fundId, structureTypeCode, initialQuery);
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
                errors.count = i18n('arr.structure.modal.addMultiple.error.count.tooSmall');
            }
            if (incrementedTypeIds.length < 1) {
                errors[FORM_ERROR] = i18n('arr.structure.modal.addMultiple.error.itemTypeIds.required');
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
                                    {i18n('global.data.loading')}
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
                                                label={i18n('arr.structure.modal.increment')}
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
                                    label={i18n('arr.structure.modal.addMultiple.count')}
                                />
                            )}
                        </Modal.Body>
                        <Modal.Footer>
                            <Button type="submit" variant="outline-secondary" disabled={submitting || isLoading}>
                                {i18n('global.action.add')}
                            </Button>
                            <Button
                                type="button"
                                variant="link"
                                disabled={submitting}
                                // Prevents blur → validation flash when clicking cancel.
                                onMouseDown={(e) => e.preventDefault()}
                                onClick={onClose}
                            >
                                {i18n('global.action.cancel')}
                            </Button>
                        </Modal.Footer>
                    </Form>
            )}
        </FinalForm>
    );
}

export default AddStructureDataForm;
