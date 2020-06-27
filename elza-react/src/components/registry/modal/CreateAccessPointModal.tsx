import React, {useEffect} from 'react';
import {
    ConfigProps,
    Field,
    Form as ReduxForm,
    FormSection,
    formValueSelector,
    InjectedFormProps,
    reduxForm,
    SubmitHandler,
} from 'redux-form';
import {Form, Modal} from 'react-bootstrap';
import {connect} from "react-redux";
import {Action} from "redux";
import {ThunkDispatch} from "redux-thunk";
import PartEditForm from "./../form/PartEditForm";
import {ApPartFormVO} from "../../../api/ApPartFormVO";
import {Button} from "../../ui";
import i18n from "../../i18n";
import ReduxFormFieldErrorDecorator from "../../shared/form/ReduxFormFieldErrorDecorator";
import {Autocomplete} from "../../shared";
import {ApTypeVO} from "../../../api/ApTypeVO";
import Scope from "../../shared/scope/Scope";
import {objectById} from "../../../shared/utils";

const FORM_NAME = "createAccessPointForm";

const formConfig: ConfigProps<ApPartFormVO> = {
    form: FORM_NAME,
};

type Props = {
    refTables: {};
    handleSubmit: SubmitHandler<FormData, any, any>;
    apTypeId: number;
    formData?: ApPartFormVO;
    submitting: boolean;
    onClose: () => void;
} & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const CreateAccessPointModal = ({handleSubmit, onClose, refTables, apTypeId, apType, scopeId, partForm, submitting, change}: Props) => {

    // eslint-disable-next-line
    useEffect(() => {
        change('partForm', {
            partTypeCode: objectById(refTables.partTypes.items, 'PT_NAME', 'code').code,
            items: []
        } as ApPartFormVO);
    }, [apTypeId]);

    return <ReduxForm onSubmit={handleSubmit}>
        <Modal.Body>
            <span>
                Nejprve vyberte třídu a oblast nové archivní entity. Dle typu vybrané třídy se zobrazí
                příslušné atributy, které vyplňte. Poté můžete novou archivní entitu založit.
            </span>
            <Form.Label>
                {i18n('registry.add.type')}
            </Form.Label>
            <Field
                name={'apType'}
                disabled={submitting || apTypeId}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={Autocomplete}
                passOnly
                items={refTables.apTypes.items}
                tree
                alwaysExpanded
                allowSelectItem={item => item.addRecord}
                value={apTypeId ? apTypeId : (apType ? apType.id : null)}
            />

            <Field
                name={'scopeId'}
                disabled={submitting}
                label={i18n('registry.scopeClass')}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={Scope}
                passOnly
                items={refTables.scopesData}
                tree
                alwaysExpanded
                allowSelectItem={item => item.addRecord}
                value={scopeId}
            />

            {(apTypeId || (apType && apType.id)) && scopeId && partForm && <FormSection name="partForm">
                <hr/>
                <PartEditForm
                    formInfo={{
                        formName: FORM_NAME,
                        sectionName: "partForm"
                    }}
                    partTypeId={objectById(refTables.partTypes.items, 'PT_NAME', 'code').id}
                    apTypeId={apType.id}
                    scopeId={scopeId}
                    formData={partForm}
                    submitting={submitting}
                />
            </FormSection>}
        </Modal.Body>
        <Modal.Footer>
            <Button type="submit" variant="outline-secondary" onClick={handleSubmit} disabled={submitting}>
                {i18n('global.action.store')}
            </Button>

            <Button variant="link" onClick={onClose} disabled={submitting}>
                {i18n('global.action.cancel')}
            </Button>
        </Modal.Footer>
    </ReduxForm>;
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({});

const selector = formValueSelector(FORM_NAME);
const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
        apType: selector(state, 'apType') as ApTypeVO,
        scopeId: selector(state, 'scopeId'),
        partForm: selector(state, 'partForm'),
    }
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm<any, any>(formConfig)(CreateAccessPointModal));
