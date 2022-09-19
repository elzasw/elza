import classnames from 'classnames';
import React, { FC, ReactNode } from 'react';
import { Icon } from '../../../index';
import './ApTypeNames.scss';

interface ApType {
    parents: string[];
    name: string;
}

interface ApTypeNamesProps {
    apType: ApType;
    delimiter?: ReactNode;
    className?: string;
}

export const ApTypeNames:FC<ApTypeNamesProps> = ({
    apType,
    className,
    delimiter = <Icon glyph="fa-angle-right"/>,
}) => {
    const elements: ReactNode[] = [];

    if (apType.parents) {
        apType.parents.reverse().forEach((name, i) => {
            elements.push(
                <span key={'name-' + i} className="hierarchy-level">
                    {name.toUpperCase()}
                </span>,
            );
            elements.push(
                <span key={'delimiter-' + i} className="hierarchy-delimiter">
                    {delimiter}
                </span>,
            );
        });
    }
    elements.push(
        <span key="name-main" className="hierarchy-level main">
            {apType.name.toUpperCase()}
        </span>,
    );

    return <div className={`ap-type ${className}`}>
            {elements}
        </div>
}

export const RevisionApTypeNames:FC<ApTypeNamesProps & {apTypeNew?: ApType}> = ({
    apType,
    apTypeNew,
    className,
    ...otherProps
}) => {
    const isApTypeChanged = apTypeNew?.name !== apType.name;
    const oldClassName = classnames({
        old: apTypeNew && isApTypeChanged,
    }, className)
    const newClassName = classnames({
        new: apTypeNew && isApTypeChanged,
    }, className)

    return <>
        <ApTypeNames apType={apType} {...otherProps} className={oldClassName}/>
        {apTypeNew && isApTypeChanged && <ApTypeNames apType={apTypeNew} {...otherProps} className={newClassName}/>}
    </>
}

