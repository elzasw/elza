import { DescItem } from "typings/DescItem";

export type IDescItemBaseProps = {
    hasSpecification: boolean;
    descItem: IDescItemStructure;
    locked: boolean;
    readMode: boolean;
    cal: boolean;
    typePrefix: string;
    readOnly: boolean;
    versionId: number;
    fundId: number;
    repeatable: boolean;
    singleDescItemTypeEdit: boolean;
    onChange: (value: any) => void;
    onBlur: (value: any) => void;
    onFocus: (value: any) => void;
    descItemFactory: Function;
};

export interface IDescItemStructure extends DescItem<any> {
    structureData: {
        id: number;
        value: string;
        complement: string;
        state: ArrStructureObjectState;
        assignable: boolean;
        errorDescription: string;
        typeCode: string;
    };
}

export type IDescItemError = {
    value: string;
    hasError: boolean;
};

export type ArrStructureObjectState = 'TEMP' | 'OK' | 'ERROR';

export type DescItemComponentProps<T> = {
    descItem: DescItem<T | undefined>;
    onChange: (newValue: T) => void;
    onBlur: () => void;
    onFocus: () => void;
    hasSpecification: boolean;
    locked: boolean;
    readMode: boolean;
    cal: boolean;
    typePrefix: string;
    readOnly: boolean;
    versionId: number;
    fundId: number;
    descItemFactory: Function;
    repeatable: boolean;
};
