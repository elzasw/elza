import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {AbstractReactComponent, AddRemoveList, i18n, FormInput} from 'components/index.jsx';
import * as perms from 'actions/user/Permission.jsx';
import {permissionBlur, permissionAdd, permissionChange, permissionReceive, permissionRemove} from 'actions/admin/permission.jsx'

require('./Permissions.less');

const Permissions = class Permissions extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            "renderPermission",
            "handleChangePermission",
            "handleChangeValue",
            "handleAdd",
            "handleRemove",
        );
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    handleAdd() {
        const {area} = this.props;
        this.dispatch(permissionAdd(area));
    }
    
    handleRemove(permission, index) {
        const {area} = this.props;
        this.dispatch(permissionRemove(area, index));
    }
    
    handleChangePermission(permission, index, e) {
        const {area} = this.props;
        const value = e.target.value;
        this.dispatch(permissionChange(area, index, {...permission, permission: value}));
    }
    
    handleChangeValue(e) {
        const value = e.target.value;
    }

    renderPermission(permission, index) {
        const {area} = this.props;
        const permInfo = perms.all[permission.permission]

        const permInput = (
            <FormInput
                componentClass="select"
                value={permission.permission}
                onChange={this.handleChangePermission.bind(this, permission, index)}
                onBlur={() => this.dispatch(permissionBlur(area, index))}
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
                    <FormInput
                        type="text"
                        value={permission.fundId}
                        onChange={this.handleChangeValue.bind(this, permission, index)}
                        onBlur={() => this.dispatch(permissionBlur(area, index))}
                    />
                )
            } else if (permInfo.scope) {
                permValue = (
                    <FormInput
                        type="text"
                        value={permission.scopeId}
                        onChange={this.handleChangeValue.bind(this, permission, index)}
                        onBlur={() => this.dispatch(permissionBlur(area, index))}
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
        const {permissions, addTitle, removeTitle} = this.props;

        return (
            <AddRemoveList
                className="permissions-container"
                items={permissions}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                addTitle={addTitle}
                removeTitle={removeTitle}
                renderItem={this.renderPermission}
            />
        );
    }
};

Permissions.propTypes = {
    permissions: React.PropTypes.array.isRequired,
    area: React.PropTypes.string.isRequired,
};

function mapStateToProps(state) {
    return {
    }
}

module.exports = connect(mapStateToProps)(Permissions);

