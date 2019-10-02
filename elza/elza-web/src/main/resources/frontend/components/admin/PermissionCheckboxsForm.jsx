// --
import React from 'react';
import {Checkbox} from "react-bootstrap";
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import getMapFromList from "../../shared/utils/getMapFromList";
import "./PermissionCheckboxsForm.less";
import TooltipTrigger from "../shared/tooltip/TooltipTrigger";
import {modalDialogShow} from '../../actions/global/modalDialog.jsx'
import FundNodesList from "../arr/FundNodesList";
import FundNodesSelectForm from "../arr/FundNodesSelectForm";
import {connect} from "react-redux";
import {WebApi} from "../../actions";
import {FUND_ARR_NODE} from "../../actions/user/Permission";

/**
 * Panel spravující oprávnění typu checkbox.
 */
class PermissionCheckboxsForm extends AbstractReactComponent {
    static PropTypes = {
        permCodes: React.PropTypes.array.isRequired,    // seznam kódů oprávnění
        onChangePermission: React.PropTypes.func.isRequired,    // callback při změně
        onAddNodePermission: React.PropTypes.func.isRequired,    // callback při přidání oprávnění na JP
        onRemoveNodePermission: React.PropTypes.func.isRequired,    // callback při přidání oprávnění na JP
        labelPrefix: React.PropTypes.string.isRequired,    // i18n prefix pro názvy položek
        permission: React.PropTypes.object.isRequired,    // oprávnění, které se edituje
        permissionAll: React.PropTypes.object,    // oprávnění pro all položky, pokud exisutje (a needituje se právě ono, tedy je naplněno pouze pokud permission !== permissionAll a vůbec permissionAll může existovat)
        permissionAllTitle: React.PropTypes.string,    // odkaz do resource textů jak se jmenuje zdroj all persmission
        groups: React.PropTypes.array,    // seznam přiřazených skupin
        fundId: React.PropTypes.number,
    };


    constructor(props) {
        super(props);
        this.state = {
            nodes: []
        };
    }

    componentDidMount() {
        this.fetch(this.props);
    }

    componentWillReceiveProps(nextProps, nextState) {
        if (nextProps.fundId !== this.props.fundId || nextProps.permission !== this.props.permission) {
            this.fetch(nextProps);
        }
    }

    fetch = (props) => {
        if (props.fundId) {
            let nodes = [];
            const fundArrData = props.permission[FUND_ARR_NODE];

            if (fundArrData && fundArrData.ids) {
                nodes = fundArrData.ids;
            } else if (fundArrData && fundArrData.groupIds) {
                nodes = fundArrData.groupIds[props.groupId] ? fundArrData.groupIds[props.groupId][props.fundId] : [];
            }

            if (nodes && nodes.length > 0) {
                WebApi.findNodeByIds(props.fundId, nodes).then(nodes => {
                    this.setState({nodes});
                });
            } else {
                this.setState({nodes: []});
            }
        }
    };

    renderNodes = () => {
        return <FundNodesList
            nodes={this.state.nodes}
            addInLabel
            addLabel={"admin.perms.tabs.funds.perm.FUND_ARR_NODE.add"}
            onDeleteNode={this.handleRemoveNode}
            onAddNode={this.handleAddNodes}
            fundId={this.props.fundId}
        />
    };

    handleRemoveNode = (node) => {
        if (confirm(i18n("arr.fund.nodes.deleteNode"))) {
            this.props.onRemoveNodePermission(this.props.fundId, node);
        }
    };

    handleAddNodes = () => {
        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesSelectForm
                fundId={this.props.fundId}
                onSubmitForm={(ids, nodes) => {
                    this.props.onAddNodePermission(this.props.fundId, nodes);
                }}
            />))
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
                    const groupNames = Object.keys(obj.groupIds).map(id => groupMap[id] ? groupMap[id].name : "");
                    infoIcon = <Icon style={{color:"green"}} glyph="fa-check"/>;

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
                    infoIcon = <Icon style={{visibility:"hidden"}} glyph="fa-check"/>;
                }

                if (infoMessage) {
                    infoIcon = <TooltipTrigger
                        key="info"
                        content={infoMessage}
                        placement="left"
                        showDelay={1}
                    >
                        {infoIcon}
                    </TooltipTrigger>
                }


                return <div className="item-row">
                    {infoIcon}
                    <Checkbox inline checked={checked} onChange={e => onChangePermission(e, permCode)}>{i18n(`${labelPrefix}${permCode}`)}</Checkbox>
                </div>
            })}
            {this.props.fundId && this.renderNodes()}
        </div>
    }
}

export default connect()(PermissionCheckboxsForm);
