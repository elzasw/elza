import { Icon } from 'components';
import React, { FC } from 'react';

export const RevisionFieldExample:FC<{
    prevValue?: string;
    value?: string;
    isDeleted?: boolean;
    label: string;
    disableRevision?: boolean;
    alignTop?: boolean;
}> = ({
    prevValue, 
    value,
    isDeleted, 
    label,
    children, 
    disableRevision,
    alignTop,
}) => {
    const valuesEqual = value === prevValue; // fake equality with empty field
    // console.log(valuesEqual, prevValue, value)
    return <div>
        <label>
            {label}
            {!disableRevision && !valuesEqual && <span style={{marginLeft: "10px"}}>
                <Icon glyph="fa-undo"/>
            </span>}
        </label>
        <div style={{
            display: "flex",
            alignItems: alignTop ? undefined : "center"
            }}>
            {!valuesEqual && !isDeleted && !disableRevision &&
                <>
                    <div style={{ 
                        padding: "0px",
                        maxWidth: "50%"
                        }}>
                        {prevValue || <i>Nevyplněno</i>}
                    </div>
                    <div style={{margin: "0 10px"}}>
                    🡒
                    </div>
                </>
            }
            <div style={{
                flexGrow: 1, 
                flexShrink: 1, 
                display: "flex",
                flexDirection: "column",
            }}>
                <div style={{flex: 1}}>
                {children}
                </div>
            </div>
            {isDeleted && !disableRevision &&
                <>
                    <div style={{margin: "0 10px"}}>
                    🡒
                    </div>
                    <div >
                        <i>Smazáno</i>
                    </div>
                </>
            }
        </div>
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
