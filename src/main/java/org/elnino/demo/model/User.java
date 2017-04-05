package org.elnino.demo.model;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="operuser")
public class User {
	
	public String Gh;
	public String Name;
	public String GhType;
	
	public String getGh() {
		return Gh;
	}

	public void setGh(String gh) {
		Gh = gh;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public String getGhType() {
		return GhType;
	}

	public void setGhType(String ghType) {
		GhType = ghType;
	}
	
}
