import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Button } from '../ui';
import { AbstractReactComponent, i18n } from 'components/shared';
import { dateToString } from 'components/Utils.jsx';
import { getFundFromFundAndVersion } from 'components/arr/ArrUtils.jsx';
import { selectFundTab } from 'actions/arr/fund.jsx';
import { routerNavigate } from 'actions/router.jsx';
import { downloadFile } from 'actions/global/download';
import { UrlFactory } from 'actions/index.jsx';

import './FundDetailExt.scss';

const FundDetailExt = class FundDetailExt extends AbstractReactComponent {
    static propTypes = {
        fundDetail: PropTypes.object.isRequired,
    };

    constructor(props) {
        super(props);

        this.bindMethods('handleShowInArr');
    }

    componentDidMount() {
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
    }

    handleShowInArr(version) {
        // Přepnutí na stránku pořádání
        this.props.dispatch(routerNavigate('/arr'));

        // Otevření archivního souboru
        const fund = this.props.fundDetail;
        const fundObj = getFundFromFundAndVersion(fund, version);
        this.props.dispatch(selectFundTab(fundObj));
    }

    handleDownload = (id) => {
        this.props.dispatch(downloadFile(UrlFactory.downloadOutputResult(id)));
    };

    render() {
        const { fundDetail } = this.props;

        if (fundDetail.id === null) {
            return <div className='fund-detail-container'></div>;
        }

        const validOutputs = fundDetail.validNamedOutputs.map((arrOutput, index) => {
            if (arrOutput.state === 'FINISHED') {
                return (
                    <div className="output" key={index}>
                        <div className="output-label">{arrOutput.name}</div>
                        <Button
                            onClick={() => {
                                this.handleDownload(arrOutput.outputResultId);
                            }}
                            variant="link"
                        >
                            {i18n('global.action.download')}
                        </Button>
                    </div>
                );
            }
            return null;
        });

        const histOutputs = fundDetail.historicalNamedOutputs.map((output, index) => {
            return (
                <div className="output with-versions" key={index}>
                    <div className="output-label">{output.name}</div>
                    <div className="versions-container">
                        {output.outputs.map(output => (
                            <div className="version">
                                <div
                                    className="version-label">{i18n('arr.fund.outputDefinition.version', dateToString(new Date(output.deleteDate)))}</div>
                                <Button
                                    onClick={() => {
                                        this.handleDownload(output.outputResultId);
                                    }}
                                    variant="link"
                                >
                                    {i18n('global.action.download')}
                                </Button>
                            </div>
                        ))}
                    </div>
                </div>
            );
        });

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
                                <div className='fund-version' key={'fund-version-' + index}>
                                    <div
                                        className="version-label">{i18n('arr.fund.version', dateToString(new Date(ver.lockDate)))}</div>
                                    <Button onClick={this.handleShowInArr.bind(this, ver)}
                                            variant='link'>{i18n('arr.fund.action.showInArr')}</Button>
                                </div>
                            );
                        } else {
                            return (
                                <div className='fund-version' key={'fund-version-' + index}>
                                    <div className="version-label">{i18n('arr.fund.currentVersion')}</div>
                                    <Button onClick={this.handleShowInArr.bind(this, ver)}
                                            variant='link'>{i18n('arr.fund.action.openInArr')}</Button>
                                </div>
                            );
                        }
                    })}
                </div>
            </div>
        );
    }
};

export default connect()(FundDetailExt);
