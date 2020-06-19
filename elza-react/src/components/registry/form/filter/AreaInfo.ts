import {Area} from "../../../../api/Area";

export function getValues(): Area[] {
    return [Area.ALLNAMES, Area.ALLPARTS, Area.PREFERNAMES]
}

export function getName(area: Area): string {
    switch (area) {
        case Area.PREFERNAMES:
            return 'Pouze preferovaná označení';
        case Area.ALLPARTS:
            return 'Všechny části popisu';
        case Area.ALLNAMES:
            return 'Všechna označení';
        case Area.ENTITYCODE:
            return 'Podle kódu entity';
        default:
            return 'Neznámá oblast ' + area;
    }
}
