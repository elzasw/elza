import React, {FC} from 'react';
import {AeDetailHeadGlobalVO, AeDetailHeadLocalVO, ApState, AuthRole} from '../../../api/generated/model';
import {connect} from 'react-redux';
/*
import {
  faArrowRight,
  faBan,
  faCheck,
  faEdit,
  faEye,
  faPen,
  faReply,
  faSave,
  faStar
} from '@fortawesome/free-solid-svg-icons';
*/
// import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
// import StyledButton from '../StyledButton';
import {ThunkDispatch} from 'redux-thunk';
import {Action} from 'redux';
/*
import {
  handleAmendRequest,
  handleApprove,
  handleApproveRequest,
  handleCancelEdit,
  handleChangeAeType,
  handleEdit,
  handleInvalidate,
  handleNewRecord,
  handleShowLocal
} from './DetailSiderActionsImpl';
*/
import * as H from 'history';
// import {AsyncClickHandler} from '../AsyncClickHandler';
import {RouteComponentProps, withRouter} from 'react-router';
import {NavLink} from 'react-router-dom';
// import AuthWrapper from '../../shared/rights/AuthWrapper';
import {Button} from 'react-bootstrap';
import Icon from "../../shared/icon/Icon";
import {ApAccessPointVO} from "../../../api/ApAccessPointVO";

export enum DetailActionsEnum {
  GLOBAL = 'global',
  EDIT = 'edit',
  APPROVE = 'approve'
}

type OwnProps = {
  isValid: boolean
  detail: ApAccessPointVO;
  type: DetailActionsEnum;
  globalDetailUrlPrefix?: string;
  editDetailUrlPrefix?: string;
  approveDetailUrlPrefix?: string;
};

type ComponentProps = OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & RouteComponentProps<any>;

