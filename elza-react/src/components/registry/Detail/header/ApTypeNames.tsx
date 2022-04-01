import React, { FC, ReactNode } from 'react';
import { Icon } from '../../../index';

interface ApType {
    parents: string[];
    name: string;
}

interface ApTypeNamesProps {
    apType: ApType;
    delimiter?: ReactNode;
}

export const ApTypeNames:FC<ApTypeNamesProps> = ({
    apType,
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

    return <div className="ap-type">
        {elements}
    </div>;
}

