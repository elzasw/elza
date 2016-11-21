require('./DescItemPacketRef.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateValue, decorateAutocompleteValue} from './DescItemUtils.jsx'
import {WebApi} from 'actions/index.jsx';
import {indexById} from 'stores/app/utils.jsx';
import {Button} from 'react-bootstrap';
import DescItemLabel from './DescItemLabel.jsx'
import PacketFormatter from 'components/arr/packets/PacketFormatter.jsx';

var DescItemPacketRef = class DescItemPacketRef extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'formatPacketName',
            'findType',
            'focus',
            'handleFocus',
            'handleBlur',
            'getPacketName',
            'handleSearchChange',
            'handleChange',
            'renderPacket',
            'handleKeyUp',
            'handleFundPackets',
            'handleCreatePacket');

        this.state = {packets: [], active: false};
    }

    componentWillMount() {
        const {packetTypes} = this.props;
        this.pf = new PacketFormatter(packetTypes);
    }
    handleFocus(){
        this.setState({active:true});
        this.props.onFocus && this.props.onFocus();
    }
    handleBlur(){
        this.setState({active:false});
        this.props.onBlur && this.props.onBlur();
    }

    handleSearchChange(text) {
        const {fundId} = this.props;
        WebApi.getPackets(fundId, text, 200)
            .then(json => {
                this.setState({
                    packets: json
                })
            })
    }

    focus() {
        this.refs.focusEl.focus();
    }


    handleKeyUp(e){
        if (e.keyCode == 13 && this.state.packets.length == 1){
            this.props.onChange(this.state.packets[0]);
        }
    }

    renderPacket(item, isHighlighted, isSelected) {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus';
        }
        if (isSelected) {
            cls += ' active'
        }

                // {false && this.packetName(item)}
        return (
            <div className={cls} key={item.id}>
                {this.formatPacketName(item)}
            </div>
        )
    }
    /**
     * Vrácení názvu obalu
     * Pokud má komponenta focus tak se v názvu nezobrazí typ obalu.
     * Pokud
     */
    getPacketName(packet){
        var name;
        if(this.state.active && packet)
        {
            name = packet.storageNumber;
        } else if(!this.state.active && packet) {
            name = this.formatPacketName(packet);
        } else {
            name = "";
        }
        return name;
    }

    findType(packetTypeId) {
        if (packetTypeId == null) {
            return null;
        }

        const {packetTypes} = this.props;
        for (var i = 0; i < packetTypes.items.length; i++) {
            if (packetTypeId == packetTypes.items[i].id) {
                return packetTypes.items[i];
            }
        }
        return null;
    }
    /**
     * Vrací název obalu formátovaný PacketFormatterem
     */
    formatPacketName(packet) {
        return this.pf.format(packet);
    }

    handleFundPackets() {
        if (this.props.onFundPackets) {
            this.refs.focusEl.closeMenu();
            this.props.onFundPackets();
        } else {
            console.warn("undefined onFundPackets");
        }
    }

    handleCreatePacket() {
        if (this.props.onCreatePacket) {
            this.refs.focusEl.closeMenu();
            this.props.onCreatePacket();
        } else {
            console.warn("undefined onCreatePacket");
        }
    }

    renderFooter() {
        const {refTables} = this.props;
        return (
            <div className="create-packet">
                <Button onClick={this.handleCreatePacket}><Icon glyph='fa-plus'/>{i18n('arr.fund.packets.action.add.single')}</Button>
                <Button onClick={this.handleFundPackets}>{i18n('arr.panel.title.packets')}</Button>
            </div>
        )
    }

    render() {
        const {descItem, onChange, onBlur, locked, packetTypes, packets, singleDescItemTypeEdit, readMode, cal} = this.props;
        var packet = descItem.packet ? descItem.packet : null;
        if (readMode) {
            let calValue = cal && packet == null ? i18n("subNodeForm.descItemType.calculable") : "";
            return (
                <DescItemLabel value={packet ? this.pf.format(packet) : calValue} cal={cal} />
            )
        }

        const footer = this.renderFooter();

        return (
            <div className='desc-item-value desc-item-value-parts'>
                {/**<select
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                        ref='focusEl'
                        value={descItem.value}
                        disabled={locked}
                        onChange={(e) => this.props.onChange(e.target.value)} >
                    <option key="novalue" />
                    {packets.map(packet => (
                            <option key={packet.id} value={packet.id}>{this.formatPacketName(packet)}</option>
                    ))}
                </select>*/}


                <Autocomplete
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-packet'])}
                    ref='focusEl'
                    customFilter
                    onKeyUp={this.handleKeyUp}
                    onFocus={this.handleFocus}
                    onBlur={this.handleBlur}
                    value={packet}
                    disabled={locked}
                    items={this.state.packets}
                    onSearchChange={this.handleSearchChange}
                    onChange={onChange}
                    onBlur={onBlur}
                    renderItem={this.renderPacket}
                    getItemName={(item) => this.getPacketName(item)}
                    footer={footer}
                />

            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemPacketRef);
