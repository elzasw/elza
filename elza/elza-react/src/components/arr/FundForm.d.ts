export type FundScope = {id: number, code: string, name: string, language: null | string};

export interface IFundFormData {
    name: string
    internalCode: string
    institutionId: string
    ruleSetId: string
    dateRange: string
    apScopes: FundScope[]
    fundAdmins: string[]
}
