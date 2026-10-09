/**
 * Stav přístupového bodu.
 */

import { ApStateApproval } from "elza-api";
import { defineMessages } from "react-intl";
import { getIntl } from "components/shared/lang/intlInstance";

// Funkce vrací text do datové struktury (options selectu), ne do JSX,
// proto sdílená instance.
const messages = defineMessages({
    new: { id: "registry.state.new", defaultMessage: "Nová" },
    toApprove: { id: "registry.state.toApprove", defaultMessage: "Ke schválení" },
    approved: { id: "registry.state.approved", defaultMessage: "Schválená" },
    toAmend: { id: "registry.state.toAmend", defaultMessage: "K doplnění" },
    invalid: { id: "registry.state.invalid", defaultMessage: "Zneplatněná" },
    replaced: { id: "registry.state.replaced", defaultMessage: "Nahrazená" },
});

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

export const StateApprovalCaption = (value: StateApproval | StateApprovalEx | ApStateApproval): string => {
    switch (value) {
        case StateApproval.NEW:
        case ApStateApproval.New:
            return getIntl().formatMessage(messages.new);
        case StateApproval.TO_APPROVE:
        case ApStateApproval.ToApprove:
            return getIntl().formatMessage(messages.toApprove);
        case StateApproval.APPROVED:
        case ApStateApproval.Approved:
            return getIntl().formatMessage(messages.approved);
        case StateApproval.TO_AMEND:
        case ApStateApproval.ToAmend:
            return getIntl().formatMessage(messages.toAmend);
        case StateApprovalEx.INVALID:
            return getIntl().formatMessage(messages.invalid);
        case StateApprovalEx.REPLACED:
            return getIntl().formatMessage(messages.replaced);
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
