import { Button, Dialog, DialogActions, DialogBody, DialogContent, DialogSurface, DialogTitle } from "@fluentui/react-components";
import { DismissRegular, ChevronUpRegular, ChevronDownRegular } from "@fluentui/react-icons";
import { PropsWithChildren, createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { DraggableWindow } from "..";
import SearchFundsForm from "components/arr/search-funds-form/SearchFundsForm";
import { DraggableWindowDragger } from "../draggable-window";

interface DialogProps<R, D> {
  handleResult: (result: R, data?: D) => void;
  id: string;
  isSingleInstance?: boolean;
  name?: string;
}
type CreateDialogFnType<R, D> = (props: DialogProps<R, D>) => JSX.Element;
export interface FluentDialogContextType {
  showModal: <R, D>(props: ShowModalProps<R, D>) => Promise<{ result: R, data: D }>
}

export const FluentDialogContext = createContext<FluentDialogContextType>(null);

let nextDialogId = 0;
function getNextDialogId() {
  nextDialogId++;
  return nextDialogId.toString();
}

interface FluentDialogProviderProps extends PropsWithChildren {
  hidden: boolean;
}

interface ShowModalProps<R, D> {
  name?: string;
  isSingleInstance?: boolean;
  createDialog: CreateDialogFnType<R, D>;
}

export function FluentDialogProvider({ children, hidden }: FluentDialogProviderProps) {
  const [modals, setModals] = useState<{ id: string, name?: string, isSingleInstance?: boolean, dialog: JSX.Element }[]>([]);

  const modalsRef = useRef(modals);

  useEffect(() => {
    modalsRef.current = modals;
  }, [modals])

  const showModal = <R, D>({ createDialog, isSingleInstance, name }: ShowModalProps<R, D>) => {
    return new Promise<{ result: R, data: D }>((resolve) => {
      const modalId = getNextDialogId();

      const handleResult = (result: R, data: D) => {
        setModals(modalsRef.current.filter(({ id }) => id !== modalId));
        resolve({ result, data });
      }

      if (isSingleInstance) {
        const isOpen = !!modalsRef.current?.find(({ name: _name }) => name == _name);
        console.log("#fmd - is open", isOpen, name);

        if (isOpen) {
          return;
        }
      }

      console.log("#fmd - showmodal", isSingleInstance, name);

      setModals([...modals, {
        id: modalId,
        isSingleInstance,
        name,
        dialog: createDialog({
          handleResult,
          id: modalId,
          isSingleInstance,
          name,
        })
      }]);
    })
  }

  return <FluentDialogContext.Provider value={{ showModal }}>
    {children}
    <div style={{
      position: "fixed",
      top: 0,
      left: 0,
      width: 0,
      height: 0,
      zIndex: 5000,
      display: hidden ? "none" : "block"
    }}>
      {modals.map(({ dialog }, index) => <div key={index}>
        {dialog}
      </div>)}
    </div>
  </FluentDialogContext.Provider>
}

export function useTestModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);
  return async function showModal() {
    return _showModal<"OK" | "CANCEL", { number: number }>({
      createDialog: ({ handleResult, id }) =>
        <DraggableWindow>
          <div style={{ background: "white", padding: "20px", border: "1px solid black" }}>
            <div>{id}</div>
            <div>
              <Button onClick={() => handleResult("OK", { number: 3 })}>OK</Button>
              <Button onClick={() => handleResult("CANCEL")}>CANCEL</Button>
            </div>
          </div>
        </DraggableWindow>
    })
  }
}

interface SearchFundsWindowProps {
  onResult: () => void;
}

export function SearchFundsWindow({ onResult }: SearchFundsWindowProps) {
  return <CollapsibleDragWindow
    title="Vyhledavani v arch. souborech"
    onClose={onResult}
  >
    <SearchFundsForm />
  </CollapsibleDragWindow>
}

interface CollapsibleDragWindowProps {
  onClose: () => void;
  title: string;
  initialWidth?: number;
  initialHeight?: number;
}

export function CollapsibleDragWindow({
  onClose = () => { return; },
  title,
  children,
  initialWidth = 650,
  initialHeight = 700,
}: PropsWithChildren<CollapsibleDragWindowProps>) {
  const initialPosition = { x: window.innerWidth / 2 - initialWidth / 2, y: window.innerHeight / 2 - initialHeight / 2 };

  const [open, setOpen] = useState(true);
  const [height, setHeight] = useState(initialHeight)
  const [lastHeight, setLastHeight] = useState(initialHeight)

  const handleCollapse = () => {
    if (open) {
      setLastHeight(height);
    } else {
      setHeight(lastHeight);
    }

    setOpen(!open);
  }

  const measuredRef = useCallback((node: HTMLDivElement) => {
    if (node !== null) {
      const resizeObserver = new ResizeObserver(() => {
        setHeight(node.getBoundingClientRect().height);
      });
      resizeObserver.observe(node);
    }
  }, []);

  return <DraggableWindow initialPosition={initialPosition}>
    <div
      ref={measuredRef}
      style={{
        background: "var(--shade-0)",
        // border: "var(--primary-border)",
        minWidth: "300px",
        width: initialWidth,
        minHeight: open ? "300px" : undefined,
        height: open ? height : "auto",
        zIndex: 10000,
        borderRadius: "8px",
        boxShadow: "5px 5px 30px 5px rgba(0, 0, 0, 0.2)",
        overflow: "hidden",
        resize: open ? "both" : "horizontal",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <DraggableWindowDragger style={{ display: "flex", padding: "5px", alignItems: "center" }}>
        <div>&nbsp;{title}</div>
        <div style={{ flexGrow: 1 }}></div>
        <Button
          icon={open ? <ChevronUpRegular /> : <ChevronDownRegular />}
          onClick={handleCollapse}
          appearance="subtle"
        />
        <Button
          icon={<DismissRegular />}
          onClick={() => onClose()}
          appearance="subtle"
        />
      </DraggableWindowDragger>
      <div style={{
        padding: "0 20px 20px",
        flexGrow: 1,
        display: open ? "flex" : "none",
        flexDirection: "column",
        overflow: "hidden",
      }}>
        {children}
      </div>
    </div>
  </DraggableWindow>
}

export function useSearchFundsModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);

  return async function showModal() {
    return _showModal<undefined, undefined>({
      isSingleInstance: true,
      name: "search-funds-modal",
      createDialog: ({ handleResult }) => {
        return <SearchFundsWindow onResult={() => handleResult(undefined, undefined)} />
      }
    })
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

  return <Dialog open={open} onOpenChange={(_event, data) => !data.open && _handleResult(TestFluentModalResult.OK)}>
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
    return _showModal<TestFluentModalResult, undefined>({
      createDialog: ({ handleResult }) => {
        return <TestFluentModal onResult={handleResult} />
      }
    })
  }
}
