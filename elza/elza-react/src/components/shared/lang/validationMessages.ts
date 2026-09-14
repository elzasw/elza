import { defineMessages } from "react-intl";

/**
 * Validační hlášky sdílené mimo React.
 *
 * Používá je `components/validate`, reducer `stores/app/arr/subNodeForm`
 * i `stores/app/registry/registryDetail` - tedy kód, který běží mimo render
 * a nemá přístup k hookům. Formátuje se přes `getIntl()`; hláška je krátkodobá
 * a přepočítá se při další validaci, takže to pravidlo ze `intlInstance.ts`
 * splňuje.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const validationMessages = defineMessages({
    valueNotEmpty: {
        id: "subNodeForm.validate.value.notEmpty",
        defaultMessage: "Hodnota musí být uvedena",
    },
    specRequired: {
        id: "subNodeForm.validate.spec.required",
        defaultMessage: "Specifikace musí být uvedena",
    },
    pointCoordinates: {
        id: "subNodeForm.errorPointCoordinates",
        defaultMessage:
            'Chybný formát souřadnic. Podporován je formát WKT nebo bod zadaný ve tvaru "15.5154,49.535"',
    },
    intNotInt: {
        id: "validate.validateInt.notInt",
        defaultMessage: "Nejedná se o celé číslo",
    },
    intOutOfRange: {
        id: "validate.validateInt.outOfRange",
        defaultMessage: "Zadané číslo je mimo rozsah celého čísla typu INT",
    },
    durationOutOfRange: {
        id: "validate.validateDuration.outOfRange",
        defaultMessage:
            "Zadaná doba trvání je mimo rozsah intervalu <00:00:00,596523:14:07>",
    },
    doubleOutOfRange: {
        id: "validate.validateDouble.outOfRange",
        defaultMessage:
            "Zadané číslo je mimo rozsah desetinného čísla typu Decimal(18,6)",
    },
});
