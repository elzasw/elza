import { ApItemVO } from 'api/ApItemVO';
import { ApPartVO } from 'api/ApPartVO';
import { RevisionItem, RevisionPart } from './types';

export const getRevisionParts = (parts: ApPartVO[] = [], updatedParts: ApPartVO[] = []) => {
    const revisionItems: RevisionPart[] = [];

    // add updated items
    parts.forEach((part)=>{
        const updatedPart = updatedParts.find((updatedPart) => updatedPart.id === part.id);
        revisionItems.push({part, updatedPart});
    })

    // add newly added items
    updatedParts.forEach((updatedPart)=>{
        const item = parts.find((item)=> item.id === updatedPart.id)
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
        const updatedItem = updatedItems.find((updatedItem) => updatedItem.id === item.id);
        revisionItems.push({item, updatedItem});
    })

    // add potentially added items
    updatedItems.forEach((updatedItem)=>{
        const item = items.find((item)=> item.id === updatedItem.id)
        if(!item){
            revisionItems.push({updatedItem});
        }
    })

    return revisionItems;
}
