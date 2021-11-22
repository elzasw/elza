import { Icon } from 'components';
import React, { FC } from 'react';
import { RevisionDisplay } from './RevisionDisplay';

export const RevisionFieldExample:FC<{
    prevValue?: string;
    value?: string;
    isDeleted?: boolean;
    label: string;
    disableRevision?: boolean;
    alignTop?: boolean;
    equalSplit?: boolean;
}> = ({
    prevValue, 
    value,
    isDeleted, 
    label,
    children, 
    disableRevision = true,
    alignTop,
    equalSplit,
}) => {
    const valuesEqual = value === prevValue;
    // console.log(valuesEqual, prevValue, value)
    const renderPrevValue = () => prevValue;
    const renderValue = () => <div style={{flex: 1}}>{children}</div>;
    return <div>
        <label>
            {label}
            {!disableRevision && !valuesEqual && <span style={{marginLeft: "10px"}}>
                <Icon glyph="fa-undo"/>
            </span>}
        </label>
        <RevisionDisplay
            renderPrevValue={valuesEqual || disableRevision ? renderValue : renderPrevValue}
            renderValue={renderValue}
            valuesEqual={valuesEqual}
            alignTop={alignTop}
            isDeleted={isDeleted}
            disableRevision={disableRevision}
            equalSplit={equalSplit}
            isField={true}
            />
    </div>
}

// ----------
// textarea 
// ----------
//
// return <div>
//     <label>
//         {label}&nbsp; &nbsp;<Icon glyph="fa-undo"/>
//     </label>
//     <div style={{display: "flex", flexDirection: "row"}}>
//         {props.input.value && <>
//         <div style={{ display: "flex", padding: "0px", flex: 1, maxWidth: "50%"}}>
//             <div style={{flexGrow: 1}}>
//             {props.input.value}
//             </div>
//         </div>
//             <div style={{margin: "0 10px"}}>
//             🡒
//             </div>
//             </>}
//         <div style={{flex:1}}>
//             <ReduxFormFieldErrorDecorator
//                 {...props as any}
//                 input={{
//                     ...props.input as any,
//                     onBlur: handleChange // inject modified onChange handler
//                 }}
//                 disabled={disabled}
//                 maxLength={limitLength}
//                 renderComponent={FormInput}
//                 type="textarea"
//                 />
//         </div>
//     </div>
// </div>
