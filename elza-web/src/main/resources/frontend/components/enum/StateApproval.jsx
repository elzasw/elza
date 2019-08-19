
export const NEW = 'NEW';
export const TO_APPROVE = 'TO_APPROVE';
export const APPROVED = 'APPROVED';
export const TO_AMEND = 'TO_AMEND';

/**
 * Seznam všech hodnot.
 */
export const values = [
    NEW,
    TO_APPROVE,
    APPROVED,
    TO_AMEND
];

const labels = {
    [NEW]: "Nový",
    [TO_APPROVE]: "Ke schválení",
    [APPROVED]: "Schválený",
    [TO_AMEND]: "K doplnění",
};

export function getCaption(typ) {
    return labels[typ];
}
