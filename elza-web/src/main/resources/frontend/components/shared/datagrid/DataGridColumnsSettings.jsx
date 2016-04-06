/**
 * Komponenta výběru sloupců pro grid - s možností změny jejich pořadí.
 */

require ('./DataGridColumnsSettings.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, i18n} from 'components';
import {Modal, Button} from 'react-bootstrap';
import {getMapFromList} from 'stores/app/utils'

var DataGridColumnsSettings = class DataGridColumnsSettings extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChangeOrder', 'handleAddVisible', 'handleRemoveVisible', 'handleChangeSelection')

        var colsMap = getMapFromList(props.columns)
        const visible = []
        const available = []
        props.columns.forEach(col => {
            if (props.visibleColumns[col.id]) {
                visible.push(col)
            } else {
                available.push(col)
            }
        })

        this.state = {
            columns: props.columns,
            leftSelected: [],
            rightSelected: [],
            visible: visible,
            available: available,
        }
    }

    componentWillReceiveProps(nextProps) {
    }

    handleChangeOrder(from, to) {
        var visible = [...this.state.visible]
        visible.splice(to, 0, visible.splice(from, 1)[0]);
        this.setState({visible: visible})
    }

    handleAddVisible() {
        var {available, visible} = this.state
        const {leftSelected} = this.state

        var selectedMap = {}
        leftSelected.forEach(index => {
            selectedMap[index] = true
        })

        var newAvailable = []
        var newVisible = [...visible]

        available.forEach((item, index) => {
            if (selectedMap[index]) {
                newVisible.push(item)
            } else {
                newAvailable.push(item)
            }
        })

        this.setState({
            available: newAvailable,
            visible: newVisible,
            leftSelected: [],
        })
    }

    handleRemoveVisible() {
        const {rightSelected, available, visible} = this.state
        const {columns} = this.props

        var selectedMap = {}
        rightSelected.forEach(index => {
            selectedMap[index] = true
        })

        // Upravení seznamu visible
        const newVisible = []
        visible.forEach((item, index) => {
            if (!selectedMap[index]) {
                newVisible.push(item)
            }
        })

        // Vytvoření seřazeného seznamu available (newAvailable) podle pořadí, které je definované na vstupu jako columns
        const visibleMap = getMapFromList(newVisible)
        const newAvailable = []
        columns.forEach(col => {
            if (!visibleMap[col.id]) {
                newAvailable.push(col)
            }
        })

        // ---

        this.setState({
            available: newAvailable,
            visible: newVisible,
            rightSelected: [],
        })
    }

    handleChangeSelection(type, sel) {
        if (type === 'left') {
            this.setState({leftSelected: sel})
        } else if (type === 'right') {
            this.setState({rightSelected: sel})
        }
    }

    render() {
        const {onSubmitForm, onClose} = this.props
        const {available, visible} = this.state

        var cls = this.props.className ? 'datagrid-columns-settings-container ' + this.props.className : 'datagrid-columns-settings-container'
        return (
            <div>
                <Modal.Body>
                    <div className={cls} >
                        <div className='panels-container'>
                            <div className='left'>
                                <ListBox
                                    items={available}
                                    multiselect
                                    activeIndexes={this.state.leftSelected}
                                    renderItemContent={(item, isActive) => <div>{item.title}</div>}
                                    onChangeSelection={this.handleChangeSelection.bind(this, 'left')}
                                    onDoubleClick={this.handleAddVisible}
                                />
                            </div>
                            <div className='center'>
                                <Button onClick={this.handleAddVisible}>&gt;</Button>
                                <Button onClick={this.handleRemoveVisible}>&lt;</Button>
                            </div>
                            <div className='right'>
                                <ListBox
                                    items={visible}
                                    sortable
                                    multiselect
                                    activeIndexes={this.state.rightSelected}
                                    renderItemContent={(item, isActive) => <div>{item.title}</div>}
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
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = connect()(DataGridColumnsSettings);

