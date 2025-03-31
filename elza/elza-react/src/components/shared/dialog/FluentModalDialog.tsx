import { Button, Dialog, DialogActions, DialogBody, DialogContent, DialogSurface, DialogTitle } from "@fluentui/react-components";
import { PropsWithChildren, createContext, useContext, useEffect, useRef, useState } from "react";
import { DraggableWindow } from "..";
import SearchFundsForm from "components/arr/SearchFundsForm";

interface DialogProps<R, D> {
  handleResult: (result: R, data?: D) => void;
  id: string;
}
type CreateDialogFnType<R, D> = (props: DialogProps<R, D>) => JSX.Element;
export interface FluentDialogContextType {
  showModal: <R, D>(createDialog: CreateDialogFnType<R, D>) => Promise<{ result: R, data: D }>
}

export const FluentDialogContext = createContext<FluentDialogContextType>(null);

let nextDialogId = 0;
function getNextDialogId() {
  nextDialogId++;
  return nextDialogId.toString();
}

export function FluentDialogProvider({ children }: PropsWithChildren) {
  const [modals, setModals] = useState<{ id: string, dialog: JSX.Element }[]>([]);

  const modalsRef = useRef(modals);

  useEffect(() => {
    modalsRef.current = modals;
  }, [modals])

  const showModal = <R, D>(createDialog: CreateDialogFnType<R, D>) => {
    return new Promise<{ result: R, data: D }>((resolve) => {
      const modalId = getNextDialogId();

      const handleResult = (result: R, data: D) => {
        setModals(modalsRef.current.filter(({ id }) => id !== modalId));
        resolve({ result, data });
      }

      setModals([...modals, {
        id: modalId,
        dialog: createDialog({
          handleResult,
          id: modalId
        })
      }]);
    })
  }

  return <FluentDialogContext.Provider value={{ showModal }}>
    {children}
    <div style={{ position: "fixed", top: 0, left: 0, width: 0, height: 0 }}>
      {modals.map(({ dialog }) => <div>
        {dialog}
      </div>)}
    </div>
  </FluentDialogContext.Provider>
}

export function useTestModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);
  return async function showModal() {
    return _showModal<"OK" | "CANCEL", { number: number }>(({ handleResult, id }) =>
      <DraggableWindow>
        <div style={{ background: "white", padding: "20px", border: "1px solid black" }}>
          <div>{id}</div>
          <div>
            <Button onClick={() => handleResult("OK", { number: 3 })}>OK</Button>
            <Button onClick={() => handleResult("CANCEL")}>CANCEL</Button>
          </div>
        </div>
      </DraggableWindow>
    )
  }
}

export function useSearchFundsModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);
  return async function showModal() {
    return _showModal<undefined, undefined>(({ handleResult }) =>
      <DraggableWindow>
        <div style={{ background: "white", padding: "20px", border: "1px solid black", width: "500px", zIndex: 10000 }}>
          <Button onClick={() => handleResult(undefined)}>X</Button>
          <SearchFundsForm />
        </div>
      </DraggableWindow>
    )
  }
}

enum TestFluentModalResult {
  OK = "OK",
  CANCEL = "CANCEL"
}

function TestFluentModal({ onResult }: { onResult: (result: TestFluentModalResult) => void }) {
  const [open, setOpen] = useState(true);

  const _handleResult = (result: TestFluentModalResult) => {
    setOpen(false);
    setTimeout(() => {
      onResult(result);
    }, 200);
  }

  return <Dialog open={open}>
    <DialogSurface>
      <DialogBody>
        <DialogTitle>Fluent dialog</DialogTitle>
        <DialogContent>
          Test fluent dialogu
        </DialogContent>
        <DialogActions>
          <Button
            appearance="primary"
            onClick={() => _handleResult(TestFluentModalResult.OK)}
          >
            OK
          </Button>
          <Button
            onClick={() => _handleResult(TestFluentModalResult.CANCEL)}
          >
            CANCEL
          </Button>
        </DialogActions>
      </DialogBody>
    </DialogSurface>
  </Dialog>
}

export function useTestFluentModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);

  return async function showModal() {
    return _showModal<TestFluentModalResult, undefined>(({ handleResult }) => {
      return <TestFluentModal onResult={handleResult} />
    }
    )
  }
}
