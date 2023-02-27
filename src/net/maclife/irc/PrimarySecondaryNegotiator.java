package net.maclife.irc;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.text.*;
import java.util.*;

import org.apache.commons.codec.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.util.*;

import net.maclife.ansi.*;

//import net.maclife.ansi.*;
//import net.maclife.irc.dialog.*;

/**
 简单的主从协商，主要用于解决在频道里发 $ht模板、.action模板 快捷命令时，多个 bot 会全部响应的问题。
 <p>
 支持在不同的频道里进行主从协商。即，支持这种情况：
<ul>
<li>Bot A 在 Channel A 是主，但在 Channel B 是从；</li>
<li>Bot B 在 Channel A 是从，但在 Channel B 是主。</li>
</ul>
 </p>

<p>
 消息要加密，避免被其他人、其他 Bot 恶搞，消息经过验证后是真的主从协商消息，才执行主从协商。
 同一个群组内的多个 Bot 实例，需要使用相同的钥匙对。
</p>

<p>
 异常情况处理：Bot A 刚进频道，同组 Bot 只有它自己，它发起主从协商，无其他 Bot 应答，超时后，它成为了首选 Bot。然后它超时断线了。同组的另一个 Bot B 进来了，执行了相同的操作，成为了首先 Bot。然后 Bot A 回来了…
</p>

<p>
 发起主从协商，请求当首选 Bot (Master Slave Negotiation / Primary Secondary Negotiation)：
 {"psn":{"c":"阶段1_发起协商_我要当首选Bot", iid:"主从协商发起号，目前使用毫秒时间戳当 iid，但是，是字符串类型", "m":"反对的请举手", "t":毫秒时间戳, "s":"签名（base64）0011223344556677889900...."}}
 </p>

<p>
 响应，情况1，已经有 Bot 实例是 首选 Bot：
 {"psn"{"c":"阶段2_投票_同意|阶段2_投票_反对", iid:"主从协商发起号", "m":"这里可以选一些经典电影里的台词，如“还有谁？！”“You Shall Not Pass!”", "t":毫秒时间戳, "s":"签名（base64）0011223344556677889900...."}}
</p>

<p>
 无响应，当没有 Bot 响应时，暂定 60 秒内，超时后，自封为首先 Bot。
 </p>

 <hr/>
 ---------<br/>
 sign 验证字符串算法：signature_initialized_by_private_key.sign (), ($msg + $msg + $msgtime).getBytes()
 <br/>
 ---------<br/>
 <hr/>

 <h1>常见问题 Q/A</h1>
 <dl>
 	<dt>用途是什么</dt>
 	<dd>避免 Bot 快捷命令被多个 Bot 实例一起执行。Bot 的正常命令，可以通过命令前缀来避免多个 Bot 实例同时执行，但快捷命令却没法避免。</dd>

  	<dt>Primary 和 Secondary 有没有心跳检测？</dt>
 	<dd>没有心跳检测。首先，一个 Bot 不知道另一个 Bot 的存在。而且，因为这是在 IRC 频道中执行主从协商，首次主从协商成功后，即使知道了同组 Bot 的存在，也不便于在频道里频繁的发心跳信息。
 		其次，即使使用私信发送心跳检测，也因为 IRC 通信的不可靠，导致检测无效，如：当 Bot 断线重连后，昵称可能发生了改变，这时就收不到别的 Bot 的心跳检测。
 	</dd>

  	<dt>没有心跳检测，那 Primary 断线了怎么办，快捷命令就没有 Primary Bot 响应了</dt>
 	<dd>暂时没有完美的解决方法。目前，需要在另一个 Bot 的命令行控制台手工执行主从协商，将其设置为首选 Bot</dd>

  	<dt>怎么与别的 Bot 实例区分开，不混在一起？（或者说：Bot 群组 / Bot 集群 如何划分？）</dt>
 	<dd>一组 Bot 使用同一个公钥秘钥对，使用不同的 KeyPair 进行签名和验证时，验证会失败 -- 不会当做自己所在 Bot 群组的主从协商消息。所以，只需要使用不同的秘钥对，即可划分为不同的 Bot 群组</dd>

  	<dt>同一时间内能进行多个主从协商吗？</dt>
 	<dd>在同一个频道内，同一群组（同一集群）的 Bot 同一时间内只能有一个主从协商，只有一个主从协商结束后，才能进行下一个主从协商。当不同群组的 Bot 在一起时，各群组之间可以同时进行不同的主从协商。</dd>

  	<dt>为什么要支持【在不同的频道里进行主从协商】</dt>
 	<dd>存在这种需求：在不同的频道需要由特定 Bot 实例执行快捷命令。比如：多个 Bot 实例分别运行在不同 CPU 架构的主机上，然后有几个关于各自 CPU 架构的频道，这时“有可能”需要在特定 CPU 架构频道用运行在该 CPU 架构上的 Bot 实例来执行快捷命令。</dd>

  	<dt></dt>
 	<dd></dd>

  	<dt></dt>
 	<dd></dd>
</dl>
 */
public class PrimarySecondaryNegotiator //implements DialogUser
{
	enum NegotiationCode
	{
		阶段1_发起协商_我要当首选Bot,	// stage 1: 发起协商 - 我要当首选 Bot
		阶段2_投票_同意,	// stage 2: 投票 - 同意
		阶段2_投票_反对,	// stage 2: 投票 - 反对
		阶段3_自我宣告已当选,	// stage 3: 自我宣告当选（若有其他 Bot 实例反对，则不执行该步骤）
	};

	LiuYanBot oThisBot = null;

	/**
	主从协商器在通道中的状态。
	<dl>
		<dt>key: 频道名称</dt>
	<dd>每个频道名一个 key，如： <code>#liuyanbot</code></dd>

	<dt>value:</dt>
	<dd>又是一个 Map，
			<dl>
				<dt>key = <code>CurrentNegotiation</code></dt>
				<dd>value: 当前正在进行的主从协商。<code>JsonNode</code> 类型。同一组 Bot、在同一频道内、同一时间内 只能有一个主从协商。当有其他主从协商时</dd>

				<dt>key = <code>CurrentNegotiationInitiator</code></dt>
				<dd>value: 当前主从协商发起方的昵称。<code>String</code> 类型。</dd>

				<dt>key = <code>AmIPrimary</code></dt>
				<dd>value: 我是否是首选 Bot。<code>Boolean</code> 类型。</dd>

				<dt>key = <code>Votes</code></dt>
				<dd>value: 投票记录。<code>Map</code> 类型。
					<dl>
						<dt>key: 投票 Bot 的昵称</dt>
						<dd>因为采用的是一票否决制，所以，不用担心【使用传统投票计数方式】时才会出现的【当 Bot 昵称改名后再次投票，被计为多张票】的问题</dd>
						<dt>value: 投票结果，阶段2_投票_同意 或 阶段2_投票_反对。<code>enum NegotiationCode</code> 类型。</dt>
					</dl>
				</dd>

				<dt>key = <code>SchedulerTimer</code></dt>
				<dd>value: 发起主从协商的调度定时器。仅在发起方使用，发起方发起时，就启动该定时器。如果有人在定时器到时前就返回了 阶段2_投票_反对 ，则此定时器立刻取消调度。<code>Timer</code> 类型。</dd>
			</dl>
	</dd>
	<dl>
	 */
	Map<String, Map<String, Object>> mapChannelsState = new HashMap<String, Map<String, Object>> ();

