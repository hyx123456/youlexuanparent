package com.offcn.task;

import com.offcn.mapper.TbSeckillGoodsMapper;
import com.offcn.pojo.TbSeckillGoods;
import com.offcn.pojo.TbSeckillGoodsExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 刷新秒杀商品
 */
@Component
public class SeckillTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;

    /**
     * 刷新秒杀商品
     */
    @Scheduled(cron = "0 * * * * ?")
    public void refreshSeckillGoods() {
        System.out.println("执行了任务调度" + new Date());
        //查询所有的秒杀商品键集合
        List ids = new ArrayList<>(redisTemplate.boundHashOps("seckillGoods").keys());
        //查询正在秒杀的商品列表
        TbSeckillGoodsExample example = new TbSeckillGoodsExample();
        TbSeckillGoodsExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1");
        criteria.andStockCountGreaterThan(0);
        criteria.andStartTimeLessThanOrEqualTo(new Date());
        criteria.andEndTimeGreaterThan(new Date());
        if (ids != null && ids.size() > 0) {
            criteria.andIdNotIn(ids);
        }
        List<TbSeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(example);
        for (TbSeckillGoods seckillGoods : seckillGoodsList) {
            redisTemplate.boundHashOps("seckillGoods").put(seckillGoods.getId(), seckillGoods);
        }
        System.out.println("将" + seckillGoodsList.size() + "条商品装入缓存");
    }

    /**
     * 移除秒杀商品
     */
    @Scheduled(cron = "* * * * * ?")
    public void removeSeckillGoods() {
        System.out.println("移除秒杀商品任务在执行");
        List<TbSeckillGoods> seckillGoods = redisTemplate.boundHashOps("seckillGoods").values();
        for (TbSeckillGoods seckillGood : seckillGoods) {
            if (seckillGood.getEndTime().getTime() < new Date().getTime()) {
                seckillGoodsMapper.updateByPrimaryKey(seckillGood);
                redisTemplate.boundHashOps("seckillGoods").delete(seckillGood.getId());
                System.out.println("移除秒杀商品" + seckillGood.getId());
            }
        }
        System.out.println("移除秒杀商品任务结束");
    }
}
