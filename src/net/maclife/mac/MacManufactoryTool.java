package net.maclife.mac;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * 用来根据网卡 MAC 地址查找制造商信息的 Java 工具类。
 *
 * @author liuyan
 *
 */
public class MacManufactoryTool
{
	/**
	 * oui.txt 首行的生成时间的日期时间格式
	 *   Generated: Thu, 11 Jun 2015 05:00:03 -0400
	 */
	public static final SimpleDateFormat dbGeneratedTimeFormat = new SimpleDateFormat ("EEE, dd MMM yyyy hh:mm:ss ZZZ", Locale.ENGLISH);

	public String dbfile = "oui.txt";
	public Timestamp dbGeneratedTime = null;
	public long dbFileTimestamp = 0;
	public Map<String, Map<String, String>> mapCache_All = new HashMap<String, Map<String, String>> ();
	public Map<String, Map<String, Map<String, String>>> mapCache_GroupByRegion = new ConcurrentSkipListMap<String, Map<String, Map<String, String>>> ();
	public Map<String, Map<String, Map<String, String>>> mapCache_GroupByManufactories = new ConcurrentSkipListMap<String, Map<String, Map<String, String>>> ();

	public MacManufactoryTool (String dbfile)
	{
		this.dbfile = dbfile;
	}

	public void OpenDatabase () throws IOException, ParseException
	{
		File fDB = new File (dbfile);
		if (fDB.lastModified () == dbFileTimestamp)	// 文件最后修改时间一致，则认为不需要重新读取
			return;

		// 清理以前的缓存
		dbFileTimestamp = fDB.lastModified ();
		dbGeneratedTime = null;
		mapCache_All.clear ();
		mapCache_GroupByRegion.clear ();
		mapCache_GroupByManufactories.clear ();

		// 开始读取
		BufferedReader br = new BufferedReader (new FileReader(fDB));
		String sLine = null;
		int nLine = 0;
		int nMacAddresses = 0;	// MAC 地址数量

	every_line_loop:
		while (true)
		{
			if (nLine==0)	// 第一行，文件生成时间
			{
				sLine = br.readLine ();	nLine ++;
				if (sLine == null)
					break;

				//String[] arrayTimeInfo = sLine.split (":", 2);
				//String sTime = arrayTimeInfo[1].trim ();
				//dbGeneratedTime =  new Timestamp (dbGeneratedTimeFormat.parse (sTime).getTime ());
				dbGeneratedTime =  new Timestamp (fDB.lastModified ());
System.out.println ("数据库文件生成日期: " + dbGeneratedTime);

				// 略过无用的行
				sLine = br.readLine ();	nLine ++;
				sLine = br.readLine ();	nLine ++;
				sLine = br.readLine ();	nLine ++;
				//sLine = br.readLine ();	nLine ++;
				//sLine = br.readLine ();	nLine ++;
				//sLine = br.readLine ();	nLine ++;
				continue;
			}

			// 逐段读取，每段就是一个 MAC 地址厂商信息
			int nLine2 = 0;	//
			Map<String, String> mapMACManufactory = new HashMap<String, String> ();
			String sPreviousLine = null;
		manufactory_paragraph_loop:
			while (true)
			{
				sLine = br.readLine ();	nLine ++;	nLine2 ++;
				if (sLine == null)
					break every_line_loop;
//System.out.println (nLine + ": " + sLine);
				if (sLine.isEmpty ())
				{
					// 本段 MAC 地址厂商信息读取完毕，按照我国/东亚的习惯将厂商地址按 国家-省份-城市-街道 的顺序重新组织地址
//System.out.println (nMacAddresses + ": " + mac_manufactory);
					String sCountryOrRegionName = null;
					if (nLine2 <=3)
						sCountryOrRegionName = "*****";	// 有一些是私有数据，什么地址信息也没有，国家信息更没有
					else
						sCountryOrRegionName = sPreviousLine.trim ();
					Map<String, Map<String, String>> mapRegionCache = mapCache_GroupByRegion.get (sCountryOrRegionName);
					if (mapRegionCache==null)
					{
						mapRegionCache = new HashMap<String, Map<String, String>> ();
						mapCache_GroupByRegion.put (sCountryOrRegionName, mapRegionCache);
					}
					mapRegionCache.put (mapMACManufactory.get ("mac"), mapMACManufactory);
					mapMACManufactory.put ("region", sCountryOrRegionName);
					break;
				}
				if (nLine2==1 || nLine2==2)
				{
					String[] arrayInfo = sLine.split (" {3,}|\\t+", 3);
//System.out.println ("arrayInfo.length = " + arrayInfo.length + ", arrayInfo[0]="+arrayInfo[0] + ",  arrayInfo[1]="+arrayInfo[1]);
					String mac = arrayInfo[0].trim ();
					String sManufactoryName = arrayInfo[2];
					mapCache_All.put (mac, mapMACManufactory);

					if (nLine2==1)	// 1 和 2 是重复的，所以只加一次就好了（虽然加多次也无所谓）
					{
						nMacAddresses ++;
						mapMACManufactory.put ("mac", mac);
						mapMACManufactory.put ("name", sManufactoryName);
						mapMACManufactory.put ("line-number", String.valueOf (nLine));
						mapMACManufactory.put ("number", String.valueOf (nMacAddresses));

						Map<String, Map<String, String>> mapManufactoryCache = mapCache_GroupByManufactories.get (sManufactoryName);
						if (mapManufactoryCache==null)
						{
							mapManufactoryCache = new HashMap<String, Map<String, String>> ();
							mapCache_GroupByManufactories.put (sManufactoryName, mapManufactoryCache);
						}
						mapManufactoryCache.put (mac, mapMACManufactory);
					}
					else if (nLine2==2)
					{
						mapMACManufactory.put ("mac_base16", mac);
					}
				}
				else
				{
					String sAddress = mapMACManufactory.get ("address");
					if (sAddress==null)
						mapMACManufactory.put ("address", sLine.trim ());
					else
						mapMACManufactory.put ("address", sLine.trim () + " ∙ " + sAddress);
				}
				sPreviousLine = sLine;	// 记住上一行，用于在分段时，取得刚刚读完的一段 MAC 厂商信息里的“国家/地域”信息
			}
		}
		br.close ();

System.out.println ("MAC 地址厂商数据库中的厂商名称数量: " + mapCache_GroupByManufactories.size ());

System.out.println ("MAC 地址厂商数据库中的国家/地区数量: " + mapCache_GroupByRegion.size ());
		for (String sCountryOrRegionName: mapCache_GroupByRegion.keySet ())
		{
			Map<String, Map<String, String>> regionCache = mapCache_GroupByRegion.get (sCountryOrRegionName);
System.out.println (String.format ("%-35s	%5d", sCountryOrRegionName, regionCache.size ()));
		}
//System.out.println (mapCache_GroupByRegion.get (""));
	}

