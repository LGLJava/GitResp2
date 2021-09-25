package com.bjpowernode.controller;

import com.bjpowernode.pojo.Admin;
import com.bjpowernode.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class AdminAction {
    //切记:在所有的界面层,一定会有业务逻辑层的对象
    @Autowired
    AdminService adminService;

    //实现登判断,并进行相应的跳转
    @RequestMapping("/login")
    public String login(String name , String pwd, HttpServletRequest request){

        Admin admin = adminService.login(name,pwd);
        if(admin != null){
            request.setAttribute("admin","┃﹋ ε ﹋ ┃瓜皮林在此！");
            //表示登录成功，跳转
            return "main";
        }else{
            //登录失败
            request.setAttribute("errmsg","密码错了，什么玩意！！！！");
            return "login";
        }

    }

}
