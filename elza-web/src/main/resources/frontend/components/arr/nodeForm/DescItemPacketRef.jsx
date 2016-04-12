
import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'

var DescItemPacketRef = class DescItemPacketRef extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('packetName', 'findType', 'focus');
    }

    focus() {
        this.refs.focusEl.focus()
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

    render() {
        const {descItem, locked, packetTypes, packets, singleDescItemTypeEdit} = this.props;

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <select
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                        ref='focusEl'
                        value={descItem.value}
                        disabled={locked}
                        onChange={(e) => this.props.onChange(e.target.value)} >
                    <option key="novalue" />
                    {packets.map(packet => (
                            <option key={packet.id} value={packet.id}>{this.packetName(packet)}</option>
                    ))}
                </select>
                {!locked && !singleDescItemTypeEdit && <div className='desc-item-type-actions'><NoFocusButton onClick={this.props.onCreatePacket} title={i18n('subNodeForm.addDescItem')}><Icon glyph="fa-plus" /></NoFocusButton></div>}
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemPacketRef);