	//KeyPair keypair = null;
	PrivateKey keyPrivateKey = null;
	PublicKey keyPublicKey = null;

	String sSignatureAlgorithm = System.getProperty ("primary-secondary-negotiation.signature.algorithm", "SHA256withRSA");

	static final String[] arrayInitiateMessages =
		{
			"谁赞成？谁反对？",
			"同意的请举手，反对的请举手",
		};
	static final String[] arrayInitiateMessages_Forced =
		{
			"还有谁？！",
			"反对的请举手",
		};
	static final String[] arrayVoteMessages =
		{
			// 中国古装电视剧风格
			"准奏",
			"准了",

			// OA 审批风格
			"同意",

			// 社畜风格
			"收到",
			"好的",
		};
	static final String[] arrayVoteAgainstMessages =
		{
			//
			"反对",

			// 电影《黑社会 Election》中“阿乐”的台词
			"下一届 我会全力支持你做话事人",

			// 电影《Lord of the Rings:The Fellowship of the Ring 指环王/魔戒：护戒使者》中 Gandalf”的台词
			"You Shall Not Pass!",
		};
	static final String[] arrayAnnouncementMessages =
		{
			//
			"新话(打)事(工)人就是我",

			// 看电影看多了
			//"月黑风高",

			// 周星驰在颁奖典礼的感谢词
			"多谢嗮",

			// 三国杀 袁术 台词
			"玉玺在手，天下我有",

			// 大内密探零零发 慧贤雅叙老板娘 台词
			"老鸨就是我呀",

			// 常见的其他人在颁奖典礼的感谢词样式
			"感谢 ｛A..Z｝",

			// 仿Jack Sparrow 台词
			"Captain {0}. If you please.",

			// 仿 美利坚合众国：Stand by Your Ad provision
			"I''m {0}, and I approve this announcement. [doge]",
		};

	//Timer schedulerTimer = new Timer ();

	public PrimarySecondaryNegotiator (LiuYanBot bot, String sKeyStoreFileName, String sKeyStorePassword, String sKeyName, String sKeyPassword) throws Exception
	{
		//this ("JKS", sKeyStoreFileName, sKeyName);
		this (bot, new File (sKeyStoreFileName), sKeyStorePassword, sKeyName, sKeyPassword);
	}
	public PrimarySecondaryNegotiator (LiuYanBot bot, File fKeyStoreFile, String sKeyStorePassword, String sKeyName, String sKeyPassword) throws Exception
	{
		oThisBot = bot;
System.out.println ("KeyStore.getInstance(File, char[]) " + new java.sql.Timestamp (System.currentTimeMillis ()));
		KeyStore ks = KeyStore.getInstance (fKeyStoreFile, sKeyStorePassword.toCharArray ());
System.out.println ("KeyStore.getInstance(File, char[]) done " + new java.sql.Timestamp (System.currentTimeMillis ()));

		GetPrivatePublicKey (ks, sKeyName, sKeyPassword);
	}
	public PrimarySecondaryNegotiator (LiuYanBot bot, String sKeyStoreType, String sKeyStoreFileName, String sKeyStorePassword, String sKeyName, String sKeyPassword) throws Exception
	{
		oThisBot = bot;
System.out.println ("KeyStore.getInstance(String) " + new java.sql.Timestamp (System.currentTimeMillis ()));
		KeyStore ks = KeyStore.getInstance (sKeyStoreType);	// 在 赛昉科技StarFive VisionFive 1 单板机上，Ubuntu 22.04.01 操作系统下，这一步骤耗时 50 秒左右，但在 openSUSE Tumbleweed 下，却很快
System.out.println ("ks.load(InputStream, char[]) " + new java.sql.Timestamp (System.currentTimeMillis ()));
		ks.load (new FileInputStream(sKeyStoreFileName), sKeyStorePassword.toCharArray ());
System.out.println ("ks.load(InputStream, char[]) done " + new java.sql.Timestamp (System.currentTimeMillis ()));

		GetPrivatePublicKey (ks, sKeyName, sKeyPassword);
	}
	public void GetPrivatePublicKey (KeyStore ks, String sKeyName, String sPassword) throws Exception
	{
System.out.println ("ks.getKey (String, char[]) " + new java.sql.Timestamp (System.currentTimeMillis ()));
		keyPrivateKey = (PrivateKey)ks.getKey (sKeyName, sPassword.toCharArray ());	// 在 赛昉科技StarFive VisionFive 1 单板机上，Ubuntu 22.04.01 操作系统下，这一步骤耗时 30 秒左右，但在 openSUSE Tumbleweed 下，却很快
System.out.println ("ks.getCertificate (String).getPublicKey () " + new java.sql.Timestamp (System.currentTimeMillis ()));
		keyPublicKey = ks.getCertificate (sKeyName).getPublicKey ();
System.out.println ("ks.getCertificate (String).getPublicKey () done " + new java.sql.Timestamp (System.currentTimeMillis ()));
	}

	Map<String, Object> GetCurrentChannelState (String sChannel)
	{
		if (StringUtils.isEmpty (sChannel))
			return null;
		sChannel = StringUtils.lowerCase (sChannel);
		Map<String, Object> mapCurrentChannelState = mapChannelsState.get (sChannel);
		if (mapCurrentChannelState == null)
		{
			mapCurrentChannelState = new HashMap<String, Object> ();
			mapChannelsState.put (sChannel, mapCurrentChannelState);
		}
		return mapCurrentChannelState;
	}

	JsonNode GetCurrentNegotiation (String sChannel)
	{
		sChannel = StringUtils.lowerCase (sChannel);
		return (JsonNode)GetCurrentChannelState (sChannel).get ("CurrentNegotiation");
	}

