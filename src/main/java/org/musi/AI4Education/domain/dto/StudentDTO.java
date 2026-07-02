package org.musi.AI4Education.domain.dto;

import lombok.Data;
import org.musi.AI4Education.domain.entity.Student;

@Data
public class StudentDTO {
    private String sid;
    private String username;
    private String phone;
    private String email;
    private String gender;
    private String description;
    private String grade;
    private String major;
    private String ranking;
    private int isLogin;

    public static StudentDTO from(Student student) {
        StudentDTO dto = new StudentDTO();
        dto.setSid(student.getSid());
        dto.setUsername(student.getUsername());
        dto.setPhone(student.getPhone());
        dto.setEmail(student.getEmail());
        dto.setGender(student.getGender());
        dto.setDescription(student.getDescription());
        dto.setGrade(student.getGrade());
        dto.setMajor(student.getMajor());
        dto.setRanking(student.getRanking());
        dto.setIsLogin(student.getIsLogin());
        return dto;
    }
}
