import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {PARTY_TYPE_CODES, RELATION_CLASS_CODES} from 'actions/party/party.jsx'
import classNames from 'classnames';

import './RegistryListItem.less';

/**
 * Komponenta item listu osob
 */
class RegistryListItem extends AbstractReactComponent {

    static PropTypes = {
        onClick: React.PropTypes.func,
        partyType: React.PropTypes.object.isRequired,
        relationTypesForClass: React.PropTypes.object,
        eidTypes: React.PropTypes.object.isRequired,
        record: React.PropTypes.object.isRequired,
        relations: React.PropTypes.array,
        invalid: React.PropTypes.bool
    };

    getDisplayIds = () => {
        const {eidTypes} = this.props;
		const eids = this.props.externalIds;
		if (!eids || eids.length == 0) {
			return [this.props.id];
		}

		let eidArr = [];
		eids.forEach(eid => {
            const typeId = eid.typeId;
            const eidTypeName = eidTypes && eidTypes[typeId] ? eidTypes[typeId].name : "eid_type_name-" + typeId;
			eidArr.push(eidTypeName + ":" + eid.value);
		});
		return eidArr;
    }

	getApTypeNames = () => {
        const type = this.props.apTypeIdMap[this.props.typeId];

		let names = [type.name];
        if (type.parents) {
			type.parents.forEach(name => names.push(name));
		}
		return names;
    }

    render() {
        const {className, isActive, id, record, invalid, scopeName} = this.props;

        const cls = classNames(className, 'registry-list-item', {
            active: isActive,
            invalid: invalid
        });

		const typeNames = this.getApTypeNames().join(' | ');
		const displayId = this.getDisplayIds().join(', ');

		return <div key={'record-id-' + id} className={cls}>
			<div>
				<Icon glyph='fa-file-o' />
				<span className="name" title={record}>{record}</span>
			</div>
			<div>
				<span className="types" title={typeNames}>{typeNames}</span>
				<span className="ids" title={displayId}>{displayId}</span>
			</div>
            <div>
                {scopeName}
            </div>
		</div>;
    };
}

export default RegistryListItem;
