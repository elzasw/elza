/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading, DataGrid, DataGridPagination} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {fundDataGridFetchFilterIfNeeded, fundDataGridFetchDataIfNeeded, fundDataGridSetPageIndex, fundDataGridSetPageSize} from 'actions/arr/fundDataGrid'

var cols = []

cols.push({
    dataName: 'id',
    title: 'Id',
    desc: 'popis id',
    width: 60,
})
cols.push({
    dataName: 'firstname',
    title: 'Jmeno',
    desc: 'popis jmena',
    width: 120,
})
cols.push({
    dataName: 'surname',
    title: 'Prijmeni',
    desc: 'popis prijmeni',
    width: 120,
})
cols.push({
    dataName: 'age',
    title: 'Vek',
    desc: 'popis vek',
    width: 160,
})
cols.push({
    dataName: 'address',
    title: 'Adresa',
    desc: 'popis adresy',
    width: 220,
})
cols.push({
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
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))
    }

    componentWillReceiveProps(nextProps) {
        const {fundDataGrid, versionId} = nextProps;
        //this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId))
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize))
    }

    render() {
        const {fundDataGrid, versionId} = this.props;

        if (!fundDataGrid.fetchedFilter || !fundDataGrid.fetchedData) {
            return <Loading/>
        }

        return (
            <div>
                {true && <DataGrid
                    rows={fundDataGrid.items}
                    cols={cols}
                    selectedIds={[]}
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


