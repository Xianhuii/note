package com.example.servletmvc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * 测试文件上传的Controller
 *
 * @author jxh
 * @date 2022年10月13日 12:55
 */
@RestController
@RequestMapping("file")
public class FileUploadController {

    @RequestMapping("/upload")
    public String upload(MultipartHttpServletRequest request) {
        // 获取非文件参数
        String value1 = request.getParameter("key1");
        System.out.println(value1); // value1
        String value2 = request.getParameter("key2");
        System.out.println(value2); // value2
        // 获取文件
        MultipartFile file1 = request.getFile("file1");
        System.out.println(file1 != null ? file1.getOriginalFilename() : "null"); // rfc1867.pdf
        MultipartFile file2 = request.getFile("file2");
        System.out.println(file2 != null ? file2.getOriginalFilename() : "null"); // rfc1314.pdf
        return "Hello MultipartResolver!";
    }
}
