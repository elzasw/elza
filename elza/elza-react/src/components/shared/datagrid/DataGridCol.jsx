import React from 'react';
import AbstractReactComponent from '../../AbstractReactComponent';

class DataGridCol extends AbstractReactComponent {
    static propTypes = {
    };

    handleCheckboxChange = () => {
        const {onCheckboxChange, row, rowIndex} = this.props;
        onCheckboxChange(row, rowIndex)
    };

    handleCellClick = e => {
        const {onCellClick, col, rowIndex, colIndex} = this.props;
        if (col._rowCheck && (colIndex === 0 && e.target.tagName === 'INPUT')) {
            // kliknul na checkbox, nevoláme cell click jako takový
        } else {
            onCellClick(rowIndex, colIndex, e);
        }
    };

    render() {
        const {row, rowIndex, col, colIndex, hasFocus, onEdit, onContextMenu, colWidth, checked, fixedLeft, startRowIndex} = this.props;

        let content;
        if (col._rowCheck) {    // speciální slupeček pro označování řádků
            let style = {};
            style.left = fixedLeft;
            const val = rowIndex + 1 + startRowIndex;
            content = (
                <div key="content" className='cell-container rowCheck' style={style}>
                    <div title={val}>
                        <input type='checkbox' checked={checked} onChange={this.handleCheckboxChange} /> {val}
                    </div>
                </div>
            )
        } else {
            if (col.cellRenderer) {
                content = <div key={"content-"+colIndex} className='cell-container'>{col.cellRenderer(row, rowIndex, col, colIndex, hasFocus)}</div>
            } else {
                content = <div key={"content-"+colIndex} className='cell-container'><div key={colIndex} className='value'>{row[col.dataName]}</div></div>
            }
        }

        const cellCls = hasFocus ? ' cell-focus' : '';
        const colRowCheckCls = col._rowCheck && fixedLeft > 0 ? ' col-fixed' : '';

        let style = {};
        if (rowIndex === 0) {
            style = {width: colWidth, maxWidth: colWidth }
        }

        return (
            <td
                key={rowIndex + '-' + colIndex}
                ref={rowIndex + '-' + colIndex}
                className={cellCls + colRowCheckCls}
                style={style}
                onClick={this.handleCellClick}
                onDoubleClick={(e) => onEdit(rowIndex, colIndex)}
                onContextMenu={(e) => onContextMenu(row, rowIndex, col, colIndex, e)}
            >
                {content}
            </td>
        )
    }
}

export default DataGridCol;
