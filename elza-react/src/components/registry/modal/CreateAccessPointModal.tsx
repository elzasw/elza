import React, { FC, useEffect, useState } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { connect, useSelector } from 'react-redux';
import { Form as FinalForm, Field, FormSpy } from 'react-final-form';
import arrayMutators from 'final-form-arrays';
import { ApPartFormVO } from '../../../api/ApPartFormVO';
import { ApTypeVO } from '../../../api/ApTypeVO';
// import { objectById } from '../../../shared/utils';
import requireFields from '../../../shared/utils/requireFields';
import i18n from '../../i18n';
import { Autocomplete, Loading } from '../../shared';
import ReduxFormFieldErrorDecorator from '../../shared/form/ReduxFormFieldErrorDecorator';
import Scope from '../../shared/scope/Scope';
import { Button } from '../../ui';
import PartEditForm from './../form/part-edit-form/PartEditForm';
import {AP_VIEW_SETTINGS} from '../../../constants';
import storeFromArea from '../../../shared/utils/storeFromArea';
import {ApViewSettings} from '../../../api/ApViewSettings';
import {DetailStoreState} from '../../../types';
import {RulPartTypeVO} from '../../../api/RulPartTypeVO';
import { AppState, ScopeData, UserDetail } from "../../../typings/store";
import { WebApi } from '../../../actions/WebApi';
import {ApAccessPointCreateVO} from '../../../api/ApAccessPointCreateVO';
import { compareCreateTypes, hasItemValue } from '../../../utils/ItemInfo';
import { addItems } from '../form/part-edit-form/actions';

const FORM_NAME = 'createAccessPointForm';

/*
const formConfig: ConfigProps<CreateAccessPointModalFields, CreateAccessPointModalProps> = {
    form: FORM_NAME,
    validate: (values) => {
        return requireFields<string>('apType', 'scopeId')(values) as any;
    },
};
*/

export interface CreateAccessPointModalFields {
    apType?: any;
    scopeId?: any;
    partForm?: ApPartFormVO;
}

export interface CreateAccessPointModalProps {
    apTypeFilter?: string[];
    apTypeId?: number;
    onClose?: () => void;
    initialValues?: any;
    onSubmit: (data: any) => any;
}

type Props = CreateAccessPointModalProps & 
ReturnType<typeof mapStateToProps>

