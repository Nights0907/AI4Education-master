package org.musi.AI4Education.controller;

import org.musi.AI4Education.domain.dto.StudentDTO;
import org.musi.AI4Education.domain.entity.Student;
import org.musi.AI4Education.domain.request.LoginRequest;
import org.musi.AI4Education.domain.response.LoginResponse;
import cn.dev33.satoken.stp.StpUtil;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/student")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return loginWithCredentials(request.getUsername(), request.getPassword());
    }

    @Deprecated
    @GetMapping("/login")
    public CommonResponse<LoginResponse> loginLegacy(@RequestParam String username, @RequestParam String password) {
        return loginWithCredentials(username, password);
    }

    private CommonResponse<LoginResponse> loginWithCredentials(String username, String password) {
        Student student = studentService.authenticate(username, password);
        if (student == null) {
            return CommonResponse.creatForError("用户名或密码错误");
        }
        StpUtil.login(student.getSid());
        String token = StpUtil.getTokenValue();
        studentService.updateStudentState(student.getSid(),1);
        System.out.println("[Login] sid=" + student.getSid() + ", token=" + token);
        return CommonResponse.creatForSuccess(new LoginResponse(student.getSid(), token));
    }

    @PostMapping("/register")
    public CommonResponse<String> registerUser(@RequestBody Student student) {
        // Return the token to the frontend
        return studentService.register(student);
    }

    @GetMapping("/logout")
    public CommonResponse<String> logout() {
        if (!StpUtil.isLogin()) {
            return CommonResponse.creatForError("已经登出！");
        } else {
            String sid = StpUtil.getLoginIdAsString();
            studentService.updateStudentState(sid,0);
            // Return the token to the frontend
            StpUtil.logout();
            return CommonResponse.creatForSuccess("success");
        }
    }

    @GetMapping("/info")
    public CommonResponse<StudentDTO> getStudentInfo() {
        if (StpUtil.isLogin()) {
            String sid = StpUtil.getLoginIdAsString();
            Student student = studentService.getStudentBySid(sid);
            return CommonResponse.creatForSuccess(StudentDTO.from(student));
        } else {
            return CommonResponse.creatForError("请先登录！");
        }
    }

    @PutMapping("/info")
    public CommonResponse<StudentDTO> UpdateStudentInfo(@RequestBody Student student) {
        if (StpUtil.isLogin()) {
            String sid = StpUtil.getLoginIdAsString();
            student.setSid(sid);
            student.setIsLogin(1);
            if (studentService.updateStudentInfo(student)) {
                return CommonResponse.creatForSuccess(StudentDTO.from(student));
            } else {
                return CommonResponse.creatForError("fail");
            }
        } else {
            return CommonResponse.creatForError("请先登录！");
        }
    }
}
