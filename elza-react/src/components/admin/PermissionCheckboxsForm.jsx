// --
import PropTypes from 'prop-types';

import React from 'react';
import {FormCheck} from 'react-bootstrap';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import getMapFromList from '../../shared/utils/getMapFromList';
import './PermissionCheckboxsForm.scss';
import TooltipTrigger from '../shared/tooltip/TooltipTrigger';
import {modalDialogShow} from '../../actions/global/modalDialog.jsx';
import FundNodesList from '../arr/FundNodesList';
import FundNodesSelectForm from '../arr/FundNodesSelectForm';
import {connect} from 'react-redux';
import {WebApi} from '../../actions';
import {FUND_ARR_NODE} from '../../actions/user/Permission';

/**
 * Panel spravující oprávnění typu checkbox.
 */
class PermissionCheckboxsForm extends AbstractReactComponent {
    static propTypes = {
        permCodes: PropTypes.array.isRequired, // seznam kódů oprávnění
        onChangePermission: PropTypes.func.isRequired, // callback při změně
        onAddNodePermission: PropTypes.func.isRequired, // callback při přidání oprávnění na JP
        onRemoveNodePermission: PropTypes.func.isRequired, // callback při přidání oprávnění na JP
        labelPrefix: PropTypes.string.isRequired, // i18n prefix pro názvy položek
        permission: PropTypes.object.isRequired, // oprávnění, které se edituje
        permissionAll: PropTypes.object, // oprávnění pro all položky, pokud exisutje (a needituje se právě ono, tedy je naplněno pouze pokud permission !== permissionAll a vůbec permissionAll může existovat)
        permissionAllTitle: PropTypes.string, // odkaz do resource textů jak se jmenuje zdroj all persmission
        groups: PropTypes.array, // seznam přiřazených skupin
        fundId: PropTypes.number,
    };

    constructor(props) {
        super(props);
        this.state = {
            nodes: [],
        };
    }

    componentDidMount() {
        this.fetch(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps, nextState) {
        if (nextProps.fundId !== this.props.fundId || nextProps.permission !== this.props.permission) {
            this.fetch(nextProps);
        }
    }

    fetch = props => {
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
        return (
            <FundNodesList
                nodes={this.state.nodes}
                addInLabel
                addLabel={'admin.perms.tabs.funds.perm.FUND_ARR_NODE.add'}
                onDeleteNode={this.handleRemoveNode}
                onAddNode={this.handleAddNodes}
                fundId={this.props.fundId}
            />
        );
    };

    handleRemoveNode = node => {
        if (window.confirm(i18n('arr.fund.nodes.deleteNode'))) {
            this.props.onRemoveNodePermission(this.props.fundId, node);
        }
    };

    handleAddNodes = () => {
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.nodes.title.select'),
                <FundNodesSelectForm
                    fundId={this.props.fundId}
                    onSubmitForm={(ids, nodes) => {
                        this.props.onAddNodePermission(this.props.fundId, nodes);
                    }}
                />,
            ),
        );
    };

    render() {
        const {permissionAllTitle, groups, permission, labelPrefix, onChangePermission, permCodes} = this.props;
        const groupMap = groups ? getMapFromList(groups) : {};

        return (
            <div className="permission-checkbox-form">
                <div className="items-left">
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
                            const groupNames = Object.keys(obj.groupIds).map(id =>
                                groupMap[id] ? groupMap[id].name : '',
                            );
                            infoIcon = <Icon style={{color: 'green'}} glyph="fa-check" />;

                            infoMessage = (
                                <div className="permission-checkbox-form-tooltip">
                                    <div>{i18n('permission.activePermission.title')}</div>
                                    <br />
                                    <div>{i18n('permission.source.title')}:</div>
                                    <ul>
                                        {groupNames.map(x => (
                                            <li>{x}</li>
                                        ))}
                                        {allChecked && <li>{i18n(permissionAllTitle)}</li>}
                                        {checked && <li>{i18n('permission.explicit.title')}</li>}
                                    </ul>
                                </div>
                            );
                        } else {
                            infoIcon = <Icon style={{visibility: 'hidden'}} glyph="fa-check" />;
                        }

                        if (infoMessage) {
                            infoIcon = (
                                <TooltipTrigger key="info" content={infoMessage} placement="left" showDelay={1}>
                                    {infoIcon}
                                </TooltipTrigger>
                            );
                        }

                        return (
                            <div className="item-row">
                                {infoIcon}
                                <FormCheck inline checked={checked} onChange={e => onChangePermission(e, permCode)}>
                                    {i18n(`${labelPrefix}${permCode}`)}
                                </FormCheck>
                            </div>
                        );
                    })}
                </div>
                {this.props.fundId && <div className="items-right">{this.renderNodes()}</div>}
            </div>
        );
    }
}

export default connect()(PermissionCheckboxsForm);
