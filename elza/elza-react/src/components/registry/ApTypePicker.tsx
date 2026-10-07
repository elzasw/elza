import { Combobox, Field, Option, makeStyles, tokens } from '@fluentui/react-components';
import { WebApi } from 'actions';
import { ApTypeVO } from 'api/ApTypeVO';
import { useEffect, useMemo, useState } from 'react';
import { defineMessages, useIntl } from 'react-intl';

const messages = defineMessages({
    chooseScopeFirst: {
        id: 'registry.apTypePicker.chooseScopeFirst',
        defaultMessage: 'Nejprve vyberte oblast',
    },
    noMatch: {
        id: 'registry.apTypePicker.noMatch',
        defaultMessage: 'Žádná podtřída neodpovídá',
    },
});

const useStyles = makeStyles({
    option: {
        display: 'block',
    },
    group: {
        color: tokens.colorNeutralForeground3,
    },
});

/** Class of the tree with its depth, in the order of the tree. */
interface FlatApType {
    type: ApTypeVO;
    depth: number;
}

export interface ApTypePickerProps {
    /** Scope whose rule set decides the offered classes; without it the picker is disabled. */
    scopeId?: number;
    /** Id of the selected class. */
    value?: number;
    onChange: (apType: ApTypeVO | undefined) => void;
    label: string;
    disabled?: boolean;
    required?: boolean;
    /** Only these class codes (with their parents); empty for all. */
    apTypeFilter?: string[];
}

/**
 * Entity class (subclass) for an entity in a scope: the classes the rule set of the scope offers, as
 * an indented tree. Classes that cannot be assigned there (for example abstract roots) are shown but
 * cannot be chosen; typing filters by name. A selected class the new scope does not allow is cleared.
 */
export const ApTypePicker = ({
    scopeId,
    value,
    onChange,
    label,
    disabled,
    required,
    apTypeFilter,
}: ApTypePickerProps) => {
    const intl = useIntl();
    const styles = useStyles();
    const [types, setTypes] = useState<FlatApType[]>([]);
    const [query, setQuery] = useState('');

    useEffect(() => {
        let cancelled = false;
        if (scopeId == null) {
            setTypes([]);
            return;
        }
        WebApi.getApTypes(scopeId).then((tree) => {
            if (!cancelled) {
                setTypes(flatten(filterApTypes(tree, apTypeFilter), 0));
            }
        });
        return () => {
            cancelled = true;
        };
        // apTypeFilter is a constant of the dialog
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [scopeId]);

    const selected = types.find(({ type }) => type.id === value)?.type;

    // the class chosen for another scope may not be allowed in this one
    useEffect(() => {
        if (value != null && types.length > 0 && !selected?.addRecord) {
            onChange(undefined);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [types]);

    useEffect(() => {
        setQuery(selected?.name ?? '');
    }, [selected]);

    const visible = useMemo(() => {
        const text = query.trim().toLocaleLowerCase();
        if (!text || text === selected?.name.toLocaleLowerCase()) {
            return types;
        }
        return types.filter(({ type }) => type.name.toLocaleLowerCase().includes(text));
    }, [types, query, selected]);

    return (
        <Field label={label} required={required}>
            <Combobox
                value={query}
                selectedOptions={value != null ? [String(value)] : []}
                placeholder={scopeId == null ? intl.formatMessage(messages.chooseScopeFirst) : undefined}
                disabled={disabled || scopeId == null}
                onChange={(event) => setQuery(event.target.value)}
                onOptionSelect={(_event, data) => {
                    const type = types.find((t) => String(t.type.id) === data.optionValue)?.type;
                    if (type?.addRecord) {
                        onChange(type);
                    }
                }}
            >
                {visible.map(({ type, depth }) => (
                    <Option
                        key={type.id}
                        value={String(type.id)}
                        text={type.name}
                        disabled={!type.addRecord}
                        className={type.addRecord ? styles.option : `${styles.option} ${styles.group}`}
                    >
                        <span style={{ paddingLeft: `${depth * 16}px` }}>{type.name}</span>
                    </Option>
                ))}
                {scopeId != null && types.length > 0 && visible.length === 0 && (
                    <Option disabled value="" text="">
                        {intl.formatMessage(messages.noMatch)}
                    </Option>
                )}
            </Combobox>
        </Field>
    );
};

function flatten(tree: ApTypeVO[], depth: number): FlatApType[] {
    return tree.flatMap((type) => [{ type, depth }, ...flatten(type.children ?? [], depth + 1)]);
}

/** Classes of the given codes with their parents. */
function filterApTypes(apTypes: ApTypeVO[] = [], apTypeCodes: string[] = []): ApTypeVO[] {
    if (apTypeCodes.length === 0) {
        return apTypes;
    }
    const result: ApTypeVO[] = [];
    apTypes.forEach((type) => {
        if (apTypeCodes.includes(type.code)) {
            result.push(type);
        } else if (type.children) {
            const children = filterApTypes(type.children, apTypeCodes);
            if (children.length > 0) {
                result.push({ ...type, children });
            }
        }
    });
    return result;
}

export default ApTypePicker;
