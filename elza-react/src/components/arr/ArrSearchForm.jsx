import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInput, i18n, Icon} from 'components/shared';
import DatationField from '../party/DatationField';
import {Col, Form, Modal, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx';

import './ArrSearchForm.scss';

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
    static propTypes = {};

    static validate(values, props) {
        const errors = {};

        errors.condition = [];
        values.condition.forEach((item, index) => {
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

    componentDidMount() {
        this.props.dispatch(calendarTypesFetchIfNeeded());
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(calendarTypesFetchIfNeeded());
    }

    renderFormItem = (condition, index) => {
        const {
            refTables: {calendarTypes},
        } = this.props;

        switch (condition.type.value) {
            case TYPE_TEXT: {
                return <FormInput type="text" {...condition.value} />;
            }

            case TYPE_UNITDATE: {
                return (
                    <div className="unitdate">
                        <div className="field">
                            <FormInput as="select" {...condition.condition}>
                                <option value={GE} key={GE}>
                                    {i18n('search.extended.form.unitdate.type.ge')}
                                </option>
                                <option value={LE} key={LE}>
                                    {i18n('search.extended.form.unitdate.type.le')}
                                </option>
                                <option value={CONTAINS} key={CONTAINS}>
                                    {i18n('search.extended.form.unitdate.type.contains')}
                                </option>
                            </FormInput>
                        </div>
                        <div className="field">
                            <FormInput as="select" {...condition.calendarTypeId}>
                                {calendarTypes &&
                                    calendarTypes.fetched &&
                                    calendarTypes.items.map((calendar, index) => {
                                        return (
                                            <option value={calendar.id} key={calendar.id}>
                                                {i18n('search.extended.form.unitdate.calendar.' + calendar.code)}
                                            </option>
                                        );
                                    })}
                            </FormInput>
                        </div>
                        <div className="text">
                            <FormInput type="text" {...condition.value} />
                        </div>
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
            fields: {type, text, condition},
            handleSubmit,
            submitting,
            onClose,
        } = this.props;

        const formForm = (
            <div className="arr-search-form-container">
                <Button className="action-button" onClick={() => condition.addField({type: TYPE_TEXT})}>
                    <Icon glyph="fa-plus" /> {i18n('search.extended.form.text')}
                </Button>
                <Button
                    className="action-button"
                    onClick={() => condition.addField({type: TYPE_UNITDATE, calendarTypeId: 1, condition: GE})}
                >
                    <Icon glyph="fa-plus" /> {i18n('search.extended.form.unitdate')}
                </Button>

                <div className="items">
                    {condition.map((conditionRow, index, self) => (
                        <div className="condition" key={'condition' + index}>
                            {this.renderFormItem(conditionRow, index)}
                            <Button className="delete" variant="action" onClick={() => self.removeField(index)}>
                                <Icon glyph="fa-times" />
                            </Button>
                        </div>
                    ))}
                </div>
            </div>
        );

        const textForm = (
            <div>
                <FormInput as="textarea" label={i18n('search.extended.input.text')} {...text} />
            </div>
        );

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <Row>
                        <Col xs={4}>
                            <FormInput
                                type="radio"
                                label={i18n('search.extended.type.form')}
                                {...type}
                                value={FORM_FORM}
                                checked={type.value === FORM_FORM}
                                onBlur={() => {}}
                            />
                        </Col>
                        <Col xs={4}>
                            <FormInput
                                type="radio"
                                label={i18n('search.extended.type.text')}
                                {...type}
                                value={FORM_TEXT}
                                checked={type.value === FORM_TEXT}
                                onBlur={() => {}}
                            />
                        </Col>
                    </Row>
                    {type.value === FORM_FORM && formForm}
                    {type.value === FORM_TEXT && textForm}
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

export default reduxForm(
    {
        form: 'searchForm',
        fields: [
            'type',
            'text',
            'condition[].type',
            'condition[].condition',
            'condition[].calendarTypeId',
            'condition[].value',
        ],
    },
    state => ({
        refTables: state.refTables,
    }),
)(ArrSearchForm);
