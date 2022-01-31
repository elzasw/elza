/**
 * Stav revize přístupového bodu.
 */

export enum RevStateApproval {
    ACTIVE = 'ACTIVE',
    TO_APPROVE = 'TO_APPROVE',
    TO_AMEND = 'TO_AMEND',
}

export const RevStateApprovalCaption = (value: RevStateApproval): string => {
    switch (value) {
        case RevStateApproval.ACTIVE:
            return 'aktivní';
        case RevStateApproval.TO_APPROVE:
            return 'ke schválení';
        case RevStateApproval.TO_AMEND:
            return "k doplnění";
        default:
            console.warn('Nepřeložená hodnota', value);
            return '?';
    }
};

export const RevStateApprovalIcon = (value: RevStateApproval): string => {
    switch (value) {
        case RevStateApproval.ACTIVE:
            return 'fa-check';
        case RevStateApproval.TO_AMEND:
            return 'fa-arrow-right';
        case RevStateApproval.TO_APPROVE:
            return 'fa-arrow-up';
        default:
            console.warn('Nedefinovaná ikona hodnota', value);
            return 'fa-question-circle';
    }
};
