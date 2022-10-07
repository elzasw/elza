import React from 'react';
import {connect} from 'react-redux';
import {Modal} from 'react-bootstrap';
import classNames from 'classnames';
import {createReferenceMark, getNodeIcon} from 'components/arr/ArrUtils.jsx';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {WebApi} from '../../actions/WebApi';

import * as types from 'actions/constants/ActionTypes';

import {routerNavigate} from 'actions/router.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';
import * as fundSearchActions from '../../actions/arr/fundSearch.jsx';
import Search from '../shared/search/Search';
import HorizontalLoader from '../shared/loading/HorizontalLoader';

import {createFundRoot, getParentNode} from './ArrUtils.jsx';

import {fundsSelectFund} from 'actions/fund/fund.jsx';
import {selectFundTab} from 'actions/arr/fund.jsx';
import {
    fundTreeFetch,
    fundTreeFulltextChange,
    fundTreeFulltextResult,
    fundTreeSelectNode,
} from 'actions/arr/fundTree.jsx';
import {fundSelectSubNode} from 'actions/arr/node.jsx';

import './SearchFundsForm.scss';
import {URL_FUND_TREE} from "../../constants";

const FUND_NAME_MAX_CHARS = 60;

/**
 * Formulář pro vyhledávání nad archivními soubory.
 */
