/**
 * Data grid komponent - typu tabulka excel.
 */

require ('./DataGrid.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Resizer} from 'components';
const scrollIntoView = require('dom-scroll-into-view')
import {propsEquals} from 'components/Utils'

const __emptyColWidth = 8000
const __minColWidth = 16

var keyDownHandlers = {
    changeFocus: function(newFocus) {
        this.setState({ focus: newFocus, selectedRowIndexes: {[newFocus.row]: true} }, this.ensureFocusVisible(newFocus))
    },
    'F2': function(e) {
        const {focus} = this.state
        this.handleEdit(focus.row, focus.col)
    },
    ' ': function(e) {
        const {focus} = this.state
        const {rows} = this.props
        
        this.handleCheckboxChange(rows[focus.row], focus.row, e)
    },
    ArrowUp: function(e) {
        const {focus} = this.state
        if (focus.row > 0) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row - 1, col: focus.col })
        }
    },
    ArrowDown: function(e) {
        const {focus} = this.state
        const {rows} = this.props

        if (focus.row + 1 < rows.length) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row + 1, col: focus.col })
        }
    },
    ArrowLeft: function(e) {
        const {focus} = this.state

        if (focus.col > 0) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row, col: focus.col - 1 })
        }
    },
    ArrowRight: function(e) {
        const {focus} = this.state
        const {cols} = this.props

        if (focus.col + 1 < cols.length) {
            keyDownHandlers.changeFocus.bind(this)({ row: focus.row, col: focus.col + 1 })
        }
    },
}

var DataGrid = class DataGrid extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleScroll', 'renderHeaderCol', 'renderCell',
            'handleKeyDown', 'ensureFocusVisible', 'handleResizerMouseDown',
            'handleMouseUp', 'handleMouseMove', 'unFocus', 'handleCellClick',
            'handleCheckboxChange', 'getExtColumnIndex', 'handleEdit');

        var state = this.getStateFromProps(props, {})
        state.columnSizeDragged = false
        state.selectedRowIndexes = {[state.focus.row]: true}   // označené řádky v aktuálním zobrazení - pouze klientské označení

        this.state = state
    }

    componentWillReceiveProps(nextProps) {
        this.setState(this.getStateFromProps(nextProps, this.state))
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;  // zde NECHCEME!!! - stavse meni vzdy, protoze se vola getStateFromProps, po jeho pripadne uprave je mozne toto odkomentovat!
        }

        var eqProps = ['rows', 'cols', 'selectedIds', 'onColumnResize', 'onSelectedIdsChange']
        //var eqProps = ['rows', 'cols', 'selectedIds']
        return !propsEquals(this.props, nextProps, eqProps)
    }

    getStateFromProps(props, currState) {
        var cols = []
        cols.push({_rowCheck: true, width: 32, resizeable: false})
        props.cols.forEach((col, colIndex) => {
            cols.push(col)
        })

        var colWidths = {}
        cols.forEach((col, colIndex) => {
            colWidths[colIndex] = col.width
        })

        var selectedIds = {}
        props.selectedIds.forEach(id => {
            selectedIds[id] = true
        })

        var focus = currState.focus || props.focus || {row: 0, col: 0}

        return {
            focus: focus,
            cols: cols,
            colWidths: colWidths,
            selectedIds: selectedIds,    // označené řádky podle id - napříč stránkami - jedná se o reálné označení řádku, např. pro akce atp.
        }
    }

    componentDidMount() {
        document.addEventListener('mouseup', this.handleMouseUp);
        document.addEventListener('mousemove', this.handleMouseMove);
    }

    componentWillUnmount() {
        document.removeEventListener('mouseup', this.handleMouseUp);
        document.removeEventListener('mousemove', this.handleMouseMove);
    }

    handleCellClick(rowIndex, colIndex, e) {
        if (colIndex === 0 && e.target.tagName === 'INPUT') {   // první sloupec - označování řádků, zde neřešíme, pokud klikne na checkbox
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
        const {rows} = this.props
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

        this.props.onSelectedIdsChange(Object.keys(newSelectedIds))
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
                content = col.cellRenderer(row, rowIndex, col, colIndex, colFocus, cellFocus)
            } else {
                content = <div className='cell-container'>{row[col.dataName]}</div>
            }
        }

        var colCls = colFocus ? 'focus' : ''
        var cellCls = cellFocus ? ' cell-focus' : ''
        
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
            >
                {content}
            </td>
        )
    }

    handleEdit(rowIndex, colIndex) {
        const {rows} = this.props
        const {cols} = this.state
        this.props.onEdit(rows[rowIndex], cols[colIndex])
    }

    renderHeaderCol(col, colIndex, colFocus) {
        const {colWidths} = this.state

        var content

        if (col._rowCheck) {    // speciální slupeček pro označování řádků
            content = <div className='cell-container'></div>
        } else {
            if (col.headerColRenderer) {
                content = col.headerColRenderer(col)
            } else {
                content = <div className='cell-container' title={col.desc}>{col.title}</div>
            }
        }

        var colCls = colFocus ? 'focus' : ''

        var resizer
        if (typeof col.resizeable === 'undefined'
            || col.resizeable !== false) {
            resizer = <Resizer onMouseDown={this.handleResizerMouseDown.bind(this, colIndex)} />
        }

        return (
            <th ref={'col' + colIndex} className={colCls} style={{width: colWidths[colIndex], maxWidth: colWidths[colIndex]}}>
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

        const {rows} = this.props
        const {focus, cols, colWidths, selectedRowIndexes, selectedIds} = this.state

        var fullWidth = 0
        cols.forEach((col, colIndex) => {
            fullWidth += colWidths[colIndex]
        })
//var t1 = new Date().getTime()
        var ret = (
            <div className={cls} onKeyDown={this.handleKeyDown} tabIndex={0}>
                <div ref='header' className='header-container'>
                    <table style={{width: fullWidth + __emptyColWidth}}>
                        <thead>
                            <tr>
                                {cols.map((col, colIndex) => this.renderHeaderCol(col, colIndex, focus.col === colIndex))}
                                <th className='th-empty-scroll'></th>
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
                                    <tr className={rowCls} key={rowIndex}>
                                        {cells}
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            </div>
        )
//console.log('ee', new Date().getTime() - t1)
        return ret
    }
}

module.exports = connect()(DataGrid);
