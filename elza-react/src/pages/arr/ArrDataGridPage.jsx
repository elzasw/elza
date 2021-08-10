import './ArrPage.scss';
import './ArrDataGridPage.scss';
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx';

import ArrParentPage from './ArrParentPage.jsx';
import {i18n, Icon, RibbonGroup, StoreHorizontalLoader} from 'components/shared';
import {FundDataGrid, Ribbon} from 'components/index.jsx';
import {Button} from '../../components/ui';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import DataGridExportDialog from '../../components/arr/DataGridExportDialog';

/**
 * Stránka archivních pomůcek.
 */

const ArrDataGridPage = class ArrDataGridPage extends ArrParentPage {
    constructor(props) {
        super(props, 'fa-page');
    }

    componentDidMount() {
        this.props.dispatch(refRuleSetFetchIfNeeded());
        super.componentDidMount();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(refRuleSetFetchIfNeeded());
        super.UNSAFE_componentWillReceiveProps(nextProps);
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const activeFund = this.getActiveFund(this.props);

        var altActions = [];

        var itemActions = [];
        itemActions.push(
            <Button
                key="export-dataGrid"
                onClick={() => {
                    this.handleDataGridExport(activeFund.versionId);
                }}
            >
                <Icon glyph="fa-download" />
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.dataGrid.export')}</span>
                </div>
            </Button>,
        );

        var altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return (
            <Ribbon
                arr
                subMenu
                fundId={activeFund ? activeFund.id : null}
                altSection={altSection}
                itemSection={itemSection}
            />
        );
    }

    handleDataGridExport(versionId) {
        const fund = this.getActiveFund(this.props);
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('dataGrid.export.title'),
                <DataGridExportDialog versionId={versionId} fundDataGrid={fund.fundDataGrid} />,
            ),
        );
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts ArrDataGridPage', '[' + action + ']', this);
        super.handleShortcuts(action, e);
    }

    renderCenterPanel(readMode, closed) {
        const {descItemTypes, rulDataTypes, ruleSet} = this.props;
        const fund = this.getActiveFund(this.props);

        return (
            <div className="datagrid-content-container">
                <StoreHorizontalLoader store={ruleSet} />
                {ruleSet.fetched && (
                    <FundDataGrid
                        versionId={fund.versionId}
                        fundId={fund.id}
                        fund={fund}
                        closed={fund.closed}
                        readMode={readMode}
                        fundDataGrid={fund.fundDataGrid}
                        descItemTypes={descItemTypes}
                        rulDataTypes={rulDataTypes}
                        ruleSet={ruleSet}
                    />
                )}
            </div>
        );
    }
};

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, focus, developer, userDetail, tab} = state;
    return {
        splitter,
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        tab,
    };
}

ArrDataGridPage.propTypes = {
    splitter: PropTypes.object.isRequired,
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrDataGridPage);
