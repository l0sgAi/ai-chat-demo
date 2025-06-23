package com.losgai.ai.controller.exam;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.losgai.ai.common.Result;
import com.losgai.ai.entity.user.User;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exam/student")
@Slf4j
public class StudentController {

    private final UserService userService;

    @PostMapping("/add")
    @Tag(name = "新增学生", description = "管理员新增学生信息")
    public Result<String> add(@RequestBody User user) {
        ResultCodeEnum resultCodeEnum = userService.addStudent(user);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("新增学生信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @GetMapping("/query")
    @Tag(name = "查询学生", description = "管理员根据关键字分页查询学生信息")
    public Result<List<User>> query(
            @RequestParam(required = false) String keyWord,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<User> users = userService.queryByKeyWord(keyWord);
        // 获取分页信息
        PageInfo<User> pageInfo = new PageInfo<>(users);
        // 使用自定义分页返回方法
        return Result.page(users, pageInfo.getTotal());
    }


    @PutMapping("/update")
    @Tag(name = "编辑学生信息", description = "管理员编辑学生信息")
    public Result<String> update(@RequestBody User user) {
        ResultCodeEnum resultCodeEnum = userService.updateStudent(user);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("编辑学生信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PutMapping("/delete")
    @Tag(name = "逻辑删除学生信息", description = "管理员逻辑删除学生信息")
    public Result<String> delete(@RequestParam Long id) {
        ResultCodeEnum resultCodeEnum = userService.delete(id);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("删除学生信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }


}
