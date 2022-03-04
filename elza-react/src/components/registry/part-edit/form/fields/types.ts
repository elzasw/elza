export interface CommonFieldProps<Type> {
    name: string;
    label: string;
    disabled?: boolean;
    prevItem?: Type;
    disableRevision?: boolean;
}
