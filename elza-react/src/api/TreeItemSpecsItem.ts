import {TreeItemSpecsItemType} from "./TreeItemSpecsItemType";

/**
 * VO datového typu.
 */
export interface TreeItemSpecsItem {
    type: TreeItemSpecsItemType;
    specId: number;
    name: string;
    children: TreeItemSpecsItem[];
}
