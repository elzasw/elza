import { downloadFile } from 'actions/global/download';
import { UrlFactory } from 'actions/index.jsx';
import { AbstractReactComponent } from 'components/shared';
import { FormattedMessage, defineMessages } from 'react-intl';
import { globalMessages } from 'components/shared/lang';
import { dateToString } from 'components/Utils.jsx';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { LinkContainer } from 'react-router-bootstrap';
import { urlFundTree } from "../../constants";
import { Button } from '../ui';
import './FundDetailExt.scss';

// Id jsou převzatá z legacy katalogu beze změny. Placeholder {0} zůstává:
// ICU bere jako jméno argumentu i číslo, takže legacy tvar funguje beze změny.
const messages = defineMessages({
    activeOutputs: { id: 'arr.fund.outputDefinition.active', defaultMessage: 'Výstupy' },
    versionList: { id: 'arr.fund.version.list', defaultMessage: 'Verze AS' },
    version: { id: 'arr.fund.version', defaultMessage: 'Verze {0}' },
    currentVersion: { id: 'arr.fund.currentVersion', defaultMessage: 'Aktuální verze' },
    showInArr: { id: 'arr.fund.action.showInArr', defaultMessage: 'Zobrazit' },
    openInArr: { id: 'arr.fund.action.openInArr', defaultMessage: 'Otevřít' },
});

const FundDetailExt = class FundDetailExt extends AbstractReactComponent {
    static propTypes = {
        fundDetail: PropTypes.object.isRequired,
    };

    constructor(props) {
        super(props);

    }

    componentDidMount() {}

    handleDownload = id => {
        this.props.dispatch(downloadFile(UrlFactory.downloadOutputResults(id)));
    };

    render() {
        const {fundDetail} = this.props;

        if (fundDetail.id === null) {
            return <div className="fund-detail-container"></div>;
        }

        const validOutputs = fundDetail.validNamedOutputs.map((arrOutput, index) => {
            if (arrOutput.state === 'FINISHED') {
                return (
                    <div className="output" key={index}>
                        <div className="output-label">{arrOutput.name}</div>
                        <Button
                            onClick={() => {
                                this.handleDownload(arrOutput.id);
                            }}
                            variant="link"
                        >
                            <FormattedMessage {...globalMessages.download} />
                        </Button>
                    </div>
                );
            }
            return null;
        });


        return (
            <div className="fund-detail-ext-container">
                {validOutputs.length > 0 && (
                    <div className="outputs-container">
                        <h1><FormattedMessage {...messages.activeOutputs} /></h1>
                        {validOutputs}
                    </div>
                )}
                <div className="versions-container">
                    <h1><FormattedMessage {...messages.versionList} /></h1>
                    {fundDetail.versions.map((ver, index) => {
                        if (ver.lockDate) {
                            return (
                                <div className="fund-version" key={'fund-version-' + index}>
                                    <div className="version-label">
                                        <FormattedMessage {...messages.version} values={{ 0: dateToString(new Date(ver.lockDate)) }} />
                                    </div>
                                    <LinkContainer key={`fund-${ver.id}`} to={urlFundTree(fundDetail.id, ver.id)}>
                                        <Button variant='link'>
                                        <FormattedMessage {...messages.showInArr} />
                                        </Button>
                                    </LinkContainer>
                                </div>
                            );
                        } else {
                            return (
                                <div className="fund-version" key={'fund-version-' + index}>
                                    <div className="version-label"><FormattedMessage {...messages.currentVersion} /></div>
                                    <LinkContainer key={`fund-${ver.id}`} to={urlFundTree(fundDetail.id)}>
                                        <Button variant='link'>
                                        <FormattedMessage {...messages.openInArr} />
                                        </Button>
                                    </LinkContainer>
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
