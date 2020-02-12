export default function apiErrorsTransform(errors) {
    const result = {};

    for (let index in errors) {
        if (errors.hasOwnProperty(index)) {
            const error = errors[index];
            const attrs = error.field.split(".");

            var obj = result;
            for (let a=0; a<attrs.length; a++) {
                const attr = attrs[a];

                if (a + 1 == attrs.length) {    // poslední
                    obj[attr] = error.message;
                } else {
                    if (!obj[attr]) {
                        obj[attr] = {};
                    }
                    obj = obj[attr];
                }
            }
        }
    }

    return result;
}
