// @ts-nocheck
import React, { memo, PropsWithChildren, ReactElement } from 'react';
import { Form } from 'react-bootstrap';


const FileInput: React.FC<PropsWithChildren<IFormInputProps>> = memo(React.forwardRef((props, ref) => {

    const {
        error,
        touched,
        children,
        type,
        label,
        value,
        inline,
        feedback,
        active,
        asyncValidating,
        valid,
        visited,
        autofilled,
        dirty,
        invalid,
        pristine,
        submitting,
        submitFailed,
        dispatch,
        onChange,
        onBlur,
        ...otherProps
    } = props;

    const hasError = !!(touched && error);
    let inlineProps = {...(inline ? error ? {title: error} : {} : {})};

    return (
        <Form.Group>
            {label && <Form.Label>{label}</Form.Label>}
            <Form.Control
                type={"file"}
                ref={ref}
                //value={value}
                children={children}
                isInvalid={hasError}
                {...otherProps}
                {...inlineProps}
                onBlur={null}
                onChange={(e) => {
                    onChange(e.target.files[0]);
                }}
            />
            {!inline && hasError && <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>}
        </Form.Group>
    );
}));

export default FileInput;
