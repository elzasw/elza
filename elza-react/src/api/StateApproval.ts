/**
 * Stav přístupového bodu.
 */

export enum StateApproval {
    NEW = 'NEW',
    TO_APPROVE = 'TO_APPROVE',
    APPROVED = 'APPROVED',
    TO_AMEND = 'TO_AMEND',
}

export enum StateApprovalEx {
    NEW = 'NEW',
    TO_APPROVE = 'TO_APPROVE',
    APPROVED = 'APPROVED',
    TO_AMEND = 'TO_AMEND',
    INVALID = 'INVALID',
    REPLACED = 'REPLACED'
}

export const StateApprovalCaption = (value: StateApproval | StateApprovalEx): string => {
    switch (value) {
        case StateApproval.NEW:
            return 'Nová';
        case StateApproval.TO_APPROVE:
            return 'Ke schválení';
        case StateApproval.APPROVED:
            return "Schválená";
        case StateApproval.TO_AMEND:
            return "K doplnění";
        case StateApprovalEx.INVALID:
            return "Zneplatněná";
        case StateApprovalEx.REPLACED:
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
        default:
            console.warn('Nedefinovaná ikona hodnota', value);
            return 'fa-question-circle';
    }
}
