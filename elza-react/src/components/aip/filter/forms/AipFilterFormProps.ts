export type AipFilterFormProps = {
    item: any;
    onSubmit: (e) => void;
    onClose: () => void;
    selectValues?: SelectionOptions[];
}

export type SelectionOptions = {
    label: string;
    value: string | boolean | number;
}
