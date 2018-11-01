
package com.uc4.ara.feature.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import com.uc4.ara.feature.FeatureUtil;

/**
 * The Class ScpWrapper is a wrapper for jcraft and offers methods to access a
 * remote machine via ssh or scp.
 * 
 * @see http://www.jcraft.com/jsch/.
 */
public class SftpWrapper {

	/**
	 * The remote host.
	 */
	private String remoteHost = null;

	/**
	 * The remote port.
	 */
	private int remotePort = 22;

	/**
	 * The remote username.
	 */
	private String remoteUsername = "root";

	/**
	 * The remote passwd.
	 */
	private String remotePasswd = "";


	private String certificate = "";
	/**
	 * The session to the remote machine. Each session supports several
	 * channels.
	 */
	private Session session;

	private long timeout;
	/**
	 * Instantiates a new scp wrapper.
	 */
	private ChannelSftp channelSftp ;

	public static final int ALL = 0;
	public static final int DIRECTORY = 1;
	public static final int FILE = 2;

	public SftpWrapper() {
	}

	/**
	 * Instantiates a new scp wrapper with convenient parameter settings.
	 * 
	 * @param remoteHost
	 *            the remote host
	 * @param remotePort
	 *            the remote port
	 * @param remoteUsername
	 *            the remote username
	 * @param remotePasswd
	 *            the remote passwd
	 */
	public SftpWrapper(String remoteHost, int remotePort, String remoteUsername,
			String remotePasswd) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.remoteUsername = remoteUsername;
		this.remotePasswd = remotePasswd;
	}

	public SftpWrapper(String remoteHost, int remotePort, String remoteUsername,
			String remotePasswd, String certificate) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.remoteUsername = remoteUsername;
		this.remotePasswd = remotePasswd;
		this.certificate = certificate;
	}

	/**
	 * Gets the remote username.
	 * 
	 * @return the remote username
	 */
	public String getRemoteUsername() {
		return remoteUsername;
	}

	/**
	 * Sets the remote username.
	 * 
	 * @param username
	 *            the new remote username
	 */
	public void setRemoteUsername(String username) {
		this.remoteUsername = username;
	}

	/**
	 * Gets the remote port.
	 * 
	 * @return the remote port
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * Sets the remote port.
	 * 
	 * @param port
	 *            the new remote port
	 */
	public void setRemotePort(int port) {
		this.remotePort = port;
	}



	/**
	 * Gets the remote passwd.
	 * 
	 * @return the remote passwd
	 */
	public String getRemotePasswd() {
		return remotePasswd;
	}

	/**
	 * Sets the remote passwd.
	 * 
	 * @param passwd
	 *            the new remote passwd
	 */
	public void setRemotePasswd(String passwd) {
		this.remotePasswd = passwd;
	}

	/**
	 * Gets the remote host.
	 * 
	 * @return the remote host
	 */
	public String getRemoteHost() {
		return remoteHost;
	}

	/**
	 * Sets the remote host.
	 * 
	 * @param host
	 *            the new remote host
	 */
	public void setRemoteHost(String host) {
		this.remoteHost = host;
	}

	public void setTimeOut(long timeout){
		this.timeout = timeout;
	}
	/**
	 * Opens a session to the remote machine. Always call this method before
	 * executing any command on the remote machine. Do not forget to close the
	 * session after finishing the job.
	 * 
	 * @return the session
	 * @throws ScpException
	 *             the scp exception
	 */
	public Session openSession() throws ScpException {
		if (session != null)
			return session;

		try {
			JSch jsch = new JSch();

			if(certificate.length() > 0)
				jsch.addIdentity(certificate);

			session = jsch.getSession(remoteUsername, remoteHost, remotePort);
			MyUserInfo ui = new MyUserInfo();

			if(remotePasswd.length() > 0)
				ui.setPassword(remotePasswd);

			session.setUserInfo(ui);
			session.setTimeout((int)timeout);
			session.connect();

			return session;
		} catch (JSchException e) {
			session = null;
			throw new ScpException(e.getLocalizedMessage());
		}
	}

	/**
	 * Closes the session to the remote machine. Do not forget to call this
	 * method in case of errors or when finishing the job.
	 */
	public void closeSession() {
		session.disconnect();
		session = null;
	}

	/**
	 * Open the channel for SFTP connection. Should only be used in case SFTP protocol is used.
	 * 
	 */
	public void openSftpChannel() throws ScpException {

		try {
			channelSftp = (ChannelSftp) session.openChannel("sftp");
			channelSftp.connect();

		} catch (JSchException e) {
			throw new ScpException(e.getLocalizedMessage());
		}

	}

	public void closeSftpChannel(){
		channelSftp.disconnect();
	}

	public ChannelSftp getChannelSftp() {
		return channelSftp;
	}

	public void setChannelSftp(ChannelSftp channelSftp) {
		this.channelSftp = channelSftp;
	}


	/**
	 * @param remoteFile
	 * @param localFile
	 * @throws IOException
	 * @throws SftpException
	 */
	public void readFile(String remoteFile, String localFile, boolean preserve) throws SftpException {
		File lFile = new File(localFile);
		lFile.getParentFile().mkdirs();
		channelSftp.get(remoteFile,localFile);

		if(preserve){
			SftpATTRS fileAttr = channelSftp.lstat(remoteFile);
			lFile.setLastModified((long)(fileAttr.getMTime())*1000);
			String filePermission = fileAttr.getPermissionsString();

			lFile.setReadable(filePermission.substring(1, 2).equals("r"));
			lFile.setWritable(filePermission.substring(2, 3).equals("w"));
			lFile.setExecutable(filePermission.substring(3, 4).equals("x"));
		}
	}

	public void writeFile(String localFile, String remoteFile)
			throws SftpException {

		channelSftp.put(localFile, remoteFile);
	}
	
	public void writeFile(String localFile, String remoteFile, boolean preserve)
			throws SftpException, IOException {
		channelSftp.put(localFile, remoteFile);
		Path lFile = Paths.get(localFile);
		BasicFileAttributes attributes = Files.readAttributes(lFile, BasicFileAttributes.class);
		FileTime lastModifiedTime = attributes.lastModifiedTime();
		FileTime lastAccessTime = attributes.lastAccessTime();
		int permissions = getOctalPermissions(lFile);
		
		if (preserve) {
			SftpATTRS fileAttr = channelSftp.lstat(remoteFile);
			fileAttr.setPERMISSIONS(permissions);
			fileAttr.setACMODTIME((int)lastAccessTime.to(TimeUnit.SECONDS), (int)lastModifiedTime.to(TimeUnit.SECONDS));
			channelSftp.setStat(remoteFile, fileAttr);
		}
	}

	private static int getOctalPermissions(Path path) throws IOException {
		String osName = System.getProperty("os.name").toLowerCase();
		int owner = 0;
		int group = 0;
		int others = 0;
		
		if (osName.startsWith("windows")) {
			boolean executable = Files.isExecutable(path);
			boolean writable = Files.isWritable(path);
			boolean readable = Files.isReadable(path);
			if (executable) {
				owner += 1;
			}
			if (writable) {
				owner += 2;
			}
			if (readable) {
				owner += 4;
			}
		} else {
			Set<PosixFilePermission> set = Files.getPosixFilePermissions(path);
			for (PosixFilePermission permission : set) {
				switch (permission) {
				case OWNER_EXECUTE:
					owner += 1;
					break;
				case OWNER_WRITE:
					owner += 2;
					break;
				case OWNER_READ:
					owner += 4;
					break;
				case GROUP_EXECUTE:
					group += 1;
					break;
				case GROUP_WRITE:
					group += 2;
					break;
				case GROUP_READ:
					group += 4;
					break;
				case OTHERS_EXECUTE:
					others += 1;
					break;
				case OTHERS_WRITE:
					others += 2;
					break;
				case OTHERS_READ:
					others += 4;
					break;
				default:
					break;
				}
			}
		}
		int permissions = (owner * 8 + group) * 8 + others;
        return permissions;
	}

	public void removeFile(String remoteFile) throws SftpException {

		channelSftp.rm(remoteFile);
	}


	public void removeDir(String remoteDir) throws SftpException {
		channelSftp.rmdir(remoteDir);
	}

	public List<String> listFile(String remoteDir) throws SftpException {
		List entries = new Vector<LsEntry>();
		List<String> files = new ArrayList<>();

		entries =channelSftp.ls(remoteDir);
		for(Object entry : entries){
			String name = ( (LsEntry)entry ).getFilename();
			if(!name.equals(".") && !name.equals(".."))
				files.add(name);
		}
		return files;
	}

	public boolean isDirectory(String remoteDir) {
		boolean isDir = false;
		try {
			isDir = channelSftp.lstat(remoteDir).isDir();
		} catch (SftpException e) {
			return false;
		}
		return isDir;
	}

	public boolean isFile(String remoteFile) {
		boolean isFile = false;
		try {
			isFile = ! (channelSftp.lstat(remoteFile).isDir());

		} catch (SftpException e) {
			return false;
		}
		return isFile;
	}

	public boolean isExisted(String remoteFile) {

		try {
			channelSftp.lstat(remoteFile);

		} catch (SftpException e) {
			return false;
		}
		return true;
	}

	public boolean createDirectory(String remoteDir) {
		try {
			channelSftp.mkdir(remoteDir);
		} catch (SftpException e) {
			return false;
		}
		return true;
	}

	public boolean createDirectoryRecursive(String dirPath)
			throws IOException, IllegalStateException {
		dirPath = dirPath.replaceAll("\\\\", "/");
		dirPath = FileUtil.normalize(dirPath);
		String[] dirs = dirPath.split("/+");
		String currentDir = "";
		for (String dir : dirs){
			if(!dir.equals("")){
				currentDir = currentDir + "/" + dir;

				if(isFile(currentDir)){
					FeatureUtil.logMsg("Remote directory '" + session.getUserName() + "@" + session.getHost() + ":" + dirPath + "' can not be created!");
					return false;

				}else if(!isDirectory(currentDir))
					createDirectory(currentDir );
			}
		}
		return true;
	}

	/**
	 * The Class MyUserInfo.
	 */
	private final static class MyUserInfo implements UserInfo,
	UIKeyboardInteractive {

		/**
		 * The passwd.
		 */
		String passwd = "";

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.jcraft.jsch.UserInfo#getPassword()
		 */
		@Override
		public String getPassword() {
			return passwd;
		}

		/**
		 * Sets the password.
		 * 
		 * @param passwd
		 *            the new password
		 */
		public void setPassword(String passwd) {
			this.passwd = passwd;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.jcraft.jsch.UserInfo#promptYesNo(java.lang.String)
		 */
		@Override
		public boolean promptYesNo(String str) {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.jcraft.jsch.UserInfo#getPassphrase()
		 */
		@Override
		public String getPassphrase() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.jcraft.jsch.UserInfo#promptPassphrase(java.lang.String)
		 */
		@Override
		public boolean promptPassphrase(String message) {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.jcraft.jsch.UserInfo#promptPassword(java.lang.String)
		 */
		@Override
		public boolean promptPassword(String message) {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.jcraft.jsch.UserInfo#showMessage(java.lang.String)
		 */
		@Override
		public void showMessage(String message) {
			// JOptionPane.showMessageDialog(null, message);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.jcraft.jsch.UIKeyboardInteractive#promptKeyboardInteractive(java
		 * .lang.String, java.lang.String, java.lang.String, java.lang.String[],
		 * boolean[])
		 */
		@Override
		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {
			FeatureUtil.logMsg("promptKeyboardInteractive destination: "
					+ destination + ", name: " + name + ", instruction: "
					+ instruction);
			for (String pr : prompt)
				FeatureUtil.logMsg("  prompt: " + pr);
			for (boolean ec : echo)
				FeatureUtil.logMsg("  echo: " + ec);
			// return null == cancel job

			if (prompt.length > 0 && prompt[0].startsWith("Password")) {
				return new String[] { passwd };
			}
			return null;
		}
	}
}
