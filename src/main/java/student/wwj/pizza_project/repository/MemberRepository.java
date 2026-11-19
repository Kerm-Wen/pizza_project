package student.wwj.pizza_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import student.wwj.pizza_project.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByAccount(String account);
    
    // 透過 帳號 + 電話 來驗證是否為本人
    Optional<Member> findByAccountAndPhone(String account, String phone);
}