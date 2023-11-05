package com.example.demo.domain.member.service;

import com.example.demo.base.rsData.RsData;
import com.example.demo.domain.cash.entity.CashLog;
import com.example.demo.domain.cash.service.CashService;
import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.exception.AlreadyJoinException;
import com.example.demo.domain.member.repository.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CashService cashService;

    public Member join(String username, String password, String email) {
        if (memberRepository.findByUsername(username).isPresent()) {
            throw new AlreadyJoinException();
        }

        Member member = Member.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .build();

        memberRepository.save(member);

        return member;
    }

    @Transactional(readOnly = true)
    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }

    @Transactional
    public RsData<AddCashRsDataBody> addCash(Member member, long price, String eventType) {
        CashLog cashLog = cashService.addCash(member, price, eventType);

        long newRestCash = member.getRestCash() + cashLog.getPrice();
        member.setRestCash(newRestCash);
        memberRepository.save(member);

        return RsData.of(
                "S-1",
                "성공",
                new AddCashRsDataBody(cashLog, newRestCash)
        );
    }

    @Data
    @AllArgsConstructor
    public static class AddCashRsDataBody {
        CashLog cashLog;
        long newRestCash;
    }

    public long getRestCash(Member member) {
        Member foundMember = findByUsername(member.getUsername()).get();

        return foundMember.getRestCash();
    }
}