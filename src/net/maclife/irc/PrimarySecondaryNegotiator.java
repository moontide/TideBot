package net.maclife.irc;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

import org.apache.commons.lang3.*;
import org.apache.pdfbox.util.*;
import org.jibble.pircbot.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import net.maclife.ansi.*;
import net.maclife.irc.dialog.*;

/**
 简单的主从协商，主要用于解决在频道里发 $ht模板、.action模板 快捷命令时，多个 bot 会全部响应的问题。
 <p>
 支持在不同的频道里进行协商。即，支持这种情况：
<ul>
<li>Bot A 在 Channel A 是主，但在 Channel B 是从；</li>
<li>Bot B 在 Channel A 是从，但在 Channel B 是主。</li>
</ul>
 </p>

<p>
 消息要加密，避免被其他人、其他 bot 恶搞，消息经过验证后是真的协商消息，才执行协商。
 多个 bot 实例，需要使用相同的秘钥。
</p>

<p>
 异常情况处理：Bot A 刚进频道，同组 bot 只有它自己，它发起协商，无其他 bot 应答，超时后，它成为了主 bot。然后它超时断线了。同组的另一个 Bot B 进来了，执行了相同的操作，成为了主 Bot。然后 Bot A 回来了…
</p>

<p>
 发起协商，请求当话事人(Master Slave Negotiation / Primary Secondary Negotiation)：
 {"msn":"i wanna be primary","msg":"反对的请举手","msgtime":"2022-11-15 12:00:00.001","become-primary-time":"这里是成为主 Bot 的时间，仅当出现多个主 Bot 的情况时才有用","sign":"0011223344556677889900...."}
 </p>

<p>
 响应，情况1，已经有 bot 实例是 主要 bot：
 {"msna":"reject/down/against","msg":"这里可以选一些景点电影里的台词，如“还有谁？！”“You Shall Not Pass!”","msgtime":"2022-11-15 12:00:00.002","become-primary-time":"这里是成为主 Bot 的时间，仅当出现多个主 Bot 的情况时才有用","sign":"0011223344556677889900...."}
</p>

<p>
 响应，情况2，其他 bot 实例不是 主要 bot：
 {"msna":"ok","msg":"这里可以选一些社畜的台词，如“收到！”“好的！”“1”","sign":"0011223344556677889900...."}
 </p>

<p>
 无响应，当没有 Bot 响应时，暂定 60 秒内，超时后，自封为主 Bot。
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
 	<dd>没有，一个 Bot 不知道另一个 Bot 的存在。而且，因为这是在 IRC 频道中执行协商，首次协商成功后，即使知道了同组 Bot 的存在，也不便于在频道里频繁的发心跳信息。</dd>

  	<dt>没有心跳检测，那 Primary 断线了怎么办，快捷命令就没有 Primary Bot 响应了</dt>
 	<dd>暂时没有完美的解决方法。目前，需要在另一个 Bot 的命令行控制台手工执行协商，将其设置为首选 Bot</dd>

  	<dt>怎么与别的 Bot 实例区分开，不混在一起？（或者说：Bot 组、Bot 集群 如何划分？）</dt>
 	<dd>一组 Bot 使用同一个公钥秘钥对，使用不同的 KeyPair 进行签名和验证时，验证会失败 -- 不会当做自己所在 Bot 组的协商消息。所以，只需要使用不同的秘钥对，即可划分为不同的 Bot 组</dd>

  	<dt>同一时间内能进行多个协商吗？</dt>
 	<dd>同一组（同一集群）的 Bot 同一时间内只能有一个协商，只有一个协商结束后，才能进行下一个协商。当不同组的 Bot 在一起时，可以同时进行各组的协商。</dd>

  	<dt>为什么要支持【在不同的频道里进行协商】</dt>
 	<dd>存在这种需求：在不同的频道需要由特定 Bot 实例执行快捷命令。比如：多个 Bot 实例分别运行在不同 CPU 架构的主机上，然后有几个关于各自 CPU 架构的频道，这时“有可能”需要在特定 CPU 架构频道用运行在该 CPU 架构上的 Bot 实例来执行快捷命令。</dd>

  	<dt></dt>
 	<dd></dd>

  	<dt></dt>
 	<dd></dd>
</dl>
 */
