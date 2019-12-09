package com.offcn.pay.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCancelRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCancelResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.offcn.pay.service.AliPayService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AliPayServiceImpl implements AliPayService {

    @Autowired
    private AlipayClient alipayClient;

    @Override
    public Map createNative(String out_trade_no, String total_fee) {
        Map<String, String> map = new HashMap<>();
        //创建预下单请求对象
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        //转换下单金额按照元
        long total = Long.parseLong(total_fee);
        BigDecimal bigDecimal = BigDecimal.valueOf(total);
        BigDecimal cs = BigDecimal.valueOf(100d);
        BigDecimal bigYuan = bigDecimal.divide(cs);
        System.out.println("预下单金额:" + bigYuan.doubleValue());
        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + out_trade_no + "\"," +
                "    \"total_amount\":\"" + bigYuan.doubleValue() + "\"," +
                "    \"subject\":\"测试购买商品001\"," +
                "    \"store_id\":\"xa_001\"," +
                "    \"timeout_express\":\"90m\"}"
        );//设置业务参数
        //发出预下单业务请求
        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            //从相应对象读取相应结果
            String code = response.getCode();
            System.out.println("响应码:" + code);
            //全部的响应结果
            String body = response.getBody();
            System.out.println("返回结果:" + body);
            if ("10000".equals(code)) {
                map.put("qrcode", response.getQrCode());
                map.put("out_trade_no", response.getOutTradeNo());
                map.put("total_fee", total_fee);
                System.out.println("qrcode:" + response.getQrCode());
                System.out.println("out_trade_no:" + response.getOutTradeNo());
                System.out.println("total_fee:" + total_fee);

            } else {
                System.out.println("预下单接口调用失败:" + body);
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public Map queryPayStatus(String out_trade_no) {
        Map<String, String> map = new HashMap<>();
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + out_trade_no + "\"," +
                "    \"trade_no\":\"\"}"
        );//设置业务参数
        //发出请求
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            String code = response.getCode();
            System.out.println("返回值1:" + response.getBody());
            if ("10000".equals(code)) {
                map.put("out_trade_no", out_trade_no);
                map.put("tradestatus", response.getTradeStatus());
                map.put("trade_no", response.getTradeNo());
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 关闭订单接口
     *
     * @param out_trade_no
     * @return
     */
    @Override
    public Map closePay(String out_trade_no) {
        Map<String, String> map = new HashMap<>();
        //撤销交易请求对象
        AlipayTradeCancelRequest request = new AlipayTradeCancelRequest();
        request.setBizContent(
                "{" +
                        "    \"out_trade_no\":\"" + out_trade_no + "\"," +
                        "    \"trade_no\":\"\"}"

        );//设置业务参数
        try {
            AlipayTradeCancelResponse response = alipayClient.execute(request);
            String code = response.getCode();
            if (code.equals("10000")) {
                System.out.println("返回值:" + response.getBody());
                map.put("code", code);
                map.put("out_trade_no", out_trade_no);
                return map;
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
