import PropTypes from 'prop-types';
import React from 'react';
import {connect} from "react-redux";
import {change, Field, formValueSelector, getFormValues, reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n, Icon, NoFocusButton} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField, submitReduxFormWithProp} from 'components/form/FormUtils.jsx';
import {WebApi} from 'actions/index.jsx';
import {getTreeItemById} from './registryUtils';
import Scope from '../shared/scope/Scope';
import LanguageCodeField from '../LanguageCodeField';
import ApItemNameForm from '../accesspoint/ApItemNameForm';
import {accessPointFormActions} from '../accesspoint/AccessPointFormActions';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';
import AddDescItemTypeForm from '../arr/nodeForm/AddDescItemTypeForm';
import {FormInputField} from "../shared";
import ReduxFormFieldErrorDecorator from "../shared/form/ReduxFormFieldErrorDecorator";

/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm onSubmit={this.handleCallAddRegistry} />
 */
class AddRegistryForm extends AbstractReactComponent {
    static formName = 'addRegistryForm';
    static validate = (values, props) => {
        console.log('VALIDATE', values);
        const errors = {};
        if (!values.structured) {
            if (!values.name) {
                errors.name = i18n('global.validation.required');
            }
            if (!values.description) {
                errors.description = i18n('global.validation.required');
            }
        }
        if (!values.scopeId) {
            errors.scopeId = i18n('global.validation.required');
        }

        if (!values.type) {
            errors.type = i18n('global.validation.required');
        }

        return errors;
    };

    static propTypes = {
        typeId: PropTypes.number,
        versionId: PropTypes.number,
        showSubmitTypes: PropTypes.bool.isRequired,
    };

    // static defaultProps = {
    //     versionId: -1,
    // };

    state = {
        disabled: false,
        ruleSystemId: null,
        accessPointId: null,
        step: 1,
        working: false,
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.prepareState(nextProps);
    }

    componentDidMount() {
        this.prepareState(this.props);
    }

    prepareState = props => {
        const {
            typeId,
            refTables: {
                recordTypes: {typeIdMap},
            },
        } = props;

        if (typeId && typeIdMap[typeId]) {
            change(AddRegistryForm.formName, 'type', typeIdMap[typeId])
            change(AddRegistryForm.formName, 'structured', typeIdMap[typeId].ruleSystemId != null)
        }
    };

    nextStep = () => {
        const {
            formValues,
            touchAll,
        } = this.props;

        const errors = AddRegistryForm.validate(formValues, this.props);

        if (Object.keys(errors).length > 0) {
            touchAll();
            return;
        }

        if (this.state.working) {
            return;
        }

        this.setState({working: true});

        WebApi.createStructuredAccessPoint(
            formValues.name,
            formValues.complement,
            formValues.languageCode,
            formValues.description,
            formValues.type.id,
            formValues.scopeId,
        ).then(data => {
            this.setState({
                accessPointId: data.accessPointId,
                step: 2,
                working: false,
            });
        });
    };

