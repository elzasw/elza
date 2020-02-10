import React from 'react';

export default function renderFieldWithAddons(field, addonsBefore, addonsAfter) {
    const hasAddonsBefore = addonsBefore && (!(addonsBefore instanceof Array) || (addonsBefore instanceof Array && addonsBefore.length > 0));
    const hasAddonsAfter = addonsAfter && (!(addonsAfter instanceof Array) || (addonsAfter instanceof Array && addonsAfter.length > 0));

    if (hasAddonsBefore || hasAddonsAfter) {
        return (
            <div className="input-group">
                {addonsBefore}
                {field}
                {addonsAfter}
            </div>
        )
    } else {
        return field;
    }
}
