import PropTypes from 'prop-types';
import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {PARTY_TYPE_CODES, RELATION_CLASS_CODES} from '../../constants.tsx'
import classNames from 'classnames';

import './PartyListItem.less';

/**
 * Komponenta item listu osob
 */
class PartyListItem extends AbstractReactComponent {

    static PropTypes = {
        onClick: PropTypes.func,
        partyType: PropTypes.object.isRequired,
        relationTypesForClass: PropTypes.object,
        record: PropTypes.object.isRequired,
        relations: PropTypes.array,
        partyNames: PropTypes.array,
    };

    static partyIconByPartyTypeCode = (code) => {
        switch(code) {
            case PARTY_TYPE_CODES.PERSON:
                return 'fa-user';
            case PARTY_TYPE_CODES.GROUP_PARTY:
                return 'fa-building';
            case PARTY_TYPE_CODES.EVENT:
                return 'fa-calendar';
            case PARTY_TYPE_CODES.DYNASTY:
                return 'fa-shield';
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
        const {id, relationTypesForClass, partyType, relations, accessPoint, accessPoint: {invalid}, partyNames, className, ...otherProps} = this.props;

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

        let preferredName = null;
        if (partyNames && partyNames.length > 0) {
            const preferredNames = partyNames.filter(i => i.prefferedName);
            if (preferredNames.length > 0) {
                if (preferredNames.length > 1) {
                    console.warn("2 preferred names in party");
                }

                preferredName = preferredNames[0];
            }
        }

        return <div className={classNames('party-list-item', className, {
            invalid
        })} {...otherProps}>
            <div>
                <Icon glyph={icon} />
                <span className="name">{preferredName && preferredName.displayName}</span>
            </div>
            <div>
                <span className="date">{datation}</span>
                {accessPoint.externalId && accessPoint.externalSystem && accessPoint.externalSystem.name && <span className="description">{accessPoint.externalSystem.name + ':' + accessPoint.externalId}</span>}
                {accessPoint.externalId && (!accessPoint.externalSystem || !accessPoint.externalSystem.name) && <span className="description">{'UNKNOWN:' + accessPoint.externalId}</span>}
                {!accessPoint.externalId && <span className="description">{id}</span>}
            </div>
        </div>
    };
}

export default PartyListItem;
