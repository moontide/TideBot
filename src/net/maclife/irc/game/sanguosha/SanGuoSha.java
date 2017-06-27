package net.maclife.irc.game.sanguosha;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;
import net.maclife.irc.game.*;
import net.maclife.irc.game.sanguosha.card.*;

/**
三国杀引擎。
三国杀有几种不同的玩法，比如：身份局、国战、1v1、3v3 或者其他，几种不同的玩法在整体的逻辑和流程上是统一的（是可以被抽象出相同的逻辑和流程的）：
<ul>
	<li>玩家顺序出牌</li>
	<li>人物角色也会濒死/死亡，会触发濒死事件处理</li>
	<li>武器技能会触发</li>
	<li>人物角色技能会触发</li>
	<li>距离</li>
		<ul>
			<li>武器攻击距离</li>
			<li>+1 马距离</li>
			<li>-1 马距离</li>
		</ul>
</ul>
引擎只处理几件事情
<dl>
	<dt></dt>
	<dd></dd>

	<dt></dt>
	<dd></dd>

	<dt></dt>
	<dd></dd>

	<dt></dt>
	<dd></dd>
</dl>
 * @author liuyan
 *
 */
public abstract class SanGuoSha extends CardGame
{
	public enum 三国杀玩法
	{
		三国杀入门,
		三国杀身份,
		三国杀国战,
		三国杀1v1,
		三国杀3v3,
	}

	public static int GAME_STAGE_选武将 = 1;
	public static int GAME_STAGE_a = 3;
	public static int GAME_STAGE_战斗中 = 8;

	public enum PLAY_TURN_STAGE_玩家回合阶段
	{
		开始,	// 甄姬“洛神”技能
		判定,	// 闪电 乐 兵粮，夏侯渊“神速1”技能可跳过，张郃“”
		摸牌,	//
		出牌,	//
		弃牌,	// 吕蒙“克己”技能跳过
		结束,	// 貂蝉闭月
	}

	public enum PLAY_CARD_STAGE
	{
		出牌开始,
		出牌生效,
	}

	public enum EVENT
	{
		受到伤害,
		濒临死亡,
		角色阵亡,

		装备武器,
		装备防具,
		装备防御马,
		装备进攻马,
	}

	Object lastPlayedPlayer = null;
	String sLastPlayedPlayer = null;

	/**
	 * 弃牌堆
	 */
	List<SanGuoShaCard> recycle_deck = new ArrayList<SanGuoShaCard> ();

	/**
	 * 当前游戏的玩家状态。
	 * 想来想去，还是不要放到 SanGuoShaPlayer 里，因为状态
	 *
	 * key 列表：
	 * <dl>
	 * 	<dt>玩家名<code>.hp</code></dt>
	 * 	<dd>血量。int 类型。</dd>
	 * 	<dt>玩家名<code>.翻面</code></dt>
	 * 	<dd>当前是否翻面（是否被暂停一轮。呃，要不要换成 int 类型，可以暂停 n 轮？）。boolean 类型。</dd>
	 * </dl>
	 */
	Map<String, Object> mapPlayerState = new HashMap<String, Object> ();

