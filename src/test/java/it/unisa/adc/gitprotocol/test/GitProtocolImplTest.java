package it.unisa.adc.gitprotocol.test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import it.unisa.adc.gitprotocol.beans.GitFile;
import it.unisa.adc.gitprotocol.beans.Repository;
import it.unisa.adc.gitprotocol.implementations.GitProtocolImpl;

public class GitProtocolImplTest {

	public static void main(String[] args) {

		boolean flag;
		String repositoryName = "GitProtocol-Unisa";
		String path = ".\\";
		File directoryName;
		GitProtocolImpl peer0, peer1;
		List<File> files0, files1, files2;
		File file;


		try {
			peer0 = new GitProtocolImpl(0, "127.0.0.1");
			peer1 = new GitProtocolImpl(1, "127.0.0.1");
			files0 = new ArrayList<File>();
			files1 = new ArrayList<File>();
			files2 = new ArrayList<File>();
			directoryName = new File(path + repositoryName);

			System.out.println("Directory path: " + directoryName.getAbsolutePath());

			if(!directoryName.exists())
				System.out.println("Create directory: "+ directoryName.mkdir());
			else
				System.out.println("Error: Directory "+ directoryName.getAbsolutePath() + " already exists!");
			
			//PUSH TO REPOSITORY
			
			System.out.println("\n\n *** PUSH NON EXISTING REPOSITORY *** \n\n");
			System.out.println(peer0.push(repositoryName));
			
			System.out.println("\n\n *** PULL FROM REPOSITORY *** \n\n");
			System.out.println(peer0.pull(repositoryName));

			//CREATE REPOSITORY
			
			System.out.println("\n\n *** CREATE LOCAL REPOSITORY *** \n\n");

			flag = peer0.createRepository(repositoryName, directoryName);
			System.out.println("createRepository = "+ flag + " by peer0 [true]"); // ---> true 
			flag = peer0.createRepository(repositoryName, directoryName);
			System.out.println("createRepository = "+ flag + " by peer0 [false]"); // ---> false, repository already exists 
			flag = peer1.createRepository(repositoryName, directoryName);
			System.out.println("createRepository = "+ flag + " by peer1 [false]"); // ---> false, repository already exists
			
			//PUSH TO REPOSITORY
			
			System.out.println("\n\n *** PUSH NEW REPOSITORY *** \n\n");
			System.out.println(peer0.push(repositoryName));

			//CREATE FILES FOR REPOSITORY 
			PrintWriter pw = null;
			
			//files0 - Pippo
			for(int i=0; i<3; i++) {
				file = new File(directoryName + "\\Pippo"+i+".txt");
				file.createNewFile();
				pw = new PrintWriter(file);
				pw.println("Ciao Pippo"+i);
				files0.add(file);
				pw.close();
			}


			//CREATE FILES FOR REPOSITORY 
			
			//files1 - Pluto
			for(int i=0; i<2; i++) {
				file = new File(directoryName + "\\Pluto"+i+".txt");
				file.createNewFile();
				pw = new PrintWriter(file);
				pw.println("Ciao Pluto"+i);
				files1.add(file);
				pw.close();
			}

			Repository repository = null;

			//ADD FILES TO LOCAL REPOSITORY
			System.out.println("\n\n *** ADD FILES TO LOCAL REPOSITORY *** \n\n");

			flag = peer0.addFilesToRepository(repositoryName, files0);
			System.out.println("addFilesToRepository = " + flag + " by peer0 [true]"); // ---> true

			flag = peer0.addFilesToRepository(repositoryName, files1);
			System.out.println("addFilesToRepository = "+ flag +" by peer0 [true]"); // ---> true

			flag = peer0.addFilesToRepository("Hello", files1);
			System.out.println("addFilesToRepository = "+ flag +" by peer0 [false]"); // ---> false, repository not exists

			pw = new PrintWriter(files0.get(0));
			pw.println("CIAO");
			pw.close();

			repository = peer0.getLocalRepository(repositoryName);
			System.out.println("# files to repository " + repositoryName + ":" + repository.getFilesRepository().size() + " [5]");


			//COMMIT FILES TO REPOSITORY
			
			System.out.println("\n\n *** COMMIT FILES *** \n\n");

			flag = peer0.commit(repositoryName, "First commit, by peer0");
			System.out.println("commit = "+ flag +" by peer0 [true]"); // ---> true
			flag = peer0.commit("Hello", "First commit, by peer0");
			System.out.println("commit = "+ flag +" by peer0 [false]"); // ---> false, repository not exists
			
			repository = peer0.getLocalRepository(repositoryName);
			
			//files2 - Paperino

			for(int i = 0; i < 2; i++) {
				file = new File(directoryName + "\\Paperino"+i+".txt");
				file.createNewFile();
				pw = new PrintWriter(file);
				pw.println("Ciao Paperino"+i);
				files2.add(file);
				pw.close();
			}
			
			flag = peer0.addFilesToRepository(repositoryName, files2);
			System.out.println("addFilesToRepository = " + flag + " by peer0 [true]"); // ---> true
			flag = peer0.commit(repositoryName, "Second commit, by peer0");
			
			repository = peer0.getLocalRepository(repositoryName);
			System.out.println("# files to repository " + repositoryName + ":" + repository.getFilesRepository().size() + " [7]");
			System.out.println(repository.getCommits());
			
			//PUSH TO REPOSITORY
			
			System.out.println("\n\n *** PUSH REPOSITORY *** \n\n");
			System.out.println(peer0.push(repositoryName));
			
			//PUSH TO REPOSITORY
			
			System.out.println("\n\n *** PUSH REPOSITORY *** \n\n");
			System.out.println(peer0.push(repositoryName));
			
			//PULL FROM REPOSITORY
			
			System.out.println("\n\n *** PULL FROM REPOSITORY *** \n\n");
			
			System.out.println("Peer0 " + peer0.pull(repositoryName) + "\n\n\t" + peer0.getLocalRepository(repositoryName) + "\n\n");
			System.out.println("Peer1 " + peer1.pull(repositoryName) + "\n\n\t" + peer1.getLocalRepository(repositoryName)  + "\n\n");
			
			repository = peer0.getLocalRepository(repositoryName);

			Set<GitFile> filesss = repository.getFilesRepository();
			for(GitFile gf: filesss)
				System.out.println(gf.getName());
			
			//PULL WITH CONFLICTS
			
			System.out.println(" \n\n ********************** PULL WITH CONFLICTS ********************** \n\n");

			file = new File(directoryName + "\\Peer0(0).txt");
			file.createNewFile();
			files0.add(file);
			peer0.addFilesToRepository(repositoryName, files0);
			peer0.commit(repositoryName, "Third commit, by peer0");
			System.out.println(peer0.push(repositoryName) + "\n");
			
			System.out.println("\n List files on peer0:");

			repository = peer0.getRemoteRepository(repositoryName);
			filesss = repository.getFilesRepository();
			for(GitFile gf: filesss)
				System.out.println(gf.getName());
			
			file = new File(directoryName + "\\Peer1(0).txt");
			file.createNewFile();
			files1.add(file);
			peer1.addFilesToRepository(repositoryName,files1);
			peer1.commit(repositoryName, "First commit, by peer1");
			//"Found existing conflicts with local files, please check duplicated files!"
			System.out.println("\n Pull by peer1: " + peer1.pull(repositoryName));
			
			System.out.println("\n List files with duplicates on peer1:");
			repository = peer1.getLocalRepository(repositoryName);
			filesss = repository.getFilesRepository();
			for(GitFile gf: filesss)
				System.out.println(gf.getName());
			
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
