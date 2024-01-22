import {PropsWithChildren} from 'react';

import "./DetailDescriptions.scss";
import classNames from "classnames";

interface Props extends PropsWithChildren{
  className?: string;
}

const DetailDescriptions = ({className, children}: Props) => {
  const cls = classNames("detail-descriptions", className)
  return (
    <span className={cls}>
        {children}
    </span>
  );
};

export default DetailDescriptions;
