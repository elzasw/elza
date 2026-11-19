import { act, renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "test/mocks/server";
import { EventType } from "typings/websocket";
import type { WebsocketListener } from "websocket/WebsocketClient";
import { useFundFile } from "./useFundFile";

const listeners = vi.hoisted(() => new Set<(message: unknown) => void>());

vi.mock("components/shared/web-socket/WebsocketProvider", () => {
  const client = {
    addListener: (listener: WebsocketListener) => {
      listeners.add(listener);
      return listener;
    },
    removeListener: (listener: WebsocketListener) => {
      listeners.delete(listener);
    },
  };
  return { useWebsocket: () => client };
});

const FUND_ID = 7;
const OTHER_FUND_ID = 8;
const FILE_ID = 42;

function sendFilesChange(fundId: number) {
  act(() => {
    for (const listener of listeners) {
      listener({ eventType: EventType.FILES_CHANGE, versionId: fundId, entityId: "ArrFile" });
    }
  });
}

/** Serves the file; the returned state sets the served name and counts the requests. */
function serveFile() {
  const state = { name: "A", requests: 0 };
  server.use(
    http.get(`/api/dms/fund/${FUND_ID}/${FILE_ID}`, () => {
      state.requests++;
      return HttpResponse.json({ id: FILE_ID, fundId: FUND_ID, name: state.name });
    }),
  );
  return state;
}

describe("useFundFile", () => {
  beforeEach(() => {
    listeners.clear();
  });

  it("reloads the file after a files change of its fund", async () => {
    const served = serveFile();
    const { result } = renderHook(() => useFundFile(FUND_ID, FILE_ID));
    await waitFor(() => expect(result.current?.name).toBe("A"));

    served.name = "B";
    sendFilesChange(FUND_ID);

    await waitFor(() => expect(result.current?.name).toBe("B"));
  });

  it("ignores a files change of another fund", async () => {
    const served = serveFile();
    const { result } = renderHook(() => useFundFile(FUND_ID, FILE_ID));
    await waitFor(() => expect(result.current?.name).toBe("A"));

    served.name = "B";
    sendFilesChange(OTHER_FUND_ID);

    // give a possible reload time to run before checking it did not happen
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(served.requests).toBe(1);
    expect(result.current?.name).toBe("A");
  });

  it("returns nothing without a file id", () => {
    const { result } = renderHook(() => useFundFile(FUND_ID, undefined));
    expect(result.current).toBeUndefined();
  });
});
