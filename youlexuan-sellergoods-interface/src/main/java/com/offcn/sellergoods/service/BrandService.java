package com.offcn.sellergoods.service;

import com.offcn.entity.PageResult;
import com.offcn.entity.Result;
import com.offcn.pojo.TbBrand;

import java.util.List;
import java.util.Map;

public interface BrandService {

    /**
     * 返回全部列表
     */
    List<TbBrand> findAll();

    /**
     * 返回分页列表
     */
    PageResult findPage(TbBrand brand, int pageNum, int pageSize);

    /**
     * 新增品牌
     */
    void add(TbBrand brand);

    /**
     * 根据ID查询
     */
    TbBrand findOne(Long id);

    /**
     * 修改品牌
     */
    void update(TbBrand brand);

    /**
     * 删除品牌
     */
    void delete(Long[] ids);

    /**
     * 品牌下拉框数据
     */
    List<Map> selectOptionList();
}
