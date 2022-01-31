import { ApCreateTypeVO } from 'api/ApCreateTypeVO';
import { ApPartFormVO } from 'api/ApPartFormVO';
import { ApTypeVO } from 'api/ApTypeVO';
import { ApViewSettings } from 'api/ApViewSettings';
import { RulPartTypeVO } from 'api/RulPartTypeVO';
import i18n from 'components/i18n';
import { Loading } from 'components/shared';
import { Button } from 'components/ui';
import arrayMutators from 'final-form-arrays';
import React, { FC, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { Form } from 'react-final-form';
import { useSelector } from 'react-redux';
import debounce from 'shared/utils/debounce';
import storeFromArea from 'shared/utils/storeFromArea';
import { DetailStoreState } from 'types';
import { AppState, ScopeData, UserDetail } from "typings/store";
import { hasItemValue } from 'utils/ItemInfo';
import { AP_VIEW_SETTINGS } from '../../../constants';
import { getUpdatedForm } from '../part-edit/form/actions';
import { PartEditForm } from '../part-edit/form/PartEditForm';
import { FormAutocomplete, FormScope } from '../part-edit/form/fields';
import { getValueChangeMutators } from '../part-edit/form/valueChangeMutators';

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

const CreateAccessPointModal:FC<CreateAccessPointModalProps> = ({
    onClose,
    apTypeId,
    apTypeFilter,
    onSubmit,
}) => {
    const partTypeCode = "PT_NAME";
    const apViewSettings = useSelector((state: AppState) => storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>);
    const refTables = useSelector((state:AppState) => state.refTables);
    const userDetail = useSelector((state:AppState) => state.userDetail);
    const [values, setValues] = useState<CreateAccessPointModalFields>({partForm:{partTypeCode, items: []}});
    const [availableAttributes, setAvailableAttributes] = useState<ApCreateTypeVO[] | undefined>();
    const [editErrors, setEditErrors] = useState<Array<string> | undefined>(undefined);

    const loading = 
        !refTables.apTypes.fetched || 
        !refTables.scopesData.scopes || 
        !refTables.partTypes.fetched || 
        !refTables.rulDataTypes.fetched || 
        !refTables.descItemTypes.fetched || 
        !apViewSettings.fetched;

    const apTypes = filterApTypes(refTables.apTypes.fetched ? refTables.apTypes.items : [], apTypeFilter);
    const scopes = getScopes(refTables.scopesData.scopes, userDetail);
    const partTypeId = getPartTypeId(refTables.partTypes.items, partTypeCode) as number;

    const fetchAttributes = async (data: CreateAccessPointModalFields) => {
        if(data.apType?.id == null || data.scopeId == null){return;}
        const items = data.partForm?.items ? [...data.partForm.items] : [];
        const form = data.partForm || {
            partTypeCode,
            items: items.filter(hasItemValue),
        }
        const { attributes, errors, data: partForm } = await getUpdatedForm(
            form,
            data.apType?.id, 
            data.scopeId, 
            apViewSettings, 
            refTables, 
            partTypeId
        )
        setAvailableAttributes(attributes);
        setEditErrors(errors);
        setValues({
            ...data,
            partForm,
        })
    };

    const debouncedFetchAttributes = debounce(fetchAttributes, 100) as typeof fetchAttributes;

    if(loading){ return <Loading/> }

    return (
        <Form<CreateAccessPointModalFields> 
            initialValues={values} 
            onSubmit={onSubmit}
            mutators={{
                ...arrayMutators,
                ...getValueChangeMutators(debouncedFetchAttributes),
            }}
        >
            {({submitting, values: {apType, scopeId, partForm}, handleSubmit}) => {
                return <>
                    <Modal.Body>
                        <p>
                            {i18n('accesspoint.create.titleMessage')}
                        </p>
                        <FormAutocomplete
                            name={'apType'}
                            label={i18n('registry.add.type')}
                            disabled={submitting || apTypeId != null}
                            items={apTypes}
                            tree
                            alwaysExpanded
                            allowSelectItem={(item: ApTypeVO) => item.addRecord}
                            />

                        <FormScope
                            name={'scopeId'}
                            disabled={submitting}
                            label={i18n('registry.scopeClass')}
                            items={scopes}
                            />

                        {(apTypeId || (apType && apType.id)) && scopeId && partForm && partTypeId !== undefined && (
                            <>
                                <hr />
                                <PartEditForm
                                    // formInfo={{
                                    //     formName: FORM_NAME,
                                    //     sectionName: 'partForm',
                                    // }}
                                    partTypeId={partTypeId}
                                    apTypeId={apType.id}
                                    scopeId={scopeId}
                                    submitting={submitting}
                                    availableAttributes={availableAttributes}
                                    editErrors={editErrors}
                                    arrayName="partForm.items"
                                    partItems={null}
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
                    </>
            }}
        </Form>
    );
};

const getPartTypeId = (partTypes: RulPartTypeVO[] = [], partTypeName: "PT_NAME") => {
    const partType = partTypes.find((item:any)=>item.code === partTypeName);
    return partType ? partType.id : undefined;
}

const getScopes = (scopes: ScopeData[] = [], userDetail: UserDetail) => {
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

export default CreateAccessPointModal;
