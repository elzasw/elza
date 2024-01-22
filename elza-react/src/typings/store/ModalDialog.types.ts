export enum DialogCloseType {
  DIALOG = "DIALOG",
  DIALOG_CONTENT = "DIALOG_CONTENT"
}

export interface ModalRenderFunctionProps {
  key: number;
  onClose: (closeType?: DialogCloseType) => void;
  visible: boolean;
}

export interface ModalDialog {
  title: string;
  component: unknown;
  content: React.ReactElement | ((props: ModalRenderFunctionProps) => React.ReactElement);
  dialogClassName: string;
  onClose: (closeType?: DialogCloseType) => void;
  key: number;
}

export interface ModalDialogState {
  lastKey?: number;
  items: ModalDialog[];
}