	String GetCurrentNegotiationInitiator (String sChannel)
	{
		sChannel = StringUtils.lowerCase (sChannel);
		return (String)GetCurrentChannelState (sChannel).get ("CurrentNegotiationInitiator");
	}

	void SetCurrentNegotiationAndInitiator (String sChannel, JsonNode jsonInitiateNegotiatioin_WithoutWrapper, String sInitiator)
	{
		sChannel = StringUtils.lowerCase (sChannel);
		GetCurrentChannelState (sChannel).put ("CurrentNegotiation", jsonInitiateNegotiatioin_WithoutWrapper);
		GetCurrentChannelState (sChannel).put ("CurrentNegotiationInitiator", sInitiator);
	}

	public boolean AmIPrimary (String sChannel)
	{
		if (StringUtils.isEmpty (sChannel))
			return true;
		sChannel = StringUtils.lowerCase (sChannel);
		Boolean bPrimary = (Boolean)GetCurrentChannelState (sChannel).get ("AmIPrimary");
		return bPrimary==null ? false : bPrimary;
	}

	Map<String, NegotiationCode> GetCurrentNegotiationVotes (String sChannel)
	{
		sChannel = StringUtils.lowerCase (sChannel);
		Map<String, NegotiationCode> mapVotes = (Map<String, NegotiationCode>)GetCurrentChannelState (sChannel).get ("Votes");
		if (mapVotes == null)
		{
			mapVotes = new HashMap<String, NegotiationCode> ();
			GetCurrentChannelState (sChannel).put ("Votes", mapVotes);
		}
		return mapVotes;
	}

	String GetRandomString (String[] arrayString)
	{
		String s = arrayString[oThisBot.rand.nextInt (arrayString.length)];
		s = MessageFormat.format (s, oThisBot.getNick ());
		return s;
	}

