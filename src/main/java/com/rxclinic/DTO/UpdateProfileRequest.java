package com.rxclinic.DTO;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;
    private String oldPassword;
    private String newPassword;
}