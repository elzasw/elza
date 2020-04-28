import React, {FC} from 'react';
import {AeDetailHeadGlobalVO, AeDetailHeadLocalVO, AeDetailVO, AeState, AuthRole} from '../../api/generated/model';
import {connect} from 'react-redux';
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
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import StyledButton from '../StyledButton';
import {ThunkDispatch} from 'redux-thunk';
import {Action} from 'redux';
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
import * as H from 'history';
import {AsyncClickHandler} from '../AsyncClickHandler';
import {RouteComponentProps, withRouter} from 'react-router';
import {NavLink} from 'react-router-dom';
import AuthWrapper from '../../shared/rights/AuthWrapper';

export enum DetailActionsEnum {
  GLOBAL = 'global',
  EDIT = 'edit',
  APPROVE = 'approve'
}

type OwnProps = {
  isValid: boolean
  detail: AeDetailVO;
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
      return detail.head.aeState !== AeState.RLCSTOAPPROVE;
    }
    return false;
  };

  const amendRequestDisabled = () => {
    if(type === DetailActionsEnum.APPROVE){
      return detail.head.aeState !== AeState.RLCSTOAPPROVE;
    }
    return true;
  };

  const edit = <StyledButton icon={faEdit} title="Upravit" onClick={() => onEdit(history)}/>;
  const invalidate = <StyledButton icon={faBan} title="Zneplatnit" onClick={onInvalidate}/>;

  const approve = <StyledButton
    icon={faCheck}
    title={props.isValid ? "Schválit" : "Nelze shválit, entita není validní"}
    onClick={onApprove}
    disabled={!props.isValid || approveDisabled()}
  />;

  const showLocal = <AsyncClickHandler onClick={() => onShowLocalAction()}>
    <StyledButton
      iconElement={<div className="fa-stack" style={{ fontSize: '10px' }}>
        <FontAwesomeIcon icon={faEye} fixedWidth className="fa-stack-2x"/>
        <FontAwesomeIcon icon={faPen} fixedWidth className="fa-stack-1x"
                         style={{ marginLeft: '18px', marginTop: '11px' }}/>
      </div>}
      title="Zobrazit rozpracovanou revizi archivní entity"
    />
  </AsyncClickHandler>;

  const canShowNewRecord = (): boolean => {
    if (detail && detail.head) {
      const sourceState = (detail.head as AeDetailHeadLocalVO).sourceState;
      return sourceState === null || sourceState === AeState.APSNEW;
    }
    return false;
  };

  let showGlobal;
  if (globalDetailUrlPrefix) {
    if (type === DetailActionsEnum.APPROVE || type === DetailActionsEnum.EDIT) {
      const globalId = (detail.head as AeDetailHeadLocalVO).globalId;
      if (!!globalId) {
        showGlobal = <NavLink to={globalDetailUrlPrefix + globalId}>
          <StyledButton icon={faEye} title={'Zobrazit poslední platnou revizi archivní entity'}/>
        </NavLink>;
      } else {
        showGlobal = <StyledButton disabled={true} icon={faEye} title="Entita je nově zakládaná, ještě nezapsaná do CAM."/>;
      }
    }
  }

  let newRecord;
  if (canShowNewRecord()) {
    newRecord = <StyledButton
      icon={faSave}
      title={props.isValid ? "Založit záznam do jádra CAM" : "Nelze založit záznam do jádra, entita není validní"}
      onClick={onNewRecord}
      disabled={!props.isValid}
    />;
  }

  const approveRequest = <StyledButton title="Ke schválení" onClick={onApproveRequest}
                                       iconElement={<div className="fa-stack" style={{ fontSize: '10px' }}>
                                         <FontAwesomeIcon icon={faCheck} fixedWidth className="fa-stack-2x"/>
                                         <FontAwesomeIcon icon={faArrowRight} fixedWidth className="fa-stack-1x"
                                                          style={{ marginLeft: '-6px', marginTop: '13px' }}/>
                                       </div>}/>;
  const cancelEdit = <StyledButton icon={faBan} title="Zrušit úpravu" onClick={onCancelEdit}/>;
  const amendRequest = <StyledButton icon={faReply} title="K doplnění" onClick={onAmendRequest} disabled={amendRequestDisabled()}/>;
  const changeAeType = <StyledButton icon={faStar} title="Změnit třídu" onClick={onChangeAeType}/>;

  const userIsOwner = (): boolean => {
    const user = (detail.head as AeDetailHeadLocalVO).ownerUser;
    return authInfo && authInfo.id === user.id;
  };

  const isInvalid = detail.head.aeState === AeState.RLCSINVALID;
  const isSavedInGlobal = detail.head.aeState === AeState.RLCSSAVED;
  const isToApprove = detail.head.aeState === AeState.RLCSTOAPPROVE;

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
      handleCancelEdit(dispatch, ownProps.type, detail.head);
    },
    onChangeAeType: () => {
      handleChangeAeType(dispatch, ownProps.type, detail.head);
    },
    onEdit: (history: H.History) => {
      handleEdit(dispatch, ownProps.type, detail.head, history);
    },
    onShowLocalAction: () => {
      if (ownProps.editDetailUrlPrefix && ownProps.approveDetailUrlPrefix) {
        return handleShowLocal(dispatch, ownProps.type, detail.head, ownProps.editDetailUrlPrefix, ownProps.approveDetailUrlPrefix);
      } else {
        return Promise.resolve();
      }
    },
    onApproveRequest: () => {
      handleApproveRequest(dispatch, ownProps.type, detail.head);
    },
    onNewRecord: () => {
      handleNewRecord(dispatch, ownProps.type, detail.head);
    },
    onAmendRequest: () => {
      handleAmendRequest(dispatch, ownProps.type, detail.head);
    },
    onApprove: () => {
      handleApprove(dispatch, ownProps.type, detail.head);
    },
    onInvalidate: () => {
      handleInvalidate(dispatch, ownProps.type, detail.head);
    }
  };
};

export default withRouter(connect(
  mapStateToProps,
  mapDispatchToProps
)(DetailSiderActions));