public class PrimarySecondaryNegotiator //implements DialogUser
{
	/**
	 * 协商阶段。
	 * @author liuyan
	 *
	 */
	//enum NegotiationStage
	//{
	//	__NONE__,

		/**
		 * 发起
		 */
	//	INITIATE,

		/**
		 * 对发起方进行 回复 / 响应 / 投票
		 * （投票策略：一票否决制，有 Bot 反对就不成功）
		 */
	//	REPLY,	// VOTE

		/**
		 * 宣布结果阶段
		 */
	//	ANNOUNCE,
	//};
	enum NegotiationCode
	{
		I_WANNA_BE_PRIMARY,
		OK,
		REJECT,
		ANNOUNCE,
	};

	LiuYanBot oThisBot = null;

	/**
	 * 协商器在通道中的状态。
	 * <dl>
	 * 	<dt>key : 频道名称</dt>
	 * <dd>每个频道名一个 key，如： <code>#liuyanbot</code></dd>
	 *
	 * <dt>value</dt>
	 * <dd>又是一个 Map，
	 * 		<dl>
	 * 			<dt>key = <code>CurrentNegotiation</code></dt>
	 * 			<dd><code>JsonNode</code> 类型，当前正在进行的协商。同一组 Bot、在同一频道内、同一时间内 只能有一个协商。当有其他协商时</dd>

	 * 			<dt>key = <code>CurrentNegotiationInitiator</code></dt>
	 * 			<dd><code>String</code> 类型, 当前主从协商发起方的昵称</dd>

	 * 			<dt>key = <code>AmIPrimary</code></dt>
	 * 			<dd><code>Boolean</code> 类型, 我是否是首选 Bot</dd>

	 * 			<dt>key = <code>Votes</code></dt>
	 * 			<dd><code>Map</code> 类型, 投票记录。
	 * 				<dl>
	 * 					<dt>key: 投票 Bot 的昵称</dt>
	 * 					<dt>value: <code>NegotiationCode</code> ，OK 或 REJECT</dt>
	 * 				</dl>
	 * 			</dd>
	 * 		</dl>
	 * </dd>
	 * <dl>
	 */
	Map<String, Map<String, Object>> mapChannelsState = new HashMap<String, Map<String, Object>> ();

	//KeyPair keypair = null;
	PrivateKey keyPrivateKey = null;
	PublicKey keyPublicKey = null;

	String sSignatureAlgorithm = System.getProperty ("primary-secondary-negotiation.signature.algorithm", "SHA256withDSA");

	String[] arrayInitiatMessages =
		{
			"谁赞成？谁反对？",
			"同意的请举手，反对的请举手",
		};
	String[] arrayVoteMessages =
		{
			// 中国古装电视剧风格
			"恩准",
			"准奏",

			// OA 审批风格
			"同意",

			// 社畜风格
			"收到",
			"好的",
		};
	String[] arrayVoteAgainstMessages =
		{
			//
			"反对",
		};

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
		KeyStore ks = KeyStore.getInstance (sKeyStoreType);
System.out.println ("ks.load(InputStream, char[]) " + new java.sql.Timestamp (System.currentTimeMillis ()));
		ks.load (new FileInputStream(sKeyStoreFileName), sKeyStorePassword.toCharArray ());
System.out.println ("ks.load(InputStream, char[]) done " + new java.sql.Timestamp (System.currentTimeMillis ()));

