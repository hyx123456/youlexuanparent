package com.offcn.search.service;

import com.offcn.pojo.TbItem;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {

    /**
     * 搜索
     */
    public Map<String, Object> search(Map searchMap);

    /**
     * 导入数据
     */
    public void importList(List<TbItem> list);

    /**
     * 删除数据
     */
    public void deleteByGoodsIds(List goodsList);
}
