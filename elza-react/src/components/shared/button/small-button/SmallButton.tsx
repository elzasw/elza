import { PropsWithChildren } from 'react';
import './SmallButton.scss';

interface Props extends PropsWithChildren{
    onClick: (e: React.MouseEvent) => void;
    title?: string;
}

export const SmallButton = ({
    onClick,
    title,
    children
}: Props) => <div
    className="small-button"
    onClick={onClick}
    title={title}
>
    {children}
</div>
