import React from 'react';
import ReactDOM from 'react-dom';
import AbstractReactComponent from '../../AbstractReactComponent';
import DataGridCol from './DataGridCol';
import classNames from 'classnames';

class DataGridRow extends AbstractReactComponent {
    static propTypes = {
    };

    getCellElement = (colIndex) => {
        return ReactDOM.findDOMNode(this.refs[`col-${colIndex}`]);
    };

    render() {
        const {rowIndex, cols, hasFocus, onCheckboxChange, onCellClick, onEdit, colFocus, onContextMenu, selected, row, checked, staticColumns, startRowIndex, fixedLeft, colWidths} = this.props;

        let rowCls = classNames({
            focus: hasFocus,
            selected: checked,
            "selected-index": selected
        });

        const cells = cols.map((col, colIndex) => <DataGridCol
            key={colIndex}
            ref={`col-${colIndex}`}
            hasFocus={hasFocus && colFocus === colIndex}
            onCheckboxChange={onCheckboxChange}
            onCellClick={onCellClick}
            onEdit={onEdit}
            onContextMenu={onContextMenu}
            startRowIndex={startRowIndex}
            row={row}
            rowIndex={rowIndex}
            col={col}
            colIndex={colIndex}
            fixedLeft={fixedLeft}
            colWidth={colWidths[colIndex]}
            checked={checked}
        />);

        return <tr key={rowIndex} className={rowCls}>
            {cells}
            {staticColumns && <td key={-1} />}
        </tr>
    }
}

export default DataGridRow;