	/**
	 * 根据 MAC 地址查询厂商信息。
	 * @param macs MAC 地址列表。 MAC 地址格式
	 * <ul>
	 * 	<li>AA:BB:CC:DD:EE:FF - 完整 MAC 地址，分隔符为 :</li>
	 * 	<li>AA-BB-CC-DD-EE-FF - 完整 MAC 地址，分隔符为 -</li>
	 * 	<li>AA:BB:CC - 前半个 MAC 地址，分隔符为 :</li>
	 * 	<li>AA-BB-CC - 前半个 MAC 地址，分隔符为 -</li>
	 * 	<li>AABBCCDDEEFF - 不带分隔符的完整 MAC 地址</li>
	 * 	<li>AABBCC - 不带分隔符的前半个 MAC 地址</li>
	 * </ul>
	 * @return 返回的是一个 List 对象，List 的元素个数等于传入的 MAC 地址数量：如果元素值为 null，则表示未找到该 MAC 地址的厂商信息
	 * @throws ParseException
	 * @throws IOException
	 */
	public List<Map<String, String>> Query (String... macs) throws IOException, ParseException
	{
		OpenDatabase ();

		List<Map<String, String>> listResult = new ArrayList<Map<String, String>>();
		for (String mac : macs)
		{
			listResult.add (mapCache_All.get (FormalMACAddress(mac)));
		}
		return listResult;
	}
	public static Pattern PAT_HalfToFull = Pattern.compile ("\\p{XDigit}{2}(:|-)\\p{XDigit}{2}(:|-)\\p{XDigit}{2}((:|-)\\p{XDigit}{1,2})*", Pattern.CASE_INSENSITIVE);
	public static Pattern PAT_HalfToFull_NoDelimiter = Pattern.compile ("\\p{XDigit}{6,}", Pattern.CASE_INSENSITIVE);
	/**
	 * 将用户输入的 MAC 地址规范为 oui.txt　中的 MAC 地址样式： 前半个 MAC 地址，分隔符为 - ，且都转换为大写字母
	 * @param mac_loose
	 * @return
	 */
	public static String FormalMACAddress (String mac_loose)
	{
		Matcher matcherHalfToFull = PAT_HalfToFull.matcher (mac_loose);
		if (matcherHalfToFull.matches ())
			return (mac_loose.substring (0, 2) + "-" + mac_loose.substring (3, 5) + "-" + mac_loose.substring (6, 8)).toUpperCase();

		Matcher matcherHalfToFull_NoDelimiter = PAT_HalfToFull_NoDelimiter.matcher (mac_loose);
		if (matcherHalfToFull_NoDelimiter.matches ())
			return (mac_loose.substring (0, 2) + "-" + mac_loose.substring (2, 4) + "-" + mac_loose.substring (4, 6)).toUpperCase();

		return null;
	}

