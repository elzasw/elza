import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {Icon, AbstractReactComponent, NoFocusButton, i18n, Loading} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {usersUserDetailFetchIfNeeded} from 'actions/admin/user.jsx'

import './AddRemoveList.less';

export default class AddRemoveList extends AbstractReactComponent {

    static propTypes = {
        items: React.PropTypes.array.isRequired,
        onAdd: React.PropTypes.func.isRequired,
        onRemove: React.PropTypes.func.isRequired,
        renderItem: React.PropTypes.func.isRequired,
        addTitle: React.PropTypes.string,
        removeTitle: React.PropTypes.string,
        readOnly: React.PropTypes.bool.isRequired,
    };

    static defaultProps = {
        addTitle: "global.action.add",
        removeTitle: "global.action.remove",
        readOnly: false,
        renderItem: (item, index) => <div key={"rendered-item-" + index}>{item.name}</div>
    };

    handleRemove = (item, index) => {
        const {onRemove} = this.props;
        onRemove(item, index);
    };

    render() {
        const {items, readOnly, className, onAdd, renderItem, addTitle, removeTitle} = this.props;

        const groups = items == null ? [] : items.map((item, index) => {
            return (
                <div className="item-container" key={"item-" + index}>
                    {renderItem(item, index)}
                    {!readOnly && <div className="item-actions-container">
                        <NoFocusButton className="remove" onClick={this.handleRemove.bind(this, item, index)} title={i18n(removeTitle)}>
                            <Icon glyph="fa-remove"/>
                        </NoFocusButton>
                    </div>}
                </div>
            )
        });

        return (
            <div className={className ? "list-add-remove-container " + className : "list-add-remove-container"}>
                <div className="item-list-container">
                    {groups}
                </div>
                {!readOnly && <div className="actions-container">
                    <NoFocusButton onClick={onAdd} title={i18n(addTitle)}>
                        <Icon glyph="fa-plus"/>
                    </NoFocusButton>
                </div>}
            </div>
        )        
    }
};
