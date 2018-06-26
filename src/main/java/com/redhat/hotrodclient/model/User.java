package com.redhat.hotrodclient.model;

import java.io.Serializable;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoDoc("@Indexed")
public class User implements Serializable {

	private static final long serialVersionUID = -6965601136887211042L;

	private String userName;
	
	private String password;

	@ProtoField(number = 1, required = true)
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@ProtoField(number = 2, required = true)
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