	public static void main (String[] args) throws Exception
	{
		String sDatabaseFile = "oui.txt";	// 默认用当前路径下的 oui.txt 作为数据库文件
		//boolean bCacheToMemory = false;	// 是否将数据库中的文件缓存到内存中（读到 Map 对象中，查询时直接从 Map 对象中 get/查找）
		List<String> listMacAddresses = new ArrayList<String> ();

		if (args.length==0)
			System.out.println ("Usage: java -cp ../lib/ net.maclife.mac.MacManufactoryTool [-db 数据库文件(oui.txt)] [MAC地址]...");

		int i=0;
		for (i=0; i<args.length; i++)
		{
			String arg = args[i];
			if (arg.startsWith("-") || arg.startsWith("/"))
			{
				arg = arg.substring (1);
				if (arg.equalsIgnoreCase("db"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定 oui.txt 数据库文件");
						return;
					}
					sDatabaseFile = args[i+1];
					i ++;
				}
				//else if (arg.equalsIgnoreCase("cache"))
				//{
				//	bCacheToMemory = true;
				//	i ++;
				//}
			}
			else
				listMacAddresses.add (arg);
		}

		if (listMacAddresses.isEmpty ())
		{
			System.err.println ("需要指定至少一个 MAC 地址");
			return;
		}
		MacManufactoryTool mactool = new MacManufactoryTool (sDatabaseFile);
		mactool.OpenDatabase ();

		List<Map<String, String>> listResults = null;
		listResults = mactool.Query (listMacAddresses.toArray (new String[0]));
		System.out.println (listResults);
		if (listResults.size() == 0)
		{
			System.err.println ("未查到 MAC 地址的制造商信息");
			return;
		}

		Map<String, String> manufactory = null;
		for (i=0; i<listResults.size (); i++)
		{
			manufactory = listResults.get (i);

			System.out.println (
				String.format ("%-17s", listMacAddresses.get(i)) + "  " +

				(manufactory == null ?
				"          未找到该 MAC　地址的厂商信息"
				:
				manufactory.get ("mac") + "  " +
				manufactory.get ("name") +
				"  行号: "  + manufactory.get ("line-number") +
				",  地址: "  + manufactory.get ("address") +
				",  该厂商共有 " + mactool.mapCache_GroupByManufactories.get (manufactory.get ("name")).size () + " 条" +
				",  该地区共有 " + mactool.mapCache_GroupByRegion.get (manufactory.get ("region")).size () + " 条"
				)  +
				(i==0 ?
					"    (oui.txt 版本: " + mactool.dbGeneratedTime + ", 共 " + mactool.mapCache_All.size () + " 条" + ")"
					: "")	// 第一条加上数据库信息
			);
		}
	}
}
