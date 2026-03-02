package com.cigama.cook_schedule.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSchedule {

    @Id
    private String username;

    private String noMarket;
    private String noCookNoon;
    private String noWashNoon;
    private String noCookNight;
    private String noWashNight;
}
