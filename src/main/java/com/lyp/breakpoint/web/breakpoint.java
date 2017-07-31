package com.lyp.breakpoint.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.json.JSONException;
import org.json.JSONObject;

import com.lyp.breakpoint.config.Configurations;
import com.lyp.breakpoint.entity.Range;
import com.lyp.breakpoint.exception.StreamException;
import com.lyp.breakpoint.util.IoUtil;
import com.lyp.breakpoint.util.TokenUtil;
/**
 * 断点续传实现类
* @Title: breakpoint.java 
* @Description: 
* @author lyp  
* @date 2017年5月22日 
* @version V1.0
 */
public class breakpoint implements breakpointInterface {
	public breakpoint() {
		super();
	}


	//	StreamServlet
	static final int BUFFER_LENGTH = 10240;
	static final String START_FIELD = "start";
	public static final String CONTENT_RANGE_HEADER = "content-range";
	
	
	
//	FormDataServlet	
	
	private static final String METHOD_DELETE = "DELETE";
    private static final String METHOD_HEAD = "HEAD";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_TRACE = "TRACE";
    @SuppressWarnings("unused")
	private static final String HEADER_IFMODSINCE = "If-Modified-Since";
    @SuppressWarnings("unused")
	private static final String HEADER_LASTMOD = "Last-Modified";
    @SuppressWarnings("unused")
	private static final String LSTRING_FILE ="javax.servlet.http.LocalStrings";
	
	
	
	
//	TokenServlet
	static final String FILE_NAME_FIELD = "name";
	static final String FILE_SIZE_FIELD = "size";
	static final String TOKEN_FIELD = "token";
	static final String SERVER_FIELD = "server";
	static final String SUCCESS = "success";
	static final String MESSAGE = "message";

	/**
	 * 获取已上传文件大小
	 */
	public void uploadByGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doOptions(req, resp);
		final String token = req.getParameter(TOKEN_FIELD);
		final String size = req.getParameter(FILE_SIZE_FIELD);
		final String fileName = req.getParameter(FILE_NAME_FIELD);
		final PrintWriter writer = resp.getWriter();
	
