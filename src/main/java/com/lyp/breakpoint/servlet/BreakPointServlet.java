package com.lyp.breakpoint.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lyp.breakpoint.config.Configurations;
import com.lyp.breakpoint.web.breakpoint;

@SuppressWarnings("serial")
public class BreakPointServlet extends HttpServlet{

	 @Override
	    public void init() throws ServletException {
	    }

	    @Override
	    protected void service(HttpServletRequest req, HttpServletResponse resp)
	            throws ServletException, IOException {
	    	req.setCharacterEncoding("utf-8");
	    	resp.setCharacterEncoding("utf-8");
	    	breakpoint breakpoint = new breakpoint();
	    	if(req.getRequestURI().contains(Configurations.getConfig("MAPPING_TOKEN_URL"))){
	    		breakpoint.generateToken(req, resp);
	    	}else if (req.getRequestURI().contains(Configurations.getConfig("MAPPING_UPLOAD_URL"))) {
	    		if(req.getMethod().equalsIgnoreCase("get"))breakpoint.uploadByGet(req, resp);
	    		if(req.getMethod().equalsIgnoreCase("post"))breakpoint.uploadByPost(req, resp);
			}else if (req.getRequestURI().contains(Configurations.getConfig("MAPPING_FROMUPLOAD_URL"))) {
				breakpoint.flushUpload(req, resp);
			}
	    	return;
	    }

	    @Override
	    public void destroy() {
	        super.destroy();
	    }
	
	
	
	
	
	
	
	
	
	
}
