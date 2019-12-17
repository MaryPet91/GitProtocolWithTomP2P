package it.unisa.adc.gitprotocol.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GitFile implements Serializable{

	private static final long serialVersionUID = 1L;
	private String name;
	private List<String> content;
	
	public GitFile(String name, List<String> content) {
		this.name = name;
		this.content = content;
	}
	
	public GitFile(String name) {
		this.name = name;
		this.content = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getContent() {
		return content;
	}

	public void setContent(List<String> content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "GitFile [name=" + name + ", contentHashCode=" + content.hashCode() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GitFile other = (GitFile) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (! (content.hashCode() == other.content.hashCode()))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
