import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {PARTY_TYPE_CODES, RELATION_CLASS_CODES} from 'constants.jsx'
import classNames from 'classnames';

import './PartyListItem.less';

/**
 * Komponenta item listu osob
 */
class PartyListItem extends AbstractReactComponent {

    static PropTypes = {
        onClick: React.PropTypes.func,
        partyType: React.PropTypes.object.isRequired,
        relationTypesForClass: React.PropTypes.object,
        record: React.PropTypes.object.isRequired,
        relations: React.PropTypes.array,
    };

    static partyIconByPartyTypeCode = (code) => {
        switch(code) {
            case PARTY_TYPE_CODES.PERSON:
                return 'fa-user';
                break;
            case PARTY_TYPE_CODES.GROUP_PARTY:
                return 'fa-building';
                break;
            case PARTY_TYPE_CODES.EVENT:
                return 'fa-calendar';
                break;
            case PARTY_TYPE_CODES.DYNASTY:
                return 'fa-shield';
                break;
            default:
                return 'fa-times';
        }
    };

    getDatationRelationString = (array, firstChar) => {
        let datation = "";
        let first = true;
        for (let birth of array) {
            if (first) {
                datation += firstChar;
                first = false;
            } else {
                datation += ',';
            }

            if (birth.from && birth.to) {
                datation += birth.from.value + "..." + birth.to.value;
            } else {
                if (birth.from) {
                    datation += birth.from.value
                } else if (birth.to) {
                    datation += birth.to.value;
                }
            }
        }
        return datation
    };



    render() {
        const {id, relationTypesForClass, partyType, relations, record, className, ...otherProps} = this.props;

        let icon = PartyListItem.partyIconByPartyTypeCode(partyType.code);
        const birth = relations == null || relationTypesForClass == false ? "" : this.getDatationRelationString(relations.filter(i => (relationTypesForClass[RELATION_CLASS_CODES.BIRTH].indexOf(i.relationTypeId) !== -1) && ((i.from && i.from.value) || (i.to && i.to.value))),'*');
        const extinction = relations == null || relationTypesForClass == false ? "" : this.getDatationRelationString(relations.filter(i => (relationTypesForClass[RELATION_CLASS_CODES.EXTINCTION].indexOf(i.relationTypeId) !== -1) && ((i.from && i.from.value) || (i.to && i.to.value))),'†');
        let datation = null;
        if (birth != "" && extinction != "") {
            datation = birth + ", " + extinction
        } else if (birth != "") {
            datation = birth;
        } else if (extinction != "") {
            datation = extinction;
        }

        return <div className={classNames('party-list-item', className)}>
            <div>
                <Icon glyph={icon} />
                <span className="name">{record.record}</span>
            </div>
            <div>
                <span className="date">{datation}</span>
                {record.externalId && record.externalSystem && record.externalSystem.name && <span className="description">{record.externalSystem.name + ':' + record.externalId}</span>}
                {record.externalId && (!record.externalSystem || !record.externalSystem.name) && <span className="description">{'UNKNOWN:' + record.externalId}</span>}
                {!record.externalId && <span className="description">{id}</span>}
            </div>
        </div>
    };
}

export default PartyListItem;
