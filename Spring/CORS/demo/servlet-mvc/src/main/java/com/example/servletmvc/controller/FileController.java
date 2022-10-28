package com.example.servletmvc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO
 *
 * @author jxh
 * @date 2022年10月27日 15:31
 */
@Controller
public class FileController {
    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("file")
    public void file(@RequestParam String fileUrl, HttpServletResponse response) throws IOException {
        System.out.println(1);
        Resource resource = restTemplate.getForObject(fileUrl, Resource.class);
        System.out.println(resource);
        InputStream inputStream = resource.getInputStream();
        OutputStream out = response.getOutputStream();
        int len = 0;
        byte[] b = new byte[1024];
        while ((len = inputStream.read(b)) != -1) {
            out.write(b, 0, len);
            System.out.println(b);
        }
        out.flush();
    }
    @RequestMapping("file1")
    public void file1(@RequestParam String fileUrl, HttpServletResponse response) throws IOException {
        System.out.println(1);
        Resource resource = restTemplate.getForObject(fileUrl, Resource.class);
        System.out.println(resource);
        InputStream inputStream = resource.getInputStream();
        OutputStream out = response.getOutputStream();
        int len = 0;
        byte[] b = new byte[1024];
        while ((len = inputStream.read(b)) != -1) {
            out.write(b, 0, len);
            System.out.println(b);
        }
        out.flush();
    }
    @RequestMapping("file2")
    public void file2(HttpServletResponse response) throws IOException {
        InputStream inputStream = new FileInputStream("d:\\Downloads\\61aeda8c8ca54c96bc2acaeb1dcb0752.mp4");
        OutputStream out = response.getOutputStream();
        int len = 0;
        byte[] b = new byte[1024];
        while ((len = inputStream.read(b)) != -1) {
            out.write(b, 0, len);
            System.out.println(b);
        }
        out.flush();
    }
    @RequestMapping("file3")
    public void file3(HttpServletResponse response) throws IOException {
        InputStream inputStream = new FileInputStream("d:\\Downloads\\61aeda8c8ca54c96bc2acaeb1dcb0752.mp4");
        OutputStream out = response.getOutputStream();
        int len = 0;
        byte[] b = new byte[1024];
        while ((len = inputStream.read(b)) != -1) {
            out.write(b, 0, len);
            System.out.println(b);
        }
        out.flush();
    }
    @RequestMapping("file4")
    public void file4(HttpServletResponse response) throws IOException {
        InputStream inputStream = new FileInputStream("d:\\Downloads\\61aeda8c8ca54c96bc2acaeb1dcb0752.mp4");
        OutputStream out = response.getOutputStream();
        int len = 0;
        byte[] b = new byte[1024];
        while ((len = inputStream.read(b)) != -1) {
            out.write(b, 0, len);
            System.out.println(b);
        }
        out.flush();
    }
    @RequestMapping("file5")
    public void file5(HttpServletResponse response) throws IOException {
        InputStream inputStream = new FileInputStream("d:\\Downloads\\61aeda8c8ca54c96bc2acaeb1dcb0752.mp4");
        OutputStream out = response.getOutputStream();
        int len = 0;
        byte[] b = new byte[1024];
        while ((len = inputStream.read(b)) != -1) {
            out.write(b, 0, len);
            System.out.println(b);
        }
        out.flush();
    }
}
