package com.lyp.breakpoint.entity;

/**
 * 
* @Title: Range类
* @Description: 用于记录请求指定区间的数据
* @author lyp  
* @date 2017年5月22日 
* @version V1.0
 */
public class Range {
	private long from;
	private long to;
	private long size;

	public Range(long from, long to, long size) {
		this.from = from;
		this.to = to;
		this.size = size;
	}

	public long getFrom() {
		return from;
	}

	public void setFrom(long from) {
		this.from = from;
	}

	public long getTo() {
		return to;
	}

	public void setTo(long to) {
		this.to = to;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}
}
