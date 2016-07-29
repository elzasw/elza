import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button, Panel} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, Loading, FundDetailTree} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {routerNavigate} from 'actions/router.jsx'

require('./FundDetailExt.less');

const FundDetailExt = class FundDetailExt extends AbstractReactComponent {
    static PropTypes = {
        fundDetail: React.PropTypes.object.isRequired,
    }

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
        const fundObj = getFundFromFundAndVersion(fund, version);
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
        //             {id: 1, lockDate: 1460469000591},
        //             {id: 2, lockDate: 1460462260849},
        //         ]},
        //     {id: 2, code: 'ddd', name: 'nazev 2',
        //         outputs: [
        //             {id: 133, lockDate: 1464469000591},
        //             {id: 244, lockDate: 1465462260849},
        //         ]},
        // ]

        const validOutputs = fundDetail.validNamedOutputs.map((outputDefinition, index) => {
            return (
                <div className="output" key={index}>
                    <div className="output-label">{outputDefinition.name}</div>
                    <Button bsStyle="link">{i18n('arr.fund.outputDefinition.action.showPDF')}</Button>
                </div>
            )
        })

        const histOutputs = fundDetail.historicalNamedOutputs.map((outputDefinition, index) => {
            return (
                <div className="output with-versions"  key={index}>
                    <div className="output-label">{outputDefinition.name}</div>
                    <div className="versions-container">
                        {outputDefinition.outputs.map(output => (
                            <div className="version">
                                <div className="version-label">{i18n('arr.fund.outputDefinition.version', dateToString(new Date(output.lockDate)))}</div>
                                <Button bsStyle="link">{i18n('arr.fund.outputDefinition.action.showPDF')}</Button>
                            </div>
                        ))}
                    </div>
                </div>
            )
        })

        return (
            <div className='fund-detail-ext-container'>
                {validOutputs.length > 0 && <div className="outputs-container">
                    <h1>{i18n('arr.fund.outputDefinition.active')}</h1>
                    {validOutputs}
                </div>}
                {histOutputs.length > 0 && <div className="outputs-container">
                    <h1>{i18n('arr.fund.outputDefinition.hist')}</h1>
                    {histOutputs}
                </div>}
                <div className="versions-container">
                    <h1>{i18n('arr.fund.version.list')}</h1>
                    {fundDetail.versions.map((ver, index) => {
                        if (ver.lockDate) {
                            return (
                                <div className='fund-version' key={'fund-version-' +  index}>
                                    <div className="version-label">{i18n('arr.fund.version', dateToString(new Date(ver.lockDate)))}</div>
                                    <Button onClick={this.handleShowInArr.bind(this, ver)} bsStyle='link'>{i18n('arr.fund.action.showInArr')}</Button>
                                </div>
                            )
                        } else {
                            return (
                                <div className='fund-version' key={'fund-version-' +  index}>
                                    <div className="version-label">{i18n('arr.fund.currentVersion')}</div>
                                    <Button onClick={this.handleShowInArr.bind(this, ver)} bsStyle='link'>{i18n('arr.fund.action.openInArr')}</Button>
                                </div>
                            )
                        }
                    })}
                </div>
            </div>
        );
    }
}

module.exports = connect()(FundDetailExt);

