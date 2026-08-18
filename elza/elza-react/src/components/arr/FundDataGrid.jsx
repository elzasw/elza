/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';
import {
    AbstractReactComponent,
    DataGrid,
    DataGridColumnsSettings,
    DataGridPagination,
    i18n,
    Icon,
    SearchWithGoto,
    StoreHorizontalLoader,
} from 'components/shared';
import FundBulkModificationsForm, { OperationType } from './FundBulkModificationsForm';
import FundFilterSettings from './FundFilterSettings';
import { FundDataGridCellForm } from './node-edit/FundDataGridCellForm';
import ArrSearchForm from './ArrSearchForm';
import { Dropdown } from 'react-bootstrap';
import { Button } from '../ui';
import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import {
    FILTER_NULL_VALUE,
    fundBulkModifications,
    fundDataFulltextClear,
    fundDataFulltextExtended,
    fundDataFulltextSearch,
    fundDataGridFetchDataIfNeeded,
    fundDataGridFetchFilterIfNeeded,
    fundDataGridFilterChange,
    fundDataGridFilterClearAll,
    fundDataGridPrepareEdit,
    fundDataGridRefreshRows,
    fundDataGridSetColumnSize,
    fundDataGridSetColumnsSettings,
    fundDataInitIfNeeded,
} from 'actions/arr/fundDataGrid';
import { contextMenuHide, contextMenuShow } from 'actions/global/contextMenu';
import { descItemTypesFetchIfNeeded } from 'actions/refTables/descItemTypes';
import { structureTypesFetchIfNeeded } from 'actions/refTables/structureTypes';
import { nodeFormActions } from 'actions/arr/subNodeForm';
import { fundSelectSubNode } from 'actions/arr/node';
import { refRulDataTypesFetchIfNeeded } from 'actions/refTables/rulDataTypes';
import {
    createFundRoot,
    createReferenceMarkFromArray,
    getSpecsIds,
    getValueIds,
    hasDescItemTypeValue,
} from 'components/arr/ArrUtils';
import { getMapFromList, getSetFromIdsList } from 'stores/app/utils';
import { propsEquals } from 'components/Utils';
import { COL_DEFAULT_WIDTH, COL_REFERENCE_MARK } from './FundDataGridConst';
import './FundDataGrid.scss';
import { getPagesCount } from '../shared/datagrid/DataGridPagination';
import { toDuration } from '../validate';
import { isMaskViewDefinition, maskString } from './item-form/desc-items/maskUtils';
import { DisplayType, urlFundGrid } from '../../constants';
import Moment from 'moment';
import * as groups from '../../actions/refTables/groups';
import { JAVA_ATTR_CLASS } from '../../constants';
import { WebApi } from "../../actions/WebApi";
import { showConfirmDialog } from 'components/shared/dialog';
import { withRouter } from "react-router";
import { storeSave } from "../../actions/store/storeEx";

export const serializeJson = (json) => {
    let s = JSON.stringify(json);
    return json == null || s === '{}' ? null : btoa(encodeURIComponent(s));
}

export const deserializeJson = (str) => {
    try {
        return JSON.parse(decodeURIComponent(atob(str)));
    } catch (e) {
        console.warn("Data objektu se nepodařilo deserializovat", str);
        return {}
    }
}

// DataGrid renders a leading checkbox column (allowRowCheck), so its focus column index is one greater
// than the corresponding index in this component's cols array.
const CHECKBOX_COLUMN_OFFSET = 1;

class FundDataGridClass extends AbstractReactComponent {
    dataGridRef = null;

    static propTypes = {
        fundId: PropTypes.number.isRequired,
        versionId: PropTypes.number.isRequired,
        fund: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        ruleSet: PropTypes.object.isRequired,
        readMode: PropTypes.bool.isRequired,
        closed: PropTypes.bool.isRequired,
        fundDataGrid: PropTypes.object.isRequired, // store
        structureTypes: PropTypes.object.isRequired,
        urlFilterEncoded: PropTypes.string
    };

    constructor(props) {
        super(props);

        this.bindMethods(
            'handleSelectedIdsChange',
            'handleColumnResize',
            'handleColumnSettings',
            'handleChangeColumnsSettings',
            'handleBulkModifications',
            'handleFilterSettings',
            'headerColRenderer',
            'cellRenderer',
            'resizeGrid',
            'handleFilterClearAll',
            'handleFilterUpdateData',
            'handleContextMenu',
            'handleSelectInNewTab',
            'handleSelectInTab',
            'handleEdit',
            'handleFulltextSearch',
            'handleFulltextChange',
            'handleFulltextPrevItem',
            'handleFulltextNextItem',
            'handleChangeFocus',
            'handleToggleExtendedSearch',
            'handleChangeRowIndexes',
            'fetchData',
            'consumeDeepLink',
        );

        var colState = this.getColsStateFromProps(props, { fundDataGrid: {} });
        if (!colState) {
            colState = { cols: [], itemTypeCodes: [] };
        }

        this.state = { ...colState, cellForm: null };
    }

