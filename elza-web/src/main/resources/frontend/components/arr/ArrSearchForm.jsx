import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FundNodesSelectForm, i18n, FormInput, Icon} from 'components/index.jsx';
import {Modal, Button, FormGroup, Form} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {LazyListBox} from 'components/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {getScrollbarWidth, timeToString, dateToString} from 'components/Utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {dateTimeToLocalUTC} from "components/Utils"
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'

require("./ArrSearchForm.less");

const TYPE_TEXT = "TEXT";
const TYPE_UNITDATE = "UNITDATE";

const FORM_TEXT = "TEXT";
const FORM_FORM = "FORM";

const GE = "GE";
const LE = "LE";
const CONTAINS = "CONTAINS";

/**
 * Formulář zobrazení hostorie.
 */
class ArrSearchForm extends AbstractReactComponent {
    static PropTypes = {};

    constructor(props) {
        super(props);
    }

    static validate(values, props) {
        const errors = {};
        // TODO
        return errors;
    }

    componentDidMount() {
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    renderFormItem = (condition, index) => {

        const {refTables: {calendarTypes}} = this.props;

        switch (condition.type.value) {
            case TYPE_TEXT: {
                return <FormInput type="text" {...condition.value} />;
            }

            case TYPE_UNITDATE: {
                return <div>
                    <FormInput componentClass="select" {...condition.condition}>
                        <option value={GE} key={GE}>{i18n('search.extended.form.unitdate.type.ge')}</option>
                        <option value={LE} key={LE}>{i18n('search.extended.form.unitdate.type.le')}</option>
                        <option value={CONTAINS} key={CONTAINS}>{i18n('search.extended.form.unitdate.type.contains')}</option>
                    </FormInput>
                    <FormInput componentClass="select" {...condition.calendarId}>
                        {calendarTypes && calendarTypes.fetched && calendarTypes.items.map((calendar, index) => {
                            return <option value={calendar.id} key={calendar.id}>{i18n('search.extended.form.unitdate.calendar.' + calendar.code)}</option>
                        })}
                    </FormInput>
                    <FormInput type="text" {...condition.value} />
                </div>;
            }
        }

    };

    render() {

        const {
            fields: {
                type,
                text,
                condition
            },
            handleSubmit, submitting, onClose,
        } = this.props;

        const submitForm = submitReduxForm.bind(this, ArrSearchForm.validate);

        const formForm = <div>
            <Button onClick={() => condition.addField({type: TYPE_TEXT})}><Icon glyph="fa-plus" /> {i18n('search.extended.form.text')}</Button>
            <Button onClick={() => condition.addField({type: TYPE_UNITDATE, calendarId: 1, condition: GE})}><Icon glyph="fa-plus" /> {i18n('search.extended.form.unitdate')}</Button>

            {condition.map((conditionRow, index, self) => <div className="condition" key={'condition' + index}>
                {this.renderFormItem(conditionRow, index)}
                <Button bsStyle="action" onClick={()=>self.removeField(index)}><Icon glyph="fa-times"/></Button>
            </div>)}
        </div>;

        const textForm = <div>
            <FormInput componentClass="textarea" label={i18n('search.extended.input.text')} {...text} />
        </div>;

        return <Form onSubmit={handleSubmit(submitForm)}>
            <Modal.Body>
                <FormInput type="radio" label={i18n('search.extended.type.form')} {...type} value={FORM_FORM} checked={type.value === FORM_FORM} onBlur={()=>{}} />
                <FormInput type="radio" label={i18n('search.extended.type.text')} {...type} value={FORM_TEXT} checked={type.value === FORM_TEXT} onBlur={()=>{}} />
                {type.value === FORM_FORM && formForm}
                {type.value === FORM_TEXT && textForm}
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('search.extended.search')}</Button>
                <Button bsStyle="link" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
    form: 'searchForm',
    fields: ['type', 'text', 'condition[].type', 'condition[].condition', 'condition[].calendarId', 'condition[].value'],
}, state => ({
    refTables: state.refTables,
}))(ArrSearchForm);
