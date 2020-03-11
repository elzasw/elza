/**
 * Stránka požadavků na digitalizaci.
 */

import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx';
import {connect} from 'react-redux';
import {ArrRequestDetail, Ribbon} from 'components/index.jsx';
import {FormInput, i18n, Icon, ListBox, RibbonGroup, SearchWithGoto, StoreHorizontalLoader} from 'components/shared';
import {Button} from '../../components/ui';
import {canSetFocus, focusWasSet, isFocusFor, setFocus} from 'actions/global/focus.jsx';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import {createDigitizationName, DIGITIZATION} from 'components/arr/ArrUtils.jsx';
import ArrParentPage from './ArrParentPage.jsx';

import classNames from 'classnames';

import './ArrRequestPage.scss';
import {FOCUS_KEYS} from '../../constants.tsx';

class ArrRequestPage extends ArrParentPage {
    constructor(props) {
        super(props, 'arr-request-page');
    }

    static propTypes = {
        splitter: PropTypes.object.isRequired,
        arrRegion: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
    };

    componentDidMount() {
        super.componentDidMount();

        const fund = this.getActiveFund(this.props);
        if (fund) {
            this.props.dispatch(arrRequestActions.fetchListIfNeeded(fund.versionId));
        }

        this.trySetFocus(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        super.UNSAFE_componentWillReceiveProps(nextProps);

        const fund = this.getActiveFund(nextProps);
        if (fund) {
            this.props.dispatch(arrRequestActions.fetchListIfNeeded(fund.versionId));
        }

        this.trySetFocus(nextProps);
    }

    trySetFocus(props) {
        var {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.FUND_REQUEST, 1)) {
                this.refs.fundOutputList && this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refs.fundOutputList).focus();
                });
                focusWasSet();
            }
        }
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts ArrRequestPage', '[' + action + ']', this);
        switch (action) {
            case 'newOutput':
                this.handleAddOutput();
                break;
            case 'area1':
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_REQUEST, 1));
                break;
            case 'area2':
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_REQUEST, 2));
                break;
            default:
                super.handleShortcuts(action);
        }
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        var itemActions = [];
        var altActions = [];
        if (fund) {
            const requestDetail = fund.requestDetail;
            const detailSelected = requestDetail.id !== null;
            const detailLoaded = requestDetail.fetched && !requestDetail.isFetching;

            if (!readMode && !closed) {
                if (detailSelected && !closed) {
                    itemActions.push(
                        <Button key="send" onClick={() => {
                            this.handleSend(requestDetail.id);
                        }} disabled={!detailLoaded || requestDetail.data.state !== 'OPEN'}><Icon
                            glyph="fa-youtube-play"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.fund.request.send')}</span></div>
                        </Button>,
                    );
                    itemActions.push(
                        <Button key="delete" onClick={() => {
                            this.handleDelete(requestDetail.id);
                        }} disabled={!detailLoaded || requestDetail.data.state !== 'OPEN'}><Icon glyph="fa-trash"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.fund.request.delete')}</span></div>
                        </Button>,
                    );
                }
            }
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>;
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>;
        }

        return (
            <Ribbon arr subMenu fundId={fund ? fund.id : null} altSection={altSection} itemSection={itemSection}/>
        );
    }

    handleSelect = (item) => {
        const fund = this.getActiveFund(this.props);
        this.props.dispatch(arrRequestActions.selectDetail(fund.versionId, item.id));
    };

    handleSend = (id) => {
        const fund = this.getActiveFund(this.props);
        this.props.dispatch(arrRequestActions.sendRequest(fund.versionId, id));
    };

    handleDelete = (id) => {
        const fund = this.getActiveFund(this.props);
        if (confirm(i18n('ribbon.action.arr.fund.request.delete.confirm'))) {
            this.props.dispatch(arrRequestActions.deleteRequest(fund.versionId, id));
        }
    };

    renderListItem = (props) => {
        const {item} = props;
        const {userDetail} = this.props;

        var cls = {};

        return (
            <div className={classNames(cls)}>
                <div className='name'>{createDigitizationName(item, userDetail)}</div>
                <div
                    className='state'>{i18n('arr.request.title.state')}: {i18n('arr.request.title.state.' + item.state)}</div>
            </div>
        );
    };

    handleFilterType = (e) => {
        const fund = this.getActiveFund(this.props);
        const {requestList: {filter}} = fund;

        const val = e.target.value;
        const newFilter = {
            ...filter,
            type: val,
        };

        this.props.dispatch(arrRequestActions.filterList(fund.versionId, newFilter));
    };

    handleFilterText = (filterText) => {
        const fund = this.getActiveFund(this.props);
        const {requestList: {filter}} = fund;

        const newFilter = {
            ...filter,
            description: filterText,
        };

        this.props.dispatch(arrRequestActions.filterList(fund.versionId, newFilter));
    };

    handleFilterTextClear = () => {
        this.handleFilterText(null);
    };

    renderLeftPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const {requestList, requestDetail} = fund;

        let activeIndex = null;
        if (requestDetail.id !== null) {
            activeIndex = indexById(requestList.rows, requestDetail.id);
        }

        return (
            <div className="fund-request-list-container">
                <div className="filter">
                    <FormInput as="select" className="type" onChange={this.handleFilterType}
                               value={requestList.filter.type}>
                        <option value={''}>{i18n('global.all')}</option>
                        <option value="DIGITIZATION"
                                key="DIGITIZATION">{i18n('arr.request.title.type.' + DIGITIZATION)}</option>
                        <option value="DESTRUCTION"
                                key="DESTRUCTION">{i18n('arr.request.title.type.dao.DESTRUCTION')}</option>
                        <option value="TRANSFER" key="TRANSFER">{i18n('arr.request.title.type.dao.TRANSFER')}</option>
                        {/*<option value="DAO_LINK" key="DAO_LINK">{i18n("arr.request.title.type." + DAO_LINK)}</option>*/}
                    </FormInput>
                    <SearchWithGoto
                        onFulltextSearch={this.handleFilterText}
                        onClear={this.handleFilterTextClear}
                        placeholder={i18n('search.input.search')}
                        filterText={requestList.filter.description}
                        showFilterResult={true}
                        type="INFO"
                        itemsCount={requestList.filteredRows ? requestList.filteredRows.length : 0}
                        allItemsCount={requestList.count}
                    />
                </div>
                <StoreHorizontalLoader store={requestList}/>
                {requestList.fetched && <ListBox
                    className='fund-request-listbox'
                    ref='fundDigitizationRequestList'
                    items={requestList.rows}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />}
            </div>
        );
    }

    renderCenterPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const {requestDetail} = fund;

        return (
            <ArrRequestDetail
                versionId={fund.versionId}
                fund={fund}
                requestDetail={requestDetail}
            />
        );
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, focus, userDetail} = state;
    return {
        splitter,
        arrRegion,
        focus,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        templates: refTables.templates,
        outputTypes: refTables.outputTypes.items,
    };
}

export default connect(mapStateToProps)(ArrRequestPage);
