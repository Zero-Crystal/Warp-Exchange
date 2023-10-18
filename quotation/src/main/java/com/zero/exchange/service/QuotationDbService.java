package com.zero.exchange.service;

import com.zero.exchange.entity.quotation.*;
import com.zero.exchange.support.AbstractDbService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
public class QuotationDbService extends AbstractDbService {

    /**
     * 保存 bar 信息
     *
     * @param secBar
     * @param minBar
     * @param hourBar
     * @param dayBar
     * */
    public void saveBars(SecBarEntity secBar, MinBarEntity minBar, HourBarEntity hourBar, DayBarEntity dayBar) {
        if (secBar != null) {
            db.insertIgnore(secBar);
        }
        if (minBar != null) {
            db.insertIgnore(minBar);
        }
        if (hourBar != null) {
            db.insertIgnore(hourBar);
        }
        if (dayBar != null) {
            db.insertIgnore(dayBar);
        }
    }

    /**
     * 保存 ticks 信息
     *
     * @param ticks
     * */
    public void saveTicks(List<TickEntity> ticks) {
        db.insertIgnore(ticks);
    }
}