const DetailSiderActions: FC<ComponentProps> = (
  {
    type,
    onEdit,
    onCancelEdit,
    onNewRecord,
    onAmendRequest,
    onApproveRequest,
    onApprove,
    onInvalidate,
    onShowLocalAction,
    onChangeAeType,
    detail,
    history,
    globalDetailUrlPrefix,
    authInfo,
    ...props
  }) => {

  const approveDisabled = () => {
    if(type === DetailActionsEnum.APPROVE){
      return detail.head.aeState !== ApState.RLCSTOAPPROVE;
    }
    return false;
  };

  const amendRequestDisabled = () => {
    if(type === DetailActionsEnum.APPROVE){
      return detail.head.aeState !== ApState.RLCSTOAPPROVE;
    }
    return true;
  };

    const edit = <Button onClick={() => onEdit(history)}><Icon glyph="fa-edit" /> Upravit</Button>;
  const invalidate = <Button onClick={onInvalidate}>
      <Icon glyph="fa-ban" /> Zneplatnit
  </Button>;

  const approve = <Button
    onClick={onApprove}
    disabled={!props.isValid || approveDisabled()}
  ><Icon glyph={"fa-check"} />{props.isValid ? "Schválit" : "Nelze shválit, entita není validní"}</Button>;

  const showLocal = <Button onClick={() => onShowLocalAction()}>
    <div className="fa-stack" style={{ fontSize: '10px' }}>
        <Icon glyph={"fa-eye"} fixedWidth className="fa-stack-2x"/>
        <Icon glyph={"fa-pen"} fixedWidth className="fa-stack-1x"
              style={{ marginLeft: '18px', marginTop: '11px' }}/>
    </div>
    Zobrazit rozpracovanou revizi archivní entity
</Button>;

  const canShowNewRecord = (): boolean => {
    if (detail && detail.head) {
      const sourceState = (detail.head as AeDetailHeadLocalVO).sourceState;
      return sourceState === null || sourceState === ApState.APSNEW;
    }
    return false;
  };

  let showGlobal;
  if (globalDetailUrlPrefix) {
    if (type === DetailActionsEnum.APPROVE || type === DetailActionsEnum.EDIT) {
      const globalId = (detail.head as AeDetailHeadLocalVO).globalId;
      if (!!globalId) {
        showGlobal = <NavLink to={globalDetailUrlPrefix + globalId}>
            <Button>
                <Icon glyph={"fa-eye"} /> Zobrazit poslední platnou revizi archivní entity
            </Button>
        </NavLink>;
      } else {
        showGlobal = <Button disabled={true}>
            <Icon glyph={"fa-eye"} /> Entita je nově zakládaná, ještě nezapsaná do CAM
          </Button>
      }
    }
  }

  let newRecord;
  if (canShowNewRecord()) {
    newRecord = <Button
      onClick={onNewRecord}
      disabled={!props.isValid}
    >
        <Icon glyph={"fa-save"} /> {props.isValid ? "Založit záznam do jádra CAM" : "Nelze založit záznam do jádra, entita není validní"}
    </Button>;
  }

  const approveRequest = <Button onClick={onApproveRequest}>
      <div className="fa-stack" style={{ fontSize: '10px' }}>
          <Icon glyph={"fa-check"} fixedWidth className="fa-stack-2x"/>
          <Icon glyph={"fa-arrow-right"} fixedWidth className="fa-stack-1x"
                           style={{ marginLeft: '-6px', marginTop: '13px' }}/>
      </div> Ke schválení
  </Button>;
  const cancelEdit = <Button onClick={onCancelEdit}>
      <Icon glyph={"fa-ban"} /> Zrušit úpravu
  </Button>;
  const amendRequest = <Button onClick={onAmendRequest} disabled={amendRequestDisabled()}>
      <Icon glyph={"fa-reply"} /> K doplnění
  </Button>;
  const changeAeType = <Button onClick={onChangeAeType}>
      <Icon glyph={"fa-star"} /> Změnit třídu
  </Button>;

  const userIsOwner = (): boolean => {
    const user = (detail.head as AeDetailHeadLocalVO).ownerUser;
    return authInfo && authInfo.id === user.id;
  };

  const isInvalid = detail.head.aeState === ApState.RLCSINVALID;
  const isSavedInGlobal = detail.head.aeState === ApState.RLCSSAVED;
  const isToApprove = detail.head.aeState === ApState.RLCSTOAPPROVE;
  return <div></div>;
/*
  return <>
    {type === DetailActionsEnum.GLOBAL && <>
      <AuthWrapper requiredRole={AuthRole.CAM2}>{edit}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM4}>{invalidate}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM3} and={() => (detail.head as AeDetailHeadGlobalVO).aeState === AeState.APSNEW}>{approve}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM2}>{showLocal}</AuthWrapper>
    </>}

    {type === DetailActionsEnum.EDIT && <>
      <AuthWrapper requiredRole={AuthRole.CAM1} and={userIsOwner}>{showGlobal}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM2} and={[userIsOwner, !isInvalid, !isSavedInGlobal]}>{newRecord}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM2} and={[userIsOwner, !isInvalid, !isSavedInGlobal]}>{approveRequest}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM2} and={[userIsOwner, !isInvalid, !isSavedInGlobal]}>{amendRequest}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM3} and={[() => !userIsOwner() && isToApprove]}>{approve}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM2} and={[userIsOwner, !isInvalid, !isSavedInGlobal]}>{cancelEdit}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM2} and={[userIsOwner, !isInvalid, !isSavedInGlobal]}>{changeAeType}</AuthWrapper>
    </>}

    {type === DetailActionsEnum.APPROVE && <>
      <AuthWrapper requiredRole={AuthRole.CAM1}>{showGlobal}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM3}>{amendRequest}</AuthWrapper>
      <AuthWrapper requiredRole={AuthRole.CAM3} and={() => !userIsOwner()}>{approve}</AuthWrapper>
    </>}

  </>;

 */
};

const mapStateToProps = ({ app, codelist }: any) => ({
  codelist: codelist.data,
  authInfo: app.authInfo.data
});

const mapDispatchToProps = (
  dispatch: ThunkDispatch<{}, {}, Action<string>>,
  ownProps: OwnProps
) => {
  const detail = ownProps.detail;

  return {
    onCancelEdit: () => {
      // handleCancelEdit(dispatch, ownProps.type, detail.head);
    },
    onChangeAeType: () => {
      // handleChangeAeType(dispatch, ownProps.type, detail.head);
    },
    onEdit: (history: H.History) => {
      // handleEdit(dispatch, ownProps.type, detail.head, history);
    },
    onShowLocalAction: () => {
      if (ownProps.editDetailUrlPrefix && ownProps.approveDetailUrlPrefix) {
        // return handleShowLocal(dispatch, ownProps.type, detail.head, ownProps.editDetailUrlPrefix, ownProps.approveDetailUrlPrefix);
      } else {
        return Promise.resolve();
      }
    },
    onApproveRequest: () => {
      // handleApproveRequest(dispatch, ownProps.type, detail.head);
    },
    onNewRecord: () => {
      // handleNewRecord(dispatch, ownProps.type, detail.head);
    },
    onAmendRequest: () => {
      // handleAmendRequest(dispatch, ownProps.type, detail.head);
    },
    onApprove: () => {
      // handleApprove(dispatch, ownProps.type, detail.head);
    },
    onInvalidate: () => {
      // handleInvalidate(dispatch, ownProps.type, detail.head);
    }
  };
};

export default withRouter(connect(
  mapStateToProps,
  mapDispatchToProps
)(DetailSiderActions));
