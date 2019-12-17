package it.unisa.adc.gitprotocol.beans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Set;

public class Commit implements Serializable{

	private static final long serialVersionUID = 1L;
	private String message;
	private HashMap<String, Set<Integer>> modifiedLinesInFiles;
	private LocalDateTime date;
	private static long idCounter = 0;
	private long id;
	
	public Commit(HashMap<String, Set<Integer>> modifiedFiles, String message) {
		this.message = message;
		this.date = LocalDateTime.now();
		this.modifiedLinesInFiles = modifiedFiles;
		this.id = ++idCounter;
	}

	public String getMessageCommit() {
		return message;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public String getMessage() {
		return message;
	}

	public HashMap<String, Set<Integer>> getModifiedLinesInFiles() {
		return modifiedLinesInFiles;
	}
	
	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Commit [message=" + message + ", modifiedLinesInFiles=" + modifiedLinesInFiles + ", date=" + date
				+ ", id=" + id + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Commit other = (Commit) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (id != other.id)
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (modifiedLinesInFiles == null) {
			if (other.modifiedLinesInFiles != null)
				return false;
		} else if (!modifiedLinesInFiles.equals(other.modifiedLinesInFiles))
			return false;
		return true;
	}
	
}
