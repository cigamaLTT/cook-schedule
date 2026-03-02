package com.cigama.cook_schedule.repository;

import com.cigama.cook_schedule.entity.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserScheduleRepository extends JpaRepository<UserSchedule, String> {
}
