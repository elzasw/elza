import React, {FC} from 'react';

interface Props {
  label?: String;
}

const DetailDescriptionsItem: FC<Props> = ({label, children}) => {
  return (
    <span className="detail-descriptions-item">
      {label && <span className="detail-descriptions-item-label">{label}</span>}
      <span className="detail-descriptions-item-content">{children}</span>
    </span>
  );
};

export default DetailDescriptionsItem;
