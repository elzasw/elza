
import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateValue, decorateAutocompleteValue} from './DescItemUtils.jsx'
import {WebApi} from 'actions/index.jsx';
import {indexById} from 'stores/app/utils.jsx';

var DescItemPacketRef = class DescItemPacketRef extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('packetName', 'findType', 'focus', 'handleSearchChange', 'handleChange', 'renderPacket', 'handleKeyUp');

        this.state = {packetTypes: []};
    }

    handleSearchChange(text) {
        const {fundId} = this.props

        WebApi.getPackets(fundId, text, 200)
            .then(json => {
                this.setState({
                    packetTypes: json
                })
            })
    }

    focus() {
        this.refs.focusEl.focus()
    }


    handleKeyUp(e){
        if (e.keyCode == 13 && this.state.packetTypes.length == 1){
            this.props.onChange(this.state.packetTypes[0]);
        }
    }

    renderPacket(item, isHighlighted, isSelected) {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

                // {false && this.packetName(item)}
        return (
            <div className={cls} key={item.id}>
                {item.storageNumber}
            </div>
        )
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

    packetName(packet) {
        var type = this.findType(packet.packetTypeId);

        if (type == null) {
            return packet.storageNumber;
        } else {
            return packet.storageNumber + " [" + type.name + "]";
        }
    }

    handleChange(id, valueObj) {
        this.props.onChange(valueObj);
    }

    render() {
        const {descItem, locked, packetTypes, packets, singleDescItemTypeEdit} = this.props;
        var value = descItem.packet ? descItem.packet : null;
        console.log(value);
        return (
            <div className='desc-item-value desc-item-value-parts'>
                {false && <select
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                        ref='focusEl'
                        value={descItem.value}
                        disabled={locked}
                        onChange={(e) => this.props.onChange(e.target.value)} >
                    <option key="novalue" />
                    {packets.map(packet => (
                            <option key={packet.id} value={packet.id}>{this.packetName(packet)}</option>
                    ))}
                </select>}


                <Autocomplete
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-packet'])}
                    ref='focusEl'
                    customFilter
                    onKeyUp={this.handleKeyUp}
                    value={value}
                    disabled={locked}
                    items={this.state.packetTypes}
                    onSearchChange={this.handleSearchChange}
                    onChange={this.handleChange}
                    renderItem={this.renderPacket}
                    getItemName={(item) => item ? item.storageNumber : ''}
                />

            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemPacketRef);
