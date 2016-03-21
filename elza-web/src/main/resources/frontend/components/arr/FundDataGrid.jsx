/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FundFindAndReplaceForm, Icon, ListBox, DataGridColumnsSettings, AbstractReactComponent, i18n, Loading, DataGrid, DataGridPagination} from 'components';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import * as types from 'actions/constants/ActionTypes';
import {fundDataGridSetColumnsSettings, fundDataGridSetSelection, fundDataGridSetColumnSize, fundDataGridFetchFilterIfNeeded,
        fundDataGridFetchDataIfNeeded, fundDataGridSetPageIndex, fundDataGridSetPageSize,
        findAndReplace} from 'actions/arr/fundDataGrid'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {getSetFromIdsList, getMapFromList} from 'stores/app/utils'
import {propsEquals} from 'components/Utils'
import {Button} from 'react-bootstrap'

var FundDataGrid = class FundDataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelectedIdsChange', 'handleColumnResize', 'handleColumnSettings', 'handleChangeColumnsSettings',
            'handleFindAndReplace', 'headerColRenderer');

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
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))
    }

    componentWillReceiveProps(nextProps) {
        const {fundDataGrid, versionId, descItemTypes} = nextProps;
        //this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.dispatch(descItemTypesFetchIfNeeded())
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))

        const colState = this.getColsStateFromProps(nextProps, this.props)
        colState && this.setState(colState)
    }

    headerColRenderer(col) {
        return (
            <div className='' title={col.refType.name}>
                {col.refType.shortcut}
                <Button onClick={this.handleFindAndReplace.bind(this, col.refType)}><Icon glyph='fa-edit'/></Button>
            </div>
        )
    }

    getColsStateFromProps(nextProps, props) {
        const {fundDataGrid, descItemTypes} = nextProps;

        if (descItemTypes.fetched) {
            if (props.fundDataGrid.columnsOrder !== fundDataGrid.columnsOrder
                || props.descItemTypes !== descItemTypes
                || props.fundDataGrid.columnInfos !== fundDataGrid.columnInfos
            ) {
                const cols = this.buildColumns(fundDataGrid, descItemTypes)
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

    buildColumns(fundDataGrid, descItemTypes) {
        const refTypesMap = getMapFromList(descItemTypes.items)
        const columnsOrder = this.getColumnsOrder(fundDataGrid, refTypesMap)

        var cols = []
        columnsOrder.forEach(id => {
            const refType = refTypesMap[id]
            if (fundDataGrid.visibleColumns[id]) {  // je vidět
                const colInfo = fundDataGrid.columnInfos[id]

                const col = {
                    id: refType.id,
                    refType: refType,
                    title: refType.shortcut,
                    desc: refType.name,
                    width: colInfo ? colInfo.width : 60,
                    dataName: refType.id,
                    headerColRenderer: this.headerColRenderer,
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
        this.dispatch(modalDialogShow(this, i18n('dataGrid.columnSettings.title'),
            <DataGridColumnsSettings
                onSubmitForm={this.handleChangeColumnsSettings}
                columns={columns}
                visibleColumns={visibleColumns}
            />
        ));
    }

    handleFindAndReplace(refType) {
        const {versionId, fundDataGrid} = this.props

        var submit = (data) => {
            // Sestavení seznamu id
            var ids;
            switch (data.itemsArea) {
                case 'all':
                    ids = fundDataGrid.items.map(i => i.id)
                    break
                case 'selected':
                    var set = getSetFromIdsList(fundDataGrid.selectedIds)
                    ids = []
                    fundDataGrid.items.forEach(i => {
                        if (set[i.id]) {
                            ids.push(i.id)
                        }
                    })
                    break
                case 'unselected':
                    ids = []
                    var set = getSetFromIdsList(fundDataGrid.selectedIds)
                    fundDataGrid.items.forEach(i => {
                        if (!set[i.id]) {
                            ids.push(i.id)
                        }
                    })
                    break
            }

            this.dispatch(findAndReplace(versionId, refType.id, data.findText, data.replaceText, ids))
        }

        this.dispatch(modalDialogShow(this, i18n('arr.fund.findAndReplace.title'),
            <FundFindAndReplaceForm
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
        const {fundDataGrid, versionId, descItemTypes} = this.props;
        const {cols} = this.state;

        if (!fundDataGrid.fetchedFilter || !fundDataGrid.fetchedData || !descItemTypes.fetched) {
            return <Loading/>
        }

        return (
            <div>
<Button onClick={this.handleColumnSettings}><Icon glyph='fa-columns'/></Button>
<Button onClick={this.handleFindAndReplace}><Icon glyph='fa-edit'/></Button>
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
        )
    }
}

module.exports = connect()(FundDataGrid);


