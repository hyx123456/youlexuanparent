package com.offcn.sms;

import com.aliyuncs.CommonResponse;
import com.aliyuncs.exceptions.ClientException;
import com.offcn.util.SmsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

@Component
public class SmsListener implements MessageListener {

    @Autowired
    private SmsUtil smsUtil;

    @Override
    public void onMessage(Message message) {
        if (message instanceof MapMessage) {
            MapMessage map = (MapMessage) message;
            try {
                CommonResponse response = smsUtil.sendSms(map.getString("mobile"), map.getString("template_code"), map.getString("sign_name"), map.getString("parm"));
                System.out.println("data:" + response.getData());
            } catch (ClientException e) {
                e.printStackTrace();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
