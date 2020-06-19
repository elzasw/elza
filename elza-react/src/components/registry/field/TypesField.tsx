import {Autocomplete, Icon} from 'components/shared';
import {Button} from 'components/ui';
import {Form} from 'react-bootstrap';
import React, {memo} from 'react';
import {WrappedFieldArrayProps} from 'redux-form';
import {objectById} from "../../../shared/utils";
import {ApTypeVO} from "../../../api/ApTypeVO";
import {flatRecursiveMap, getMapFromList} from "../../../stores/app/utils";

interface ITypesFieldProps extends WrappedFieldArrayProps<string> {
    label: string;
    disabled: boolean;
    items: ApTypeVO[];
}

export const TypesField: React.FC<ITypesFieldProps> = memo(({fields, meta, label, disabled, items, ...props}) => {
    const itemsFlatt = flatRecursiveMap(getMapFromList(items));
    return (
        <>
            <Autocomplete
                tags
                tree
                alwaysExpanded
                label={label}
                items={items}
                disabled={disabled}
                getItemId={item => (item ? item.id : null)}
                getItemName={item => (item ? item.name : '')}
                onChange={(type: ApTypeVO) => {
                    const values = fields.getAll() || [];
                    if (objectById(values, type.id, 'id') === null) {
                        fields.push("" + type.id);
                    }
                }}
            />
            {meta.error && <Form.Control.Feedback type="invalid">
                {meta.error}
            </Form.Control.Feedback>}
            {fields.length > 0 && <div className="selected-data-container">
                {fields.map((field, index) =>
                    <TypeField disabled={disabled} key={index} index={index} fields={fields} types={itemsFlatt}/>,
                )}
            </div>}
        </>
    );
});

interface ITypeFieldProps {
    index: number;
    fields: ITypesFieldProps['fields'];
    types: any
    disabled: boolean;
}

const TypeField: React.FC<ITypeFieldProps> = memo(({index, fields, types, disabled}) => {
    const name = types[parseInt(fields.get(index))].name;
    return (
        <div title={name} className="selected-data" key={index}>
            <span>{name}</span>
            <Button disabled={disabled} variant={"default"} onClick={() => fields.remove(index)}>
                <Icon glyph="fa-times"/>
            </Button>
        </div>
    );
});