	public SanGuoSha (LiuYanBot bot, List<Game> listGames, 三国杀玩法 玩法, Set<? extends Object> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (玩法.toString (), bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
System.out.println (participants);
		// 将 participants 类型改为 SanGuoShaPlayer
		for (int i=0; i<participants.size (); i++)
		{
			String sPlayerName = (String) participants.get (i);

			SanGuoShaPlayer p = new SanGuoShaPlayer (sPlayerName);
			participants.set (i, p);
		}
	}

	class SanGuoShaPlayRound implements Runnable
	{
		@Override
		public void run ()
		{
		}
	}

	Stack<SanGuoShaPlayRound> stackRounds = new Stack<SanGuoShaPlayRound> ();
	@Override
	public void run ()
	{
		threadID = Thread.currentThread ().getId ();
		try
		{
			if (participants.size ()<2 || participants.size ()>10)
				throw new IllegalArgumentException (name + " 游戏人数需要 2 - 10 人，而本次游戏人数为 " + participants.size ());

			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + threadID + " 开始。");

			//
			分配座次 ();

			//
			分配身份或者国家势力 ();

			//
			分配与挑选武将 ();

			//
			初始化牌堆 ();

			洗牌 ();

			//
			发初始手牌 ();


			开战 ();

			//
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + threadID + " 结束。");
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + threadID + " 异常: " + e);
		}
		finally
		{
			games.remove (this);
		}
	}

	public void 分配座次 () {};
	public void 分配身份或者国家势力 () {};
	public void 通告座次 () {};
	public void 分配与挑选武将 () {};
	public void 初始化牌堆 () {};
	public void 发初始手牌 () {};
	public void 发武将牌 () {};

	/**
	 * 顺序轮流
	 * @param iTurn 当前玩家的索引号
	 * @return 玩家在列表中索引号 (从 0 开始)
	 */
	int NextTurn (int iTurn)
	{
		int nDead = 0;
		while (true)
		{
			iTurn ++;
			if (iTurn >= participants.size ())
				iTurn = 0;

			SanGuoShaPlayer p = (SanGuoShaPlayer)participants.get (iTurn);
			if (p.isDead ())
			{
				nDead += 1;
				if (nDead >= participants.size () - 1)
				{
					// 全都死亡？？
					throw new RuntimeException ("游戏结束");
				}
			}
			if (! p.isDead ())
				break;
		}
		return iTurn;
	}
	int NextTurn (Object currentPlayer)
	{
		int iTurn = participants.indexOf (currentPlayer);
		return NextTurn (iTurn);
	}

	/**
	 * 	生成单个玩家的牌的信息
	 * @param p
	 * @param sb_in
	 * @return
	 */
	StringBuilder GenerateCardsInfoTo (String p, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p);
		GenerateCardsInfoTo (player_cards, sb);
		return sb;
	}
	StringBuilder GenerateCardsInfoTo (String p)
	{
		return GenerateCardsInfoTo (p, null);
	}
	StringBuilder GenerateCardsInfoTo (List<SanGuoShaCard> cards, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		for (int i=0; i<cards.size (); i++)
		{
			SanGuoShaCard card  = cards.get (i);
			sb.append (Colors.GREEN);
			sb.append (i+1);
			sb.append (Colors.NORMAL);
			sb.append (':');

			//sb.append (card.get ("rank"));
			sb.append (card.getIRCColor());
			sb.append (card.getSuit());
			sb.append (card.getRank());
			sb.append (card.getName());
			sb.append (Colors.NORMAL);

			sb.append (" ");
		}
		return sb;
	}
	StringBuilder GenerateCardsInfoTo (List<SanGuoShaCard> cards)
	{
		return GenerateCardsInfoTo (cards, null);
	}

	public void 开战 ()
	{
		int iTurn = 0;
		while (true)
		{
			SanGuoShaPlayer p = (SanGuoShaPlayer) participants.get (iTurn);
System.out.println ("进入 " + p.getName () + " 的回合； my turn!");
			for (PLAY_TURN_STAGE_玩家回合阶段 回合阶段 : PLAY_TURN_STAGE_玩家回合阶段.values ())
			{
				//if (p.拥有此阶段技能吗 (阶段))
				switch (回合阶段)
				{
					case 开始:
						当玩家进入回合开始阶段 (p);
						break;
					case 判定:
						当玩家进入回合判定阶段 (p);
						break;
					case 摸牌:
						当玩家进入回合摸牌阶段 (p);
						break;
					case 出牌:
						当玩家进入回合出牌阶段 (p);
						break;
					case 弃牌:
						当玩家进入回合弃牌阶段 (p);
						break;
					case 结束:
						当玩家进入回合结束阶段 (p);
						break;
				}
			}

			iTurn = NextTurn (iTurn);
		}
	}
	public void 当玩家进入回合开始阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合开始阶段"));
	}
	public void 当玩家进入回合判定阶段 (SanGuoShaPlayer p)
	{
		//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合判定阶段"));
	}
	public void 当玩家进入回合摸牌阶段 (SanGuoShaPlayer p)
	{
		//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合摸牌阶段"));
		int n摸牌数量 = 2;	// 周瑜或鲁肃其他可能多摸牌的武将
		让玩家摸牌 (p, n摸牌数量);
	}
	public void 让玩家摸牌 (SanGuoShaPlayer p, int n摸牌数量)
	{
		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p.getName ());
		for (int i=0; i<n摸牌数量; i++)
		{
			if (deck.size () == 0)
			{
				deck.addAll (recycle_deck);
				recycle_deck.clear ();
				洗牌 ();
			}
			player_cards.add ((SanGuoShaCard)deck.remove (0));
		}
	}
	public void 当玩家进入回合出牌阶段 (SanGuoShaPlayer p)
	{
		//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合出牌阶段"));
		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p.getName ());
		if (player_cards == null)
		{
			player_cards = new ArrayList<SanGuoShaCard> ();
			players_cards.put (p.getName (), player_cards);
		}
		boolean isOver = false;
		while (! isOver)
		{
			Dialog dlg = new Dialog (this,
					bot, bot.dialogs,
					"你的回合出牌阶段, 请出牌. 当前手牌: " + GenerateCardsInfoTo (p.getName ()) + ". 回答 " + Colors.REVERSE + "结束" + Colors.REVERSE + " 或者 " + Colors.REVERSE + "Over" + Colors.REVERSE + " 结束出牌" +
						"",
					true, p.getName (),
					channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
			dlg.showUsage = false;
			dlg.timeout_second = 5 * player_cards.size () + 10;	// 每张牌 5 秒钟的出牌时间，外加应对 IRC 延时的 10 秒钟。
			//for (Object o : participants)	// 通告其他人
			//{
			//	SanGuoShaPlayer p2 = (SanGuoShaPlayer)o;
			//	if (p != p2)
			//		bot.SendMessage (null, p2.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p2.getName () + " 的回合开始，请等他/她出牌…");
			//}
			// 三国杀玩家人数可能很多，在 IRC 里以私信方式通知是问题：服务器一般限制连续发送消息的次数和频率。所以，改发到频道里…
			bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p.getName () + " 的回合开始，请等他/她出牌…");

			Map<String, Object> participantAnswers = null;
			try
			{
				participantAnswers = bot.executor.submit (dlg).get ();
				String answer = (String)participantAnswers.get (p.getName ());
				if (StringUtils.isEmpty (answer))
				{
					break;
				}
				else if (StringUtils.equalsAnyIgnoreCase (answer, "结束", "Over"))
				{
					isOver = true;
					break;
				}
				else if ((StringUtils.equalsIgnoreCase (answer, "掀桌子") || StringUtils.equalsIgnoreCase (answer, "不玩了")))
					throw new RuntimeException (Colors.BOLD + p.getName () + Colors.NORMAL + " " + Colors.RED + answer + Colors.NORMAL);
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}
	}
	public void 当玩家进入回合弃牌阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 的回合弃牌阶段开始，请等他/她弃牌…"));
		让玩家弃牌 (p);
	}
	/**
	 * 让玩家弃置手牌
	 * @param p 玩家
	 * @param n需要弃置的牌数量
	 * @param n能留的手牌数量
	 */
	public void 让玩家弃牌 (SanGuoShaPlayer p, int n需要弃置的牌数量, int n能留的手牌数量)
	{
		int n需要弃置的总牌数 = 0;
		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p.getName ());
		if (n需要弃置的牌数量 == 0)
		{
			if (n能留的手牌数量 == 0)
			{
				throw new RuntimeException ("让玩家弃牌时，“需要弃置的牌数量”和“能留的手牌数量”不能全是 0");
			}
			if (player_cards.size () > n能留的手牌数量)
				n需要弃置的总牌数 = player_cards.size () - n能留的手牌数量;
		}
		else
			n需要弃置的总牌数 = n需要弃置的牌数量;

		if (n需要弃置的总牌数 == 0)
			return;

		List<SanGuoShaCard> list要弃置的牌 = new ArrayList<SanGuoShaCard> ();
		//
		while (n需要弃置的总牌数 > list要弃置的牌.size ())	// 做成循环，这样，可以跟三国杀官网一样：通过多次弃牌完成弃牌的动作，这对手牌太多时非常有帮助。
		{
			Dialog dlg = new Dialog (this,	// TODO: 看看需不需要单独用一个 DialogUser 子类来处理弃牌校验
					bot, bot.dialogs,
					"你的回合弃牌阶段, 请弃置 " + (n需要弃置的总牌数 - list要弃置的牌.size ()) + " 张手牌. 当前手牌: " + GenerateCardsInfoTo (p.getName ()) + ". 请回答牌的序号进行弃牌，多张牌用空格隔开，如：回答“4 8 1”将会弃置第 1 张、 第 4 张和第 8 张牌" +
						"",
					true, p.getName (),
					channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
			dlg.showUsage = false;
			dlg.timeout_second = 5 * (n需要弃置的总牌数 - list要弃置的牌.size ()) + 10;	// 时间上，要控制好，防止有人故意拖延时间。

			//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p.getName () + " 的回合开始，请等他/她出牌…");

			Map<String, Object> participantAnswers = null;
			try
			{
				participantAnswers = bot.executor.submit (dlg).get ();
				String answer = (String)participantAnswers.get (p.getName ());
				if (StringUtils.isEmpty (answer))
				{
					// TODO
					// 这里要自动选牌扔出
					break;
				}
				else if ((StringUtils.equalsIgnoreCase (answer, "掀桌子") || StringUtils.equalsIgnoreCase (answer, "不玩了")))
					throw new RuntimeException (Colors.BOLD + p.getName () + Colors.NORMAL + " " + Colors.RED + answer + Colors.NORMAL);

				Set<SanGuoShaCard> setCardsToDrop = new HashSet<SanGuoShaCard> ();
				String[] arrayCardNOs = answer.split (" +");
				for (int i=0; i<arrayCardNOs.length; i++)
				{
					String sInputedCardNO = arrayCardNOs[i];
					try
					{
						int nNumber = Integer.parseInt (sInputedCardNO);
						if (nNumber<1 || nNumber>player_cards.size ())
						{
							bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("你所出的牌序号 " + sInputedCardNO + " 无效。牌序号必须大于等于 1，且必须小于等于手牌数量。"));
							continue;
						}
						setCardsToDrop.add (player_cards.get (nNumber - 1));
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace ();
						bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("你输入的 " + sInputedCardNO + " 不是数值。牌序号"));
						continue;
					}
				}

				if (setCardsToDrop.size () == 0)
				{
					continue;
				}

				list要弃置的牌.addAll (setCardsToDrop);	// 不断累积要弃的牌，等到全部弃完，一次性通知出去
				player_cards.removeAll (setCardsToDrop);	// 从手牌中移除
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}

		recycle_deck.addAll (list要弃置的牌);	// 放入弃牌堆
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 弃置了 " + list要弃置的牌.size () + " 张牌： " + GenerateCardsInfoTo (list要弃置的牌)));
	}
	public void 让玩家弃牌 (SanGuoShaPlayer p)
	{
		int hp = (int)mapPlayerState.get (p.getName () + ".hp");
		让玩家弃牌 (p, 0, hp);
	}
	public void 当玩家进入回合结束阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合结束"));
	}

	// 1v1 可能会触发该事件
	public void 当玩家使用武将牌时 ()
	{

	}

	// 当拥有武将技能后
	public void 当拥有武将技能后 ()
	{

	}

	public void 当玩家使用装备牌时 ()
	{

	}

	public void 当玩家使用XX牌时 ()
	{

	}

	public void 牌生效前 ()
	{

	}

	public void 牌生效后 ()
	{

	}


	/**
	 * 收到用户的输入，判断用户输入是否正确有效。如果无效，则提示用户；如果有效，则触发用户出牌的事件。
	 */
	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{

		if (stage == GAME_STAGE_战斗中)
		{
			//
		}
		else
		{

		}
		return true;
	}

	/**
	 * 将用户输入的字符串，解析成游戏引擎可识别/可处理的数据格式。
	 * 这里只解析，不判断输入是正确还是错误。
	 * @param ch
	 * @param n
	 * @param u
	 * @param host
	 * @param answer
	 * @return
	 */
	public Map<String, Object> ParseDialogAnswer (String ch, String n, String u, String host, String answer)
	{
		Map<String, Object> mapAnswer = new HashMap<String, Object> ();
		String[] arrayAnswer = answer.split (" +");
		String sCardIndex = null;
		String sTargets = null;	// target1,target2,target3


		if (arrayAnswer.length >= 1)
			sCardIndex = arrayAnswer[0];	// 牌.修饰牌 （如 '杀.酒'，按正常的做法，诸如：酒、钴锭刀、火杀中的“酒”应该分开出，当做“使用了一张牌”，但现阶段，单独打出“酒”并没有触发任何武将的技能，所以可以合在一起)
		if (arrayAnswer.length >= 2)
			sTargets = arrayAnswer[1];	// target1,target2,target3

		if (StringUtils.isNotEmpty (sTargets))
		{
			String[] arrayTargets = sTargets.split (",");
		}

		return mapAnswer;
	}
	public boolean ValidateAnswer_战斗阶段 (String ch, String n, String u, String host, String answer)
	{
		SanGuoShaCard c = null;
		if (1==1)	// 如果是自己的回合（确切的说，是在出牌的一轮中，因为会遇到“自己出锦囊牌、别人无懈可击、自己要再反无懈可击”的情况），则不能打出被动牌。
		{
			//
			if (! (c instanceof I主动牌))
			{
				throw new IllegalArgumentException ("打出牌不是主动牌。发起牌时，打出的牌必须是主动牌");
				//throw new IllegalArgumentException ("发起人不是自己，不能打出主动牌");
				//return false;
			}
		}
		else if (2==2)	// 如果不是自己的回合
		{
			//
			if (! (c instanceof I被动牌))
			{
				throw new IllegalArgumentException ("打出的牌必须是被动牌");
				//throw new IllegalArgumentException ("发起人不是自己，不能打出主动牌");
				//return false;
			}
		}
		return false;
	}


	// 不含武将牌、身份牌的标准包卡牌
	public static final SanGuoShaCard[] 三国杀标准包游戏牌 =
	{
		//new 万箭齐发 ("♥", "A", 1),	// A
		//new 桃园结义 ("♥", "A", 1),	// A
		new 闪       ("♥", "2", 2),
		new 闪       ("♥", "2", 2),
		new 桃       ("♥", "3", 3),
		//new 五谷丰登 ("♥", "3", 3),
		new 桃       ("♥", "4", 4),
		//new 五谷丰登 ("♥", "4", 4),
		//new 麒麟弓   ("♥", "5", 5),	// 攻击范围=5
		//new 赤兔     ("♥", "5", 5),	// -1 马
		new 桃       ("♥", "6", 6),
		//new 乐不思蜀 ("♥", "6", 6),
		new 桃       ("♥", "7", 7),
		//new 无中生有 ("♥", "7", 7),
		new 桃       ("♥", "8", 8),
		//new 无中生有 ("♥", "8", 8),
		new 桃       ("♥", "9", 9),
		//new 无中生有 ("♥", "9", 9),
		new 杀       ("♥", "10", 10),
		new 杀       ("♥", "10", 10),
		new 杀       ("♥", "J", 11),	// J
		//new 无中生有 ("♥", "J", 11),	// J
		new 桃       ("♥", "Q", 12),	// Q
		//new 过河拆桥 ("♥", "Q", 12),	// Q
		new 闪       ("♥", "K", 13),	// K
		//new 爪黄飞电 ("♥", "K", 13),	// K, +1 马

		//new 诸葛连弩 ("♦", "A", 1),	// A, 攻击范围=1
		//new 决斗     ("♦", "A", 1),	// A
		new 闪       ("♦", "2", 2),
		new 闪       ("♦", "2", 2),
		new 闪       ("♦", "3", 3),
		//new 顺手牵羊 ("♦", "3", 3),
		new 闪       ("♦", "4", 4),
		//new 顺手牵羊 ("♦", "4", 4),
		new 闪       ("♦", "5", 5),
		//new 贯石斧   ("♦", "5", 5),
		new 杀       ("♦", "6", 6),
		new 闪       ("♦", "6", 6),
		new 杀       ("♦", "7", 7),
		new 闪       ("♦", "7", 7),
		new 杀       ("♦", "8", 8),
		new 闪       ("♦", "8", 8),
		new 杀       ("♦", "9", 9),
		new 闪       ("♦", "9", 9),
		new 杀       ("♦", "10", 10),
		new 闪       ("♦", "10", 10),
		new 闪       ("♦", "J", 11),	// J
		new 闪       ("♦", "J", 11),	// J
		new 桃       ("♦", "Q", 12),	// Q
		//new 方天画戟 ("♦", "Q", 12),	// Q, 攻击范围=4
		new 杀       ("♦", "K", 13),	// K
		//new 紫骍     ("♦", "K", 13),	// K, -1 马

		//new 闪电     ("♠", "A", 1),
		//new 决斗     ("♠", "A", 1),
		//new 八卦阵   ("♠", "2", 2),
		//new 雌雄双股剑("♠", "2", 2),	// 攻击范围=2
		//new 过河拆桥 ("♠", "3", 3),
		//new 顺手牵羊 ("♠", "3", 3),
		//new 过河拆桥 ("♠", "4", 4),
		//new 顺手牵羊 ("♠", "4", 4),
		//new 青龙偃月刀("♠", "5", 5),	// 攻击范围=3
		//new 绝影     ("♠", "5", 5),	// +1 马
		//new 乐不思蜀 ("♠", "6", 6),
		//new 青釭剑   ("♠", "6", 6),	// 攻击范围=2
		new 杀       ("♠", "7", 7),
		//new 南蛮入侵 ("♠", "7", 7),
		new 杀       ("♠", "8", 8),
		new 杀       ("♠", "8", 8),
		new 杀       ("♠", "9", 9),
		new 杀       ("♠", "9", 9),
		new 杀       ("♠", "10", 10),
		new 杀       ("♠", "10", 10),
		//new 无懈可击 ("♠", "J", 11),	// J
		//new 顺手牵羊 ("♠", "J", 11),	// J
		//new 丈八蛇矛 ("♠", "Q", 12),	// Q, 攻击范围=3
		//new 过河拆桥 ("♠", "Q", 12),	// Q
		//new 大宛     ("♠", "K", 13),	// K, -1 马
		//new 南蛮入侵 ("♠", "K", 13),	// K

		//new 诸葛连弩 ("♣", "A", 1),	// A
		//new 决斗     ("♣", "A", 1),	// A
		new 杀       ("♣", "2", 2),
		//new 八卦阵   ("♣", "2", 2),
		new 杀       ("♣", "3", 3),
		//new 过河拆桥 ("♣", "3", 3),
		new 杀       ("♣", "4", 4),
		//new 过河拆桥 ("♣", "4", 4),
		new 杀       ("♣", "5", 5),
		//new 的卢     ("♣", "5", 5),	// +1 马
		new 杀       ("♣", "6", 6),
		//new 乐不思蜀 ("♣", "6", 6),
		new 杀       ("♣", "7", 7),
		//new 南蛮入侵 ("♣", "7", 7),
		new 杀       ("♣", "8", 8),
		new 杀       ("♣", "8", 8),
		new 杀       ("♣", "9", 9),
		new 杀       ("♣", "9", 9),
		new 杀       ("♣", "10", 10),
		new 杀       ("♣", "10", 10),
		new 杀       ("♣", "J", 11),	// J
		new 杀       ("♣", "J", 11),	// J
		//new 借刀杀人 ("♣", "Q", 12),	// Q
		//new 无懈可击 ("♣", "Q", 12),	// Q
		//new 借刀杀人 ("♣", "K", 13),	// K
		//new 无懈可击 ("♣", "K", 13),	// K
	};
}
