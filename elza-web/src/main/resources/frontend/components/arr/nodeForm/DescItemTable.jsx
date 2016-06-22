/**
 * Input prvek pro desc item - typ TABLE.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, NoFocusButton, DataGrid, Icon, i18n} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import DescItemTableCellForm from './DescItemTableCellForm.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'

require ("./DescItemTable.less")

var DescItemTable = class DescItemTable extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('focus', "handleEdit", "handleEditClose",
            "handleCellChange", "cellRenderer",
            "cellRowDeleteRenderer", "handleAddRow", "handleDownload",
            "handleUpload", "handleRemoveRow", "getStateFromProps",
            "handleBlur");

        this.blurEnabled = true;
        this.state = this.getStateFromProps(props)
    }

    componentWillReceiveProps(nextProps) {
        this.setState(this.getStateFromProps(nextProps));
    }

    getStateFromProps(props) {
        const refType = props.refType;

        // Sloupce
        var cols = []
        refType.columnsDefinition.forEach(colDef => {
            cols.push({
                title: colDef.name,
                width: colDef.width,
                widthPercent: true,cellRenderer: this.cellRenderer,
                colDef: colDef,
            })
        })
        // Sloupec s akcí pro mazání řádků
        cols.push({
            title: "",
            width: 24,
            widthPercent: false,
            cellRenderer: this.cellRowDeleteRenderer,
        })

        // Data tabulky
        var rows = [
            {values: {KEY: "klic1", VALUE: "value1"}},
            {values: {KEY: "klic2", VALUE: "value2"}},
            {values: {KEY: "klic3", VALUE: "value3"}},
        ]
        rows = (props.descItem.value && props.descItem.value.rows) ? props.descItem.value.rows : [];
        // rows = [
        //     {values: {KEY: "klic1", VALUE: "value1"}},
        //     {values: {KEY: "klic2", VALUE: "value2"}},
        //     {values: {KEY: "klic3", VALUE: "value3"}},
        // ]

        return {
            rows: rows,
            cols: cols,
        }
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleBlur() {
        if (this.blurEnabled) {
            const {onBlur} = this.props
            onBlur();
        }
    }

    handleCellChange(row, rowIndex, col, colIndex, value) {
        const {rows} = this.state

        var newRows = [...rows];
        newRows[rowIndex] = {...rows[rowIndex]};
        newRows[rowIndex].values = {...newRows[rowIndex].values};
        newRows[rowIndex].values[col.colDef.code] = value;

        this.props.onChange({ rows: newRows });
    }

    handleEdit(row, rowIndex, col, colIndex) {
        const dataGridComp = this.refs.dataGrid.getWrappedInstance();
        const cellEl = dataGridComp.getCellElement(rowIndex, colIndex);
        const cellRect = cellEl.getBoundingClientRect();

        this.blurEnabled = false;
        this.dispatch(modalDialogShow(this, null,
            <DescItemTableCellForm
                position={{x: cellRect.left, y: cellRect.top}}
                value={row.values[col.colDef.code]}
                onChange={this.handleCellChange.bind(this, row, rowIndex, col, colIndex)}
            />,
            'desc-item-table-cell-edit', this.handleEditClose));
    }

    handleEditClose() {
        this.setState({},
            ()=>{
                ReactDOM.findDOMNode(this.refs.dataGrid).focus();
                this.blurEnabled = true;
            }
        )
    }

    cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        return (
            <div className='value'>{row.values[col.colDef.code]}</div>
        )
    }

    handleAddRow() {
        const {rows} = this.state
        var newRows = [...rows, { values: {} }];
        this.props.onChange({ rows: newRows });
    }

    handleDownload() {

    }

    handleUpload() {

    }

    handleRemoveRow(row, rowIndex) {
        const {rows} = this.state
        var newRows = [
            ...rows.slice(0, rowIndex),
            ...rows.slice(rowIndex + 1),
        ];

        this.props.onChange({ rows: newRows });
    }

    cellRowDeleteRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        return (
            <div className='value'>
                <NoFocusButton className="remove" onClick={this.handleRemoveRow.bind(this, row, rowIndex)} title={i18n("subNodeForm.descItem.jsonTable.action.removeRow")}>
                    <Icon glyph="fa-remove"/>
                </NoFocusButton>
            </div>
        )
    }

    render() {
        const {descItem, locked, onFocus} = this.props;
        const {rows, cols} = this.state;

        return (
            <div className='desc-item-value desc-item-value-table'>
                <DataGrid
                    ref='dataGrid'
                    rows={rows}
                    cols={cols}
                    onFocus={onFocus}
                    onBlur={this.handleBlur}
                    selectedIds={[]}
                    allowRowCheck={false}
                    staticColumns={true}
                    onEdit={this.handleEdit}
                    />
                <div className='desc-item-value-actions'>
                    <NoFocusButton onClick={this.handleAddRow} title={i18n('subNodeForm.descItem.jsonTable.action.addRow')}><Icon glyph="fa-plus" /></NoFocusButton>
                    <NoFocusButton onClick={this.handleDownload} title={i18n('subNodeForm.descItem.jsonTable.action.download')}><Icon glyph="fa-download" /></NoFocusButton>
                    <NoFocusButton onClick={this.handleUpload} title={i18n('subNodeForm.descItem.jsonTable.action.upload')}><Icon glyph="fa-upload" /></NoFocusButton>
                </div>
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemTable);
