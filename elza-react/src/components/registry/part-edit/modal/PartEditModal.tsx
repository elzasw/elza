import { ApCreateTypeVO } from 'api/ApCreateTypeVO';
import { ApViewSettings } from 'api/ApViewSettings';
import { PartType } from "api/generated/model";
import { getPartEditDialogLabel } from "api/old/PartTypeInfo";
import i18n from "components/i18n";
import { Loading } from 'components/shared';
import { ModalDialogWrapper } from 'components/shared/dialog/ModalDialogWrapper';
import { Button } from "components/ui";
import arrayMutators from 'final-form-arrays';
import React, { FC, useEffect, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { Form } from 'react-final-form';
import { useSelector } from "react-redux";
import debounce from 'shared/utils/debounce';
import storeFromArea from 'shared/utils/storeFromArea';
import { DetailStoreState } from 'types';
import { AppState } from 'typings/store';
import { AP_VIEW_SETTINGS } from '../../../../constants';
import { getUpdatedForm, getValueChangeMutators, PartEditForm } from '../form';
import { RevisionApPartForm } from '../form';

type Props = {
    partTypeId: number;
    initialValues?: RevisionApPartForm;
    apTypeId: number;
    scopeId: number;
    parentPartId?: number;
    apId: number;
    partId?: number;
    onClose: () => void;
    onSubmit: (data:any) => Promise<void>;
    revision?: boolean;
}

const PartEditModal:FC<Props> = ({
    onClose,
    partTypeId,
    apTypeId,
    scopeId,
    initialValues,
    parentPartId,
    apId,
    partId,
    onSubmit,
    revision = false,
}) => {
    const apViewSettings = useSelector((state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>);
    const refTables = useSelector((state:AppState) => state.refTables);
    const [values, setValues] = useState(initialValues);
    const [availableAttributes, setAvailableAttributes] = useState<ApCreateTypeVO[] | undefined>();
    const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);

    const handleClose = () => { onClose(); }

    useEffect(()=>{
        if(values){ debouncedFetchAttributes(values); }
    }, [])

    if (!refTables) { return <Loading/> }

    const fetchAttributes = async (data: RevisionApPartForm) => {
        const {attributes, errors, data: formData} = await getUpdatedForm(
            data, 
            apTypeId, 
            scopeId, 
            apViewSettings, 
            refTables, 
            partTypeId, 
            partId, 
            parentPartId, 
            apId
        )

        setAvailableAttributes(attributes);
        setEditErrors(errors);
        setValues(formData)
    };

    const debouncedFetchAttributes = debounce(fetchAttributes, 100) as typeof fetchAttributes;

    const createMode = partId != undefined ? false : true;
    const partType = refTables.partTypes.itemsMap[partTypeId];
    
    return <ModalDialogWrapper
        className='dialog-visible dialog-lg'
        title={getPartEditDialogLabel(partType.code as PartType, createMode)}
        onHide={handleClose}
    >
        <Form<RevisionApPartForm> 
            mutators={{ 
                ...arrayMutators as any, 
                ...getValueChangeMutators(debouncedFetchAttributes),
            }} 
            onSubmit={onSubmit}
            initialValues={values}
        >
            {({ submitting, handleSubmit }) => {
                return <>
                    <Modal.Body>
                        <PartEditForm
                            partTypeId={partTypeId}
                            apTypeId={apTypeId}
                            scopeId={scopeId}
                            submitting={submitting}
                            availableAttributes={availableAttributes}
                            editErrors={editErrors}
                            revision={revision} 
                            />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary" onClick={handleSubmit} disabled={submitting}>
                            {i18n('global.action.store')}
                        </Button>

                        <Button variant="link" onClick={handleClose} disabled={submitting}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                    </>
            }}
        </Form>
    </ModalDialogWrapper>
};


export default PartEditModal
