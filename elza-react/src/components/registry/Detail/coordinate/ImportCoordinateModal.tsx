import React, {useState} from 'react';
/*
import {Button, Col, Form, Radio, Row, Upload} from 'react-bootstrap';
import {ModalFormProps} from "../../../shared/reducers/modal/ModalActions";
import ModalFormBody from "../../modal/ModalFormBody";
import ModalFormFooter from "../../modal/ModalFormFooter";
import {connect} from "react-redux";
import {ConfigProps, InjectedFormProps, reduxForm, SubmitHandler} from "redux-form";
import {UploadFile} from "antd/lib/upload/interface";
import {RadioChangeEvent} from "antd/lib/radio";
import {ApikeyVO, FileType} from "../../../../api/generated/model";

const FORM_NAME = 'importCoordinatesForm';


const formConfig: ConfigProps<ApikeyVO, ModalFormProps> = {
  form: FORM_NAME
};

type Props = {
  message: string,
  confirmLabel: string,
  comment: boolean,
  onClose: () => void,
  handleSubmit: SubmitHandler<FormData, any, any>;
  onSubmit: (data: any) => void;
} & ModalFormProps & InjectedFormProps;

const ImportCoordinateModal = ({
                                 message,
                                 confirmLabel,
                                 handleSubmit,
                                 comment,
                                 onClose,
                                 submitting,
                                 onSubmit
                               }: Props) => {


  const [fileList, setFileList] = useState<Array<UploadFile>>([]);

  const uploadProps = {
    onRemove: (file: UploadFile) => {
      setFileList([]);
    },
    beforeUpload: () => {
      return false;
    },
    fileList,
  };

  const [format, setFormat] = useState(1);

  const onChange = (e: RadioChangeEvent) => {
    setFormat(parseInt(e.target.value));
  };

  const customHandleSubmit = (e: any) => {
    let type: FileType;
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

    handleSubmit(data => {
      return onSubmit({fileList, type});
    })(e);
  };

  return <Form layout="vertical" onSubmit={customHandleSubmit}>
    <ModalFormBody>
      <Row>
        <Col>
          <Upload {...uploadProps} onChange={data => {setFileList([data.fileList[data.fileList.length-1]]);}}>
            <Button>
              Vyberte soubor
            </Button>
          </Upload>
        </Col>
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
    <ModalFormFooter
      disabled={submitting}
      onClose={onClose}
      okText={confirmLabel}
    />
  </Form>;
};

export default connect()(reduxForm<any, any>(formConfig)(ImportCoordinateModal));
*/
