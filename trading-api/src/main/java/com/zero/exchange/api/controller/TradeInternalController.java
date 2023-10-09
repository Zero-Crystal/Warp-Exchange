package com.zero.exchange.api.controller;

import com.zero.exchange.api.ApiResult;
import com.zero.exchange.model.TransferVO;
import com.zero.exchange.enums.UserType;
import com.zero.exchange.message.event.TransferEvent;
import com.zero.exchange.service.SendEventService;
import com.zero.exchange.support.LoggerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class TradeInternalController extends LoggerSupport {

    @Autowired
    private SendEventService sendEventService;

    /**
     * 转账请求，可重复调用，根据 uniqueId 去重
     *
     * @param transferVO
     * @return ApiResult
     * */
    @PostMapping("/transfer")
    public ApiResult transferIn(@RequestBody TransferVO transferVO) {
        log.info("transferIn: {}", transferVO.toString());
        transferVO.validate();

        var event = new TransferEvent();
        event.uniqueId = transferVO.transferId;
        event.fromUserId = transferVO.fromUserId;
        event.toUserId = transferVO.toUserId;
        event.assetType = transferVO.type;
        event.amount = transferVO.amount;
        event.sufficient = transferVO.fromUserId != UserType.DEBT.getUserType();
        sendEventService.sendMessage(event);
        log.info("send transfer event: {}", event);
        return ApiResult.success(Map.of("result", Boolean.TRUE));
    }
}
