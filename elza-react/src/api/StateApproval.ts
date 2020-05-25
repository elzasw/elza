/**
 * Stav přístupového bodu.
 */

export enum StateApproval {
    NEW = 'NEW',
    TO_APPROVE = 'TO_APPROVE',
    APPROVED = 'APPROVED',
    TO_AMEND = 'TO_AMEND',
}

export const StateApprovalCaption = (value: StateApproval): string => {
    switch (value) {
        case StateApproval.NEW:
            return 'Nový';
        case StateApproval.TO_APPROVE:
            return 'Připraven ke schválení';
        case StateApproval.APPROVED:
            return "Schválený";
        case StateApproval.TO_AMEND:
            return "K doplnění";
        default:
            console.warn('Nepřeložená hodnota', value);
            return '?';
    }
}

export const StateApprovalColor = (value: StateApproval): string => {
    switch (value) {
        case StateApproval.APPROVED:
        case StateApproval.NEW:
            return "#317E9F";
        case StateApproval.TO_APPROVE:
        case StateApproval.TO_AMEND:
            return "#EBA960";
        default:
            console.warn('Nedefinovaná barva', value);
            return '';
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
