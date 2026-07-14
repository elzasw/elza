import { Button, Dialog, DialogActions, DialogBody, DialogContent, DialogSurface, DialogTitle, tokens } from "@fluentui/react-components";
import { DismissRegular, ChevronUpRegular, ChevronDownRegular, PinRegular, PinOffRegular } from "@fluentui/react-icons";
import { PropsWithChildren, createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { DraggableWindow } from "..";
import SearchFundsForm from "components/arr/search-funds-form/SearchFundsForm";
import { DraggableWindowContext, DraggableWindowDragger } from "../draggable-window";

function PinBottomButton() {
  const { enablePinBottom, pinnedBottom, togglePinBottom } = useContext(DraggableWindowContext);
  if (!enablePinBottom) return null;
  return (
    <Button
      icon={pinnedBottom ? <PinOffRegular /> : <PinRegular />}
      onClick={togglePinBottom}
      appearance="subtle"
    />
  );
}

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
  enablePinBottom?: boolean;
}

export function CollapsibleDragWindow({
  onClose = () => { return; },
  title,
  children,
  initialWidth = 650,
  initialHeight = 700,
  enablePinBottom = false,
}: PropsWithChildren<CollapsibleDragWindowProps>) {
  const initialPosition = { x: window.innerWidth / 2 - initialWidth / 2, y: window.innerHeight / 2 - initialHeight / 2 };

  return <DraggableWindow initialPosition={initialPosition} enablePinBottom={enablePinBottom}>
    <CollapsibleWindowBody
      title={title}
      onClose={onClose}
      initialWidth={initialWidth}
      initialHeight={initialHeight}
    >
      {children}
    </CollapsibleWindowBody>
  </DraggableWindow>
}

interface CollapsibleWindowBodyProps {
  title: string;
  onClose: () => void;
  initialWidth: number;
  initialHeight: number;
}

function CollapsibleWindowBody({
  title,
  onClose,
  initialWidth,
  initialHeight,
  children,
}: PropsWithChildren<CollapsibleWindowBodyProps>) {
  const { pinnedBottom, registerResizable, isResizingRef } = useContext(DraggableWindowContext);
  const PINNED_COLLAPSED_WIDTH = 260;
  const MIN_WIDTH = 300;
  const MIN_HEIGHT = 300;

  const [open, setOpen] = useState(true);
  const [height, setHeight] = useState(initialHeight);
  const [lastHeight, setLastHeight] = useState(initialHeight);
  const [width, setWidth] = useState(initialWidth);
  const [viewport, setViewport] = useState({ width: window.innerWidth, height: window.innerHeight });

  const openRef = useRef(open);
  openRef.current = open;
  const sizeRef = useRef({ width, height });
  sizeRef.current = { width, height };

  useEffect(() => {
    const handleResize = () => setViewport({ width: window.innerWidth, height: window.innerHeight });
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  useEffect(() => {
    registerResizable({
      onResizeStart: () => ({
        width: sizeRef.current.width,
        height: sizeRef.current.height,
        minWidth: MIN_WIDTH,
        minHeight: openRef.current ? MIN_HEIGHT : 0,
      }),
      onResize: (newWidth, newHeight) => {
        setWidth(newWidth);
        if (openRef.current) setHeight(newHeight);
      },
    });
  }, [registerResizable]);

  const collapsedPinned = pinnedBottom && !open;
  const collapsedPinnedRef = useRef(collapsedPinned);
  collapsedPinnedRef.current = collapsedPinned;

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
        if (collapsedPinnedRef.current) return;
        // While the user drags a resize handle the size is driven directly by
        // setWidth/setHeight; reading it back here would fight that update.
        if (isResizingRef.current) return;
        const rect = node.getBoundingClientRect();
        // Ignore sizes forced by the viewport cap (maxWidth/maxHeight); otherwise
        // shrinking the viewport would overwrite the user's chosen size and it
        // wouldn't grow back when the viewport does.
        const cappedByViewportWidth = rect.width >= window.innerWidth;
        const cappedByViewportHeight = rect.height >= window.innerHeight;
        if (!cappedByViewportWidth) setWidth(rect.width);
        if (!cappedByViewportHeight) setHeight(rect.height);
      });
      resizeObserver.observe(node);
    }
  }, []);

  return (
    <div
      ref={measuredRef}
      style={{
        background: "var(--shade-0)",
        border: `1px solid ${tokens.colorNeutralStroke2}`,
        position: "relative",
        minWidth: collapsedPinned ? undefined : "300px",
        width: collapsedPinned ? PINNED_COLLAPSED_WIDTH : width,
        maxWidth: `${viewport.width}px`,
        minHeight: open ? "300px" : undefined,
        height: open ? height : "auto",
        maxHeight: `${viewport.height}px`,
        zIndex: 10000,
        borderRadius: pinnedBottom ? "8px 8px 0 0" : "8px",
        boxShadow: "5px 5px 30px 5px rgba(0, 0, 0, 0.2)",
        overflow: "hidden",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <DraggableWindowDragger style={{ display: "flex", padding: "5px", alignItems: "center" }}>
        <div>&nbsp;{title}</div>
        <div style={{ flexGrow: 1 }}></div>
        <PinBottomButton />
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
  );
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
