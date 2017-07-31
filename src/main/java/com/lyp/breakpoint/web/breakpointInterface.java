package com.lyp.breakpoint.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * 
* @Title: breakpointInterface.java 
* @Description: 断点上传接口
* @author lyp  
* @date 2017年5月22日 
* @version V1.0
 */
public interface breakpointInterface {
	
	public void uploadByGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException ;

	public void uploadByPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
	
	public void flushUpload(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
	//	用于生成断点续传、跨域的令牌
	public void generateToken(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException;
}


