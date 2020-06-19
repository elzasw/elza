import {AeState} from "../../../../api/AeState";

export function getValues(): AeState[] {
    return [AeState.APSNEW, AeState.APSAPPROVED, AeState.APSREPLACED, AeState.APSINVALID]
}

export function getName(state: AeState): string {
    switch (state) {
        case AeState.APSAPPROVED:
            return 'Schváleno';
        case AeState.APSINVALID:
            return 'Zneplatněný';
        case AeState.APSNEW:
            return 'Nový';
        case AeState.APSREPLACED:
            return 'Nahrazený';
        default:
            return 'Neznámý stav ' + state;
    }
}
