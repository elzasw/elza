import { Button } from '@fluentui/react-components';
import { ChevronLeftRegular, ChevronRightRegular } from "@fluentui/react-icons"

interface Props {
  onPrevious: () => void;
  onNext: () => void;
  from: number;
  pageSize: number;
  totalCount: number;
}

export function FundPager({
  onPrevious,
  onNext,
  from,
  pageSize,
  totalCount
}: Props) {
  const fundsCountLength = totalCount.toString().length || 1;

  // if (totalCount < pageSize) return <></>;
  const _from = from + 1;
  const to = Math.min(from + pageSize, totalCount);
  const padding = (fundsCountLength - _from.toString().length) + (fundsCountLength - to.toString().length);

  return <div style={{ margin: "5px" }}>
    <Button disabled={from === 0} icon={<ChevronLeftRegular />} onClick={() => onPrevious()} />
    <Button disabled={from + pageSize >= totalCount} icon={<ChevronRightRegular />} onClick={() => onNext()} />
    <span style={{
      margin: "0 5px",
      paddingRight: `${padding}ch`,
      display: "inline-block",
      whiteSpace: "pre"
    }}>
      <span style={{
        textAlign: "right",
        display: "inline-block"
      }}>
        {_from}
      </span>
      -
      <span style={{
        textAlign: "left",
        display: "inline-block"
      }}>
        {to}
      </span>
      /
      <span>
        {totalCount}
      </span>
    </span>
  </div>
}
