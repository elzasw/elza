import React, { FC } from 'react';
import './SmallButton.scss';

export const SmallButton:FC<{
    onClick: (e: React.MouseEvent) => void;
    title?: string;
}> = ({
    onClick,
    title,
    children
}) => <div 
    className="small-button"
    onClick={onClick}
    title={title}
>
    {children}
</div> 