		GetPrivatePublicKey (ks, sKeyName, sKeyPassword);
	}
	public void GetPrivatePublicKey (KeyStore ks, String sKeyName, String sPassword) throws Exception
	{
System.out.println ("ks.getKey (String, char[]) " + new java.sql.Timestamp (System.currentTimeMillis ()));
		keyPrivateKey = (PrivateKey)ks.getKey (sKeyName, sPassword.toCharArray ());
System.out.println ("ks.getCertificate (String).getPublicKey () " + new java.sql.Timestamp (System.currentTimeMillis ()));
		keyPublicKey = ks.getCertificate (sKeyName).getPublicKey ();
System.out.println ("ks.getCertificate (String).getPublicKey () done " + new java.sql.Timestamp (System.currentTimeMillis ()));
	}

	Map<String, Object> GetCurrentChannelState (String sChannel)
	{
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
		return (JsonNode)GetCurrentChannelState (sChannel).get ("CurrentNegotiation");
	}

	String GetCurrentNegotiationInitiator (String sChannel)
	{
		return (String)GetCurrentChannelState (sChannel).get ("CurrentNegotiationInitiator");
	}

	void SetCurrentNegotiationAndInitiator (String sChannel, JsonNode jsonInitiateNegotiatioin_WithoutWrapper, String sInitiator)
	{
		GetCurrentChannelState (sChannel).put ("CurrentNegotiation", jsonInitiateNegotiatioin_WithoutWrapper);
		GetCurrentChannelState (sChannel).put ("CurrentNegotiationInitiator", sInitiator);
	}

	public boolean AmIPrimary (String sChannel)
	{
		Boolean bPrimary = (Boolean)GetCurrentChannelState (sChannel).get ("AmIPrimary");
		return bPrimary==null ? false : bPrimary;
	}

	Map<String, NegotiationCode> GetCurrentNegotiationVotes (String sChannel)
	{
		Map<String, NegotiationCode> mapVotes = (Map<String, NegotiationCode>)GetCurrentChannelState (sChannel).get ("Votes");
		if (mapVotes == null)
		{
			mapVotes = new HashMap<String, NegotiationCode> ();
			GetCurrentChannelState (sChannel).put ("Votes", mapVotes);
		}
		return mapVotes;
	}

	public void InitiateNegotiation (String sChannel, boolean bForced)
	{
		if (GetCurrentNegotiation(sChannel) != null)
		{
System.err.println (sChannel + " 频道当前有正在进行的主从协商，不能同时进行多个协商，只能一个一个来");
			return;
		}
		if (AmIPrimary(sChannel) && !bForced)
		{
System.err.println ("我现在就是 " + sChannel + " 频道的首选 Bot，不需要再发起主从协商（除非强制发起）");
			return;
		}

		ObjectNode jsonWrapper = LiuYanBot.jacksonObjectMapper_Strict.createObjectNode ();
		ObjectNode jsonInitiateNegotiation = LiuYanBot.jacksonObjectMapper_Strict.createObjectNode ();
		String sActionCode = NegotiationCode.I_WANNA_BE_PRIMARY.toString ();
		String sMessage = arrayInitiatMessages[oThisBot.rand.nextInt (arrayInitiatMessages.length)];
		long lTime = System.currentTimeMillis ();
		String sIID = String.valueOf (lTime);
		//jsonInitiateNegotiation.put ("stage", NegotiationStage.INITIATE.toString ());	// stage
		jsonInitiateNegotiation.put ("c", sActionCode);	// code
		jsonInitiateNegotiation.put ("iid", sIID);	// time
		jsonInitiateNegotiation.put ("m", sMessage);	// message
		jsonInitiateNegotiation.put ("t", lTime);	// time
		String sSignature = GenerateSignatureString (oThisBot.getNick (), oThisBot.getLogin (), sChannel, sIID, sActionCode, sMessage, lTime);
		if (sSignature == null)
			return;
		jsonInitiateNegotiation.put ("s", sSignature);	// signature
		if (bForced)
			jsonInitiateNegotiation.put ("f", bForced);	// forced 强制成为首选 bot，其他 bot 收到此消息后，如果是首选 bot 的，需要退位

		jsonWrapper.set ("psn", jsonInitiateNegotiation);

		oThisBot.sendAction (sChannel, jsonWrapper.toString ());
		SetCurrentNegotiationAndInitiator (sChannel, jsonInitiateNegotiation, oThisBot.getNick ());

		new Timer().schedule
		(
			new TimerTask()
			{
				@Override
				public void run ()
				{
					Map<String, NegotiationCode> mapVotes = GetCurrentNegotiationVotes (sChannel);
					int nOK = 0;
					int nReject = 0;
					for (NegotiationCode actioncode : mapVotes.values ())
					{
						switch (actioncode)
						{
							case OK:
								nOK ++;
								break;
							case REJECT:
								nReject ++;
								break;
						}
					}
System.out.println ("主从协商结束：" + nOK + " 票同意，" + nReject + " 票反对。");
					if (nReject == 0)
					{
						// Announce 新话事人产生
						Announce (oThisBot, sChannel, sIID, "新话事人就是我");
					}
				}
			}
			, 60*1000
		);
	}

	public void OnActionReceived (LiuYanBot bot, String sFromNickName, String sFromAccount, String sHostname, String sTargetChannel, String sAction)
	{
bot.logger.entering (PrimarySecondaryNegotiator.class.getName (), "OnActionReceived");
		if (StringUtils.isEmpty (sAction) || !StringUtils.equalsIgnoreCase (StringUtils.left (sAction, 1), "{"))
			return;

		try
		{
			JsonNode json = LiuYanBot.jacksonObjectMapper_Strict.readTree (sAction);

			// 先检验消息格式是否是 PrimarySecondaryNegotiation 消息格式
			JsonNode jsonNegotiation = json.get ("psn");
			if (jsonNegotiation==null || jsonNegotiation.isNull ())
			{
System.err.println (sTargetChannel + " 频道，JSON 消息中没有包含 psn，这不是主从协商消息");
				return;
			}
			//JsonNode jsonStage = jsonNegotiation.get ("stage");
			//if (jsonStage==null || jsonStage.isNull ())
			//	return;
			JsonNode jsonInitiationID = jsonNegotiation.get ("iid");	// 此 协商发起ID ，仅仅在发起方才生成此 ID，后续响应方需要原样返回，而不再生成新 iid
			JsonNode jsonActionCode = jsonNegotiation.get ("c");	// Action Code
			JsonNode jsonMessage = jsonNegotiation.get ("m");	// Message 附加的文字消息，其本身仅仅用来生成签名、搞笑的用途，并不影响消息判断
			JsonNode jsonTime = jsonNegotiation.get ("t");	// Time of local time of the sender bot
			JsonNode jsonSignature = jsonNegotiation.get ("s");	// Signature 签名，十六进制字符串。为了避免达到受 IRC 单条消息长度限制，字符串长度可能会从原始签名字符串中裁剪出一部分，比如说：从结尾部分截取 40 个字符长度（20 个原始签名字节长度）
			JsonNode jsonForced = jsonNegotiation.get ("f");	// Forced 是否强制成为首选 Bot，

			if (false)	// 消息格式不正确
				return;

			// 再验证数据的基本有效性
			String sIID = jsonInitiationID.asText ();
			String sActionCode = jsonActionCode.asText ();
			NegotiationCode actioncode = null;

			try
			{
				actioncode = NegotiationCode.valueOf (sActionCode);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace ();
System.err.println (sTargetChannel + " 频道，ActionCode 参数无效");
				return;
			}
			String sMessage = jsonMessage.asText ();
			long lTime = jsonTime.asLong ();	// 时间不可超过 1 分钟，超过 1 分钟则认为是过期消息，不再处理
			if ((System.currentTimeMillis () - lTime) > 60*1000)
			{
System.err.println (sTargetChannel + " 频道，主从协商时间时长超时，不再处理");
				return;
			}

			// 再验证签名是否有效（是否同一组 Bot 发出的 PSN 消息、是否别人伪造的 PSN 消息…）
			String sSignature = jsonSignature.asText ();
			//String sMyCalculatedSign = GenerateSignatureString (sIID, sActionCode, sMessage, lTime);
			//if (! StringUtils.equalsIgnoreCase (sMyCalculatedSign, sSignature))
			if (! VerifyData (sSignature, sFromNickName, sFromAccount, sTargetChannel, sIID, sActionCode, sMessage, lTime))
			{
LiuYanBot.logger.warning (sTargetChannel + " 频道，签名不一致，不处理。（可能是不同的 Bot 集群、或 可能是伪造）");
				return;
			}

			// 开始处理

			boolean bForced = false;
			if (jsonForced!=null && ! jsonForced.isNull ())
				bForced = jsonForced.asBoolean ();

			switch (actioncode)
			{
				case I_WANNA_BE_PRIMARY:
System.err.println (sTargetChannel + " 频道，收到其他 Bot 想要成为首选的请求");
					if (bForced)
					{
System.err.println ("强制性的");
						//
						//Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.OK, arrayVoteMessages[bot.rand.nextInt (arrayVoteMessages.length)]);
						OnPrimaryWasElected (bot, sTargetChannel, sFromNickName);
					}
					else
					{
System.err.println ("非强制性的");
						// 如果有当前有正在进行的协商，则否决（同一组 Bot ，同一时间内，不能进行多个协商，即：一个一个来）
						// implement it...
						if (GetCurrentNegotiation(sTargetChannel) != null)
						{
							Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.REJECT, "本频道、本 Bot 群组、当前有另外一个主从协商正在进行，一个一个来");
							break;
						}
						// 如果自己是 Primary，则否决；否则，赞成
						if (AmIPrimary(sTargetChannel))
						{
							Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.REJECT, "下一届 我会全力支持你做话事人");
							break;
						}

						SetCurrentNegotiationAndInitiator (sTargetChannel, jsonNegotiation, sFromNickName);
						Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.OK, arrayVoteMessages[bot.rand.nextInt (arrayVoteMessages.length)]);

						//// 测试
						//int n = bot.rand.nextInt ();
//System.err.println ("随机数 n = " + n);
						//if ((n & 0x01) == 0)
						//	Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.OK, arrayVoteMessages[bot.rand.nextInt (arrayVoteMessages.length)]);
						//else
						//	Reply (bot, sFromNickName, sFromAccount, sHostname, sTargetChannel, sIID, NegotiationCode.REJECT, arrayVoteAgainstMessages[bot.rand.nextInt (arrayVoteAgainstMessages.length)]);
					}
					break;
				case OK:
				case REJECT:
					// 通常由发起方处理回复。其他接收方，也可以存储结果，但目前的实现方式是不处理
					if (! StringUtils.equalsIgnoreCase (bot.getNick (), GetCurrentNegotiationInitiator(sTargetChannel))
							|| (GetCurrentNegotiation(sTargetChannel)!=null
								&& !StringUtils.equalsIgnoreCase (sIID, GetCurrentNegotiation(sTargetChannel).get ("iid").asText ())
								)
						)
					{
System.err.println (sTargetChannel + " 频道，回复人不是发起人，或者，回复的不是当前正在进行的协商，不处理");
						break;
					}
					GetCurrentNegotiationVotes (sTargetChannel).put (sFromNickName, actioncode);
					break;
				case ANNOUNCE:
					OnPrimaryWasElected (bot, sTargetChannel, sFromNickName);
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

	void Reply (LiuYanBot bot, String sFromNickName, String sFromAccount, String sHostname, String sTargetChannel, String sIID, NegotiationCode actioncode, String sMessage)
	{
bot.logger.entering (PrimarySecondaryNegotiator.class.getName (), "Reply");
		ObjectNode jsonWrapper = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		ObjectNode jsonInitiateNegotiation = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		String sActionCode = actioncode.toString ();
		//if (actioncode == NegotiationCode.OK)
		//	sMessage = Colors.GREEN + sMessage + Colors.NORMAL;
		//else if (actioncode == NegotiationCode.REJECT)
		//	sMessage = Colors.RED + sMessage + Colors.NORMAL;
		long lTime = System.currentTimeMillis ();
		//jsonInitiateNegotiation.put ("stage", NegotiationStage.INITIATE.toString ());	// stage
		jsonInitiateNegotiation.put ("c", sActionCode);	// code
		jsonInitiateNegotiation.put ("iid", sIID);	// time
		jsonInitiateNegotiation.put ("m", sMessage);	// message
		jsonInitiateNegotiation.put ("t", lTime);	// time
		String sSignature = GenerateSignatureString (bot.getNick (), bot.getLogin (), sTargetChannel, sIID, sActionCode, sMessage, lTime);
		if (sSignature == null)
			return;
		jsonInitiateNegotiation.put ("s", sSignature);	// signature

		jsonWrapper.set ("psn", jsonInitiateNegotiation);

		bot.sendAction (sTargetChannel, jsonWrapper.toString ());
bot.logger.exiting (PrimarySecondaryNegotiator.class.getName (), "Reply");
	}

	void Announce (LiuYanBot bot, String sTargetChannel, String sIID, String sMessage)
	{
bot.logger.entering (PrimarySecondaryNegotiator.class.getName (), "Announce");
		ObjectNode jsonWrapper = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		ObjectNode jsonNegotiationAnnouncement = LiuYanBot.jacksonObjectMapper_Loose.createObjectNode ();
		long lTime = System.currentTimeMillis ();
		//jsonNegotiationAnnouncement.put ("stage", NegotiationStage.INITIATE.toString ());	// stage
		jsonNegotiationAnnouncement.put ("c", NegotiationCode.ANNOUNCE.toString ());	// code
		jsonNegotiationAnnouncement.put ("iid", sIID);	// time
		jsonNegotiationAnnouncement.put ("m", sMessage);	// message
		jsonNegotiationAnnouncement.put ("t", lTime);	// time
		String sSignature = GenerateSignatureString (bot.getNick (), bot.getLogin (), sTargetChannel, sIID, NegotiationCode.ANNOUNCE.toString (), sMessage, lTime);
		if (sSignature == null)
			return;
		jsonNegotiationAnnouncement.put ("s", sSignature);	// signature

		jsonWrapper.set ("psn", jsonNegotiationAnnouncement);

		bot.sendAction (sTargetChannel, jsonWrapper.toString ());

		OnPrimaryWasElected (bot, sTargetChannel, bot.getNick ());
bot.logger.exiting (PrimarySecondaryNegotiator.class.getName (), "Announce");
	}

	void OnPrimaryWasElected (LiuYanBot bot, String sChannel, String sFromNickName)
	{
		// 接收方收到消息后，要根据情况进行退位、删除缓存的当前协商（以便可以进行下一个协商）
		Map<String, Object> mapChannelState = GetCurrentChannelState (sChannel);
		mapChannelState.put ("AmIPrimary", StringUtils.equalsIgnoreCase (sFromNickName, bot.getNick ()));
		mapChannelState.remove ("CurrentNegotiation");
		mapChannelState.remove ("CurrentNegotiationInitiator");
	}

	// 不能截取签名字符串，然后对比字符串是否一致 的方式进行签名验证，因为即使输入是一样的，签名字符串也会变
	/**
	public String GenerateShortSignatureString (String sIID, String sActionCode, String sMessage, Long nTime)
	{
		return GenerateShortSignatureString (sIID, sActionCode, sMessage, nTime, "");
	}
	public String GenerateShortSignatureString (String sIID, String sActionCode, String sMessage, Long nTime, String sSalt)
	{
		String sFullSignature = GenerateSignatureString (sIID, sActionCode, sMessage, nTime, sSalt);
		if (sFullSignature == null)
			return null;
		String sShortSignature = StringUtils.right (sFullSignature, 40);
System.err.println ("sShortSignature: " + sShortSignature);
		return sShortSignature;
	}
	*/

	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sActionCode, String sMessage, Long nTime)
	{
		return GenerateSignatureString (sFromNickName, sFromAccount, sChannel,sIID, sActionCode, sMessage, nTime, false);
	}
	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sActionCode, String sMessage, Long nTime, boolean bLowerCase)
	{
		return GenerateSignatureString (keyPrivateKey, sSignatureAlgorithm, sFromNickName, sFromAccount, sChannel,sIID, sActionCode, sMessage, nTime, "", bLowerCase);
	}

	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sActionCode, String sMessage, Long nTime, String sSalt)
	{
		return GenerateSignatureString (sFromNickName, sFromAccount, sChannel,sIID, sActionCode, sMessage, nTime, sSalt, false);
	}
	public String GenerateSignatureString (String sFromNickName, String sFromAccount, String sChannel, String sIID, String sActionCode, String sMessage, Long nTime, String sSalt, boolean bLowerCase)
	{
		return GenerateSignatureString (keyPrivateKey, sSignatureAlgorithm, sFromNickName, sFromAccount, sChannel,sIID, sActionCode, sMessage, nTime, sSalt, bLowerCase);
	}

	public static String GenerateSignatureString (PrivateKey private_key, String sSignatureAlgorithm, String sFromNickName, String sFromAccount, String sChannel, String sIID, String sActionCode, String sMessage, Long nTime, String sSalt, boolean bLowerCase)
	{
		Signature sign = null;
		if (private_key == null)
		{
System.err.println ("private_key 为 null，不能生成签名");
			return null;
		}
		try
		{
System.out.println ("private_key " + private_key);
System.out.println ("private_key 算法 " + private_key.getAlgorithm ());
System.out.println ("Signature.getInstance " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign = Signature.getInstance (sSignatureAlgorithm);
System.out.println ("initSign " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.initSign (private_key);
			String sSrc = sFromNickName + /*"!~" + sFromAccount +*/ "@... PRIVMSG " + sChannel + " :ACTION " + sIID + sActionCode + sMessage + nTime + sSalt;	// 在 znc 后面运行 bot 时，FromAccount 是 znc 分配的帐号，不是 IRC Server 分配的帐号，其他 bot 不知道 znc 分配的帐号，所以，签名会验证失败
System.out.println ("update " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.update (sSrc.getBytes (StandardCharsets.UTF_8));
System.out.println ("sign " + new java.sql.Timestamp (System.currentTimeMillis ()));
			String sSignature = ByteArrayToHexString (sign.sign (), bLowerCase);
System.out.println ("sign done " + new java.sql.Timestamp (System.currentTimeMillis ()));

System.err.println ("----------");
System.err.println ("sSignatureAlgorithm: " + sSignatureAlgorithm);
System.err.println ("----------");
System.err.println ("sFromNickName: " + sFromNickName);
//System.err.println ("sFromAccount: " + sFromAccount);
System.err.println ("sChannel: " + sChannel);
System.err.println ("sIID: " + sIID);
System.err.println ("sActionCode: " + sActionCode);
System.err.println ("sMessage: " + sMessage);
System.err.println ("nTime: " + nTime);
System.err.println ("sSalt: " + sSalt);
System.err.println ("----------");
System.err.println ("发送方构造的 sSrc: " + sSrc);
System.err.println ("发送方计算的 sSignature: " + sSignature);
System.err.println ("----------");
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

	public Boolean VerifyData (String sSignature, String sFromNickName, String sFromAccount, String sChannel, String sIID, String sActionCode, String sMessage, Long nTime)
	{
		return VerifyData (keyPublicKey, sSignatureAlgorithm, sSignature, sFromNickName, sFromAccount, sChannel, sIID, sActionCode, sMessage, nTime);
	}
	public static Boolean VerifyData (PublicKey public_key, String sSignatureAlgorithm, String sSignature, String sFromNickName, String sFromAccount, String sChannel, String sIID, String sActionCode, String sMessage, Long nTime)
	{
		Signature sign = null;
		if (public_key == null)
		{
System.err.println ("public_key 为 null，不能验证签名");
			return false;
		}
		try
		{
System.out.println ("public_key " + public_key);
System.out.println ("public_key 算法 " + public_key.getAlgorithm ());
System.out.println ("Signature.getInstance " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign = Signature.getInstance (sSignatureAlgorithm);
System.out.println ("initVerify " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.initVerify (public_key);
			String sSrc = sFromNickName + /*"!~" + sFromAccount +*/ "@... PRIVMSG " + sChannel + " :ACTION " + sIID + sActionCode + sMessage + nTime;
System.out.println ("签收方构造的 sSrc " + sSrc);
System.out.println ("update " + new java.sql.Timestamp (System.currentTimeMillis ()));
			sign.update (sSrc.getBytes (StandardCharsets.UTF_8));

			byte[] arraySignature = Hex.decodeHex (sSignature);
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
		if (args.length < 4)
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
System.err.println ("可选参数：\n\t参数 6 指定 Action， 参数 7 指定 Message，参数 8 指定一个长整数，参数 9 指定一个任意随机的字符串（调料）");
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


$ keytool -genkey -keystore primary-secondary-negotiation.ks -storepass changeit -alias psn-ed25519 -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN' -keyalg EdDSA -keysize 4096 -sigalg Ed25519 -validity 3660
keytool 错误: java.lang.IllegalArgumentException: Unsupported size: 4096

$ keytool -genkey -keystore primary-secondary-negotiation.ks -storepass changeit -alias psn-ed25519 -keypass changeit    -dname 'CN=TideBot, OU=技术部, O=TideStudio, L=深圳, S=广东, C=CN'    -validity 3660    -keyalg EdDSA  -sigalg Ed25519
正在为以下对象生成 255 位Ed25519密钥对和自签名证书 (Ed25519) (有效期为 3,660 天):
	 CN=TideBot, OU=技术部, O=TideStudio, L=深圳, ST=广东, C=CN


$ keytool -list -keystore ./primary-secondary-negotiation.ks
输入密钥库口令:
密钥库类型: PKCS12
密钥库提供方: SUN

您的密钥库包含 2 个条目

psn-dsa, 2022年11月17日, PrivateKeyEntry,
证书指纹 (SHA-256): 99:9A:E1:70:D6:BB:86:1F:8A:7F:56:44:7C:53:5E:A9:E6:55:B2:89:5F:E3:F5:C5:D4:33:98:B1:2A:BB:C8:94
psn-ed25519, 2022年11月17日, PrivateKeyEntry,
证书指纹 (SHA-256): C9:26:47:CB:A8:6A:2A:90:03:D2:A4:72:85:46:B6:28:C4:44:48:D5:8D:F5:26:C8:40:E0:61:7C:0E:CB:6D:BE
 */
		PrimarySecondaryNegotiator psn = new PrimarySecondaryNegotiator (null, args[0], args[1], args[2], args[3]);
		psn.sSignatureAlgorithm = args[4];

		int iArg=5;
		String sActionCode = args.length > iArg ? args[iArg] : "I_WANNA_BE_PRIMARY";	iArg ++;
		String sMessage = args.length > iArg ? args[iArg] : "反对的请举手";	iArg ++;
		long lTime = args.length > iArg ? Long.valueOf (args[iArg]) : System.currentTimeMillis ();	iArg ++;
		String sSalt =  args.length > iArg ? args[iArg] : "";	iArg ++;
		psn.GenerateSignatureString ("", "", "", String.valueOf(lTime), sActionCode, sMessage, lTime, sSalt);
//System.out.println ();
	}
}
