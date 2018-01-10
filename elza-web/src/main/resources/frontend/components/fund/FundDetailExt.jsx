import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button, Panel} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, FundDetailTree} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {downloadFile} from "actions/global/download";
import {UrlFactory} from 'actions/index.jsx';

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

    handleDownload = (id) => {
        this.dispatch(downloadFile("arr-output-file-" + id, UrlFactory.downloadOutputResult(id)));
    };

    render() {
        const {fundDetail, focus} = this.props

        if (fundDetail.id === null) {
            return <div className='fund-detail-container'></div>
        }

        const validOutputs = fundDetail.validNamedOutputs.map((outputDefinition, index) => {
            if(outputDefinition.state === "FINISHED"){
                return (
                    <div className="output" key={index}>
                        <div className="output-label">{outputDefinition.name}</div>
                        <Button 
                            onClick={()=>{this.handleDownload(outputDefinition.outputResultId);}}
                            bsStyle="link"
                        >
                            {i18n('global.action.download')}
                        </Button>
                    </div>
                )
            }
        })

        const histOutputs = fundDetail.historicalNamedOutputs.map((outputDefinition, index) => {
            return (
                <div className="output with-versions"  key={index}>
                    <div className="output-label">{outputDefinition.name}</div>
                    <div className="versions-container">
                        {outputDefinition.outputs.map(output => (
                            <div className="version">
                                <div className="version-label">{i18n('arr.fund.outputDefinition.version', dateToString(new Date(output.lockDate)))}</div>
                                <Button
                                    onClick={()=>{this.handleDownload(outputDefinition.outputResultId);}}
                                    bsStyle="link"
                                >
                                    {i18n('global.action.download')}
                                </Button>
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

export default connect()(FundDetailExt);

