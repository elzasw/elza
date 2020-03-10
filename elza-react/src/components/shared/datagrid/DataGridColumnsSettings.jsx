/**
 * Komponenta výběru sloupců pro grid - s možností změny jejich pořadí.
 */
import React from 'react';
import {Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import {getMapFromList} from 'stores/app/utils.jsx';

import './DataGridColumnsSettings.scss';
import AbstractReactComponent from '../../AbstractReactComponent';
import ListBox from '../listbox/ListBox';
import i18n from '../../i18n';

class DataGridColumnsSettings extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChangeOrder', 'handleAddVisible', 'handleRemoveVisible', 'handleChangeSelection');

        var colsMap = getMapFromList(props.columns);
        const visible = [];
        const available = [];
        props.columns.forEach(col => {
            if (props.visibleColumns[col.id]) {
                visible.push(col);
            } else {
                available.push(col);
            }
        });

        // Seřazení dostupných sloupečků podle abecedy
        available.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

        this.state = {
            columns: props.columns,
            leftSelected: [],
            rightSelected: [],
            visible: visible,
            available: available,
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
    }

    handleChangeOrder(from, to) {
        var visible = [...this.state.visible];
        visible.splice(to, 0, visible.splice(from, 1)[0]);
        const index = this.state.rightSelected.indexOf('' + from);
        const result = {visible};
        if (index > -1) {
            result.rightSelected = [...this.state.rightSelected];
            result.rightSelected.splice(index, 1, '' + to);
        }
        this.setState(result);
    }

    handleAddVisible() {
        var {available, visible} = this.state;
        const {leftSelected} = this.state;

        var selectedMap = {};
        leftSelected.forEach(index => {
            selectedMap[index] = true;
        });

        var newAvailable = [];
        var newVisible = [...visible];

        available.forEach((item, index) => {
            if (selectedMap[index]) {
                newVisible.push(item);
            } else {
                newAvailable.push(item);
            }
        });

        const newRightSelected = [];
        const originalLength = visible.length;
        for (let newLength = newVisible.length; newLength > originalLength; newLength--) {
            newRightSelected.push(newLength - 1);
        }

        this.setState({
            available: newAvailable,
            visible: newVisible,
            leftSelected: [],
            rightSelected: newRightSelected,
        });
    }

    handleRemoveVisible() {
        const {rightSelected, available, visible} = this.state;
        const {columns} = this.props;

        const selectedMap = {};
        rightSelected.forEach(index => {
            selectedMap[index] = true;
        });

        const rightSelectedIds = [];
        visible.map((item, index) => {
            if (selectedMap[index]) {
                rightSelectedIds.push(item.id);
            }
        });


        // Upravení seznamu visible
        const newVisible = [];
        visible.forEach((item, index) => {
            if (!selectedMap[index]) {
                newVisible.push(item);
            }
        });

        // Získání nového seznamu available
        const visibleMap = getMapFromList(newVisible);
        const newAvailable = [];
        columns.forEach(col => {
            if (!visibleMap[col.id]) {
                newAvailable.push(col);
            }
        });
        // Seřazení dostupných sloupečků podle abecedy
        newAvailable.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

        // ---

        // Ziskání

        const newLeftSelected = [];
        newAvailable.map((item, index) => {
            if (rightSelectedIds.indexOf(item.id) > -1) {
                newLeftSelected.push(index);
            }
        });

        this.setState({
            available: newAvailable,
            visible: newVisible,
            rightSelected: [],
            leftSelected: newLeftSelected,
        });
    }

    handleChangeSelection(type, sel) {
        if (type === 'left') {
            this.setState({leftSelected: sel});
        } else if (type === 'right') {
            this.setState({rightSelected: sel});
        }
    }

    renderItemContent(props) {
        return (<div>{props.item.name}</div>);
    }

    render() {
        const {onSubmitForm, onClose} = this.props;
        const {available, visible, leftSelected, rightSelected} = this.state;

        const cls = this.props.className ? 'datagrid-columns-settings-container ' + this.props.className : 'datagrid-columns-settings-container';
        return (
            <div>
                <Modal.Body>
                    <div className={cls}>
                        <div className='panels-container'>
                            <div className='left'>
                                <h4>{i18n('arr.fund.columnSettings.available')}</h4>
                                <ListBox
                                    items={available}
                                    multiselect
                                    activeIndexes={leftSelected}
                                    renderItemContent={this.renderItemContent}
                                    onChangeSelection={this.handleChangeSelection.bind(this, 'left')}
                                    onDoubleClick={this.handleAddVisible}
                                />
                            </div>
                            <div className='center'>
                                <div className="action-buttons">
                                    <Button onClick={this.handleAddVisible}>&gt;</Button>
                                    <Button onClick={this.handleRemoveVisible}>&lt;</Button>
                                </div>
                            </div>
                            <div className='right'>
                                <h4>{i18n('arr.fund.columnSettings.visible')}</h4>
                                <ListBox
                                    items={visible}
                                    sortable
                                    multiselect
                                    activeIndexes={rightSelected}
                                    renderItemContent={this.renderItemContent}
                                    onChangeOrder={this.handleChangeOrder}
                                    onChangeSelection={this.handleChangeSelection.bind(this, 'right')}
                                    onDoubleClick={this.handleRemoveVisible}
                                />
                            </div>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={() => onSubmitForm(this.state.visible)}>{i18n('global.action.store')}</Button>
                    <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        );
    }
}

// columns - musí být seřazeno podle definovaného pořadí!!!

export default DataGridColumnsSettings;

