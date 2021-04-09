import React, { FC, useEffect } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { connect } from 'react-redux';
import {
    ConfigProps,
    Field,
    Form as ReduxForm,
    FormSection,
    formValueSelector,
    InjectedFormProps,
    reduxForm,
} from 'redux-form';
import { ApPartFormVO } from '../../../api/ApPartFormVO';
import { ApTypeVO } from '../../../api/ApTypeVO';
// import { objectById } from '../../../shared/utils';
import requireFields from '../../../shared/utils/requireFields';
import i18n from '../../i18n';
import { Autocomplete, Loading } from '../../shared';
import ReduxFormFieldErrorDecorator from '../../shared/form/ReduxFormFieldErrorDecorator';
import Scope from '../../shared/scope/Scope';
import { Button } from '../../ui';
import PartEditForm from './../form/PartEditForm';
import {AP_VIEW_SETTINGS} from '../../../constants';
import storeFromArea from '../../../shared/utils/storeFromArea';
import {ApViewSettings} from '../../../api/ApViewSettings';
import {DetailStoreState} from '../../../types';
import { AppState, PartType, ScopeData, UserDetail } from "../../../typings/store";

const FORM_NAME = 'createAccessPointForm';

const formConfig: ConfigProps<CreateAccessPointModalFields, CreateAccessPointModalProps> = {
    form: FORM_NAME,
    validate: (values) => {
        return requireFields<string>('apType', 'scopeId')(values) as any;
    },
};

export interface CreateAccessPointModalFields {
    apType: any;
    scopeId: any;
    partForm: ApPartFormVO;
}

export interface CreateAccessPointModalProps {
    apTypeFilter?: string[];
    apTypeId?: number;
    onClose?: () => void;
}

type Props = CreateAccessPointModalProps & 
ReturnType<typeof mapStateToProps> &
InjectedFormProps<CreateAccessPointModalFields>;

const CreateAccessPointModal:FC<Props> = ({
    apViewSettings,
    handleSubmit,
    onClose,
    refTables,
    apTypeId,
    apType,
    apTypeFilter,
    scopeId,
    partForm,
    submitting,
    change,
    userDetail,
}) => {
    const partTypeCode = "PT_NAME";

    useEffect(() => {
        // const partType = getPartTypeId(refTables.partTypes.items, "PT_NAME");
        change('partForm', {
            partTypeCode,
            items: [],
        } as ApPartFormVO);
    }, [apTypeId, apType, change]);

    const loading = 
        !refTables.apTypes.fetched || 
        !refTables.scopesData.scopes || 
        !refTables.partTypes.fetched || 
        !refTables.rulDataTypes.fetched || 
        !refTables.descItemTypes.fetched || 
        !apViewSettings.fetched;

    const filteredApTypes = filterApTypes(refTables.apTypes.fetched ? refTables.apTypes.items : [], apTypeFilter);
    const filteredScopes = filterScopes(refTables.scopesData.scopes, userDetail);
    const partTypeId = getPartTypeId(refTables.partTypes.items, partTypeCode);

    return (
        <ReduxForm onSubmit={handleSubmit}>
            { loading ? <Loading/> :
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
                    <FormSection name="partForm">
                        <hr />
                        <PartEditForm
                            formInfo={{
                                formName: FORM_NAME,
                                sectionName: 'partForm',
                            }}
                            partTypeId={partTypeId}
                            apTypeId={apType.id}
                            scopeId={scopeId}
                            formData={partForm}
                            submitting={submitting}
                        />
                    </FormSection>
                )}
            </Modal.Body>
            }
            <Modal.Footer>
                <Button type="submit" variant="outline-secondary" onClick={handleSubmit} disabled={submitting}>
                    {i18n('global.action.store')}
                </Button>

                <Button variant="link" onClick={onClose} disabled={submitting}>
                    {i18n('global.action.cancel')}
                </Button>
            </Modal.Footer>
        </ReduxForm>
    );
};

const getPartTypeId = (partTypes: PartType[] = [], partTypeName: "PT_NAME") => {
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

const selector = formValueSelector(FORM_NAME);
const mapStateToProps = (state: AppState) => {
    return {
        refTables: state.refTables,
        apType: selector(state, 'apType') as ApTypeVO,
        scopeId: selector(state, 'scopeId'),
        partForm: selector(state, 'partForm'),
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
        userDetail: state.userDetail,
    };
};

export default reduxForm<CreateAccessPointModalFields, CreateAccessPointModalProps>(formConfig)(connect(mapStateToProps)(CreateAccessPointModal));
