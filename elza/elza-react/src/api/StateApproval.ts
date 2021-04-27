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
            return 'nová';
        case StateApproval.TO_APPROVE:
            return 'ke schválení';
        case StateApproval.APPROVED:
            return "schválená";
        case StateApproval.TO_AMEND:
            return "k doplnění";
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
