export interface PartValidationErrorsVO {
    /**
     * Identifikátor partu
     */
    id: number;

    /**
     * Validační chyby partu
     */
    errors?: string[];
}
