import React, {useEffect, useState} from 'react';
import {Field, FieldArray, Form, InjectedFormProps, reduxForm, formValueSelector} from 'redux-form';
import {connect} from "react-redux";
import {ThunkDispatch} from "redux-thunk";
import {Action} from "redux";
import {ApAccessPointCreateVO} from "../../../api/ApAccessPointCreateVO";
import {ApAttributesInfoVO} from "../../../api/ApAttributesInfoVO";
import {WebApi} from "../../../actions/WebApi";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {ApItemVO} from "../../../api/ApItemVO";
import {ApPartFormVO} from "../../../api/ApPartFormVO";

type OwnProps = {
    partTypeId: number;
    apTypeId: number;
    parentPartId?: number;
    aeId?: number;
    partId?: number;

    handleSubmit?: () => void;
    onSubmit?: (formData: any) => void;
    initialValues?: ApPartFormVO;
}

type Props = OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const PartEditForm: React.FC<Props> = ({handleSubmit, initialValues, refTables, aeId, partId, parentPartId, partTypeId, itemsValue}) => {
    const [attrInfo, setAttrInfo] = useState<ApAttributesInfoVO>({
        attributes: [],
        errors: [],
    });

    console.log('ITEMSVALUE', itemsValue);

    useEffect(() => {
        getAttrInfo(initialValues);
    }, []);

    const getAttrInfo = async (partForm: ApPartFormVO): Promise<void> => {
        setAttrInfo(await WebApi.getAvailableItems({
            partForm,
            typeId: partTypeId,
            languageCode: 'cs', //todo: co sem?
            accessPointId: aeId,
            scopeId: 1, //todo: co sem?
        } as ApAccessPointCreateVO));
    };

    const renderItem = (name: string, item: ApItemVO) => {
        const itemType: RulDescItemTypeExtVO = refTables.descItemTypes.itemsMap[item.typeId];
        const common = {
            name,
            label: itemType.name,
        };

        switch (item['@class']) {
            case 'ApItemEnumVO':
                return <Field
                    component={'select'}
                    {...common}
                >
                    {}
                </Field>;
            case 'ApItemAPFragmentRefVO':
            case 'ApItemAccessPointRefVO':
            case 'ApItemBitVO':
            case 'ApItemCoordinatesVO':
            case 'ApItemDateVO':
            case 'ApItemDecimalVO':
            case 'ApItemFormattedTextVO':
            case 'ApItemIntVO':
            case 'ApItemJsonTableVO':
            case 'ApItemPartyRefVO':
            case 'ApItemStringVO':
            case 'ApItemTextVO':
            case 'ApItemUnitdateVO':
            case 'ApItemUnitidVO':
            case 'ApItemUriRefVO':
            default:
                return <Field
                    component={'input'}
                    type={'text'}
                    {...common}
                />
        }
    };

    const renderItems = ({fields}) => <div>
        {attrInfo.attributes.map((attr, idx) => {
            const itemType: RulDescItemTypeExtVO = refTables.descItemTypes.itemsMap[attr.itemTypeId];

            return <span
                key={idx}
                onClick={() => fields.push({
                    id: null,
                    position: null,
                    objectId: null,
                    typeId: attr.itemTypeId,
                    specId: attr.itemSpecIds ? attr.itemSpecIds.pop() : null,
                })}
            >
                {itemType ? itemType.name : false}
            </span>;
        })}
        {fields.map((item, index) => {

            console.log('ITEM', item, refTables.descItemTypes.itemsMap);
            const value = itemsValue[index];
            const itemType: RulDescItemTypeExtVO = refTables.descItemTypes.itemsMap[value.typeId];

            return <div key={index}>
                <div className={'form-group'}>
                    <Field
                        component={'input'}
                        type={'text'}
                        name={`items.${index}.value`}
                        label={itemType.name}
                    />
                    {/*{renderItem(`items.${index}.value`, item)}*/}
                </div>
            </div>
        })}
    </div>;

    return <Form onSubmit={handleSubmit}>
        <FieldArray name={'items'} component={renderItems}/>
    </Form>;
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>, props: OwnProps) => ({});

const mapStateToProps = (state: any) => {
    const selector = formValueSelector('PartEditForm');
    return {
        itemsValue: selector(state, 'items'),
        refTables: state.refTables,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm({
    form: 'PartEditForm',
})(PartEditForm));
