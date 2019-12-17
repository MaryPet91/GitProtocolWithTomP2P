package it.unisa.adc.gitprotocol.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.junit.Test;
import junit.framework.TestCase;
import it.unisa.adc.gitprotocol.beans.GitFile;
import it.unisa.adc.gitprotocol.implementations.GitProtocolImpl;

public class GitProtocolImplUnitTest extends TestCase {

	private GitProtocolImpl peer0, peer1;
	boolean flag;
	String repositoryName = "GitProtocol-Unisa";
	String pathDirectory = ".\\";
	File directory;
	File file;
	List<File> files0, files1;
	PrintWriter pw = null;
	Set<GitFile> filesss;


	@Test
	public void testMethods() throws Exception {
		peer0 = new GitProtocolImpl(0, "127.0.0.1");		
		peer1 = new GitProtocolImpl(1, "127.0.0.1");


		directory = new File(pathDirectory+repositoryName);
		directory.mkdir();
		files0 = new ArrayList<>();
		files1 = new ArrayList<>();
		filesss = new HashSet<>();

		for(int i = 0; i < 5; i++) {
			file = new File(directory + "\\Peer0"+i+".txt");
			try{
				file.createNewFile();
				pw = new PrintWriter(file);
				pw.println("Hi, I am peer with ID 0");
				files0.add(file);
				pw.close();
			}catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}


		//create repository
		assertTrue(peer0.createRepository(repositoryName, directory));
		/*
		 * false, repository already exists
		 */
		assertFalse(peer1.createRepository(repositoryName, directory)); 
		//add files to repository
		assertTrue(peer0.addFilesToRepository(repositoryName, files0));
		/*
		 * false, repository does not exist
		 */
		assertFalse(peer0.addFilesToRepository("Hello", files0)); 

		/*
		 * Local Repository contains 5 files because it was the first commit since its creation by peer0
		 */
		assertEquals(5, peer0.getLocalRepository(repositoryName).getFilesRepository().size()); 

		//commit
		assertTrue(peer0.commit(repositoryName, "First commit, by peer0"));

		//Push
		assertEquals("Push successful!", peer0.push(repositoryName)); 
		assertEquals("Already up to date!", peer0.push(repositoryName));

		/*
		 * Remote Repository contains 5 files because it was the first commit since its creation by peer0
		 */
		assertEquals(5, peer0.getRemoteRepository(repositoryName).getFilesRepository().size()); 

		//Pull
		assertEquals("Pull successful!", peer1.pull(repositoryName));
		assertEquals("Already up to date!", peer1.pull(repositoryName));

		file = new File(directory + "\\Peer1(0).txt");
		file.createNewFile();
		files1.add(file);
		assertTrue(peer1.addFilesToRepository(repositoryName, files1));
		assertTrue(peer1.commit(repositoryName, "First commit, by peer1"));
		assertEquals("Local repository is ahead of remote, please push!", peer1.pull(repositoryName));

		file = new File(directory + "\\Peer0(0).txt");
		file.createNewFile();
		files0.add(file);
		assertTrue(peer0.addFilesToRepository(repositoryName, files0));
		assertTrue(peer0.commit(repositoryName, "Second commit, by peer0"));
		assertEquals("Push successful!", peer0.push(repositoryName));

		file = new File(directory + "\\Peer1(1).txt");
		file.createNewFile();
		files1.add(file);
		assertTrue(peer1.addFilesToRepository(repositoryName,files1));
		assertTrue(peer1.commit(repositoryName, "Second commit, by peer1"));
		assertEquals("Found existing conflicts with local files, please check duplicated files!", peer1.pull(repositoryName));
		/*
		 * Repository contains 6 files because peer0 committed a new file - Peer0(0)
		 */
		assertEquals(6, peer0.getRemoteRepository(repositoryName).getFilesRepository().size()); 
		/*
		 * Repository contains 2 commits because only 2 have been pushed successfully
		 */
		assertEquals(2, peer0.getRemoteRepository(repositoryName).getCommits().size()); 

		assertTrue(peer0.destroyRepository(repositoryName));
		assertFalse(peer1.destroyRepository(repositoryName));
		
		files0.clear();
		files1.clear();
		
		try(Stream<Path> walk = Files.walk(Paths.get(directory.getPath()))){
			List<File> result = null;
			result = walk.filter(Files::isRegularFile).map(x -> x.toFile()).collect(Collectors.toList());
			if(!result.isEmpty()) {
				result.forEach(file -> {
					file.delete();		
				});
			}
		}catch(IOException e){
			System.err.println(e.getMessage());
		}
		directory.delete();

	}
}
