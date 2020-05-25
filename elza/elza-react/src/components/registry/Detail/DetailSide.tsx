import React, { useState } from 'react';
import { Button, Col, Row} from 'react-bootstrap';
// import Row from '../Row';
import { connect } from 'react-redux';
// import * as ModalActions from '../../shared/reducers/modal/ModalActions';
import { Action, Dispatch } from 'redux';
// import SharedModal, { SharedModalProps } from '../SharedModal';
import './DetailSide.scss';
// import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
// import { IconDefinition } from '@fortawesome/fontawesome-common-types';
/*
import {
  faEye,
  faSave,
  faCheck,
  faChevronLeft,
  faBan,
  faChevronUp,
  faChevronDown,
  faEdit,
  faReply
} from '@fortawesome/free-solid-svg-icons';
*/
// import StyledButton from "../StyledButton";
import Icon from "../../shared/icon/Icon";

type Props = ReturnType<typeof mapDispatchToProps>;

const DetailSide = (props: Props) => {
  const [editMode, setEditMode] = useState<boolean>(true);

  const onEyeClick = () => {
    setEditMode(!editMode);
  };

  const onEditClick = () => {
    /*props.onAdd({
      title: 'Upravit archivní entitu',
      body: 'Přejete si vytvořit novou revizi archivní entity k úpravě?',
      buttonText: 'Vytvořit',
      form: false
    });*/
  };

  const handleNewRecordClick = () => {
    /*props.onAdd({
      title: 'Nový záznam do jádra',
      body: 'Přejete si vytvořit nový záznam do jádra?',
      buttonText: 'Vytvořit',
      form: true
    });*/
  };

  const handleApproveRequestClick = () => {
    /*props.onAdd({
      title: 'Ke schválení',
      body: 'Přejete si předat archivní entitu ke schválení?',
      buttonText: 'Ke schválení',
      form: true
    });*/
  };

  const handleReturnRequestClick = () => {
    /*props.onAdd({
      title: 'Vrátit k doplňení',
      body: 'Přejete si vrátit archivní entitu k doplňení?',
      buttonText: 'K doplnění',
      form: true
    });*/
  };

  const handleApproveClick = () => {
    /*props.onAdd({
      title: 'Schválit archivní entitu',
      body: 'Přejete si schválit archivní entitu?',
      buttonText: 'Schválit',
      form: false
    });*/
  };

  const handleInvalidateClick = () => {
    /*props.onAdd({
      title: 'Zneplatnit archivní entitu',
      body: 'Přejete si zneplatnit archivní entitu?',
      buttonText: 'Zneplatnit',
      form: false
    });*/
  };

  return (
    <Row className="container justify-space-between flex-column">
      <Col>
        <div className="top">
          <Button onClick={() => window.history.back()}>
            <Icon glyph={"fa-chevron-left"} />
          </Button>
        </div>
        <div className="middle">
          <Button onClick={onEyeClick}>
              <Icon glyph={"fa-eye"} />
          </Button>
          <Button onClick={onEditClick}>
              <Icon glyph={"fa-edit"} /> Upravit archivní entitu
          </Button>
          <Button onClick={handleNewRecordClick}>
              <Icon glyph={"fa-save"} /> Nový záznam do jádra
          </Button>
          <Button onClick={handleApproveRequestClick}>
              <Icon glyph={"fa-check"} /> Ke schválení
          </Button>
          <Button onClick={handleReturnRequestClick}>
              <Icon glyph={"fa-reply"} /> Vrátit k doplnění
          </Button>
          <Button onClick={handleApproveClick}>
              <Icon glyph={"fa-check"} /> Schválit archivní entitu
          </Button>
          <Button onClick={handleInvalidateClick}>
              <Icon glyph={"fa-ban"} /> Zneplatnit archivní entitu
          </Button>
        </div>
      </Col>
      <Col className="bottom">
        <Button>
            <Icon glyph={"fa-chevron-up"} />
        </Button>
        <Button>
            <Icon glyph={"fa-chevron-down"} />
        </Button>
      </Col>
    </Row>
  );
};

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
  onAdd: () => null /*(modalProps: SharedModalProps) =>
    dispatch(
      ModalActions.show(SharedModal, {
        ...modalProps,
        onOk: () => dispatch(ModalActions.hide()),
        onCancel: () => dispatch(ModalActions.hide())
      })
    )*/
});

export default connect(
  null,
  mapDispatchToProps
)(DetailSide);
