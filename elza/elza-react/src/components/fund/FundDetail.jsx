import { refInstitutionsFetchIfNeeded } from 'actions/refTables/institutions';
import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet';
import { AbstractReactComponent, i18n, Icon, StoreHorizontalLoader } from 'components/shared';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { LinkContainer } from 'react-router-bootstrap';
import { indexById, indexByProperty } from 'stores/app/utils';
import { urlFundTree } from "../../constants";
import { Button } from '../ui';
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
                            {fundCount > 0 ? i18n('fund.noSelection.title') : i18n('fund.emptyList.title')}
                        </div>
                        <div className="msg-text">
                            {fundCount > 0 ? i18n('fund.noSelection.message') : i18n('fund.emptyList.message')}
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
                        <LinkContainer key={`fund-${fundDetail.id}`} to={urlFundTree(fundDetail.id)}>
                            <Button
                                className="fund-detail-button"
                                variant="outline-secondary"
                            >
                                <Icon glyph="fa-folder-open" />
                                &nbsp;{i18n('arr.fund.action.openInArr')}
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
