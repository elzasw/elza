import { ReactNode, useEffect } from "react";
import { OperationCompareType } from "elza-api";
import { IntlShape } from "react-intl";
import { ShapeIntersectFilled, SquaresNestedRegular } from "@fluentui/react-icons";
import { messages } from "./messages";

export function formatOperation(operation: OperationCompareType, intl?: IntlShape, isEnum?: boolean): ReactNode {
  switch (operation) {
    case OperationCompareType.Eq:
      return isEnum
        ? ": "
        : <span style={{ padding: "0 5px", fontSize: "1.4rem" }}>=</span>;
    case OperationCompareType.Neq:
      return <span style={{ padding: "0 5px", fontSize: "1.4rem" }}>≠</span>;
    case OperationCompareType.Contains:
      return <span style={{ padding: "0 5px", fontSize: "1.2rem" }}>∋</span>;
    case OperationCompareType.Gt:
      return <span style={{ padding: "0 5px", fontSize: "1.2rem" }}>{">"}</span>;
    case OperationCompareType.Lt:
      return <span style={{ padding: "0 5px", fontSize: "1.2rem" }}>{"<"}</span>;
    case OperationCompareType.Gte:
      return <span style={{ padding: "0 5px", fontSize: "1.2rem" }}>{">="}</span>;
    case OperationCompareType.Lte:
      return <span style={{ padding: "0 5px", fontSize: "1.2rem" }}>{"<="}</span>;
    case OperationCompareType.Intersect:
      return <span style={{ padding: "0 5px", fontSize: "1.2rem" }}><ShapeIntersectFilled /></span>;
    case OperationCompareType.IsIn:
      return <span style={{ padding: "0 5px", fontSize: "1.2rem" }}><SquaresNestedRegular /></span>;
    case OperationCompareType.NotNull:
    case OperationCompareType.IsNull:
      return <span style={{ padding: "0 5px" }}>{intl?.formatMessage(messages[operation]) || operation}</span>;
    default:
      return <span style={{ padding: "0 5px" }}>{operation}</span>;
  }
}

export function useInitialFocus<T extends HTMLElement>(ref: React.RefObject<T>) {
  useEffect(() => {
    if (ref.current) {
      ref.current.focus();
    }
  }, [ref]);
}
