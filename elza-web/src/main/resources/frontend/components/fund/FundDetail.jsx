import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, Loading, FundDetailTree} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'

require ('./FundDetail.less');

var FundDetail = class FundDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleShowInArr')
    }

    componentDidMount() {
        this.dispatch(refInstitutionsFetchIfNeeded());
        this.dispatch(refRuleSetFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
    }

    handleShowInArr(version) {
        // Přepnutí na stránku pořádání
        this.dispatch(routerNavigate('/arr'))

        // Otevření archivního souboru
        const fund = this.props.fundDetail
        var fundObj = getFundFromFundAndVersion(fund, version);
        this.dispatch(selectFundTab(fundObj));
    }


    render() {
        const {fundDetail, focus, refTables: {institutions, ruleSet}} = this.props;

        if (fundDetail.id === null) {
            return <div className='fund-detail-container'></div>
        }

        if (!fundDetail.fetched) {
            return <div className='fund-detail-container'><Loading/></div>
        }

        const instIndex = indexById(institutions.items, fundDetail.institutionId);
        const institution = instIndex !== null ? institutions.items[instIndex].name : '';

        const activeVersionIndex = indexById(fundDetail.versions, null, 'lockChange');
        const ruleIndex = indexById(ruleSet.items, fundDetail.versions[activeVersionIndex].ruleSetId);
        const rule = instIndex !== null ? ruleSet.items[ruleIndex].name : '';

        return (
            <div className='fund-detail-container'>
                <FundDetailTree
                    className='fund-detail-tree'
                    fund = {fundDetail}
                    cutLongLabels={true}
                    versionId={fundDetail.versionId}
                    {...fundDetail.fundTree}
                    ref='tree'
                    focus={focus}
                />

                <div className='fund-detail-info'>
                    <small>{i18n('arr.fund.detail')}</small>
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
                    <hr className="fund-detail-split" />
                    <div className="versions-container">
                        {fundDetail.versions.map(ver => {
                            if (ver.lockDate) {
                                return (
                                    <div className='fund-version'>
                                        <div className="version-label">{i18n('arr.fund.version', dateToString(new Date(ver.lockDate)))}</div>
                                        <Button onClick={this.handleShowInArr.bind(this, ver)} bsStyle='link'>{i18n('arr.fund.action.showInArr')}</Button>
                                    </div>
                                )
                            } else {
                                return (
                                    <div className='fund-version'>
                                        <div className="version-label">{i18n('arr.fund.currentVersion')}</div>
                                        <Button onClick={this.handleShowInArr.bind(this, ver)} bsStyle='link'>{i18n('arr.fund.action.showInArr')}</Button>
                                    </div>
                                )
                            }
                        })}
                    </div>
                </div>
            </div>
        );
    }
};

FundDetail.propTypes = {
    fundDetail: React.PropTypes.object.isRequired
};

function mapStateToProps(state) {
    return {
        refTables: state.refTables
    }
}

module.exports = connect(mapStateToProps)(FundDetail);