	public void InitiateNegotiation (String sChannel, boolean bForced) throws JsonProcessingException
	{
		//sChannel = StringUtils.lowerCase (sChannel);	// Local variable sChannel defined in an enclosing scope must be final or effectively final
		if (GetCurrentNegotiation(sChannel) != null)
		{
System.err.println (sChannel + " 频道当前有正在进行的主从协商，不能同时进行多个主从协商，只能一个一个来");
			return;
		}
		if (AmIPrimary(sChannel) && !bForced)
		{
System.err.println ("我现在就是 " + sChannel + " 频道的首选 Bot，不需要再发起主从协商（除非强制发起）");
			return;
		}

		ObjectNode jsonWrapper = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		ObjectNode jsonInitiateNegotiation = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		String sNegotiationCode = NegotiationCode.阶段1_发起协商_我要当首选Bot.toString ();
		String sMessage = bForced ? GetRandomString (arrayInitiateMessages_Forced) : GetRandomString (arrayInitiateMessages);
		sMessage = Colors.CYAN + sMessage + Colors.NORMAL;
		long lTime = System.currentTimeMillis ();
		String sIID = String.valueOf (lTime);
		//jsonInitiateNegotiation.put ("stage", NegotiationStage.INITIATE.toString ());	// stage
		jsonInitiateNegotiation.put ("c", sNegotiationCode);	// code
		jsonInitiateNegotiation.putRawValue ("m", new RawValue ("\"" + sMessage + "\""));	// message
		String sSignature_Base64 = GenerateSignatureBase64String (oThisBot.getNick (), oThisBot.getLogin (), sChannel, sIID, sNegotiationCode, sMessage, lTime);
		if (sSignature_Base64 == null)
			return;
		jsonInitiateNegotiation.put ("s", sSignature_Base64);	// signature
		if (bForced)
		{
			jsonInitiateNegotiation.put ("f", bForced);	// forced 强制成为首选 Bot，其他 bot 收到此消息后，如果是首选 Bot 的，需要退位
			GetCurrentChannelState (sChannel).put ("AmIPrimary", true);
		}
		jsonInitiateNegotiation.put ("iid", sIID);	// initiation id
		jsonInitiateNegotiation.put ("t", lTime);	// time

		jsonWrapper.set ("psn", jsonInitiateNegotiation);

		oThisBot.sendAction (sChannel, LiuYanBot.jacksonObjectMapper_Loose.writeValueAsString (jsonWrapper));
		SetCurrentNegotiationAndInitiator (sChannel, jsonInitiateNegotiation, oThisBot.getNick ());

		try
		{
			GetSchedulerTimer (sChannel).schedule
			(
				new TimerTask()
				{
					@Override
					public void run ()
					{
						Map<String, NegotiationCode> mapVotes = GetCurrentNegotiationVotes (sChannel);	// Local variable sChannel defined in an enclosing scope must be final or effectively final
						int nOK = 0;
						int nReject = 0;
						for (NegotiationCode negotiation_code : mapVotes.values ())
						{
							switch (negotiation_code)
							{
								case 阶段2_投票_同意:
									nOK ++;
									break;
								case 阶段2_投票_反对:
									nReject ++;
									break;
								default:
									break;
							}
						}
System.out.println ("由“我”在 " + sChannel + " 频道发起的主从协商 " + sIID + " 结束：" + nOK + " 票同意，" + nReject + " 票反对。");
						if (nReject == 0)
						{
							// Announce 新首选 Bot 产生
							try
							{
								Announce (oThisBot, sChannel, sIID, GetRandomString (arrayAnnouncementMessages));	// “话事人/首选 Bot”本质上其实是打工人：快捷命令它全干，同组的其他 Bot 实例全歇着。
							}
							catch (Exception e)
							{
								e.printStackTrace ();
							}
						}
						else
						{
							CleanUpCurrentNegotiation (sChannel);	// 一票否决制
						}
					}
				}
				, 60*1000
			);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}

	/**
	主从协商，使用 IRC 的 /me ACTION_MESSAGE 消息进行通信，当收到 IRC 的 /me ACTION_MESSAGE 消息时，尝试将 ACTION_MESSAGE 消息进行 JSON 解码，只有是有效的 JSON、且该 JSON 包含了 psn 信息、且消息格式正确、且签名也正确时，才进行下一步处理。
	 * @param bot
	 * @param sFromNickName
	 * @param sFromAccount
	 * @param sHostname
	 * @param sChannel
	 * @param sAction
	 */
	public void OnActionReceived (LiuYanBot bot, String sFromNickName, String sFromAccount, String sHostname, String sChannel, String sAction)
	{
bot.logger.entering (PrimarySecondaryNegotiator.class.getName (), "OnActionReceived");
		if (StringUtils.isEmpty (sAction) || !StringUtils.equalsIgnoreCase (StringUtils.left (sAction, 1), "{"))
			return;

		sChannel = StringUtils.lowerCase (sChannel);
		try
		{
			JsonNode json = LiuYanBot.jacksonObjectMapper_Loose.readTree (sAction);

			// 先检验消息格式是否是 PrimarySecondaryNegotiation 消息格式
			JsonNode jsonNegotiation = json.get ("psn");
			if (jsonNegotiation==null || jsonNegotiation.isNull ())
			{
System.err.println (sChannel + " 频道，JSON 消息中没有包含 psn，这不是主从协商消息");
				return;
			}
			//JsonNode jsonStage = jsonNegotiation.get ("stage");
			//if (jsonStage==null || jsonStage.isNull ())
			//	return;
			JsonNode jsonInitiationID = jsonNegotiation.get ("iid");	// 此 主从协商发起ID ，仅仅在发起方才生成此 ID，后续响应方需要原样返回，而不再生成新 iid
			JsonNode jsonNegotiationCode = jsonNegotiation.get ("c");	// Negotiation Code
			JsonNode jsonMessage = jsonNegotiation.get ("m");	// Message 附加的文字消息，其本身仅仅用来生成签名、搞笑的用途，并不影响消息判断
			JsonNode jsonTime = jsonNegotiation.get ("t");	// Time of local time of the sender bot
			JsonNode jsonSignature = jsonNegotiation.get ("s");	// Signature 签名，十六进制字符串。为了避免达到受 IRC 单条消息长度限制，字符串长度可能会从原始签名字符串中裁剪出一部分，比如说：从结尾部分截取 40 个字符长度（20 个原始签名字节长度）
			JsonNode jsonForced = jsonNegotiation.get ("f");	// Forced 是否强制成为首选 Bot，

			if (false)	// 消息格式不正确
				return;

			// 再验证数据的基本有效性
			String sIID = jsonInitiationID.asText ();
			String sNegotiationCode = jsonNegotiationCode.asText ();
			NegotiationCode negotiation_code = null;

			try
			{
				negotiation_code = NegotiationCode.valueOf (sNegotiationCode);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace ();
System.err.println (sChannel + " 频道，NegotiationCode 参数无效");
				return;
			}
			String sMessage = jsonMessage.asText ();
			long lTime = jsonTime.asLong ();	// 时间不可超过 1 分钟，超过 1 分钟则认为是过期消息，不再处理
			if ((System.currentTimeMillis () - lTime) > 60*1000)
			{
System.err.println (sChannel + " 频道，主从协商时间已过有效期，不再处理");
				return;
			}

			// 再验证签名是否有效（是否同一组 Bot 发出的 PSN 消息、是否别人伪造的 PSN 消息…）
			String sSignature_Base64 = jsonSignature.asText ();
			if (! VerifyData_Signature_Base64 (sSignature_Base64, sFromNickName, sFromAccount, sChannel, sIID, sNegotiationCode, sMessage, lTime))
			{
LiuYanBot.logger.warning (sChannel + " 频道，签名不一致，不处理。（可能是不同的 Bot 群组、或 可能是伪造）");
				return;
			}

			// 开始处理

			boolean bForced = false;
			if (jsonForced!=null && ! jsonForced.isNull ())
				bForced = jsonForced.asBoolean ();

			switch (negotiation_code)
			{
				case 阶段1_发起协商_我要当首选Bot:
System.err.println (sChannel + " 频道，收到其他 Bot 想要成为首选 Bot 的请求");
					if (bForced)
					{
System.err.println ("强制性的");
						//
						//Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.阶段2_投票_同意, GetRandomeString (arrayVoteMessages));
						OnPrimaryWasElected (bot, sChannel, sFromNickName);
					}
					else
					{
System.err.println ("非强制性的");
						// 如果有当前有正在进行的主从协商，则否决（同一组 Bot ，同一时间内，不能进行多个主从协商，即：一个一个来）
						// implement it...
						if (GetCurrentNegotiation(sChannel) != null)
						{
							Reply (bot, sFromNickName, sFromAccount, sHostname, sChannel, sIID, NegotiationCode.阶段2_投票_反对, "在本频道的本 Bot 群组中，当前有另外一个主从协商正在进行，一个一个来。不同的 Bot 群组，用不同的 KeyPair 区分。");
							break;
						}

						// 如果自己是 Primary，则否决；否则，赞成
						if (AmIPrimary(sChannel))
						{
							Reply (bot, sFromNickName, sFromAccount, sHostname, sChannel, sIID, NegotiationCode.阶段2_投票_反对, GetRandomString (arrayVoteAgainstMessages));
							break;
						}

						SetCurrentNegotiationAndInitiator (sChannel, jsonNegotiation, sFromNickName);
						Reply (bot, sFromNickName, sFromAccount, sHostname, sChannel, sIID, NegotiationCode.阶段2_投票_同意, GetRandomString (arrayVoteMessages));

						//// 测试
						//int n = bot.rand.nextInt ();
//System.err.println ("随机数 n = " + n);
						//if ((n & 0x01) == 0)
						//	Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.阶段2_投票_同意, GetRandomeString (arrayVoteMessages));
						//else
						//	Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.阶段2_投票_反对, GetRandomeString (arrayVoteAgainstMessages));
					}
					break;
				case 阶段2_投票_同意:
				case 阶段2_投票_反对:
					// 通常由发起方处理回复。其他接收方，也可以存储结果，但目前的实现方式是不处理
					if (GetCurrentNegotiation(sChannel)!=null && !StringUtils.equalsIgnoreCase (sIID, GetCurrentNegotiation(sChannel).get ("iid").asText ()) )
					{
System.err.println (sChannel + " 频道，回复的不是当前正在进行的主从协商，不处理。正常情况下，这个条件不会满足，因为正常情况下，同一频道、同一组 Bot、同一时间只能有一个主从协商");
						break;
					}
					if (! StringUtils.equalsIgnoreCase (bot.getNick (), GetCurrentNegotiationInitiator(sChannel)))
					{
System.err.println (sChannel + " 频道，回复人不是发起人，一般情况下不处理。但当回复 阶段2_投票_反对 时，需要立刻取消本地缓存的主从协商，否则会阻塞下一次的主从协商。");
						if (negotiation_code == NegotiationCode.阶段2_投票_反对)
							this.CleanUpCurrentNegotiation (sChannel);
						break;
					}
					if (negotiation_code == NegotiationCode.阶段2_投票_反对)
					{
System.out.println ("因有其他 Bot 投反对票（一票否决制），由“我”在 " + sChannel + " 频道发起的主从协商 " + sIID + " 立刻结束，不做其他操作。针对本次主从协商，其他 Bot 若还有回复将不会回应。");
						this.CleanUpCurrentNegotiation (sChannel);	// 一票否决制
						break;
					}
					GetCurrentNegotiationVotes (sChannel).put (sFromNickName, negotiation_code);
					break;
				case 阶段3_自我宣告已当选:
					OnPrimaryWasElected (bot, sChannel, sFromNickName);
					break;
			}

		}
		catch (JsonProcessingException e)
		{
			e.printStackTrace ();
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace ();;
		}
bot.logger.exiting (PrimarySecondaryNegotiator.class.getName (), "OnActionReceived");
	}

	void Reply (LiuYanBot bot, String sFromNickName, String sFromAccount, String sHostname, String sTargetChannel, String sIID, NegotiationCode negotiation_code, String sMessage) throws JsonProcessingException
	{
bot.logger.entering (PrimarySecondaryNegotiator.class.getName (), "Reply");
		ObjectNode jsonWrapper = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		ObjectNode jsonInitiateNegotiation = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		String sNegotiationCode = negotiation_code.toString ();
		if (negotiation_code == NegotiationCode.阶段2_投票_同意)
			sMessage = Colors.GREEN + sMessage + Colors.NORMAL;
		else if (negotiation_code == NegotiationCode.阶段2_投票_反对)
			sMessage = Colors.RED + sMessage + Colors.NORMAL;
		long lTime = System.currentTimeMillis ();
		//jsonInitiateNegotiation.put ("stage", NegotiationStage.INITIATE.toString ());	// stage
		jsonInitiateNegotiation.put ("c", sNegotiationCode);	// code
		jsonInitiateNegotiation.putRawValue ("m", new RawValue ("\"" + sMessage + "\""));	// message
		String sSignature_Base64 = GenerateSignatureBase64String (bot.getNick (), bot.getLogin (), sTargetChannel, sIID, sNegotiationCode, sMessage, lTime);
		if (sSignature_Base64 == null)
			return;
		jsonInitiateNegotiation.put ("s", sSignature_Base64);	// signature
		jsonInitiateNegotiation.put ("iid", sIID);	// initiation id
		jsonInitiateNegotiation.put ("t", lTime);	// time

		jsonWrapper.set ("psn", jsonInitiateNegotiation);

		bot.sendAction (sTargetChannel, LiuYanBot.jacksonObjectMapper_Loose.writeValueAsString (jsonWrapper));
bot.logger.exiting (PrimarySecondaryNegotiator.class.getName (), "Reply");
	}

	void Announce (LiuYanBot bot, String sTargetChannel, String sIID, String sMessage) throws JsonProcessingException
	{
bot.logger.entering (PrimarySecondaryNegotiator.class.getName (), "Announce");
		ObjectNode jsonWrapper = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		ObjectNode jsonNegotiationAnnouncement = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		long lTime = System.currentTimeMillis ();
		sMessage = ANSIEscapeTool.COLOR_DARK_CYAN + sMessage + Colors.NORMAL;
		//jsonNegotiationAnnouncement.put ("stage", NegotiationStage.INITIATE.toString ());	// stage
		jsonNegotiationAnnouncement.put ("c", NegotiationCode.阶段3_自我宣告已当选.toString ());	// code
		jsonNegotiationAnnouncement.putRawValue ("m", new RawValue ("\"" + sMessage + "\""));	// message
		String sSignature_Base64 = GenerateSignatureBase64String (bot.getNick (), bot.getLogin (), sTargetChannel, sIID, NegotiationCode.阶段3_自我宣告已当选.toString (), sMessage, lTime);
		if (sSignature_Base64 == null)
			return;
		jsonNegotiationAnnouncement.put ("s", sSignature_Base64);	// signature
		jsonNegotiationAnnouncement.put ("iid", sIID);	// time
		jsonNegotiationAnnouncement.put ("t", lTime);	// time

		jsonWrapper.set ("psn", jsonNegotiationAnnouncement);

		bot.sendAction (sTargetChannel, LiuYanBot.jacksonObjectMapper_Loose.writeValueAsString (jsonWrapper));

		OnPrimaryWasElected (bot, sTargetChannel, bot.getNick ());
bot.logger.exiting (PrimarySecondaryNegotiator.class.getName (), "Announce");
	}

	void OnPrimaryWasElected (LiuYanBot bot, String sChannel, String sFromNickName)
	{
		sChannel = StringUtils.lowerCase (sChannel);
		// 接收方收到消息后，要根据情况进行退位、删除缓存的当前主从协商（以便可以进行下一个主从协商）
		Map<String, Object> mapChannelState = GetCurrentChannelState (sChannel);
		mapChannelState.put ("AmIPrimary", StringUtils.equalsIgnoreCase (sFromNickName, bot.getNick ()));
		CleanUpCurrentNegotiation (sChannel);
	}

	Timer GetSchedulerTimer (String sChannel)
	{
		return GetSchedulerTimer (sChannel, true);
	}
	Timer GetSchedulerTimer (String sChannel, boolean bNew)
	{
		sChannel = StringUtils.lowerCase (sChannel);
		Map<String, Object> mapChannelState = GetCurrentChannelState (sChannel);
		Timer schedulerTimer = (Timer) mapChannelState.get ("SchedulerTimer");
		if (/*schedulerTimer==null ||*/ bNew)
		{
			schedulerTimer = new Timer ();
			mapChannelState.put ("SchedulerTimer", schedulerTimer);
		}
		return schedulerTimer;
	}
	void CancelSchedulerTimer (String sChannel)
	{
		Timer schedulerTimer = GetSchedulerTimer (sChannel, false);
		if (schedulerTimer == null)
			return;
		schedulerTimer.cancel ();
		GetCurrentChannelState (sChannel).remove ("SchedulerTimer");
	}
	void CleanUpCurrentNegotiation (String sChannel)
	{
		sChannel = StringUtils.lowerCase (sChannel);
		Map<String, Object> mapChannelState = GetCurrentChannelState (sChannel);
		mapChannelState.remove ("CurrentNegotiation");
		mapChannelState.remove ("CurrentNegotiationInitiator");
		mapChannelState.remove ("Votes");
		CancelSchedulerTimer (sChannel);
	}

	// 不能截取签名字符串，然后对比字符串是否一致 的方式进行签名验证，因为即使输入是一样的，签名字符串也会变
	/**
	public String GenerateShortSignatureString (String sIID, String sNegotiationCode, String sMessage, Long nTime)
	{
		return GenerateShortSignatureString (sIID, sNegotiationCode, sMessage, nTime, "");
	}
	public String GenerateShortSignatureString (String sIID, String sNegotiationCode, String sMessage, Long nTime, String sSalt)
	{
		String sFullSignature = GenerateSignatureString (sIID, sNegotiationCode, sMessage, nTime, sSalt);
		if (sFullSignature == null)
			return null;
		String sShortSignature = StringUtils.right (sFullSignature, 40);
System.err.println ("sShortSignature: " + sShortSignature);
		return sShortSignature;
	}
	*/

	public String GenerateSignatureBase64String (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime)
	{
		return GenerateSignatureString (sFromNickName, sFromAccount, sChannel,sIID, sNegotiationCode, sMessage, nTime, true);
	}
	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime, boolean bBase64Encoded)
	{
		return GenerateSignatureString (sFromNickName, sFromAccount, sChannel,sIID, sNegotiationCode, sMessage, nTime, bBase64Encoded, false);
	}
	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime, boolean bBase64Encoded, boolean bLowerCase)
	{
		return GenerateSignatureString (keyPrivateKey, sSignatureAlgorithm, sFromNickName, sFromAccount, sChannel,sIID, sNegotiationCode, sMessage, nTime, "", bBase64Encoded, bLowerCase);
	}

	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime, String sSalt, boolean bBase64Encoded)
	{
		return GenerateSignatureString (sFromNickName, sFromAccount, sChannel,sIID, sNegotiationCode, sMessage, nTime, sSalt, bBase64Encoded, false);
	}
	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime, String sSalt, boolean bBase64Encoded, boolean bLowerCase)
	{
		return GenerateSignatureString (keyPrivateKey, sSignatureAlgorithm, sFromNickName, sFromAccount, sChannel,sIID, sNegotiationCode, sMessage, nTime, sSalt, bBase64Encoded, bLowerCase);
	}

	public static String GenerateSignatureString (PrivateKey private_key, String sSignatureAlgorithm, String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime, String sSalt, boolean bBase64Encoded, boolean bLowerCase)
	{
		Signature sign = null;
		if (private_key == null)
		{
System.err.println ("private_key 为 null，不能生成签名");
			return null;
		}

		sChannel = StringUtils.lowerCase (sChannel);
		try
		{
System.out.println ("private_key " + private_key);
System.out.println ("private_key 算法 " + private_key.getAlgorithm ());
System.out.println ("Signature.getInstance " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign = Signature.getInstance (sSignatureAlgorithm);
System.out.println ("initSign " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.initSign (private_key);
			String sSrc = sFromNickName + /*"!~" + sFromAccount +*/ "@... PRIVMSG " + sChannel + " :ACTION " + sIID + sNegotiationCode + sMessage + nTime + sSalt;	// 在 znc 后面运行 bot 时，FromAccount 是 znc 分配的帐号，不是 IRC Server 分配的帐号，其他 bot 不知道 znc 分配的帐号，所以，签名会验证失败
System.out.println ("update " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.update (sSrc.getBytes (StandardCharsets.UTF_8));
System.out.println ("sign " + new java.sql.Timestamp (System.currentTimeMillis ()));
			byte[] arraySignature = sign.sign ();
			String sSignature = ByteArrayToHexString (arraySignature, bLowerCase);
			String sSignature_Base64 = Base64.getEncoder ().encodeToString (arraySignature);
System.out.println ("sign done " + new java.sql.Timestamp (System.currentTimeMillis ()));

System.err.println ("----------");
System.err.println ("sSignatureAlgorithm: " + sSignatureAlgorithm);
System.err.println ("----------");
System.err.println ("sFromNickName: " + sFromNickName);
//System.err.println ("sFromAccount: " + sFromAccount);
System.err.println ("sChannel: " + sChannel);
System.err.println ("sIID: " + sIID);
System.err.println ("sNegotiationCode: " + sNegotiationCode);
System.err.println ("sMessage: " + sMessage);
System.err.println ("nTime: " + nTime);
System.err.println ("sSalt: " + sSalt);
System.err.println ("----------");
System.err.println ("发送方构造的 sSrc: " + sSrc);
System.err.println ("发送方计算的 sSignature: " + sSignature);
System.err.println ("发送方计算的 sSignature_Base64: " + sSignature_Base64);
System.err.println ("----------");
			if (bBase64Encoded)
				return sSignature_Base64;
			else
				return sSignature;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static String ByteArrayToHexString (byte[] array)
	{
		return ByteArrayToHexString (array, false);
	}
	public static String ByteArrayToHexString (byte[] array, boolean bLowerCase)
	{
		char[] arrayHexCharacters = new char[array.length * 2];
		for (int i=0; i<array.length; i++)
		{
			int nTemp = array[i] & 0xFF;
			int nHigh = nTemp >>> 4;
			int nLow = nTemp & 0x0F;
			arrayHexCharacters[i*2]     = nHigh<10 ? (char)('0'+nHigh) : (char)((bLowerCase ? 'a' : 'A')+nHigh-0x0A);
			arrayHexCharacters[i*2 + 1] = nLow <10 ? (char)('0'+nLow ) : (char)((bLowerCase ? 'a' : 'A')+nLow -0x0A);
		}
		return new String (arrayHexCharacters);
	}

	public Boolean VerifyData_Signature_Base64 (String sSignature_Base64, String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime)
	{
		byte[] arraySignature = Base64.getDecoder ().decode (sSignature_Base64);
		return VerifyData (keyPublicKey, sSignatureAlgorithm, arraySignature, sFromNickName, sFromAccount, sChannel, sIID, sNegotiationCode, sMessage, nTime);
	}
	public Boolean VerifyData (String sSignature, String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime) throws IOException, DecoderException
	{
		byte[] arraySignature = Hex.decodeHex (sSignature);
		return VerifyData (keyPublicKey, sSignatureAlgorithm, arraySignature, sFromNickName, sFromAccount, sChannel, sIID, sNegotiationCode, sMessage, nTime);
	}
	public static Boolean VerifyData (PublicKey public_key, String sSignatureAlgorithm, byte[] arraySignature, String sFromNickName, String sFromAccount, String sChannel, String sIID, String sNegotiationCode, String sMessage, Long nTime)
	{
		Signature sign = null;
		if (public_key == null)
		{
System.err.println ("public_key 为 null，不能验证签名");
			return false;
		}

		sChannel = StringUtils.lowerCase (sChannel);
		try
		{
System.out.println ("public_key " + public_key);
System.out.println ("public_key 算法 " + public_key.getAlgorithm ());
System.out.println ("Signature.getInstance " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign = Signature.getInstance (sSignatureAlgorithm);
System.out.println ("initVerify " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.initVerify (public_key);
			String sSrc = sFromNickName + /*"!~" + sFromAccount +*/ "@... PRIVMSG " + sChannel + " :ACTION " + sIID + sNegotiationCode + sMessage + nTime;
System.out.println ("签收方构造的 sSrc " + sSrc);
System.out.println ("update " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.update (sSrc.getBytes (StandardCharsets.UTF_8));

System.out.println ("verify " + new java.sql.Timestamp (System.currentTimeMillis ()));
			boolean bValid = sign.verify (arraySignature);
System.out.println ("verify done " + new java.sql.Timestamp (System.currentTimeMillis ()));
System.out.println ("签收方 bValid " + bValid);
			return bValid;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static void main (String[] args) throws Exception
	{
		if (args.length < 5)
		{
/*
keyalg 必须匹配，否则报错：
java.security.InvalidKeyException: not a DSA private key: SunRsaSign RSA private CRT key, 4096 bits
  params: null
  modulus: 696897410278629116918970634403279731282220696045806437008837951844476322835857392403706309320959365738005373673141435247199041630966083659052128229505187766528530754658895748738370533444051080016324403863546009174616554112878355245183037487398871165965857561643629719647122373910644809308680907099055219683816582027427610784396491859831367134328618202270045597852879761197469114954187125992527579885432411453521192068326323112677304738868557750637006481787806082064951758591567869071886521980942547208231154619023844743153073752490617216401646572203502917573183601706500633704396782729039486167800561346346161415537813479341471537343667686972078641660299970006726539025184830369923781242593295463960971672788296763565724406509501333223187651142146310257319252922748601249672433810443827172153904313026724001501345989245187287509097119700257682955769801799546590895770371547989407118941543659410186103985902875205272591054907370577072139897823451362623816341103630784320150581250545199989363837697934539122492805109573202037781982348774148304663200136415734407408378903892166908891779492456140661980188247227713694497599847767288099692628409724552060093532651604849691110180509043211179449500264450298314780010434250618628453521843979
  private exponent: 2994491334903469627100924411019527098975445637533066770009217341545009581034783065938670729207842485427032983247023376341157604219138590750069195217139299491577601244778900872889522061216038913837618221986216642658896479353933165708203274547465481671261891341069882432801662268408367994781651821778051412434968934493212296711390375461693980702122683362669282816787168969466872870979573786461339087825042276224892758981129977854518435150988407709251666740929516265290498323842926722365051189130634200879407448254691870561057531272170921059744483417221804284502461735902203609078499639008037862694530051311814327988155174701987789362016328401123840548309329894150215872777911214798831748691736983493750747208955498253873981389893540796212646585052900932989615896364362862858195885699197144127623143588989064552027489066925281782964663737033975254552731716454710471235386789082502456100277292190337716818963178134806753240370952546336926082851323139261801290393630037533439216406910389913733960116125051164310562059032025598317998945643428462700118348156365234770540120044444297913313032071845222978016835024021289779392838648365748052080272038134955794999575381602627814190232974836447918119781908539721665694098815452997953769179937
	at java.base/sun.security.provider.DSA.engineInitSign(DSA.java:140)
	at java.base/java.security.Signature$Delegate.engineInitSign(Signature.java:1351)
	at java.base/java.security.Signature.initSign(Signature.java:636)
	at net.maclife.irc.PrimarySecondaryNegotiator.GenerateVerificationString(PrimarySecondaryNegotiator.java:135)
	at net.maclife.irc.PrimarySecondaryNegotiator.GenerateVerificationString(PrimarySecondaryNegotiator.java:117)
	at net.maclife.irc.PrimarySecondaryNegotiator.main(PrimarySecondaryNegotiator.java:211)

 */
System.err.println ("必填参数：\n\t参数 1 指定 KeyStore 的文件名，参数 2 指定 KeyStore 的密码，参数 3 指定 Key 的名称，参数 4 指定 Key 的密码，参数 5 指定 签名 的算法，如 SHA256withDSA SHA256withRSA。");
System.err.println ("可选参数：\n\t参数 6 指定 NegotiationCode， 参数 7 指定 Message，参数 8 指定一个长整数，参数 9 指定一个任意随机的字符串（调料）");
			return;
		}

/*
$ keytool -genkey -keystore primary-secondary-negotiation.ks -storepass changeit -keyalg DSA -keysize 4096 -validity 3660 -alias psn-dsa -keypass changeit
您的名字与姓氏是什么?
  [Unknown]:  TideBot
您的组织单位名称是什么?
  [Unknown]:  技术部
您的组织名称是什么?
  [Unknown]:  TideStudio
您所在的城市或区域名称是什么?
  [Unknown]:  深圳
您所在的省/市/自治区名称是什么?
  [Unknown]:  广东
该单位的双字母国家/地区代码是什么?
  [Unknown]:  CN
CN=TideBot, OU=技术部, O=TideStudio, L=深圳, ST=广东, C=CN是否正确?
  [否]:  y

keytool 错误: java.lang.IllegalArgumentException: Invalid DSA Prime Size: 4096


$ keytool -genkey -keystore primary-secondary-negotiation.ks -storepass changeit -keyalg DSA -keysize 2048 -validity 3660 -alias psn-dsa -keypass changeit
您的名字与姓氏是什么?
  [Unknown]:  TideBot
您的组织单位名称是什么?
  [Unknown]:  技术部
您的组织名称是什么?
  [Unknown]:  TideStudio
您所在的城市或区域名称是什么?
  [Unknown]:  深圳
您所在的省/市/自治区名称是什么?
  [Unknown]:  广东
该单位的双字母国家/地区代码是什么?
  [Unknown]:  CN
CN=TideBot, OU=技术部, O=TideStudio, L=深圳, ST=广东, C=CN是否正确?
  [否]:  y

正在为以下对象生成 2,048 位DSA密钥对和自签名证书 (SHA256withDSA) (有效期为 3,660 天):
	 CN=TideBot, OU=技术部, O=TideStudio, L=深圳, ST=广东, C=CN


$ keytool -genkey -keystore primary-secondary-negotiation.ks -storepass changeit -keyalg DSA -keysize 3072 -validity 3660 -alias psn-dsa -keypass changeit
您的名字与姓氏是什么?
  [Unknown]:  TideBot
您的组织单位名称是什么?
  [Unknown]:  技术部
您的组织名称是什么?
  [Unknown]:  TideStudio
您所在的城市或区域名称是什么?
  [Unknown]:  深圳
您所在的省/市/自治区名称是什么?
  [Unknown]:  广东
该单位的双字母国家/地区代码是什么?
  [Unknown]:  CN
CN=TideBot, OU=技术部, O=TideStudio, L=深圳, ST=广东, C=CN是否正确?
  [否]:  y

正在为以下对象生成 3,072 位DSA密钥对和自签名证书 (SHA256withDSA) (有效期为 3,660 天):
	 CN=TideBot, OU=技术部, O=TideStudio, L=深圳, ST=广东, C=CN


################################################################################
# Ed25519，从 JDK 17 才开始支持
################################################################################
$ /usr/lib/jvm/jre-17/bin/keytool -genkey -keystore primary-secondary-negotiation.ks -storepass changeit -alias psn-ed25519 -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN' -keyalg EdDSA -keysize 4096 -sigalg Ed25519 -validity 3660
keytool 错误: java.lang.IllegalArgumentException: Unsupported size: 4096

$ /usr/lib/jvm/jre-17/bin/keytool -genkey -keystore primary-secondary-negotiation.ks -storepass changeit -alias psn-ed25519 -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN'    -validity 3660    -keyalg EdDSA  -sigalg Ed25519
正在为以下对象生成 255 位Ed25519密钥对和自签名证书 (Ed25519) (有效期为 3,660 天):
	 CN=TideBot, OU=技术部, O=TideStudio, L=深圳, ST=广东, C=CN


$ /usr/lib/jvm/jre-17/bin/keytool -list -keystore ./primary-secondary-negotiation.ks
输入密钥库口令:
密钥库类型: PKCS12
密钥库提供方: SUN

您的密钥库包含 2 个条目

psn-dsa, 2022年11月17日, PrivateKeyEntry,
证书指纹 (SHA-256): 99:9A:E1:70:D6:BB:86:1F:8A:7F:56:44:7C:53:5E:A9:E6:55:B2:89:5F:E3:F5:C5:D4:33:98:B1:2A:BB:C8:94
psn-ed25519, 2022年11月17日, PrivateKeyEntry,
证书指纹 (SHA-256): C9:26:47:CB:A8:6A:2A:90:03:D2:A4:72:85:46:B6:28:C4:44:48:D5:8D:F5:26:C8:40:E0:61:7C:0E:CB:6D:BE

Warning:
<psn-dsa> 使用的 SHA256withDSA 签名算法被视为存在安全风险而且被禁用。
<psn-dsa> 使用的 3072 位 DSA 密钥 被视为存在安全风险而且被禁用。

################################################################################
# SHA256withRSA，JDK 1.8 可用的版本
#
# Every implementation of the Java platform is required to support the following standard Signature algorithms:
#
#    SHA1withDSA
#    SHA256withDSA
#    SHA1withRSA
#    SHA256withRSA
################################################################################
$ /usr/lib/jvm/jre-1.8.0/bin/keytool -genkey -keystore "primary-secondary-negotiation[JDK1.8] keyalg=RSA sigalg=SHA256withRSA.ks" -storepass changeit -alias psn-rsa -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN'    -validity 3660    -keyalg RSA -keysize 2048    -sigalg SHA256withRSA

Warning:
JKS 密钥库使用专用格式。建议使用 "keytool -importkeystore -srckeystore primary-secondary-negotiation[JDK1.8] keyalg=RSA sigalg=SHA256withRSA.ks -destkeystore primary-secondary-negotiation[JDK1.8] keyalg=RSA sigalg=SHA256withRSA.ks -deststoretype pkcs12" 迁移到行业标准格式 PKCS12

$ /usr/lib/jvm/jre-1.8.0/bin/keytool -genkey -keystore "primary-secondary-negotiation[JDK1.8] keyalg=RSA sigalg=SHA256withRSA.ks" -storetype PKCS12 -storepass changeit -alias psn-rsa -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN'    -validity 3660    -keyalg RSA -keysize 2048    -sigalg SHA256withRSA

# 又或者，主从协商不需要太强的保护，collision 不至于引起什么大问题，那么可以尝试一下 RSA keysize<1024、MD5 （但需要先修改 JDK 的 security/java.properties）
$ /usr/lib/jvm/jre-1.8.0/bin/keytool -genkey -keystore "primary-secondary-negotiation[JDK1.8] keyalg=RSA[keysize=384] sigalg=SHA1withRSA.ks" -storetype PKCS12 -storepass changeit -alias psn-rsa -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN'    -validity 3660    -keyalg RSA -keysize 384    -sigalg SHA1withRSA
keytool 错误: java.lang.IllegalArgumentException: Invalid key sizes

$ /usr/lib/jvm/jre-1.8.0/bin/keytool -genkey -keystore "primary-secondary-negotiation[JDK1.8] keyalg=RSA[keysize=384] sigalg=MD5withRSA.ks" -storetype PKCS12 -storepass changeit -alias psn-rsa -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN'    -validity 3660    -keyalg RSA -keysize 384    -sigalg MD5withRSA

# 不修改 security/java.properties 的话，只能用最小允许的强度
$ /usr/lib/jvm/jre-1.8.0/bin/keytool -genkey -keystore "primary-secondary-negotiation[JDK1.8] keyalg=RSA[keysize=1024] sigalg=SHA1withRSA.ks" -storetype PKCS12 -storepass changeit -alias psn-rsa -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN'    -validity 3660    -keyalg RSA -keysize 1024    -sigalg SHA1withRSA

Warning:
生成的证书 uses the SHA1withRSA signature algorithm which is considered a security risk and is disabled.
生成的证书 uses a 1024 位 RSA 密钥 which is considered a security risk and is disabled.
 */
		PrimarySecondaryNegotiator psn = new PrimarySecondaryNegotiator (null, args[0], args[1], args[2], args[3]);
		psn.sSignatureAlgorithm = args[4];

		int iArg=5;
		String sNegotiationCode = args.length > iArg ? args[iArg] : "阶段1_发起协商_我要当首选Bot";	iArg ++;
		String sMessage = args.length > iArg ? args[iArg] : "反对的请举手";	iArg ++;
		long lTime = args.length > iArg ? Long.valueOf (args[iArg]) : System.currentTimeMillis ();	iArg ++;
		String sSalt =  args.length > iArg ? args[iArg] : "";	iArg ++;
		psn.GenerateSignatureString ("IRC昵称", "", "IRC#频道名", String.valueOf(lTime), sNegotiationCode, sMessage, lTime, sSalt, true);
//System.out.println ();
	}
}
