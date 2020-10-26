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
import { objectById } from '../../../shared/utils';
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
}) => {
    // eslint-disable-next-line
    useEffect(() => {
        const partType = objectById(refTables.partTypes.items, 'PT_NAME', 'code');
        if (partType) {
            change('partForm', {
                partTypeCode: partType.code,
                items: [],
            } as ApPartFormVO);
        }
    }, [apTypeId, apType]);

    const loading = 
        !refTables.apTypes.fetched || 
        !refTables.scopesData.scopes || 
        !refTables.partTypes.fetched || 
        !refTables.rulDataTypes.fetched || 
        !refTables.descItemTypes.fetched || 
        !apViewSettings.fetched;

    const filteredApTypes = filterApTypes(refTables.apTypes.fetched ? refTables.apTypes.items : [], apTypeFilter);

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
                    allowSelectItem={item => item.addRecord}
                    value={apTypeId ? apTypeId : apType ? apType.id : null}
                />

                <Field
                    name={'scopeId'}
                    disabled={submitting}
                    label={i18n('registry.scopeClass')}
                    component={ReduxFormFieldErrorDecorator}
                    renderComponent={Scope}
                    passOnly
                    items={refTables.scopesData.scopes}
                    tree
                    alwaysExpanded
                    allowSelectItem={item => item.addRecord}
                    value={scopeId}
                />

                {(apTypeId || (apType && apType.id)) && scopeId && partForm && (
                    <FormSection name="partForm">
                        <hr />
                        <PartEditForm
                            formInfo={{
                                formName: FORM_NAME,
                                sectionName: 'partForm',
                            }}
                            partTypeId={objectById(refTables.partTypes.items, 'PT_NAME', 'code').id}
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

const filterApTypes = (apTypes: ApTypeVO[], apTypeCodes?: string[]) => {
    const filteredTypes: ApTypeVO[] = [];

    if(!apTypeCodes || apTypeCodes.length === 0){ return apTypes; }

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
const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
        apType: selector(state, 'apType') as ApTypeVO,
        scopeId: selector(state, 'scopeId'),
        partForm: selector(state, 'partForm'),
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
    };
};

export default reduxForm<CreateAccessPointModalFields, CreateAccessPointModalProps>(formConfig)(connect(mapStateToProps)(CreateAccessPointModal));
