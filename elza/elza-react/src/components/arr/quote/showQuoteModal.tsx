import { modalDialogShow } from "actions/global/modalDialog"
import { FormattedMessage } from "react-intl"
import { QuoteModal } from "./QuoteModal"
import { Dispatch } from "redux"
import { messages } from "."

export const showQuoteModal = (nodeId: number, versionId: number) =>
  (dispatch: Dispatch) => {
    return dispatch(modalDialogShow(
      this,
      <FormattedMessage {...messages.quoteTitle} />,
      <QuoteModal versionId={versionId} nodeId={nodeId} />,
      null,
    ))
  }
