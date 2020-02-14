import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, StoreHorizontalLoader, FundDetailTree} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'

import './FundDetail.scss';

class FundDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleShowInArr')
    }

    componentDidMount() {
        this.props.dispatch(refInstitutionsFetchIfNeeded());
        this.props.dispatch(refRuleSetFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
    }

    handleShowInArr(version) {
        // Přepnutí na stránku pořádání
        this.props.dispatch(routerNavigate('/arr'))

        // Otevření archivního souboru
        const fund = this.props.fundDetail
        var fundObj = getFundFromFundAndVersion(fund, version);
        this.props.dispatch(selectFundTab(fundObj));
    }


    render() {
        const {fundDetail, focus, fundCount, refTables: {institutions, ruleSet}} = this.props;

        if (fundDetail.id === null) {
            return <div className='fund-detail-container'>
                        <div className="unselected-msg">
                            <div className="title">{fundCount > 0 ? i18n('fund.noSelection.title') : i18n('fund.emptyList.title')}</div>
                            <div className="msg-text">{fundCount > 0 ? i18n('fund.noSelection.message') : i18n('fund.emptyList.message') }</div>
                        </div>
                    </div>
        }

        let content;
        if (fundDetail.fetched) {
            const instIndex = indexById(institutions.items, fundDetail.institutionId);
            const institution = instIndex !== null ? institutions.items[instIndex].name : '';

            const activeVersionIndex = indexById(fundDetail.versions, null, 'lockChange');
            const ruleIndex = indexById(ruleSet.items, fundDetail.versions[activeVersionIndex].ruleSetId);
            const rule = ruleIndex !== null ? ruleSet.items[ruleIndex].name : '';
            const ver = fundDetail.versions[activeVersionIndex];
            content = (
                <div className='fund-detail-container'>
                    <div className='fund-detail-info'>
                        <h1>{fundDetail.name}</h1>
                        <div>
                            <label>{i18n('arr.fund.detail.internalCode')}:</label>
                            <span>{fundDetail.internalCode}</span>
                        </div>
                        <div>
                            <label>{i18n('arr.fund.detail.institution')}:</label>
                            <span>{institution}</span>
                        </div>

                        <div>
                            <label>{i18n('arr.fund.detail.ruleSet')}:</label>
                            <span>{rule}</span>
                        </div>
                        <Button className='fund-detail-button' onClick={this.handleShowInArr.bind(this, ver)}><Icon glyph="fa-folder-open" />&nbsp;{i18n('arr.fund.action.openInArr')}</Button>
                    </div>
                </div>
            );
        }

        return <div>
            <StoreHorizontalLoader store={fundDetail}/>
            {content}
        </div>
    }
};

FundDetail.propTypes = {
    fundDetail: PropTypes.object.isRequired,
    fundCount: PropTypes.number.isRequired
};

function mapStateToProps(state) {
    return {
        refTables: state.refTables
    }
}

export default connect(mapStateToProps)(FundDetail);

