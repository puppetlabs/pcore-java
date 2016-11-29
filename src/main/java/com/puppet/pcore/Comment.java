package com.puppet.pcore;

public class Comment {
	public final String comment;

	public Comment(String comment) {
		this.comment = comment;
	}

	public boolean equals(Object o) {
		return o instanceof Comment && comment.equals(((Comment)o).comment);
	}

	public int hashCode() {
		return comment.hashCode();
	}
}
