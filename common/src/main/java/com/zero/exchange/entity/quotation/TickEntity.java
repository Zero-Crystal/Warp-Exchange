package com.zero.exchange.entity.quotation;

import com.zero.exchange.support.LoggerSupport;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ticks", uniqueConstraints = @UniqueConstraint(name = "UNI_T_M", columnNames = {"takerOrderId", "makerOrderId"}),
indexes = @Index(name = "IDX_CT", columnList = "createAt"))
public class TickEntity extends LoggerSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public long id;

    @Column(nullable = false, updatable = false)
    public long sequenceId;

    @Column(nullable = false, updatable = false)
    public Long takerOrderId;

    @Column(nullable = false, updatable = false)
    public Long makerOrderId;

    /**
     * Bit for taker direction: 1=LONG, 0=SHORT.
     */
    @Column(nullable = false, updatable = false)
    public boolean takerDirection;

    @Column(nullable = false, updatable = false)
    public BigDecimal price;

    @Column(nullable = false, updatable = false)
    public BigDecimal quantity;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public Long createdAt;

    public String toJson() {
        return "[" + createdAt + "," + (takerDirection ? 1 : 0) + "," + price + "," + quantity + "]";
    }

}
