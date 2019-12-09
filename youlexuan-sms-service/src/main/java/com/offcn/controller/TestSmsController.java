package com.offcn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.*;

@RestController
public class TestSmsController {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Destination destination;

    @RequestMapping("/sendsms")
    public String sendMsg(String mobile, String msg) {
        jmsTemplate.send(destination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                MapMessage map = session.createMapMessage();
                map.setString("mobile", mobile);
                map.setString("template_code", "SMS_175530092");
                map.setString("sign_name", "优乐选");
                map.setString("parm", "{\"code\":\"" + msg + "\"}");
                return map;
            }
        });
        return "send ok";
    }
}
