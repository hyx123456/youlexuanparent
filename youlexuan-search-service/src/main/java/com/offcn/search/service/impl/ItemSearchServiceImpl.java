package com.offcn.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.promeg.pinyinhelper.Pinyin;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(timeout = 3000)
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> search(Map searchMap) {
        //多关键字空格处理
        searchMap.put("keywords", searchMap.get("keywords").toString().replace(" ", ""));
        HashMap<String, Object> map = new HashMap<>();
        //1.查询列表
        map.putAll(searchList(searchMap));
        //2.根据关键字查询商品分类
        List categoryList = searchCategoryList(searchMap);
        map.put("categoryList", categoryList);
        //3.查询品牌和规格列表
        String categoryName = searchMap.get("category").toString();
        if (!"".equals(categoryName)) {
            //按照分类名称重新读取对应品牌、规格
            map.putAll(searchBrandAndSpecList(categoryName));
        } else {
            if (categoryList.size() > 0) {
                map.putAll(searchBrandAndSpecList(categoryList.get(0).toString()));
            }
        }
        return map;
    }

    @Override
    public void importList(List<TbItem> list) {
        for (TbItem item : list) {
            System.out.println(item.getTitle());
            //从数据库中提取规格json字符串转换为map
            Map<String, String> specMap = JSON.parseObject(item.getSpec(), Map.class);
            Map map = new HashMap<>();
            for (String key : specMap.keySet()) {
                map.put("item_spec_" + Pinyin.toPinyin(key, "").toLowerCase(), specMap.get(key));
            }
            item.setSpecMap(map);
        }
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }

    @Override
    public void deleteByGoodsIds(List goodsList) {
        System.out.println("删除商品ID" + goodsList);
        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goods_id").in(goodsList);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }

    /**
     * 根据关键字搜索列表
     */
    private Map searchList(Map searchMap) {
        Map map = new HashMap();
        HighlightQuery query = new SimpleHighlightQuery();
        //设置高亮的域
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");
        //高亮前缀
        highlightOptions.setSimplePrefix("<em style='color:red'>");
        //高亮后缀
        highlightOptions.setSimplePostfix("</em>");
        //设置高亮选项
        query.setHighlightOptions(highlightOptions);
        //1.1按照关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //1.2按分类筛选
        if (!"".equals(searchMap.get("category"))) {
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
            query.addFilterQuery(filterQuery);
        }
        //1.3按品牌筛选
        if (!"".equals(searchMap.get("brand"))) {
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
            query.addFilterQuery(filterQuery);
        }
        //1.4过滤规格
        if (searchMap.get("spec") != null) {
            Map<String, String> specMap = (Map<String, String>) searchMap.get("spec");
            for (String key : specMap.keySet()) {
                Criteria filterCriteria = new Criteria("item_spec_" + Pinyin.toPinyin(key, "").toLowerCase()).is(specMap.get(key));
                FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }
        //1.5按价格筛选
        if (!"".equals(searchMap.get("price"))) {
            String[] prices = searchMap.get("price").toString().split("-");
            //如果区间起点不等于0
            if (!prices[0].equals("0")) {
                Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(prices[0]);
                FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            //如果区间起点不等于*
            if (!prices[1].equals("*")) {
                Criteria filterCriteria = new Criteria("item_price").lessThanEqual(prices[1]);
                FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }

        }
        //1.6分页查询
        //提取页码
        Integer pageNo = (Integer) searchMap.get("pageNo");
        if (pageNo == null) {
            //默认第一页
            pageNo = 1;
        }
        //每页记录数
        Integer pageSize = (Integer) searchMap.get("pageSize");
        if (pageSize == null) {
            //默认20
            pageSize = 20;
        }
        query.setOffset(pageNo);
        query.setRows(pageSize);
        //1.7排序
        String sortValue = searchMap.get("sort").toString();
        String sortField = searchMap.get("sortField").toString();
        if (sortValue != null && !sortValue.equals("")) {
            if (sortValue.equals("ASC")) {
                Sort sort = new Sort(Sort.Direction.ASC, "item_" + sortField);
                query.addSort(sort);
            }
            if (sortValue.equals("DESC")) {
                Sort sort = new Sort(Sort.Direction.DESC, "item_" + sortField);
                query.addSort(sort);
            }
        }
        //高亮显示
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        //循环高亮入口集合
        for (HighlightEntry<TbItem> h : page.getHighlighted()) {
            TbItem item = h.getEntity();
            //获取原实体类
            if (h.getHighlights().size() > 0 && h.getHighlights().get(0).getSnipplets().size() > 0) {
                //设置高亮的结果
                item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));
            }
        }
        map.put("rows", page.getContent());
        //返回总页数
        map.put("totalPages", page.getTotalPages());
        //返回总记录数
        map.put("total", page.getTotalElements());
        return map;
    }

    /**
     * 查询分类列表
     */
    private List searchCategoryList(Map searchMap) {
        List<String> list = new ArrayList<>();
        Query query = new SimpleQuery();
        //按照关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //设置分组选项
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(groupOptions);
        //得到分组页
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //根据列得到分组结果集
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        //得到分组结果入口页
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        //得到分组入口集合
        List<GroupEntry<TbItem>> content = groupEntries.getContent();
        for (GroupEntry<TbItem> entry : content) {
            //将分组结果的名称封装到返回值中
            list.add(entry.getGroupValue());
        }
        return list;
    }

    /**
     * 查询品牌和规格列表
     */
    private Map searchBrandAndSpecList(String category) {
        Map map = new HashMap();
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (typeId != null) {
            //根据模板ID查询品牌列表
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
            map.put("brandList", brandList);
            //根据模板ID查询规格列表
            List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
            map.put("specList", specList);
        }
        return map;
    }
}
