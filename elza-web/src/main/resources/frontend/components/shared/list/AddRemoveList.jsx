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

require ('./AddRemoveList.less');

var AddRemoveList = class AddRemoveList extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {items, onRemove, onAdd, renderItem, addTitle, removeTitle} = this.props;

        const groups = items.map(item => {
            return (
                <div className="item">
                    {renderItem(item)}
                    <NoFocusButton className="remove" onClick={this.props.onRemove} title={i18n(removeTitle)}>
                        <Icon glyph="fa-remove"/>
                    </NoFocusButton>
                </div>
            )
        })

        return (
            <div className="list-add-remove-container">
                <div className="item-list-container">
                    {groups}
                </div>
                <div className="actions">
                    <NoFocusButton onClick={this.props.onAdd} title={i18n(addTitle)}>
                        <Icon glyph="fa-plus"/>
                    </NoFocusButton>
                </div>
            </div>
        )        
    }
};

AddRemoveList.propTypes = {
    items: React.PropTypes.array.isRequired,
    onAdd: React.PropTypes.func.isRequired,
    onRemove: React.PropTypes.func.isRequired,
    renderItem: React.PropTypes.func.isRequired,
    addTitle: React.PropTypes.string,
    removeTitle: React.PropTypes.string,
};

AddRemoveList.defaultProps = {
    addTitle: "global.action.add",
    removeTitle: "global.action.remove",
    renderItem: (item) => <div>{item.name}</div>
};

function mapStateToProps(state) {
    return {
    }
}

module.exports = connect(mapStateToProps)(AddRemoveList);

