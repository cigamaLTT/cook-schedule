package com.cigama.cook_schedule.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approved_roster")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedRoster {

    @Id
    private Integer id;

    private String noMarket;
    private String noCookNoon;
    private String noWashNoon;
    private String noCookNight;
    private String noWashNight;
    private Integer seed;
}
