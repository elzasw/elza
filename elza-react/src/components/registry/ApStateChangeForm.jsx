import PropTypes from 'prop-types';
import React from 'react';
import {formValueSelector, reduxForm, Field} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField} from 'components/form/FormUtils.jsx';
import {getTreeItemById} from './registryUtils';
import Scope from '../shared/scope/Scope';
import * as StateApproval from '../enum/StateApproval';
import FormInputField from '../../components/shared/form/FormInputField';
import {connect} from "react-redux";

class ApStateChangeForm extends AbstractReactComponent {
    static validate = (values, props) => {
        const errors = {};

        if (!values.state) {
            errors.state = i18n('global.validation.required');
        }

        return errors;
    };

    static propTypes = {
        accessPointId: PropTypes.number.isRequired,
        partyTypeId: PropTypes.number,
        versionId: PropTypes.number,
        hideType: PropTypes.bool,
    };

    static defaultProps = {
        hideType: false,
    };

    getStateWithAll() {
        return StateApproval.values.map(item => {
            return {
                id: item,
                name: StateApproval.getCaption(item),
            };
        });
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        if (!this.props.scopeId) {
            const {refTables: {scopesData}, versionId} = this.props;
            let index = scopesData.scopes ? indexById(scopesData.scopes, versionId, 'versionId') : false;
            if (index && scopesData.scopes[index].scopes && scopesData.scopes[index].scopes[0].id) {
                this.props.change('scopeId', scopesData.scopes[index].scopes[0].id);
            }
        }
    }

    render() {
        const {
            handleSubmit,
            onClose,
            hideType,
            versionId,
            refTables: {scopesData, apTypes},
            submitting
        } = this.props;

        return (
            <div key={this.props.key}>
                <Form onSubmit={handleSubmit}>
                    <Modal.Body>
                        <Field
                            component={Scope}
                            disabled={submitting}
                            versionId={versionId}
                            label={i18n('ap.state.title.scope')}
                            name={"scopeId"}
                        />
                        {!hideType && (
                            <Field
                                component={Autocomplete}
                                label={i18n('ap.state.title.type')}
                                items={apTypes.items ? apTypes.items : []}
                                tree
                                alwaysExpanded
                                allowSelectItem={item => item.addRecord}
                                name={"typeId"}
                                useIdAsValue
                                disabled={submitting}
                            />
                        )}
                        <Field
                            component={FormInputField}
                            type="autocomplete"
                            disabled={submitting}
                            useIdAsValue
                            label={i18n('ap.state.title.state')}
                            items={this.getStateWithAll()}
                            name={"state"}
                        />
                        <Field
                            component={FormInputField}
                            disabled={submitting}
                            type="text"
                            label={i18n('ap.state.title.comment')}
                            name={"comment"}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary" disabled={submitting}>
                            {i18n('global.action.store')}
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            </div>
        );
    }
}

const mapStateToProps = (state) => {
    const selector = formValueSelector('apStateChangeForm');

    return {
        scopeId: selector(state, 'scopeId'),
        typeId: selector(state, 'typeId'),
        refTables: state.refTables,
    };
};

export default connect(mapStateToProps)(reduxForm({
    form: 'apStateChangeForm',
    // fields: ['comment', 'typeId', 'scopeId', 'state'],
    validate: ApStateChangeForm.validate,
})(ApStateChangeForm));
