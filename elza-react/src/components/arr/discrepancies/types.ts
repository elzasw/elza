import { Fund, NodeBase } from 'typings/store';

export interface DiscrepanciesListProps {
    activeFund: Fund;
}

export interface DiscrepanciesResponse {
    count: number;
    items: DiscrepancyItem[];
}

export interface DiscrepancyItem {
    id: number;
    name: string;
    parentNode: NodeBase | null;
}
