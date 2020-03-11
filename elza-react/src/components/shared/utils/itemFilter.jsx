// flatten data in tree structure
// converts:
// let itemsExample = [{
//     id:1,
//     name:"a",
//     children: [{
//         id:2,
//         name:"a1"
//     },{
//         id:3,
//         name:"a2",
//         children: [{
//             id:4,
//             name:"a2a"
//         }]
//     }]
// },{
//     id:5,
//     name:"b"
// }];
//
// // to:
// let flatItemsExample = {
//     1:{
//         id:1,
//         name:"a",
//         depth:0,
//         parent: null
//     },
//     2:{
//         id:2,
//         name:"a1",
//         depth:1,
//         parent:1
//     },
//     3:{
//         id:3,
//         name:"a2",
//         depth:1,
//         parent:1
//     },
//     4:{
//         id:4,
//         name:"a2a",
//         depth:2,
//         parent:3
//     },
//     5:{
//         id:5,
//         name:"b",
//         depth:0,
//         parent: null
//     },
//     ids: [1,2,3,4,5]
// }

let _getItemId = defaultGetItemId;

export default function flattenItems(items, props) {
    const {getItemId} = props;
    _getItemId = getItemId;

    const flatTreeItems = getFlatTree(items);
    return flatTreeItems;
};

function defaultGetItemId(item){
    return item.id;
}

// global _items variable
let _items;

function getFlatTree(items) {
    // reset _items
    _items = {ids:[]};
    items.forEach(node => _getFlatSubTree(node, 0));
    return _items;
};

function _getFlatSubTree(node, depth, parentId=null) {
    node = {...node};
    node.parent = parentId;
    node.depth = depth;
    let nodeId = _getItemId(node);

    if(!nodeId && nodeId !== 0){
        nodeId = "noid";
    }

    _items[nodeId] = node;
    _items.ids.push(nodeId);

    // go through each child recursively
    node.children && node.children.forEach(child => _getFlatSubTree(child, depth + 1, node.id));
};

export function cleanItem(item){
    delete item.depth;
    delete item.parent;
}

function nameContains(filterText, itemName){
    const caseSensitive = false;

    if(!caseSensitive){
        itemName = itemName.toLowerCase();
        filterText = filterText.toLowerCase();
    }

    return itemName.indexOf(filterText) >= 0;
}

function defaultAllowSelectItem(item){
    return true;
}

function defaultGetItemName(item){
    return item.name;
}

export function filterItems(filterText = "", items, props){
    let newItems = {ids:[]}, currentDepth = 0;
    let parents = [];
    let _itemAdded = false; // indicates whether the previous item was added or not
    let _empty = true; // indicates whether any one of the items was selected
    let filteredCount = 0;
    const filterCondition = props.filterCondition || nameContains;
    const allowSelectItem = props.allowSelectItem || defaultAllowSelectItem;
    const getItemName = props.getItemName || defaultGetItemName;

    for(let i = 0; i < items.ids.length; i++){
        const itemId = items.ids[i];
        const item = items[itemId];

        // when depth increases, add previous item as a potential parent,
        // but only when it wasn't added already
        if(item.depth > currentDepth && i > 0){
            currentDepth = item.depth;
            const prevItemId = items.ids[i-1];
            const prevItem = items[prevItemId];

            if(!_itemAdded){
                parents.push(prevItem);
            }
        }

        // when depth decreases, remove previously added potential parents,
        // that are no longer viable (none of their children were selected)
        if(item.depth < currentDepth){
            let newParents = [];
            for(let p = 0; p < parents.length; p++){
                const parent = parents[p];
                if(parent.depth < item.depth){
                    newParents.push(parent);
                }
            }
            parents = newParents;
            currentDepth = item.depth;
        }

        // reset itemAdded
        _itemAdded = false;

        // checks if the item matches the specified filter condition
        // and if it can be selected
        if(filterCondition(filterText, getItemName(item)) && allowSelectItem(item)){
            // when item matches the condition, add all current potential
            // parents as proper items before adding the item itself
            for(let p = 0; p < parents.length; p++){
                const parent = parents[p];
                const parentId = parent.id;
                newItems[parentId] = parent;
                newItems.ids.push(parentId);
            }
            // when no item has been added yet, selects current item as first item
            if(_empty){
                newItems.firstItemId = item.id;
            }
            parents = [];
            newItems[itemId] = item;
            newItems.ids.push(itemId);
            filteredCount++;
            _itemAdded = true;
            _empty = false;
        }
    }
    newItems.filteredCount = filteredCount;

    return newItems;
}


