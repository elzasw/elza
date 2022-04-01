import React, {FC} from 'react';

interface Props {
    label?: String;
    className?: string;
    title?: string;
}

const DetailDescriptionsItem: FC<Props> = ({
    label, 
    className="", 
    title,
    children
}) => {
    return (
        <div title={title} className={`detail-descriptions-item ${className}`}>
            <div className={`detail-descriptions-item-content`}>
                {label && <span className="detail-descriptions-item-label">{label}</span>}
                {children}
            </div>
        </div>
    );
};

export const DetailDescriptionsItemWithButton: FC<Props & {
    renderButton?: () => React.ReactNode;
}> = ({
    label, 
    renderButton,
    className = "", 
    children,
    title,
}) => {
    return (
        <div title={title} className={`detail-descriptions-item with-button ${className}`}>
            <div className={`detail-descriptions-item-content`}>
                {label && <span className="detail-descriptions-item-label">{label}</span>}
                {children}
            </div>
            {renderButton && <div className="detail-descriptions-item-button">{renderButton()}</div>}
        </div>
    );
};

export default DetailDescriptionsItem;
