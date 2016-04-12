import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, Loading, FundDetailTree} from 'components';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils'
import {selectFundTab} from 'actions/arr/fund'
import {routerNavigate} from 'actions/router'

require ('./FundDetail.less');

var FundDetail = class FundDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleShowInArr')
    }

    componentDidMount() {
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
        const {fundDetail, focus} = this.props

        if (fundDetail.id === null) {
            return <div className='fund-detail-container'></div>
        }

        if (fundDetail.fetching || !fundDetail.fetched) {
            return <div className='fund-detail-container'><Loading/></div>
        }

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
                    {fundDetail.versions.map(ver => {
                        if (ver.lockDate) {
                            return (
                                <div className='fund-version'>
                                    <label>{i18n('arr.fund.version', dateToString(new Date(ver.createDate)))}</label>
                                    <Button onClick={this.handleShowInArr.bind(this, ver)} bsStyle='link'>{i18n('arr.fund.action.showInArr')}</Button>
                                </div>
                            )
                        } else {
                            return (
                                <div className='fund-version'>
                                    <label>{i18n('arr.fund.currentVersion')}</label>
                                    <Button onClick={this.handleShowInArr.bind(this, ver)} bsStyle='link'>{i18n('arr.fund.action.showInArr')}</Button>
                                </div>
                            )
                        }
                    })}
                </div>
            </div>
        );
    }
}

FundDetail.propTypes = {
    fundDetail: React.PropTypes.object.isRequired,
}

module.exports = connect()(FundDetail);

