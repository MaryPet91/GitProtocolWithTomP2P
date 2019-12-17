package it.unisa.adc.gitprotocol.implementations;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import it.unisa.adc.gitprotocol.beans.Commit;
import it.unisa.adc.gitprotocol.beans.GitFile;
import it.unisa.adc.gitprotocol.beans.Repository;
import it.unisa.adc.gitprotocol.interfaces.GitProtocol;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class GitProtocolImpl implements GitProtocol {

	final private Peer peer;
	final private int DEFAULT_MASTER_PORT = 4000;
	final private PeerDHT _dht;
	private HashMap<String, Repository> repositories;
	private HashMap<String, Set<Integer>> modifiedLinesInFiles;


	public GitProtocolImpl(int _id, String _master_peer) throws Exception {
		repositories = new HashMap<>();
		modifiedLinesInFiles = new HashMap<>();
		peer = new PeerBuilder(Number160.createHash(_id)).ports(DEFAULT_MASTER_PORT+_id).start();
		_dht = new PeerBuilderDHT(peer).start();	
		FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(_master_peer)).ports(DEFAULT_MASTER_PORT).start();
		fb.awaitUninterruptibly();
		if(fb.isSuccess()) {
			peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
		}
		else {
			throw new Exception("Error in master peer bootstrap.");
		}
	}

	/**
	 * Creates new repository in a directory
	 * @param _repo_name a String, the name of the repository.
	 * @param _directory a File, the directory where create the repository.
	 * @return true if it is correctly created, false otherwise.
	 */
	public boolean createRepository(String _repo_name, File _directory) {
		//if given repository name belongs to already existing one 
		//just return false without creating a new one
		if(repositories.get(_repo_name) != null) 
			return false;
		Repository repository = new Repository(_repo_name, _directory);
		repositories.put(_repo_name, repository);
		try {
			FutureGet futureGet = _dht.get(Number160.createHash(_repo_name)).start();
			futureGet.awaitUninterruptibly();
			if(futureGet.isSuccess() && futureGet.isEmpty()) {
				_dht.put(Number160.createHash(_repo_name)).data(new Data(repository)).start().awaitUninterruptibly();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Adds a list of File to the given local repository. 
	 * @param _repo_name a String, the name of the repository.
	 * @param files a list of Files to be added to the repository.
	 * @return true if it is correctly added, false otherwise.
	 */
	public boolean addFilesToRepository(String _repo_name, List<File> files) {
		//if given repository name belongs to already existing one 
		//add files to it
		if(repositories.get(_repo_name) != null) {
			Repository repository = repositories.get(_repo_name);
			Set<GitFile> gitFilesToAdd = new HashSet<>();
			GitFile gitFile = null;
			Scanner scanner = null;
			//create GitFile List
			for(File f: files) {
				gitFile = new GitFile(f.getName());
				List<String> content = new ArrayList<>();
				try {
					scanner = new Scanner(f);
					while(scanner.hasNext()) {
						content.add(scanner.nextLine());
					}
					scanner.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				gitFile.setContent(content);
				gitFilesToAdd.add(gitFile);
			}//end for

			//check whether repository file list is empty
			Set<GitFile> repoGitFiles = repository.getFilesRepository();
			if(repoGitFiles.isEmpty()) {
				repository.addFiles(gitFilesToAdd);

				// add new files to map of edited files
				Set<Integer> indexes = new HashSet<>();
				for(GitFile gitFileToAdd: gitFilesToAdd) {
					for(int k = 0; k < gitFileToAdd.getContent().size(); k++)
					indexes.add(k);
				modifiedLinesInFiles.put(gitFileToAdd.getName(), indexes);
				}

				return true;
			}

			//check whether GitFiles already exist
			GitFile gitFileInRepo = null;
			Set<GitFile> addedFiles = new HashSet<>();
			Iterator<GitFile> filesToAddIt = gitFilesToAdd.iterator();
			GitFile gitFileToAdd = null;

			while (filesToAddIt.hasNext()) {
				gitFileToAdd = filesToAddIt.next();
				Iterator<GitFile> repoGitFilesIt = repoGitFiles.iterator();
				
				while(repoGitFilesIt.hasNext()) {
					Set<Integer> indexes = new HashSet<>();
					gitFileInRepo = repoGitFilesIt.next();
					if(gitFileToAdd.getName().equals(gitFileInRepo.getName())) {// check whether a file is already in repository
						if(gitFileToAdd.hashCode() == gitFileInRepo.hashCode())
							break; //we are trying to add unmodified file to repository
						//we have to find which lines have been changed
						int minContentSize = 0;
						minContentSize = gitFileInRepo.getContent().size() < gitFileToAdd.getContent().size() 
							?  gitFileInRepo.getContent().size() : gitFileToAdd.getContent().size();
	
						// find out which lines got edited
						for(int j = 0; j < minContentSize; j++) {
							if(!gitFileInRepo.getContent().get(j).equals(gitFileToAdd.getContent().get(j))) {
								indexes.add(j);
							}	
						}
						modifiedLinesInFiles.put(gitFileToAdd.getName(), indexes);

						//mark file for update
						addedFiles.add(gitFileToAdd);
						break;
					}//end if
					else if(!repoGitFilesIt.hasNext()) { //we're adding a new file to repository
						//since this is a new file we're marking all its lines as modified 

						for(int k = 0; k < gitFileToAdd.getContent().size(); k++)
							indexes.add(k);
						modifiedLinesInFiles.put(gitFileToAdd.getName(), indexes);
						addedFiles.add(gitFileToAdd);
					}//end else if
				}//end while gitFileInRepo
				filesToAddIt.remove();
			}//end while gitFileToAdd	
			
			//we have to replace the modified files in repository list
			repository.addFiles(addedFiles);
			repositories.put(_repo_name, repository);
			//everything went successfully
			return true;
		}

		//repository doesn't exist
		return false;
	}

	/**
	 * Apply the changing to the files in  the local repository.
	 * @param _repo_name a String, the name of the repository.
	 * @param _message a String, the message for this commit.
	 * @return true if it is correctly committed, false otherwise.
	 */
	public boolean commit(String _repo_name, String _message) {
		Repository repository = repositories.get(_repo_name);
		if(repository != null && repository.getFilesCommit().size() > 0) {
			//clone current edit map
			HashMap<String, Set<Integer>> modifiedFiles = (HashMap<String, Set<Integer>>)modifiedLinesInFiles.clone(); 
			Commit commit = new Commit(modifiedFiles, _message);
			repository.addCommit(commit);
			repository.clearFilesToCommit();
			repositories.put(_repo_name, repository);
			modifiedLinesInFiles.clear(); //clear current map
			return true;
		}
		else 
			return false;
	}

	/**
	 * Push all commits on the Network. If the status of the remote repository is changed, 
	 * the push fails, asking for a pull.
	 * @param _repo_name _repo_name a String, the name of the repository.
	 * @return a String, operation message.
	 */
	public String push(String _repo_name) {
		Repository localRepository = repositories.get(_repo_name);
		if(localRepository == null)
			return "Error! Local repository does not exist!";
		try {
			FutureGet futureGet = _dht.get(Number160.createHash(_repo_name)).start();
			futureGet.awaitUninterruptibly();
			if(futureGet.isSuccess()) {
				if(futureGet.isEmpty())
					return "Error! Remote repository does not exist";
				Repository remoteRepository =  (Repository) futureGet.dataMap().values().iterator().next().object();
				long head = 0L;
				if(localRepository.getLastEdit().isAfter(remoteRepository.getLastEdit())) {
					head = localRepository.getCommits().get(localRepository.getCommits().size()-1).getId();
					localRepository.setHead(head);
					_dht.put(Number160.createHash(_repo_name)).data(new Data(localRepository)).start().awaitUninterruptibly();
					return "Push successful!";
				}
				else if (localRepository.getLastEdit().equals(remoteRepository.getLastEdit()))
					return "Already up to date!";
				else
					return "Push failed! Pull request!";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Push failed!";
	}

	/**
	 * Pull the files from the Network. If there is a conflict, the system duplicates 
	 * the files and the user should manually fix the conflict.
	 * @param _repo_name _repo_name a String, the name of the repository.
	 * @return a String, operation message.
	 */
	public String pull(String _repo_name) {
		Repository localRepository = repositories.get(_repo_name);
		if(localRepository == null)
			return "Error! Local repository does not exist!";
		try {
			FutureGet futureGet = _dht.get(Number160.createHash(_repo_name)).start();
			futureGet.awaitUninterruptibly();
			if(futureGet.isSuccess()) {
				if(futureGet.isEmpty())
					return "Error! Remote repository does not exist";
				Repository remoteRepository =  (Repository) futureGet.dataMap().values().iterator().next().object();
				if(localRepository.getHead() == remoteRepository.getHead()) { 
					//last edit to local repository is at least equals to 
					//last edit to remote repository (or greater)
					if(localRepository.getLastEdit().equals(remoteRepository.getLastEdit()))
						return "Already up to date!";
					else {
						//local can only come after remote
						return "Local repository is ahead of remote, please push!";
					}
						
				}
				else if(localRepository.getHead() == 0L) {
					//local repository got created, but never synced with remote
					//pull can be safely performed
					repositories.put(_repo_name,remoteRepository);
					return "Pull successful!";
				}
				else {
					int index = localRepository.getCommits().size() == 0 ? 0 : localRepository.getCommits().size()-1;
					long lastCommitID = localRepository.getCommits().get(index).getId();
					if(localRepository.getHead() == lastCommitID) {
						//pull can be safely performed
						repositories.put(_repo_name,remoteRepository);
						return "Pull successful!";
					}
					else {
						//local didn't pull before committing
						//conflicts have to be solved
						GitFile gitFileToAdd = null;
						//add remote files not in local repository
						Set<GitFile> difference = new HashSet<>(remoteRepository.getFilesRepository());
						difference.removeAll(localRepository.getFilesRepository());
						for(GitFile gf: difference)
							gf.setName(gf.getName()+".new");
						Iterator<GitFile> differenceIt = difference.iterator();
						while(differenceIt.hasNext()) {
							gitFileToAdd = differenceIt.next();
							localRepository.addFile(gitFileToAdd);
						}
						repositories.put(_repo_name, localRepository);
						return "Found existing conflicts with local files, please check duplicated files!";
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Pull failed!";
	}

	/**
	 * Returns list of files of repository.
	 * @param _repo_name a String, the name of the repository.
	 * @return a List of GitFile, files of repository.
	 */
	public Set<GitFile> getListFilesRepository(String _repo_name) {
		Set<GitFile> files = new HashSet<>();
		try {
			FutureGet futureGet = _dht.get(Number160.createHash(_repo_name)).start();
			futureGet.awaitUninterruptibly();	
			if(futureGet.isSuccess()) {
				if(futureGet.isEmpty())
					return null;
				Repository remoteRepository = (Repository) futureGet.dataMap().values().iterator().next().object();
				files = remoteRepository.getFilesRepository();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return files;
	}

	/**
	 * Removes remote and local repository
	 * @param _repo_name a String, the name of the repository.
	 * @return true if it is correctly removed, false otherwise.
	 */
	public boolean destroyRepository(String _repo_name) {
		try {
			FutureGet futureGet = _dht.get(Number160.createHash(_repo_name)).start();
			futureGet.awaitUninterruptibly();	
			if(futureGet.isSuccess()) {
				if(futureGet.isEmpty())
					return false;
				_dht.remove(Number160.createHash(_repo_name)).start().awaitUninterruptibly();
				repositories.remove(_repo_name);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Returns list of commits.
	 * @param _repo_name a String, the name of the repository.
	 * @return a List of Commit, list of commits.
	 */
	public List<Commit> getListOfCommit(String _repo_name) {
		List<Commit> files = new ArrayList<Commit>();
		try {
			FutureGet futureGet = _dht.get(Number160.createHash(_repo_name)).start();
			futureGet.awaitUninterruptibly();	
			if(futureGet.isSuccess()) {
				if(futureGet.isEmpty())
					return null;
				Repository remoteRepository = (Repository) futureGet.dataMap().values().iterator().next().object();
				files = remoteRepository.getCommits();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return files;
	}
	
	/**
	 * Returns local repository object.
	 * @param _repo_name a String, the name of the repository.
	 * @return local repository.
	 */
	public Repository getLocalRepository(String _repo_name) {
		return repositories.get(_repo_name);
	}
	
	/**
	 * Returns local repository object.
	 * @param _repo_name a String, the name of the repository.
	 * @return local repository.
	 */
	public Repository getRemoteRepository(String _repo_name) {
		Repository repository = null;
		try {
			FutureGet futureGet = _dht.get(Number160.createHash(_repo_name)).start();
			futureGet.awaitUninterruptibly();	
			if(futureGet.isSuccess()) {
				if(futureGet.isEmpty())
					return repository;
				repository = (Repository) futureGet.dataMap().values().iterator().next().object();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return repository;
	}
}
