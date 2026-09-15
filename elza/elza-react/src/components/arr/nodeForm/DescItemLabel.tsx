import React, { ReactNode } from 'react';
import {} from 'components/shared';
import { FormattedMessage } from 'react-intl';
import { nodeMessages } from 'components/arr/nodeMessages';

import classNames from 'classnames';
import './DescItemLabel.scss';
import { Button } from '../../ui';
import { CLS_CALCULABLE } from "../../../constants";

interface Props {
    value: string;
    onClick?: () => void;
    cal?: boolean;
    isValueUndefined?: boolean;
    isValueInhibited?: boolean;
    hideTooltip?: boolean;
}

export const DescItemLabel = ({
    value,
    onClick,
    cal,
    isValueUndefined,
    isValueInhibited,
    hideTooltip,
}: Props) => {
    // Sestavení hodnoty - změna znaku < na entitu, nahrazení enterů <br/>
    const updatedValue = value ? ('' + value).replace(/</g, '&lt;').replace(/(?:\r\n|\r|\n)/g, '<br />') : '';

    let renderItem: ReactNode;

    if (onClick == null) {
        renderItem = <div dangerouslySetInnerHTML={{ __html: updatedValue }} />;
    } else {
        // eslint-disable-next-line jsx-a11y/anchor-is-valid
        renderItem = (
            <Button
                variant="link"
                style={{ cursor: 'pointer' }}
                onClick={onClick}
                dangerouslySetInnerHTML={{ __html: updatedValue }}
            />
        );
    }

    if (isValueUndefined) {
        renderItem = <i>{<FormattedMessage {...nodeMessages.subNodeFormDescItemTypeUndefinedValue} />}</i>;
    }

    return (
        <div
            title={!hideTooltip ? value : undefined}
            className={classNames('desc-item-label-value', { [CLS_CALCULABLE]: cal, "inhibited": isValueInhibited })}
        >
            {renderItem}
        </div>
    );
}

export default DescItemLabel;
