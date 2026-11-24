import { IssueListVO } from 'types';
import { useAppSelector } from 'utils/hooks';

interface IssueProtocolsStore {
    fetched: boolean;
    rows: IssueListVO[];
    /** Fund the protocols were loaded for. */
    parent: number | null;
}

/** Protocols of the fund; none while they load, as the shared list in the store may still hold another fund's. */
export function useFundProtocols(fundId: number | undefined): IssueListVO[] | undefined {
    const issueProtocols = useAppSelector(({ app }) => app.issueProtocols as IssueProtocolsStore);
    const areProtocolsOfFund = issueProtocols.fetched && issueProtocols.parent === fundId;
    return areProtocolsOfFund ? issueProtocols.rows : undefined;
}
