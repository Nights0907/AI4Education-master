package org.musi.AI4Education.domain.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
