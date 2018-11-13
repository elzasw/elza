import React from 'react';
import {AbstractReactComponent, FormInput, Icon, i18n} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import Search from "../shared/search/Search";
import {connect} from "react-redux";
<<<<<<< HEAD
import './SearchFundsForm.less';

//  Actions
import * as types from '../../actions/constants/ActionTypes.js';
import {fundModalFulltextChange, fundModalFulltextSearch} from '../../actions/arr/fundModal.jsx'
=======
import * as fundSearchActions from '../../actions/arr/fundSearch.jsx'
import Loading from "../shared/loading/Loading";
import HorizontalLoader from "../shared/loading/HorizontalLoader";
>>>>>>> b492a1f1c51c28a9208016c40044da209057f515

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
     * Renderování uzlu.
     * @param node {Object} uzel
     * @return {Object} view
     */
    renderNode = (node) => {
        const {onNodeDoubleClick, onOpenCloseNode, onContextMenu} = this.props;

        const expanded = this.props.expandedIds[node.id];

        const clickProps = {
            onClick: (e)=>this.handleNodeClick(node, false, e),
            onDoubleClick: (e)=>this.handleNodeDoubleClick(node, false, e),
        };

        const expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');
        expCol = <span className={expColCls} onClick={onOpenCloseNode.bind(this, node, !expanded)}></span>

        let active = false;
        active |= this.props.selectedId === node.id;
        if (this.props.selectedIds && this.props.selectedIds[node.id]) {
            active = true
        }
        const cls = classNames({
            node: true,
            opened: expanded,
            closed: !expanded,
            active: active,
            focus: this.props.focusId === node.id,
            "node-color": this.props.colorCoded
        });
        const iconClass = classNames({
            "node-icon": true,
            "node-icon-color": this.props.colorCoded
        });
        var levels = createReferenceMark(node, clickProps);

        let name = node.name ? node.name : i18n('fundTree.node.name.undefined', node.id);
        const title = name;
        if (this.props.cutLongLabels) {
            if (name.length > TREE_NAME_MAX_CHARS) {
                name = name.substring(0, TREE_NAME_MAX_CHARS - 3) + '...'
            }
        }
        let style = {};
        let backgroundColor, color;

        if(this.props.colorCoded){
            if(colorMap[node.icon]){
                backgroundColor = colorMap[node.icon].background;
                color = colorMap[node.icon].color;
            } else {
                backgroundColor = colorMap["default"].background;
                color = colorMap["default"].color;
            }
            style = {
                backgroundColor:backgroundColor,
                color:color
            };
        }
        let icon = getGlyph(node.icon);
        let iconRemap = {
            "fa-folder-o":"folder",
            "ez-serie":"serie",
            "fa-sitemap":"sitemap",
            "fa-file-text-o":"fileText",
            "ez-item-part-o":"fileTextPart",
            "fa-exclamation-triangle":"triangleExclamation"
        };
        if(iconRemap[icon] && this.props.colorCoded){
            icon = iconRemap[icon];
        }

        return <div key={node.id} className={cls}>
            {levels}
            {expCol}
            <Icon {...clickProps} className={iconClass} style={style} fill={backgroundColor} stroke="none" glyph={icon}/>
            <div
                title={title}
                className='node-label'
                {...clickProps}
                onContextMenu={onContextMenu ? onContextMenu.bind(this, node) : null}>
                {name}
                {this.props.onLinkClick && node.link && <Icon glyph="fa-sign-out fa-lg" onClick={() => this.props.onLinkClick(node)}/>}
            </div>
        </div>;
    };

    render() {
<<<<<<< HEAD
        let { data } = this.props;

        // mockup data
        data = [];
        const node = {
            depth: 1,
            hasChildren: true,
            icon: "fa-sitemap",
            id: 1,
            name: "Test",
            referenceMark: [],
            version: 2
        }

=======
        const {fundSearch} = this.props;
>>>>>>> b492a1f1c51c28a9208016c40044da209057f515
        return (
            <Modal.Body>
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleClearSearch}
                    placeholder={i18n('search.input.search')}
                    value={fundSearch.fulltext}
                />
<<<<<<< HEAD
                {data.length > 0 ?
                    <div className={"fund-search-result"}>
                        {i18n('arr.fund.search.result.count', this.props.count)}
                        {this.props.coun > 0 &&
                            <div className="result-list">
                                {this.renderNode(node)}
                            </div>
                        }
                    </div>
                    : 
                    <div className="fund-search-no-fulltext">
                        {i18n('arr.fund.search.noFulltext')}
                    </div>
                }
=======
                {this.renderResult()}
>>>>>>> b492a1f1c51c28a9208016c40044da209057f515
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
            result.push(<div key="result">
                {i18n('arr.fund.search.result.count', this.getAllCount(fundSearch.funds))}
                {/*TODO ELZA-1656: přidat komponentu pro zobrazení výsledků */}
            </div>)
        }

        return result;
    };

    getAllCount = (funds) => {
        let count = 0;
        funds.forEach(fund => count += fund.count);
        return count;
    }
}

function mapStateToProps(state) {
    const {fundSearch} = state.arrRegion;
    return {
        fundSearch
    }
}

export default connect(mapStateToProps)(SearchFundsForm);
