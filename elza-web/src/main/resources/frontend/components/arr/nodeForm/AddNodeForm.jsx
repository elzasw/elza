/**
 * Dialog pro přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 26.8.2016
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import * as types from 'actions/constants/ActionTypes.js';
import {AbstractReactComponent, i18n, FormInput, Loading} from 'components/index.jsx';
import {Modal, Button, Radio, FormGroup, ControlLabel} from 'react-bootstrap';
import {reduxForm} from 'redux-form';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

require ('./AddNodeForm.less');

const INIT_STATE = {
    items: undefined,
    loading: false,
};

const validate = (values, props) => {
    const errors = {};
    if (!values.direction) {
        errors.direction = i18n('global.validation.required');
    }

    return errors;
};

var AddNodeForm = class AddNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = INIT_STATE;
    }

    /**
     * Načte scénáře a vrátí je pro zobrazení
     */
    loadScenarios() {
        if (!this.state.loading) {
            this.setState({
                ...this.state,
                loading: true
            });
            const {node, version, direction} = this.props;
            WebApi.getNodeAddScenarios(node, version, direction).then((result) => {
                if (result.length > 1) {
                    this.setState({
                        ...this.state,
                        items: result,
                        loading: false
                    }, ()=>{this.focusFirstMenuItem()});
                } else if (result.length === 1) {
                    this.props.action(undefined, result[0].name);
                    this.setState(INIT_STATE);
                } else {
                    this.props.action();
                    this.setState(INIT_STATE);
                }
            });
        } else {
            this.setState({
                ...this.state,
                items: undefined,
            });
        }
    }

    render() {
        const {fields:{direction}, handleSubmit, onClose, initDirection} = this.props;
        const {items, loading} = this.state;
        const submitForm = submitReduxForm.bind(this, validate);

        return(
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <FormInput componentClass='select' label={i18n('arr.fund.addNode.direction')} {...direction} {...decorateFormField(direction)}>
                            <option />
                            <option value='before' key='before'>{i18n('arr.fund.addNode.before')}</option>
                            <option value='after' key='after'>{i18n('arr.fund.addNode.after')}</option>
                            <option value='child' key='child'>{i18n('arr.fund.addNode.child')}</option>
                            <option value='atEnd' key='atEnd'>{i18n('arr.fund.addNode.atEnd')}</option>
                        </FormInput>
                        <FormGroup>
                            <ControlLabel>{i18n('arr.fund.addNode.scenario')}</ControlLabel>
                            {loading ? <Loading /> :
                                <FormGroup>
                                    <Radio name='a'>a</Radio>
                                    <Radio name='a'>b</Radio>
                                </FormGroup>
                            }
                        </FormGroup>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        );
    }
};

AddNodeForm.propTypes = {
    node: React.PropTypes.object.isRequired,
    direction: React.PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND', ''])
};

module.exports = reduxForm({
    form: 'addNodeForm',
    fields: ['direction']
})(AddNodeForm);
