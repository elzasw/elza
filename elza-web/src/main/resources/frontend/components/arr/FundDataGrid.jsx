/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading, DataGrid, DataGridPagination} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {fundDataGridSetSelection, fundDataGridSetColumnSize, fundDataGridFetchFilterIfNeeded, fundDataGridFetchDataIfNeeded, fundDataGridSetPageIndex, fundDataGridSetPageSize} from 'actions/arr/fundDataGrid'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {getMapFromList} from 'stores/app/utils'

var cols = []

cols.push({
    id: 1,
    dataName: 'id',
    title: 'Id',
    desc: 'popis id',
    width: 60,
})
cols.push({
    id: 2,
    dataName: 'firstname',
    title: 'Jmeno',
    desc: 'popis jmena',
    width: 120,
})
cols.push({
    id: 3,
    dataName: 'surname',
    title: 'Prijmeni',
    desc: 'popis prijmeni',
    width: 120,
})
cols.push({
    id: 4,
    dataName: 'age',
    title: 'Vek',
    desc: 'popis vek',
    width: 160,
})
cols.push({
    id: 5,
    dataName: 'address',
    title: 'Adresa',
    desc: 'popis adresy',
    width: 220,
})
cols.push({
    id: 6,
    dataName: 'tel',
    title: 'Telefon',
    desc: 'popis telefonu',
    width: 120,
})

var FundDataGrid = class FundDataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods('');
    }

    componentDidMount() {
        const {fundDataGrid, versionId} = this.props;
        //this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.dispatch(descItemTypesFetchIfNeeded())
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))
    }

    componentWillReceiveProps(nextProps) {
        const {fundDataGrid, versionId} = nextProps;
        //this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.dispatch(descItemTypesFetchIfNeeded())
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))
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
                    title: refType.shortcut,
                    desc: refType.name,
                    width: colInfo ? colInfo.width : 60,
                    dataName: refType.id,
                }
                cols.push(col)
            }
        })

        return cols
    }

    render() {
        const {fundDataGrid, versionId, descItemTypes} = this.props;

        if (!fundDataGrid.fetchedFilter || !fundDataGrid.fetchedData || !descItemTypes.fetched) {
            return <Loading/>
        }

        var cols2 = this.buildColumns(fundDataGrid, descItemTypes)
console.log('fundDataGrid', fundDataGrid)
        return (
            <div>
                {true && <DataGrid
                    rows={fundDataGrid.items}
                    cols={cols2}
                    selectedIds={fundDataGrid.selectedIds}
                    onColumnResize={(colIndex, width) => this.dispatch(fundDataGridSetColumnSize(versionId, cols2[colIndex].id, width))}
                    onSelectedIdsChange={ids => this.dispatch(fundDataGridSetSelection(versionId, ids))}
                />}
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


