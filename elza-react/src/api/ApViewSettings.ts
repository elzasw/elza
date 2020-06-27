
export interface PartOrder {
    code: string;
}

export interface ItemType {
    code: string;
    position?: number;
    width: number;
    partType?: string;
    geoSearchItemType?: string;
}

export interface ApViewSettings {
    partsOrder: PartOrder[];
    itemTypes: ItemType[];
}
