import React, { FC } from 'react';
// import {CodelistState} from '../../shared/reducers/codelist/CodelistReducer';
import { connect } from 'react-redux';
import { Bindings } from '../../../../types';
import { AppState } from '../../../../typings/store';
import './DetailItem.scss';
import DetailItemContent from './DetailItemContent';
import { RevisionDisplay, RevisionItem } from '../../revision';
import {SyncIcon} from "../sync-icon";
import {SyncState} from "../../../../api/SyncState";
import {Button} from '../../../ui';
import {ApItemCoordinatesClass, itemValue} from "../../../../utils/ItemInfo";
import CrossTabHelper, {CrossTabEventType, getThisLayout} from "../../../CrossTabHelper";
import Icon from "../../../shared/icon/Icon";
import i18n from 'components/i18n';

interface Props extends ReturnType<typeof mapStateToProps> {
    bindings?: Bindings;
    items: RevisionItem[];
    globalEntity: boolean;
    typeId?: number;
    isPartModified?: boolean | undefined;
    revision?: boolean;
    select: boolean;
}

const DetailMultipleItem: FC<Props> = ({
    items = [],
    globalEntity,
    descItemTypesMap,
    bindings,
    typeId,
    isPartModified,
    revision,
    select,
}) => {
    const itemType = typeId !== undefined ? descItemTypesMap[typeId] : undefined;
    const itemTypeName = itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${typeId}`;

    const isValueModified = (item?: any, updatedItem?: any) => {
        if(item && !updatedItem) {return false};
        if(!item && updatedItem) {return true};
        return item?.value !== updatedItem?.value || item?.specId !== updatedItem?.specId;
    }

    const isValueNew = (item?: any, updatedItem?: any) => {
        return (!item?.value && !item?.specId)
            && (updatedItem?.value != undefined || updatedItem?.specId != undefined);
    }

    const showInMap = (polygon) => {
        const thisLayout = getThisLayout();

        if (thisLayout) {
            CrossTabHelper.sendEvent(thisLayout, {type: CrossTabEventType.SHOW_IN_MAP, data: polygon});
        }
    };

    return (
        <div className="detail-item">
            <div className="detail-cell header">
                {itemTypeName}
            </div>
            <div className="detail-cell content">
                <div style={{display: "flex"}}>
                    <div style={{}}>
                        {items.map(({item, updatedItem}, index) => {
                            const isDeleted = isPartModified ? updatedItem == null : false;
                            const isNew = isValueNew(item, updatedItem);
                            const isModified = isValueModified(item, updatedItem);

                            return <div style={{display: "flex", alignItems: "center"}}>
                                <RevisionDisplay
                                    valuesEqual={!isModified}
                                    isDeleted={isDeleted}
                                    isNew={isNew}
                                    renderPrevValue={() => {
                                        return item ? <DetailItemContent
                                            select={select}
                                            item={item}
                                            key={index}
                                            globalEntity={globalEntity}
                                            bindings={!isModified ? bindings : undefined}
                                            revision={revision}
                                        /> : "no prev"
                                    }}
                                    renderValue={() => {
                                        return updatedItem ? <DetailItemContent
                                            select={select}
                                            item={updatedItem}
                                            key={index}
                                            bindings={bindings}
                                            globalEntity={globalEntity}
                                            revision={revision}
                                        /> : "no current"
                                    }}
                                >
                                </RevisionDisplay>
                                <div className="actions">
                                    {(revision) && (
                                        <SyncIcon
                                            syncState={
                                                !isModified && !isDeleted ?
                                                    SyncState.SYNC_OK :
                                                    SyncState.LOCAL_CHANGE
                                            }
                                        />
                                    )}
                                    {item?.['@class'] === ApItemCoordinatesClass && <Button className={'mb-1'} onClick={() => showInMap(itemValue(item))} title={i18n('global.action.showInMap')} variant={'action' as any}>
                                        <Icon glyph={'fa-map'} />
                                    </Button>}
                                </div>
                        </div>
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
};

const mapStateToProps = (state: AppState) => ({
    descItemTypesMap: state.refTables.descItemTypes.itemsMap || {},
});

export default connect(mapStateToProps)(DetailMultipleItem);
