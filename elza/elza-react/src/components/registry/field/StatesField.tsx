import {Autocomplete, Icon} from 'components/shared';
import {Button} from 'components/ui';
import {Form} from 'react-bootstrap';
import React, {memo} from 'react';
import {WrappedFieldArrayProps} from 'redux-form';
import {indexById, objectById} from "../../../shared/utils";
import * as FieldUtils from "../../../utils/FieldUtils";
import * as StateInfo from "../form/filter/StateInfo";

interface IStatesFieldProps extends WrappedFieldArrayProps<string> {
    label: string;
    disabled: boolean;
}

const items = FieldUtils.createItems(StateInfo.getValues, StateInfo.getName);

export const StatesField: React.FC<IStatesFieldProps> = memo(({fields, meta, label, disabled, ...props}) => {
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
                    if (values.indexOf(state.id) === -1) {
                        fields.push(state.id);
                    }
                }}
            />
            {meta.error && <Form.Control.Feedback type="invalid">
                {meta.error}
            </Form.Control.Feedback>}
            {fields.length > 0 && <div className="selected-data-container">
                {fields.map((field, index) =>
                    <StateField disabled={disabled} key={index} index={index} fields={fields} states={items}/>,
                )}
            </div>}

        </>
    );
});

interface IStateFieldProps {
    index: number;
    fields: IStatesFieldProps['fields'];
    states: any[];
    disabled: boolean;
}

const StateField: React.FC<IStateFieldProps> = memo(({index, fields, states, disabled}) => (
    <div className="selected-data" key={index}>
        <span>{objectById(states, fields.get(index), 'id').name}</span>
        <Button disabled={disabled} variant={"default"} onClick={() => fields.remove(index)}>
            <Icon glyph="fa-times"/>
        </Button>
    </div>
));
