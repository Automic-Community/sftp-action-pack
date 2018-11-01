package com.uc4.ara.feature.dirfilehandling.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.uc4.ara.feature.dirfilehandling.PutFiles;

public class PutFilesTest {
	PutFiles builder = new PutFiles();

	@Before
	public void setUp() throws Exception {
		builder.initialize();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRun() throws Exception {
		String[] args = new String[] {
				"-prc", "SFTP",
				"-h", "10.131.237.187",
				"-p", "22",
				"-u", "root",
				"-pwd", "interOP@15429",
				"-src", "C:\\temp\\preserve",
				"-tgt", "/root/Documents/preserve",
				"-prv", "YES",
				"-r", "YES",
				"-o", "YES",
				"-to", "500",
				"-ll", "INFO"
			};
			int retcode = builder.run(args);
			assertEquals(0, retcode);
	}

}
