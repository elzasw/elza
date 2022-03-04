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
            {!valuesEqual && !disableRevision &&
                <>
                    <div className="value-previous">
                        {!isNew ? renderPrevValue() : <i>Nevyplněno</i>}
                    </div>
                    <div className="arrow">
                    🡒
                    </div>
                </>
            }
            {!isDeleted &&  
                <>
                    <div className="value-current">
                        <div style={{flex: 1}}>
                            {renderValue()}
                        </div>
                    </div>
                </>
            }
            {isDeleted && !disableRevision &&
                <>
                    <div className="value-current">
                        <i>Smazáno</i>
                    </div>
                </>
            }
        </div>
    );
};
