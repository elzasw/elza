export type IDescItemBaseProps = {
    hasSpecification: boolean,
    descItem: IDescItemStructure,
    locked: boolean,
    readMode: boolean,
    cal: boolean,
    typePrefix: string,
    readOnly: boolean,
    versionId: number,
    fundId: number,
    repeatable: boolean,
    singleDescItemTypeEdit: boolean,
    onChange: (value: any) => void,
    onBlur: (value: any) => void,
    onFocus: (value: any) => void,
    descItemFactory: Function
}

export interface IDescItemProps<T> {
    "@class": string,
    prevValue: null,
    hasFocus: boolean,
    touched: boolean,
    visited: boolean,
    saving: boolean,
    value: T,
    error: IDescItemError,
    addedByUser: boolean,
    formKey: string,
    position: number,
    "undefined": boolean
}

export interface IDescItemStructure extends IDescItemProps<any> {
    structureData: {
        id: number,
        value: string,
        complement: string,
        state: ArrStructureObjectState,
        assignable: boolean,
        errorDescription: string,
        typeCode: string
    }
};

export type IDescItemError = {
    value: string,
    hasError: boolean
};

export type ArrStructureObjectState = "TEMP" | "OK" | "ERROR";
