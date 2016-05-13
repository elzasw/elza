/**
 * Správa obalů.
 */

require('./FundPackets.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FilterableListBox, Loading, AddPacketForm} from 'components/index.jsx';
import {DropdownButton, MenuItem,Input} from 'react-bootstrap'
import {fetchFundPacketsIfNeeded, fundPacketsFilterByText, fundPacketsChangeSelection, fundPacketsFilterByState,
    fundPacketsChangeSelctions, fundPacketsChangeState, fundPacketsCreate, fundPacketsChangeNumbers, fundPacketsDelete} from 'actions/arr/fundPackets.jsx'
import {getMapFromList, getSetFromIdsList} from 'stores/app/utils.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'

var FundPackets = class FundPackets extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelectionChange', 'handleTextSearch', 'handleFilterStateChange', 'handleChangeState',
            'handleChangeNumbers', 'handleDelete', 'handleAddOne', 'handleAddMany', 'handleCreatePacketFormSubmit',
            'handleChangePacketNumberSubmit', 'focus', 'focus');
    }

    componentDidMount() {
        const {versionId, fundId} = this.props
        this.dispatch(fetchFundPacketsIfNeeded(versionId, fundId))
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, fundId} = this.props
        this.dispatch(fetchFundPacketsIfNeeded(versionId, fundId))
    }

    handleSelectionChange(selectionType, ids, unselectedIds, type) {
        const {versionId, packets, selectedIds} = this.props

        console.log(selectionType, ids, unselectedIds, type)

        var newSelectedIds
        switch (type) {
            case "TOGGLE_ITEM":
                var s = getSetFromIdsList([...selectedIds, ...ids])
                unselectedIds.forEach(id => {
                    delete s[id]
                })
                newSelectedIds = Object.keys(s)
                break
            case "SELECT_ALL":
                newSelectedIds = Object.keys(getSetFromIdsList([...selectedIds, ...ids]))
                break
            case "UNSELECT_ALL":
                newSelectedIds = []
                break
        }
        console.log("NEW_SELECTION: " + newSelectedIds)
        this.dispatch(fundPacketsChangeSelection(versionId, newSelectedIds))
    }

    handleTextSearch(text) {
        const {versionId} = this.props
        this.dispatch(fundPacketsFilterByText(versionId, text))
    }

    handleFilterStateChange(e) {
        const {versionId} = this.props
        this.dispatch(fundPacketsFilterByState(versionId, e.target.value))
    }

    handleChangeState(state) {
        const {fundId, versionId, selectedIds} = this.props
        this.dispatch(fundPacketsChangeState(versionId, fundId, selectedIds, state))
    }

    handleChangeNumbers() {
        const {fundId, selectedIds} = this.props

        const form = <AddPacketForm
            initData={{count: selectedIds.length}}
            changeNumbers
            fundId={fundId}
            onSubmitForm={this.handleChangePacketNumberSubmit.bind(this, selectedIds)}
        />

        this.dispatch(modalDialogShow(this, i18n('arr.packet.title.add'), form));
    }

    handleDelete() {
        const {fundId, versionId, selectedIds} = this.props
        this.dispatch(fundPacketsDelete(versionId, fundId, selectedIds))
    }

    handleAddOne() {
        const {fundId} = this.props

        const form = <AddPacketForm
            initData={{}}
            createSingle
            fundId={fundId}
            onSubmitForm={this.handleCreatePacketFormSubmit.bind(this, "SINGLE")}
        />

        this.dispatch(modalDialogShow(this, i18n('arr.packet.title.add'), form));
    }

    handleAddMany() {
        const {fundId, selectedIds} = this.props

        const form = <AddPacketForm
            initData={{}}
            createMany
            fundId={fundId}
            onSubmitForm={this.handleCreatePacketFormSubmit.bind(this, "MORE")}
        />

        this.dispatch(modalDialogShow(this, i18n('arr.packet.title.add'), form));
    }

    handleCreatePacketFormSubmit(type, data) {
        const {fundId} = this.props
        this.dispatch(fundPacketsCreate(fundId, type, data))
    }

    handleChangePacketNumberSubmit(selectedIds, data) {
        const {fundId} = this.props
        this.dispatch(fundPacketsChangeNumbers(fundId, data, selectedIds))
    }
    
    focus() {
        this.refs.listBox.focus()
    }

    render() {
        const {versionId, packetTypes, filterState, filterText, fetched, packets, selectedIds} = this.props

        if (!fetched) {
            return <Loading/>
        }

        const packetTypesMap = getMapFromList(packetTypes.items)
        const items = packets.map(packet => {
            let name
            // if (typeof packet.packetTypeId !== 'undefined' && packet.packetTypeId !== null) {
            //     name = packet.storageNumber + " [" + packetTypesMap[packet.packetTypeId].name + "]"
            // } else {
            //     name = packet.storageNumber
            // }
            name = packet.storageNumber
            return {id: packet.id, name: name}
        })

        const altSearch = (
            <div className="state-filter">
                <Input type="select" _label={i18n('arr.fund.packets.state')} value={filterState} onChange={this.handleFilterStateChange}>
                    <option value="OPEN">{i18n('arr.fund.packets.state.open')}</option>
                    <option value="CLOSED">{i18n('arr.fund.packets.state.closed')}</option>
                    <option value="CANCELED">{i18n('arr.fund.packets.state.canceled')}</option>
                </Input>
            </div>
        )
        
        return (
            <div className='fund-packets'>
                <div className="actions-container">
                    <div className="actions">
                        <DropdownButton noCaret title={<div><Icon glyph='fa-plus' /> {i18n('arr.fund.packets.action.add')}</div>}>
                            <MenuItem onClick={this.handleAddOne} eventKey='changeNumbers'>{i18n('arr.fund.packets.action.add.single')}</MenuItem>
                            <MenuItem onClick={this.handleAddMany} eventKey='changeNumbers'>{i18n('arr.fund.packets.action.add.more')}</MenuItem>
                        </DropdownButton>
                        <DropdownButton noCaret disabled={selectedIds.length === 0} title={<div><Icon glyph='fa-edit' /> {i18n('arr.fund.packets.action.checkedItems')}</div>}>
                            {filterState !== "OPEN" && <MenuItem onClick={this.handleChangeState.bind(this, "OPEN")} eventKey='toOpen'>{i18n('arr.fund.packets.action.changeState.toOpen')}</MenuItem>}
                            {filterState !== "CLOSED" && <MenuItem onClick={this.handleChangeState.bind(this, "CLOSED")} eventKey='toClosed'>{i18n('arr.fund.packets.action.changeState.toClosed')}</MenuItem>}
                            {filterState !== "CANCELED" && <MenuItem onClick={this.handleChangeState.bind(this, "CANCELED")} eventKey='toCanceled'>{i18n('arr.fund.packets.action.changeState.toCanceled')}</MenuItem>}
                            <MenuItem divider />
                            <MenuItem onClick={this.handleChangeNumbers} eventKey='changeNumbers'>{i18n('arr.fund.packets.action.changeNumbers')}</MenuItem>
                            <MenuItem divider />
                            <MenuItem onClick={this.handleDelete} eventKey='delete'>{i18n('arr.fund.packets.action.delete')}</MenuItem>
                        </DropdownButton>
                    </div>
                </div>

                <FilterableListBox
                    ref="listBox"
                    items={items}
                    searchable
                    filterText={filterText}
                    supportInverseSelection={false}
                    selectedIds ={selectedIds}
                    altSearch={altSearch}
                    onChange={this.handleSelectionChange}
                    onSearch={this.handleTextSearch}
                />
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(FundPackets);
