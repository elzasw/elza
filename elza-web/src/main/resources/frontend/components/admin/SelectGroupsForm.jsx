/**
 * Formular pro vybrani nekolika skupin pomoci tag input.
 */
import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import GroupField from "./GroupField.jsx"
import Tags from "components/form/Tags.jsx"
import {renderGroupItem} from "components/admin/adminRenderUtils.jsx"

var SelectGroupsForm = class SelectGroupsForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            "handleChange",
            "handleRemoveGroup",
        );

        this.state = {
            groups: [],
        };
    }

    componentWillReceiveProps(nextProps) {
    }

    handleRemoveGroup(group, groupIndex) {
        const {groups} = this.state;

        const index = indexById(groups, group.id);
        if (index !== null) {
            this.setState({
                groups: [
                    ...groups.slice(0, index),
                    ...groups.slice(index + 1)
                ]
            });
        }
    }

    handleChange(group) {
        const {groups} = this.state;

        const index = indexById(groups, group.id);
        if (index === null) {
            this.setState({
                groups: [...groups, group],
            });
        }
    }

    render() {
        const {onSubmitForm, onClose} = this.props;
        const {groups} = this.state;

        return (
            <div>
                <Modal.Body>
                    <div>
                        <GroupField
                            tags
                            onChange={this.handleChange}
                            />
                        <Tags
                            items={groups}
                            renderItem={renderGroupItem}
                            onRemove={this.handleRemoveGroup}
                        />
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={() => {onSubmitForm(groups)}}>{i18n('global.action.add')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = SelectGroupsForm



