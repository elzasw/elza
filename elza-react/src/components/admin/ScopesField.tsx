import { FundScope } from '../../types';
import { Autocomplete, i18n, Icon } from 'components/shared';
import { Button } from 'components/ui';
import { Form } from 'react-bootstrap';
import React, { memo } from 'react';
import {FieldArrayFieldsProps, WrappedFieldArrayProps} from 'redux-form';
import objectById from "../../shared/utils/objectById";

interface IScopesFieldProps extends WrappedFieldArrayProps<string> {
    scopeList: FundScope[]
}

export const ScopesField: React.FC<IScopesFieldProps> = memo(({fields, meta, ...props}) => (
    <>
        <Autocomplete
            tags
            label={i18n('arr.fund.regScope')}
            items={props.scopeList || []}
            getItemId={item => (item ? item.id : null)}
            getItemName={item => (item ? item.name : '')}
            onChange={(scope: FundScope) => {
                fields.push(scope.code);
            }}
        />
        {meta.error && <Form.Control.Feedback type="invalid">
            {meta.error}
        </Form.Control.Feedback>}
        <div className="selected-data-container">
            {fields.map((field, index) =>
                <ScopeField key={index} index={index} fields={fields} scopeList={props.scopeList} />,
            )}
        </div>
    </>
));

interface IScopeFieldProps {
    index: number
    fields: IScopesFieldProps['fields']
    scopeList: FundScope[]
}

const ScopeField: React.FC<IScopeFieldProps> = memo(({index, fields, scopeList}) => (
    <div className="selected-data" key={index}>
        <span>{objectById(scopeList, fields.get(index), 'code').name}</span>
        <Button variant={"default"} onClick={() => fields.remove(index)}>
            <Icon glyph="fa-times"/>
        </Button>
    </div>
));
