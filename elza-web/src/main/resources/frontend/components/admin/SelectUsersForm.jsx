/**
 * Formular pro vybrani nekolika uzivatelu pomoci tag input.
 */
import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import UserField from "./UserField.jsx"
import Tags from "components/form/Tags.jsx"
import {renderUserItem} from "components/admin/adminRenderUtils.jsx"

const SelectUsersForm = class SelectUsersForm extends AbstractReactComponent {
    static PropTypes = {
        excludedGroupId: React.PropTypes.number
    };

    static defaultProps = {
        excludedGroupId: null
    };

    constructor(props) {
        super(props);

        this.bindMethods(
            "handleChange",
            "handleRemoveUser",
        );

        this.state = {
            users: [],
        };
    }

    handleRemoveUser(user, userIndex) {
        const {users} = this.state;

        const index = indexById(users, user.id);
        if (index !== null) {
            this.setState({
                users: [
                    ...users.slice(0, index),
                    ...users.slice(index + 1)
                ]
            });
        }
    }

    handleChange(user) {
        const {users} = this.state;

        const index = indexById(users, user.id);
        if (index === null) {
            this.setState({
                users: [...users, user],
            });
        }
    }

    render() {
        const {onSubmitForm, onClose, excludedGroupId} = this.props;
        const {users} = this.state;

        return (
            <div>
                <Modal.Body>
                    <div>
                        <UserField
                            tags
                            onChange={this.handleChange}
                            excludedGroupId={excludedGroupId}
                        />
                        <Tags
                            items={users}
                            renderItem={renderUserItem}
                            onRemove={this.handleRemoveUser}
                        />
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={() => {onSubmitForm(users)}}>{i18n('global.action.add')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default SelectUsersForm



