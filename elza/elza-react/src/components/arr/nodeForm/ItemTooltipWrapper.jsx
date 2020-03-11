/**
 * Wrapper pro hodnoty prvků popisu se zobrazením tooltipu, pokud existuje.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent, i18n, TooltipTrigger} from 'components/shared';

class ItemTooltipWrapper extends AbstractReactComponent {

    static propTypes = {
        tooltipTitle: PropTypes.string.isRequired,
    }

    render() {
        const {tooltipTitle, children, ...otherProps} = this.props;

        const tooltipText = i18n("^" + tooltipTitle);
        const tooltip = tooltipText ? <div dangerouslySetInnerHTML={{__html: tooltipText}}></div> : null;

        return (
            <TooltipTrigger
                content={tooltip}
                holdOnHover
                placement="vertical"
                {...otherProps}
            >
                {children}
            </TooltipTrigger>
        )
    }
}

export default ItemTooltipWrapper;
