package com.example.demo.domain.order.controller;

import com.example.demo.DemoApplication;
import com.example.demo.base.rsData.RsData;
import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.service.MemberService;
import com.example.demo.domain.order.entity.Order;
import com.example.demo.domain.order.exception.ActorCanNotPayOrderException;
import com.example.demo.domain.order.exception.ActorCanNotSeeOrderException;
import com.example.demo.domain.order.exception.OrderIdNotMatchedException;
import com.example.demo.domain.order.exception.OrderNotEnoughRestCashException;
import com.example.demo.domain.order.service.OrderService;
import com.example.demo.domain.security.dto.MemberContext;
import com.example.demo.util.Ut;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final MemberService memberService;

    @PostMapping("/{id}/payByRestCashOnly")
    @PreAuthorize("isAuthenticated()")
    public String payByRestCashOnly(@AuthenticationPrincipal MemberContext memberContext, @PathVariable long id) {
        Order order = orderService.findForPrintById(id).get();

        Member actor = memberContext.getMember();

        long restCash = memberService.getRestCash(actor);

        if (orderService.actorCanPayment(actor, order) == false) {
            throw new ActorCanNotPayOrderException();
        }

        orderService.payByRestCashOnly(order);

        return "redirect:/order/%d?msg=%s".formatted(order.getId(), Ut.url.encode("예치금으로 결제했습니다."));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String showDetail(@AuthenticationPrincipal MemberContext memberContext, @PathVariable long id, Model model) {
        Order order = orderService.findForPrintById(id).get();

        Member actor = memberContext.getMember();

        long restCash = memberService.getRestCash(actor);

        if (orderService.actorCanSee(actor, order) == false) {
            throw new ActorCanNotSeeOrderException();
        }

        model.addAttribute("order", order);
        model.addAttribute("actorRestCash", restCash);

        return "order/detail";
    }

    @PostConstruct
    private void init() {
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) {
            }
        });
    }

    private final String SECRET_KEY = "test_sk_DLJOpm5QrldPWmka04z5rPNdxbWn";

    @RequestMapping("/{id}/success")
    public String confirmPayment(
            @PathVariable long id,
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            Model model,
            @AuthenticationPrincipal MemberContext memberContext
    ) throws Exception {

        Order order = orderService.findForPrintById(id).get();

        long orderIdInputed = Long.parseLong(orderId.split("__")[1]);

        if (id != orderIdInputed) {
            throw new OrderIdNotMatchedException();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes()));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("orderId", orderId);
        payloadMap.put("amount", String.valueOf(amount));

        Member actor = memberContext.getMember();
        long restCash = memberService.getRestCash(actor);
        long payPriceRestCash = order.calculatePayPrice() - amount;

        if (payPriceRestCash > restCash) {
            throw new OrderNotEnoughRestCashException();
        }

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

        ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/payments/" + paymentKey, request, JsonNode.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {

            orderService.payByTossPayments(order, payPriceRestCash, paymentKey);

            return "redirect:/order/%d?msg=%s".formatted(order.getId(), Ut.url.encode("결제가 완료되었습니다."));
        } else {
            JsonNode failNode = responseEntity.getBody();
            model.addAttribute("message", failNode.get("message").asText());
            model.addAttribute("code", failNode.get("code").asText());
            return "order/fail";
        }
    }

    @RequestMapping("/{id}/fail")
    public String failPayment(@RequestParam String message, @RequestParam String code, Model model) {
        model.addAttribute("message", message);
        model.addAttribute("code", code);
        return "order/fail";
    }

    @PostMapping("/makeOrder")
    @PreAuthorize("isAuthenticated()")
    public String makeOrder(@AuthenticationPrincipal MemberContext memberContext) {
        Member member = memberContext.getMember();
        Order order = orderService.createFromCart(member);
        String redirect = "redirect:/order/%d".formatted(order.getId()) + "?msg=" + Ut.url.encode("%d번 주문이 생성되었습니다.".formatted(order.getId()));

        return redirect;
    }

    @RequestMapping("/refund/{id}")
    @ResponseBody
    public RsData refund(
            @PathVariable Long id
    ) throws Exception {

        Order order = orderService.findForPrintById(id).get();
        String paymentKey = order.getPaymentKey();
        String reason = "단순 변심";

        // 요청 URL
        String url = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

        // HTTP 헤더 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes()));
        headers.add("Content-Type", "application/json");

        // 요청 본문 데이터 생성
        Map<String, String> body = new HashMap<>();
        body.put("cancelReason", "단순 변심");

        // HttpEntity 객체 생성 (헤더 + 본문)
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        // RestTemplate 생성 및 요청 보내기
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // 상태 코드, 헤더, 본문 출력
            System.out.println("Status Code: " + responseEntity.getStatusCode());
            System.out.println("Headers: " + responseEntity.getHeaders());
            System.out.println("Response Body: " + responseEntity.getBody());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return RsData.of("S-1", "성공했으면 좋겠다...", null);

    }
}



// 요청 URL
//        String url = "https://api.tosspayments.com/v1/payments/" + paymentKey;
//
//        // HTTP 헤더 생성
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes()));
//
//        // HttpEntity 객체 생성 (헤더만 필요한 경우 본문은 null로 설정)
//        HttpEntity<String> entity = new HttpEntity<>(null, headers);
//
//        // RestTemplate 생성 및 요청 보내기
//        RestTemplate restTemplate = new RestTemplate();
//
//        try {
//            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
//
//            // 상태 코드, 헤더, 본문 출력
//            System.out.println("Status Code: " + responseEntity.getStatusCode());
//            System.out.println("Headers: " + responseEntity.getHeaders());
//            System.out.println("Response Body: " + responseEntity.getBody());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return RsData.of("S-1" , "성공!", null);

//        String orderId = "order__4__659854507056832";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(("test_sk_DLJOpm5QrldPWmka04z5rPNdxbWn" + ":").getBytes()));
//        HttpEntity<String> request = new HttpEntity<>(headers);
//
//        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(
//                "https://api.tosspayments.com/v1/payments/orders/" + orderId, JsonNode.class);
//
//        System.out.println("Headers: " + headers);
//        System.out.println("Request Body: " + request.getBody());
//
//        System.out.println("HTTP Status Code: " + responseEntity.getStatusCode());
//        System.out.println("Response Body: " + responseEntity.getBody());
//
//
//        if (responseEntity.getStatusCode() == HttpStatus.OK) {
//            JsonNode responseBody = responseEntity.getBody();
//            String paymentKey = responseBody.get("paymentKey").asText();
//
//            System.out.println("paymentKey : " + paymentKey);
//
//            return RsData.of("S-1", "여기까지는 성공했습니다.", null);
//        } else {
//            return RsData.of("F-1", "결제정보가 존재하지 않습니다.", null);
//        }


//
//    }

