package com.example.servletmvc.controller;

import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/upload")
    public String upload(MultipartHttpServletRequest request) {
        System.out.println(request);
        MultipartFile p1 = request.getFile("p1");
        return "Hello Cors!";
    }
}
