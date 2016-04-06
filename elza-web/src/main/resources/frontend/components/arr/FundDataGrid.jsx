/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FundBulkModificationsForm, Icon, ListBox, DataGridColumnsSettings, AbstractReactComponent, i18n, Loading,
    DataGrid, FundFilterSettings, DataGridPagination} from 'components';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import * as types from 'actions/constants/ActionTypes';
import {fundDataGridSetColumnsSettings, fundDataGridSetSelection, fundDataGridSetColumnSize, fundDataGridFetchFilterIfNeeded,
    fundDataGridFetchDataIfNeeded, fundDataGridSetPageIndex, fundDataGridSetPageSize,
    fundDataGridFilterChange, fundBulkModifications, fundDataGridFilterClearAll, fundDataGridFilterUpdateData} from 'actions/arr/fundDataGrid'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {getSetFromIdsList, getMapFromList} from 'stores/app/utils'
import {propsEquals} from 'components/Utils'
import {Button} from 'react-bootstrap'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {getSpecsIds} from 'components/arr/ArrUtils'

require('./FundDataGrid.less')

var FundDataGrid = class FundDataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelectedIdsChange', 'handleColumnResize', 'handleColumnSettings', 'handleChangeColumnsSettings',
            'handleBulkModifications', 'handleFilterSettings', 'headerColRenderer', 'cellRenderer', 'resizeGrid', 'handleFilterClearAll',
            'handleFilterUpdateData');

        const colState = this.getColsStateFromProps(props, {fundDataGrid: {}})
        if (colState) {
            this.state = colState
        } else {
            this.state = {cols: []}
        }
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
        const value = row[col.dataName]

        var displayValue
        if (value) {
            displayValue = value.value
        }

        return (
            <div className=''>{displayValue}</div>
        )
    }

    cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        const value = row[col.dataName]

        var displayValue
        if (value) {
            displayValue = value.value
        }

        return (
            <div className=''>{displayValue}</div>
        )
    }

    supportBulkModifications(refType, dataType) {
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
        return result
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
                {col.refType.shortcut}{col.dataType.code}
                {showBulkModifications && <Button onClick={this.handleBulkModifications.bind(this, col.refType)} title={i18n('arr.fund.bulkModifications.action')}><Icon glyph='fa-edit'/></Button>}
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

    handleBulkModifications(refType) {
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

            this.dispatch(fundBulkModifications(versionId, refType.id, specsIds, data.operationType, data.findText, data.replaceText, nodes))
        }

        this.dispatch(modalDialogShow(this, i18n('arr.fund.bulkModifications.title'),
            <FundBulkModificationsForm
                refType={refType}
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

    render() {
        const {fundDataGrid, versionId, rulDataTypes, descItemTypes} = this.props;
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
                            rows={fundDataGrid.items}
                            cols={cols}
                            selectedIds={fundDataGrid.selectedIds}
                            onColumnResize={this.handleColumnResize}
                            onSelectedIdsChange={this.handleSelectedIdsChange}
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


