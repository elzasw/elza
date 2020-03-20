import { FundScope } from 'components/arr/FundForm.d';
import { Autocomplete, i18n, Icon } from 'components/shared';
import { Button } from 'components/ui';
import React, { memo } from 'react';
import { FieldArrayFieldsProps } from 'redux-form';

interface IScopesFieldProps {
    fields: FieldArrayFieldsProps<FundScope>
    scopeList: FundScope[]
}

export const ScopesField: React.FC<IScopesFieldProps> = memo(({fields, ...props}) => (
    <>
        <Autocomplete
            tags
            label={i18n('arr.fund.regScope')}
            items={props.scopeList}
            getItemId={item => (item ? item.id : null)}
            getItemName={item => (item ? item.name : '')}
            onChange={(scope: FundScope) => {
                fields.push(scope);
            }}
        />
        <div className="selected-data-container">
            {fields.map((field, index) =>
                <ScopeField key={index} index={index} fields={fields}/>,
            )}
        </div>
    </>
));

interface IScopeFieldProps {
    index: number
    fields: IScopesFieldProps['fields']
}

const ScopeField: React.FC<IScopeFieldProps> = memo(({index, fields}) => (
    <div className="selected-data" key={index}>
        <span>{fields.get(index).name}</span>
        <Button onClick={() => fields.remove(index)}>
            <Icon glyph="fa-times"/>
        </Button>
    </div>
));