class SearchFundsForm extends AbstractReactComponent {
    static propTypes = {};

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(fundSearchActions.fundSearchFetchIfNeeded());
    }

    componentDidMount() {
        this.props.dispatch(fundSearchActions.fundSearchFetchIfNeeded());
    }

    /**
     * Vyhledání v archivních souborech.
     *
     * @param fulltext hledaný výraz
     */
    handleSearch = fulltext => {
        this.props.dispatch(fundSearchActions.fundSearchFulltextChange(fulltext));
    };

    /**
     * Smazání výsledků vyhledávání.
     */
    handleClearSearch = () => {
        this.props.dispatch(fundSearchActions.fundSearchFulltextClear());
    };

    /**
     * Zobrazení seznamu výskytů hledaného výrazu v AS
     */
    handleFundClick = fund => {
        this.props.dispatch(fundSearchActions.fundSearchExpandFund(fund));
    };

    /**
     * Přejít na detail uzlu
     */
    handleNodeClick = item => {
        const itemFund = this.props.fundSearch.funds.find(fund => fund.nodes.some(nd => nd.id === item.id));

        // Přepnutí na stránku pořádání a zavření dialogu
        this.props.dispatch(routerNavigate(URL_FUND_TREE));
        this.props.dispatch(modalDialogHide());

        // Otevře detailu uzlu
        this.navigateToNode(itemFund, item);
    };

    /**
     * Přepnutí do detailu uzlu.
     * @param fund {Object} AS
     * @param node {Object} uzel
     */
    navigateToNode = (fund, node) => {
        const {arrRegion} = this.props;
        const activeFund = this.getActiveIndex(arrRegion);

        if (fund.id !== activeFund.id) {
            WebApi.getFundDetail(fund.id).then(fund => {
                this.props.dispatch(fundsSelectFund(fund.id));
                this.props.dispatch(selectFundTab(fund));
                this.props.dispatch(this.fundSelectSubNode(fund, node, false, true));
            });
        } else {
            this.props.dispatch(this.fundSelectSubNode(activeFund, node, false, false));
        }
    };

    /**
     * Výběr uzlu.
     * @param fund {Object} AS
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     * @param force {Boolean} true, natáhne informace o stromové struktuře
     */
    fundSelectSubNode = (fund, node, openNewTab, force) => {
        return (dispatch, getState) => {
            const {fulltext} = this.props.fundSearch;
            const {arrRegion} = getState();
            const activeFund = this.getActiveIndex(arrRegion);

            let activeNode;
            if (activeFund.nodes.activeIndex !== null) {
                activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
            }

            let parentNode = activeNode ? getParentNode(activeNode, activeFund.nodes.nodes) : null;
            if (parentNode === null) {
                parentNode = createFundRoot(activeFund);
            }

            if (force) {
                dispatch(
                    fundTreeFetch(types.FUND_TREE_AREA_MAIN, fund.versionId, node.id, activeFund.fundTree.expandedIds),
                ).then(() => {
                    const {arrRegion} = getState();
                    const activeFund = this.getActiveIndex(arrRegion);

                    let activeNode;
                    if (activeFund.nodes.activeIndex !== null) {
                        activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
                    }

                    let parentNode = getParentNode(activeNode, activeFund.nodes.nodes);
                    if (parentNode === null) {
                        parentNode = createFundRoot(activeFund);
                    }

                    dispatch(fundSelectSubNode(fund.versionId, node.id, parentNode, openNewTab, null, false));
                });
            } else {
                dispatch(fundSelectSubNode(fund.versionId, node.id, parentNode, openNewTab, null, false));
            }

            // Vyplní vyhledávací políčko na stránce pořádání
            dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_MAIN, fund.versionId, fulltext));

            const searchedData = this.getSearchedData(fund.id);
            const activeNodeIndex = searchedData.findIndex(data => data.nodeId === node.id);

            dispatch(
                fundTreeFulltextResult(
                    types.FUND_TREE_AREA_MAIN,
                    fund.versionId,
                    fulltext,
                    searchedData,
                    false,
                    {type: 'FORM'},
                    null,
                ),
            );
            dispatch(
                fundTreeSelectNode(
                    types.FUND_TREE_AREA_MAIN,
                    fund.versionId,
                    node.id,
                    parentNode,
                    false,
                    activeNodeIndex,
                    true,
                ),
            );
        };
    };

    getActiveIndex(arrRegion) {
        return arrRegion.activeIndex !== null ? arrRegion.funds[arrRegion.activeIndex] : null;
    }

    getSearchedData(fundId) {
        const {arrRegion, fundSearch} = this.props;

        const activeFund = this.getActiveIndex(arrRegion);
        const nodes = fundSearch.funds.find(fund => fund.id === fundId).nodes;

        let searchedData = [];
        nodes.forEach(node => {
            let parentNode = getParentNode(node, activeFund.nodes.nodes);
            if (parentNode === null) {
                parentNode = createFundRoot(activeFund);
            }

            searchedData.push({
                nodeId: node.id,
                parent: parentNode,
            });
        });

        return searchedData;
    }

    /**
     * Renderování vyhledaného archivního souboru.
     * @param fund {Object} AS
     * @return {Object} view
     */
    renderFund = fund => {
        const {expanded} = fund;
        const expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');

        let cls = classNames({
            item: true,
            opened: expanded,
            closed: !expanded,
        });
        fund.icon = '';

        let name = fund.name;
        if (name.length > FUND_NAME_MAX_CHARS) {
            name = name.substring(0, FUND_NAME_MAX_CHARS - 3) + '...';
        }

        return (
            <div key={fund.id} className="fund">
                <div className={cls}>
                    <span className={expColCls} onClick={() => this.handleFundClick(fund)} />
                    <Icon className="item-icon" glyph="fa-database" />
                    <div title={fund.name} className="item-label">
                        {name} {fund.count && `(${fund.count})`}
                    </div>
                </div>
                {expanded && fund.nodes && <div className="nodes">{fund.nodes.map(node => this.renderNode(node))}</div>}
            </div>
        );
    };

    /**
     * Render JP.
     *
     * @param node objekt JP
     * @returns {*}
     */
    renderNode = node => {
        const levels = createReferenceMark(node, null);
        const iconProps = getNodeIcon(true, node.icon);
        return (
            <div key={node.id} className="node">
                <div className="levels">{levels}</div>
                <Icon className="item-icon" {...iconProps} />
                <div title={node.name} className="item-label">
                    {node.name}
                </div>
                <span className="detail-col fa fa-sign-out" onClick={() => this.handleNodeClick(node)} />
            </div>
        );
    };

    render() {
        const {fundSearch} = this.props;
        const isFulltext = fundSearch.fulltext.length > 0;
        const totalCount = this.getTotalCount(fundSearch.funds);

        return (
            <Modal.Body>
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleClearSearch}
                    placeholder={i18n('search.input.search')}
                    value={fundSearch.fulltext}
                />
                {fundSearch.isFetching && <HorizontalLoader hover showText={false} key="loader" />}
                {isFulltext && i18n('arr.fund.search.result.count', totalCount)}
                <div className={`fund-search ${isFulltext && totalCount > 0 ? 'result' : 'no-fulltext'}`}>
                    {isFulltext ? this.renderResult() : i18n('arr.fund.search.noFulltext')}
                </div>
            </Modal.Body>
        );
    }

    renderResult = () => {
        const {fundSearch} = this.props;

        const result = [];

        if (fundSearch.fetched) {
            result.push(
                <div key="result" className="result-list">
                    {fundSearch.funds.length > 0 && fundSearch.funds.map(fund => this.renderFund(fund))}
                </div>,
            );
        }

        return result;
    };

    getTotalCount = funds => {
        let count = 0;
        funds.forEach(fund => (count += fund.count));
        return count;
    };
}

function mapStateToProps(state) {
    const {fundSearch} = state.arrRegion;

    return {
        fundSearch,
        arrRegion: state.arrRegion,
    };
}

export default connect(mapStateToProps)(SearchFundsForm);
