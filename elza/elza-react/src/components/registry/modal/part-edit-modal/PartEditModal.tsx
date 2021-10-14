import React, { useEffect, useState } from 'react';
import { Form } from 'react-final-form';
import arrayMutators from 'final-form-arrays';
import { useSelector } from "react-redux";
import PartEditForm from "../../form/part-edit-form/PartEditForm";
import { ApPartFormVO } from "../../../../api/ApPartFormVO";
import { Modal } from 'react-bootstrap';
import { Button } from "../../../ui";
import i18n from "../../../i18n";
import { WebApi } from '../../../../actions/WebApi';
import { RulDescItemTypeExtVO } from '../../../../api/RulDescItemTypeExtVO';
import { DetailStoreState } from '../../../../types';
import { ApViewSettings } from '../../../../api/ApViewSettings';
import ModalDialogWrapper from '../../../shared/dialog/ModalDialogWrapper';
import { compareCreateTypes, hasItemValue } from '../../../../utils/ItemInfo';
import storeFromArea from '../../../../shared/utils/storeFromArea';
import {AP_VIEW_SETTINGS} from '../../../../constants';
import {ApAccessPointCreateVO} from '../../../../api/ApAccessPointCreateVO';
import {RequiredType} from '../../../../api/RequiredType';
import { addItems } from '../../form/part-edit-form/actions';
import {ApItemVO} from '../../../../api/ApItemVO';
import {ApCreateTypeVO} from '../../../../api/ApCreateTypeVO';
import { AppState } from 'typings/store'

const FORM_NAME = "partEditForm";

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

const PartEditModal = ({
    onClose,
    partTypeId,
    apTypeId,
    scopeId,
    initialValues,
    parentPartId,
    apId,
    partId,
    onSubmit,
}: Props) => {
    const descItemTypesMap = useSelector((state: AppState) => state.refTables.descItemTypes.itemsMap);
    const apViewSettings = useSelector((state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>);
    const refTables = useSelector((state:AppState) => state.refTables);
    const [values, setValues] = useState(initialValues);
    const [availableAttributes, setAvailableAttributes] = useState<ApCreateTypeVO[] | undefined>();
    const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);

    const handleClose = () => { onClose(); }

    useEffect(()=>{
        if(values){
            fetchAttributes(values, partId, parentPartId);
        }
    }, [])

    if (!refTables) {
        return <div />;
    }

    const fetchAttributes = (data: ApPartFormVO, partId?: number, parentPartId?: number) => {
        const apViewSettingRule = apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]];
        const form: ApAccessPointCreateVO = {
            typeId: apTypeId,
            partForm: {
                ...data,
                parentPartId,
                items: [...data.items.filter(hasItemValue)],
                partId: partId,
            },
            accessPointId: apId,
            scopeId: scopeId,
        };

        WebApi.getAvailableItems(form).then(({attributes, errors}) => {
            // Seřazení dat
            attributes.sort((a, b) => {
                return compareCreateTypes(a, b, partTypeId, refTables, descItemTypesMap, apViewSettingRule);
            });

            setAvailableAttributes(attributes);
            setEditErrors(errors);

            // Přidání povinných atributů, pokud ještě ve formuláři nejsou
            setValues({
                ...data,
                items: getItemsWithRequired(data.items, attributes),
            })
        });
    };

    const getItemsWithRequired = ( items: ApItemVO[], attributes: ApCreateTypeVO[] ) => {
        const newItems: ApItemVO[] = [];
        addItems(
            getRequiredAttributes(items, attributes), 
            refTables,
            items,
            partTypeId,
            (_index, item) => {newItems.push(item)},
            false,
            descItemTypesMap,
            apViewSettings
        )
        return sortApItems([...items, ...newItems], descItemTypesMap);
    }

    const sortApItems = (items: ApItemVO[], descItemTypesMap: Record<number, RulDescItemTypeExtVO>) => {
        return [...items].sort((a, b) => {
            if(!a){return 1;}
            if(!b){return -1;}
            return descItemTypesMap[a.typeId].viewOrder - descItemTypesMap[b.typeId].viewOrder;
        })
    }

    const getRequiredAttributes = (items: ApItemVO[], attributes: ApCreateTypeVO[]) => {
        const existingItemTypeIds = items.map(i => i.typeId);
        const requiredAttributes = attributes.filter(attributes => {
            if (attributes.requiredType === RequiredType.REQUIRED) {
                return existingItemTypeIds.indexOf(attributes.itemTypeId) < 0;
            } else {
                return false;
            }
        });
        return requiredAttributes;
    }

    const getAttributes = (_name:string, state: any) => {
        fetchAttributes(state.formState.values, partId, parentPartId)
    }
    
    return <ModalDialogWrapper
        className='dialog-visible dialog-lg'
        title={"title"}
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
                            formInfo={{
                                formName: FORM_NAME,
                                sectionName: "partForm"
                            }}
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
