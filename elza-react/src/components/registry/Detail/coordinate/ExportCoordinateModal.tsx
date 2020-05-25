import React, {useState} from 'react';
/*
import {Button, Col, Radio, Row} from 'react-bootstrap';
import {ModalFormProps} from "../../../shared/reducers/modal/ModalActions";
import ModalFormBody from "../../modal/ModalFormBody";
import ModalFormFooter from "../../modal/ModalFormFooter";
import {AeItemCoordinatesVO, FileType} from "../../../../api/generated/model";
import {CodelistState} from "../../../shared/reducers/codelist/CodelistReducer";
import {connect} from "react-redux";
import * as EntitiesClientApiCall from "../../../api/call/EntitiesClientApiCall";
import DownloadButton from "../../DownloadButton";
import classNames from "classnames";
import {RadioChangeEvent} from "antd/lib/radio";

type Props = {
  item: AeItemCoordinatesVO,
  onClose: () => void,
  globalEntity: boolean,
} & ModalFormProps & ReturnType<typeof mapStateToProps>;

const ExportCoordinateModal = ({
                                 item,
                                 onClose,
                                 globalEntity,
                                 codelist
                               }: Props) => {
  const itemType = codelist.itemTypesMap[item.itemTypeId];

  const [format, setFormat] = useState(1);

  const handleExport = async (item: AeItemCoordinatesVO) => {
    let type;
    switch (format) {
      case 1:
        type = FileType.WKT;
        break;
      case 2:
        type = FileType.KML;
        break;
      case 3:
        type = FileType.GML;
        break;
      default:
        type = FileType.WKT;
    }


    if (globalEntity) {
      return EntitiesClientApiCall.pureApi.globalExportCoordinates(type, item.uuid as string);
    } else {
      return EntitiesClientApiCall.pureApi
        .exportCoordinates(type, item.id as number);
    }
  };

  const onChange = (e: RadioChangeEvent) => {
    setFormat(parseInt(e.target.value));
  };

  return <><ModalFormBody>
    <Row>
      <Col>Zvolte požadovaný formát exportovaných souřadnic:</Col>
    </Row>
    <Row className="pt-2">
      <Col>
        <Radio.Group onChange={onChange} value={format} buttonStyle="solid">
          <Radio.Button value={1}>
            Formát WKT
          </Radio.Button>
          <Radio.Button value={2}>
            Formát KML
          </Radio.Button>
          <Radio.Button value={3}>
            Formát GML
          </Radio.Button>
        </Radio.Group>
      </Col>
    </Row>
  </ModalFormBody>
    <ModalFormFooter onClose={onClose} customButtons={true}>
      <DownloadButton className={classNames("side-container-button", "mb-1")} title={"Exportovat"}
                      onDownload={() => handleExport(item)} type="primary" htmlType={"submit"}>
        Exportovat
      </DownloadButton>
      <Button key="main-cancel" onClick={onClose}>Zavřít</Button>
    </ModalFormFooter>
  </>;
};

const mapStateToProps = (state: { codelist: CodelistState }) => ({
  codelist: state.codelist.data
});

export default connect(mapStateToProps)(ExportCoordinateModal);
*/
