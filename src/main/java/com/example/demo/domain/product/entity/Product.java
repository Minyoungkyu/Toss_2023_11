package com.example.demo.domain.product.entity;

import com.example.demo.base.entity.BaseEntity;
import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.song.entity.Song;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = PROTECTED)
public class Product extends BaseEntity {
    private String subject;
    @ManyToOne(fetch = LAZY)
    private Member author;
    @ManyToOne(fetch = LAZY)
    private Song song;
    private int price;


    public Product(long id) {
        super(id);
    }

    public int getSalePrice() {
        return getPrice();
    }

    public int getWholesalePrice() {
        return (int) Math.ceil(getPrice() * 0.7);
    }

    public boolean isOrderable() {
        return true;
    }


    public String getJdenticon() {
        return "product__" + getId();
    }
}
