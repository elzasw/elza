import React, { FC } from 'react';
// import {CodelistState} from '../../shared/reducers/codelist/CodelistReducer';
import { connect } from 'react-redux';
import { Bindings } from '../../../../types';
import { AppState } from '../../../../typings/store';
import './DetailItem.scss';
import DetailItemContent from './DetailItemContent';
import { RevisionDisplay, RevisionItem } from '../../revision';

interface Props extends ReturnType<typeof mapStateToProps> {
    bindings?: Bindings;
    items: RevisionItem[];
    globalEntity: boolean;
    typeId?: number;
}

const DetailMultipleItem: FC<Props> = ({
    items = [], 
    globalEntity, 
    descItemTypesMap, 
    bindings,
    typeId,
}) => {
    const itemType = typeId !== undefined ? descItemTypesMap[typeId] : undefined;
    const itemTypeName = itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${typeId}`;

    const isValueModified = (item?: any, updatedItem?: any) => {
        if(item && !updatedItem) {return false};
        if(!item && updatedItem) {return true};
        return item?.value !== updatedItem?.value || item?.specId !== updatedItem?.specId;
    }

    const isValueEmpty = (item?:any) => {
        return item.value === null && item.specId === null;
    }

    const isValueNew = (item?: any, updatedItem?: any) => {
        return (!item?.value && !item?.specId) 
            && (updatedItem?.value != undefined || updatedItem?.specId != undefined);
    }

    return (
        <div className="detail-item">
            <div className="detail-cell header">
                {itemTypeName}
            </div>
            <div className="detail-cell content">
                <div style={{display: "flex"}}>
                    <div style={{}}>
                        {items.map(({item, updatedItem}, index) => {
                            const isDeleted = updatedItem ? isValueEmpty(updatedItem) : false;
                            const isNew = isValueNew(item, updatedItem);
                            const isModified = isValueModified(item, updatedItem);
                            return <RevisionDisplay 
                                valuesEqual={!isModified}
                                isDeleted={isDeleted}
                                isNew={isNew}
                                renderPrevValue={() => {
                                    return item ? <DetailItemContent 
                                        item={item} 
                                        key={index}
                                        globalEntity={globalEntity} 
                                        bindings={!isModified ? bindings : undefined} 
                                        /> : "no prev"
                                }}
                                renderValue={() => {
                                    return updatedItem ? <DetailItemContent 
                                        item={updatedItem} 
                                        key={index}
                                        bindings={bindings} 
                                        globalEntity={globalEntity} 
                                        /> : "no current"
                                }} 
                            >
                            </RevisionDisplay>
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
