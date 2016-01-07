export function decorateFormField(field) {
    if (field.touched && field.error) {
        return {
            bsStyle: 'error',
            hasFeedback: true,
            help: field.error
        }
    }
}
