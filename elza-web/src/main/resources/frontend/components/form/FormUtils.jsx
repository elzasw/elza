/**
 * Utility pro formuláře s inplace editací.
 */

/**
 * Vrácení objektu pro dekoraci input prvku inplace editace.
 * @param field {Object} objekt s informací o inplace prvku
 * @return {Object} objekt pro dekoraci input prvku
 */
export function decorateFormField(field) {
    if (field.touched && field.error) {
        return {
            bsStyle: 'error',
            hasFeedback: true,
            help: field.error
        }
    }
}
