import PropTypes from 'prop-types';
import React from 'react';
import {AbstractReactComponent, Icon} from 'components/shared';
import classNames from 'classnames';

import './RegistryListItem.scss';

/**
 * Komponenta item listu osob
 */
class RegistryListItem extends AbstractReactComponent {
    static propTypes = {
        onClick: PropTypes.func,
        //partyType: PropTypes.object.isRequired,
        relationTypesForClass: PropTypes.object,
        eidTypes: PropTypes.object.isRequired,
        name: PropTypes.string.isRequired,
        relations: PropTypes.array,
        invalid: PropTypes.bool,
    };

    getDisplayIds = () => {
        const {eidTypes} = this.props;
        const eids = this.props.externalIds;
        if (!eids || eids.length === 0) {
            return [this.props.id];
        }

        let eidArr = [];
        eids.forEach(eid => {
            const typeId = eid.typeId;
            const eidTypeName = eidTypes && eidTypes[typeId] ? eidTypes[typeId].name : 'eid_type_name-' + typeId;
            eidArr.push(eidTypeName + ':' + eid.value);
        });
        return eidArr;
    };

    getApTypeNames = () => {
        const type = this.props.apTypeIdMap[this.props.typeId];

        let names = [type.name];
        if (type.parents) {
            type.parents.forEach(name => names.push(name));
        }
        return names;
    };

    render() {
        const {className, isActive, id, invalid, scopeName, name} = this.props;

        const cls = classNames(className, 'registry-list-item', {
            active: isActive,
            invalid: invalid,
        });

        const typeNames = this.getApTypeNames().join(' | ');
        const displayId = this.getDisplayIds().join(', ');

        return (
            <div key={'record-id-' + id} className={cls}>
                <div>
                    <Icon glyph="fa-file-o" />
                    <span className="name" title={name}>
                        {name}
                    </span>
                </div>
                <div>
                    <span className="types" title={typeNames}>
                        {typeNames}
                    </span>
                    <span className="ids" title={displayId}>
                        {displayId}
                    </span>
                </div>
                <div>{scopeName}</div>
            </div>
        );
    }
}

export default RegistryListItem;
