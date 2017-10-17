// --
import React from 'react';
import {Checkbox} from "react-bootstrap";
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import getMapFromList from "../../shared/utils/getMapFromList";
import "./PermissionCheckboxsForm.less";
import TooltipTrigger from "../shared/tooltip/TooltipTrigger";

/**
 * Panel spravující oprávnění typu checkbox.
 */
class PermissionCheckboxsForm extends AbstractReactComponent {
    static PropTypes = {
        permCodes: React.PropTypes.array.isRequired,    // seznam kódů oprávnění
        onChangePermission: React.PropTypes.func.isRequired,    // callback při změně
        labelPrefix: React.PropTypes.string.isRequired,    // i18n prefix pro názvy položek
        permission: React.PropTypes.object.isRequired,    // oprávnění, které se edituje
        permissionAll: React.PropTypes.object,    // oprávnění pro all položky, pokud exisutje (a needituje se právě ono, tedy je naplněno pouze pokud permission !== permissionAll a vůbec permissionAll může existovat)
        permissionAllTitle: React.PropTypes.string,    // odkaz do resource textů jak se jmenuje zdroj all persmission
        groups: React.PropTypes.array,    // seznam přiřazených skupin
    };

    render() {
        const {permissionAllTitle, permissionAll, groups, permission, labelPrefix, onChangePermission, permCodes} = this.props;
        const groupMap = groups ? getMapFromList(groups) : {};

        return <div className="permission-checkbox-form">
            {permCodes.map(permCode => {
                const obj = permission[permCode] || {groupIds: {}};
                let checked = false;
                if (obj && obj.checked) {
                    checked = true;
                }

                // Odkomentovat jen pokud bychom chtěli zobrazovat "dědění" z položky "všechny ..."
                // let allChecked = (permissionAll && permissionAll[permCode]) ? permissionAll[permCode].checked : false;
                let allChecked = false;

                let infoIcon;
                let infoMessage;
                if (Object.keys(obj.groupIds).length > 0 || checked || allChecked) {
                    const groupNames = Object.keys(obj.groupIds).map(id => groupMap[id].name);
                    infoIcon = <Icon glyph="fa-check-circle-o"/>;

                    infoMessage = <div className="permission-checkbox-form-tooltip">
                        <div>{i18n("permission.activePermission.title")}</div>
                        <br/>
                        <div>{i18n("permission.source.title")}:</div>
                        <ul>
                            {groupNames.map(x => <li>{x}</li>)}
                            {allChecked && <li>{i18n(permissionAllTitle)}</li>}
                            {checked && <li>{i18n("permission.explicit.title")}</li>}
                        </ul>
                    </div>;
                } else {
                    infoIcon = <Icon glyph="fa-circle-o"/>;
                }

                if (infoMessage) {
                    infoIcon = <TooltipTrigger
                        key="info"
                        content={infoMessage}
                        holdOnHover
                        placement="auto"
                        showDelay={1}
                    >
                        {infoIcon}
                    </TooltipTrigger>
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
