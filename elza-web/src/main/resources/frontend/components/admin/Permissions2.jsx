/**
 * Formulář inline editace oprávnění.
 */

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, AddRemoveList, i18n, FormInput} from 'components/index.jsx';
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import {initForm} from "actions/form/inlineForm.jsx"
import {indexById} from 'stores/app/utils.jsx'
import FundField from './FundField.jsx'
import ScopeField from './ScopeField.jsx'
import * as perms from 'actions/user/Permission.jsx';

require('./Permissions.less');

/**
 * Validace formuláře.
 */
const validate = (values, props) => {
    const errors = {};

    errors.permissions = values.permissions.map(permission => {
        var result = {}

        if (!permission.permission) {
            result.permission = i18n("global.validation.required");
        } else {
            const permInfo = perms.all[permission.permission]
            if (permInfo) {
                if (permInfo.fund && !permission.fund) {
                    result.fund = i18n("global.validation.required");
                }
                if (permInfo.scope && !permission.scope) {
                    result.scope = i18n("global.validation.required");
                }
            }
        }

        return result;
    });

    return errors;
};

const Permissions2 = class Permissions2 extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            "renderPermission",
            "handleAdd",
            "handleRemove",
            "handlePermissionChange",
        );

        this.state = {
            scopes: this.getScopes(props)
        };
    }

    componentDidMount() {
        this.props.initForm(this.props.onSave);
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            scopes: this.getScopes(nextProps)
        });
    }

    getScopes(props) {
        const {scopesData} = props;
        const scopeIndex = indexById(scopesData.scopes, null, 'versionId');
        let scopes;
        if (scopeIndex !== null) {
            scopes = scopesData.scopes[scopeIndex].scopes;
        } else {
            scopes = [];
        }
        return scopes;
    }

    handleAdd() {
        const {fields: {permissions}} = this.props;
        permissions.addField({});
    }

    handleRemove(permission, index) {
        const {fields: {permissions}} = this.props;
        permissions.removeField(index);
    }

    handlePermissionChange(permission, event) {
        // Při změně případně vynulujeme doplňující hodnoty - např. odkaz na fund nebo scope
        permission.fund.onChange(null);
        permission.scope.onChange(null);

        // Vlastní změna hodnoty permission
        permission.permission.onChange(event);
    }

    renderPermission(permission, index) {
        const {scopes} = this.state;
        const permInfo = perms.all[permission.permission.value]
        const permInput = (
            <FormInput
                componentClass="select"
                {...permission.permission}
                onChange={this.handlePermissionChange.bind(this, permission)}
                inline
            >
                <option />
                {Object.keys(perms.all).map(perm => {
                    return <option value={perm}>{i18n("permission." + perm)}</option>
                })}
            </FormInput>
        )

        var permValue;
        if (permInfo && (permInfo.fund || permInfo.scope)) {
            if (permInfo.fund) {
                permValue = (
                    <FundField
                        {...permission.fund}
                        inline
                    />
                )
            } else if (permInfo.scope) {
                permValue = (
                    <ScopeField
                        type="text"
                        {...permission.scope}
                        inline
                        scopes={scopes}
                    />
                )
            }
        } else {
            permValue = <div className="form-group"></div>
        }

        return (
            <div className="permission-container">
                {permInput}
                {permValue}
            </div>
        )
    }

    render() {
        const {fields: {permissions}, addTitle, removeTitle} = this.props;

        return (
            <AddRemoveList
                className="permissions-container"
                items={[...permissions]}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                addTitle={addTitle}
                removeTitle={removeTitle}
                renderItem={this.renderPermission}
            />
        );
    }
};

Permissions2.propTypes = {
    area: React.PropTypes.string.isRequired,
    scopesData: React.PropTypes.object.isRequired,
    addTitle: React.PropTypes.string.isRequired,
    removeTitle: React.PropTypes.string.isRequired,
    initData: React.PropTypes.object,
    onSave: React.PropTypes.func.isRequired,
};

const fields = [
    "permissions[].permission",
    "permissions[].fund",
    "permissions[].scope",
];
module.exports = reduxForm({
        form: 'permissionsEditForm',
        fields,
        validate,
    },(state, props) => {
        return {
            initialValues: props.initData,
            scopesData: state.refTables.scopesData,
        }
    },
    {initForm: (onSave) => (initForm("permissionsEditForm", validate, onSave))}
)(Permissions2);