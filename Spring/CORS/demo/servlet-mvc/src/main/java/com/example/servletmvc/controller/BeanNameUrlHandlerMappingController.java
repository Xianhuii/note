package com.example.servletmvc.controller;


import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * BeanNameUrlHandlerMapping使用
 *
 * @author jxh
 * @date 2022年11月14日 10:25
 */
@Component("/BeanNameUrlHandlerMappingController")
public class BeanNameUrlHandlerMappingController implements Controller {
    /**
     * Process the request and return a ModelAndView object which the DispatcherServlet
     * will render. A {@code null} return value is not an error: it indicates that
     * this object completed request processing itself and that there is therefore no
     * ModelAndView to render.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @return a ModelAndView to render, or {@code null} if handled directly
     * @throws Exception in case of errors
     */
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter writer = response.getWriter();
        writer.write("Hello BeanNameUrlHandlerMapping");
        writer.flush();
        return null;
    }
}
