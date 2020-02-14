/**
 * Formulář inline editace oprávnění.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, AddRemoveList, i18n, FormInput} from 'components/shared';
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import {initForm} from "actions/form/inlineForm.jsx"
import {indexById} from 'stores/app/utils.jsx'
import FundField from './FundField.jsx'
import ScopeField from './ScopeField.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'

import './Permissions.scss';;

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
            scopes: this.getScopes(props.scopesData)
        };
    }

    componentDidMount() {
        this.props.initForm(this.props.onSave);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState({
            scopes: this.getScopes(nextProps.scopesData)
        });
    }
    /**
     * Získá pole dostupných scope ze store. Pokud ve store žádný neexistuje, načte je ze serveru.
     * @param {object} scopesData
     * @return {array}
     */
    getScopes(scopesData) {
        var versionId = -1;
        if(!scopesData.scopes){
            this.props.dispatch(requestScopesIfNeeded(versionId));
        }
        const scopeIndex = indexById(scopesData.scopes, versionId, 'versionId');
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
        permissions.addField({
            permission: null,
            fund: null,
            scope: null
        });
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

    renderPermission(props) {
        const {scopes} = this.state;
        const {item, index} = props;
        const permInfo = perms.all[item.permission.value];
        const permInput = (
            <FormInput
                componentClass="select"
                {...item.permission}
                onChange={this.handlePermissionChange.bind(this, item)}
                inline
            >
                <option />
                {Object.keys(perms.all).map(perm => {
                    return <option value={perm}>{i18n("permission." + perm)}</option>;
                })}
            </FormInput>
        );

        var permValue;
        if (permInfo && (permInfo.fund || permInfo.scope)) {
            if (permInfo.fund) {
                permValue = (
                    <FundField
                        {...item.fund}
                        inline
                    />
                );
            } else if (permInfo.scope) {
                permValue = (
                    <ScopeField
                        type="text"
                        {...item.scope}
                        inline
                        scopes={scopes}
                    />
                );
            }
        } else {
            permValue = <div className="form-group"></div>;
        }

        return (
            <div className="permission-container">
                {permInput}
                {permValue}
            </div>
        );
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

    static propTypes = {
        area: PropTypes.string.isRequired,
        scopesData: PropTypes.object.isRequired,
        addTitle: PropTypes.string.isRequired,
        removeTitle: PropTypes.string.isRequired,
        initData: PropTypes.object,
        onSave: PropTypes.func.isRequired,
    }
};


const fields = [
    "permissions[].id",
    "permissions[].permission",
    "permissions[].fund",
    "permissions[].scope",
];
export default reduxForm({
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
