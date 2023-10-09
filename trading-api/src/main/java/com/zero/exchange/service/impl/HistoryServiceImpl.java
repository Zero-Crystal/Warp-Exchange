package com.zero.exchange.service.impl;

import com.zero.exchange.model.OrderMatchedDetailVO;
import com.zero.exchange.entity.trade.MatchDetailEntity;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.service.HistoryService;
import com.zero.exchange.support.AbstractDbService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryServiceImpl extends AbstractDbService implements HistoryService {

    @Override
    public List<OrderEntity> getHistoryOrders(Long userId, int maxResult) {
        return db.from(OrderEntity.class).where("userId = ?", userId).limit(maxResult).list();
    }

    @Override
    public OrderEntity getHistoryOrder(Long userId, Long orderId) {
        OrderEntity order = db.fetch(OrderEntity.class, orderId);
        if (order == null || order.userId != userId) {
            return null;
        }
        return order;
    }

    @Override
    public List<OrderMatchedDetailVO> getHistoryOrderDetail(Long orderId) {
        List<MatchDetailEntity> matchDetailEntities = db.select("price", "quantity", "type")
                .from(MatchDetailEntity.class).where("orderId = ?", orderId).list();
        return matchDetailEntities.stream().map(mde -> new OrderMatchedDetailVO(mde.price, mde.quantity, mde.type))
                .collect(Collectors.toList());
    }
}
