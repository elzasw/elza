/**
 * Wrapper pro hodnoty prvků popisu se zobrazením tooltipu, pokud existuje.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent, TooltipTrigger} from 'components/shared';
import { injectIntl } from 'react-intl';
import { formatHintMessages } from 'components/arr/nodeMessages';


class ItemTooltipWrapper extends AbstractReactComponent {
    static propTypes = {
        tooltipTitle: PropTypes.string.isRequired,
    };

    render() {
        const {tooltipTitle, children, ...otherProps} = this.props;

        // Hlaska je cely HTML blok, proto ignoreTag; neznamy klic (napr.
        // dataType.recordRef.format, ktery zadny text nema) tooltip vypne.
        const descriptor = formatHintMessages[tooltipTitle];
        const tooltipText = descriptor
            ? this.props.intl.formatMessage(descriptor, undefined, { ignoreTag: true })
            : null;
        const tooltip = tooltipText ? <div dangerouslySetInnerHTML={{__html: tooltipText}}></div> : null;

        return (
            <TooltipTrigger content={tooltip} holdOnHover placement="vertical" {...otherProps}>
                {children}
            </TooltipTrigger>
        );
    }
}

export default injectIntl(ItemTooltipWrapper);
