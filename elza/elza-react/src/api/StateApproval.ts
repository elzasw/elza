/**
 * Stav přístupového bodu.
 */

export enum StateApproval {
    NEW = 'NEW',
    TO_APPROVE = 'TO_APPROVE',
    APPROVED = 'APPROVED',
    TO_AMEND = 'TO_AMEND',
    REV_NEW = 'REV_NEW',
    REV_PREPARED = 'REV_PREPARED',
    REV_AMEND = 'REV_AMEND',
    INVALID = 'INVALID',
    REPLACED = 'REPLACED'
}

export const StateApprovalCaption = (value: StateApproval): string => {
    switch (value) {
        case StateApproval.NEW:
            return 'Nová';
        case StateApproval.TO_APPROVE:
            return 'Ke schválení';
        case StateApproval.APPROVED:
            return "Schválená";
        case StateApproval.TO_AMEND:
            return "K doplnění";
        case StateApproval.REV_NEW:
            return "Příprava revize";
        case StateApproval.REV_PREPARED:
            return "Revize ke schválení";
        case StateApproval.REV_AMEND:
            return "Revize k doplnění";
        case StateApproval.INVALID:
            return "Zneplatněná";
        case StateApproval.REPLACED:
            return "Nahrazená";
        default:
            console.warn('Nepřeložená hodnota', value);
            return '?';
    }
}

export const StateApprovalIcon = (value: StateApproval): string => {
    switch (value) {
        case StateApproval.APPROVED:
            return 'fa-check';
        case StateApproval.NEW:
            return 'fa-plus';
        case StateApproval.TO_AMEND:
            return 'fa-arrow-right';
        case StateApproval.TO_APPROVE:
            return 'fa-arrow-up';
        case StateApproval.REV_NEW:
            return 'fa-plus';
        case StateApproval.REV_PREPARED:
            return 'fa-arrow-up';
        case StateApproval.REV_AMEND:
            return 'fa-arrow-right';
        default:
            console.warn('Nedefinovaná ikona hodnota', value);
            return 'fa-question-circle';
    }
}
