import React, { FC } from 'react';
import './RevisionDisplay.scss';
import classnames from 'classnames';

type Props = {
    renderPrevValue: () => React.ReactNode;
    renderValue: () => React.ReactNode;
    isDeleted?: boolean;
    isNew?: boolean;
    disableRevision?: boolean;
    alignTop?: boolean;
    valuesEqual?: boolean;
    equalSplit?: boolean;
    expandLeft?: boolean;
    isField?: boolean;
};

export const RevisionDisplay: FC<Props> = ({
    renderPrevValue, 
    renderValue,
    isDeleted, 
    isNew,
    // children, 
    disableRevision = false,
    alignTop,
    valuesEqual,
    equalSplit,
    expandLeft,
    isField,
}) => {
    // const valuesEqual = renderValue === renderPrevValue; // fake equality with empty field
    const className = classnames({
        "revision-display": true,
        "align-center": !alignTop,
        "equal-split": equalSplit,
        "expand-left": expandLeft,
        "field": isField,
    })
    const colorize = !valuesEqual && !disableRevision && !isField;
    return (
        <div className={className}>
            {!valuesEqual && !disableRevision &&
                <>
                    <div className={`value-previous ${colorize && !isNew ? 'colored' : ''}`}>
                        {!isNew ? renderPrevValue() : <span className="constant">Nevyplněno</span>}
                    </div>
                    <div className="arrow constant">
                    🡒
                    </div>
                </>
            }
            {!isDeleted &&  
                <>
                    <div className={`value-current ${colorize ? 'colored' : ''}`}>
                        <div style={{flex: 1}}>
                            {renderValue()}
                        </div>
                    </div>
                </>
            }
            {isDeleted && !disableRevision &&
                <>
                    <div className="value-current">
                        <span className="constant">Smazáno</span>
                    </div>
                </>
            }
        </div>
    );
};
