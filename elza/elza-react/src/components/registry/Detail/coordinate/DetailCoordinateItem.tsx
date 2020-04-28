import React from 'react';
import {Button} from 'react-bootstrap';
import {AeItemCoordinatesVO} from "../../../../api/generated/model";
//import {faFileExport} from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
//import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {connect} from "react-redux";
import {Action, Dispatch} from "redux";
//import * as ModalActions from "../../../shared/reducers/modal/ModalActions";
//import CoordinateModal from "./CoordinateModal";
//import ExportCoordinateModal from "./ExportCoordinateModal";
import Icon from '../../../shared/icon/Icon';

interface Props extends ReturnType<typeof mapDispatchToProps> {
  item: AeItemCoordinatesVO;
  globalEntity: boolean;
}

const DetailCoordinateItem: React.FC<Props> = props => {
  const getLabel = (value: string) => {
    return value.split(" ")[0]
  };

  return <>
    <Button variant="link" onClick={() => props.showCoordinateDetail(props.item)}>{getLabel(props.item.textValue)}</Button>
    <Button className={classNames("side-container-button", "mb-1")} title={"Exportovat"} size="sm"
            onClick={() => props.showExportDialog(props.item, props.globalEntity)}>
      <Icon fixedWidth className="icon"/>
    </Button>
  </>
};

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
  showCoordinateDetail: (item: AeItemCoordinatesVO) => null
    /*dispatch(
      ModalActions.showForm(CoordinateModal, {
          onCancel: () => dispatch(ModalActions.hide()),
          item
        },
        {
          title: 'Detail souřadnice',
        })
    )*/,
  showExportDialog: (item: AeItemCoordinatesVO, globalEntity: boolean) => null
    /*dispatch(
      ModalActions.showForm(ExportCoordinateModal, {
          onCancel: () => dispatch(ModalActions.hide()),
          item,
          globalEntity
        },
        {
          title: 'Exportovat souřadnice',
        })
    )*/,
});
export default connect(null, mapDispatchToProps)(DetailCoordinateItem);
