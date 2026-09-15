import React from 'react';
import {Icon} from "./index";
import {Modal} from "react-bootstrap";
import {Button} from "./ui";
import { FormattedMessage } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';

interface OwnProps {
  message: string[];
  onClose: () => void;
}

type Props = OwnProps;

const ValidationResultModal: React.FC<Props> = ({
                                                  message,
                                                    onClose
                                                }) => {
  const renderErrorItem = (item: string, index: number) => {
    return <p className="validation-message" key={`validation-item-${index}`}><Icon glyph="fa-exclamation-triangle" className="validation-icon-inline mr-1" />{item}</p>
  };

  return <>
      <Modal.Body>
          {message.map((value, index) => renderErrorItem(value, index))}
      </Modal.Body>
      <Modal.Footer>
          <Button variant="link" onClick={onClose}>
              {<FormattedMessage {...globalMessages.close} />}
          </Button>
      </Modal.Footer>
  </>
};

export default ValidationResultModal;
