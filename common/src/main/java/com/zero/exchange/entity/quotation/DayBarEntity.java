package com.zero.exchange.entity.quotation;

import com.zero.exchange.entity.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {
}
