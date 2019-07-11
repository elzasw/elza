import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n} from 'components/shared';
import {Button, Form, Modal} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRecordTypes.jsx'
import {getTreeItemById} from "./registryUtils";
import Scope from "../shared/scope/Scope";
import * as StateApproval from "../enum/StateApproval";
import FormInput from "../shared/form/FormInput";

class ApStateChangeForm extends AbstractReactComponent {

    static validate = (values, props) => {
        const errors = {};

        if (!values.state) {
            errors.state = i18n('global.validation.required');
        }

        if (!values.comment) {
            errors.comment = i18n('global.validation.required');
        }

        return errors;
    };

    static propTypes = {
        accessPointId: React.PropTypes.number.isRequired,
    };

    getStateWithAll() {
        return StateApproval.values.map(item => {
            return {
                id: item,
                name: StateApproval.getCaption(item)
            }
        });
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(getRegistryRecordTypesIfNeeded());
    }

    componentDidMount() {
        this.props.dispatch(getRegistryRecordTypesIfNeeded());
    }


    render() {
        const {fields: {typeId, scopeId, state, comment}, handleSubmit, onClose, versionId, refTables: {scopesData}, submitting, registryRegionRecordTypes} = this.props;

        const items = registryRegionRecordTypes.item ? registryRegionRecordTypes.item : [];

        let scopeIdValue = scopeId.value;
        if (!scopeId.value) {
            let index = scopesData.scopes ? indexById(scopesData.scopes, versionId, 'versionId') : false;
            if (index && scopesData.scopes[index].scopes) {
                scopeIdValue = scopesData.scopes[index].scopes[0].id;
            }
        }

        const value = getTreeItemById(typeId ? typeId.value : "", items);

        return (
            <div key={this.props.key}>
                <Form onSubmit={handleSubmit}>
                    <Modal.Body>
                        <Scope disabled={submitting} versionId={versionId} label={i18n('ap.state.title.scope')} {...scopeId} value={scopeIdValue} {...decorateFormField(scopeId)}/>
                        <Autocomplete
                            label={i18n('ap.state.title.type')}
                            items={items}
                            tree
                            alwaysExpanded
                            allowSelectItem={(item) => item.addRecord}
                            {...typeId}
                            {...decorateFormField(typeId)}
                            onChange={item => {
                                typeId.onChange(item ? item.id : null);
                            }}
                            onBlur={item => {
                                typeId.onBlur(item ? item.id : null);
                            }}
                            value={value}
                            disabled={submitting}
                            />
                        <Autocomplete
                            label={i18n('ap.state.title.state')}
                            items={this.getStateWithAll()}
                            {...state}
                            onChange={item => {
                                state.onChange(item ? item.id : null);
                            }}
                            onBlur={item => {
                                state.onBlur(item ? item.id : null);
                            }}
                        />
                        <FormInput type="text" label={i18n('ap.state.title.comment')} {...comment} {...decorateFormField(comment)}/>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }
}
export default reduxForm({
    form: 'apStateChangeForm',
    fields: ['comment', 'typeId', 'scopeId', 'state'],
    validate: ApStateChangeForm.validate
},state => ({
    refTables: state.refTables,
    registryRegionRecordTypes: state.registryRegionRecordTypes
}))(ApStateChangeForm);
