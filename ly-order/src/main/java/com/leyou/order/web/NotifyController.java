package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class NotifyController {

    @Autowired
    private OrderService orderService;

    /**
     * 异步回调
     */
    @PostMapping("/wxpay/notify")
    public ResponseEntity<String> payNotify(@RequestBody Map<String,String> request){
        orderService.payNotify(request);
        String msg = "<xml>\n" +
                "\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
        return ResponseEntity.ok(msg);
    }
}
