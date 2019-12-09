package com.offcn.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;
import java.util.Map;

@Component
public class ItemSearchListener implements MessageListener {

    @Autowired
    private ItemSearchService itemSearchService;

    @Override
    public void onMessage(Message message) {
        System.out.println("接收到导入solr数据请求");
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            try {
                String text = textMessage.getText();
                //转换json串为list集合
                List<TbItem> itemList = JSON.parseArray(text, TbItem.class);
                for (TbItem item : itemList) {
                    System.out.println("title:" + item.getTitle());
                    //读取sku对应规格，转换成json对象
                    Map specMap = JSON.parseObject(item.getSpec(), Map.class);
                    //给带注解的字段赋值
                    item.setSpecMap(specMap);
                }
                itemSearchService.importList(itemList);
                System.out.println("成功导入到索引库");
            } catch (JMSException e) {
                e.printStackTrace();
            }

        }
    }
}
