package it.unisa.adc.gitprotocol.main;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import it.unisa.adc.gitprotocol.beans.Commit;
import it.unisa.adc.gitprotocol.beans.GitFile;
import it.unisa.adc.gitprotocol.implementations.GitProtocolImpl;

public class TerminalGitProtocol {
	
	/**
	 * docker build --no-cache -t test  .
	 * docker run -i -e MASTERIP="127.0.0.1" -e ID=0 test
	 * use -i for interactive mode
	 * use -e to set the environment variables
	 * @author Mariangela Petraglia
	 *
	 */

	@Option(name = "-m", aliases = "--masterip", usage = "the master peer ip address", required = true)
	private static String master;

	@Option(name = "-id", aliases = "--identifierpeer", usage = "the unique identifier for this peer", required = true)
	private static int id;

	public static void main(String[] args) throws Exception {

		TerminalGitProtocol terminalGitProtocol = new TerminalGitProtocol();
		final CmdLineParser parser = new CmdLineParser(terminalGitProtocol);  
		String localDirectoryName = "";
		String path = ".\\";
		File directoryName = null;
		String repositoryName = "";
		
		try {  
			parser.parseArgument(args);  
			TextIO textIO = TextIoFactory.getTextIO();
			TextTerminal<?> terminal = textIO.getTextTerminal();
			GitProtocolImpl peer = new GitProtocolImpl(id, master);

			terminal.printf("\n Starting peer id: %d on master node: %s \n", id, master);
			while(true) {
				printMenu(terminal);

				int option = textIO.newIntInputReader().withMaxVal(8).withMinVal(1).read(" \n Insert operation number: ");

				switch (option) {

				// Create repository
				case 1:
					terminal.printf("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository: ");
					terminal.printf("\n Insert name directory \n");
					localDirectoryName = textIO.newStringInputReader().withDefaultValue("default-name-directory").read("Name directory: ");
					directoryName = new File(path+localDirectoryName);
					if(!directoryName.exists())
						terminal.println("Directory created "+ directoryName.mkdir());
					else
						terminal.println("Error: Directory "+ directoryName.getAbsolutePath() + " already exists!");
					if(peer.createRepository(repositoryName, directoryName))
						terminal.printf("\n Repository %s successfully created \n", repositoryName);
					else
						terminal.printf("\n Error in repository creation of %s repository \n", repositoryName);
					break;

				// Add files to repository	
				case 2:
					terminal.printf("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository: ");
					Stream<Path> walk = Files.walk(Paths.get(directoryName.getPath()));
					List<File> result = null;
					result = walk.filter(Files::isRegularFile).map(x -> x.toFile()).collect(Collectors.toList());
					if(result.isEmpty())
						terminal.println("No files in current directory!");
					result.forEach(file -> {
						terminal.println("- " + file.getName());		
					});
					if(peer.addFilesToRepository(repositoryName, result))
						terminal.printf("\n Successfully adding file to %s repository \n", repositoryName);
					else
						terminal.printf("\n Error adding files to %s repository \n", repositoryName);
					break;

				// Commit
				case 3:
					terminal.printf("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository: ");
					terminal.printf("\n Inser message for commit \n");
					String message = textIO.newStringInputReader().withDefaultValue("default-message-commit").read("Message: ");
					if(peer.commit(repositoryName, message))
						terminal.println("\n Successfully commit to "+ repositoryName + " repository \n");
					else
						terminal.printf("\n Error commit to " + repositoryName + "repository \n");
					break;

				// Push
				case 4:
					terminal.println("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository:");
					String messageOperation = peer.push(repositoryName);
					terminal.println(messageOperation);
					break;

				// Pull
				case 5:
					terminal.println("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository:");
					messageOperation = peer.pull(repositoryName);
					terminal.println(messageOperation);
					break;

				// List files repository
				case 6:
					terminal.println("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository:");
					Set<GitFile> filesRep = peer.getListFilesRepository(repositoryName); 
					for(GitFile gf: filesRep) 
						terminal.println(gf.toString());
					break;

				// List commits repository
				case 7:
					terminal.println("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository:");
					List<Commit> commits = peer.getListOfCommit(repositoryName); 
					for(Commit commit: commits) 
						terminal.println(commit.toString());
					break;	
					
				// Destroy repository
				case 8:
					terminal.println("\n Insert repository name \n");
					repositoryName = textIO.newStringInputReader().withDefaultValue("default-name-repository").read("Name repository:");
					terminal.println("\n Are you sure you want to destroy repository %s?\n");
					boolean destroy = textIO.newBooleanInputReader().withDefaultValue(false).read("destroy?");
					if(destroy) {
						peer.destroyRepository(repositoryName);
						System.exit(0);
					}
					break;
					// Destroy repository
				case 9:
					terminal.println("\n Are you sure you want to destroy repository %s?\n");
					boolean exit = textIO.newBooleanInputReader().withDefaultValue(false).read("exit?");
					if(exit) 
						System.exit(0);
					break;

				default:
					break;
					
				}//end switch
			}//end while

		}  
		catch (CmdLineException clEx) {  
			System.err.println("ERROR: Unable to parse command-line options: " + clEx);  
		}  

	}
	
	public static void printMenu(TextTerminal<?> terminal) {
		terminal.printf("\n 1: Create repository \n");
		terminal.printf("\n 2: Add files to repository \n");
		terminal.printf("\n 3: Commit \n");
		terminal.printf("\n 4: Push \n");
		terminal.printf("\n 5: Pull \n");
		terminal.printf("\n 6: List of repository files \n");
		terminal.printf("\n 7: List of repository commits \n");
		terminal.printf("\n 8: Destroy repository \n");
		terminal.printf("\n 9: Exit \n");

	}
}
