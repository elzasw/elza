/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    FundBulkModificationsForm,
    Icon,
    DataGridColumnsSettings,
    AbstractReactComponent,
    i18n,
    Loading,
    DataGrid,
    FundFilterSettings,
    DataGridPagination,
    FundDataGridCellForm,
    SearchWithGoto
} from 'components/index.jsx';
import {Button, MenuItem} from 'react-bootstrap';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {
    fundDataGridSetColumnsSettings,
    fundDataGridSetSelection,
    fundDataGridSetColumnSize,
    fundDataGridFetchFilterIfNeeded,
    fundDataGridFetchDataIfNeeded,
    fundDataGridSetPageIndex,
    fundDataGridSetPageSize,
    fundDataGridFilterChange,
    fundBulkModifications,
    fundDataGridFilterClearAll,
    fundDataGridPrepareEdit,
    fundDataFulltextSearch,
    fundDataFulltextPrevItem,
    fundDataFulltextNextItem,
    fundDataChangeCellFocus,
    fundDataChangeRowIndexes,
    fundDataFulltextClear,
    fundDataFulltextExtended,
    fundDataInitIfNeeded,
    fundDataGridRefreshRows
} from 'actions/arr/fundDataGrid.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {fundSelectSubNode} from 'actions/arr/nodes.jsx'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx'
import {
    createReferenceMarkFromArray,
    getSpecsIds,
    hasDescItemTypeValue,
    createFundRoot
} from 'components/arr/ArrUtils.jsx'
import {getMapFromList, getSetFromIdsList} from 'stores/app/utils.jsx'
import {propsEquals} from 'components/Utils.jsx'
import {COL_DEFAULT_WIDTH, COL_REFERENCE_MARK} from "./FundDataGridConst";
require('./FundDataGrid.less')

