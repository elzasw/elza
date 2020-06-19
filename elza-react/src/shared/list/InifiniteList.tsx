import React, {FC} from 'react';
import InfiniteScroll from "react-infinite-scroll-component";
import Loading from "../../components/shared/loading/Loading";

interface Props {
  list: any,
  fetchMore: () => void,
  scrollableTarget: string
}

/**
 * Wrapper pro nekonečný seznam.
 * @param list
 */
const InifiniteList: FC<Props> = ({list, fetchMore, children, scrollableTarget}) => {
  return <InfiniteScroll
    dataLength={list.data.length}
    next={fetchMore}
    hasMore={list.data.length < list.total}
    scrollableTarget={scrollableTarget}
    loader={<div className="m-3"><Loading /></div>}
  >
    {children}
  </InfiniteScroll>
};

export default InifiniteList;
