// --
import React from 'react';
import {connect} from "react-redux";
import {AbstractReactComponent, i18n} from 'components/shared';
import * as perms from './../../actions/user/Permission.jsx';
import AddRemoveListBox from "../shared/listbox/AddRemoveListBox";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import SelectItemsForm from "./SelectItemsForm";
import UserAndGroupField from "./UserAndGroupField";
import {renderGroupItem, renderUserItem, renderUserOrGroupItem} from "./adminRenderUtils";

/**
 * Komponenta pro přiřazení spravovaných entit.
 */

class ControlledEntitiesPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: this.buildPermissions(props.permissions)
        };
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.permissions !== nextProps.permissions) {
            this.setState({
                permissions: this.buildPermissions(nextProps.permissions)
            });
        }
    }

    buildPermissions = (permissions) => {
        if (!permissions) {
            return [];
        }

        // Zde potřebujeme pouze přímo přiřazná oprávnění, ne zděděná ze skupiny
        const permissionsList = permissions
            .filter(p => !p.inherited && (p.permission === perms.USER_CONTROL_ENTITITY || p.permission === perms.GROUP_CONTROL_ENTITITY))
            .map(p => {
                return p;
        });

        return permissionsList;
    };

    renderItem = (item, isActive, index, onCheckItem) => {
        if (item.id === FundsPermissionPanel.ALL_ID) {
            return <div>{i18n("admin.perms.tabs.funds.items.fundAll")}</div>;
        } else {
            return <div>
                {item.fund.name}
            </div>;
        }
    };

    renderItem = (item, isActive, index, onCheckItem) => {
        if (item.permission === perms.USER_CONTROL_ENTITITY) {
            return renderUserItem(item.userControl, isActive, index, onCheckItem);
        } else if (item.permission === perms.GROUP_CONTROL_ENTITITY) {
            return renderGroupItem(item.groupControl, isActive, index, onCheckItem);
        }
    };

    handleAdd = () => {
        const {onAddPermission} = this.props;

        this.props.dispatch(modalDialogShow(this, i18n('admin.perm.advanced.control.entity.add.title'),
            <SelectItemsForm
                onSubmitForm={(items) => {
                    const {permissions} = this.state;
                    const permissionsMap = {};
                    permissions.forEach(p => {
                        if (p.permission === perms.USER_CONTROL_ENTITITY) {
                            permissionsMap[`u-${p.userControl.id}`] = p;
                        } else if (p.permission === perms.GROUP_CONTROL_ENTITITY) {
                            permissionsMap[`g-${p.groupControl.id}`] = p;
                        }
                    });

                    let permissionsToAdd = [];
                    items.forEach(item => {
                        if (!permissionsMap[item.id]) { // jen pokud ještě přidaný není
                            if (item.user) {
                                let p = {
                                    permission: perms.USER_CONTROL_ENTITITY,
                                    userControl: item.user,
                                };
                                permissionsToAdd.push(p);
                            } else if (item.group) {
                                let p = {
                                    permission: perms.GROUP_CONTROL_ENTITITY,
                                    groupControl: item.group,
                                };
                                permissionsToAdd.push(p);
                            }
                        }
                    });

                    onAddPermission(permissionsToAdd)
                        .then(data => {
                            const newPermissions = [...permissions, ...data];
                            this.setState({permissions: newPermissions});
                            this.props.dispatch(modalDialogHide());
                        });
                }}
                fieldComponent={UserAndGroupField}
                renderItem={renderUserOrGroupItem}
            />
        ));
    };

    handleRemove = (item, index) => {
        const {onDeletePermission} = this.props;
        const {permissions} = this.state;
        const newPermissions = [
            ...permissions.slice(0, index),
            ...permissions.slice(index + 1)
        ];
        onDeletePermission(item)
            .then(data => {
                this.setState({permissions: newPermissions});
            });
    };

    render() {
        const {permissions} = this.state;
        const {className} = this.props;

        return <AddRemoveListBox
                className={className}
                items={permissions}
                renderItemContent={this.renderItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
            />;
    }
}

export default connect()(ControlledEntitiesPanel);
