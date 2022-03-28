import i18n from "components/i18n";
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
            return i18n("registry.revision.state.active")
        case RevStateApproval.TO_APPROVE:
            return i18n("registry.revision.state.toApprove")
        case RevStateApproval.TO_AMEND:
            return i18n("registry.revision.state.toAmend")
        default:
            console.warn('Nepřeložená hodnota', value);
            return '?';
    }
};

export const RevStateApprovalIcon = (value: RevStateApproval): string => {
    switch (value) {
        case RevStateApproval.ACTIVE:
            return 'fa-plus';
        case RevStateApproval.TO_AMEND:
            return 'fa-arrow-right';
        case RevStateApproval.TO_APPROVE:
            return 'fa-arrow-up';
        default:
            console.warn('Nedefinovaná ikona hodnota', value);
            return 'fa-question-circle';
    }
};
