import { useEffect, useMemo, useRef, useState } from 'react';
import { Form, FormCheck, Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import { i18n } from 'components/shared';
import { Form as FinalForm, Field } from 'react-final-form';
import { FORM_ERROR } from 'final-form';
import StructureSubNodeForm from './StructureSubNodeForm';
import { structureNodeFormFetchIfNeeded, structureNodeFormSelectId } from '../../../actions/arr/structureNodeForm';
import FormInputField from '../../shared/form/FormInputField';
import { useAppThunkDispatch } from 'utils/hooks';
import DescItemFactory from 'components/arr/nodeForm/DescItemFactory';
import { WebApi } from 'actions';
import { DataTypeCode } from 'stores/app/accesspoint/itemFormUtils';
import { ItemTypeLiteVO } from 'api/ItemTypeLiteVO';
import { modalDialogHide } from 'actions/global/modalDialog';
import { structureTypeInvalidate } from 'actions/arr/structureType';

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
    descItemFactory: typeof DescItemFactory;
    onConfirm?: (structureId: number) => void | Promise<void>;
    onClose?: () => void;
}

function AddStructureDataForm({
    multiple = false,
    fundVersionId,
    fundId,
    structureTypeCode,
    initialQuery = '',
    descItemFactory,
    onConfirm,
    onClose,
}: Props) {
    const dispatch = useAppThunkDispatch();

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

    // Populate Redux store for StructureSubNodeForm (which handles its own loading).
    useEffect(() => {
        if (structureData?.id) {
            dispatch(structureNodeFormSelectId(fundVersionId, structureData.id));
            dispatch(structureNodeFormFetchIfNeeded(fundVersionId, structureData.id));
        }
    }, [dispatch, fundVersionId, structureData?.id]);

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
            {({ handleSubmit, submitting, submitError, error }) => {
                const customRender = (code: DataTypeCode, infoType: ItemTypeLiteVO) => {
                    if (code === DataTypeCode.INT) {
                        const index = incrementedTypeIds.indexOf(infoType.id);
                        const checked = index !== -1;

                        return (
                            <FormCheck
                                key="increment"
                                checked={checked}
                                onChange={() => {
                                    if (checked) {
                                        setIncrementedTypeIds((ids) => ids.filter((id) => id !== infoType.id));
                                    } else {
                                        setIncrementedTypeIds((ids) => [...ids, infoType.id]);
                                    }
                                }}
                                label={i18n('arr.structure.modal.increment')}
                            />
                        );
                    }
                    return null;
                };

                return (
                    <Form onSubmit={handleSubmit}>
                        <Modal.Body>
                            {(submitError || error) && <p>{submitError || error}</p>}
                            {isLoading ? (
                                <div style={{ display: 'flex', justifyContent: 'center', padding: '10px' }}>
                                    {i18n('global.data.loading')}
                                </div>
                            ) : (
                                <StructureSubNodeForm
                                    id={structureData!.id}
                                    versionId={fundVersionId}
                                    fundId={fundId}
                                    selectedSubNodeId={structureData!.id}
                                    customActions={multiple && customRender}
                                    descItemFactory={descItemFactory}
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
                );
            }}
        </FinalForm>
    );
}

export default AddStructureDataForm;
