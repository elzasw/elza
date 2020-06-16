import {Permission} from "./Permission";
import {UsrGroupVO} from "./UsrGroupVO";
import {UsrUserVO} from "./UsrUserVO";
import {ArrFundBaseVO} from "./ArrFundBaseVO";
import {ArrNodeVO} from "./ArrNodeVO";
import {ApScopeVO} from "./ApScopeVO";
import {WfIssueListBaseVO} from "./WfIssueListBaseVO";

export interface UsrPermissionVO {
    /** Identifikátor. */
    id: number;

    /**
     * Je právo zděděné ze skupiny?
     * TODO: inherited a groupId jsou zjevně redundantní
     */
    inherited: boolean;

    /** Pokud je právo zděděné, je zde id skupiny. */
    groupId: number;

    /** Typ oprávnění. */
    permission: Permission;

    /** AS, ke kterému se vztahuje oprávnění. */
    fund: ArrFundBaseVO;

    /** Skupina, ke které se vztahuje oprávnění. */
    groupControl: UsrGroupVO;

    /** Uživatel, ke kterému se vztahuje oprávnění. */
    userControl: UsrUserVO;

    /** Scope, ke kterému se vztahuje oprávnění. */
    scope: ApScopeVO;

    /** JP, ke které se vztahuje oprávnění. */
    node: ArrNodeVO;

    /** Protokol, ke kterému se vztahuje oprávnění. */
    issueList: WfIssueListBaseVO;
}