const CreateAccessPointModal:FC<Props> = ({
    apViewSettings,
    // handleSubmit,
    onClose,
    refTables,
    apTypeId,
    // apType,
    apTypeFilter,
    // scopeId,
    // partForm,
    // submitting,
    // change,
    userDetail,
}) => {
    const partTypeCode = "PT_NAME";
    const [previousValues, setPreviousValues] = useState<CreateAccessPointModalFields>({});
    // const descItemTypesMap = useSelector((state: AppState) => state.refTables.descItemTypes.itemsMap);
    // const apViewSettings = useSelector((state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>);
    // const refTables = useSelector((state:AppState) => state.refTables);
    // const [values, setValues] = useState(initialValues);
    // const [availableAttributes, setAvailableAttributes] = useState<ApCreateTypeVO[] | undefined>();
    // const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);

    useEffect(() => {
        // const partType = getPartTypeId(refTables.partTypes.items, "PT_NAME");
        /*
        change('partForm', {
            partTypeCode,
            items: [],
        } as ApPartFormVO);
        */
        // fetchAttributes
    }, [apTypeId/*, apType, change*/]);

    const loading = 
        !refTables.apTypes.fetched || 
        !refTables.scopesData.scopes || 
        !refTables.partTypes.fetched || 
        !refTables.rulDataTypes.fetched || 
        !refTables.descItemTypes.fetched || 
        !apViewSettings.fetched;

    const filteredApTypes = filterApTypes(refTables.apTypes.fetched ? refTables.apTypes.items : [], apTypeFilter);
    const filteredScopes = filterScopes(refTables.scopesData.scopes, userDetail);
    const partTypeId = getPartTypeId(refTables.partTypes.items, partTypeCode) as number;

    // const fetchAttributes = (data: ApPartFormVO, partId?: number, parentPartId?: number) => {
    //     const apViewSettingRule = apViewSettings.data!.rules[apViewSettings.data!.typeRuleSetMap[apTypeId]];
    //     const form: ApAccessPointCreateVO = {
    //         typeId: apTypeId,
    //         partForm: {
    //             ...data,
    //             parentPartId,
    //             items: [...data.items.filter(hasItemValue)],
    //             partId: partId,
    //         },
    //         accessPointId: apId,
    //         scopeId: scopeId,
    //     };

    //     WebApi.getAvailableItems(form).then(({attributes, errors}) => {
    //         // Seřazení dat
    //         attributes.sort((a, b) => {
    //             return compareCreateTypes(a, b, partTypeId, refTables, descItemTypesMap, apViewSettingRule);
    //         });

    //         setAvailableAttributes(attributes);
    //         setEditErrors(errors);

    //         // Přidání povinných atributů, pokud ještě ve formuláři nejsou
    //         setValues({
    //             ...data,
    //             items: getItemsWithRequired(data.items, attributes),
    //         })
    //     });
    // };

    /*
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
    */
    const handleSubmit = () => {console.log("submit")}

    return (
        <FinalForm<CreateAccessPointModalFields> onSubmit={handleSubmit}>
            {({submitting, values: {apType, scopeId, partForm}, handleSubmit}) => {
                return  loading ? <Loading/> :
                    <>
                        <Modal.Body>
                            <p>
                                {i18n('accesspoint.create.titleMessage')}
                            </p>
                            <Form.Label>{i18n('registry.add.type')}</Form.Label>
                            <Field
                                name={'apType'}
                                disabled={submitting || apTypeId}
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Autocomplete}
                                passOnly
                                items={filteredApTypes}
                                tree
                                alwaysExpanded
                                allowSelectItem={(item: ApTypeVO) => item.addRecord}
                                value={apTypeId ? apTypeId : apType ? apType.id : null}
                                />

                            <Field
                                name={'scopeId'}
                                disabled={submitting}
                                label={i18n('registry.scopeClass')}
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Scope}
                                passOnly
                                items={filteredScopes}
                                tree
                                alwaysExpanded
                                allowSelectItem={(item: ApTypeVO) => item.addRecord}
                                value={scopeId}
                                />

                            {(apTypeId || (apType && apType.id)) && scopeId && partForm && partTypeId !== undefined && (
                                <>
                                    <hr />
                                    <PartEditForm
                                        formInfo={{
                                            formName: FORM_NAME,
                                            sectionName: 'partForm',
                                        }}
                                        partTypeId={partTypeId}
                                        apTypeId={apType.id}
                                        scopeId={scopeId}
                                        // formData={partForm}
                                        submitting={submitting}
                                        />
                                    </>
                            )}
                        </Modal.Body>
                        <Modal.Footer>
                            <Button type="submit" variant="outline-secondary" onClick={handleSubmit} disabled={submitting}>
                                {i18n('global.action.store')}
                            </Button>

                            <Button variant="link" onClick={onClose} disabled={submitting}>
                                {i18n('global.action.cancel')}
                            </Button>
                        </Modal.Footer>
                        <FormSpy subscription={{values: true}} onChange={({values})=>{
                            console.log(values, previousValues, values === previousValues);
                            setPreviousValues(values)
                        }}/>
                        </>
            }}
        </FinalForm>
    );
};

const getPartTypeId = (partTypes: RulPartTypeVO[] = [], partTypeName: "PT_NAME") => {
    const partType = partTypes.find((item:any)=>item.code === partTypeName);
    return partType ? partType.id : undefined;
}

const filterScopes = (scopes: ScopeData[] = [], userDetail: UserDetail) => {
    // Don't filter, when user is admin, or has permission to write to all scopes.
    if(userDetail.isAdmin() || userDetail.permissionsMap.AP_SCOPE_WR_ALL){return scopes;}
    const userWritableScopes = userDetail.permissionsMap.AP_SCOPE_WR?.scopeIdsMap;
    // Return empty, when user doesn't have any permission to write in scopes.
    if(!userWritableScopes){return [];}

    return [...scopes].map((scopeData)=>({
        ...scopeData,
        scopes: scopeData.scopes.filter((scope) => 
            scope.id !== undefined && 
            scope.id !== null && 
            userWritableScopes[scope.id] !== undefined
        )
    }))
}

const filterApTypes = (apTypes: ApTypeVO[] = [], apTypeCodes: string[] = []) => {
    if(apTypeCodes.length === 0){ return apTypes; }

    const filteredTypes: ApTypeVO[] = [];
    apTypes.forEach((type)=>{
        if(apTypeCodes.indexOf(type.code) >= 0){
            filteredTypes.push(type);
        } else if (type.children) {
            const children = filterApTypes(type.children, apTypeCodes);
            if(children.length > 0){
                filteredTypes.push({
                    ...type,
                    children,
                })
            }
        }
    })
    return filteredTypes;
}

// const selector = formValueSelector(FORM_NAME);
const mapStateToProps = (state: AppState) => {
    return {
        refTables: state.refTables,
        // apType: selector(state, 'apType') as ApTypeVO,
        // scopeId: selector(state, 'scopeId'),
        // partForm: selector(state, 'partForm'),
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
        userDetail: state.userDetail,
    };
};

export default connect(mapStateToProps)(CreateAccessPointModal);
