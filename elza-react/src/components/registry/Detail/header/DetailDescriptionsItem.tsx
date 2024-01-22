import { PropsWithChildren} from 'react';

interface Props extends PropsWithChildren {
    label?: String;
    className?: string;
    title?: string;
}

const DetailDescriptionsItem = ({
    label,
    className="",
    title,
    children
}: Props) => {
    return (
        <div title={title} className={`detail-descriptions-item ${className}`}>
            <div className={`detail-descriptions-item-content`}>
                {label && <span className="detail-descriptions-item-label">{label}</span>}
                {children}
            </div>
        </div>
    );
};

interface PropsWithButton extends Props {
    renderButton?: () => React.ReactNode;
}

export const DetailDescriptionsItemWithButton = ({
    label,
    renderButton,
    className = "",
    children,
    title,
}: PropsWithButton) => {
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
