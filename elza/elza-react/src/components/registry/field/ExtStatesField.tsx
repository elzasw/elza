import {Autocomplete, Icon} from 'components/shared';
import {Button} from 'components/ui';
import {Form} from 'react-bootstrap';
import React, {memo} from 'react';
import {WrappedFieldArrayProps} from 'redux-form';
import {objectById} from "../../../shared/utils";
import * as FieldUtils from "../../../utils/FieldUtils";
import * as ExtStateInfo from "../form/filter/ExtStateInfo";

interface IExtStatesFieldProps extends WrappedFieldArrayProps<string> {
    label: string;
    disabled: boolean;
}

const items = FieldUtils.createItems(ExtStateInfo.getValues, ExtStateInfo.getName);

export const ExtStatesField: React.FC<IExtStatesFieldProps> = memo(({fields, meta, label, disabled, ...props}) => {
    return (
        <>
            <Autocomplete
                tags
                label={label}
                items={items}
                disabled={disabled}
                getItemId={item => (item ? item.id : null)}
                getItemName={item => (item ? item.name : '')}
                onChange={(state: any) => {
                    const values = fields.getAll() || [];
                    if (state.id && values.indexOf(state.id) === -1) {
                        fields.push(state.id);
                    }
                }}
            />
            {meta.error && <Form.Control.Feedback type="invalid">
                {meta.error}
            </Form.Control.Feedback>}
            {fields.length > 0 && <div className="selected-data-container">
                {fields.map((field, index) =>
                    <ExtStateField disabled={disabled} key={index} index={index} fields={fields} states={items}/>,
                )}
            </div>}

        </>
    );
});

interface IExtStateFieldProps {
    index: number;
    fields: IExtStatesFieldProps['fields'];
    states: any[];
    disabled?: boolean;
}

const ExtStateField: React.FC<IExtStateFieldProps> = memo(({index, fields, states, disabled}) => (
    <div className="selected-data" key={index}>
        <span>{objectById(states, fields.get(index), 'id').name}</span>
        <Button disabled={disabled} variant={"default"} onClick={() => fields.remove(index)}>
            <Icon glyph="fa-times"/>
        </Button>
    </div>
));
