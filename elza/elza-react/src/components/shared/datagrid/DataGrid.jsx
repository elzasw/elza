/**
 * Data grid komponent - typu tabulka excel.
 */
import React from 'react';
import ReactDOM from 'react-dom';
import AbstractReactComponent from '../../AbstractReactComponent';
import * as Utils from '../../Utils';
import Resizer from '../resizer/Resizer';
import DataGridRow from './DataGridRow';
import i18n from '../../i18n';
import {getScrollbarWidth, propsEquals} from 'components/Utils.jsx';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './DataGridKeymap.jsx';

import './DataGrid.scss';

import scrollIntoView from 'dom-scroll-into-view';

const __emptyColWidth = 8000;
const __minColWidth = 16;

class DataGrid extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    constructor(props) {
        super(props);

        this.bindMethods(
            'handleScroll',
            'renderHeaderCol',
            'ensureFocusVisible',
            'handleResizerMouseDown',
            'handleMouseUp',
            'handleMouseMove',
            'unFocus',
            'getExtColumnIndex',
            'handleDelete',
            'computeColumnsWidth',
            'focus',
        );

        let state = this.getStateFromProps(props, {fixedLeft: 0}, {});
        state.columnSizeDragged = false;
        state.selectedRowIndexes = {[state.focus.row]: true}; // označené řádky v aktuálním zobrazení - pouze klientské označení

