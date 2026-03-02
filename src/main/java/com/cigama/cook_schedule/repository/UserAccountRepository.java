package com.cigama.cook_schedule.repository;

import com.cigama.cook_schedule.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
}
