// --
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n} from 'components/shared';
import * as perms from './../../actions/user/Permission.jsx';
import AddRemoveList from '../shared/list/AddRemoveList';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';
import SelectItemsForm from './SelectItemsForm';
import UserAndGroupField from './UserAndGroupField';
import {renderGroupItem, renderUserItem, renderUserOrGroupItem} from './adminRenderUtils';
import FundsPermissionPanel from './FundsPermissionPanel';

/**
 * Komponenta pro přiřazení spravovaných entit.
 */

class ControlledEntitiesPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: this.buildPermissions(props.permissions),
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        if (this.props.permissions !== nextProps.permissions) {
            this.setState({
                permissions: this.buildPermissions(nextProps.permissions),
            });
        }
    }

    buildPermissions = permissions => {
        if (!permissions) {
            return [];
        }

        // Zde potřebujeme pouze přímo přiřazná oprávnění, ne zděděná ze skupiny
        const permissionsList = permissions
            .filter(
                p =>
                    !p.inherited &&
                    (p.permission === perms.USER_CONTROL_ENTITITY || p.permission === perms.GROUP_CONTROL_ENTITITY),
            )
            .map(p => {
                return p;
            });

        return permissionsList;
    };

    renderItem = (item, isActive, index, onCheckItem) => {
        if (item.id === FundsPermissionPanel.ALL_ID) {
            return <div>{i18n('admin.perms.tabs.funds.items.fundAll')}</div>;
        } else {
            return <div>{item.fund.name}</div>;
        }
    };

    renderItem = props => {
        const {item, isActive} = props;
        if (item.permission === perms.USER_CONTROL_ENTITITY) {
            return renderUserItem({item: item.userControl, selected: isActive});
        } else if (item.permission === perms.GROUP_CONTROL_ENTITITY) {
            return renderGroupItem({item: item.groupControl, selected: isActive});
        }
    };

    handleAdd = () => {
        const {onAddPermission} = this.props;

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('admin.perm.advanced.control.entity.add.title'),
                <SelectItemsForm
                    onSubmitForm={items => {
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
                            if (!permissionsMap[item.id]) {
                                // jen pokud ještě přidaný není
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

                        onAddPermission(permissionsToAdd).then(data => {
                            const newPermissions = [...permissions, ...data];
                            this.setState({permissions: newPermissions});
                            this.props.dispatch(modalDialogHide());
                        });
                    }}
                    fieldComponent={UserAndGroupField}
                    renderItem={renderUserOrGroupItem}
                />,
            ),
        );
    };

    handleRemove = (item, index) => {
        const {onDeletePermission} = this.props;
        const {permissions} = this.state;
        const newPermissions = [...permissions.slice(0, index), ...permissions.slice(index + 1)];
        onDeletePermission(item).then(data => {
            this.setState({permissions: newPermissions});
        });
    };

    render() {
        const {permissions} = this.state;
        const {className} = this.props;

        return (
            <AddRemoveList
                label={i18n('admin.perms.tabs.advanced.controller.entities.title')}
                addInLabel
                className={className}
                items={permissions}
                renderItem={this.renderItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
            />
        );
    }
}

export default connect()(ControlledEntitiesPanel);
