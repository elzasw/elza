import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button, Panel} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, Loading, FundDetailTree} from 'components';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils'
import {selectFundTab} from 'actions/arr/fund'
import {routerNavigate} from 'actions/router'

require ('./FundDetailExt.less');

var FundDetailExt = class FundDetailExt extends AbstractReactComponent {
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

        // fundDetail.validNamedOutputs = [
        //     {id: 1, code: 'ccc', name: 'nazev 1'},
        //     {id: 2, code: 'ddd', name: 'nazev 2'},
        // ]
        // fundDetail.validNamedOutputs = [
        //     {id: 1, code: 'ccc', name: 'nazev 1',
        //         outputs: [
        //             {id: 1, lockChange: {id: 111, changeDate: 1460469000591}},
        //             {id: 2, lockChange: {id: 222, changeDate: 1460462260849}},
        //         ]},
        //     {id: 2, code: 'ddd', name: 'nazev 2',
        //         outputs: [
        //             {id: 133, lockChange: {id: 1411, changeDate: 1464469000591}},
        //             {id: 244, lockChange: {id: 2242, changeDate: 1465462260849}},
        //         ]},
        // ]

        const validOutputs = fundDetail.validNamedOutputs.map(namedOutput => {
            return (
                <div className="output">
                    <div className="output-label">{namedOutput.name}</div>
                    <Button bsStyle="link">{i18n('arr.fund.namedOutput.action.showPDF')}</Button>
                </div>
            )
        })

        const histOutputs = fundDetail.validNamedOutputs.map(namedOutput => {
            return (
                <div className="output with-versions">
                    <div className="output-label">{namedOutput.name}</div>
                    <div className="versions-container">
                        {namedOutput.outputs.map(output => (
                            <div className="version">
                                <div className="version-label">{i18n('arr.fund.namedOutput.version', dateToString(new Date(output.lockChange.changeDate)))}</div>
                                <Button bsStyle="link">{i18n('arr.fund.namedOutput.action.showPDF')}</Button>
                            </div>
                        ))}
                    </div>
                </div>
            )
        })

        return (
            <div className='fund-detail-ext-container'>
                {validOutputs.length > 0 && <div className="outputs-container">
                    <h1>{i18n('arr.fund.namedOutput.active')}</h1>
                    {validOutputs}
                </div>}
                {histOutputs.length > 0 && <div className="outputs-container">
                    <h1>{i18n('arr.fund.namedOutput.hist')}</h1>
                    {histOutputs}
                </div>}
            </div>
        );
    }
}

FundDetailExt.propTypes = {
    fundDetail: React.PropTypes.object.isRequired,
}

module.exports = connect()(FundDetailExt);

