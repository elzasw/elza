import {ApSearchArea} from 'elza-api';

export function getValues(): ApSearchArea[] {
    return [ApSearchArea.AllNames, ApSearchArea.AllParts, ApSearchArea.PreferNames]
}

export function getName(area: ApSearchArea): string {
    switch (area) {
        case ApSearchArea.PreferNames:
            return 'Pouze preferovaná označení';
        case ApSearchArea.AllParts:
            return 'Všechny části popisu';
        case ApSearchArea.AllNames:
            return 'Všechna označení';
        case ApSearchArea.EntityCode:
            return 'Podle kódu entity';
        default:
            return 'Neznámá oblast ' + area;
    }
}
