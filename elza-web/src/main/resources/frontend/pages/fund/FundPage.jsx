/**
 * Stránka archivní soubory.
 */

require ('./FundPage.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components';
import {Splitter, Autocomplete, FundForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent, ImportForm,
    Search, ListBox, FundDetail} from 'components';
import {NodeTabs, FundTreeTabs} from 'components';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {modalDialogShow} from 'actions/global/modalDialog'
import {createFund} from 'actions/arr/fund'
import {storeLoadData, storeSave, storeLoad} from 'actions/store/store'
import {Combobox} from 'react-input-enhancements'
import {WebApi} from 'actions'
import {setInputFocus, dateToString} from 'components/Utils'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'
import {indexById} from 'stores/app/utils.jsx'
import {selectFundTab} from 'actions/arr/fund'
import {routerNavigate} from 'actions/router'
import {fundsFetchIfNeeded, fundsSelectFund, fundsFundDetailFetchIfNeeded, fundsSearch} from 'actions/fund/fund'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils'

var FundPage = class FundPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleAddFund', 'handleImport', 'renderListItem', 'handleSelect', 'handleSearch', 'handleSearchClear')

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fundsFetchIfNeeded())
        this.dispatch(fundsFundDetailFetchIfNeeded())
    }

    componentDidMount() {
        this.dispatch(fundsFetchIfNeeded())
        this.dispatch(fundsFundDetailFetchIfNeeded())
    }

    handleAddFund() {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.add'),
            <FundForm create onSubmitForm={(data) => {this.dispatch(createFund(data))}}/>));
    }

    handleImport() {
        this.dispatch(
            modalDialogShow(this,
                i18n('import.title.fund'),
                <ImportForm fund={true}/>
            )
        );
    }

    buildRibbon() {
        var altActions = [];
        altActions.push(
            <Button key="add-fa" onClick={this.handleAddFund}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span></div></Button>
        )
        altActions.push(
            <Button key="fa-import" onClick={this.handleImport}><Icon glyph='fa-download'/>
                <div><span className="btnText">{i18n('ribbon.action.arr.fund.import')}</span></div>
            </Button>
        )

        var itemActions = [];
        const {fundRegion} = this.props
        if (fundRegion.fundDetail.id !== null) {
            itemActions.push(
                <Button key="fa-plus" onClick={this.handleCreateFund}><Icon glyph='fa-plus'/>
                    <div><span className="btnText">{i18n('arr.fund.action.newVersion')}</span></div>
                </Button>
            )
            itemActions.push(
                <Button key="fa-delete" onClick={this.handleDeleteFund}><Icon glyph='fa-remove'/>
                    <div><span className="btnText">{i18n('arr.fund.action.deleteVersion')}</span></div>
                </Button>
            )
        }
        

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="large">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon ref='ribbon' fund altSection={altSection} itemSection={itemSection} {...this.props} />
        )
    }

    handleShowInArr(item) {
        // Přepnutí na stránku pořádání
        this.dispatch(routerNavigate('/arr'))

        // Otevření archivního souboru
        var fundObj = getFundFromFundAndVersion(item, item);
        this.dispatch(selectFundTab(fundObj));
    }

    renderListItem(item) {
        return (
            <div>
                <div className='name'>{item.id}{item.name}</div>
                <div>{item.internalCode}</div>
                <div><Button onClick={this.handleShowInArr.bind(this, item)} bsStyle='link'>{i18n('arr.fund.action.showInArr')}</Button></div>
            </div>
        )
    }

    handleSelect(item) {
        this.dispatch(fundsSelectFund(item.id))
    }

    handleSearch(filterText) {
        this.dispatch(fundsSearch(filterText))
    }

    handleSearchClear() {
        this.dispatch(fundsSearch(''))
    }

    render() {
        const {splitter, focus, fundRegion} = this.props;

        var activeIndex
        if (fundRegion.fundDetail.id !== null) {
            activeIndex = indexById(fundRegion.funds, fundRegion.fundDetail.id)
        }
        var leftPanel = (
            <div className="fund-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    />
                <ListBox 
                    className='fund-listbox'
                    ref='fundList'
                    items={fundRegion.funds}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />
            </div>
        )

        var centerPanel = (
            <FundDetail
                fundDetail={fundRegion.fundDetail}
                focus={focus}
                />
        )

        var rightPanel = (
            <div>...</div>
        )

        return (
            <PageLayout
                splitter={splitter}
                className='fund-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        )
    }
}

function mapStateToProps(state) {
    const {focus, splitter, fundRegion} = state
    return {
        focus,
        splitter,
        fundRegion,
    }
}

module.exports = connect(mapStateToProps)(FundPage);

