import { fundSelectSubNode } from 'actions/arr/node';
import { WebApi } from 'actions/index';
import { createFundRoot } from 'components/arr/ArrUtils';
import { LazyListBox } from 'components/shared';
import { FC, useRef } from 'react';
import { Node } from 'typings/store';
import './DiscrepanciesList.scss';
import { DiscrepanciesListProps, DiscrepanciesResponse, DiscrepancyItem } from './types';
import { useThunkDispatch } from 'utils/hooks';


export const DiscrepanciesList:FC<DiscrepanciesListProps> = ({
    activeFund,
}) => {
    const refFundErrors = useRef(null);
    const dispatch = useThunkDispatch();

    let activeNode:Node | null = null;
    if (activeFund.nodes.activeIndex != null) {
        activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
    }

    const renderFundErrorItem = (discrepancyItem:DiscrepancyItem) => {
        if (discrepancyItem) {
            return <div>{discrepancyItem.name}</div>;
        } else {
            return '...';
        }
    }

    const handleSelectErrorNode = (discrepancyItem:DiscrepancyItem) => {
        if (discrepancyItem.parentNode === null) {
            discrepancyItem.parentNode = createFundRoot(activeFund);
        }
        dispatch(fundSelectSubNode(activeFund.versionId, discrepancyItem.id, discrepancyItem.parentNode));
    }

    const getItems = (fromIndex:number, toIndex:number) => {
        return WebApi.getValidationItems(activeFund.versionId, fromIndex, toIndex) as Promise<DiscrepanciesResponse>;
    }

    return (
        <div className="errors-listbox-container">
            <LazyListBox
                ref={refFundErrors}
                getItems={getItems}
                renderItemContent={renderFundErrorItem}
                selectedItem={activeNode ? activeNode.selectedSubNodeId : null}
                itemHeight={25} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                onSelect={handleSelectErrorNode}
                />
        </div>
    );
}
