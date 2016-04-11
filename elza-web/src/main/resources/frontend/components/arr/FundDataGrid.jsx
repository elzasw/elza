/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FundBulkModificationsForm, Icon, ListBox, DataGridColumnsSettings, AbstractReactComponent, i18n, Loading,
    DataGrid, FundFilterSettings, DataGridPagination, FundDataGridCellForm} from 'components';
import {MenuItem} from 'react-bootstrap';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import * as types from 'actions/constants/ActionTypes';
import {fundDataGridSetColumnsSettings, fundDataGridSetSelection, fundDataGridSetColumnSize, fundDataGridFetchFilterIfNeeded,
    fundDataGridFetchDataIfNeeded, fundDataGridSetPageIndex, fundDataGridSetPageSize,
    fundDataGridFilterChange, fundBulkModifications, fundDataGridFilterClearAll, fundDataGridPrepareEdit, fundDataGridFilterUpdateData} from 'actions/arr/fundDataGrid'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {getSetFromIdsList, getMapFromList} from 'stores/app/utils'
import {propsEquals} from 'components/Utils'
import {Button} from 'react-bootstrap'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {getSpecsIds, hasDescItemTypeValue} from 'components/arr/ArrUtils'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'
import {fundSelectSubNode} from 'actions/arr/nodes'

require('./FundDataGrid.less')

