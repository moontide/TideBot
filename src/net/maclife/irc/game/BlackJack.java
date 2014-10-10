package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class BlackJack extends CardGame
{
	@Override
	public void run ()
	{
		try
		{
			StringBuilder sb = null;	//new StringBuilder ();
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 开始…");

			// 洗牌
			if (mapGlobalOptions.containsKey ("ace-test"))
				InitAcesTestDeck ();
			else
				InitDeck ();
			//System.out.println (deck);
			//sb.append ("洗牌完毕 ");
			////sb.append ("\u0003,15");	// 白色背景，好让黑桃、梅花的字符的“黑色”显示出来
			//for (Map<String, Object> card : deck)
			//{
			//	//sb.append (card.get ("color"));
			//	sb.append (card.get ("suit"));
			//	sb.append (card.get ("rank"));
			//	sb.append (" ");
			//}
			////sb.append (Colors.NORMAL);
			//System.out.println (sb);
			//bot.SendMessage (channel, "", false, 1, "洗牌完毕");

			// 分暗牌
			deal ("暗牌: ", Dialog.MESSAGE_TARGET_MASK_PM);
			bot.SendMessage (channel, null, false, 1, "每人发了一张暗牌，已通过私信发送具体牌，请注意查看");

			// 分明牌
			deal ("明牌: ", Dialog.MESSAGE_TARGET_MASK_CHANNEL | Dialog.MESSAGE_TARGET_MASK_PM);

			// 开始
			List<String> liveParticipants = participants;
			List<String> standParticipants = new ArrayList<String> ();	// 停牌的玩家
			List<String> deadParticipants = new ArrayList<String> ();	// 爆牌的玩家
			while (true)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏被终止");

				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, Dialog.Type.单选, "要牌么？ ", true, liveParticipants, wannaCards_CandidateAnswers,	// 发票要么？ 毛片要么？
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				dlg.timeout_second = 30;
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				String answer;
				String value;
				boolean isAllDontWannaCards = true;
				for (int i=0; i<liveParticipants.size (); i++)
				{
					String p = participants.get (i);
					answer = (String)participantAnswers.get (p);
					value = dlg.GetCandidateAnswerValueByValueOrLabel (answer);
					if (isQuitGameAnswer(answer))
					{
						//isParticipantWannaQuit = true;
						break;
					}

					if (StringUtils.equalsIgnoreCase (value, "1"))
					{	// 要牌
						isAllDontWannaCards = false;
						deal (p, "", Dialog.MESSAGE_TARGET_MASK_PM | Dialog.MESSAGE_TARGET_MASK_CHANNEL);
						int nSum = CalculatePoints (p);
						if (nSum > BURST_POINT)
						{	// 爆牌 （死亡）
							deadParticipants.add (liveParticipants.remove (i));	i --;
						}
					}
					else if (StringUtils.equalsIgnoreCase (value, "2"))
					{
					}
					else if (StringUtils.equalsIgnoreCase (value, "T"))
					{
						standParticipants.add (liveParticipants.remove (i));	i --;
					}
				}

				// 结束条件: 只剩下一个 【“活着”的 + “停牌”的】，或者，所有人都不要牌了
				if ((liveParticipants.size () + standParticipants.size ()) <= 1  ||  isAllDontWannaCards)
					break;
			}

			sb = new StringBuilder ();
			// 活着的人的情况
			liveParticipants.addAll (standParticipants);
			PointsComparator comparator = new PointsComparator ();
			Collections.sort (liveParticipants, Collections.reverseOrder (comparator));
			GeneratePlayersCardsInfoTo (liveParticipants, "存活", sb, null, true);

			// 爆牌的人的情况
			Collections.sort (deadParticipants, comparator);
			GeneratePlayersCardsInfoTo (deadParticipants, "爆牌", sb, ANSIEscapeTool.COLOR_DARK_RED, true);

			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 结束。" + sb.toString ());
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", false, 1, name + " 游戏异常: " + e);
		}
		finally
		{
			games.remove (this);
		}
	}


	public static int BURST_POINT = 21;

	public static List<String[]> wannaCards_CandidateAnswers = new ArrayList<String[]> ();	// 候选答案
	static
	{
		wannaCards_CandidateAnswers.add (new String[]{"1", "哟哟哟"});
		wannaCards_CandidateAnswers.add (new String[]{"2", "不要"});
		wannaCards_CandidateAnswers.add (new String[]{"T", "停牌"});
	}

	int deck_number = 1;

	public BlackJack (LiuYanBot bot, List<Game> listGames, List<String> listParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (BURST_POINT + "点", bot, listGames, listParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);

		if (listParticipants.size () < 2)
		{
			listGames.remove (this);
			throw new IllegalArgumentException ("至少需要 2 人玩。在后面用 /p 参数指定其他玩家");
		}

		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");
		if (opt_max_response_lines_specified)
		{
			if (opt_max_response_lines < 1)
				opt_max_response_lines = 1;
			else if (opt_max_response_lines > 4)
				opt_max_response_lines = 4;

			deck_number = opt_max_response_lines;
		}
	}

	/**
	 * 初始化牌堆
	 */
	void InitDeck ()
	{
		for (int i=0; i<deck_number; i++)
		{
			for (int r=1; r<=13; r++)
			{
				for (int s=0; s<CARD_SUITS.length; s++)
				{
					AddCardToDeck (r, s);
				}
			}
		}
		Collections.shuffle (deck);
	}

	/**
	 * 初始化 A 测试牌堆。4 张 A + 红桃 2-K，只是为了测试 A 点数值计算而用
	 */
	void InitAcesTestDeck ()
	{
		for (int s=0; s<CARD_SUITS.length; s++)
		{
			AddCardToDeck (1, s);
		}
		for (int r=2; r<=13; r++)
		{
			AddCardToDeck (r, 2);
		}
		Collections.shuffle (deck);
	}

	/**
	 * 将一张牌加入到牌堆
	 * @param r 点数值 (1-13)
	 * @param s 花色 索引号 (0-3)
	 */
	void AddCardToDeck (int r, int s)
	{
		Map<String, Object> card = new HashMap<String, Object> ();
		card.put ("suit", CARD_SUITS[s]);	// 花色
		card.put ("rank", CARD_RANKS[r-1]);	// 大小
		if (r >=11 && r<=13)
			card.put ("point", 10);	// J Q K 点数值为 10
		else if (r==1)
			card.put ("point", 11);	// A 可以为 1 或者 11，玩家不必过多关注其取值，因为：为了简便， bot 只会取一个最接近 21 点又不爆点的最大数值
		else
			card.put ("point", r);	// 2-10 点数值

		if (CARD_SUITS[s]=='♣' || CARD_SUITS[s]=='♠')
		{
			//card.put ("color", Colors.BLACK);
			card.put ("color", "");
		}
		else if (CARD_SUITS[s]=='♦' || CARD_SUITS[s]=='♥')
			card.put ("color", Colors.RED);

		deck.add (card);
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		return true;
	}

	String getPointsKey (String p)
	{
		return p + ".points";
	}
	String getAlternativePointsKey (String p)
	{
		return p + ".alt.points";
	}
	String getFaceUpPointsKey (String p)
	{
		return p + ".faceup.points";
	}
	String getFaceUpAlternativePointsKey (String p)
	{
		return p + ".faceup.alt.points";
	}
	/**
	 * 给一个玩家发一张牌
	 * @param p 玩家名
	 * @param msg
	 * @param msgTarget
	 * @return 发给该玩家牌后的其所有的牌
	 */
	@SuppressWarnings ("unchecked")
	List<Map<String, Object>> deal (String p, String msg, int msgTarget)
	{
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
		if (player_cards == null)
		{
			player_cards = new ArrayList<Map<String, Object>> ();
			players_cards.put (p, player_cards);
		}

		Map<String, Object> card = deck.remove (0);
		player_cards.add (card);
		CalculatePoints(p);	// 所有牌的点数值
		CalculatePoints(p, false);	// 明牌的点数值

		StringBuilder sb = new StringBuilder ();
		for (int i=1; i<player_cards.size (); i++)
		{
			sb.append (card.get ("color"));
			sb.append (card.get ("suit"));
			sb.append (card.get ("rank"));
		}

		if ((msgTarget & Dialog.MESSAGE_TARGET_MASK_CHANNEL) > 0)
			bot.SendMessage (channel, p, true, 1, "[底牌] " + msg + GenerateCardsInfoTo(p, false) + Colors.NORMAL);

		if ((msgTarget & Dialog.MESSAGE_TARGET_MASK_PM) > 0)
			bot.SendMessage (null, p, false, 1, GenerateCardsInfoTo(p, true) + Colors.NORMAL);

		return player_cards;
	}

	void deal (String msg, int msgTarget)
	{
		for (int i=0; i<participants.size (); i++)
		{
			String p = participants.get (i);
			deal (p, msg, msgTarget);
		}
	}

	/**
	 * 总点数值比较器，用于对游戏结果排序
	 * @author liuyan
	 *
	 */
	class PointsComparator implements Comparator<String>
	{
		@Override
		public int compare (String o1, String o2)
		{
			String k1 = getPointsKey (o1);
			String k2 = getPointsKey (o2);
			int v1 = (int)players_cards.get (k1);
			int v2 = (int)players_cards.get (k2);
			return v1-v2;
		}
	}

	/**
	 * 计算玩家的牌的点数值
	 * @param player 玩家名
	 * @param includeHoleCard 是否包含暗牌
	 * @return 玩家的牌的点数值
	 */
	@SuppressWarnings ("unchecked")
	int CalculatePoints (String player, boolean includeHoleCard)
	{
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (player);
		int nBestPointsSum = 0;
		int iStart = includeHoleCard ? 0 : 1;
		int nAces = 0;	// A 的牌数
		for (int i=iStart; i<player_cards.size (); i++)
		{
			Map<String, Object> card = player_cards.get (i);
			if (((String)card.get ("rank")).equalsIgnoreCase ("A"))
				nAces ++;

			nBestPointsSum += (int)card.get ("point");
		}

		if (nAces > 0)
		{	// 如果牌里有 A 这种有两个点数值的变态牌，则生成所有可能的点数值列表，并选出最佳点数值
			boolean isBestPointsSumSettled = false;
			Set<Integer> setPossiblePoints = new HashSet<Integer> ();
			GenerateAlternativeAcesPointsRecursivelyTo (nBestPointsSum, nAces, setPossiblePoints);	// 生成所有可能的点数值集合

			List<Integer> listPossiblePoints = new ArrayList<Integer> ();
			listPossiblePoints.addAll (setPossiblePoints);
			Collections.sort (listPossiblePoints, Collections.reverseOrder ());	// 按点数值由大到小排列

			for (int i=0; i<listPossiblePoints.size (); i++)
			{
				Integer nSum = listPossiblePoints.get (i);
				if (nSum > BURST_POINT && (i != listPossiblePoints.size () - 1))
				{	// 如果爆点，并且不是最后一个可能的点数值，则取下一个小一点的点数值
					nBestPointsSum = listPossiblePoints.get (i+1);
				}
				else if (nSum <= BURST_POINT && ! isBestPointsSumSettled)
				{	// 如果没爆点，并且还没选择最佳点数值，则设置该点数值为最佳点数值，并结束循环
					nBestPointsSum = nSum;
					isBestPointsSumSettled = true;	// 不超过 21 点的最大值
					break;
				}
			}
			listPossiblePoints.remove ((Integer)nBestPointsSum);
			players_cards.put (includeHoleCard ? getAlternativePointsKey (player) : getFaceUpAlternativePointsKey (player), listPossiblePoints);
		}
		players_cards.put (includeHoleCard ? getPointsKey (player) : getFaceUpPointsKey (player), nBestPointsSum);

		return nBestPointsSum;
	}
	int CalculatePoints (String player)
	{
		return CalculatePoints (player, true);
	}
	/**
	 * 根据默认 A 的默认点数值 11 算出的 nSum、A 牌的数量，生成所有可能的点数值
	 * @param nSum 一个 A 为 11 点数值时的总点数值
	 * @param nAces A 牌的数量
	 * @param setPossiblePoints 可能的点数值 Set。注意：如果用 List，则一定有重复的点数值
	 */
	void GenerateAlternativeAcesPointsRecursivelyTo (int nSum, int nAces, Set<Integer> setPossiblePoints)
	{
		if (nAces <= 0)
			return;

		setPossiblePoints.add (nSum);	// 默认 A 点数值为 11
		setPossiblePoints.add (nSum - 10);	// A 点数值为 1: 11 - 10 = 1
		GenerateAlternativeAcesPointsRecursivelyTo (nSum, nAces-1, setPossiblePoints);	// 剩下的 A
		GenerateAlternativeAcesPointsRecursivelyTo (nSum-10, nAces-1, setPossiblePoints);	// 剩下的 A
	}

	/**
	 * 	生成单个玩家的牌的信息
	 * @param p
	 * @param sb_in
	 * @return
	 */
	@SuppressWarnings ("unchecked")
	StringBuilder GenerateCardsInfoTo (String p, StringBuilder sb_in, boolean includeHoleCard)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
		int iStart = includeHoleCard ? 0 : 1;
		for (int i=iStart; i<player_cards.size (); i++)
		{
			Map<String, Object> card = player_cards.get (i);
			sb.append ("" + card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL);
			sb.append (" ");
		}
		sb.append ("-- ");
		if (includeHoleCard)
			sb.append (players_cards.get (getPointsKey (p)));
		else
			sb.append (players_cards.get (getFaceUpPointsKey (p)));
		sb.append (" 点");
		if (players_cards.get (getAlternativePointsKey (p)) != null && includeHoleCard)
		{
			sb.append (", 其他点数值: ");
			sb.append (players_cards.get (getAlternativePointsKey (p)));
		}
		else if (players_cards.get (getFaceUpAlternativePointsKey (p)) != null && !includeHoleCard)
		{
			sb.append (", 其他点数值: ");
			sb.append (players_cards.get (getFaceUpAlternativePointsKey (p)));
		}

		sb.append ("。 ");
		return sb;
	}
	StringBuilder GenerateCardsInfoTo (String p, boolean includeHoleCard)
	{
		return GenerateCardsInfoTo (p, null, includeHoleCard);
	}

	/**
	 * 生成一些玩家的牌的信息，保存到 StringBuilder 中
	 * @param players 玩家列表
	 * @param listName 列表名
	 * @param sb_in 将信息保存到该 StringBuilder 里。如果为 null，则自动新建一个
	 * @param sIRCColorOfPlayerName 该列表的玩家名颜色
	 * @return
	 */
	StringBuilder GeneratePlayersCardsInfoTo (List<String>players, String listName, StringBuilder sb_in, String sIRCColorOfPlayerName, boolean includeHoleCard)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		if (players.size () == 0)
			return sb;

		sb.append (listName);
		sb.append (": ");
		for (String p : players)
		{
			if (sIRCColorOfPlayerName!=null)
				sb.append (sIRCColorOfPlayerName);
			sb.append (p);
			if (sIRCColorOfPlayerName!=null)
				sb.append (Colors.NORMAL);
			sb.append (":");

			GenerateCardsInfoTo (p, sb, includeHoleCard);
		}

		return sb;
	}
}
