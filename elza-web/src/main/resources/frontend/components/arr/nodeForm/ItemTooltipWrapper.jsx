/**
 * Wrapper pro hodnoty prvků popisu se zobrazením tooltipu, pokud existuje.
 */

import React from 'react';
import {TooltipTrigger, AbstractReactComponent, i18n} from 'components/index.jsx';

class ItemTooltipWrapper extends AbstractReactComponent {

    static PropTypes = {
        tooltipTitle: React.PropTypes.string.isRequired,
    }

    render() {
        const {tooltipTitle, children, ...otherProps} = this.props;

        const tooltipText = i18n("^" + tooltipTitle);
        const tooltip = tooltipText ? <div dangerouslySetInnerHTML={{__html: tooltipText}}></div> : null;

        return (
            <TooltipTrigger
                content={tooltip}
                holdOnHover
                holdOnFocus
                placement="vertical"
                {...otherProps}
            >
                {children}
            </TooltipTrigger>
        )
    }
}

export default ItemTooltipWrapper;