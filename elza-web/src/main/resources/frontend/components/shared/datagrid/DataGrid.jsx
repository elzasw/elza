/**
 * Data grid komponent - typu tabulka excel.
 */

require ('./DataGrid.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Resizer} from 'components/index.jsx';
const scrollIntoView = require('dom-scroll-into-view')
import {propsEquals, getScrollbarWidth} from 'components/Utils.jsx'
import {Shortcuts} from 'react-shortcuts';

const __emptyColWidth = 8000
const __minColWidth = 16

var DataGrid = class DataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'handleScroll',
            'renderHeaderCol',
            'renderCell',
            'ensureFocusVisible',
            'handleResizerMouseDown',
            'handleMouseUp',
            'handleMouseMove',
            'unFocus',
            'handleCellClick',
            'handleCheckboxChange',
            'getExtColumnIndex',
            'handleEdit',
            'handleDelete',
            'handleContextMenu',
            'computeColumnsWidth',
            "focus",
        );

        var state = this.getStateFromProps(props, {})
        state.columnSizeDragged = false
        state.selectedRowIndexes = {[state.focus.row]: true}   // označené řádky v aktuálním zobrazení - pouze klientské označení

        this.state = state
    }

    componentWillReceiveProps(nextProps) {
        this.setState(this.getStateFromProps(nextProps, this.state),
        () => {
            this.ensureFocusVisible(this.state.focus);
            this.computeColumnsWidth();
        })
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;  // zde NECHCEME!!! - stav se meni vzdy, protoze se vola getStateFromProps, po jeho pripadne uprave je mozne toto odkomentovat!
        }

        var eqProps = ['rows', 'cols', 'selectedIds', 'onColumnResize', 'onSelectedIdsChange']
        //var eqProps = ['rows', 'cols', 'selectedIds']
        return !propsEquals(this.props, nextProps, eqProps)
    }

    focus() {
        this.refs.dataGrid.focus()
    }

    getStateFromProps(props, currState) {
        var cols = []
        var colWidths = {}
        if (props.allowRowCheck) {
            cols.push({_rowCheck: true, width: 32, resizeable: false})
            colWidths[0] = 32;
        }
        var needComputeColumnsWidth = props.staticColumns;
        props.cols.forEach((col, colIndex) => {
            var useColIndex = props.allowRowCheck ? colIndex + 1 : colIndex;

            if (props.staticColumns) {
                if (currState.colWidths) {
                    colWidths[useColIndex] = currState.colWidths[useColIndex];
                } else {
                    colWidths[useColIndex] = 10;
                }
            } else {
                colWidths[useColIndex] = col.width
            }
            cols.push(col)
        })

        var selectedIds = {}
        if (typeof props.selectedIds !== 'undefined') {
            props.selectedIds.forEach(id => {
                selectedIds[id] = true
            });
        } else {
            selectedIds = currState.selectedIds;
        }

        var selectedRowIndexes = {};
        if (typeof props.selectedRowIndexes !== 'undefined') {
            props.selectedRowIndexes.forEach(id => {
                selectedRowIndexes[id] = true
            });
        } else {
            selectedRowIndexes = currState.selectedRowIndexes;
        }

        var focusRow
        if (typeof props.focusRow !== 'undefined') {
            focusRow = props.focusRow
        } else if (currState.focus) {
            focusRow = currState.focus.row
        } else {
            focusRow = 0
        }

        var focusCol
        if (typeof props.focusCol !== 'undefined') {
            focusCol = props.focusCol
        } else if (currState.focus) {
            focusCol = currState.focus.col
        } else {
            focusCol = 0
        }
        var focus = {row: focusRow, col: focusCol}

        return {
            focus,
            cols,
            colWidths,
            selectedRowIndexes,
            selectedIds,    // označené řádky podle id - napříč stránkami - jedná se o reálné označení řádku, např. pro akce atp.
            needComputeColumnsWidth,
        }
    }

    componentDidMount() {
        document.addEventListener('mouseup', this.handleMouseUp);
        document.addEventListener('mousemove', this.handleMouseMove);
        this.ensureFocusVisible(this.state.focus);
        this.computeColumnsWidth();
    }

    componentWillUnmount() {
        document.removeEventListener('mouseup', this.handleMouseUp);
        document.removeEventListener('mousemove', this.handleMouseMove);
    }

    computeColumnsWidth() {
        const {cols, staticColumns} = this.props
        const {needComputeColumnsWidth} = this.state

        if (staticColumns && needComputeColumnsWidth) {
            const dataGrid = ReactDOM.findDOMNode(this.refs.dataGrid);
            const rect = dataGrid.getBoundingClientRect();
            var width = rect.width - getScrollbarWidth() - 4;   // konstanta 3 - kvůli padding atp.
            var colWidths = {}

            // Nejdříve sloupce s pevnou šířkou
            cols.forEach((col, colIndex) => {
                if (!col.widthPercent) {
                    colWidths[colIndex] = col.width;
                    width -= col.width;
                }
            })

            // Sloupce s šířkou definovanou v procentech
            cols.forEach((col, colIndex) => {
                if (col.widthPercent) {
                    colWidths[colIndex] = Math.floor(col.width * width / 100);
                }
            })
            // console.log("width", rect.width, "scrollbar", getScrollbarWidth(), "colWidths", colWidths, "cols", cols)

            this.setState({
                colWidths
            })
        }
    }

    handleCellClick(rowIndex, colIndex, e) {
        const {allowRowCheck} = this.props

        if (allowRowCheck && (colIndex === 0 && e.target.tagName === 'INPUT')) {   // první sloupec - označování řádků, zde neřešíme, pokud klikne na checkbox
            return
        }

        this.unFocus()

        const {focus, selectedRowIndexes} = this.state
        const {rows} = this.props
        var newFocus = {row: rowIndex, col: colIndex}
        var newSelectedRowIndexes = selectedRowIndexes

        if (e.ctrlKey) {
            if (selectedRowIndexes[rowIndex]) { // je označená, odznačíme, ji, ale jen pokud není jediná
                if (Object.keys(selectedRowIndexes).length > 1) {
                    newSelectedRowIndexes = {...selectedRowIndexes}
                    delete newSelectedRowIndexes[rowIndex]
                }
            } else {    // není označená, přidáme ji o označení
                newSelectedRowIndexes = {...selectedRowIndexes, [rowIndex]: true}
            }
        } else if (e.shiftKey) {
            var x1 = focus.row
            var x2 = rowIndex
            if (x1 > x2) {
                var x3 = x1
                x1 = x2
                x2 = x3
            }
            newSelectedRowIndexes = {}
            for (var a=x1; a<=x2; a++) {
                newSelectedRowIndexes[a] = true
            }
        } else {
            newSelectedRowIndexes = {[rowIndex]: true}
        }

        this.setState({ focus: newFocus, selectedRowIndexes: newSelectedRowIndexes }, this.ensureFocusVisible(newFocus))

        const {onChangeFocus, onChangeRowIndexes} = this.props;
        onChangeFocus && onChangeFocus(newFocus.row, newFocus.col);
        onChangeRowIndexes && onChangeRowIndexes(Object.keys(newSelectedRowIndexes));
    }

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges()
        }
    }

    handleScroll(e) {
        ReactDOM.findDOMNode(this.refs.header).scrollLeft = e.target.scrollLeft
    }

    handleResizerMouseDown(colIndex, e) {
        this.unFocus();

        this.setState({
            position: e.clientX,
            columnSizeDragged: true,
            columnSizeDraggedIndex: colIndex,
        });
    }

    handleMouseUp() {
        if (this.state.columnSizeDragged) {
            this.setState({
                columnSizeDragged: false,
            });

            const {columnSizeDraggedIndex, colWidths} = this.state
            this.props.onColumnResize(this.getExtColumnIndex(columnSizeDraggedIndex), colWidths[columnSizeDraggedIndex])
        }
    }

    getExtColumnIndex(index) {
        const {cols} = this.state
        if (cols[0]._rowCheck) {
            return index -1
        } else {
            return index
        }
    }

    handleMouseMove(e) {
        if (this.state.columnSizeDragged) {
            this.unFocus();
            const ref = this.refs['col' + this.state.columnSizeDraggedIndex];
            if (ref) {
                const node = ReactDOM.findDOMNode(ref);
                if (node.getBoundingClientRect) {
                    const width = node.getBoundingClientRect().width;
                    const height = node.getBoundingClientRect().height;
                    const current = e.clientX;
                    const size = this.state.cols[this.state.columnSizeDraggedIndex].width;
                    const position = this.state.position;

                    let newSize = size - (position - current);
                    if (newSize < __minColWidth) newSize = __minColWidth
                    var {colWidths} = this.state
                    colWidths[this.state.columnSizeDraggedIndex] = newSize

                    this.setState({
                        colWidths: colWidths,
                    })
                }
            }
        }
    }

    handleCheckboxChange(row, rowIndex, e) {
        const {rows, onSelectedIdsChange} = this.props
        const {selectedIds, selectedRowIndexes} = this.state

        let currentChecked = selectedIds[row.id] ? true : false

        var newSelectedIds = {...selectedIds}

        // Pokud kliknul na řádek, který je označený podle indexu, bude pro další označené řádky podle indexu provedena stejná akce
        if (selectedRowIndexes[rowIndex]) {
            if (currentChecked) {   // odznačení
                Object.keys(selectedRowIndexes).forEach(index => {
                    delete newSelectedIds[rows[index].id]
                })
            } else {    // označení
                Object.keys(selectedRowIndexes).forEach(index => {
                    newSelectedIds[rows[index].id] = true
                })
            }
        } else {    // kliknul na jiný řádek, upravíme jen tuto položku
            if (currentChecked) {   // odznačení
                delete newSelectedIds[row.id]
            } else {    // označení
                newSelectedIds[row.id] = true
            }
        }

        onSelectedIdsChange(Object.keys(newSelectedIds))
    }

    handleContextMenu(row, rowIndex, col, colIndex, e) {
        const {onContextMenu} = this.props
        this.handleCellClick(rowIndex, colIndex, e)
        onContextMenu && onContextMenu(row, rowIndex, col, col._rowCheck ? colIndex - 1 : colIndex, e)
    }

    renderCell(row, rowIndex, col, colIndex, colFocus, cellFocus) {
        const {colWidths, selectedIds} = this.state

        var content
        if (col._rowCheck) {    // speciální slupeček pro označování řádků
            const checked = selectedIds[row.id] === true

            /// TODO - asi použít Checkbox místo input
            content = (
                <div key="content" className='cell-container'>
                    <input type='checkbox' checked={checked} onChange={this.handleCheckboxChange.bind(this, row, rowIndex)} />
                </div>
            )
        } else {
            if (col.cellRenderer) {
                content = <div key={"content-"+colIndex} className='cell-container'>{col.cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus)}</div>
            } else {
                content = <div key={"content-"+colIndex} className='cell-container'><div key={colIndex} className='value'>{row[col.dataName]}</div></div>
            }
        }

        const colCls = colFocus ? 'focus' : '';
        const cellCls = cellFocus ? ' cell-focus' : '';

        var style = {}
        if (rowIndex === 0) {
            style = {width: colWidths[colIndex], maxWidth: colWidths[colIndex] }
        }

        return (
            <td
                key={rowIndex + '-' + colIndex}
                ref={rowIndex + '-' + colIndex}
                className={colCls + cellCls}
                style={style}
                onClick={this.handleCellClick.bind(this, rowIndex, colIndex)}
                onDoubleClick={this.handleEdit.bind(this, rowIndex, colIndex)}
                onContextMenu={this.handleContextMenu.bind(this, row, rowIndex, col, colIndex)}
            >
                {content}
            </td>
        )
    }

    getCellElement(rowIndex, colIndex) {
        return this.refs[rowIndex + '-' + colIndex]
    }

    handleEdit(rowIndex, colIndex) {
        const {rows, onEdit, disabled} = this.props;
        const {cols} = this.state;
        if (disabled == null || !disabled) {
            onEdit(rows[rowIndex], rowIndex, cols[colIndex], colIndex)
        }
    }

    handleDelete(rowIndex, colIndex) {
        const {rows, onDelete} = this.props;
        const {cols} = this.state;
        onDelete && onDelete(rows[rowIndex], rowIndex, cols[colIndex], colIndex)
    }

    renderHeaderCol(col, colIndex, colFocus) {
        const {staticColumns} = this.props
        const {colWidths} = this.state

        var content

        if (col._rowCheck) {    // speciální slupeček pro označování řádků
            content = <div key="content" className='cell-container'></div>
        } else {
            if (col.headerColRenderer) {
                content = <div key="content" className='cell-container'>{col.headerColRenderer(col)}</div>
            } else {
                content = (
                    <div key="content" className='cell-container'>
                        <div className='value' title={col.desc}>{col.title}</div>
                    </div>
                )
            }
        }

        var colCls = colFocus ? 'focus' : ''

        var resizer;
        if (!staticColumns && (typeof col.resizeable === 'undefined' || col.resizeable !== false)) {
            resizer = <Resizer key="resizer" onMouseDown={this.handleResizerMouseDown.bind(this, colIndex)} />
        }

        var style = { width: colWidths[colIndex], maxWidth: colWidths[colIndex] };

        return (
            <th key={colIndex} ref={'col' + colIndex} className={colCls} style={style}>
                {content}
                {resizer}
            </th>
        )
    }

    ensureFocusVisible(focus) {
        var cellNode = ReactDOM.findDOMNode(this.refs[focus.row + '-' + focus.col])
        if (cellNode !== null) {
            var bodyNode = ReactDOM.findDOMNode(this.refs.body)
            scrollIntoView(cellNode, bodyNode, { onlyScrollIfNeeded: true })
        }
    }
    handleFocus(e){
        console.log("focus",this.props.onFocus)
        this.props.onFocus && this.props.onFocus(e);
    }
    changeFocus = (newFocus) => {
        const {onChangeFocus, onChangeRowIndexes} = this.props;

        this.setState({
                focus: newFocus,
                selectedRowIndexes: {[newFocus.row]: true}
            },
            this.ensureFocusVisible(newFocus)
        );
        onChangeFocus && onChangeFocus(newFocus.row, newFocus.col);
        onChangeRowIndexes && onChangeRowIndexes([newFocus.row]);
    }
    selectorMoveUp = () => {
        this.selectorMoveRelative(0,-1);
    }
    selectorMoveDown = () => {
        this.selectorMoveRelative(0,1);
    }
    selectorMoveLeft = () => {
        this.selectorMoveRelative(-1,0);
    }
    selectorMoveRight = () => {
        this.selectorMoveRelative(1,0);
    }
    selectorMoveRelative = (colStep,rowStep) => {
        const {focus:{row,col}} = this.state;
        //console.log("old","r:",row,"c:",col,"colStep",colStep,"rowStep",rowStep,"new","r:",newFocus.row,"/",rows.length,"c:",newFocus.col,"/",cols.length,"rowInRange:",rowInRange,"colInRange:",colInRange);
        var nextFocus = this.getRelativeSelectableItemIndex(row,col,rowStep,colStep);
        this.changeFocus(nextFocus);
    }
    getRelativeSelectableItemIndex = (row, col, rowStep,colStep) => {
        const {canSelectItem, rows} = this.props;
        const {cols} = this.state;

        var rowIsDecrementing = rowStep < 0;
        var colIsDecrementing = colStep < 0;
        var rowLast = false;

        if((row || row === 0) && (col || col === 0)){
            while (rowStep || colStep) {
                var newFocus = {row:row+rowStep,col:col+colStep};
                var rowInRange = newFocus.row >= 0 && newFocus.row < rows.length;
                var colInRange = newFocus.col >= 0 && newFocus.col < cols.length;
                if (rowInRange && colInRange) {
                    return newFocus;
                }
                if(!rowLast && rowStep || !colStep){
                    rowIsDecrementing ? rowStep++ : rowStep--;
                    rowLast = true;
                } else if(rowLast && colStep || !rowStep){
                    colIsDecrementing ? colStep++ : colStep--;
                    rowLast = false;
                }
            }
            return {row:row,col:col};
        } else {
            return 0;
        }
    }
    actionMap = {
        "MOVE_UP": this.selectorMoveUp,
        "MOVE_DOWN": this.selectorMoveDown,
        "MOVE_LEFT": this.selectorMoveLeft,
        "MOVE_RIGHT": this.selectorMoveRight,
        "ITEM_EDIT": (e) => this.handleEdit(this.state.focus.row,this.state.focus.col),
        "ITEM_DELETE": (e) => this.handleDelete(this.state.focus.row,this.state.focus.col),
        "ITEM_ROW_CHECK": (e) => this.handleCheckboxChange(this.props.rows[this.state.focus.row], this.state.focus.row, e)
    }
    handleShortcuts = (action,e)=>{
        console.log("DataGrid",action);
        e.stopPropagation();
        e.preventDefault();
        this.actionMap[action](e);
    }
    render() {
        var cls = this.props.className ? 'datagrid-container ' + this.props.className : 'datagrid-container'

        const {rows, onFocus, onBlur, staticColumns, disabled} = this.props;
        const {focus, cols, colWidths, selectedRowIndexes, selectedIds} = this.state;

        let fullWidth = 0;
        cols.forEach((col, colIndex) => {
            fullWidth += colWidths[colIndex]
        });
//var t1 = new Date().getTime()

        var headerStyle;
        if (staticColumns) {
            headerStyle = { };
        } else {
            headerStyle = { width: fullWidth + __emptyColWidth };
        }

        var bodyStyle;
        if (staticColumns) {
            bodyStyle = { };
        } else {
            bodyStyle = { width: fullWidth };
        }

        var tabIndexProp = {}
        if (!disabled) {
            tabIndexProp = {tabIndex: 1}
        }

        var ret = (
            <Shortcuts name="DataGrid" handler={this.handleShortcuts} tabIndex={1} className={cls}>
                <div ref="dataGrid" className={cls} onFocus={(e)=>this.handleFocus(e)} onBlur={onBlur}>
                    <div ref='header' key="header" className='header-container'>
                        <table className="header-table" style={headerStyle}>
                            <thead>
                                <tr>
                                    {cols.map((col, colIndex) => this.renderHeaderCol(col, colIndex, focus.col === colIndex))}
                                    {!staticColumns && <th key={-1} className='th-empty-scroll' />}
                                    {staticColumns && <th key={-1}/>}
                                </tr>
                            </thead>
                        </table>
                    </div>
                    <div ref='body' key="body" className='body-container' onScroll={this.handleScroll}>
                        <table className="body-table" style={bodyStyle}>
                            <tbody>
                                {rows.map((row, rowIndex) => {
                                    const rowWasFocus = focus.row === rowIndex

                                    var rowCls = rowWasFocus ? 'focus' : ''
                                    if (selectedRowIndexes[rowIndex]) {
                                        rowCls += ' selected-index'
                                    }
                                    if (selectedIds[row.id]) {
                                        rowCls += ' selected'
                                    }

                                    const cells = cols.map((col, colIndex) => this.renderCell(row, rowIndex, col, colIndex, focus.col === colIndex, rowWasFocus && focus.col === colIndex))
                                    return (
                                        <tr key={rowIndex} className={rowCls}>
                                            {cells}
                                            {staticColumns && <td key={-1} />}
                                        </tr>
                                    )
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            </Shortcuts>
        );
//console.log('ee', new Date().getTime() - t1)
        return ret
    }
};

DataGrid.defaultProps = {
    allowRowCheck: true,
    staticColumns: false,
}

// Definice sloupečku:
// Pokud je nastaveno staticColumns, bere se width jako procento
/*
{
    title: "nazev sloupce",
    desc: "popis sloupce",
    width: 30,              // pokud je staticColumns==false, je šířka sloupce v bodech, pokud je staticColumns==true, je šířka v bodech nebo procentech podle widthPercent
    widthPercent: true,     // pouze pro staticColumns - určuje, zda width je v bodech nebo procentech
    resizeable: true,   // je možné sloupečku měnit šířku
    cellRenderer: func,    // renderer na buňky dat
}

*/

DataGrid.propTypes = {
    rows: React.PropTypes.array.isRequired,
    cols: React.PropTypes.array.isRequired, // pole objektů definice sloupčeků, viz výše
    allowRowCheck: React.PropTypes.bool.isRequired,
    staticColumns: React.PropTypes.bool.isRequired,
    disabled: React.PropTypes.bool,
    focusRow: React.PropTypes.object,
    focusCol: React.PropTypes.object,
    selectedIds: React.PropTypes.array,
    selectedRowIndexes: React.PropTypes.array,
    onColumnResize: React.PropTypes.func,
    onChangeFocus: React.PropTypes.func,
    onChangeRowIndexes: React.PropTypes.func,
    onSelectedIdsChange: React.PropTypes.func,
    onContextMenu: React.PropTypes.func,
    onEdit: React.PropTypes.func,
    onDelete: React.PropTypes.func,
    onFocus: React.PropTypes.func,
    onBlur: React.PropTypes.func,
};

module.exports = connect(null, null, null, { withRef: true })(DataGrid);
