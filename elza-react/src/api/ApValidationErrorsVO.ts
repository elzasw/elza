import {PartValidationErrorsVO} from "./PartValidationErrorsVO";

export interface ApValidationErrorsVO {
    /**
     * Validační chyby entity
     */
    errors?: string[];

    /**
     * Validační chyby partů
     */
    partErrors?: PartValidationErrorsVO[];
}
