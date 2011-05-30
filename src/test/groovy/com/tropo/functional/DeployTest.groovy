package com.tropo.functional

import static org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import java.io.InputStream;
import java.util.Scanner;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import com.voxeo.prism.tf.TestFramework

class DeployTest {
	
	def prismLocation
	def prismPort
	def warLocation
	def serverName
	def serverPort
	def appName
	
	@Before
	public void loadProperties() {

		prismLocation = System.getProperty('prism.location')
		prismPort = System.getProperty('prism.port')
		warLocation = System.getProperty('app.location')
		serverName = System.getProperty('server.name')
		serverPort = System.getProperty('server.port')
		appName = System.getProperty('app.name')
	
		def is = getClass().getClassLoader().getResourceAsStream("test.properties")
		def props = new Properties()
		props.load(is)
		if (!prismLocation) {
			prismLocation = props.get('prism.location')
		}
		if (!prismPort) {
			prismPort = props.get('prism.port')
		}
		if (!warLocation) {
			warLocation = props.get('app.location')
		}
		if (!serverName) {
			serverName = props.get('server.name')
		}
		if (!serverPort) {
			serverPort = props.get('server.port')
		}
		if (!appName) {
			appName = props.get('app.name')
		}
	}

	@Test
	public void testDeploy() {
		
		def tf
		def server
				
		try {
			tf = TestFramework.create(Integer.valueOf(prismPort))
			server = tf.createPrismServer(prismLocation)

			server.start 'Tropo Functional Test Server'
			
			assertEquals new URL("http://${serverName}:${serverPort}/" + appName).openConnection().responseCode, 200
			
			//TODO: run tests
			/*
			InputStream is = DeployTest.class.getClassLoader().getResourceAsStream("build.sh")
			if (is != null) {
				Scanner scanner = new Scanner(is)
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					System.out.println(String.format("Running command %s", line))
					CommandLine command = CommandLine.parse(line)
					DefaultExecutor executor = new DefaultExecutor()
					int exitValue = executor.execute(command)
					if (!exitValue == 0) {
						fail(String.format("Command %s failed with exit value %s", line, exitValue))
					}
				}
			} else {
				fail("Could not find script to run in the classpath: build.sh")
			}
			*/
		} catch (Exception e) {
			e.printStackTrace()
			throw e;
		} finally {
			if (server.isRunning()) {
				server.stop()
			}		
			TestFramework.release(tf.report(server))
		}
	}
}
