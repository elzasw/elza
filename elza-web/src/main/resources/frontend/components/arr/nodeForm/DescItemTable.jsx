/**
 * Input prvek pro desc item - typ TABLE.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, NoFocusButton, DataGrid} from 'components/index.jsx';
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
            "cellRowDeleteRenderer");

        const resizeable = true;
        var cols = [
            {
                title: "Sloupec 1",
                desc: "popis sloupce 1",
                width: 50,
                resizeable: resizeable,
                cellRenderer: this.cellRenderer,
            },
            {
                title: "Sloupec 2",
                desc: "popis sloupce 2",
                width: 30,
                resizeable: resizeable,
                cellRenderer: this.cellRenderer,
            },
            {
                title: "Sloupec 3",
                desc: "popis sloupce 3",
                width: 15,
                resizeable: resizeable,
                cellRenderer: this.cellRenderer,
            },
            {
                title: "Sloupec 4",
                desc: "popis sloupce 4",
                width: 5,
                resizeable: resizeable,
                cellRenderer: this.cellRenderer,
            },
            {
                title: "",
                desc: "",
                width: 32,
                resizeable: false,
                cellRenderer: this.cellRowDeleteRenderer,
            },
        ]

        var rows = [
            ["r1c1", "r1c2", "r1c3", "r1c4",],
            ["r2c1", "r2c2", "r2c3", "r2c4",],
            ["r3c1", "r3c2", "r3c3", "r3c4",],
            ["r4c1", "r4c2", "r4c3", "r4c4",],
            ["r5c1", "r5c2", "r5c3", "r5c4",],
            ["r6c1", "r6c2", "r6c3", "r6c4",],
        ]

        this.state = {
            rows: rows,
            cols: cols,
        }
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleCellChange(row, rowIndex, col, colIndex, value) {
        const {rows} = this.state

        var newRows = [...rows];
        newRows[rowIndex] = [...rows[rowIndex]];
        newRows[rowIndex][colIndex] = value;

        this.setState({
            rows: newRows,
        })

        this.props.onChange(rows);
    }

    handleEdit(row, rowIndex, col, colIndex) {
        const dataGridComp = this.refs.dataGrid.getWrappedInstance();
        const cellEl = dataGridComp.getCellElement(rowIndex, colIndex);
        const cellRect = cellEl.getBoundingClientRect();

        this.dispatch(modalDialogShow(this, null,
            <DescItemTableCellForm
                position={{x: cellRect.left, y: cellRect.top}}
                value={row[colIndex]}
                onChange={this.handleCellChange.bind(this, row, rowIndex, col, colIndex)}
            />,
            'desc-item-table-cell-edit', this.handleEditClose));
    }

    handleEditClose() {
        this.setState({},
            ()=>{ ReactDOM.findDOMNode(this.refs.dataGrid).focus() }
        )
    }

    cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        return (
            <div className='value'>{row[colIndex]}</div>
        )
    }

    cellRowDeleteRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        return (
            <div className='value'>{row[colIndex]}</div>
        )
    }

    render() {
        const {descItem, locked, onFocus, onBlur} = this.props;
        const {rows, cols} = this.state;

        return (
            <div className='desc-item-value'>
                <DataGrid
                    ref='dataGrid'
                    rows={rows}
                    cols={cols}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    selectedIds={[]}
                    allowRowCheck={false}
                    staticColumns={false}
                    onEdit={this.handleEdit}
                    />
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemTable);
