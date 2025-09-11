package org.musi.AI4Education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.domain.Student;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface StudentService extends IService<Student> {
    //通过Username获取学生信息
    public Student getStudentByUsername(String username);

    //通过Sid(学生ID)获取学生信息
    public Student getStudentBySid(String sid);

    //通过Username获取Sid(学生ID)
    public String getSidByUsername(String username);

    //更新学生个人信息
    public boolean updateStudentInfo(Student student);

    //验证用户名和密码是否匹配
    public boolean validation(String username, String password);

    //注册
    public CommonResponse<String> register(Student student);

    //更新学生登录信息
    public void updateStudentState(String sid,int state);
}