		JSONObject json = new JSONObject();
		long start = 0;
		boolean success = true;
		String message = "";
		try {
			File f = IoUtil.getTokenedFile(token);
			start = f.length();
			/** file size is 0 bytes. */
			if (token.endsWith("_0") && "0".equals(size) && 0 == start) f.renameTo(IoUtil.getFile(fileName));
		} catch (FileNotFoundException fne) {
			message = "Error: " + fne.getMessage();
			success = false;
		} finally {
			try {
				if (success)
					json.put(START_FIELD, start);
				json.put(SUCCESS, success);
				json.put(MESSAGE, message);
			} catch (JSONException e) {}
			writer.write(json.toString());
			IoUtil.close(writer);
		}
	}

	public void uploadByPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	
		doOptions(req, resp);
		final String token = req.getParameter(TOKEN_FIELD);
		final String fileName = req.getParameter(FILE_NAME_FIELD);
		Range range = IoUtil.parseRange(req);
		
		OutputStream out = null;
		InputStream content = null;
		final PrintWriter writer = resp.getWriter();
		
		JSONObject json = new JSONObject();
		long start = 0;
		boolean success = true;
		String message = "";
		File tokenFile = IoUtil.getTokenedFile(token);
		try {
			if (tokenFile.length() != range.getFrom()) {
				/** drop this uploaded data */
				throw new StreamException(StreamException.ERROR_FILE_RANGE_START);
			}
			
			out = new FileOutputStream(tokenFile, true);
			content = req.getInputStream();
			int read = 0;
			final byte[] bytes = new byte[BUFFER_LENGTH];
			while ((read = content.read(bytes)) != -1)
				out.write(bytes, 0, read);

			start = tokenFile.length();
		} catch (StreamException se) {
			success = StreamException.ERROR_FILE_RANGE_START == se.getCode();
			message = "Code: " + se.getCode();
		} catch (FileNotFoundException fne) {
			message = "Code: " + StreamException.ERROR_FILE_NOT_EXIST;
			success = false;
		} catch (IOException io) {
			message = "IO Error: " + io.getMessage();
			success = false;
		} finally {
			IoUtil.close(out);
			IoUtil.close(content);

			/** rename the file */
			if (range.getSize() == start) {
				try {
					// 先删除
					IoUtil.getFile(fileName).delete();
					
					Files.move(tokenFile.toPath(), tokenFile.toPath().resolveSibling(fileName));
					System.out.println("TK: `" + token + "`, NE: `" + fileName + "`");
					
					if (Configurations.isDeleteFinished()) {
						IoUtil.getFile(fileName).delete();
					}
				} catch (IOException e) {
					success = false;
					message = "Rename file error: " + e.getMessage();
				}
				
			}
			try {
				if (success)
					json.put(START_FIELD, start);
					json.put(SUCCESS, success);
					json.put(MESSAGE, message);
			} catch (JSONException e) {}
				writer.write(json.toString());
				IoUtil.close(writer);
		}
}

	public void flushUpload(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			req.setCharacterEncoding("utf8");
	        final PrintWriter writer = resp.getWriter();
	        // Check that we have a file upload request
	        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
	        if (!isMultipart) {
	            writer.println("ERROR: It's not Multipart form.");
	            return;
	        }
	        JSONObject json = new JSONObject();
	        long start = 0;
	        boolean success = true;
	        String message = "";

	        ServletFileUpload upload = new ServletFileUpload();
	        InputStream in = null;
	        String token = null;
	        try {
	            String filename = null;
	            FileItemIterator iter = upload.getItemIterator(req);
	            while (iter.hasNext()) {
	                FileItemStream item = iter.next();
	                String name = item.getFieldName();
	                in = item.openStream();
	                if (item.isFormField()) {
	                    String value = Streams.asString(in);
	                    if (TOKEN_FIELD.equals(name)) {
	                        token = value;
	                        /** TODO: validate your token. */
	                    }
	                    System.out.println(name + ":" + value);
	                } else {
	                    if (token == null || token.trim().length() < 1)
	                        token = req.getParameter(TOKEN_FIELD);
	                    /** TODO: validate your token. */

	                    // 这里不能保证token能有值
	                    filename = item.getName();
	                    if (token == null || token.trim().length() < 1)
	                        token = filename;

	                    start = IoUtil.streaming(in, token, filename);
	                }
	            }

	            System.out.println("Form Saved : " + filename);
	        } catch (FileUploadException fne) {
	            success = false;
	            message = "Error: " + fne.getLocalizedMessage();
	        } finally {
	            try {
	                if (success)
	                    json.put(START_FIELD, start);
	                json.put(SUCCESS, success);
	                json.put(MESSAGE, message);
	            } catch (JSONException e) {
	            }

	            writer.write(json.toString());
	            IoUtil.close(in);
	            IoUtil.close(writer);
	        }
	
	
	
	}

	/**
	 * 生成token  get请求
	 */
	public void generateToken(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
		String name = req.getParameter(FILE_NAME_FIELD);
		String size = req.getParameter(FILE_SIZE_FIELD);
		String token = TokenUtil.generateToken(name, size);
		
		PrintWriter writer = resp.getWriter();
		JSONObject json = new JSONObject();
		
		try {
			json.put(TOKEN_FIELD, token);
			if (Configurations.isCrossed())
				json.put(SERVER_FIELD, Configurations.getCrossServer());
			json.put(SUCCESS, true);
			json.put(MESSAGE, "");
		} catch (JSONException e) {
			
		}
		writer.write(json.toString());
	}
	
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("application/json;charset=utf-8");
		resp.setHeader("Access-Control-Allow-Headers", "Content-Range,Content-Type");
		resp.setHeader("Access-Control-Allow-Origin", Configurations.getCrossOrigins());
		resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
	}
	
	
	 protected void doOptionsByFormData(HttpServletRequest req, HttpServletResponse resp)
		        throws ServletException, IOException
		    {
		        Method[] methods = getAllDeclaredMethods(this.getClass());
		        
		        boolean ALLOW_GET = false;
		        boolean ALLOW_HEAD = false;
		        boolean ALLOW_POST = false;
		        boolean ALLOW_PUT = false;
		        boolean ALLOW_DELETE = false;
		        boolean ALLOW_TRACE = true;
		        boolean ALLOW_OPTIONS = true;
		        
		        for (int i=0; i<methods.length; i++) {
		            String methodName = methods[i].getName();
		            
		            if (methodName.equals("doGet")) {
		                ALLOW_GET = true;
		                ALLOW_HEAD = true;
		            } else if (methodName.equals("doPost")) {
		                ALLOW_POST = true;
		            } else if (methodName.equals("doPut")) {
		                ALLOW_PUT = true;
		            } else if (methodName.equals("doDelete")) {
		                ALLOW_DELETE = true;
		            }
		            
		        }
		        
		        // we know "allow" is not null as ALLOW_OPTIONS = true
		        // when this method is invoked
		        StringBuilder allow = new StringBuilder();
		        if (ALLOW_GET) {
		            allow.append(METHOD_GET);
		        }
		        if (ALLOW_HEAD) {
		            if (allow.length() > 0) {
		                allow.append(", ");
		            }
		            allow.append(METHOD_HEAD);
		        }
		        if (ALLOW_POST) {
		            if (allow.length() > 0) {
		                allow.append(", ");
		            }
		            allow.append(METHOD_POST);
		        }
		        if (ALLOW_PUT) {
		            if (allow.length() > 0) {
		                allow.append(", ");
		            }
		            allow.append(METHOD_PUT);
		        }
		        if (ALLOW_DELETE) {
		            if (allow.length() > 0) {
		                allow.append(", ");
		            }
		            allow.append(METHOD_DELETE);
		        }
		        if (ALLOW_TRACE) {
		            if (allow.length() > 0) {
		                allow.append(", ");
		            }
		            allow.append(METHOD_TRACE);
		        }
		        if (ALLOW_OPTIONS) {
		            if (allow.length() > 0) {
		                allow.append(", ");
		            }
		            allow.append(METHOD_OPTIONS);
		        }
		        
		        resp.setHeader("Allow", allow.toString());
		    }

	 
	  private Method[] getAllDeclaredMethods(Class<? extends breakpoint> class1) {
	        Class<?> clazz = class1;
	        Method[] allMethods = null;
	        while (!clazz.equals(HttpServlet.class)) {
	            Method[] thisMethods = clazz.getDeclaredMethods();
	            if (allMethods != null && allMethods.length > 0) {
	                Method[] subClassMethods = allMethods;
	                allMethods =
	                    new Method[thisMethods.length + subClassMethods.length];
	                System.arraycopy(thisMethods, 0, allMethods, 0,
	                                 thisMethods.length);
	                System.arraycopy(subClassMethods, 0, allMethods, thisMethods.length,
	                                 subClassMethods.length);
	            } else {
	                allMethods = thisMethods;
	            }

	            clazz = clazz.getSuperclass();
	        }

	        return ((allMethods != null) ? allMethods : new Method[0]);
	    }
}
