import {PartType} from "../api/generated/model";
import {ApItemVO} from "../api/ApItemVO";

export function compareItems(a: ApItemVO, b: ApItemVO): number {
    if (a.position && b.position) {
        return a.position - b.position;
    } else if (a.position && !b.position) {
        return -1;
    } else if (!a.position && b.position) {
        return 1;
    } else {
        return 0;
    }
}

export function sortItems(partType: PartType, items: ApItemVO[]): ApItemVO[] {
    return [...items].sort((a, b) => {
        return compareItems(a, b);
    });
}

