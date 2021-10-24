import { FormApi } from 'final-form';
import { FieldRenderProps } from 'react-final-form';

export const getValueChangeMutators = <DataType,>(callback: (data: DataType) => void) => ({
        onValueChange: (_name:string, state: any) => { callback(state.formState.values);}
})

/**
* @param form - form instance
* @param props - props needed if called from inside of a field. mostly used for 'isDirty' check
*/

export const handleValueUpdate = (
    form: FormApi<Record<string, any>>, 
    props?: FieldRenderProps<any>
) => {
    if(!props || props.meta.dirty){
        form.mutators.onValueChange?.(name);
    }
}
