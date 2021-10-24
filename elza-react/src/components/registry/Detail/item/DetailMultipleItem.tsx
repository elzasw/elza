import React, { FC } from 'react';
// import {CodelistState} from '../../shared/reducers/codelist/CodelistReducer';
import { connect } from 'react-redux';
import { ApItemVO } from '../../../../api/ApItemVO';
import { Bindings } from '../../../../types';
import { AppState } from '../../../../typings/store';
import './DetailItem.scss';
import DetailItemContent from './DetailItemContent';
import { RevisionDisplay } from '../../revision';

interface Props extends ReturnType<typeof mapStateToProps> {
    bindings?: Bindings;
    items: ApItemVO[];
    globalEntity: boolean;
}

const DetailMultipleItem: FC<Props> = ({items, globalEntity, descItemTypesMap, bindings}) => {
    const typeId = items.length > 0 ? items[0].typeId : undefined;
    const itemType = typeId !== undefined ? descItemTypesMap[typeId] : undefined;
    const itemTypeName = itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${typeId}`;
    // console.log(itemType?.name, typeId);
    const areValuesEqual = (value: ApItemVO, prevValue: ApItemVO) => {
        return value.specId === prevValue.specId
    }

    return (
        <div className="detail-item">
            <div className="detail-cell header">
                {itemTypeName}
            </div>
            <div className="detail-cell content">
                <div style={{display: "flex"}}>
                    <div style={{}}>
                        {items.map((item, index) => {
                            return <RevisionDisplay 
                                valuesEqual={areValuesEqual(item, {...item, specId: undefined})}
                                renderPrevValue={() => {
                                    return <DetailItemContent 
                                        item={item} 
                                        key={index}
                                        globalEntity={globalEntity} 
                                        />
                                }}
                                renderValue={() => {
                                    return <DetailItemContent 
                                        item={item} 
                                        key={index}
                                        bindings={bindings} 
                                        globalEntity={globalEntity} 
                                        />
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