var FundDataGrid = class FundDataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelectedIdsChange', 'handleColumnResize', 'handleColumnSettings', 'handleChangeColumnsSettings',
            'handleBulkModifications', 'handleFilterSettings', 'headerColRenderer', 'cellRenderer', 'resizeGrid', 'handleFilterClearAll',
            'handleFilterUpdateData', 'handleContextMenu', 'handleSelectInNewTab', 'handleSelectInTab', 'handleEdit', 'handleEditClose',
            'handleFulltextSearch', 'handleFulltextChange', 'handleFulltextPrevItem', 'handleFulltextNextItem', 'handleChangeFocus',
            'handleToggleExtendedSearch', 'handleChangeRowIndexes', 'fetchData');

        var colState = this.getColsStateFromProps(props, {fundDataGrid: {}})
        if (!colState) {
            colState = {cols: []}
        }

        colState.calendarTypesMap = getMapFromList(props.calendarTypes.items)

        this.state = colState
    }

    componentDidMount() {
        this.fetchData(this.props)

        this.setState({}, this.resizeGrid)

        // Pokud je potřeba aktualizovat, aktualizujeme při přepnutí na grid, ale zachováme stránkování
        const {versionId, fundDataGrid} = this.props
        if (fundDataGrid.rowsDirty || fundDataGrid.filterDirty) {
            this.dispatch(fundDataGridRefreshRows(versionId));
        }
    }

    fetchData(props) {
        const {fundDataGrid, descItemTypes, fund, versionId, ruleSet} = props;
        this.dispatch(descItemTypesFetchIfNeeded())
        this.dispatch(packetTypesFetchIfNeeded())
        this.dispatch(refRulDataTypesFetchIfNeeded())
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))
        this.dispatch(refRuleSetFetchIfNeeded())
        if (ruleSet.fetched && descItemTypes.fetched && fund.activeVersion) {
            var initData = {visibleColumns: []}
            var ruleMap = getMapFromList(ruleSet.items);
            var rule = ruleMap[fund.activeVersion.ruleSetId];
            var codeSet = getSetFromIdsList(rule.defaultItemTypeCodes);
            descItemTypes.items.forEach(item => {
                if (codeSet[item.code]) {
                    initData.visibleColumns.push(item.id);
                }
            })
            this.dispatch(fundDataInitIfNeeded(versionId, initData));
        }
    }

    componentWillReceiveProps(nextProps) {
        this.fetchData(nextProps)

        var colState = this.getColsStateFromProps(nextProps, this.props)
        if (!colState) {
            colState = {}
        }

        colState.calendarTypesMap = getMapFromList(nextProps.calendarTypes.items)

        this.setState(colState, this.resizeGrid)
    }

    resizeGrid() {
        const parentEl = ReactDOM.findDOMNode(this.refs.gridContainer)
        const gridEl = ReactDOM.findDOMNode(this.refs.grid)
        if (parentEl && gridEl) {
            const rect = parentEl.getBoundingClientRect()
            const width = rect.right - rect.left
            gridEl.style.width = width + 'px'
        }
    }

    referenceMarkCellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        const referenceMark = row.referenceMark;

        let itemValue;
        if (referenceMark && referenceMark.length > 0) {
            itemValue = createReferenceMarkFromArray(referenceMark);
        } else {
            itemValue = "";
        }

        return <div className='cell-value-wrapper'>{itemValue}</div>
    }

    cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        const colValue = row[col.dataName]

        var displayValue
        if (colValue && colValue.values) {
            displayValue = colValue.values.map(value => {
                let itemValue
                if (hasDescItemTypeValue(col.dataType)) {
                    switch (col.dataType.code) {
                        case 'COORDINATES':
                            switch (value.geomType) {
                                case "B":
                                    itemValue = value.geomType + ": " + value.value
                                    itemValue = <span><Button disabled>{value.geomType}</Button> {value.value}</span>
                                    break
                                default:
                                    itemValue = <span><Button
                                        disabled>{value.geomType}</Button> {i18n('subNodeForm.countOfCoordinates', value.value)}</span>
                                    break
                            }
                            break
                        case 'UNITDATE':
                            itemValue = this.state.calendarTypesMap[value.calendarTypeId].name.charAt(0) + ": " + value.value
                            break
                        case 'JSON_TABLE':
                            itemValue = i18n("arr.fund.jsonTable.cell.title", col.refType.columnsDefinition.length, value.rows);
                            break
                        default:
                            itemValue = value.value
                            break
                    }
                }

                if (col.refType.useSpecification) {
                    var spec = null
                    for (var a = 0; a < col.refType.descItemSpecs.length; a++) {
                        if (col.refType.descItemSpecs[a].code === value.specCode) {
                            spec = col.refType.descItemSpecs[a]
                            break
                        }
                    }

                    return <div
                        className='cell-value-wrapper'>{hasDescItemTypeValue(col.dataType) ? spec.name + ': ' + itemValue : spec.name}</div>
                } else {
                    return <div className='cell-value-wrapper'>{itemValue}</div>
                }
            })
        }

        return (
            <div className=''>{displayValue}</div>
        )
    }

    supportBulkModifications(refType, dataType) {
        const {closed} = this.props

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
        const {fundDataGrid, readMode} = this.props

        var cls = 'cell'

        let filtered
        if (fundDataGrid.filter[col.refType.id]) {
            cls += ' filtered'
        }

        const showBulkModifications = this.supportBulkModifications(col.refType, col.dataType)

        return (
            <div className={cls} title={col.refType.name}>
                <div className="title">{col.refType.shortcut}</div>
                {showBulkModifications && !readMode &&
                <Button onClick={this.handleBulkModifications.bind(this, col.refType, col.dataType)}
                        title={i18n('arr.fund.bulkModifications.action')}><Icon glyph='fa-pencil'/></Button>}
                <Button onClick={this.handleFilterSettings.bind(this, col.refType, col.dataType)}
                        title={i18n('arr.fund.filterSettings.action')}><Icon glyph='fa-filter'/></Button>
            </div>
        )
    }

    getColsStateFromProps(nextProps, props) {
        const {fundDataGrid, descItemTypes, packetTypes, rulDataTypes} = nextProps;

        if (descItemTypes.fetched) {
            if (props.fundDataGrid.columnsOrder !== fundDataGrid.columnsOrder
                || props.descItemTypes !== descItemTypes
                || props.packetTypes !== packetTypes
                || props.rulDataTypes !== rulDataTypes
                || props.fundDataGrid.columnInfos !== fundDataGrid.columnInfos
                || props.fundDataGrid.filter !== fundDataGrid.filter
                || props.fundDataGrid.visibleColumns !== fundDataGrid.visibleColumns
            ) {
                const cols = this.buildColumns(fundDataGrid, descItemTypes, rulDataTypes)
                return {cols: cols}
            }
        }
        return null
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['versionId', 'descItemTypes', 'ruleSet', 'packetTypes', 'rulDataTypes', 'closed', 'readMode']
        if (!propsEquals(this.props, nextProps, eqProps)) {
            return true
        }

        var eqProps2 = [
            'isFetchingFilter', 'fetchedFilter', 'isFetchingData', 'fetchedData', 'pageSize', 'pageIndex', 'closed', 'readMode',
            'items', 'itemsCount', 'filter', 'visibleColumns', 'initialised', 'columnsOrder', 'columnInfos', 'selectedIds'
        ]
        if (!propsEquals(this.props.fundDataGrid, nextProps.fundDataGrid, eqProps2)) {
            return true
        }

        return false
    }

    getColumnsOrder(fundDataGrid, refTypesMap) {
        // Pořadí sloupečků - musíme brát i variantu, kdy není definované nebo kdy v něm některé atributy chybí
        var columnsOrder = []
        var map = {...refTypesMap}
        fundDataGrid.columnsOrder.forEach(id => {
            delete map[id]
            columnsOrder.push(id)
        })
        columnsOrder = [...columnsOrder, ...Object.keys(map)]
        return columnsOrder
    }

    buildColumns(fundDataGrid, descItemTypes, rulDataTypes) {
        const refTypesMap = getMapFromList(descItemTypes.items)
        const dataTypesMap = getMapFromList(rulDataTypes.items)
        const columnsOrder = this.getColumnsOrder(fundDataGrid, refTypesMap)

        var cols = []

        // Sloupec s číslem JP
        const refMarkColInfo = fundDataGrid.columnInfos[COL_REFERENCE_MARK];
        cols.push({
            id: COL_REFERENCE_MARK,
            refType: {
                id: COL_REFERENCE_MARK,
                code: COL_REFERENCE_MARK,
                shortcut: i18n("arr.fund.title.referendeMark"),
            },
            title: i18n("arr.fund.title.referendeMark"),
            width: refMarkColInfo ? refMarkColInfo.width : COL_DEFAULT_WIDTH,
            headerColRenderer: this.headerColRenderer,
            cellRenderer: this.referenceMarkCellRenderer
        });

        // Vybrané sloupce
        columnsOrder.forEach(id => {
            const refType = refTypesMap[id]
            if (fundDataGrid.visibleColumns[id]) {  // je vidět
                const colInfo = fundDataGrid.columnInfos[id]

                const col = {
                    id: refType.id,
                    refType: refType,
                    dataType: dataTypesMap[refType.dataTypeId],
                    title: refType.shortcut,
                    desc: refType.name,
                    width: colInfo ? colInfo.width : COL_DEFAULT_WIDTH,
                    dataName: refType.id,
                    headerColRenderer: this.headerColRenderer,
                    cellRenderer: this.cellRenderer,
                }
                cols.push(col)
            }
        })

        return cols
    }

    handleColumnResize(colIndex, width) {
        const {versionId} = this.props
        this.dispatch(fundDataGridSetColumnSize(versionId, this.state.cols[colIndex].id, width))
    }

    handleSelectedIdsChange(ids) {
        const {versionId} = this.props
        this.dispatch(fundDataGridSetSelection(versionId, ids))
    }

    handleFilterClearAll() {
        const {versionId} = this.props
        this.dispatch(fundDataGridFilterClearAll(versionId))
    }

    handleFilterUpdateData() {
        const {versionId} = this.props
        this.dispatch(fundDataGridRefreshRows(versionId))
    }

    handleColumnSettings() {
        const {fundDataGrid, descItemTypes, fund, ruleSet} = this.props;

        var ruleMap = getMapFromList(ruleSet.items);
        var rule = ruleMap[fund.activeVersion.ruleSetId];

        let itemTypes = [];
        descItemTypes.items.forEach(item => {
            if (rule.itemTypeCodes.indexOf(item.code) >= 0) {
                itemTypes.push(item);
            }
        });

        const refTypesMap = getMapFromList(itemTypes);
        const columnsOrder = this.getColumnsOrder(fundDataGrid, refTypesMap);
        const visibleColumns = fundDataGrid.visibleColumns;

        const columns = columnsOrder.map(id => {
            const refType = refTypesMap[id];
            return {
                id: refType.id,
                title: refType.shortcut,
                desc: refType.description
            }
        });

        this.dispatch(modalDialogShow(this, i18n('arr.fund.columnSettings.title'),
            <DataGridColumnsSettings
                onSubmitForm={this.handleChangeColumnsSettings}
                columns={columns}
                visibleColumns={visibleColumns}
            />
        ));
    }

    handleFilterSettings(refType, dataType) {
        const {versionId, calendarTypes, fundDataGrid, packetTypes} = this.props

        this.dispatch(modalDialogShow(this, i18n('arr.fund.filterSettings.title', refType.shortcut),
            <FundFilterSettings
                versionId={versionId}
                refType={refType}
                dataType={dataType}
                packetTypes={packetTypes}
                calendarTypes={calendarTypes}
                filter={fundDataGrid.filter[refType.id]}
                onSubmitForm={this.handleChangeFilter.bind(this, versionId, refType)}
            />, 'fund-filter-settings-dialog'
        ));
    }

    handleChangeFilter(versionId, refType, filter) {
        this.dispatch(modalDialogHide())
        this.dispatch(fundDataGridFilterChange(versionId, refType.id, filter))
    }

    handleBulkModifications(refType, dataType) {
        const {versionId, fundDataGrid} = this.props

        var submit = (data) => {
            // Sestavení seznamu node s id a verzí, pro které se má daná operace provést
            var nodes;
            switch (data.itemsArea) {
                case 'all':
                    nodes = fundDataGrid.items.map(i => ({id: i.node.id, version: i.node.version}))
                    break
                case 'selected': {
                    const set = getSetFromIdsList(fundDataGrid.selectedIds)
                    nodes = []
                    fundDataGrid.items.forEach(i => {
                        if (set[i.id]) {
                            nodes.push({id: i.node.id, version: i.node.version})
                        }
                    })
                    break
                }
                case 'unselected': {
                    nodes = []
                    const set = getSetFromIdsList(fundDataGrid.selectedIds)
                    fundDataGrid.items.forEach(i => {
                        if (!set[i.id]) {
                            nodes.push({id: i.node.id, version: i.node.version})
                        }
                    })
                    break
                }
            }

            // Získání seznam specifikací
            const specsIds = getSpecsIds(refType, data.specs.type, data.specs.ids)

            this.dispatch(fundBulkModifications(versionId, refType.id, specsIds, data.operationType, data.findText, data.replaceText, data.replaceSpec, nodes))
        }

        this.dispatch(modalDialogShow(this, i18n('arr.fund.bulkModifications.title'),
            <FundBulkModificationsForm
                refType={refType}
                dataType={dataType}
                onSubmitForm={submit}
                allItemsCount={fundDataGrid.items.length}
                checkedItemsCount={fundDataGrid.selectedIds.length}
            />
        ));
    }

    handleChangeColumnsSettings(columns) {
        const {versionId} = this.props;

        var visibleColumns = {};
        var columnsOrder = [];
        columns.forEach(col => {
            visibleColumns[col.id] = true;
            columnsOrder.push(col.id)
        });
        this.dispatch(fundDataGridSetColumnsSettings(versionId, visibleColumns, columnsOrder));

        this.dispatch(modalDialogHide())
    }

    handleContextMenu(row, rowIndex, col, colIndex, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
                <MenuItem
                    onClick={this.handleSelectInNewTab.bind(this, row)}>{i18n('arr.fund.bulkModifications.action.openInNewTab')}</MenuItem>
                <MenuItem
                    onClick={this.handleSelectInTab.bind(this, row)}>{i18n('arr.fund.bulkModifications.action.open')}</MenuItem>
                <MenuItem onClick={() => {
                    this.dispatch(contextMenuHide());
                    this.handleEdit(row, rowIndex, col, colIndex)
                }}>{i18n('global.action.update')}</MenuItem>
            </ul>
        );

        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y: e.clientY}));
    }

    handleEdit(row, rowIndex, col, colIndex) {
        const {versionId, fundId, closed} = this.props;
        const parentNodeId = row.parentNode ? row.parentNode.id : null;

        if (typeof col.id === 'undefined') {
            return
        }

        this.dispatch(fundDataGridPrepareEdit(versionId, row.id, parentNodeId, col.id));

        const dataGridComp = this.refs.dataGrid.getWrappedInstance();
        const cellEl = dataGridComp.getCellElement(rowIndex, colIndex);
        const cellRect = cellEl.getBoundingClientRect();

        this.dispatch(modalDialogShow(this, null,
            <FundDataGridCellForm
                versionId={versionId}
                fundId={fundId}
                routingKey='DATA_GRID'
                closed={closed}
                position={{x: cellRect.left, y: cellRect.top}}
            />,
            'fund-data-grid-cell-edit', this.handleEditClose));
    }

    handleEditClose() {
        const {versionId} = this.props;

        this.dispatch(nodeFormActions.fundSubNodeFormHandleClose(versionId, 'DATA_GRID'))

        this.setState({},
            ()=> {
                ReactDOM.findDOMNode(this.refs.dataGrid).focus()
            }
        )
    }


    /**
     * Otevření uzlu v nové záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInNewTab(row) {
        const {versionId, fund} = this.props;
        this.dispatch(contextMenuHide());

        var parentNode = row.parentNode;
        if (parentNode == null) {   // root
            parentNode = createFundRoot(fund);
        }

        this.dispatch(fundSelectSubNode(versionId, row.node.id, parentNode, true, null, true));
    }

    handleFulltextChange(value) {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextClear(versionId))
    }

    handleFulltextSearch(value) {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextSearch(versionId, value))
    }

    handleFulltextPrevItem() {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextPrevItem(versionId))
    }

    handleFulltextNextItem() {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextNextItem(versionId))
    }

    handleChangeFocus(row, col) {
        const {versionId} = this.props;
        this.dispatch(fundDataChangeCellFocus(versionId, row, col))
    }

    handleChangeRowIndexes(indexes) {
        const {versionId} = this.props;
        this.dispatch(fundDataChangeRowIndexes(versionId, indexes))
    }

    /**
     * Otevření uzlu v záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInTab(row) {
        const {versionId, fund} = this.props;
        this.dispatch(contextMenuHide());

        var parentNode = row.parentNode
        if (parentNode == null) {   // root
            parentNode = createFundRoot(fund);
        }

        this.dispatch(fundSelectSubNode(versionId, row.node.id, parentNode, false, null, true));
    }

    handleToggleExtendedSearch() {
        const {versionId} = this.props
        this.dispatch(fundDataFulltextExtended(versionId))
    }

    render() {
        const {fundId, fund, fundDataGrid, versionId, rulDataTypes, descItemTypes, packetTypes, dispatch, readMode} = this.props;
        const {cols} = this.state;

        if (!descItemTypes.fetched || !packetTypes.fetched || !rulDataTypes.fetched) {
            // if (!fundDataGrid.fetchedFilter || !descItemTypes.fetched || !packetTypes.fetched || !rulDataTypes.fetched) {
            return <Loading/>
        }

        // Hledání
        var search = (
            <SearchWithGoto
                itemsCount={fundDataGrid.searchedItems.length}
                selIndex={fundDataGrid.searchedCurrentIndex}
                textAreaInput={fundDataGrid.searchExtended}
                filterText={fundDataGrid.searchText}
                placeholder={i18n(fundDataGrid.searchExtended ? 'arr.fund.extendedSearch.text' : 'search.input.search')}
                showFilterResult={fundDataGrid.showFilterResult}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextChange={this.handleFulltextChange}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
            />
        )

        // ---
        return (
            <div ref='gridContainer' className='fund-datagrid-container-wrap'>
                <div ref='grid' className='fund-datagrid-container'>
                    <div className='actions-container'>
                        <div className="actions-search">
                            {search}
                            <Button onClick={this.handleToggleExtendedSearch}
                                    title={i18n(fundDataGrid.searchExtended ? 'arr.fund.simpleSearch' : 'arr.fund.extendedSearch')}><Icon
                                glyph={fundDataGrid.searchExtended ? 'fa-search-minus' : 'fa-search-plus'}/></Button>
                        </div>
                        <div className="actions-buttons">
                            <Button
                                disabled={!(fundDataGrid.rowsDirty || fundDataGrid.filterDirty)}
                                onClick={this.handleFilterUpdateData}
                            ><Icon glyph='fa-refresh'/>{i18n('arr.fund.filterSettings.updateData.action')}</Button>
                            <Button onClick={this.handleFilterClearAll}><Icon
                                glyph='fa-trash'/>{i18n('arr.fund.filterSettings.clearAll.action')}</Button>
                            <Button onClick={this.handleColumnSettings}
                                    title={i18n('arr.fund.columnSettings.action')}><Icon glyph='fa-columns'/></Button>
                        </div>
                    </div>
                    <div className='grid-container'>
                        <DataGrid
                            ref='dataGrid'
                            rows={fundDataGrid.items}
                            cols={cols}
                            focusRow={fundDataGrid.cellFocus.row}
                            focusCol={fundDataGrid.cellFocus.col}
                            selectedIds={fundDataGrid.selectedIds}
                            selectedRowIndexes={fundDataGrid.selectedRowIndexes}
                            onColumnResize={this.handleColumnResize}
                            onChangeFocus={this.handleChangeFocus}
                            onChangeRowIndexes={this.handleChangeRowIndexes}
                            onSelectedIdsChange={this.handleSelectedIdsChange}
                            onContextMenu={this.handleContextMenu}
                            onEdit={this.handleEdit}
                            disabled={readMode}
                        />
                        <DataGridPagination
                            itemsCount={fundDataGrid.itemsCount}
                            pageSize={fundDataGrid.pageSize}
                            pageIndex={fundDataGrid.pageIndex}
                            onSetPageIndex={pageIndex => {
                                dispatch(fundDataGridSetPageIndex(versionId, pageIndex))
                            }}
                            onChangePageSize={pageSize => {
                                dispatch(fundDataGridSetPageSize(versionId, pageSize))
                            }}
                        />
                    </div>
                </div>
            </div>
        )
    }
}


FundDataGrid.propTypes = {
    fundId: React.PropTypes.number.isRequired,
    versionId: React.PropTypes.number.isRequired,
    fund: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    ruleSet: React.PropTypes.object.isRequired,
    readMode: React.PropTypes.bool.isRequired,
    closed: React.PropTypes.bool.isRequired,
};

function mapStateToProps(state) {
    const {splitter} = state;
    return {
        splitter
    }
}

module.exports = connect(mapStateToProps)(FundDataGrid);
