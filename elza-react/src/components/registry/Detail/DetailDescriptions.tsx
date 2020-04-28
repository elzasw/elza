import React, {FC} from 'react';

import "./DetailDescriptions.scss";
import classNames from "classnames";

interface Props {
  className?: string;
}

const DetailDescriptions: FC<Props> = ({className, children}) => {
  const cls = classNames("detail-descriptions", className)
  return (
    <span className={cls}>
        {children}
    </span>
  );
};

export default DetailDescriptions;
