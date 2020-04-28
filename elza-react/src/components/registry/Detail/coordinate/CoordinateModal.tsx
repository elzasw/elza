import React from 'react';
import {Button, Col, Row} from 'react-bootstrap';
/*
import * as ModalActions from "../../../shared/reducers/modal/ModalActions";
import ModalFormBody from "../../modal/ModalFormBody";
import ModalFormFooter from "../../modal/ModalFormFooter";
import {AeItemCoordinatesVO} from "../../../../api/generated/model";
import {CodelistState} from "../../../shared/reducers/codelist/CodelistReducer";
import {connect} from "react-redux";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faFileExport} from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
import {Action, Dispatch} from "redux";
import ExportCoordinateModal from "./ExportCoordinateModal";

type Props = {
  item: AeItemCoordinatesVO,
  onClose: () => void,
  globalEntity: boolean,
} & ModalActions.ModalFormProps & ReturnType<typeof mapStateToProps> & ReturnType<typeof mapDispatchToProps>;

const CoordinateModal = ({
                           item,
                           onClose,
                           showExportDialog,
                           globalEntity,
                           codelist
                         }: Props) => {
  const itemType = codelist.itemTypesMap[item.itemTypeId];

  return <><ModalFormBody>
    <Row>
      <Col>
        <div className="detail-item">
          <div className="detail-item-header mt-1">
            {itemType ? itemType.name : `UNKNOWN_AE_TYPE: ${item.itemTypeId}`}
          </div>
          <Row className="detail-item-content">
            <Col xs={22}>
              {item.textValue}
            </Col>
            <Col xs={2}>
              <Button className={classNames("side-container-button", "mb-1")} title={"Exportovat"} size="small"
                      onClick={() => showExportDialog(item, globalEntity)}>
                <FontAwesomeIcon fixedWidth className="icon" icon={faFileExport}/>
              </Button>
            </Col>
          </Row>
        </div>
      </Col>
    </Row>
  </ModalFormBody>
    <ModalFormFooter
      onClose={onClose}
      cancelText={"Zavřít"}
      hideOk
    />
  </>;
};

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
  showExportDialog: (item: AeItemCoordinatesVO, globalEntity: boolean) =>
    dispatch(
      ModalActions.showForm(ExportCoordinateModal, {
          onCancel: () => dispatch(ModalActions.hide()),
          item,
          globalEntity
        },
        {
          title: 'Exportovat souřadnice',
        })
    ),
});

const mapStateToProps = (state: { codelist: CodelistState }) => ({
  codelist: state.codelist.data
});

export default connect(mapStateToProps, mapDispatchToProps)(CoordinateModal);
*/
