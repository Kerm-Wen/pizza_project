package student.wwj.pizza_project.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import student.wwj.pizza_project.entity.Member;
import student.wwj.pizza_project.service.MemberService;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    // 1. 註冊 API
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Member member) {
        try {
            memberService.register(member.getAccount(), member.getPassword(), member.getName(), member.getPhone());
            return ResponseEntity.ok(Map.of("message", "註冊成功！"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

   // 2. 登入 API (使用 HttpSession 記住登入狀態)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            String account = request.get("account");
            String password = request.get("password");
            
            Member member = memberService.login(account, password);
            String role = (member.getRole() != null) ? member.getRole() : "CUSTOMER";
            
            // 登入成功後，將會員資訊與角色存入 Session
            session.setAttribute("loginMember", member);
            session.setAttribute("loginRole", role);
            
            return ResponseEntity.ok(Map.of(
                "message", "登入成功！",
                "name", member.getName(),
                "role", role
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // 3. 檢查目前是否有會員登入
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentMember(HttpSession session) {
        Member member = (Member) session.getAttribute("loginMember");
        if (member != null) {
            return ResponseEntity.ok(member);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "尚未登入"));
        }
    }

    // 4. 登出 API
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // 清除 Session
        return ResponseEntity.ok(Map.of("message", "已成功登出！"));
    }

    // 忘記密碼 / 重設密碼 API
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String account = request.get("account");
            String phone = request.get("phone");
            String newPassword = request.get("newPassword");
            
            memberService.resetPassword(account, phone, newPassword);
            
            return ResponseEntity.ok(Map.of("message", "密碼重設成功！請使用新密碼登入。"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}