        this.state = state;
    }

    static defaultProps = {
        allowRowCheck: true,
        staticColumns: false,
        startRowIndex: 0,
        morePages: false,
    };

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
    static propTypes = {
        rows: PropTypes.array.isRequired,
        cols: PropTypes.array.isRequired, // pole objektů definice sloupčeků, viz výše
        allowRowCheck: PropTypes.bool.isRequired,
        staticColumns: PropTypes.bool.isRequired,
        disabled: PropTypes.bool,
        focusRow: PropTypes.object,
        focusCol: PropTypes.object,
        selectedIds: PropTypes.array,
        selectedRowIndexes: PropTypes.array,
        onColumnResize: PropTypes.func,
        onChangeFocus: PropTypes.func,
        onChangeRowIndexes: PropTypes.func,
        onSelectedIdsChange: PropTypes.func,
        onContextMenu: PropTypes.func,
        onEdit: PropTypes.func,
        onDelete: PropTypes.func,
        onFocus: PropTypes.func,
        onBlur: PropTypes.func,
        startRowIndex: PropTypes.number, // index prvního řádku
        morePages: PropTypes.bool, // true pokud je více stránek
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState(this.getStateFromProps(nextProps, this.state, this.props), () => {
            this.ensureFocusVisible(this.state.focus);
            this.computeColumnsWidth();
        });
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true; // zde NECHCEME!!! - stav se meni vzdy, protoze se vola getStateFromProps, po jeho pripadne uprave je mozne toto odkomentovat!
        }

        var eqProps = ['rows', 'cols', 'selectedIds', 'onColumnResize', 'onSelectedIdsChange'];
        //var eqProps = ['rows', 'cols', 'selectedIds']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    focus() {
        this.refs.dataGrid.focus();
    }

    getStateFromProps(props, currState, currProps) {
        // Cols a widths
        let cols;
        let colWidths;
        let needComputeColumnsWidth;
        if (
            currProps.staticColumns !== props.staticColumns ||
            currProps.allowRowCheck !== props.allowRowCheck ||
            currProps.cols !== props.cols
        ) {
            cols = [];
            colWidths = {};
            needComputeColumnsWidth = props.staticColumns;

            if (props.allowRowCheck) {
                const width = 60;
                cols.push({_rowCheck: true, width: width, resizeable: false});
                colWidths[0] = width;
            }
            props.cols.forEach((col, colIndex) => {
                var useColIndex = props.allowRowCheck ? colIndex + 1 : colIndex;

                if (props.staticColumns) {
                    if (currState.colWidths) {
                        colWidths[useColIndex] = currState.colWidths[useColIndex];
                    } else {
                        colWidths[useColIndex] = 10;
                    }
                } else {
                    colWidths[useColIndex] = col.width;
                }
                cols.push(col);
            });
        } else {
            cols = currState.cols || [];
            colWidths = currState.colWidths || {};
            needComputeColumnsWidth = currState.needComputeColumnsWidth || false;
        }

        // Selected ids a row indexes
        let selectedIds;
        let selectedRowIndexes;
        if (props.selectedIds !== currProps.selectedIds || props.selectedRowIndexes !== currProps.selectedRowIndexes) {
            selectedIds = {};
            if (typeof props.selectedIds !== 'undefined') {
                props.selectedIds.forEach(id => {
                    selectedIds[id] = true;
                });
            } else {
                selectedIds = currState.selectedIds;
            }
            selectedRowIndexes = {};
            if (typeof props.selectedRowIndexes !== 'undefined') {
                props.selectedRowIndexes.forEach(id => {
                    selectedRowIndexes[id] = true;
                });
            } else {
                selectedRowIndexes = currState.selectedRowIndexes;
            }
        } else {
            selectedIds = currProps.selectedIds || {};
            selectedRowIndexes = currProps.selectedRowIndexes || {};
        }

        // Focus
        let focus;
        if (props.focusRow !== currProps.focusRow || props.focusCol !== currProps.focusCol) {
            let focusRow;
            if (typeof props.focusRow !== 'undefined') {
                focusRow = props.focusRow;
            } else if (currState.focus) {
                focusRow = currState.focus.row;
            } else {
                focusRow = 0;
            }
            let focusCol;
            if (typeof props.focusCol !== 'undefined') {
                focusCol = props.focusCol;
            } else if (currState.focus) {
                focusCol = currState.focus.col;
            } else {
                focusCol = 0;
            }
            focus = {row: focusRow, col: focusCol};
        } else {
            if (currState.focus) {
                focus = currState.focus;
            } else {
                focus = {row: 0, col: 0};
            }
        }

        // ---

        return {
            focus,
            cols,
            colWidths,
            selectedRowIndexes,
            selectedIds, // označené řádky podle id - napříč stránkami - jedná se o reálné označení řádku, např. pro akce atp.
            needComputeColumnsWidth,
        };
    }

    componentDidMount() {
        document.addEventListener('mouseup', this.handleMouseUp);
        document.addEventListener('mousemove', this.handleMouseMove);
        this.ensureFocusVisible(this.state.focus);
        this.computeColumnsWidth();
        let bodyNode = ReactDOM.findDOMNode(this.refs.body);
        bodyNode.addEventListener('scroll', x => {
            if (this.state.scrollLeft !== x.target.scrollLeft) {
                this.setState({fixedLeft: x.target.scrollLeft});
            }
        });
    }

    componentWillUnmount() {
        document.removeEventListener('mouseup', this.handleMouseUp);
        document.removeEventListener('mousemove', this.handleMouseMove);
    }

    computeColumnsWidth() {
        const {cols, staticColumns} = this.props;
        const {needComputeColumnsWidth} = this.state;

        if (staticColumns && needComputeColumnsWidth) {
            const dataGrid = ReactDOM.findDOMNode(this.refs.dataGrid);
            const rect = dataGrid.getBoundingClientRect();
            var width = rect.width - getScrollbarWidth() - 4; // konstanta 3 - kvůli padding atp.
            var colWidths = {};

            // Nejdříve sloupce s pevnou šířkou
            cols.forEach((col, colIndex) => {
                if (!col.widthPercent) {
                    colWidths[colIndex] = col.width;
                    width -= col.width;
                }
            });

            // Sloupce s šířkou definovanou v procentech
            cols.forEach((col, colIndex) => {
                if (col.widthPercent) {
                    colWidths[colIndex] = Math.floor((col.width * width) / 100);
                }
            });
            // console.log("width", rect.width, "scrollbar", getScrollbarWidth(), "colWidths", colWidths, "cols", cols)

            this.setState({
                colWidths,
            });
        }
    }

    handleCellClick = (rowIndex, colIndex, e) => {
        this.unFocus();

        const {focus, selectedRowIndexes} = this.state;
        var newFocus = {row: rowIndex, col: colIndex};
        var newSelectedRowIndexes = selectedRowIndexes;

        if (e.ctrlKey) {
            if (selectedRowIndexes[rowIndex]) {
                // je označená, odznačíme, ji, ale jen pokud není jediná
                if (Object.keys(selectedRowIndexes).length > 1) {
                    newSelectedRowIndexes = {...selectedRowIndexes};
                    delete newSelectedRowIndexes[rowIndex];
                }
            } else {
                // není označená, přidáme ji o označení
                newSelectedRowIndexes = {...selectedRowIndexes, [rowIndex]: true};
            }
        } else if (e.shiftKey) {
            var x1 = focus.row;
            var x2 = rowIndex;
            if (x1 > x2) {
                var x3 = x1;
                x1 = x2;
                x2 = x3;
            }
            newSelectedRowIndexes = {};
            for (var a = x1; a <= x2; a++) {
                newSelectedRowIndexes[a] = true;
            }
        } else {
            newSelectedRowIndexes = {[rowIndex]: true};
        }

        this.setState(
            {
                focus: newFocus,
                selectedRowIndexes: newSelectedRowIndexes,
            },
            this.ensureFocusVisible(newFocus),
        );

        const {onChangeFocus, onChangeRowIndexes} = this.props;
        onChangeFocus && onChangeFocus(newFocus.row, newFocus.col);
        onChangeRowIndexes && onChangeRowIndexes(Object.keys(newSelectedRowIndexes));
    };

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges();
        }
    }

    handleScroll(e) {
        ReactDOM.findDOMNode(this.refs.header).scrollLeft = e.target.scrollLeft;
    }

    handleResizerMouseDown(colIndex, e) {
        this.unFocus();

        const {colWidths} = this.state;

        this.setState({
            position: e.clientX,
            columnSizeDragged: true,
            columnSizeDraggedIndex: colIndex,
            beforeDraggedColWidths: colWidths,
        });
    }

    handleMouseUp() {
        if (this.state.columnSizeDragged) {
            this.setState({
                columnSizeDragged: false,
            });

            const {columnSizeDraggedIndex, colWidths} = this.state;
            this.props.onColumnResize(
                this.getExtColumnIndex(columnSizeDraggedIndex),
                colWidths[columnSizeDraggedIndex],
            );
        }
    }

    getExtColumnIndex(index) {
        const {cols} = this.state;
        if (cols[0]._rowCheck) {
            return index - 1;
        } else {
            return index;
        }
    }

    handleMouseMove(e) {
        if (this.state.columnSizeDragged) {
            this.unFocus();
            const ref = this.refs['col' + this.state.columnSizeDraggedIndex];
            if (ref) {
                const node = ReactDOM.findDOMNode(ref);
                if (node.getBoundingClientRect) {
                    const current = e.clientX;
                    const size = this.state.cols[this.state.columnSizeDraggedIndex].width;
                    const position = this.state.position;

                    let newSize = size - (position - current);
                    if (newSize < __minColWidth) newSize = __minColWidth;
                    const {colWidths} = this.state;
                    colWidths[this.state.columnSizeDraggedIndex] = newSize;

                    this.setState({
                        colWidths: {...colWidths},
                    });
                }
            }
        }
    }

    handleCheckboxChange = (row, rowIndex) => {
        const {rows, onSelectedIdsChange} = this.props;
        const {selectedIds, selectedRowIndexes} = this.state;

        let currentChecked = selectedIds[row.id] ? true : false;

        var newSelectedIds = {...selectedIds};

        // Pokud kliknul na řádek, který je označený podle indexu, bude pro další označené řádky podle indexu provedena stejná akce
        if (selectedRowIndexes[rowIndex]) {
            if (currentChecked) {
                // odznačení
                Object.keys(selectedRowIndexes).forEach(index => {
                    delete newSelectedIds[rows[index].id];
                });
            } else {
                // označení
                Object.keys(selectedRowIndexes).forEach(index => {
                    newSelectedIds[rows[index].id] = true;
                });
            }
        } else {
            // kliknul na jiný řádek, upravíme jen tuto položku
            if (currentChecked) {
                // odznačení
                delete newSelectedIds[row.id];
            } else {
                // označení
                newSelectedIds[row.id] = true;
            }
        }

        onSelectedIdsChange(Object.keys(newSelectedIds));
    };

    handleContextMenu = (row, rowIndex, col, colIndex, e) => {
        const {onContextMenu} = this.props;
        this.handleCellClick(rowIndex, colIndex, {});
        onContextMenu && onContextMenu(row, rowIndex, col, col._rowCheck ? colIndex - 1 : colIndex, e);
    };

    getCellElement = (rowIndex, colIndex) => {
        let rowElement = this.getRowElement(rowIndex);
        return rowElement ? rowElement.getCellElement(colIndex) : null;
        // return this.refs[rowIndex + '-' + colIndex]
    };

    getRowElement = rowIndex => {
        return this.refs[`row-${rowIndex}`];
    };

    handleEdit = (rowIndex, colIndex) => {
        const {rows, onEdit, disabled} = this.props;
        const {cols} = this.state;
        if (disabled == null || !disabled) {
            onEdit(rows[rowIndex], rowIndex, cols[colIndex], colIndex);
        }
    };

    handleCheckboxAllChange = () => {
        const {rows, onSelectedIdsChange} = this.props;

        const allSelected = this.isAllSelected();
        let newSelectedIds = [];

        if (!allSelected) {
            rows.forEach((row, rowIndex) => {
                let rowId = row.id;
                newSelectedIds.push(rowId);
            });
        }

        console.log(newSelectedIds);
        onSelectedIdsChange(newSelectedIds);
    };

    handleDelete(rowIndex, colIndex) {
        const {rows, onDelete} = this.props;
        const {cols} = this.state;
        onDelete && onDelete(rows[rowIndex], rowIndex, cols[colIndex], colIndex);
    }

    renderHeaderCol(col, colIndex, colFocus) {
        const {staticColumns} = this.props;
        const {colWidths, fixedLeft} = this.state;

        var content;

        if (col._rowCheck) {
            // speciální slupeček pro označování řádků
            let style = {};
            style.position = 'relative';
            style.left = fixedLeft;
            content = (
                <div key="content" style={style} className="cell-container">
                    <input type="checkbox" checked={this.isAllSelected()} onChange={this.handleCheckboxAllChange} />
                </div>
            );
        } else {
            if (col.headerColRenderer) {
                content = (
                    <div key="content" className="cell-container">
                        {col.headerColRenderer(col)}
                    </div>
                );
            } else {
                content = (
                    <div key="content" className="cell-container">
                        <div className="value" title={col.desc}>
                            {col.title}
                        </div>
                    </div>
                );
            }
        }

        var colCls = colFocus ? 'focus' : '';
        const colRowCheckCls = col._rowCheck && fixedLeft > 0 ? ' header-fixed' : '';

        var resizer;
        if (!staticColumns && (typeof col.resizeable === 'undefined' || col.resizeable !== false)) {
            resizer = <Resizer key="resizer" onMouseDown={this.handleResizerMouseDown.bind(this, colIndex)} />;
        }

        var style = {width: colWidths[colIndex], maxWidth: colWidths[colIndex]};

        return (
            <th key={colIndex} ref={'col' + colIndex} className={colCls + colRowCheckCls} style={style}>
                {content}
                {resizer}
            </th>
        );
    }

    isAllSelected() {
        const {rows} = this.props;
        const {selectedIds} = this.state;
        return Object.keys(selectedIds).length === rows.length;
    }

    ensureFocusVisible(focus) {
        // var cellNode = ReactDOM.findDOMNode(this.refs[focus.row + '-' + focus.col])
        const cellNode = this.getCellElement(focus.row, focus.col);
        if (cellNode !== null) {
            var bodyNode = ReactDOM.findDOMNode(this.refs.body);
            scrollIntoView(cellNode, bodyNode, {onlyScrollIfNeeded: true});
        }
    }

    handleFocus(e) {
        this.props.onFocus && this.props.onFocus(e);
    }

    changeFocus = newFocus => {
        const {onChangeFocus, onChangeRowIndexes} = this.props;

        this.setState(
            {
                focus: newFocus,
                selectedRowIndexes: {[newFocus.row]: true},
            },
            this.ensureFocusVisible(newFocus),
        );
        onChangeFocus && onChangeFocus(newFocus.row, newFocus.col);
        onChangeRowIndexes && onChangeRowIndexes([newFocus.row]);
    };
    selectorMoveUp = () => {
        this.selectorMoveRelative(0, -1);
    };
    selectorMoveDown = () => {
        this.selectorMoveRelative(0, 1);
    };
    selectorMoveLeft = () => {
        this.selectorMoveRelative(-1, 0);
    };
    selectorMoveRight = () => {
        this.selectorMoveRelative(1, 0);
    };
    selectorMoveRelative = (colStep, rowStep) => {
        const {
            focus: {row, col},
        } = this.state;
        //console.log("old","r:",row,"c:",col,"colStep",colStep,"rowStep",rowStep,"new","r:",newFocus.row,"/",rows.length,"c:",newFocus.col,"/",cols.length,"rowInRange:",rowInRange,"colInRange:",colInRange);
        var nextFocus = this.getRelativeSelectableItemIndex(row, col, rowStep, colStep);
        this.changeFocus(nextFocus);
    };
    getRelativeSelectableItemIndex = (row, col, rowStep, colStep) => {
        const {rows} = this.props;
        const {cols} = this.state;

        const rowIsDecrementing = rowStep < 0;
        const colIsDecrementing = colStep < 0;
        let rowLast = false;

        if ((row || row === 0) && (col || col === 0)) {
            while (rowStep || colStep) {
                const newFocus = {row: row + rowStep, col: col + colStep};
                const rowInRange = newFocus.row >= 0 && newFocus.row < rows.length;
                const colInRange = newFocus.col >= 0 && newFocus.col < cols.length;
                if (rowInRange && colInRange) {
                    return newFocus;
                }
                if (!rowLast && (rowStep || !colStep)) {
                    rowIsDecrementing ? rowStep++ : rowStep--;
                    rowLast = true;
                } else if (rowLast && (colStep || !rowStep)) {
                    colIsDecrementing ? colStep++ : colStep--;
                    rowLast = false;
                }
            }
            return {row: row, col: col};
        } else {
            return 0;
        }
    };
    actionMap = {
        MOVE_UP: this.selectorMoveUp,
        MOVE_DOWN: this.selectorMoveDown,
        MOVE_LEFT: this.selectorMoveLeft,
        MOVE_RIGHT: this.selectorMoveRight,
        ITEM_EDIT: e => this.handleEdit(this.state.focus.row, this.state.focus.col),
        ITEM_DELETE: e => this.handleDelete(this.state.focus.row, this.state.focus.col),
        ITEM_ROW_CHECK: e => this.handleCheckboxChange(this.props.rows[this.state.focus.row], this.state.focus.row),
    };

    handleShortcuts = (action, e) => {
        e.stopPropagation();
        e.preventDefault();
        this.actionMap[action](e);
    };

    render() {
        const cls = this.props.className ? 'datagrid-container ' + this.props.className : 'datagrid-container';

        const {rows, onBlur, staticColumns, startRowIndex, morePages} = this.props;
        const {
            columnSizeDragged,
            focus,
            cols,
            colWidths,
            beforeDraggedColWidths,
            selectedRowIndexes,
            selectedIds,
            fixedLeft,
        } = this.state;

        let fullWidth = 0;
        let beforeDraggedFullWidth = 0;
        cols.forEach((col, colIndex) => {
            fullWidth += colWidths[colIndex];
            if (columnSizeDragged) {
                beforeDraggedFullWidth += beforeDraggedColWidths[colIndex];
            }
        });

        //var t1 = new Date().getTime()

        let headerStyle;
        if (staticColumns) {
            headerStyle = {};
        } else {
            headerStyle = {width: fullWidth + __emptyColWidth};
        }

        let bodyStyle;
        if (staticColumns) {
            bodyStyle = {};
        } else {
            bodyStyle = {width: columnSizeDragged ? beforeDraggedFullWidth : fullWidth};
        }

        var ret = (
            <Shortcuts name="DataGrid" handler={this.handleShortcuts} contenteditable="true" className={cls}>
                <div ref="dataGrid" className={cls} onFocus={e => this.handleFocus(e)} tabIndex={0} onBlur={onBlur}>
                    <div ref="header" key="header" className="header-container">
                        <table className="header-table" style={headerStyle}>
                            <thead>
                                <tr>
                                    {cols.map((col, colIndex) =>
                                        this.renderHeaderCol(col, colIndex, focus.col === colIndex),
                                    )}
                                    {!staticColumns && <th key={-1} className="th-empty-scroll" />}
                                    {staticColumns && <th key={-1} />}
                                </tr>
                            </thead>
                        </table>
                    </div>
                    <div ref="body" key="body" className="body-container" onScroll={this.handleScroll}>
                        <table className="body-table" style={bodyStyle}>
                            <tbody>
                                {rows.map((row, rowIndex) => (
                                    <DataGridRow
                                        ref={`row-${rowIndex}`}
                                        key={`row-${rowIndex}`}
                                        checked={selectedIds[row.id] === true}
                                        rowIndex={rowIndex}
                                        hasFocus={focus.row === rowIndex}
                                        colFocus={focus.col}
                                        cols={cols}
                                        selected={selectedRowIndexes[rowIndex]}
                                        row={row}
                                        staticColumns={staticColumns}
                                        startRowIndex={startRowIndex}
                                        fixedLeft={fixedLeft}
                                        colWidths={columnSizeDragged ? beforeDraggedColWidths : colWidths}
                                        onCheckboxChange={this.handleCheckboxChange}
                                        onCellClick={this.handleCellClick}
                                        onEdit={this.handleEdit}
                                        onContextMenu={this.handleContextMenu}
                                    />
                                ))}
                            </tbody>
                        </table>
                        {morePages && rows.length > 0 && (
                            <div>{i18n('fund.grid.morePages', startRowIndex + 1, startRowIndex + rows.length)}</div>
                        )}
                    </div>
                </div>
            </Shortcuts>
        );
        //console.log('ee', new Date().getTime() - t1)
        return ret;
    }
}

export default DataGrid;
