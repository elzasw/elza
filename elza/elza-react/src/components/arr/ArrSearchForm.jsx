import React from 'react';
import {Field, FieldArray, formValueSelector, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n, Icon} from 'components/shared';
import DatationField from '../party/DatationField';
import {Col, Form, Modal, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';

import './ArrSearchForm.scss';
import FormInputField from "../shared/form/FormInputField";
import {connect} from "react-redux";

const TYPE_TEXT = 'TEXT';
const TYPE_UNITDATE = 'UNITDATE';

const FORM_TEXT = 'TEXT';
const FORM_FORM = 'FORM';

const GE = 'GE';
const LE = 'LE';
const CONTAINS = 'CONTAINS';

/**
 * Formulář zobrazení historie.
 */
class ArrSearchForm extends AbstractReactComponent {
    static FORM = 'searchForm';

    static propTypes = {};

    static validate(values, props) {
        const errors = {};

        errors.condition = [];
        values.condition?.forEach((item, index) => {
            if (item.type === TYPE_UNITDATE) {
                errors.condition.push(DatationField.reduxValidate(item)); // FIXME ?: Removing Party
            } else {
                errors.condition.push(null);
            }
        });

        let deleteCondition = true;
        errors.condition.forEach(item => {
            if (item != null) {
                deleteCondition = false;
            }
        });

        if (deleteCondition) {
            delete errors.condition;
        }

        return errors;
    }

    renderFormItem = (item, index, fields) => {
        const {
            submitting
        } = this.props;

        switch (fields.get(index).type) {
            case TYPE_TEXT: {
                return <Field
                    disabled={submitting}
                    name={`${item}.value`}
                    type="text"
                    component={FormInputField}
                    label={false}
                />;
            }

            case TYPE_UNITDATE: {
                return (
                    <div className="unitdate">
                        <div className="field">
                            <Field
                                disabled={submitting}
                                name={`${item}.condition`}
                                type="select"
                                component={FormInputField}
                                label={false}
                            >
                                <option value={GE} key={GE}>
                                    {i18n('search.extended.form.unitdate.type.ge')}
                                </option>
                                <option value={LE} key={LE}>
                                    {i18n('search.extended.form.unitdate.type.le')}
                                </option>
                                <option value={CONTAINS} key={CONTAINS}>
                                    {i18n('search.extended.form.unitdate.type.contains')}
                                </option>
                            </Field>
                        </div>
                        <Field
                            disabled={submitting}
                            name={`${item}.value`}
                            type="text"
                            component={FormInputField}
                            label={false}
                        />
                    </div>
                );
            }
            default:
                return null;
        }
    };

    submitReduxForm = (values, dispatch) =>
        submitForm(ArrSearchForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    render() {
        const {
            handleSubmit,
            submitting,
            onClose,
            type,
        } = this.props;

        const formForm = (
            <div className="arr-search-form-container">
                <FieldArray
                    name={'condition'}
                    component={({fields, meta}) => {
                        return (
                            <>
                                <Button variant="outline-secondary"  className="action-button" onClick={() => fields.push({type: TYPE_TEXT})}>
                                    <Icon glyph="fa-plus" /> {i18n('search.extended.form.text')}
                                </Button>
                                <Button
                                    variant="outline-secondary"
                                    className="action-button"
                                    onClick={() => fields.push({type: TYPE_UNITDATE, condition: GE})}
                                >
                                    <Icon glyph="fa-plus" /> {i18n('search.extended.form.unitdate')}
                                </Button>

                                <div className="items">
                                    {fields.map((item, index, fields) => {
                                        return (
                                            <div className="condition" key={'condition' + index}>
                                                {this.renderFormItem(item, index, fields)}
                                                <Button className="delete" variant="action" onClick={() => fields.remove(index)}>
                                                    <Icon glyph="fa-times" />
                                                </Button>
                                            </div>
                                        );
                                    })}
                                </div>
                            </>);
                    }}
                />
            </div>
        );

        const textForm = (
            <div>
                <Field
                    name="text"
                    as="textarea"
                    component={FormInputField}
                    label={i18n('search.extended.input.text')}
                    disabled={submitting}
                />
            </div>
        );

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <Row>
                        <Col xs={6}>
                            <Field
                                component={FormInputField}
                                type="radio"
                                label={i18n('search.extended.type.form')}
                                name="type"
                                value={FORM_FORM}
                            />
                        </Col>
                        <Col xs={6}>
                            <Field
                                component={FormInputField}
                                type="radio"
                                label={i18n('search.extended.type.text')}
                                name="type"
                                value={FORM_TEXT}
                            />
                        </Col>
                    </Row>
                    {type === FORM_FORM && formForm}
                    {type === FORM_TEXT && textForm}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('search.extended.search')}
                    </Button>
                    <Button variant="link" onClick={onClose} disabled={submitting}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const form = reduxForm(
    {
        form: ArrSearchForm.FORM
    },
)(ArrSearchForm);

const selector = formValueSelector(ArrSearchForm.FORM);

export default connect(state => {
    return {
        refTables: state.refTables,
        type: selector(state, 'type'),
    };
})(form);
