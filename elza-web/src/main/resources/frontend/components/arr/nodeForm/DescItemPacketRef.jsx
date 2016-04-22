
import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components';
import {connect} from 'react-redux'
import {decorateValue, decorateAutocompleteValue} from './DescItemUtils'
import {WebApi} from 'actions'

var DescItemPacketRef = class DescItemPacketRef extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('packetName', 'findType', 'focus', 'handleSearchChange', 'handleChange', 'renderPacket');

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

    renderPacket(item, isHighlighted, isSelected) {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return (
            <div className={cls} key={item.id}>
                {false && this.packetName(item)}
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
