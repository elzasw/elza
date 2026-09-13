import { refInstitutionsFetchIfNeeded } from 'actions/refTables/institutions';
import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet';
import { AbstractReactComponent, Icon, StoreHorizontalLoader } from 'components/shared';
import { FormattedMessage, defineMessages } from 'react-intl';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { LinkContainer } from 'react-router-bootstrap';
import { indexById, indexByProperty } from 'stores/app/utils';
import { urlFundTree } from "../../constants";
import { Button } from '../ui';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    noSelectionTitle: { id: 'fund.noSelection.title', defaultMessage: 'Není vybrán archivní soubor' },
    noSelectionMessage: { id: 'fund.noSelection.message', defaultMessage: 'Prosím vyberte archivní soubor ze seznamu.' },
    emptyListTitle: { id: 'fund.emptyList.title', defaultMessage: 'Žádné archivní soubory' },
    emptyListMessage: {
        id: 'fund.emptyList.message',
        defaultMessage: 'Neexistují archivní soubory nebo nemáte oprávnění pro čtení žádného archivního souboru.',
    },
    internalCode: { id: 'arr.fund.detail.internalCode', defaultMessage: 'Interní kód' },
    institution: { id: 'arr.fund.detail.institution', defaultMessage: 'Instituce' },
    ruleSet: { id: 'arr.fund.detail.ruleSet', defaultMessage: 'Pravidla tvorby' },
    openInArr: { id: 'arr.fund.action.openInArr', defaultMessage: 'Otevřít' },
});
import './FundDetail.scss';


class FundDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        this.props.dispatch(refInstitutionsFetchIfNeeded());
        this.props.dispatch(refRuleSetFetchIfNeeded());
    }

    render() {
        const {
            fundDetail,
            fundCount,
            refTables: {institutions, ruleSet},
        } = this.props;

        if (fundDetail.id === null) {
            return (
                <div className="fund-detail-container">
                    <div className="unselected-msg">
                        <div className="title">
                            <FormattedMessage {...(fundCount > 0 ? messages.noSelectionTitle : messages.emptyListTitle)} />
                        </div>
                        <div className="msg-text">
                            <FormattedMessage {...(fundCount > 0 ? messages.noSelectionMessage : messages.emptyListMessage)} />
                        </div>
                    </div>
                </div>
            );
        }

        let content;
        if (fundDetail.fetched) {
            const instIndex = indexById(institutions.items, fundDetail.institutionId);
            const institution = instIndex !== null ? institutions.items[instIndex].name : '';

            const activeVersionIndex = indexByProperty(fundDetail.versions, null, 'lockDate');
            const ruleIndex = indexById(ruleSet.items, fundDetail.versions[activeVersionIndex]?.ruleSetId);
            const rule = ruleIndex !== null ? ruleSet.items[ruleIndex].name : '';

            content = (
                <div className="fund-detail-container">
                    <div className="fund-detail-info">
                        <h1>{fundDetail.name}</h1>
                        <div>
                            <label><FormattedMessage {...messages.internalCode} />:</label>
                            <span>{fundDetail.internalCode}</span>
                        </div>
                        <div>
                            <label><FormattedMessage {...messages.institution} />:</label>
                            <span>{institution}</span>
                        </div>

                        <div>
                            <label><FormattedMessage {...messages.ruleSet} />:</label>
                            <span>{rule}</span>
                        </div>
                        <LinkContainer key={`fund-${fundDetail.id}`} to={urlFundTree(fundDetail.id)}>
                            <Button
                                className="fund-detail-button"
                                variant="outline-secondary"
                            >
                                <Icon glyph="fa-folder-open" />
                                &nbsp;<FormattedMessage {...messages.openInArr} />
                            </Button>
                        </LinkContainer>
                    </div>
                </div>
            );
        }

        return (
            <div>
                <StoreHorizontalLoader store={fundDetail} />
                {content}
            </div>
        );
    }
}

FundDetail.propTypes = {
    fundDetail: PropTypes.object.isRequired,
    fundCount: PropTypes.number.isRequired,
};

function mapStateToProps(state) {
    return {
        refTables: state.refTables,
    };
}

export default connect(mapStateToProps)(FundDetail);
