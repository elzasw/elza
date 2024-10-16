import { Icon } from 'components';
import { PropsWithChildren } from 'react';
import { RevisionDisplay } from './RevisionDisplay';
import { SmallButton } from "components/shared/button/small-button";
import './RevisionField.scss';
import FormInput from '../../shared/form/FormInput';

interface Props extends PropsWithChildren {
    prevValue?: string;
    value?: string;
    isDeleted?: boolean;
    label: string;
    disableRevision?: boolean;
    alignTop?: boolean;
    equalSplit?: boolean;
    onRevert?: () => void;
    onDelete?: () => void;
}

export const RevisionFieldExample = ({
    prevValue,
    value,
    isDeleted,
    label,
    children,
    disableRevision = true,
    alignTop,
    equalSplit,
    onRevert,
    onDelete,
}: Props) => {
    const valuesEqual = value === prevValue;
    const isLongValue = prevValue && prevValue.length > 1000 || false;

    const renderPrevValue = () => {
        if(!isLongValue) { return prevValue }
        return <FormInput
            style={{
                resize: isDeleted ? undefined : "none",
                height: "100%",
                minHeight: "6em",
            }}
            type="textarea"
            disabled={true}
        >
            {prevValue}
        </FormInput>
    };

    const renderValue = () => <div style={{flex: 1}}>{children}</div>;

    const renderActions = () => {
        const actions: React.ReactNode[] = [];
        if(!disableRevision && !valuesEqual && onRevert){
            actions.push(<SmallButton
                onClick={onRevert}
            >
                <Icon glyph="fa-undo"/>
            </SmallButton>)
        }

        if(actions.length === 0) {return <></>}

        return <div className="actions">
            {actions}
        </div>
    }

    const renderHidableActions = () => {
        const actions: React.ReactNode[] = [];
        if(onDelete){
            actions.push(<SmallButton
                onClick={onDelete}
            >
                <Icon glyph="fa-trash"/>
            </SmallButton>)
        }

        if(actions.length === 0) {return <></>}

        return <div className="actions hidable">
            {actions}
        </div>
    }

    return <div className="revision-field">
        <div className="revision-field-title">
            <label title={label}>
                {label}
            </label>
            {renderActions()}
            {renderHidableActions()}
        </div>
        <RevisionDisplay
            renderPrevValue={renderPrevValue}
            renderValue={renderValue}
            valuesEqual={valuesEqual}
            alignTop={alignTop}
            isDeleted={isDeleted}
            disableRevision={disableRevision}
            equalSplit={equalSplit}
            expandLeft={isLongValue}
            isField={true}
            isNew={!prevValue}
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
