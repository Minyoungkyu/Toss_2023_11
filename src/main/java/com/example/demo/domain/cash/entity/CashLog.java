package com.example.demo.domain.cash.entity;

import com.example.demo.base.entity.BaseEntity;
import com.example.demo.domain.member.entity.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class CashLog extends BaseEntity {
    @ManyToOne(fetch = LAZY)
    private Member member;
    private long price; // 변동
    private String eventType;

    public CashLog(long id) {
        super(id);
    }
}
