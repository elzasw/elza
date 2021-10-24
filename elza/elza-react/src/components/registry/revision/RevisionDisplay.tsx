import React, { FC } from 'react';
import './RevisionDisplay.scss';
import classnames from 'classnames';

type Props = {
    renderPrevValue: () => React.ReactNode;
    renderValue: () => React.ReactNode;
    isDeleted?: boolean;
    disableRevision?: boolean;
    alignTop?: boolean;
    valuesEqual?: boolean;
    equalSplit?: boolean;
    isField?: boolean;
};

export const RevisionDisplay: FC<Props> = ({
    renderPrevValue, 
    renderValue,
    isDeleted, 
    // children, 
    disableRevision,
    alignTop,
    valuesEqual,
    equalSplit,
    isField,
}) => {
    // const valuesEqual = renderValue === renderPrevValue; // fake equality with empty field
    const className = classnames({
        "revision-display": true,
        "align-center": !alignTop,
        "equal-split": equalSplit,
        "field": isField,
    })
    return (
        <div className={className}>
            {!valuesEqual && !isDeleted && !disableRevision &&
                <>
                    <div className="value-previous">
                        {renderPrevValue() || <i>Nevyplněno</i>}
                    </div>
                    <div className="arrow">
                    🡒
                    </div>
                </>
            }
            <div className="value-current">
                <div style={{flex: 1}}>
                {renderValue()}
                </div>
            </div>
            {isDeleted && !disableRevision &&
                <>
                    <div className="arrow">
                    🡒
                    </div>
                    <div className="value-current">
                        <i>Smazáno</i>
                    </div>
                </>
            }
        </div>
    );
};
