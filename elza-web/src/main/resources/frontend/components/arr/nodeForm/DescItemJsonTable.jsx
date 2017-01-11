/**
 * Input prvek pro desc item - typ TABLE.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, NoFocusButton, DataGrid, Icon, i18n} from 'components/index.jsx';
import {connect} from 'react-redux'
import {Input} from 'react-bootstrap'
import {decorateValue} from './DescItemUtils.jsx'
import DescItemJsonTableCellForm from './DescItemJsonTableCellForm.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

require ("./DescItemJsonTable.less")

var DescItemJsonTable = class DescItemJsonTable extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('focus', "handleEdit", "handleEditClose",
            "handleCellChange", "cellRenderer",
            "cellRowDeleteRenderer", "handleAddRow",
            "handleRemoveRow", "getStateFromProps",
            "handleBlur", "handleDelete");

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
                desc: colDef.name,
                width: colDef.width,
                widthPercent: true,
                cellRenderer: this.cellRenderer,
                colDef: colDef,
            })
        })
        // Sloupec s akcí pro mazání řádků
        cols.push({
            actions: true,
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
        this.refs.dataGrid.getWrappedInstance().focus()
    }

    handleBlur() {
        // console.log("BLUR", this.blurEnabled)
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

    handleDelete(row, rowIndex, col, colIndex) {
        if (col.actions) {  // u tohoto sloupce není možné nic editovat
            return;
        }

        const {rows} = this.state

        var newRows = [...rows];
        newRows[rowIndex] = {...rows[rowIndex]};
        newRows[rowIndex].values = {...newRows[rowIndex].values};
        newRows[rowIndex].values[col.colDef.code] = "";

        this.props.onChange({ rows: newRows });
    }

    handleEdit(row, rowIndex, col, colIndex) {
        if (col.actions) {  // u tohoto sloupce není možné nic editovat
            return;
        }

        const dataGridComp = this.refs.dataGrid.getWrappedInstance();
        const cellEl = dataGridComp.getCellElement(rowIndex, colIndex);
        const cellRect = cellEl.getBoundingClientRect();

        const value = row.values[col.colDef.code]
        this.blurEnabled = false;
        this.dispatch(modalDialogShow(this, null,
            <DescItemJsonTableCellForm
                position={{x: cellRect.left, y: cellRect.top}}
                value={value}
                dataType={col.colDef.dataType}
                onChange={this.handleCellChange.bind(this, row, rowIndex, col, colIndex)}
            />,
            'desc-item-table-cell-edit', this.handleEditClose.bind(this, row, rowIndex, col, colIndex, value)));
    }

    handleEditClose(row, rowIndex, col, colIndex, prevValue, closeType) {
        if (closeType === "DIALOG") {   // zavření dialogu bez potvrzení, vrátíme původní hodnotu
            this.handleCellChange(row, rowIndex, col, colIndex, prevValue);
        }
        this.setState({},
            ()=>{
                this.focus();
                this.blurEnabled = true;
            }
        )
    }

    cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        return (
            <div key={colIndex} className='value'>{row.values[col.colDef.code]}</div>
        )
    }

    handleAddRow() {
        const {rows} = this.state
        var newRows = [...rows, { values: {} }];
        this.props.onChange({ rows: newRows });
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
        const {locked} = this.props;

        var actions = [];

        if (!locked) {
            actions.push(<NoFocusButton key="remove" className="remove" onClick={this.handleRemoveRow.bind(this, row, rowIndex)} title={i18n("subNodeForm.descItem.jsonTable.action.removeRow")}>
                <Icon glyph="fa-remove"/>
            </NoFocusButton>)
        }

        return (
            <div key={colIndex} className='value'>
                {actions}
            </div>
        )
    }

    render() {
        const {descItem, locked, onFocus, onDownload, readMode} = this.props;
        const {rows, cols} = this.state;

        var actions = [];

        if (descItem.descItemObjectId != null) {
            actions.push(<NoFocusButton key="download" onClick={onDownload} title={i18n('subNodeForm.descItem.jsonTable.action.download')}><Icon glyph="fa-download" /></NoFocusButton>)
        }

        if (!locked) {
            actions.push(<NoFocusButton key="add" onClick={this.handleAddRow} title={i18n('subNodeForm.descItem.jsonTable.action.addRow')}><Icon glyph="fa-plus" /></NoFocusButton>)
        }

        return (
            <div className='desc-item-value desc-item-value-table'>
                <ItemTooltipWrapper tooltipTitle="dataType.jsonTable.format">
                    <DataGrid
                        key="grid"
                        ref='dataGrid'
                        rows={rows}
                        cols={cols}
                        onFocus={onFocus}
                        onBlur={this.handleBlur}
                        selectedIds={[]}
                        allowRowCheck={false}
                        staticColumns={true}
                        onEdit={this.handleEdit}
                        onDelete={this.handleDelete}
                        disabled={locked || readMode}
                        />
                </ItemTooltipWrapper>
                <div
                    key="actions"
                    className='desc-item-value-actions'
                >
                    {actions}
                </div>
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemJsonTable);
