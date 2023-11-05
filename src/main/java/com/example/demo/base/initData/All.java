package com.example.demo.base.initData;

import com.example.demo.domain.cart.service.CartService;
import com.example.demo.domain.member.service.MemberService;
import com.example.demo.domain.order.service.OrderService;
import com.example.demo.domain.product.service.ProductService;
import com.example.demo.domain.song.service.SongService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class All implements InitDataBefore {
    @Bean
    CommandLineRunner initData(
            MemberService memberService,
            SongService songService,
            ProductService productService,
            CartService cartService,
            OrderService orderService) {
        return args -> {
            before(memberService, songService, productService, cartService, orderService);
        };
    }
}
