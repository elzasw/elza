import React, { FC } from "react";
import "./CheckboxGroupItem.scss";

export const CheckboxGroupItem:FC<{
    label: string;
} & React.HTMLProps<HTMLInputElement>> = ({
    label,
    ...otherProps
}) => {
    return <div className="checkbox-item">
        <input {...otherProps} type="checkbox" />
        <div className="label">{label}</div>
    </div>
}
