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

	public enum 国家势力
	{
		魏,
		蜀,
		吴,
		群,
		野,	// 如果“野了”，则不与任何玩家属于同一个国家，包括其他已经“野”了的人（这与身份局的双内是类似的，不过，貌似，目前国战最多 8 人，出现不了两个“野”的情况）。
	}
	//boolean isAlone = false;	// 是否“野”了。判断是否某个国家时，需要首先判断“是否野了”，

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

	/**
	 * 弃牌阶段，可以通过几次弃牌操作进行弃牌。必须在 $弃牌操作次数最大值 次内把牌弃完，否则系统自动帮玩家弃牌（扔最前面的）。
	 */
	public static final int 弃牌操作次数最大值 = 4;

	public enum PLAY_CARD_STAGE
	{
		出牌开始,
		出牌生效,
	}

	public enum 响应类型
	{
		响应攻击牌,
		响应求桃,
		响应蛊惑技能,
		响应护驾技能,
		响应激将技能,
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

	@Override
	public void run ()
	{
		threadID = Thread.currentThread ().getId ();
		try
		{
			if (participants.size ()<2 || participants.size ()>10)
				throw new IllegalArgumentException (name + " 游戏人数需要 2 - 10 人，而本次游戏人数为 " + participants.size ());

			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("开始。"));

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
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束。"));
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("异常: " + e));
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
			sb.append (card);

			sb.append (" ");
		}
		return sb;
	}
	StringBuilder GenerateCardsInfoTo (List<SanGuoShaCard> cards)
	{
		return GenerateCardsInfoTo (cards, null);
	}

	void 开战 ()
	{
		stage = GAME_STAGE_战斗中;
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

	void 设置玩家存活状态 (SanGuoShaPlayer p, boolean bAlive)
	{
		mapPlayerState.put (p.getName () + ".alive", bAlive);
		mapPlayerState.put (p.getName () + ".dead", ! bAlive);
	}
	boolean 获取玩家存活状态 (SanGuoShaPlayer p)
	{
		if (mapPlayerState.get (p.getName () + ".alive") == null)
			return true;
		return (boolean) mapPlayerState.get (p.getName () + ".alive");
	}

	void 设置玩家生命值 (SanGuoShaPlayer p, int hp)
	{
		mapPlayerState.put (p.getName () + ".hp", hp);
	}
	void 设置玩家生命值上限 (SanGuoShaPlayer p, int max_hp)
	{
		mapPlayerState.put (p.getName () + ".hp.max", max_hp);
	}
	int 获取玩家生命值 (SanGuoShaPlayer p)
	{
		int hp = (int)mapPlayerState.get (p.getName () + ".hp");
		return hp;
	}
	int 获取玩家生命值上限 (SanGuoShaPlayer p)
	{
		int max_hp = (int)mapPlayerState.get (p.getName () + ".hp.max");
		return max_hp;
	}

	String 获取玩家生命值信息 (SanGuoShaPlayer p)
	{
		StringBuilder sb = new StringBuilder ();
		sb.append ("[");
		sb.append (获取玩家生命值 (p));
		sb.append ("/");
		sb.append (获取玩家生命值上限 (p));
		sb.append ("|");
		if (获取玩家生命值 (p) > 0)
		{
			sb.append (Colors.GREEN);
			for (int i=0; i<获取玩家生命值 (p); i++)
			{
				sb.append ("=");
			}
			sb.append (Colors.NORMAL);
		}
		if (获取玩家生命值上限 (p) - 获取玩家生命值 (p) > 0)
		{
			sb.append (ANSIEscapeTool.COLOR_DARK_RED);
			for (int i=0; i<获取玩家生命值上限 (p) - 获取玩家生命值 (p); i++)
			{
				sb.append ("_");
			}
			sb.append (Colors.NORMAL);
		}
		sb.append ("]");
		return sb.toString ();
	}

	/**
	 * 更改玩家生命值。这里仅仅做简单的数字运算，不做伤害判断。如果掉血，则等于三国杀官方的“体力流失”概念。伤害的判断，务必在上层封装的函数中处理。
	 * @param p
	 * @param nDelta
	 * @return
	 */
	private boolean 更改玩家生命值 (SanGuoShaPlayer p, int nDelta)
	{
		int hp = 获取玩家生命值 (p);
		if (nDelta < 0)
		{
			hp += nDelta;
			mapPlayerState.put (p.getName () + ".hp", hp);
			return true;
		}
		else
		{
			int max_hp = 获取玩家生命值上限 (p);
			if ((hp >= max_hp) || (hp+nDelta) >= max_hp)
				return false;
			hp += nDelta;
			mapPlayerState.put (p.getName () + ".hp", hp);
			return true;
		}
	}
	boolean 增加玩家生命值1 (SanGuoShaPlayer p)
	{
		return 更改玩家生命值 (p, 1);
	}
	boolean 减少玩家生命值1 (SanGuoShaPlayer p, boolean b是否触发受到伤害事件)
	{
		if (更改玩家生命值 (p, -1))
		{
			while (获取玩家生命值 (p) <= 0)
			{
				boolean 没人救 = true;
				try
				{
					SanGuoShaPlayRound round = new SanGuoShaPlayRound (响应类型.响应求桃, p);
					Future<?> future = bot.executor.submit (round);	// 之所以不用 Dialog 而用单独 SanGuoShaPlayRound 来询问目标玩家的响应，是因为：目标玩家的个数可能不是一个，用 SanGuoShaPlayRound 有可能做到并行询问…
					Object responses = future.get ();
				}
				catch (InterruptedException | ExecutionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	// 等待
				if (没人救)
				{
					当玩家阵亡时 (p);
					break;
				}
			}
			if (b是否触发受到伤害事件 && 获取玩家存活状态(p))
			{
				当玩家受到伤害时 (p, 1);
			}
			return true;
		}
		else
			return false;
	}
	boolean 减少玩家生命值1 (SanGuoShaPlayer p)
	{
		return 减少玩家生命值1 (p, true);
	}

	void 当玩家受到伤害时 (SanGuoShaPlayer p, int nHurt)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 受到 " + nHurt + " 点伤害。" + 获取玩家生命值信息 (p)));
		bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("你受到 " + nHurt + " 点伤害。" + 获取玩家生命值信息 (p)));
	}

	void 当玩家生命值恢复时 (SanGuoShaPlayer p, int nRecovery)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 恢复了 " + nRecovery + " 点体力。" + 获取玩家生命值信息 (p)));
		bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("你恢复了 " + nRecovery + " 点体力。" + 获取玩家生命值信息 (p)));
	}

	void 当玩家进入回合开始阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合开始"));
	}
	void 当玩家进入回合判定阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合判定阶段"));
	}
	void 当玩家进入回合摸牌阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合摸牌阶段"));
		int n摸牌数量 = 2;	// 周瑜或鲁肃其他可能多摸牌的武将
		让玩家摸牌 (p, n摸牌数量);
	}
	void 让玩家摸牌 (SanGuoShaPlayer p, int n摸牌数量)
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
	void 当玩家进入回合出牌阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合出牌阶段，请等他/她出牌…"));
		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p.getName ());
		if (player_cards == null)
		{
			player_cards = new ArrayList<SanGuoShaCard> ();
			players_cards.put (p.getName (), player_cards);
		}
		boolean isOver = false;
		int n出牌阶段出杀次数 = 0;
		while (! isOver)
		{
			Dialog dlg = new Dialog (this,
					bot, bot.dialogs,
					"你的回合出牌阶段, 请出牌. 当前手牌: " + GenerateCardsInfoTo (p.getName ()) + ". 回答 " + Colors.REVERSE + "结束" + Colors.REVERSE + " 或者 " + Colors.REVERSE + "Over" + Colors.REVERSE + " 结束出牌" +
						"",
					true, p.getName (),
					channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params, PLAY_TURN_STAGE_玩家回合阶段.出牌);
			dlg.showUsage = false;
			dlg.timeout_second = 5 * player_cards.size () + 10;	// 每张牌 5 秒钟的出牌时间，外加应对 IRC 延时的 10 秒钟。
			//for (Object o : participants)	// 通告其他人
			//{
			//	SanGuoShaPlayer p2 = (SanGuoShaPlayer)o;
			//	if (p != p2)
			//		bot.SendMessage (null, p2.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p2.getName () + " 的回合开始，请等他/她出牌…");
			//}
			// 三国杀玩家人数可能很多，在 IRC 里以私信方式通知是问题：服务器一般限制连续发送消息的次数和频率。所以，改发到频道里…
			//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p.getName () + " 的回合开始，请等他/她出牌…");

			Map<String, Object> participantAnswers = null;
			try
			{
				participantAnswers = bot.executor.submit (dlg).get ();
				String answer = (String)participantAnswers.get (p.getName ());
				if (StringUtils.isEmpty (answer) || StringUtils.equalsAnyIgnoreCase (answer, "结束", "Over"))
				{
					isOver = true;
					break;
				}
				else if ((StringUtils.equalsAnyIgnoreCase (answer, "掀桌子", "不玩了")))
					throw new RuntimeException (Colors.BOLD + p.getName () + Colors.NORMAL + " " + Colors.RED + answer + Colors.NORMAL);


				Map<String, Object> mapInitiation = ParseUserInput (p.getName (), answer);
				//if (! (boolean)mapResponse.get ("ParseResult"))	// ValidateAnswer 之后，剩下的要你管给都是有效格式的回答了
				//{
				//	throw new IllegalArgumentException ((String)mapResponse.get ("Message"));
				//	//return false;
				//}

				SanGuoShaCard c = (SanGuoShaCard)mapInitiation.get ("Card");
				SanGuoShaPlayer target = (SanGuoShaPlayer)mapInitiation.get ("Target");
				List<SanGuoShaPlayer> listTargets = (List<SanGuoShaPlayer>)mapInitiation.get ("Targets");
				if (c instanceof 杀)
				{
					// 检查杀
					if (target == null)
					{
						bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("“杀”牌必须指定目标（你要“杀”谁？）"));
						continue;
					}
					if (n出牌阶段出杀次数 >= 1)
					{	// 已经出过一次杀…
						bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("“杀”牌在出牌阶段只能出一次。"));
						continue;
					}
					n出牌阶段出杀次数 ++;
					SanGuoShaPlayRound round = new SanGuoShaPlayRound (响应类型.响应攻击牌, p, c, listTargets);
					Future<?> future = bot.executor.submit (round);	// 之所以不用 Dialog 而用单独 SanGuoShaPlayRound 来询问目标玩家的响应，是因为：目标玩家的个数可能不是一个，用 SanGuoShaPlayRound 有可能做到并行询问…
					Object responses = future.get ();	// 等待
				}
				else if (c instanceof 桃)
				{
					int hp = 获取玩家生命值 (p);
					int max_hp = 获取玩家生命值上限 (p);
					if (hp >= max_hp)
					{
						bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("“桃”牌只能受伤时使用。" + 获取玩家生命值信息 (p)));
						continue;
					}

					当玩家打出牌时 (p, c, true);
				}
				else if (c instanceof 闪)	// 其实，在 ValidateAnswer 之后，不会出现出闪的情况…
				{
					bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("“闪”牌不是主动牌，不能主动打出。"));
					continue;
				}
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * 玩家回合阶段（不一定非得是出牌阶段，比如夏侯渊的神速技能会造成“杀”效果）。单单一轮出牌
	 * @author liuyan
	 *
	 */
	class SanGuoShaPlayRound implements DialogUser, Callable<Object>
	{
		Object[] args = null;
		public SanGuoShaPlayRound (Object... args)
		{
			this.args = args;
		}
		@Override
		public Object call ()
		{
			int c = 0;
			响应类型 responseType = (响应类型) args[c++];
			SanGuoShaPlayer initiator = (SanGuoShaPlayer)args[c++];

			switch (responseType)
			{
				case 响应攻击牌:
					SanGuoShaCard initialCard = (SanGuoShaCard)args[c++];
					当玩家打出牌时 (initiator, initialCard);
					if (initialCard instanceof 杀)
					{
						List<SanGuoShaPlayer> listTargets = (List<SanGuoShaPlayer>) args[c++];

						bot.SendMessage (null, initiator.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("你对 " + listTargets + " 打出了杀，等待对方出抵御牌…"));
						bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (initiator.getName () + " 对 " + listTargets + " 打出了杀，等待受攻击方出抵御牌…"));
						for (int i=0; i<listTargets.size (); i++)
						{
							SanGuoShaPlayer responder = listTargets.get (i);
							String sPlayerName = responder.getName ();
							List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (sPlayerName);
							// 挨个询问是否出闪
							Dialog dlg = new Dialog (this,
									bot, bot.dialogs,
									initiator.getName () + " 对你打出了" + initialCard + "，请打出一张" + "闪" + "来抵御该攻击。 请回答牌的序号出牌。 若不出牌，回答 '过' 或 'p' 或 'n' 或 'g' 或 'over' " +
										"",
									true, sPlayerName,
									channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params, 响应类型.响应攻击牌, initialCard);
							dlg.showUsage = false;
							dlg.timeout_second = 5 * player_cards.size() + 10;	// 时间上，要控制好，防止有人故意拖延时间。

							//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p.getName () + " 的回合开始，请等他/她出牌…");

							Map<String, Object> participantAnswers = null;
							try
							{
								participantAnswers = bot.executor.submit (dlg).get ();
								String answer = (String)participantAnswers.get (sPlayerName);
								if (StringUtils.isEmpty (answer) || StringUtils.equalsAnyIgnoreCase (answer, "过", "p", "n", "g", "over"))
								{
									// 无抵消牌可出，结算扣血（除非有武将技能、装备技能）
									减少玩家生命值1 (responder);
								}
								else if ((StringUtils.equalsAnyIgnoreCase (answer, "掀桌子", "不玩了")))
									throw new RuntimeException (Colors.BOLD + sPlayerName + Colors.NORMAL + " " + Colors.RED + answer + Colors.NORMAL);
								else
								{
									Map<String, Object> mapResponse;
									mapResponse = ParseUserInput (responder.getName (), answer);
									SanGuoShaCard respondedCard = (SanGuoShaCard) mapResponse.get ("Card");
									当玩家打出牌时 (responder, respondedCard, true);
								}

							}
							catch (InterruptedException | ExecutionException e)
							{
								e.printStackTrace();
							}
						}
					}
					//else if (initialCard instanceof 万箭齐发)
					{

					}
					break;
				case 响应求桃:
						int iPlayer = participants.indexOf (initiator);	// 濒死的玩家，是起始玩家，这一轮，从濒死玩家开始…
						assert (iPlayer >= 0);

						bot.SendMessage (null, initiator.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("你濒临死亡，开始求桃子…"));
						bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (initiator.getName () + " 濒临死亡，开始求桃子…"));
						for (int iTurn=iPlayer; iTurn!=iPlayer; iTurn++)
						{
							SanGuoShaPlayer responder = (SanGuoShaPlayer)participants.get (iTurn);
							String sPlayerName = responder.getName ();
							List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (sPlayerName);
							// 挨个询问是否出闪
							Dialog dlg = new Dialog (this,
									bot, bot.dialogs,
									initiator.getName () + " 向你求桃，若出桃子，请回答牌的序号出牌，出多张桃则用空格分开。" +
										"若不出桃，回答 '过' 或 'p' 或 'n' 或 'g' 或 'over' " +
										"",
									true, sPlayerName,
									channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params, 响应类型.响应求桃, initiator);
							dlg.showUsage = false;
							dlg.timeout_second = 5 * player_cards.size() + 10;	// 时间上，要控制好，防止有人故意拖延时间。

							//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p.getName () + " 的回合开始，请等他/她出牌…");

							Map<String, Object> participantAnswers = null;
							try
							{
								participantAnswers = bot.executor.submit (dlg).get ();
								String answer = (String)participantAnswers.get (sPlayerName);
								if (StringUtils.isEmpty (answer) || StringUtils.equalsAnyIgnoreCase (answer, "过", "p", "n", "g", "over"))
								{
									//// 无抵消牌可出，结算扣血（除非有武将技能、装备技能）
								}
								else if ((StringUtils.equalsAnyIgnoreCase (answer, "掀桌子", "不玩了")))
								{
									//throw new RuntimeException (Colors.BOLD + sPlayerName + Colors.NORMAL + " " + Colors.RED + answer + Colors.NORMAL);
								}
								else
								{
									Map<String, Object> mapResponse;
									mapResponse = ParseUserInput (responder.getName (), answer);
									//SanGuoShaCard respondedCard = (SanGuoShaCard) mapResponse.get ("Card");
									//当玩家打出牌时 (responder, respondedCard, true);
								}

							}
							catch (InterruptedException | ExecutionException e)
							{
								e.printStackTrace();
							}
						}
					break;
			}
			return null;
		}

		@Override
		public boolean ValidateAnswer (String ch, String n, String u, String host, String answer, Object... args)
		{
			if (StringUtils.equalsAnyIgnoreCase (answer, "掀桌子", "不玩了", "过", "p", "n", "g", "over"))
			{
				return true;
			}

			响应类型 responseType = (响应类型) args[0];
			SanGuoShaCard initialtorCard = (SanGuoShaCard)args[1];
			Map<String, Object> mapResponse;
			mapResponse = ParseUserInput (n, answer);
			boolean bParseResult = (boolean)mapResponse.get ("ParseResult");
			if (! bParseResult)
			{
				String sMsg = (String)mapResponse.get ("Message");
				bot.SendMessage (null, n, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (sMsg));
				return false;
			}
			SanGuoShaCard respondedCard = (SanGuoShaCard) mapResponse.get ("Card");
			switch (responseType)
			{
				case 响应攻击牌:
					I攻击牌 攻击牌 = (I攻击牌) initialtorCard;
					for (Class<? extends SanGuoShaCard> 攻击牌所需的抵御牌的牌类型 : 攻击牌.获取抵御牌类型 ())
					{
						if (攻击牌所需的抵御牌的牌类型.isInstance (respondedCard))
							return true;
					}
					bot.SendMessage (null, n, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("所出的牌无效，不能抵御攻击牌，重新出牌…"));
					break;
				case 响应求桃:
					if (! (respondedCard instanceof 桃))
					{
						bot.SendMessage (null, n, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("求“桃”求“桃”，你出的是 " + respondedCard));
						return false;
					}
					break;
				default:
					return false;
			}
			return false;
		}
	}

	Stack<SanGuoShaPlayRound> stackRounds = new Stack<SanGuoShaPlayRound> ();

	void 当玩家进入回合弃牌阶段 (SanGuoShaPlayer p)
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
	void 让玩家弃牌 (SanGuoShaPlayer p, int n需要弃置的牌数量, int n能留的手牌数量)
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

		int 弃牌操作次数 = 0;
		List<SanGuoShaCard> list要弃置的牌 = new ArrayList<SanGuoShaCard> ();
		//
		while (n需要弃置的总牌数 > list要弃置的牌.size ())	// 做成循环，这样，可以跟三国杀官网一样：通过多次弃牌完成弃牌的动作，这对手牌太多时非常有帮助。
		{
			if (弃牌操作次数 > 弃牌操作次数最大值)
			{
				bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("弃牌阶段操作了 " + 弃牌操作次数 + " 次还没弃完牌，系统不等了，帮你弃牌！"));
				break;
			}
			Dialog dlg = new Dialog (this,	// TODO: 看看需不需要单独用一个 DialogUser 子类来处理弃牌校验
					bot, bot.dialogs,
					"你的回合弃牌阶段, 请弃置 " + (n需要弃置的总牌数 - list要弃置的牌.size ()) + " 张手牌. 当前手牌: " + GenerateCardsInfoTo (p.getName ()) + ". 请回答牌的序号进行弃牌，多张牌用空格隔开，如：回答“4 8 1”将会弃置第 1 张、 第 4 张和第 8 张牌" +
						"",
					true, p.getName (),
					channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params, PLAY_TURN_STAGE_玩家回合阶段.弃牌);
			dlg.showUsage = false;
			dlg.timeout_second = 5 * (n需要弃置的总牌数 - list要弃置的牌.size ()) + 10;	// 时间上，要控制好，防止有人故意拖延时间。

			//bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, p.getName () + " 的回合开始，请等他/她出牌…");

			Map<String, Object> participantAnswers = null;
			try
			{
				participantAnswers = bot.executor.submit (dlg).get ();
				弃牌操作次数 ++;
				String answer = (String)participantAnswers.get (p.getName ());
				if (StringUtils.isEmpty (answer))
				{
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
						bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("你输入的 " + sInputedCardNO + " 不是数值。"));
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

		// 如果玩家没弃完牌，这里要自动选出牌来弃置…
		int n未完成的弃牌量 = n需要弃置的总牌数 - list要弃置的牌.size ();
		for (int i=0; i<n未完成的弃牌量; i++)
		{
			list要弃置的牌.add (player_cards.remove (0));
		}

		recycle_deck.addAll (list要弃置的牌);	// 放入弃牌堆
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 弃置了 " + list要弃置的牌.size () + " 张牌： " + GenerateCardsInfoTo (list要弃置的牌)));
	}
	void 让玩家弃牌 (SanGuoShaPlayer p)
	{
		int hp = 获取玩家生命值 (p);
		让玩家弃牌 (p, 0, hp);
	}
	void 当玩家进入回合结束阶段 (SanGuoShaPlayer p)
	{
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 回合结束"));
	}

	void 当玩家阵亡时 (SanGuoShaPlayer p)
	{
		设置玩家存活状态 (p, false);
		// 牌进入弃牌堆
	}

	// 1v1 可能会触发该事件
	void 当玩家使用武将牌时 ()
	{

	}

	// 当拥有武将技能后
	void 当拥有武将技能后 ()
	{

	}

	void 当玩家使用装备牌时 ()
	{

	}

	void 当玩家打出牌时 (List<SanGuoShaCard> player_cards, SanGuoShaCard card)
	{
		player_cards.remove (card);
		recycle_deck.add (card);
	}
	void 当玩家打出牌时 (List<SanGuoShaCard> player_cards, Collection<SanGuoShaCard> cards)
	{
		player_cards.removeAll (cards);
		recycle_deck.addAll (cards);
		cards.clear ();
	}

	void 当玩家打出牌时 (SanGuoShaPlayer p, SanGuoShaCard card, boolean bSendNotification)
	{
		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p.getName ());
		当玩家打出牌时 (player_cards, card);
		if (bSendNotification)
		{
			bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 打出了 1 张 " + card + ""));
			bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 打出了 1 张 " + card + ""));
		}
	}
	void 当玩家打出牌时 (SanGuoShaPlayer p, SanGuoShaCard card)
	{
		当玩家打出牌时 (p, card, false);
	}

	void 当玩家打出牌时 (SanGuoShaPlayer p, Collection<SanGuoShaCard> cards, boolean bSendNotification)
	{
		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p.getName ());
		当玩家打出牌时 (player_cards, cards);
		if (bSendNotification)
		{
			bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 打出了 " + cards.size () + " 张牌: " + cards + ""));
			bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (p.getName () + " 打出了 " + cards.size () + " 张牌: " + cards + ""));
		}
	}
	void 当玩家打出牌时 (SanGuoShaPlayer p, Collection<SanGuoShaCard> cards)
	{
		当玩家打出牌时 (p, cards, false);
	}

	void 牌生效前 ()
	{

	}

	void 牌生效后 ()
	{

	}


	/**
	 * 收到用户的输入，判断用户输入是否正确有效。如果无效，则提示用户；如果有效，则触发用户出牌的事件。
	 */
	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer, Object... args)
	{

		if (stage == GAME_STAGE_战斗中)
		{
			return ValidateAnswer_战斗阶段 (ch, n, u, host, answer, args);
		}
		else
		{

		}
		return true;
	}

	/**
	 * 在玩家出牌、回牌时，将玩家用户输入的字符串，解析成游戏引擎可识别/可处理的数据格式。
	 * 这里只做 (1)解析 (2)判断出的牌是否有效（是否在手里），不判断输入在逻辑上是正确还是错误。
	 * @param n
	 * @param answer 用户输入的字符串。这里的字符串，应该是排除掉类似“过”/“n”/“over”、“掀桌子”/“不玩了”之后的内容。
	 * <p>
	 * 出牌时输入格式：
	 * $牌序号1[<code>/</code>$牌序号2]...[.选项1[.其他选项]...] [目标1[,其他目标]...]<br/>
	 * 例如:
	 * <ul>
	 * 	<li><code>3</code> -  将第三张牌对自己打出。通常， 桃、酒、无中生有 等等这些不需要指定目标的牌需要这样出牌</li>
	 * 	<li><code>2 LiuYan2</code> - 将第二张牌对 <code>LiuYan2</code> 打出。通常，杀、决斗 需要这样出牌</li>
	 * 	<li><code>2 LiuYan2,LiuYan3,LiuYan4</code> - 将第二张牌对 <code>LiuYan2</code>、<code>LiuYan3</code>、<code>LiuYan4</code> 打出。通常，发动方天画戟武器技能 + 最后一张手牌杀 需要这样出牌</li>
	 * 	<li><code>2/3 LiuYan2</code> - 将第二张牌和第三张牌对 <code>LiuYan2</code> 打出。通常，
	 * 		<ul>
	 * 		<li>发动丈八蛇矛武器技能将两张手牌当做“杀”打出时</li>
	 * 		<li>袁绍武将将两张同花色的牌当做万箭齐发</li>
	 * 		<li>酒 + 杀时，因为目前的武将中，喝酒并不会触发任何技能，所以本游戏引擎可以不需要单独出酒，而是在这里和“杀”一起出，起到【修饰/修改】杀的伤害强度的目的</li>
	 * 		</ul>
	 * 		需要这样出牌
	 * 	</li>
	 * 	<li><code>2/3/6 LiuYan2</code> - 将第二张牌和第三张牌和第六张对 <code>LiuYan2</code> 打出。这可能是 酒 + 丈八蛇矛武器技能 + 任意两张牌当杀打出</li>
	 * </ul>
	 * </p>
	 * @return Map&lt;String, Object&gt; 的数据。该 Map 会包含以下 key
	 * <ul>
	 * 		<li><code>ParseResult</code> - boolean 类型。 true 表示解析和简单验证通过，false 则反之</li>
	 * 		<li><code>Message</code> - String 类型。当 <code>ParseResult</code> 为 false 时，才会有 <code>Message</code>，这个 <code>Message</code> 会给出原因。</li>
	 * 		<li><code>Card</code></li>
	 * 		<li><code>Targets</code> - List&lt;SanGuoShaPlayer&gt; 类型。多个目标</li>
	 * 		<li><code>Target</code> - SanGuoShaPlayer 类型。单个目标，或者，多个目标的第一个目标</li>
	 * </ul>
	 */
	public Map<String, Object> ParseUserInput (String n, String answer)
	{
		Map<String, Object> mapAnswer = new HashMap<String, Object> ();
		mapAnswer.put ("ParseResult", true);
		String[] arrayAnswerParts = answer.split (" +");
		String sCardsIndexes = null;
		String[] arrayCardsIndexes = null;
		String sTargets = null;	// target1,target2,target3

		////////////////////////////////////////
		// 解析 “卡牌” 信息的输入
		////////////////////////////////////////

		if (arrayAnswerParts.length >= 1)
		{
			sCardsIndexes = arrayAnswerParts[0];	// 牌.修饰牌 （如 '杀.酒'，按正常的做法，诸如：酒、钴锭刀、火杀中的“酒”应该分开出，当做“使用了一张牌”，但现阶段，单独打出“酒”并没有触发任何武将的技能，所以可以合在一起)
			arrayCardsIndexes = new String[1];
			arrayCardsIndexes[0] = sCardsIndexes;	// 三国杀入门 只能出一张牌，等以后实现标准包卡牌时再做处理

			//if (StringUtils.contains (sCardsIndexes, '/'))	// TODO 三国杀入门 不会有这种情况，等以后实现标准包卡牌时再做处理
			//{
			//	arrayCardsIndexes = sCardsIndexes.split ("/+");
			//}
		}
		else
		{
			mapAnswer.put ("ParseResult", false);
			mapAnswer.put ("Message", "回答不能为空");
			return mapAnswer;
		}

		List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (n);
		Set<SanGuoShaCard> setCardsResponded = new HashSet<SanGuoShaCard> ();
		for (int j=0; j<arrayCardsIndexes.length; j++)
		{
			String sInputedCardNO = arrayCardsIndexes[j];
			try
			{
				int nNumber = Integer.parseInt (sInputedCardNO);
				if (nNumber<1 || nNumber>player_cards.size ())
				{
					mapAnswer.put ("ParseResult", false);
					mapAnswer.put ("Message", "你所出的牌的序号 " + sInputedCardNO + " 无效。牌的序号必须大于等于 1，且必须小于等于手牌数量。");
					return mapAnswer;
					//break;
				}
				setCardsResponded.add (player_cards.get (nNumber - 1));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace ();
				mapAnswer.put ("ParseResult", false);
				mapAnswer.put ("Message", "你输入的 " + sInputedCardNO + " 不是数值。");
				return mapAnswer;
				//break;
			}
		}

		mapAnswer.put ("Cards", setCardsResponded);
		for (SanGuoShaCard card : setCardsResponded)
		{
			mapAnswer.put ("Card", card);	// 对于只出一张牌的情况，将该牌放到一个单独的 key 中
			break;
		}

		////////////////////////////////////////
		// 解析 “目标” 信息的输入（如果有的话）
		////////////////////////////////////////

		if (arrayAnswerParts.length >= 2)
			sTargets = arrayAnswerParts[1];	// target1,target2,target3

		if (StringUtils.isNotEmpty (sTargets))
		{
			String[] arrayTargets = sTargets.split ("[,;/]+");
			List<SanGuoShaPlayer> listTargets = new ArrayList<SanGuoShaPlayer> ();
			mapAnswer.put ("Targets", listTargets);
			for (String sPlayerName : arrayTargets)
			{
				if (StringUtils.equalsIgnoreCase (sPlayerName, n))
				{
					mapAnswer.put ("ParseResult", false);
					mapAnswer.put ("Message", "目标玩家不能指定自己。");
					return mapAnswer;
				}

				boolean bFound = false;
				for (int i=0; i<participants.size (); i++)
				{
					SanGuoShaPlayer p = (SanGuoShaPlayer)participants.get (i);
					if (StringUtils.equalsIgnoreCase (sPlayerName, p.getName ()))
					{
						bFound = true;
						listTargets.add (p);
						if (mapAnswer.get ("Target") == null)
							mapAnswer.put ("Target", p);
					}
				}

				if (! bFound)
				{
					mapAnswer.put ("ParseResult", false);
					mapAnswer.put ("Message", "你输入的目标玩家名【" + sPlayerName + "】无效： 在本局游戏中找不到该玩家名。");
					return mapAnswer;
				}
			}
		}

		return mapAnswer;
	}

	public boolean ValidateAnswer_战斗阶段 (String ch, String n, String u, String host, String answer, Object... args)
	{
		if (StringUtils.equalsAnyIgnoreCase (answer, "掀桌子", "不玩了", "Over", "结束"))
		{
			return true;
		}

		PLAY_TURN_STAGE_玩家回合阶段 玩家阶段 = null;
		if (args!=null && args.length >= 1)
			玩家阶段 = (PLAY_TURN_STAGE_玩家回合阶段)args[0];

		if (玩家阶段 == null)
			return false;

		switch (玩家阶段)
		{
			case 出牌:
				Map<String, Object> mapResponse = ParseUserInput (n, answer);
				if (! (boolean)mapResponse.get ("ParseResult"))
				{
					throw new IllegalArgumentException ((String)mapResponse.get ("Message"));
					//return false;
				}

				SanGuoShaCard c = (SanGuoShaCard)mapResponse.get ("Card");
				//if (currentTurnPlayer == player)	// 如果是自己的回合（确切的说，是在出牌的一轮中，因为会遇到“自己出锦囊牌、别人无懈可击、自己要再反无懈可击”的情况），则不能打出被动牌。
				{
					//
					if (! (c instanceof I主动牌))
						throw new IllegalArgumentException ("打出牌不是主动牌。发起牌时，打出的牌必须是主动牌");
				}
				//else	// if (2==2)	// 如果不是自己的回合
				//{
				//	//
				//	if (! (c instanceof I被动牌))
				//	{
				//		throw new IllegalArgumentException ("打出的牌必须是被动牌");
				//		//throw new IllegalArgumentException ("发起人不是自己，不能打出主动牌");
				//		//return false;
				//	}
				//}
				return true;
			case 弃牌:
				return true;	// 因为可以多次弃牌，所以交给弃牌阶段自己判断是否有效
				//break;
			default:
				break;
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
