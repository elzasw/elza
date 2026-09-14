import { defineMessages } from "react-intl";
import { getIntl } from "components/shared/lang/intlInstance";

// Id jsou převzatá z legacy katalogu beze změny. Funkce vrací text do datové
// struktury, ne do JSX, proto sdílená instance.
const messages = defineMessages({
    active: { id: "registry.revision.state.active", defaultMessage: "Revize v přípravě" },
    toApprove: { id: "registry.revision.state.toApprove", defaultMessage: "Revize ke schválení" },
    toAmend: { id: "registry.revision.state.toAmend", defaultMessage: "Revize k doplnění" },
});
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
            return getIntl().formatMessage(messages.active)
        case RevStateApproval.TO_APPROVE:
            return getIntl().formatMessage(messages.toApprove)
        case RevStateApproval.TO_AMEND:
            return getIntl().formatMessage(messages.toAmend)
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
