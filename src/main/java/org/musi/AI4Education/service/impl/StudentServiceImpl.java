package org.musi.AI4Education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.domain.Student;
import org.musi.AI4Education.mapper.StudentMapper;
import org.musi.AI4Education.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Autowired
    private StudentMapper studentMapper; // 注入StudentMapper接口

    @Override
    public Student getStudentByUsername(String username) {
        QueryWrapper<Student> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        Student student = studentMapper.selectOne(wrapper);
        return student;
    }

    @Override
    public Student getStudentBySid(String sid) {
        QueryWrapper<Student> wrapper = new QueryWrapper<>();
        wrapper.eq("sid", sid);
        Student student = studentMapper.selectOne(wrapper);
        return student;
    }

    @Override
    public String getSidByUsername(String username) {
        QueryWrapper<Student> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        Student student = studentMapper.selectOne(wrapper);
        return student.getSid();
    }

    @Override
    public boolean updateStudentInfo(Student student) {
        int result = studentMapper.updateById(student);
        if(result==1){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean validation(String username, String password) {
        //使用studentMapper获取用户信息
        QueryWrapper<Student> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        wrapper.eq("password", password);
        Student student = studentMapper.selectOne(wrapper);
        if (student != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CommonResponse<String> register(Student student) {
        // 先判断用户名是否在数据库中存在
        QueryWrapper<Student> wrapper = new QueryWrapper<>();
        wrapper.eq("username", student.getUsername());
        Student student1 = studentMapper.selectOne(wrapper);
        if (student1 != null) {
            return CommonResponse.creatForError( "用户名已存在");
        } else {
            // 生成不重复的sid
            String sid = String.valueOf(System.currentTimeMillis());
            Student student2 = new Student();
            student2.setSid(sid);

            //学生基本信息
            student2.setUsername(student.getUsername());
            student2.setPassword(student.getPassword());
            student2.setPhone(student.getPhone());
            student2.setEmail(student.getEmail());
            student2.setGender(student.getGender());
            student2.setDescription(student.getDescription());

            //学生学业信息
            student2.setGrade(student.getGrade());
            student2.setMajor(student.getMajor());
            student2.setRanking(student.getRanking());

            // 不存在则插入
            studentMapper.insert(student2);
            return CommonResponse.creatForSuccess("注册成功");
        }
    }
    @Override
    public void updateStudentState(String sid,int state) {
        Student student = studentMapper.selectById(sid);
        student.setIsLogin(state);
        studentMapper.updateById(student);
    }
}
