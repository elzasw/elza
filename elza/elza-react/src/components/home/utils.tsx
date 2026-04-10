import { TooltipTrigger } from "components/shared";

export const truncateStringWithTooltip = (string: string, length: number, maxWidth = "13em") => {
  if (string.length <= length) {
    return string;
  }
  return <TooltipTrigger content={<div style={{ maxWidth }}>{string}</div>} placement='vertical'>
    {`${string.slice(0, length - 3).trim()}...`}
  </TooltipTrigger>
}
