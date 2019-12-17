package it.unisa.adc.gitprotocol.beans;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Repository implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String repositoryName;
	private String directoryName;
	private Set<GitFile> filesRepository;
	private Set<GitFile> filesToCommit; 
	private List<Commit> commits;
	private LocalDateTime lastEdit;
	private long head;
	
	public Repository(String repositoryName, File directory) {
		this.repositoryName = repositoryName;
		this.directoryName = directory.getName();
		this.filesRepository = new HashSet<>();
		this.filesToCommit = new HashSet<>();
		this.commits = new ArrayList<>();
		this.lastEdit = LocalDateTime.now();
		this.head = 0L;
	}
	
	public void addFile(GitFile gitFile) {
		Iterator<GitFile> it = filesRepository.iterator();
		GitFile gf = null;
		GitFile gfRemove = null;
		while(it.hasNext()) {
			gf = it.next();
			if(gf.getName().equals(gitFile.getName())) {
				gfRemove = gf;
				break;
			}
		}
		this.filesRepository.remove(gfRemove);
		this.filesRepository.add(gitFile);
		this.filesToCommit.add(gitFile);
	}
	
	public void addFiles(Set<GitFile> gitFiles) {
		Iterator<GitFile> itGitFilesToAdd = gitFiles.iterator();
		GitFile gfToAdd = null;
		GitFile gfInRepo = null;
		Set<GitFile> gfRemove = new HashSet<>();
		while(itGitFilesToAdd.hasNext()) {
			gfToAdd = itGitFilesToAdd.next();
			Iterator<GitFile> itGitFilesRepository = this.filesRepository.iterator();
			while(itGitFilesRepository.hasNext()) {
				gfInRepo = itGitFilesRepository.next();
				if(gfInRepo.getName().equals(gfToAdd.getName()) && gfInRepo.hashCode() != gfToAdd.hashCode()) {
					gfRemove.add(gfInRepo);
					break;
				}
			}
		}
		for(GitFile gf : gfRemove)
			this.filesRepository.remove(gf);
		this.filesRepository.addAll(gitFiles);
		this.filesToCommit.addAll(gitFiles);
	}

	public String getRepositoryName() {
		return this.repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public String getDirectoryName() {
		return this.directoryName;
	}

	public void setDirectoryName(String directoryName) {
		this.directoryName = directoryName;
	}

	public Set<GitFile> getFilesRepository() {
		return this.filesRepository;
	}

	public Set<GitFile> getFilesCommit() {
		return this.filesToCommit;
	}
	
	public void clearFilesToCommit() {
		this.filesToCommit = new HashSet<>(this.filesToCommit);
		this.filesToCommit.clear();
	}

	public List<Commit> getCommits() {
		return this.commits;
	}

	public void setCommits(List<Commit> commits) {
		this.commits = commits;
		this.lastEdit = commits.get(commits.size()-1).getDate();
	}
	
	public void addCommit(Commit commit) {
		this.commits.add(commit);
		this.lastEdit = commit.getDate();
	}
	
	public LocalDateTime getLastEdit() {
		return lastEdit;
	}
	
	public long getHead() {
		return head;
	}

	public void setHead(long head) {
		this.head = head;
	}

	@Override
	public String toString() {
		return "Repository [repositoryName=" + repositoryName + ", directoryName=" + directoryName
				+ ", filesRepository=" + filesRepository + ", filesToCommit=" + filesToCommit + ", commits=" + commits
				+ ", lastEdit=" + lastEdit + ", head=" + head + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Repository other = (Repository) obj;
		if (commits == null) {
			if (other.commits != null)
				return false;
		} else if (!commits.equals(other.commits))
			return false;
		if (directoryName == null) {
			if (other.directoryName != null)
				return false;
		} else if (!directoryName.equals(other.directoryName))
			return false;
		if (filesRepository == null) {
			if (other.filesRepository != null)
				return false;
		} else if (!filesRepository.equals(other.filesRepository))
			return false;
		if (filesToCommit == null) {
			if (other.filesToCommit != null)
				return false;
		} else if (!filesToCommit.equals(other.filesToCommit))
			return false;
		if (head != other.head)
			return false;
		if (lastEdit == null) {
			if (other.lastEdit != null)
				return false;
		} else if (!lastEdit.equals(other.lastEdit))
			return false;
		if (repositoryName == null) {
			if (other.repositoryName != null)
				return false;
		} else if (!repositoryName.equals(other.repositoryName))
			return false;
		return true;
	}
	
}
