package org.musi.AI4Education.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.domain.Student;
import org.musi.AI4Education.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @GetMapping("/login")
    public CommonResponse<List<String>> login(@RequestParam String username, @RequestParam String password) {
        // 验证是否登录成功并返回token
        if (studentService.validation(username, password)) {
            //sa-token存储登录信息
            String sid = studentService.getSidByUsername(username);
            StpUtil.login(sid);
            String token = StpUtil.getTokenValue();

            ArrayList<String> resultList = new ArrayList<>();
            resultList.add(sid);
            resultList.add(token);

            Student student = studentService.getStudentBySid(sid);
            //更新学生登录状态，设置为已登录
            studentService.updateStudentState(sid,1);
            return CommonResponse.creatForSuccess(resultList);
        } else {
            return CommonResponse.creatForError("用户名或密码错误");
        }
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
    public CommonResponse<Student> getStudentInfo() {
        if (StpUtil.isLogin()) {
            String sid = StpUtil.getLoginIdAsString();
            Student student = studentService.getStudentBySid(sid);
            return CommonResponse.creatForSuccess(student);
        } else {
            return CommonResponse.creatForError("请先登录！");
        }
    }

    @PutMapping("/info")
    public CommonResponse<Student> UpdateStudentInfo(@RequestBody Student student) {
        if (StpUtil.isLogin()) {
            String sid = StpUtil.getLoginIdAsString();
            student.setSid(sid);
            student.setIsLogin(1);
            if (studentService.updateStudentInfo(student)) {
                return CommonResponse.creatForSuccess(student);
            } else {
                return CommonResponse.creatForError("fail");
            }
        } else {
            return CommonResponse.creatForError("请先登录！");
        }
    }
}
