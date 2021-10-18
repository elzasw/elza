import React, { FC, useEffect, useState } from 'react';
import { Form } from 'react-final-form';
import arrayMutators from 'final-form-arrays';
import { useSelector } from "react-redux";
import { ApPartFormVO } from "api/ApPartFormVO";
import { getPartEditDialogLabel } from "api/old/PartTypeInfo";
import { PartType } from "api/generated/model";
import { Modal } from 'react-bootstrap';
import { Button } from "components/ui";
import i18n from "components/i18n";
import { DetailStoreState } from 'types';
import { ApViewSettings } from 'api/ApViewSettings';
import ModalDialogWrapper from 'components/shared/dialog/ModalDialogWrapper';
import storeFromArea from 'shared/utils/storeFromArea';
import debounce from 'shared/utils/debounce';
import {AP_VIEW_SETTINGS} from '../../../../constants';
import { getUpdatedForm } from '../../form/part-edit-form/actions';
import { PartEditForm } from "../../form/part-edit-form/PartEditForm";
import {ApCreateTypeVO} from 'api/ApCreateTypeVO';
import { AppState } from 'typings/store'
import { Loading } from 'components/shared';

type Props = {
    partTypeId: number;
    initialValues?: ApPartFormVO;
    apTypeId: number;
    scopeId: number;
    parentPartId?: number;
    apId: number;
    partId?: number;
    onClose: () => void;
    onSubmit: (data:any) => Promise<void>;
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

    const fetchAttributes = async (data: ApPartFormVO) => {
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

    const debouncedFetchAttributes = debounce(fetchAttributes, 50) as typeof fetchAttributes;

    const getAttributes = (_name:string, state: any) => {
        debouncedFetchAttributes(state.formState.values);
    }

    const createMode = partId != undefined ? false : true;
    const partType = refTables.partTypes.itemsMap[partTypeId];
    
    return <ModalDialogWrapper
        className='dialog-visible dialog-lg'
        title={getPartEditDialogLabel(partType.code as PartType, createMode)}
        onHide={handleClose}
    >
        <Form<ApPartFormVO> 
            mutators={{ 
                ...arrayMutators, 
                attributes: getAttributes
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
