const BLANK = "";
// import * as ModalActions from "../../shared/reducers/modal/ModalActions";
// import ConfirmModal from "../modal/ConfirmModal";
/*import {AeDetailHeadGlobalVO, AeDetailHeadLocalVO, AeDetailHeadVO, AeDetailVO, Area} from "../../../api/generated/model";
import * as Constants from "../../constants";
import {DetailActionsEnum} from "./DetailSiderActions";
import {ThunkDispatch} from "redux-thunk";
import {Action} from "redux";
import * as H from 'history';
import InvalidateEntityModalForm from "../modal/InvalidateEntityModalForm";
import CreateEntryModalForm from "../modal/CreateEntryModalForm";
import ChangeTypeModal from "../modal/ChangeTypeModal";
import SelectLocalEntityModal from "../modal/SelectLocalEntityModal";
import * as detailActions from "../../shared/reducers/detail/DetailActions";
import * as infiniteListActions from "../../shared/reducers/list/infinite/InfiniteListActions";
import * as EntitiesClientApiCall from "../../../api/call/EntitiesClientApiCall";

export const handleChangeAeType = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO) => {
  const localId = (head as AeDetailHeadLocalVO).id;
  dispatch(
    ModalActions.showForm(ChangeTypeModal, {
      confirmLabel: 'Uložit',
      rootTypeId: head.aeTypeId,
      initialValues: {
        typeId: head.aeTypeId
      },
      onSubmit: (formData: any) => {
        return EntitiesClientApiCall.formApi
          .changeTypeArchiveEntity(localId, formData.typeId);
      },
      onSubmitSuccess: () => {
        dispatch(detailActions.invalidate(Constants.EDIT_AE_DETAIL_AREA, localId));
        dispatch(detailActions.invalidate(Constants.GLOBAL_EDIT_AE_DETAIL_AREA, localId));
        dispatch(infiniteListActions.invalidate(Constants.EDIT_AE_LIST_AREA));
        dispatch(ModalActions.hide());
      }
    }, {
      title: 'Změna podtřídy',
    }));
};

export const handleEdit = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO, history: H.History) => {
  const globalId = (head as AeDetailHeadGlobalVO).id;
  dispatch(
    ModalActions.showForm(ConfirmModal, {
      message: 'Přejete si vytvořit novou revizi archivní entity k úpravě?',
      confirmLabel: 'Vytvořit',
      comment: false,
      onSubmit: () => {
        return EntitiesClientApiCall.formApi
          .takeArchiveEntity(globalId)
          .then(result => result.data);
      },
      onSubmitSuccess: (detail: AeDetailVO) => {
        const localId = (detail.head as AeDetailHeadLocalVO).id;
        history.push(`/global/${globalId}/edit/${localId}`);
        dispatch(ModalActions.hide());
        dispatch(detailActions.invalidate(Constants.GLOBAL_AE_DETAIL_AREA, localId));
        dispatch(infiniteListActions.invalidate(Constants.GLOBAL_AE_LIST_AREA));
        dispatch(detailActions.invalidate(Constants.APPROVE_GLOBAL_AE_DETAIL_AREA, localId));
      }
    }, {
      title: 'Upravit archivní entitu',
    }));
};

export const handleNewRecord = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO) => {
  const lHead = head as AeDetailHeadLocalVO;
  dispatch(
    ModalActions.showForm(CreateEntryModalForm, {
      initialValues: {},
      onSubmit: (formData: any) => {
          let comment = undefined;
          if (formData.comment) {
              comment = {
                  comment: formData.comment
              }
          }
        return EntitiesClientApiCall.formApi
          .saveArchiveEntity(lHead.id, comment)
      },
      onSubmitSuccess: () => {
        dispatch(detailActions.invalidate(Constants.EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(detailActions.invalidate(Constants.GLOBAL_EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(infiniteListActions.invalidate(Constants.GLOBAL_AE_LIST_AREA));
        dispatch(infiniteListActions.invalidate(Constants.EDIT_AE_LIST_AREA));
        dispatch(ModalActions.hide());
      }
    }, {
      title: 'Nový záznam do jádra'
    }));
};

export const handleInvalidate = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO) => {
  const gHead = head as AeDetailHeadGlobalVO;
  dispatch(
    ModalActions.showForm(InvalidateEntityModalForm, {
      initialValues: {
        onlyMainPart: false,
        area: Area.ALLNAMES
      },
      onSubmit: (formData: any) => {
        return EntitiesClientApiCall.formApi
          .globalInvalidateArchiveEntity(gHead.id, formData.code);
      },
      onSubmitSuccess: () => {
        dispatch(detailActions.invalidate(Constants.GLOBAL_AE_DETAIL_AREA, gHead.id));
        dispatch(detailActions.invalidate(Constants.EDIT_GLOBAL_AE_DETAIL_AREA, gHead.id));
        dispatch(detailActions.invalidate(Constants.APPROVE_GLOBAL_AE_DETAIL_AREA, gHead.id));
        dispatch(infiniteListActions.invalidate(Constants.GLOBAL_AE_LIST_AREA));
        dispatch(ModalActions.hide());
      }
    }, {
      title: 'Zneplatnit archivní entitu',
      width: "800px"
    }));
};

export const handleShowGlobal = async (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO, history: H.History) => {
  const globalId = (head as AeDetailHeadLocalVO).globalId;
  history.push(`/global/${globalId}`);
};

export const handleShowLocal = async (
  dispatch: ThunkDispatch<{}, {}, Action<string>>,
  type: DetailActionsEnum,
  head: AeDetailHeadVO,
  editDetailUrlPrefix: string,
  approveDetailUrlPrefix: string
) => {
  const globalId = (head as AeDetailHeadGlobalVO).id;
  const result = await EntitiesClientApiCall.standardApi
    .getUnfinishedEntities(globalId)
    .then(x => x.data);

  let dialogCount = 0;
  if (result.entity) {
    dialogCount++;
  }
  if (result.list) {
    dialogCount += result.list.length;
  }

  if (dialogCount === 0) {
    // TODO - dodelat na hezky dialog
    alert("Entita nemá rozpracovanou revizi")
  } else {
    dispatch(ModalActions.showForm(SelectLocalEntityModal, {
      unfinishedEntities: result,
      editDetailUrlPrefix,
      approveDetailUrlPrefix
    }, {
      title: 'Upravované archivní entity a archivní entity ke schválení',
      width: "800px"
    }));
  }
};

export const handleAmendRequest = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO) => {
  const lHead = head as AeDetailHeadLocalVO;
  dispatch(
    ModalActions.showForm(ConfirmModal, {
      message: 'Přejete si vrátit archivní entitu k doplnění?',
      confirmLabel: 'Vrátit',
      comment: true,
      onSubmit: (formData: any) => {

        let comment = undefined;
        if (formData.comment) {
          comment = {
            comment: formData.comment
          }
        }
        return EntitiesClientApiCall.formApi
          .toAmendArchiveEntity(lHead.id, comment);
      },
      onSubmitSuccess: () => {
        dispatch(detailActions.invalidate(Constants.EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(detailActions.invalidate(Constants.GLOBAL_EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(infiniteListActions.invalidate(Constants.EDIT_AE_LIST_AREA));
        dispatch(ModalActions.hide());
      }
    }, {
      title: 'Vrátit k doplnění',
    }));
};

export const handleApproveRequest = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO) => {
  const lHead = head as AeDetailHeadLocalVO;
  dispatch(
    ModalActions.showForm(ConfirmModal, {
      message: 'Přejete si předat archivní entitu ke schválení?',
      confirmLabel: 'Ke schválení',
      comment: true,
      onSubmit: (formData: any) => {

        let comment = undefined;
        if (formData.comment) {
          comment = {
            comment: formData.comment
          }
        }
        return EntitiesClientApiCall.formApi
          .toApproveArchiveEntity(lHead.id, comment);
      },
      onSubmitSuccess: () => {
        dispatch(ModalActions.hide());
        dispatch(detailActions.invalidate(Constants.EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(detailActions.invalidate(Constants.GLOBAL_EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(infiniteListActions.invalidate(Constants.EDIT_AE_LIST_AREA));
        dispatch(infiniteListActions.invalidate(Constants.APPROVE_AE_LIST_AREA));
      }
    }, {
      title: 'Ke schválení',
    }));
};

export const handleCancelEdit = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO) => {
  const lHead = head as AeDetailHeadLocalVO;
  dispatch(
    ModalActions.showForm(ConfirmModal, {
      message: 'Přejete si zrušit úpravy archivní entity?',
      confirmLabel: 'Zrušit',
      comment: true,
      onSubmit: (formData: any) => {

        let comment = undefined;
        if (formData.comment) {
          comment = {
            comment: formData.comment
          }
        }
        return EntitiesClientApiCall.formApi
          .invalidateArchiveEntity(lHead.id, comment);
      },
      onSubmitSuccess: () => {
        dispatch(detailActions.invalidate(Constants.EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(detailActions.invalidate(Constants.GLOBAL_AE_DETAIL_AREA, lHead.id));
        dispatch(detailActions.invalidate(Constants.GLOBAL_EDIT_AE_DETAIL_AREA, lHead.id));
        dispatch(infiniteListActions.invalidate(Constants.EDIT_AE_LIST_AREA));
        dispatch(ModalActions.hide())
      }
    }, {
      title: 'Zrušit úpravy archivní entity',
    }));
};

export const handleApprove = (dispatch: ThunkDispatch<{}, {}, Action<string>>, type: DetailActionsEnum, head: AeDetailHeadVO) => {
  const gHead = head as AeDetailHeadGlobalVO;
  const lHead = head as AeDetailHeadLocalVO;
  dispatch(
    ModalActions.showForm(ConfirmModal, {
      message: 'Přejete si schválit archivní entitu?',
      confirmLabel: 'Schválit',
      comment: type !== DetailActionsEnum.GLOBAL,
      onSubmit: (formData: any) => {
        if (type === DetailActionsEnum.GLOBAL) {
          return EntitiesClientApiCall.formApi
            .globalApproveArchiveEntity(gHead.id);
        } else if (type === DetailActionsEnum.EDIT || type === DetailActionsEnum.APPROVE) {
          let comment = undefined;
          if (formData.comment) {
            comment = {
              comment: formData.comment
            }
          }

          return EntitiesClientApiCall.formApi
            .approveArchiveEntity(lHead.id, comment);
        }
      },
      onSubmitSuccess: () => {
        if (type === DetailActionsEnum.EDIT || type === DetailActionsEnum.APPROVE) {
          dispatch(detailActions.invalidate(Constants.EDIT_AE_DETAIL_AREA, lHead.id));
          dispatch(detailActions.invalidate(Constants.EDIT_GLOBAL_AE_DETAIL_AREA, lHead.id));
          dispatch(infiniteListActions.invalidate(Constants.EDIT_AE_LIST_AREA));
          dispatch(detailActions.invalidate(Constants.APPROVE_AE_DETAIL_AREA, lHead.id));
          dispatch(detailActions.invalidate(Constants.APPROVE_GLOBAL_AE_DETAIL_AREA, lHead.id));
          dispatch(infiniteListActions.invalidate(Constants.APPROVE_AE_LIST_AREA));
        }

        if (type === DetailActionsEnum.GLOBAL) {
          dispatch(detailActions.invalidate(Constants.GLOBAL_AE_DETAIL_AREA, lHead.id));
          dispatch(detailActions.invalidate(Constants.EDIT_GLOBAL_AE_DETAIL_AREA, lHead.id));
          dispatch(detailActions.invalidate(Constants.APPROVE_GLOBAL_AE_DETAIL_AREA, lHead.id));
          dispatch(infiniteListActions.invalidate(Constants.EDIT_AE_LIST_AREA));
        }

        dispatch(ModalActions.hide());
      },
    }, {
      title: 'Schválit archivní entitu',
    }));
};
*/
