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
        record: React.PropTypes.object.isRequired,
        relations: React.PropTypes.array,
        invalid: React.PropTypes.bool
    };

    getRecordId = (data) => {
        if(data.externalId) {
            if(data.externalSystem && data.externalSystem.name){
                return data.externalSystem.name + ':' + data.externalId;
            } else {
                return 'UNKNOWN:' + data.externalId;
            }
        } else  {
            return data.id;
        }
    }

    render() {
        const {className, parents, typesToRoot, isActive, hierarchical, id, record, registryParentId, registryTypesId, externalId, externalSystem, invalid, ...otherProps} = this.props;

        const parentsShown = [];
        const parentsTypeShown = [];
        if (parents && parents.length > 0) {
            parents.map((val) => {
                parentsShown.push(val.id);
            });
        }

        var data = {
            externalId:externalId,
            externalSystem:externalSystem,
            id:id
        };

        if (typesToRoot) {
            typesToRoot.map((val) => {
                parentsTypeShown.push(val.id);
            });
        }

        //let doubleClick = this.handleDoubleClick.bind(this, item);
        let iconName = 'fa-folder';
        let clsItem = 'registry-list-icon-record';

        if (hierarchical === false) {
            iconName = 'fa-file-o';
            clsItem = 'registry-list-icon-list';
            //doubleClick = false;
        }

        const doubleClick = false;

        const cls = classNames(className, 'registry-list-item', {
            active: isActive,
            invalid: invalid
        });


        // výsledky z vyhledávání
        if (!registryParentId) {
            const path = [];
            if (parents) {
                parents.map((val) => {
                    if (parentsShown.indexOf(val.id) === -1) {
                        path.push(val.name);
                    }
                });
            }

            if (typesToRoot) {
                typesToRoot.map((val) => {
                    if (registryTypesId !== val.id) {
                        path.push(val.name);
                    }
                });
            }

            return <div key={'record-id-' + id} title={path} className={cls} onDoubleClick={doubleClick}>
                <div>
                    <Icon glyph={iconName} />
                    <span className="name">{record}</span>
                </div>
                <div>
                    <span className="path date">{path.join(' | ')}</span>
                    <span className="description">{this.getRecordId(data)}</span>
                </div>
            </div>;
        }  else {
            // jednořádkový výsledek
            return <div key={'record-id-' + id} className={cls} onDoubleClick={doubleClick} {...otherProps}>
                <div><Icon glyph={iconName} key={id} /></div>
                <div key={'record-' + id + '-name'} title={record} className={clsItem}>{record}</div>
            </div>;
        }
    };
}

export default RegistryListItem;
