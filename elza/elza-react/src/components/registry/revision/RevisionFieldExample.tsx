import { Icon } from 'components';
import { PropsWithChildren } from 'react';
import { RevisionDisplay } from './RevisionDisplay';
import { SmallButton } from "components/shared/button/small-button";
import './RevisionField.scss';
import FormInput from '../../shared/form/FormInput';

interface Props extends PropsWithChildren {
    prevValue?: string;
    isDeleted?: boolean;
    isUpdated?: boolean;
    label: string;
    disableRevision?: boolean;
    alignTop?: boolean;
    equalSplit?: boolean;
    onRevert?: () => void;
    onDelete?: () => void;
}

export const RevisionFieldExample = ({
    prevValue,
    isDeleted,
    label,
    children, // current value display components
    disableRevision = true,
    alignTop,
    equalSplit,
    onRevert,
    onDelete,
    isUpdated,
}: Props) => {
    const isLongValue = prevValue && prevValue.length > 1000 || false;

    const renderPrevValue = () => {
        if (!isLongValue) { return prevValue }
        // Omezi zobrazeni do textarea, pokud je text dlouhy
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

    const renderValue = () => <div style={{ flex: 1 }}>{children}</div>;

    const renderActions = () => {
        const actions: React.ReactNode[] = [];
        if (!disableRevision && (isUpdated || isDeleted) && onRevert) {
            actions.push(<SmallButton
                onClick={onRevert}
            >
                <Icon glyph="fa-undo" />
            </SmallButton>)
        }

        if (actions.length === 0) { return <></> }

        return <div className="actions">
            {actions}
        </div>
    }

    const renderHidableActions = () => {
        const actions: React.ReactNode[] = [];
        if (onDelete) {
            actions.push(<SmallButton
                onClick={onDelete}
            >
                <Icon glyph="fa-trash" />
            </SmallButton>)
        }

        if (actions.length === 0) { return <></> }

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
            valuesEqual={!isUpdated && !isDeleted}
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