    componentDidMount() {
        const { versionId, fundDataGrid } = this.props;
        this.props.dispatch(fundDataGridFilterChange(versionId, this.getFilterFromUrl()));

        this.fetchData(this.props);
        this.setState({}, this.resizeGrid);
        this.consumeDeepLink();

        // Šířka gridu se počítá v pixelech z rozměru kontejneru, takže je nutné ji přepočítat
        // i při změně velikosti okna (změnu velikosti splitteru pokrývá změna stavu splitteru ve store).
        window.addEventListener('resize', this.resizeGrid);

        // Pokud je potřeba aktualizovat, aktualizujeme při přepnutí na grid, ale zachováme stránkování
        if (fundDataGrid.rowsDirty || fundDataGrid.filterDirty) {
            this.props.dispatch(fundDataGridRefreshRows(versionId));
        }
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.resizeGrid);
    }

    /**
     * Resolves an attribute type to a column index against the current columns. If the type is not shown
     * but the rules offer it, the column is enabled and null is returned to signal "retry after rebuild".
     * A missing/absent type falls back to the first column (0).
     */
    resolveColumnForType(descItemTypeId) {
        const cols = this.state.cols;

        if (descItemTypeId == null) {
            return 0;
        }
        const index = cols.findIndex(c => String(c.refType.id) === String(descItemTypeId));
        if (index >= 0) {
            return index;
        }

        // Column not shown - enable it if it is a known attribute type (matches the set of
        // columns the column-settings picker can add).
        const { descItemTypes } = this.props;
        const allowed = descItemTypes.itemsMap[descItemTypeId] != null;
        if (allowed && !this.columnAutoShown) {
            this.columnAutoShown = true;
            const { columnsOrder, visibleColumns } = this.props.fundDataGrid;
            const typeIdKey = String(descItemTypeId);
            const newVisibleColumns = { ...visibleColumns, [typeIdKey]: true };
            const newColumnsOrder = columnsOrder.some(id => String(id) === typeIdKey)
                ? columnsOrder
                : [...columnsOrder, typeIdKey];
            this.props.dispatch(fundDataGridSetColumnsSettings(this.props.versionId, newVisibleColumns, newColumnsOrder));
            // After the columns rebuild this method runs again and finds the column.
            return null;
        }
        // Column cannot be shown - fall back to the first column.
        return 0;
    }

    /**
     * Focuses the target cell, from either the URL deep link ("open in datagrid") or the cross-reload
     * highlight restored from localStorage. Both carry an attribute type that is resolved to a column here
     * (the only place with the built columns). Called repeatedly (also after a column rebuild) until done.
     */
    consumeDeepLink() {
        const { focusNodeId, focusDescItemTypeId, requestRestore, restoreDescItemTypeId, resolveRestoreColumn } = this.props;
        const cols = this.state.cols;
        if (!cols || cols.length === 0) {
            return;
        }

        // URL deep link: start a restore for the target node + column.
        if (!this.deepLinkDone && focusNodeId != null && requestRestore) {
            const colsIndex = this.resolveColumnForType(focusDescItemTypeId);
            if (colsIndex == null) {
                return; // auto-showing the column; will retry after rebuild
            }
            this.deepLinkDone = true;
            requestRestore(focusNodeId, this.toGridColumn(colsIndex));
            return;
        }

        // Reload highlight: the node restore is already pending, resolve only its column.
        if (!this.reloadColumnDone && restoreDescItemTypeId != null && resolveRestoreColumn) {
            const colsIndex = this.resolveColumnForType(restoreDescItemTypeId);
            if (colsIndex == null) {
                return; // auto-showing the column; will retry after rebuild
            }
            this.reloadColumnDone = true;
            resolveRestoreColumn(this.toGridColumn(colsIndex));
        }
    }

    fetchData(props) {
        const { fundDataGrid, descItemTypes, fund, versionId, ruleSet, location } = props;

        this.props.dispatch(descItemTypesFetchIfNeeded());
        this.props.dispatch(structureTypesFetchIfNeeded(this.props.versionId));
        this.props.dispatch(groups.fetchIfNeeded(this.props.versionId));
        this.props.dispatch(refRulDataTypesFetchIfNeeded());
        this.props.dispatch(fundDataGridFetchFilterIfNeeded(versionId));
        this.props.dispatch(fundDataGridFetchDataIfNeeded(versionId, props.view.pageIndex, props.view.pageSize));

        // this.props.dispatch(refRuleSetFetchIfNeeded());
        if (ruleSet.fetched && descItemTypes.fetched && fund.activeVersion) {
            // Prvotní inicializace zobrazení, pokud ještě grid nebyl zobrazem
            // Určí se seznam viditelných sloupečků a případně i šířka

            if (
                Object.keys(fundDataGrid.visibleColumns).length === 0 &&
                Object.keys(fundDataGrid.columnInfos).length === 0
            ) {
                const initData = { visibleColumns: [], columnInfos: [] };
                const initData2 = { visibleColumns: [], columnInfos: {} };
                const ruleMap = getMapFromList(ruleSet.items);
                const rule = ruleMap[fund.activeVersion.ruleSetId];
                if (rule.gridViews) {
                    rule.gridViews.map(({ id, width, showDefault }) => {
                        if (showDefault) {
                            initData2.visibleColumns.push(id);
                        }
                        if (width) {
                            initData2.columnInfos[id] = { width };
                        }
                    })
                    const gwMap = {}; // mapa kódu item type na GridView
                    rule.gridViews.forEach(gw => {
                        gwMap[gw.code] = gw;
                    });

                    descItemTypes.items.forEach(item => {
                        const gw = gwMap[item.code];
                        if (gw) {
                            if (gw.showDefault) {
                                initData.visibleColumns.push(item.id);
                            }
                            if (gw.width) {
                                initData.columnInfos[item.id] = {
                                    width: gw.width,
                                };
                            }
                        }
                    });
                }
                console.log("#### init data grid", initData, initData2)
                this.props.dispatch(fundDataInitIfNeeded(versionId, initData2));
            }
        }
    }

    getFilterFromUrl = () => {
        const { location } = this.props;

        const params = new URLSearchParams(location.search);
        const urlFilter = params.get("filter");

        return urlFilter ? deserializeJson(urlFilter) : {};
    }

    setFilterUrl = (filter) => {
        const { fund, history } = this.props;
        const encodeFilter = serializeJson(filter);
        let url = urlFundGrid(fund.id, undefined, encodeFilter);

        history.replace(url);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.fetchData(nextProps);

        var colState = this.getColsStateFromProps(nextProps, this.props);
        if (!colState) {
            colState = {};
        }

        this.setState(colState, () => {
            this.resizeGrid();
            this.consumeDeepLink();
        });
    }

    resizeGrid() {
        const parentEl = ReactDOM.findDOMNode(this.refs.gridContainer);
        const gridEl = ReactDOM.findDOMNode(this.refs.grid);
        if (parentEl && gridEl) {
            const rect = parentEl.getBoundingClientRect();
            const width = rect.right - rect.left;
            gridEl.style.width = width + 'px';
        }
    }

    referenceMarkCellRenderer(row, rowIndex, col, colIndex, cellFocus) {
        const referenceMark = row.referenceMark;

        let itemValue;
        if (referenceMark && referenceMark.length > 0) {
            itemValue = createReferenceMarkFromArray(referenceMark);
        } else {
            itemValue = '';
        }

        return <div className="cell-value-wrapper">{itemValue}</div>;
    }

    cellRenderer(row, rowIndex, col, colIndex, cellFocus) {
        const colValue = row[col.dataName];

        var displayValue;
        if (colValue && colValue.values) {
            displayValue = colValue.values.map(value => {
                let itemValue;
                if (hasDescItemTypeValue(col.dataType)) {
                    switch (col.dataType.code) {
                        case 'COORDINATES':
                            switch (value.geomType) {
                                case 'B':
                                    itemValue = value.geomType + ': ' + value.value;
                                    itemValue = (
                                        <span key={row.id + '-' + value.position}>
                                            <Button disabled>{value.geomType}</Button> {value.value}
                                        </span>
                                    );
                                    break;
                                default:
                                    itemValue = (
                                        <span key={row.id + '-' + value.position}>
                                            <Button disabled>{value.geomType}</Button>{' '}
                                            {i18n('subNodeForm.countOfCoordinates', value.value)}
                                        </span>
                                    );
                                    break;
                            }
                            break;
                        case 'UNITDATE':
                            itemValue = value.value;
                            break;
                        case 'JSON_TABLE':
                            itemValue = i18n(
                                'arr.fund.jsonTable.cell.title',
                                col.refType.viewDefinition.length,
                                value.rows,
                            );
                            break;
                        case 'INT':
                            const refType = col.refType;
                            if (refType.viewDefinition === DisplayType.DURATION) {
                                itemValue = toDuration(value.value);
                            } else {
                                itemValue = value.value;
                            }
                            break;
                        case 'DATE':
                            itemValue = Moment(value.value).format('l');
                            break;
                        default: {
                            const viewDefinition = col.refType.viewDefinition;
                            if (isMaskViewDefinition(viewDefinition)) {
                                itemValue = maskString(value.value || '', viewDefinition.mask);
                            } else {
                                itemValue = value.value;
                            }
                            break;
                        }
                    }
                }

                if (col.refType.useSpecification) {
                    var spec = null;
                    for (var a = 0; a < col.refType.descItemSpecs.length; a++) {
                        if (col.refType.descItemSpecs[a].code === value.specCode) {
                            spec = col.refType.descItemSpecs[a];
                            break;
                        }
                    }

                    // if enum is without data, it has no specification
                    return (
                        <div className="cell-value-wrapper">                            
                            {hasDescItemTypeValue(col.dataType) ? spec.name + ': ' + itemValue : (spec ? spec.name : '')}
                        </div>
                    );
                } else {
                    return <div className="cell-value-wrapper">{itemValue}</div>;
                }
            });
        }

        return <div className="">{displayValue}</div>;
    }

    supportBulkModifications(refType, dataType) {
        const { closed } = this.props;

        if (refType.id === COL_REFERENCE_MARK) {
            return false;
        }

        return !closed;
        /*
         let result

         switch (dataType.code) {
         case 'TEXT':
         case 'STRING':
         case 'FORMATTED_TEXT':
         case 'UNITID':
         result = true
         break
         default:
         result = false
         break
         }
         return result*/
    }

    headerColRenderer(col) {
        const { fundDataGrid, readMode } = this.props;

        var cls = 'cell';

        let filtered;
        if (fundDataGrid.filter[col.refType.id]) {
            cls += ' filtered';
        }

        const showBulkModifications = this.supportBulkModifications(col.refType, col.dataType);

        return (
            <div className={cls} title={col.refType.name}>
                <div className="title">{col.refType.shortcut}</div>
                {showBulkModifications && !readMode && (
                    <Button
                        onClick={this.handleBulkModifications.bind(this, col.refType, col.dataType)}
                        title={i18n('arr.fund.bulkModifications.action')}
                    >
                        <Icon glyph="fa-pencil" />
                    </Button>
                )}
                <Button
                    onClick={this.handleFilterSettings.bind(this, col.refType, col.dataType)}
                    title={i18n('arr.fund.filterSettings.action')}
                >
                    <Icon glyph="fa-filter" />
                </Button>
            </div>
        );
    }

    getColsStateFromProps(nextProps, props) {
        const { fundDataGrid, descItemTypes, rulDataTypes } = nextProps;

        if (descItemTypes.fetched) {
            if (
                props.fundDataGrid.columnsOrder !== fundDataGrid.columnsOrder ||
                props.descItemTypes !== descItemTypes ||
                props.rulDataTypes !== rulDataTypes ||
                props.fundDataGrid.columnInfos !== fundDataGrid.columnInfos ||
                props.fundDataGrid.filter !== fundDataGrid.filter ||
                props.fundDataGrid.visibleColumns !== fundDataGrid.visibleColumns
            ) {
                const cols = this.buildColumns(
                    nextProps.fund,
                    nextProps.ruleSet,
                    fundDataGrid,
                    descItemTypes,
                    rulDataTypes,
                );
                return { cols: cols };
            }
        }
        return null;
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['versionId', 'descItemTypes', 'ruleSet', 'rulDataTypes', 'closed', 'readMode'];
        if (!propsEquals(this.props, nextProps, eqProps)) {
            return true;
        }

        var eqProps2 = [
            'isFetchingFilter',
            'fetchedFilter',
            'isFetchingData',
            'fetchedData',
            'pageSize',
            'pageIndex',
            'closed',
            'readMode',
            'items',
            'itemsCount',
            'filter',
            'visibleColumns',
            'initialised',
            'columnsOrder',
            'columnInfos',
            'selectedIds',
        ];
        if (!propsEquals(this.props.fundDataGrid, nextProps.fundDataGrid, eqProps2)) {
            return true;
        }

        return false;
    }

    getColumnsOrder(prevColumnsOrder = [], gridViews = []) {
        // If prev columns order exists, use it, otherwise use default gridViews
        if(prevColumnsOrder.length > 0){
            return prevColumnsOrder;
        }
        return (gridViews || []).map(({id}) => id.toString());
    }

    buildColumns(fund, ruleSet, fundDataGrid, descItemTypes, rulDataTypes) {
        const refTypesMap = descItemTypes.itemsMap;
        const dataTypesMap = getMapFromList(rulDataTypes.items);
        const {gridViews} = getMapFromList(ruleSet.items)[fund.activeVersion.ruleSetId];

        const columnsOrder = this.getColumnsOrder(fundDataGrid.columnsOrder, gridViews);

        let cols = [];

        // Sloupec s číslem JP
        const refMarkColInfo = fundDataGrid.columnInfos[COL_REFERENCE_MARK];
        cols.push({
            id: COL_REFERENCE_MARK,
            refType: {
                id: COL_REFERENCE_MARK,
                code: COL_REFERENCE_MARK,
                shortcut: i18n('arr.fund.title.referendeMark'),
            },
            title: i18n('arr.fund.title.referendeMark'),
            width: refMarkColInfo ? refMarkColInfo.width : COL_DEFAULT_WIDTH,
            headerColRenderer: this.headerColRenderer,
            cellRenderer: this.referenceMarkCellRenderer,
        });

        columnsOrder.forEach(id => {
            const refType = refTypesMap[id];
            if (fundDataGrid.visibleColumns[id]) {
                // je vidět
                const colInfo = fundDataGrid.columnInfos[id];

                let width;
                if (colInfo) {
                    // je již definovaná
                    width = colInfo.width;
                } else if (gridViews && gridViews[id]?.width) {
                    // můžeme použít z pravidel
                    width = gridViews[id].width;
                } else {
                    // dáme implicitní konstantu, nikde ji nemáme
                    width = COL_DEFAULT_WIDTH;
                }

                const col = {
                    // id musi byt cislo (nikoliv string) kvuli editaci hodnot, resp.
                    // zachovani funkce subNodeForm
                    id: refType.id,
                    refType,
                    dataType: dataTypesMap[refType.dataTypeId],
                    title: refType.shortcut,
                    desc: refType.name,
                    width,
                    dataName: refType.id,
                    headerColRenderer: this.headerColRenderer,
                    cellRenderer: this.cellRenderer,
                };
                cols.push(col);
            }
        });

        return cols;
    }

    handleColumnResize(colIndex, width) {
        const { versionId } = this.props;
        this.props.dispatch(fundDataGridSetColumnSize(versionId, this.state.cols[colIndex].id, width));
    }

    handleSelectedIdsChange(ids) {
        this.props.onSetSelectedIds(ids);
    }

    handleFilterClearAll() {
        const { versionId, dispatch, fundId, history } = this.props;
        // Reset to the first page and clear selection/focus - the filtered result set changed.
        this.props.onSetPageIndex(0);
        dispatch(fundDataGridFilterClearAll(versionId));
        dispatch(storeSave()); // musíme uložit ihned store, abychom se vyhnuli problémům s opožděným savem
        history.push(urlFundGrid(fundId));
    }

    handleFilterUpdateData() {
        const { versionId } = this.props;
        this.props.dispatch(fundDataGridRefreshRows(versionId));
    }

    handleColumnSettings() {
        const { fundDataGrid: {columnsOrder, visibleColumns}, descItemTypes, fund, ruleSet } = this.props;
        const refTypesMap = descItemTypes.itemsMap;
        const { gridViews } = getMapFromList(ruleSet.items)[fund.activeVersion.ruleSetId];

        const selectedColumnsIds = this.getColumnsOrder(columnsOrder, gridViews);

        const selectedColumns = selectedColumnsIds.map((id) => {
            const { name, description, code} = refTypesMap[id];
            return { id, name, desc: description, code };
        });

        const unselectedColumns = descItemTypes.items
        .filter(({id}) => !selectedColumns.find(({id: _id}) => _id.toString() === id.toString()))
        .map(({ id, name, description, code}) => {
            return { id, name, desc: description, code };
        });

        WebApi.getItemTypeCodesByRuleSet(fund.activeVersion.ruleSetId).then(items => {
            // this.setState({itemTypeCodes: items});
            // const {itemTypeCodes} = items;
            this.props.dispatch(
                modalDialogShow(
                    this,
                    i18n('arr.fund.columnSettings.title'),
                    <DataGridColumnsSettings
                        onSubmitForm={this.handleChangeColumnsSettings}
                        columns={[...selectedColumns, ...unselectedColumns]}
                        visibleColumns={visibleColumns}
                        itemTypeCodes={items}
                    />,
                    "dialog-md"
                ),
            );
        });
    }

    handleFilterSettings(refType, dataType) {
        const { versionId, fundDataGrid } = this.props;

        const otherFilters = { ...fundDataGrid.filter };
        delete otherFilters[refType.id];

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.filterSettings.title', refType.shortcut),
                <FundFilterSettings
                    versionId={versionId}
                    refType={refType}
                    dataType={dataType}
                    filter={fundDataGrid.filter[refType.id]}
                    filters={otherFilters}
                    onSubmitForm={this.handleChangeFilter.bind(this, versionId, refType)}
                />,
                'fund-filter-settings-dialog',
            ),
        );
    }

    handleChangeFilter(versionId, refType, _filter) {
        const filter = { ...(this.props.fundDataGrid?.filter || {}) };

        if (refType != null) {
            // null je pro případ, kdy jen chceme aktualizovat data
            if (filter && _filter) {
                filter[refType.id] = _filter;
            } else {
                delete filter[refType.id];
            }
        }

        // Reset to the first page and clear selection/focus - the filtered result set changed.
        this.props.onSetPageIndex(0);
        this.props.dispatch(modalDialogHide());
        this.props.dispatch(fundDataGridFilterChange(versionId, filter));
        this.setFilterUrl(filter);
    }

    handleBulkModifications(refType, dataType) {
        const { versionId, fundDataGrid, structureTypes, dispatch } = this.props;

        const submit = async (data) => {
            // Sestavení seznamu node s id a verzí, pro které se má daná operace provést
            let nodes;
            let selectionType;
            switch (data.itemsArea) {
                case 'page':
                    nodes = fundDataGrid.items.map(i => ({ id: i.node.id, version: i.node.version }));
                    selectionType = 'NODES';
                    break;
                case 'selected': {
                    const set = getSetFromIdsList(this.props.view.selectedIds);
                    nodes = [];
                    selectionType = 'NODES';
                    fundDataGrid.items.forEach(i => {
                        if (set[i.id]) {
                            nodes.push({ id: i.node.id, version: i.node.version });
                        }
                    });
                    break;
                }
                case 'unselected': {
                    nodes = [];
                    selectionType = 'NODES';
                    const set = getSetFromIdsList(this.props.view.selectedIds);
                    fundDataGrid.items.forEach(i => {
                        if (!set[i.id]) {
                            nodes.push({ id: i.node.id, version: i.node.version });
                        }
                    });
                    break;
                }
                case 'all':
                    nodes = [];
                    selectionType = 'FUND';
                    break;
                default:
                    break;
            }

            // Zpracování hodnoty pro odeslání - musíme ji správně převést do testového formáltu pro odeslání na serveru
            let replaceText = data.replaceText;
            let description = undefined;
            if (replaceText) {
                switch (dataType.code) {
                    case 'UNITDATE':
                        if (typeof replaceText === 'object') {
                            replaceText = data.replaceText.value;
                        }
                        break;
                    case 'RECORD_REF':
                        replaceText = data.replaceText.id;
                        break;
                    case 'URI_REF':
                        replaceText = data.replaceText.value;
                        description = data.replaceText.description;
                        break;
                    default:
                        // standardně nic neděláme, zpracovávají se jen speciální typy
                        break;
                }
            }

            const response = selectionType !== 'FUND' || await dispatch(showConfirmDialog(i18n('arr.fund.bulkModifications.warn')));
            if (response) {
                return this.props.dispatch(
                    fundBulkModifications(
                        versionId,
                        refType.id,
                        data.specIds,
                        data.operationType,
                        data.findText,
                        replaceText,
                        description,
                        data.replaceSpec,
                        nodes,
                        selectionType,
                        data.replaceValueId,
                        [],
                    ),
                );
            }
        };

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.bulkModifications.title'),
                <FundBulkModificationsForm
                    refType={refType}
                    dataType={dataType}
                    onSubmitForm={submit}
                    allItemsCount={fundDataGrid.items.length}
                    checkedItemsCount={this.props.view.selectedIds.length}
                    versionId={versionId}
                    structureTypes={structureTypes}
                />,
            ),
        );
    }

    handleChangeColumnsSettings(columns) {
        const { versionId } = this.props;

        var visibleColumns = {};
        var columnsOrder = [];
        columns.forEach(col => {
            visibleColumns[col.id] = true;
            columnsOrder.push(col.id);
        });
        this.props.dispatch(fundDataGridSetColumnsSettings(versionId, visibleColumns, columnsOrder));

        this.props.dispatch(modalDialogHide());
    }

    handleContextMenu(row, rowIndex, col, colIndex, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
                <Dropdown.Item onClick={this.handleSelectInNewTab.bind(this, row)}>
                    {i18n('arr.fund.bulkModifications.action.openInNewTab')}
                </Dropdown.Item>
                <Dropdown.Item onClick={this.handleSelectInTab.bind(this, row)}>
                    {i18n('arr.fund.bulkModifications.action.open')}
                </Dropdown.Item>
                <Dropdown.Item
                    onClick={() => {
                        this.props.dispatch(contextMenuHide());
                        this.handleEdit(row, rowIndex, col, colIndex);
                    }}
                >
                    {i18n('global.action.update')}
                </Dropdown.Item>
            </ul>
        );

        this.props.dispatch(contextMenuShow(this, menu, { x: e.clientX, y: e.clientY }));
    }

    handleEdit(row, rowIndex, col, colIndex) {
        if (typeof col.id === 'undefined') {
            return;
        }

        if (col.id === COL_REFERENCE_MARK) {
            return;
        }

        const { versionId } = this.props;
        const dataGridComp = this.dataGridRef;
        const cellEl = dataGridComp.getCellElement(rowIndex, colIndex);

        this.setState({
            cellForm: {
                fondsVersionId: versionId,
                nodeId: row.node.id,
                nodeVersionId: row.node.version,
                descItemTypeId: col.id,
                target: cellEl,
            },
        });
    }

    handleEditClose = () => {
        this.setState({ cellForm: null }, () => {
            this.dataGridRef.focus();
        });
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInNewTab(row) {
        const { versionId, fund } = this.props;
        this.props.dispatch(contextMenuHide());

        var parentNode = row.parentNode;
        if (parentNode == null) {
            // root
            parentNode = createFundRoot(fund);
        }

        this.props.dispatch(fundSelectSubNode(versionId, row.node.id, parentNode, true, null, true, undefined, undefined, true));
    }

    handleFulltextChange(value) {
        const { versionId } = this.props;
        this.props.dispatch(fundDataFulltextClear(versionId));
    }

    handleFulltextSearch(value) {
        const { versionId } = this.props;
        this.props.dispatch(fundDataFulltextSearch(versionId, value, false, null, this.props.fundDataGrid.data));
    }

    handleFulltextPrevItem() {
        this.props.onFulltextPrevItem();
    }

    handleFulltextNextItem() {
        this.props.onFulltextNextItem();
    }

    handleChangeFocus(row, col) {
        this.props.onSetCellFocus(row, col);
        this.updateCellUrl(row, col);
    }

    /**
     * Reflects the focused cell in the URL (.../grid/{nodeId}/{descItemTypeId}) so it can be shared or
     * survive a reload. Uses replace, not push, to avoid polluting the browser history on every cell move.
     */
    updateCellUrl(row, col) {
        const { fund, history, fundDataGrid } = this.props;

        const item = fundDataGrid.items[row];
        const nodeId = item?.node?.id;
        if (nodeId == null) {
            return;
        }

        const colDef = this.state.cols[this.toColsIndex(col)];
        const typeId = colDef && colDef.refType.id !== COL_REFERENCE_MARK ? colDef.refType.id : undefined;

        history.replace(urlFundGrid(fund.id, undefined, fundDataGrid.serializedFilter, nodeId, typeId));

        // Persist the highlight (node + attribute type) so a later reload can restore this cell.
        this.props.rememberRestoreNode(nodeId, typeId);
    }

    // The DataGrid prepends a checkbox column, so its focus column index is offset by one from
    // this.state.cols (which has no checkbox). These translate between the two indexings.
    toColsIndex(gridCol) {
        return gridCol - CHECKBOX_COLUMN_OFFSET;
    }

    toGridColumn(colsIndex) {
        return colsIndex + CHECKBOX_COLUMN_OFFSET;
    }

    handleChangeRowIndexes(indexes) {
        this.props.onSetRowIndexes(indexes);
    }

    /**
     * Otevření uzlu v záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInTab(row) {
        const { versionId, fund } = this.props;
        this.props.dispatch(contextMenuHide());

        var parentNode = row.parentNode;
        if (parentNode == null) {
            // root
            parentNode = createFundRoot(fund);
        }

        this.props.dispatch(fundSelectSubNode(versionId, row.node.id, parentNode, false, null, true, undefined, undefined, true));
    }

    handleToggleExtendedSearch() {
        const { versionId } = this.props;
        this.props.dispatch(fundDataFulltextExtended(versionId));
    }

    handleExtendedSearch = () => {
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('search.extended.title'),
                <ArrSearchForm
                    onSubmitForm={this.handleExtendedSearchData}
                    initialValues={this.props.fundDataGrid.data}
                />,
            ),
        );
    };

    handleExtendedSearchData = result => {
        const { versionId } = this.props;

        let params = [];

        let text = null;
        switch (result.type) {
            case 'FORM': {
                result.condition.forEach((conditionItem, index) => {
                    let param = {};
                    param.type = conditionItem.type;
                    param.value = conditionItem.value;
                    switch (conditionItem.type) {
                        case 'TEXT': {
                            param[JAVA_ATTR_CLASS] = '.TextSearchParam';
                            break;
                        }
                        case 'UNITDATE': {
                            param[JAVA_ATTR_CLASS] = '.UnitdateSearchParam';
                            param.condition = conditionItem.condition;
                            break;
                        }
                        default:
                            break;
                    }
                    params.push(param);
                });
                break;
            }

            case 'TEXT': {
                text = result.text;
                break;
            }
            default:
                break;
        }

        return this.props.dispatch(fundDataFulltextSearch(versionId, text, true, params, result));
    };

    render() {
        const { fundId, fund, fundDataGrid, versionId, rulDataTypes, descItemTypes, dispatch, readMode, view } = this.props;
        const { cols, cellForm } = this.state;

        // Hledání
        var search = (
            <SearchWithGoto
                itemsCount={fundDataGrid.searchedItems.length}
                selIndex={fundDataGrid.searchedCurrentIndex}
                textAreaInput={fundDataGrid.searchExtended}
                filterText={fundDataGrid.luceneQuery ? i18n('search.extended.label') : fundDataGrid.searchText}
                placeholder={i18n(fundDataGrid.searchExtended ? 'arr.fund.extendedSearch.text' : 'search.input.search')}
                showFilterResult={fundDataGrid.showFilterResult}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextChange={this.handleFulltextChange}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
                extendedSearch
                extendedReadOnly={fundDataGrid.luceneQuery}
                onClickExtendedSearch={this.handleExtendedSearch}
            />
        );

        // ---
        return (
            <div ref="gridContainer" className="fund-datagrid-container-wrap">
                <div ref="grid" className="fund-datagrid-container">
                    <div className="actions-container">
                        <div className="actions-search">{search}</div>
                        <div className="actions-buttons">
                            <Button
                                className="update"
                                disabled={!(fundDataGrid.rowsDirty || fundDataGrid.filterDirty)}
                                onClick={this.handleFilterUpdateData}
                            >
                                <Icon glyph="fa-refresh" />
                                {i18n('arr.fund.filterSettings.updateData.action')}
                            </Button>
                            <Button onClick={this.handleFilterClearAll}>
                                <Icon glyph="fa-trash" />
                                {i18n('arr.fund.filterSettings.clearAll.action')}
                            </Button>
                            <Button onClick={this.handleColumnSettings} title={i18n('arr.fund.columnSettings.action')}>
                                <Icon glyph="fa-columns" />
                            </Button>
                        </div>
                    </div>
                    <StoreHorizontalLoader
                        store={{
                            isFetching:
                                fundDataGrid.isFetchingData ||
                                fundDataGrid.isFetchingFilter ||
                                descItemTypes.isFetching ||
                                rulDataTypes.isFetching,
                            fetched:
                                fundDataGrid.fetchedData ||
                                fundDataGrid.fetchedFilter ||
                                descItemTypes.fetched ||
                                rulDataTypes.fetched,
                        }}
                    />
                    <div className="grid-container">
                        <DataGrid
                            ref={ref => (this.dataGridRef = ref)}
                            rows={fundDataGrid.items}
                            cols={cols}
                            focusRow={view.cellFocus.row}
                            focusCol={view.cellFocus.col}
                            selectedIds={view.selectedIds}
                            selectedRowIndexes={view.selectedRowIndexes}
                            onColumnResize={this.handleColumnResize}
                            onChangeFocus={this.handleChangeFocus}
                            onChangeRowIndexes={this.handleChangeRowIndexes}
                            onSelectedIdsChange={this.handleSelectedIdsChange}
                            onContextMenu={this.handleContextMenu}
                            onEdit={this.handleEdit}
                            disabled={readMode}
                            startRowIndex={view.pageSize * view.pageIndex}
                            morePages={getPagesCount(fundDataGrid.itemsCount, view.pageSize) > 1}
                        />
                        <DataGridPagination
                            itemsCount={fundDataGrid.itemsCount}
                            pageSize={view.pageSize}
                            pageIndex={view.pageIndex}
                            onSetPageIndex={this.props.onSetPageIndex}
                            onChangePageSize={this.props.onSetPageSize}
                        />
                    </div>
                </div>
                {cellForm && (
                    <FundDataGridCellForm
                        fondsVersionId={cellForm.fondsVersionId}
                        nodeId={cellForm.nodeId}
                        nodeVersionId={cellForm.nodeVersionId}
                        descItemTypeId={cellForm.descItemTypeId}
                        target={cellForm.target}
                        onClose={this.handleEditClose}
                    />
                )}
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { splitter } = state;
    return {
        splitter,
    };
}

export const FundDataGridConnected = withRouter(connect(mapStateToProps)(FundDataGridClass));
