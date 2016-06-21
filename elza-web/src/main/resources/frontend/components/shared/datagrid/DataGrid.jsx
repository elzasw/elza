/**
 * Data grid komponent - typu tabulka excel.
 */

require ('./DataGrid.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Resizer} from 'components/index.jsx';
const scrollIntoView = require('dom-scroll-into-view')
import {propsEquals} from 'components/Utils.jsx'

const __emptyColWidth = 8000
const __minColWidth = 16

const keyDownHandlers = {
    changeFocus: function(newFocus) {
        this.setState({ focus: newFocus, selectedRowIndexes: {[newFocus.row]: true} }, this.ensureFocusVisible(newFocus))

        const {onChangeFocus, onChangeRowIndexes} = this.props
        onChangeFocus && onChangeFocus(newFocus.row, newFocus.col)
        onChangeRowIndexes && onChangeRowIndexes([newFocus.row]);
    },
    Enter: function(e) {
        const {focus} = this.state

        e.stopPropagation();
        e.preventDefault();

        this.handleEdit(focus.row, focus.col)
    },
    'F2': function(e) {
        const {focus} = this.state

        e.stopPropagation();
        e.preventDefault();

        this.handleEdit(focus.row, focus.col)
    },
    ' ': function(e) {
        const {focus} = this.state
        const {rows} = this.props

        e.stopPropagation();
        e.preventDefault();

        this.handleCheckboxChange(rows[focus.row], focus.row, e)
    },
    ArrowUp: function(e) {
        const {focus} = this.state

        e.stopPropagation();
        e.preventDefault();

        if (focus.row > 0) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row - 1, col: focus.col })
        }
    },
    ArrowDown: function(e) {
        const {focus} = this.state
        const {rows} = this.props

        e.stopPropagation();
        e.preventDefault();

        if (focus.row + 1 < rows.length) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row + 1, col: focus.col })
        }
    },
    ArrowLeft: function(e) {
        const {focus} = this.state

        e.stopPropagation();
        e.preventDefault();

        if (focus.col > 0) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row, col: focus.col - 1 })
        }
    },
    ArrowRight: function(e) {
        const {focus, cols} = this.state

        e.stopPropagation();
        e.preventDefault();
        if (focus.col + 1 < cols.length) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row, col: focus.col + 1 })
        }
    },
}

var DataGrid = class DataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'handleScroll',
            'renderHeaderCol',
            'renderCell',
            'handleKeyDown',
            'ensureFocusVisible',
            'handleResizerMouseDown',
            'handleMouseUp',
            'handleMouseMove',
            'unFocus',
            'handleCellClick',
            'handleCheckboxChange',
            'getExtColumnIndex',
            'handleEdit',
            'handleContextMenu'
        );

        var state = this.getStateFromProps(props, {})
        state.columnSizeDragged = false
        state.selectedRowIndexes = {[state.focus.row]: true}   // označené řádky v aktuálním zobrazení - pouze klientské označení

        this.state = state
    }

    componentWillReceiveProps(nextProps) {
        this.setState(this.getStateFromProps(nextProps, this.state),
        () => {
            this.ensureFocusVisible(this.state.focus)
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

    getStateFromProps(props, currState) {
        var cols = []
        if (props.allowRowCheck) {
            cols.push({_rowCheck: true, width: 32, resizeable: false})
        }
        props.cols.forEach((col, colIndex) => {
            cols.push(col)
        })

        var colWidths = {}
        cols.forEach((col, colIndex) => {
            colWidths[colIndex] = col.width
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
        }
    }

    componentDidMount() {
        document.addEventListener('mouseup', this.handleMouseUp);
        document.addEventListener('mousemove', this.handleMouseMove);
        this.ensureFocusVisible(this.state.focus)
    }

    componentWillUnmount() {
        document.removeEventListener('mouseup', this.handleMouseUp);
        document.removeEventListener('mousemove', this.handleMouseMove);
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

    handleKeyDown(event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
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

            content = (
                <div className='cell-container'>
                    <input type='checkbox' checked={checked} onChange={this.handleCheckboxChange.bind(this, row, rowIndex)} />
                </div>
            )
        } else {
            if (col.cellRenderer) {
                content = <div className='cell-container'>{col.cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus)}</div>
            } else {
                content = <div className='cell-container'><div className='value'>{row[col.dataName]}</div></div>
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
                key={colIndex}
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
        const {rows, onEdit} = this.props;
        const {cols} = this.state;
        onEdit(rows[rowIndex], rowIndex, cols[colIndex], colIndex)
    }

    renderHeaderCol(col, colIndex, colFocus) {
        const {colWidths} = this.state
        const {staticColumns} = this.props

        var content

        if (col._rowCheck) {    // speciální slupeček pro označování řádků
            content = <div className='cell-container'></div>
        } else {
            if (col.headerColRenderer) {
                content = <div className='cell-container'>{col.headerColRenderer(col)}</div>
            } else {
                content = (
                    <div className='cell-container'>
                        <div className='value' title={col.desc}>{col.title}</div>
                    </div>
                )
            }
        }

        var colCls = colFocus ? 'focus' : ''

        var resizer;
        if (typeof col.resizeable === 'undefined' || col.resizeable !== false) {
            resizer = <Resizer onMouseDown={this.handleResizerMouseDown.bind(this, colIndex)} />
        }

        var style;
        if (staticColumns) {
            style = { width: "20%" };
        } else {
            style = { width: colWidths[colIndex], maxWidth: colWidths[colIndex] };
        }

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

    render() {
        var cls = this.props.className ? 'datagrid-container ' + this.props.className : 'datagrid-container'

        const {rows, onFocus, onBlur, staticColumns} = this.props;
        const {focus, cols, colWidths, selectedRowIndexes, selectedIds} = this.state;

        let fullWidth = 0;
        cols.forEach((col, colIndex) => {
            fullWidth += colWidths[colIndex]
        });
//var t1 = new Date().getTime()

        var style;
        if (staticColumns) {
            style = { width: "100%" };
        } else {
            style = { width: fullWidth + __emptyColWidth };
        }

        var ret = (
            <div className={cls} onKeyDown={this.handleKeyDown} tabIndex={0} onFocus={onFocus} onBlur={onBlur}>
                <div ref='header' className='header-container'>
                    <table style={style}>
                        <thead>
                            <tr>
                                {cols.map((col, colIndex) => this.renderHeaderCol(col, colIndex, focus.col === colIndex))}
                                {!staticColumns && <th key={-1} className='th-empty-scroll' />}
                            </tr>
                        </thead>
                    </table>
                </div>
                <div ref='body' className='body-container' onScroll={this.handleScroll}>
                    <table style={{width: fullWidth}}>
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
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            </div>
        );
//console.log('ee', new Date().getTime() - t1)
        return ret
    }
};

DataGrid.defaultProps = {
    allowRowCheck: true,
    staticColumns: false,
}

DataGrid.propTypes = {
    rows: React.PropTypes.array.isRequired,
    cols: React.PropTypes.array.isRequired,
    allowRowCheck: React.PropTypes.bool.isRequired,
    staticColumns: React.PropTypes.bool.isRequired,
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
    onFocus: React.PropTypes.func,
    onBlur: React.PropTypes.func,
};

module.exports = connect(null, null, null, { withRef: true })(DataGrid);
