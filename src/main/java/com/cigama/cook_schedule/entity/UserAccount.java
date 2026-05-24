package com.cigama.cook_schedule.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    private String userId;

    private String displayName;
    private String password;
    private String role;

    public String getDecryptedPassword() {
        try {
            String decoded = this.password;
            for (int i = 0; i < 3; i++) {
                decoded = new String(java.util.Base64.getDecoder().decode(decoded));
            }
            return decoded;
        } catch (Exception e) {
            return "N/A";
        }
    }
}
