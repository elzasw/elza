import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, Loading} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'

// require ('./UserDetail.less');

var GroupDetail = class GroupDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        // this.bindMethods('')
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }


    render() {
        const {groupDetail, focus} = this.props;

        if (groupDetail.id === null) {
            return <div className='group-detail-container'></div>
        }

        if (!groupDetail.fetched) {
            return <div className='group-detail-container'><Loading/></div>
        }

        return (
            <div className='group-detail-container'>
                {groupDetail.name}
            </div>
        );
    }
};

GroupDetail.propTypes = {
    groupDetail: React.PropTypes.object.isRequired
};

function mapStateToProps(state) {
    return {
    }
}

module.exports = connect(mapStateToProps)(GroupDetail);

