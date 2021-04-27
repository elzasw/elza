import {AeState} from "../../../../api/AeState";

export function getValues(): AeState[] {
    return [AeState.APSNEW, AeState.APSAPPROVED, AeState.APSREPLACED, AeState.APSINVALID]
}

export function getName(state: AeState): string {
    switch (state) {
        case AeState.APSAPPROVED:
            return 'schválená';
        case AeState.APSINVALID:
            return 'zneplatněná';
        case AeState.APSNEW:
            return 'nová';
        case AeState.APSREPLACED:
            return 'nahrazená';
        default:
            return 'neznámý stav ' + state;
    }
}
