package com.bjpowernode.controller;

import com.bjpowernode.pojo.ProductInfo;
import com.bjpowernode.pojo.vo.ProductInfoVo;
import com.bjpowernode.service.ProductInfoService;
import com.bjpowernode.utils.FileNameUtil;
import com.github.pagehelper.PageInfo;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/prod")
public class ProductInfoAction {
    //扩大范围，文件异步上传的名称！
    String saveFileName = "";

    //每页显示的记录数
    public static final int PAGE_SIZE = 5;
    @Autowired
    ProductInfoService productInfoService;

    //显示所有商品，不分页
    @RequestMapping("/getAll")
    public String getAll(HttpServletRequest request) {
        List<ProductInfo> list = productInfoService.getAll();
        request.setAttribute("list", list);
        return "product";
    }

    //显示第一页五条记录
    @RequestMapping("/split")
    public String split(HttpServletRequest request) {
        //得到第一页数据
        PageInfo info = productInfoService.splitPage(1, PAGE_SIZE);
        request.setAttribute("info", info);
        return "product";
    }

    //ajax分页处理，客户端提交那一业，那一页就处理！
    @ResponseBody
    @RequestMapping("/ajaxsplit")
    public void ajaxSplit(int page, HttpSession session) {
        //获取当前page参数
        PageInfo info = productInfoService.splitPage(page, PAGE_SIZE);
        session.setAttribute("info", info);
    }

    //上传图片和把图片返回给前端页面
    @ResponseBody
    @RequestMapping(value = "/ajaxImg")
    public Object ajaxImg(HttpServletRequest request, MultipartFile pimage) {
        // 提取生成文件名(UUID+文件名后缀)
        saveFileName = FileNameUtil.getUUIDFileName() + FileNameUtil.getFileType(pimage.getOriginalFilename());
        // 得到项目中图片存储的路径
        String path = request.getServletContext().getRealPath("/image_big");
        System.out.println("path:" + path);
        // 转存
        try {
            pimage.transferTo(new File(path + File.separator + saveFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 返回给客户端JSON对象，封装图片的路径，为了在页面实现立即回显
        JSONObject object = new JSONObject();
        object.put("imgurl", saveFileName);

        return object.toString();
    }

    //添加商品功能
    @RequestMapping("/save")
    public String save(ProductInfo info, HttpServletRequest request) {
        info.setpImage(saveFileName);
        info.setpDate(new Date());
        //info对象有表单提交的5个数据，现在把数据放到数据库
        int num = -1;
        try {
            num = productInfoService.save(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (num > 0) {
            request.setAttribute("msg", "商品添加成功");
        } else {
            request.setAttribute("msg", "添加失败");
        }
        //增加成功后更新数据库，跳转到分页显示的action
        saveFileName = "";
        return "forward:/prod/split.action";

    }

    //根据主键查询商品,然后跳转到到update的界面
    @RequestMapping("/one")
    public String one(int pid, Model mode) {
        //调用业务逻辑层,根据前端提供的主键值进行业务查询
        ProductInfo info = productInfoService.getById(pid);
        mode.addAttribute("prod", info);
        return "update";
    }

    @RequestMapping("/update")
    public String update(ProductInfo info, HttpServletRequest request) {
        //因为ajax的异步图片上传,如果有上传过,
        // 则saveFileName里有上传上来的图片的名称,
        // 如果没有使用异步ajax上传过图片,则saveFileNme="",
        // 实体类info使用隐藏表单域提供上来的pImage原始图片的名称;
        if (!saveFileName.equals("")) {
            info.setpImage(saveFileName);
        }
        //完成更新处理
        int num = -1;
        try {
            num = productInfoService.update(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (num > 0) {
            //此时说明更新成功
            request.setAttribute("msg", "更新成功!");
        } else {
            //更新失败
            request.setAttribute("msg", "更新失败!");
        }

        //处理完更新后,saveFileName里有可能有数据,
        // 而下一次更新时要使用这个变量做为判断的依据,就会出错,所以必须清空saveFileName.
        saveFileName = "";
        return "forward:/prod/split.action";
    }

    @RequestMapping("/delete")
    public String delete(int pid,HttpServletRequest request) {
        int num = -1;
        //删除一定要加try,catch！
        try {
            num = productInfoService.delete(pid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //进行判断！
        if (num > 0) {
            request.setAttribute("msg", "删除成功!");
        } else {
            request.setAttribute("msg", "删除失败!");
        }

        //删除结束后跳到分页显示
        return "forward:/prod/deleteAjaxSplit.action";
    }
    @ResponseBody
    @RequestMapping(value = "/deleteAjaxSplit", produces = "text/html;charset=UTF-8")
    public Object deleteAjaxSplit(HttpServletRequest request) {
        //取得第1页的数据
        PageInfo info = null;
        info = productInfoService.splitPage(1, PAGE_SIZE);
        request.getSession().setAttribute("info",info);
        return request.getAttribute("msg");
    }


    //批量删除商品
    @RequestMapping("/deleteBatch")
    //pids="1,4,5"  ps[1,4,5]
    public String deleteBatch(String pids,HttpServletRequest request){
        //将上传上来的字符串截开,形成商品id的字符数组
        String []ps = pids.split(",");

        try {
            int num = productInfoService.deleteBatch(ps);
            if(num > 0 ){
                request.setAttribute("msg","批量删除成功!");
            }else{
                request.setAttribute("msg","批量删除失败!");
            }
        } catch (Exception e) {
            request.setAttribute("msg","商品不可删除!");
        }
        return "forward:/prod/deleteAjaxSplit.action";
    }

    //   多条件查询功能实现
    @ResponseBody
    @RequestMapping("/condition")
    public void condition(ProductInfoVo vo, HttpSession session){
        List<ProductInfo> list = productInfoService.selectCondition(vo);
        session.setAttribute("list",list);
    }
}