var FundDataGrid = class FundDataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelectedIdsChange', 'handleColumnResize', 'handleColumnSettings', 'handleChangeColumnsSettings',
            'handleBulkModifications', 'handleFilterSettings', 'headerColRenderer', 'cellRenderer', 'resizeGrid', 'handleFilterClearAll',
            'handleFilterUpdateData', 'handleContextMenu', 'handleSelectInNewTab', 'handleSelectInTab', 'handleEdit', 'setFocusAfterCellEdit');

        var colState = this.getColsStateFromProps(props, {fundDataGrid: {}})
        if (!colState) {
            colState = {cols: []}
        }

        this.state = colState
    }

    componentDidMount() {
        const {fundDataGrid, versionId} = this.props;
        //this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.dispatch(descItemTypesFetchIfNeeded())
        this.dispatch(refRulDataTypesFetchIfNeeded())
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))

        this.setState({}, this.resizeGrid)
    }

    componentWillReceiveProps(nextProps) {
        const {fundDataGrid, versionId, descItemTypes} = nextProps;
        //this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.dispatch(descItemTypesFetchIfNeeded())
        this.dispatch(refRulDataTypesFetchIfNeeded())
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))

        const colState = this.getColsStateFromProps(nextProps, this.props)
        if (colState) {
            this.setState(colState, this.resizeGrid)
        } else {
            this.setState({}, this.resizeGrid)
        }
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

    cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        const colValue = row[col.dataName]

        var displayValue
        if (colValue && colValue.values) {
            displayValue = colValue.values.map(value => {
                if (col.refType.useSpecification) {
                    var spec = null
                    for (var a=0; a<col.refType.descItemSpecs.length; a++) {
                        if (col.refType.descItemSpecs[a].code === value.specCode) {
                            spec = col.refType.descItemSpecs[a]
                            break
                        }
                    }

                    return <div className='cell-value-wrapper'>{hasDescItemTypeValue(col.dataType) ? spec.name + ': ' + value.value : spec.name}</div>
                } else {
                    return <div className='cell-value-wrapper'>{value.value}</div>
                }
            })
        }

        return (
            <div className=''>{displayValue}</div>
        )
    }

    supportBulkModifications(refType, dataType) {

        return true
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
        const {fundDataGrid} = this.props

        var cls = ''

        let filtered
        if (fundDataGrid.filter[col.refType.id]) {
            cls += ' filtered'
        }

        const showBulkModifications = this.supportBulkModifications(col.refType, col.dataType)

        return (
            <div className={cls} title={col.refType.name}>
                {col.refType.shortcut}
                {showBulkModifications && <Button onClick={this.handleBulkModifications.bind(this, col.refType, col.dataType)} title={i18n('arr.fund.bulkModifications.action')}><Icon glyph='fa-edit'/></Button>}
                <Button onClick={this.handleFilterSettings.bind(this, col.refType, col.dataType)} title={i18n('arr.fund.filterSettings.action')}><Icon glyph='fa-filter'/></Button>
            </div>
        )
    }

    getColsStateFromProps(nextProps, props) {
        const {fundDataGrid, descItemTypes, rulDataTypes} = nextProps;

        if (descItemTypes.fetched) {
            if (props.fundDataGrid.columnsOrder !== fundDataGrid.columnsOrder
                || props.descItemTypes !== descItemTypes
                || props.fundDataGrid.columnInfos !== fundDataGrid.columnInfos
                || props.fundDataGrid.filter !== fundDataGrid.filter
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

        var eqProps = ['versionId', 'descItemTypes']
        if (!propsEquals(this.props, nextProps, eqProps)) {
            return true
        }

        var eqProps2 = [
            'isFetchingFilter', 'fetchedFilter', 'isFetchingData', 'fetchedData', 'pageSize', 'pageIndex',
            'items', 'itemsCount', 'filter', 'visibleColumns', 'columnsOrder', 'columnInfos', 'selectedIds'
        ]
        if (!propsEquals(this.props.fundDataGrid, nextProps.fundDataGrid, eqProps2)) {
            return true
        }

        return false
    }

    getColumnsOrder(fundDataGrid, refTypesMap, dataTypesMap) {
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
        const columnsOrder = this.getColumnsOrder(fundDataGrid, refTypesMap, dataTypesMap)

        var cols = []
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
                    width: colInfo ? colInfo.width : 60,
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
        this.dispatch(fundDataGridFilterUpdateData(versionId))
    }

    handleColumnSettings() {
        const {fundDataGrid, descItemTypes} = this.props
        const refTypesMap = getMapFromList(descItemTypes.items)
        const columnsOrder = this.getColumnsOrder(fundDataGrid, refTypesMap)
        const visibleColumns = fundDataGrid.visibleColumns
        const columns = columnsOrder.map(id => {
            const refType = refTypesMap[id]
            return {
                id: refType.id,
                title: refType.shortcut,
                desc: refType.description,
            }
        })
        columns.sort((a, b) => a.title.toLowerCase().localeCompare(b.title.toLowerCase()))

        this.dispatch(modalDialogShow(this, i18n('arr.fund.columnSettings.title'),
            <DataGridColumnsSettings
                onSubmitForm={this.handleChangeColumnsSettings}
                columns={columns}
                visibleColumns={visibleColumns}
            />
        ));
    }

    handleFilterSettings(refType, dataType) {
        const {versionId, fundDataGrid} = this.props

        this.dispatch(modalDialogShow(this, i18n('arr.fund.filterSettings.title', refType.shortcut),
            <FundFilterSettings
                versionId={versionId}
                refType={refType}
                dataType={dataType}
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
                    nodes = fundDataGrid.items.map(i => ({id: i.node.id, version: i.node.version}) )
                    break
                case 'selected':
                    var set = getSetFromIdsList(fundDataGrid.selectedIds)
                    nodes = []
                    fundDataGrid.items.forEach(i => {
                        if (set[i.id]) {
                            nodes.push({id: i.node.id, version: i.node.version})
                        }
                    })
                    break
                case 'unselected':
                    nodes = []
                    var set = getSetFromIdsList(fundDataGrid.selectedIds)
                    fundDataGrid.items.forEach(i => {
                        if (!set[i.id]) {
                            nodes.push({id: i.node.id, version: i.node.version})
                        }
                    })
                    break
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
        const {versionId} = this.props

        var visibleColumns = {}
        var columnsOrder = []
        columns.forEach(col => {
            visibleColumns[col.id] = true
            columnsOrder.push(col.id)
        })
        this.dispatch(fundDataGridSetColumnsSettings(versionId, visibleColumns, columnsOrder))

        this.dispatch(modalDialogHide())
    }

    handleContextMenu(row, rowIndex, col, colIndex, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, row)}>{i18n('arr.fund.bulkModifications.action.openInNewTab')}</MenuItem>
                <MenuItem onClick={this.handleSelectInTab.bind(this, row)}>{i18n('arr.fund.bulkModifications.action.open')}</MenuItem>
                <MenuItem onClick={() => {this.dispatch(contextMenuHide());this.handleEdit(row, col)}}>{i18n('global.action.update')}</MenuItem>
            </ul>
        )

        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    handleEdit(row, rowIndex, col, colIndex) {
        const {versionId, fundId, closed} = this.props
        const parentNodeId = row.parentNode ? row.parentNode.id : null

        if (typeof col.id === 'undefined') {
            return
        }

        this.dispatch(fundDataGridPrepareEdit(versionId, row.id, parentNodeId, col.id))

        const dataGridComp = this.refs.dataGrid.getWrappedInstance()
        const cellEl = dataGridComp.getCellElement(rowIndex, colIndex)
        const cellRect = cellEl.getBoundingClientRect()

        this.dispatch(modalDialogShow(this, null,
            <FundDataGridCellForm
                versionId={versionId}
                fundId={fundId}
                nodeKey='DATA_GRID'
                closed={closed}
                position={{x: cellRect.left, y: cellRect.top}}
            />,
        'fund-data-grid-cell-edit', this.setFocusAfterCellEdit));
    }

    setFocusAfterCellEdit() {
        this.setState({},
            ()=>{ ReactDOM.findDOMNode(this.refs.dataGrid).focus() }
        )
    }


    /**
     * Otevření uzlu v nové záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInNewTab(row) {
        const {versionId} = this.props
        this.dispatch(contextMenuHide());
        this.dispatch(fundSelectSubNode(versionId, row.node.id, row.parentNode, true, null, true));
    }

    /**
     * Otevření uzlu v záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInTab(row) {
        const {versionId} = this.props
        this.dispatch(contextMenuHide());
        this.dispatch(fundSelectSubNode(versionId, row.node.id, row.parentNode, false, null, true));
    }

    render() {
        const {fundId, fundDataGrid, versionId, rulDataTypes, descItemTypes} = this.props;
        const {cols} = this.state;

        if (!fundDataGrid.fetchedFilter || !fundDataGrid.fetchedData || !descItemTypes.fetched || !rulDataTypes.fetched) {
            return <Loading/>
        }

        return (
            <div ref='gridContainer' className='fund-datagrid-container-wrap'>
                <div ref='grid' className='fund-datagrid-container'>
                    <div className='actions-container'>
                        <Button onClick={this.handleColumnSettings} title={i18n('arr.fund.columnSettings.action')}><Icon glyph='fa-columns'/></Button>
                        <Button onClick={this.handleFilterUpdateData}>{i18n('arr.fund.filterSettings.updateData.action')}</Button>
                        <Button onClick={this.handleFilterClearAll}>{i18n('arr.fund.filterSettings.clearAll.action')}</Button>
                    </div>
                    <div className='grid-container'>
                        <DataGrid
                            ref='dataGrid'
                            rows={fundDataGrid.items}
                            cols={cols}
                            selectedIds={fundDataGrid.selectedIds}
                            onColumnResize={this.handleColumnResize}
                            onSelectedIdsChange={this.handleSelectedIdsChange}
                            onContextMenu={this.handleContextMenu}
                            onEdit={this.handleEdit}
                        />
                        <DataGridPagination
                            itemsCount={fundDataGrid.itemsCount}
                            pageSize={fundDataGrid.pageSize}
                            pageIndex={fundDataGrid.pageIndex}
                            onSetPageIndex={pageIndex => {this.props.dispatch(fundDataGridSetPageIndex(versionId, pageIndex))}}
                            onChangePageSize={pageSize => {this.props.dispatch(fundDataGridSetPageSize(versionId, pageSize))}}
                        />
                    </div>
                </div>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {splitter} = state
    return {
        splitter,
    }
}

module.exports = connect(mapStateToProps)(FundDataGrid);
