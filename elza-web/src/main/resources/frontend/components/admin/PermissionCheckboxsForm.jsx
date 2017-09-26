// --
import React from 'react';
import {Checkbox} from "react-bootstrap";
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import getMapFromList from "../../shared/utils/getMapFromList";

/**
 * Panel spravující oprávnění typu checkbox.
 */
class PermissionCheckboxsForm extends AbstractReactComponent {
    static PropTypes = {
        permCodes: React.PropTypes.array.isRequired,    // seznam kódů oprávnění
        onChangePermission: React.PropTypes.func.isRequired,    // callback při změně
        labelPrefix: React.PropTypes.string.isRequired,    // i18n prefix pro názvy položek
        permission: React.PropTypes.object.isRequired,    // oprávnění, které se edituje
        groups: React.PropTypes.array.isRequired,    // seznam přiřazených skupin
    };

    render() {
        const {groups, permission, labelPrefix, onChangePermission, permCodes} = this.props;
        const groupMap = getMapFromList(groups);

        return <div>
            {permCodes.map(permCode => {
                const obj = permission[permCode] || {groupIds: {}};
                const checked = obj ? obj.checked : false;

                let infoIcon;
                if (Object.keys(obj.groupIds).length > 0) {
                    const groupNames = Object.keys(obj.groupIds).map(id => groupMap[id].name);
                    const groupNamesTitle = groupNames.join(", ");
                    infoIcon = <Icon title={groupNamesTitle} glyph="fa-check-circle-o"/>;
                } else {
                    infoIcon = <Icon glyph="fa-circle-o"/>;
                }

                return <div>
                    {infoIcon}
                    <Checkbox inline checked={checked} onChange={e => onChangePermission(e, permCode)}>{i18n(`${labelPrefix}${permCode}`)}</Checkbox>
                </div>
            })}
        </div>
    }
}

export default PermissionCheckboxsForm;