    add = () => {
        const {nameItemForm} = this.props;

        const formData = nameItemForm.formData;
        const itemTypes = [];
        const strictMode = true;

        let infoTypesMap = new Map(nameItemForm.infoTypesMap);

        formData.itemTypes.forEach(descItemType => {
            infoTypesMap.delete(descItemType.id);
        });

        nameItemForm.refTypesMap.forEach(refType => {
            if (infoTypesMap.has(refType.id)) {
                // ještě ji na formuláři nemáme
                const infoType = infoTypesMap.get(refType.id);
                // v nestriktním modu přidáváme všechny jinak jen možné
                if (!strictMode || infoType.type !== 'IMPOSSIBLE') {
                    // nový item type na základě původního z refTables
                    itemTypes.push(refType);
                }
            }
        });

        const descItemTypes = [
            {
                groupItem: true,
                id: 'DEFAULT',
                name: i18n('subNodeForm.descItemGroup.default'),
                children: itemTypes,
            },
        ];

        const submit = data => {
            this.props.dispatch(modalDialogHide());
            this.props.dispatch(accessPointFormActions.fundSubNodeFormDescItemTypeAdd(data.descItemTypeId.id));
        };

        // Modální dialog
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('subNodeForm.descItemType.title.add'),
                <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>,
            ),
        );
    };

    onBack = () => {
        const {accessPointId} = this.state;

        if (this.state.step === 2) {
            WebApi.deleteAccessPoint(accessPointId).then(() => {
                this.setState({
                    step: 1,
                    accessPointId: null,
                });
            });
        }
    };

    onClose = () => {
        const {onClose} = this.props;
        const {accessPointId} = this.state;

        if (this.state.step === 2 && accessPointId) {
            WebApi.deleteAccessPoint(accessPointId);
        }
        onClose && onClose();
    };

    render() {
        const {
            handleSubmit,
            change,
            blur,
            versionId,
            refTables: {apTypes},
            submitting,
            typeValue,
            structuredValue,
        } = this.props;

        const okSubmitForm = submitReduxFormWithProp.bind(this, AddRegistryForm.validate, 'store');
        const okAndDetailSubmitForm = submitReduxFormWithProp.bind(
            this,
            AddRegistryForm.validate,
            'storeAndViewDetail',
        );

        const value = getTreeItemById(typeValue ? typeValue.id : '', apTypes.items);
        const isStructured = !!(structuredValue);

        return (
            <div key={this.props.key}>
                <Form onSubmit={handleSubmit(okSubmitForm)}>
                    {this.state.step === 1 && (
                        <Modal.Body>
                            <Field
                                disabled={this.state.disabled || submitting}
                                versionId={versionId}
                                label={i18n('registry.scopeClass')}
                                name={'scopeId'}
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Scope}
                                passOnly
                            />
                            <Field
                                label={i18n('registry.add.type')}
                                items={apTypes.items}
                                tree
                                alwaysExpanded
                                allowSelectItem={item => item.addRecord}
                                name={'type'}
                                onChange={(item) => {
                                    change('structured', item && item.ruleSystemId != null);
                                }}
                                value={value}
                                disabled={this.state.disabled || submitting}
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Autocomplete}
                                passOnly
                            />
                            {!isStructured && (
                                <Field
                                    type="text"
                                    label={i18n('registry.name')}
                                    name={'name'}
                                    component={FormInputField}
                                    disabled={this.state.disabled || submitting}
                                />
                            )}
                            {!isStructured && (
                                <Field
                                    type="text"
                                    label={i18n('accesspoint.complement')}
                                    name={'complement'}
                                    component={FormInputField}
                                    disabled={this.state.disabled || submitting}
                                />
                            )}
                            <Field
                                label={i18n('accesspoint.languageCode')}
                                name={'languageCode'}
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={LanguageCodeField}
                                passOnly={true}
                                disabled={this.state.disabled || submitting}
                            />
                            {!isStructured && (
                                <Field
                                    as="textarea"
                                    label={i18n('accesspoint.description')}
                                    name={'description'}
                                    component={FormInputField}
                                    disabled={this.state.disabled || submitting}
                                />
                            )}
                        </Modal.Body>
                    )}
                    {this.state.step === 2 && (
                        <Modal.Body>
                            <NoFocusButton onClick={this.add}>
                                <Icon glyph="fa-plus-circle"/>
                                {i18n('subNodeForm.section.item')}
                            </NoFocusButton>
                            <ApItemNameForm
                                parent={{id: this.state.data.names[0].objectId, accessPointId: this.state.data.id}}
                            />
                        </Modal.Body>
                    )}
                    <Modal.Footer>
                        {(!isStructured || this.state.step === 2) && this.props.showSubmitTypes && (
                            <Button onClick={handleSubmit(okAndDetailSubmitForm)} disabled={submitting}>
                                {i18n('global.action.storeAndViewDetail')}
                            </Button>
                        )}
                        {(!isStructured || this.state.step === 2) && (
                            <Button type="submit" variant="outline-secondary" onClick={handleSubmit(okSubmitForm)}
                                    disabled={submitting}>
                                {i18n('global.action.store')}
                            </Button>
                        )}
                        {isStructured && this.state.step === 1 && (
                            <Button type="button" variant="outline-secondary" onClick={this.nextStep}
                                    disabled={this.state.working}>
                                {i18n('global.action.next')}
                            </Button>
                        )}
                        {this.state.step === 2 && (
                            <Button variant="link" onClick={this.onBack}>
                                {i18n('global.action.back')}
                            </Button>
                        )}
                        <Button variant="link" onClick={this.onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

const mapStateToProps = (state) => {
    const selector = formValueSelector(AddRegistryForm.formName);
    return {
        formValues: getFormValues(AddRegistryForm.formName)(state),
        nameItemForm: state.ap.nameItemForm,
        initialValues: {}, //state.form.addRegistryForm.initialValues,
        refTables: state.refTables,
        registryList: state.app.registryList,
        scopeIdValue: selector(state, 'scopeId'),
        typeValue: selector(state, 'type'),
        structuredValue: selector(state, 'structured'),
    };
}

export default connect(mapStateToProps)(reduxForm(
    {
        form: AddRegistryForm.formName,
        validate: AddRegistryForm.validate,
    }
)(AddRegistryForm));
