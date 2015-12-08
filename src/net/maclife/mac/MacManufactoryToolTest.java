package net.maclife.mac;

import static org.junit.Assert.*;

import java.io.*;
import java.text.*;
import java.util.*;

import org.junit.*;

public class MacManufactoryToolTest
{
	@Test
	public void test () throws IOException, ParseException
	{
		MacManufactoryTool mmt = new MacManufactoryTool ("db" + File.separator + "oui.txt");
		Map<String, String> manufactory = null;
		List<Map<String, String>> results = null;
		results = mmt.Query ("00-00-00", "00-00-6C", "FF-00-00");

		manufactory = results.get (0);
		assertEquals (manufactory.get ("name"), "XEROX CORPORATION");

		manufactory = results.get (1);
		assertEquals (manufactory.get ("name"), "PRIVATE");

		manufactory = results.get (2);
		assertNull (manufactory);
	}

}
