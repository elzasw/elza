import { ApItemVO } from 'api/ApItemVO';
import { ApPartVO } from 'api/ApPartVO';
import { RevisionItem, RevisionPart } from './types';

export const getRevisionParts = (parts: ApPartVO[] = [], updatedParts: ApPartVO[] = []) => {
    const revisionItems: RevisionPart[] = [];

    // add updated items
    parts.forEach((part)=>{
        // nalezeni odpovidajiciho partu v updatedParts
        const updatedPart = updatedParts.find((updatedPart) => updatedPart.origPartId === part.id);

        if( (updatedPart?.items && updatedPart.items.length > 0) ||
            (updatedPart?.value && updatedPart?.value!=part?.value) ) {
            revisionItems.push({part, updatedPart});
        } else {
            revisionItems.push({part});
        }
    })

    // add newly added items
    updatedParts.forEach((updatedPart)=>{
        const item = parts.find((item)=> item.id === updatedPart.origPartId)
        if(!item){
            revisionItems.push({updatedPart});
        }
    })

    return revisionItems;
}

export const getRevisionItems = (items: ApItemVO[] = [], updatedItems: ApItemVO[] = []) => {
    const revisionItems: RevisionItem[] = [];

    // add updated items
    items.forEach((item)=>{
        const updatedItem = updatedItems.find((updatedItem) => updatedItem.origObjectId === item.objectId);
        revisionItems.push({
            item, 
            updatedItem: updatedItem || item,
            typeId: item.typeId,
            '@class': item["@class"],
        });
    })

    // add potentially added items
    updatedItems.forEach((updatedItem)=>{
        const item = items.find((item)=> item.objectId === updatedItem.origObjectId)
        if(!item){
            revisionItems.push({
                updatedItem,
                typeId: updatedItem.typeId,
                '@class': updatedItem["@class"],
            });
        }
    })

    return revisionItems;
}
