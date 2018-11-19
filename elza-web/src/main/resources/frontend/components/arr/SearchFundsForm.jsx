import React from 'react';
import {connect} from "react-redux";
import {Modal} from 'react-bootstrap';
const classNames = require('classnames');
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {createReferenceMark, getGlyph, getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {AbstractReactComponent, FormInput, Icon, i18n} from 'components/shared';
import { WebApi } from '../../actions/WebApi';

import * as types from 'actions/constants/ActionTypes.js';

import {routerNavigate} from 'actions/router.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import * as fundSearchActions from '../../actions/arr/fundSearch.jsx'
import Search from "../shared/search/Search";
import Loading from "../shared/loading/Loading";
import HorizontalLoader from "../shared/loading/HorizontalLoader";

import {createFundRoot, getParentNode} from './ArrUtils.jsx'
import {fundSelectSubNode} from 'actions/arr/node.jsx'

import {fundTreeFulltextChange} from 'actions/arr/fundTree.jsx'

import {fundsSelectFund} from 'actions/fund/fund.jsx'
import {fundTreeFetch} from 'actions/arr/fundTree.jsx';
import {selectFundTab} from 'actions/arr/fund.jsx'

import './SearchFundsForm.less';

const FUND_NAME_MAX_CHARS = 60

const colorMap = {
    "fa-database":{background:"#fff",color:"#000"},
    "fa-folder-o":{background:"#ffcc00",color:"#fff"},
    "ez-serie":{background:"#6696dd", color:"#fff"},
    "fa-sitemap":{background:"#4444cc", color:"#fff"},
    "fa-file-text-o":{background:"#ff972c", color:"#fff"},
    "ez-item-part-o":{background:"#cc3820", color: "#fff"},
    "default":{background:"#333", color: "#fff"}
}

/**
 * Formulář pro vyhledávání nad archivními soubory.
 */
class SearchFundsForm extends AbstractReactComponent {
    static propTypes = {};

    componentWillReceiveProps(nextProps) {
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
    handleSearch = (fulltext) => {
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
    handleFundClick = (fund) => {
        this.props.dispatch(fundSearchActions.fundSearchExpandFund(fund));
    };

    /**
     * Přejít na detail uzlu
     */
    handleNodeClick = (item) => {
        const { fulltext, funds } = this.props.fundSearch;
        const itemFund = this.props.fundSearch.funds.find(fund => fund.nodes.some(nd => nd.id === item.id));

        // Přepnutí na stránku pořádání a zavření dialogu
        this.props.dispatch(routerNavigate('/arr'));
        this.props.dispatch(modalDialogHide());

        // Vyplní vyhledávací políčko na stránce pořádání
        this.props.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_MAIN, item.version, fulltext));

        // Otevře detailu uzlu
        this.navigateToNode(itemFund, item);
    };

     /**
     * Přepnutí do detailu uzlu.
     * @param fund {Object} AS
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    navigateToNode = (fund, node) => {
        WebApi.getFundDetail(fund.id).then(fund => {
            this.props.dispatch(fundsSelectFund(fund.id));
            this.props.dispatch(selectFundTab(fund));
            this.props.dispatch(this.fundSelectSubNode(fund, node, false));
        });
    }

    /**
     * Výběr uzlu.
     * @param fund {Object} AS
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    fundSelectSubNode = (fund, node, openNewTab) => {
        return (dispatch, getState) => {
            const { arrRegion } = getState();
            const activeFund = this.getActiveIndex(arrRegion);

            dispatch(fundTreeFetch(types.FUND_TREE_AREA_MAIN, fund.versionId, node.id, activeFund.expandedIds)).then(() => {
                const { arrRegion } = getState();
                const activeFund = this.getActiveIndex(arrRegion);

                const nodeFromTree = activeFund.fundTree.nodes.find(n => n.id === node.id);

                let parentNode = getParentNode(nodeFromTree, activeFund.fundTree.nodes);

                if (parentNode === null) {
                    parentNode = createFundRoot(fund);
                }

                dispatch(fundSelectSubNode(fund.versionId, node.id, parentNode, openNewTab, null, true));
            });
        };
    }

    getActiveIndex(arrRegion) {
        return arrRegion.activeIndex !== null ? arrRegion.funds[arrRegion.activeIndex] : null;
    }

    /**
     * Renderování vyhledaného archivního souboru.
     * @param type {string} typ
     * @param item {Object} AS / uzel
     * @return {Object} view
     */
    renderItem = (type, item) => {
        const {expanded} = item;

        const expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');
        const expCol = <span className={expColCls} onClick={() => this.handleFundClick(item)}></span>
        const detailCol = <span className="detail-col fa fa-sign-out" onClick={() => this.handleNodeClick(item)}></span>

        const levels = createReferenceMark(item, null);

        let cls;
        if (type === 'fund') {
            cls = classNames({
                item: true,
                opened: expanded,
                closed: !expanded,
            });
            item.icon = 'fa-database';
        } else {
            cls = 'item';
        }

        let name = item.name;
        if (name.length > FUND_NAME_MAX_CHARS) {
            name = name.substring(0, FUND_NAME_MAX_CHARS - 3) + '...'
        }

        let backgroundColor, color;
        if (colorMap[item.icon]){
            backgroundColor = colorMap[item.icon].background;
            color = colorMap[item.icon].color;
        } else {
            backgroundColor = colorMap["default"].background;
            color = colorMap["default"].color;
        }

        const iconStyle = {
            backgroundColor:backgroundColor,
            color:color
        };

        let icon = getGlyph(item.icon);
        const iconRemap = {
            "fa-folder-o":"folder",
            "ez-serie":"serie",
            "fa-sitemap":"sitemap",
            "fa-file-text-o":"fileText",
            "ez-item-part-o":"fileTextPart",
            "fa-exclamation-triangle":"triangleExclamation"
        };

        if (iconRemap[icon]){
            icon = iconRemap[icon];
        }

        return <div key={item.id} className={type}>
            <div className={cls}>
                {type === 'fund' ? expCol : levels}
                <Icon className="item-icon" style={iconStyle} fill={iconStyle.backgroundColor} stroke="none" glyph={icon}/>
                <div
                    title={item.name}
                    className="item-label"
                >
                    {name} {item.count && `(${item.count})`}
                    {type === 'node' && detailCol}
                </div>
            </div>
            {expanded && item.nodes &&
                <div className="nodes">
                    {item.nodes.map(node => this.renderItem('node', node))}
                </div>
            }
        </div>;
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
                {isFulltext && i18n('arr.fund.search.result.count', totalCount)}
                <div className={`fund-search ${isFulltext && totalCount > 0 ? 'result' : 'no-fulltext'}`}>
                    {isFulltext 
                        ? this.renderResult() 
                        : i18n('arr.fund.search.noFulltext'
                    )}
                </div>
            </Modal.Body>
        )
    }

    renderResult = () => {
        const {fundSearch} = this.props;

        const result = [];

        if (fundSearch.isFetching) {
            result.push(<HorizontalLoader hover showText={false} key="loader"/>);
        }

        if (fundSearch.fetched) {
            result.push(
                <div key="result" className="result-list">
                    {fundSearch.funds.length > 0 &&
                        fundSearch.funds.map(fund => this.renderItem('fund', fund)) 
                    }
                </div>
            )
        }

        return result;
    };

    getTotalCount = (funds) => {
        let count = 0;
        funds.forEach(fund => count += fund.count);
        return count;
    }
}

function mapStateToProps(state) {
    const {fundSearch} = state.arrRegion;
    const {fundDetail} = state.fundRegion;

    return {
        fundSearch,
        fundDetail
    }
}

export default connect(mapStateToProps)(SearchFundsForm);
