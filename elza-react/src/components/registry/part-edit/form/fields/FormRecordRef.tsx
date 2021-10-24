import { ApAccessPointVO } from 'api';
import { ApCreateTypeVO } from 'api/ApCreateTypeVO';
import { Area } from 'api/Area';
import { RulDescItemSpecExtVO } from 'api/RulDescItemSpecExtVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import React, { FC } from 'react';
import { Button, Col, Form, Row } from 'react-bootstrap';
import { useField, useForm } from 'react-final-form';
import { useDispatch } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState } from 'typings/store';
import { modalDialogHide, modalDialogShow } from '../../../../../actions/global/modalDialog';
import { ApItemAccessPointRefVO } from '../../../../../api/ApItemAccessPointRefVO';
import { objectById } from '../../../../../shared/utils';
import { Icon } from '../../../../index';
import RelationPartItemEditModalForm from '../../../modal/RelationPartItemEditModalForm';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample } from '../../../revision';

type ThunkAction<R> = (dispatch: ThunkDispatch<AppState, void, AnyAction>, getState: () => AppState) => Promise<R>;
const useThunkDispatch = <State,>():ThunkDispatch<State, void, AnyAction> => useDispatch()
interface RelationPartItemEditModalFormFields {
    onlyMainPart: boolean,
    area?: Area,
    specId?: string,
    codeObj: {
        id: number,
        codeObj?: ApAccessPointVO,
        name: string,
        specId?: number,
    },
}


export const FormRecordRef:FC<{
    name: string;
    label: string;
    disabled: boolean;
    item: ApItemAccessPointRefVO;
    itemType: RulDescItemTypeExtVO;
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
    partTypeId: number;
    scopeId: number;
    apTypeId: number;
}> = ({
    name,
    label,
    disabled,
    item,
    itemType,
    itemTypeAttributeMap,
    partTypeId,
    scopeId,
    apTypeId,
}) => {
    const dispatch = useThunkDispatch<AppState>();
    const form = useForm();
    const field = useField(name);
    const handleEditItem = async () => {
        const fieldValue = await dispatch(handleSelectAccessPointRef(item, partTypeId, itemTypeAttributeMap, scopeId, apTypeId))
        form.change(name, fieldValue)
        handleValueUpdate(form);
    }
    return (
        <Row className={'d-flex'}>
            <Col>
                <RevisionFieldExample 
                    label={label} 
                    prevValue={`zakladatel/zřizovatel: Neruda, Jan (1834-1891)`} 
                    value={getDisplayValue(item, itemType)}
                >
                    <div style={{display: "flex"}}>
                        <Form.Control style={{flexShrink: 1}} value={getDisplayValue(item, itemType)} disabled={true} />
                        <Button
                            disabled={disabled}
                            variant={'action'}
                            onClick={handleEditItem}
                        >
                            <Icon glyph="fa-edit" />
                        </Button>
                    </div>
                </RevisionFieldExample>
            </Col>
        </Row>
    );
}

const getDisplayValue = (apItem: ApItemAccessPointRefVO, itemType: RulDescItemTypeExtVO) => {
    const apItemName = apItem.accessPoint?.name || apItem.externalName;

    if (itemType.useSpecification && apItem.specId) {
        const spec:RulDescItemSpecExtVO | null = objectById(itemType.descItemSpecs, apItem.specId) || null;
        return `${spec?.shortcut}: ${ apItemName }`
    } 

    return apItemName;
}

const handleSelectAccessPointRef = (
    item: ApItemAccessPointRefVO,
    partTypeId: number,
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    scopeId: number,
    apTypeId: number,
):ThunkAction<ApItemAccessPointRefVO> => {
    return (dispatch, getState) => {
        const state = getState();
        return new Promise((resolve) => dispatch(
            modalDialogShow(
                this,
                state.refTables.descItemTypes.itemsMap[item.typeId].shortcut,
                <RelationPartItemEditModalForm
                    initialValues={getInitialValues(item)}
                    itemTypeAttributeMap={itemTypeAttributeMap}
                    typeId={item.typeId}
                    apTypeId={apTypeId}
                    scopeId={scopeId}
                    partTypeId={partTypeId}
                    onSubmit={(form: RelationPartItemEditModalFormFields) => {
                        resolve(getFieldValue(form, item))
                        dispatch(modalDialogHide());
                    }}
                    />
            )))
    }
}

const getInitialValues = (item: ApItemAccessPointRefVO):Partial<RelationPartItemEditModalFormFields> => {
    return {
        onlyMainPart: false,
        area: Area.ALLNAMES,
        specId: item.specId?.toString(),
        codeObj: item.value != null ? {
            id: item.value,
            codeObj: item.accessPoint,
            name: item.accessPoint && item.accessPoint.name,
            specId: item.specId,
        } : undefined,
    };
}

const getFieldValue = (form: RelationPartItemEditModalFormFields, item: ApItemAccessPointRefVO): ApItemAccessPointRefVO => {
        return {
            ...item,
            specId: form.specId ? parseInt(form.specId) : undefined,
            accessPoint: {
                //@ts-ignore - ignore @class property
                '@class': '.ApAccessPointVO',
                id: form.codeObj.id,
                name: form.codeObj?.name,
            },
            value: form.codeObj.id,
        };
}
