import { WebApi } from "actions";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { useEffect, useState } from "react";
import { EventType } from "typings/websocket";
import { AnyMessage } from "typings/websocket/Message";
import { ArrFileVO } from "./DescItemFileRef";

/**
 * Loads a fund file and reloads it whenever the fund's files change (rename, content
 * replacement), so a description item always shows the current file name.
 */
export function useFundFile(fundId: number, fileId: number | undefined): ArrFileVO | undefined {
  const [file, setFile] = useState<ArrFileVO>();
  const [reloadCount, setReloadCount] = useState(0);
  const ws = useWebsocket();

  useEffect(() => {
    function handleMessage(message: AnyMessage) {
      if (message.eventType === EventType.FILES_CHANGE && message.versionId === fundId) {
        setReloadCount((count) => count + 1);
      }
    }
    const listener = ws.addListener(handleMessage);
    return () => ws.removeListener(listener);
  }, [ws, fundId]);

  useEffect(() => {
    if (!fileId) {
      setFile(undefined);
      return;
    }
    // A late response for a previous fileId must not overwrite the current one.
    let cancelled = false;
    (async () => {
      const loadedFile: ArrFileVO = await WebApi.getEditableFundFile(fundId, fileId);
      if (!cancelled) {
        setFile(loadedFile);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [fundId, fileId, reloadCount]);

  return file;
}
