package student.wwj.pizza_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import student.wwj.pizza_project.entity.Member;
import student.wwj.pizza_project.repository.MemberRepository;

import java.util.Optional;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    // 建立密碼加密工具
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 註冊邏輯
    public void register(String account, String password, String name, String phone) {
        if (memberRepository.findByAccount(account).isPresent()) {
            throw new RuntimeException("此帳號已經被註冊過了！");
        }
        
        Member member = new Member();
        member.setAccount(account);
        member.setPassword(passwordEncoder.encode(password)); // 密碼加密後儲存
        member.setName(name);
        member.setPhone(phone);
        
        memberRepository.save(member);
    }

    // 登入驗證邏輯
    public Member login(String account, String password) {
        Optional<Member> optionalMember = memberRepository.findByAccount(account);
        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();
            // 比對輸入的密碼與資料庫中的加密密碼
            if (passwordEncoder.matches(password, member.getPassword())) {
                return member;
            }
        }
        throw new RuntimeException("帳號或密碼錯誤！");
    }

    // 忘記密碼並重設
    public void resetPassword(String account, String phone, String newPassword) {
        Member member = memberRepository.findByAccountAndPhone(account, phone)
                .orElseThrow(() -> new RuntimeException("帳號或聯絡電話輸入錯誤，無法驗證身分！"));
        
        // 將新密碼加密後存入
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }
}