package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class BlackJack extends Game
{
	public static int BURST_POINT = 21;
	public static final char[] CARD_SUITS =
		{
			'♣', '♦', '♥', '♠',
			//clubs (♣), diamonds (♦), hearts (♥) and spades (♠)
		};
	public static final String[] CARD_RANKS =
		{
			"A", "2", "3", "4", "5",
			"6", "7", "8", "9", "10",
			"K", "Q", "K",
		};

	public static final int MESSAGE_TYPE_MASK_PM     = 1;
	public static final int MESSAGE_TYPE_MASK_PUBLIC = 2;
	public static final int MESSAGE_TYPE_MASK_QUIET  = 0;

	public static List<String[]> wannaCards_CandidateAnswers = new ArrayList<String[]> ();	// 候选答案
	static
	{
		wannaCards_CandidateAnswers.add (new String[]{"1", "哟哟哟"});
		wannaCards_CandidateAnswers.add (new String[]{"2", "不要"});
		wannaCards_CandidateAnswers.add (new String[]{"T", "停牌"});
	}

	int deck_number = 1;
	List<Map<String, Object>> deck = new ArrayList<Map<String, Object>> ();
	Map<String, Object> players_cards = new HashMap<String, Object> ();

	public BlackJack (LiuYanBot bot, List<Game> listGames, List<String> listParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (BURST_POINT + "点", bot, listGames, listParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
	}

	void InitDeck ()
	{
		// clubs (♣), diamonds (♦), hearts (♥) and spades (♠),
		//Random rand = new SecureRandom ();
		for (int i=0; i<deck_number; i++)
		{
			for (int r=1; r<=13; r++)
			{
				for (int s=0; s<CARD_SUITS.length; s++)
				{
					Map<String, Object> card = new HashMap<String, Object> ();
					card.put ("suit", CARD_SUITS[s]);	// 花色
					card.put ("rank", CARD_RANKS[r-1]);	// 大小
					if (r >=11 && r<=13)
						card.put ("point", 10);	// J Q K 点数值为 10
					else
						card.put ("point", r);	// A 2-10 点数值，A 可以为 1 或者 11，由玩家决定。但为了 bot 只会取一个最接近 21 点可能性的数值

					if (CARD_SUITS[s]=='♣' || CARD_SUITS[s]=='♠')
					{
						//card.put ("color", Colors.BLACK);
						card.put ("color", "");
					}
					else if (CARD_SUITS[s]=='♦' || CARD_SUITS[s]=='♥')
						card.put ("color", Colors.RED);

					deck.add (card);
				}
			}
		}
		Collections.shuffle (deck);
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
	String getFaceUpPointsKey (String p)
	{
		return p + ".faceup.points";
	}
	@SuppressWarnings ("unchecked")
	List<Map<String, Object>> deal (String p, String msg, int msgType)
	{
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
		if (player_cards == null)
		{
			player_cards = new ArrayList<Map<String, Object>> ();
			players_cards.put (p, player_cards);
		}

		Map<String, Object> card = deck.remove (0);
		player_cards.add (card);
		players_cards.put (getPointsKey (p), CalculatePoints(p));	// 所有牌的点数值
		players_cards.put (getFaceUpPointsKey (p), CalculatePoints(p, false));	// 明牌的点数值

		StringBuilder sb = new StringBuilder ();
		for (int i=1; i<player_cards.size (); i++)
		{
			sb.append (card.get ("color"));
			sb.append (card.get ("suit"));
			sb.append (card.get ("rank"));
		}

		if ((msgType & MESSAGE_TYPE_MASK_PUBLIC) > 0)
			bot.SendMessage (channel, p, true, 1, msg + GenerateCardsInfoTo(p, false) + Colors.NORMAL);

		if ((msgType & MESSAGE_TYPE_MASK_PM) > 0)
			bot.SendMessage (null, p, false, 1, GenerateCardsInfoTo(p, true) + Colors.NORMAL);

		return player_cards;
	}

	void deal (String msg, int msgType)
	{
		for (int i=0; i<participants.size (); i++)
		{
			String p = participants.get (i);
			deal (p, msg, msgType);
		}
	}

	int CalculatePoints (List<Map<String, Object>> player_cards, boolean includeHoleCard)
	{
		int nSum = 0;
		int iStart = includeHoleCard ? 0 : 1;
		for (int i=iStart; i<player_cards.size (); i++)
		{
			Map<String, Object> card = player_cards.get (i);
			if (((String)card.get ("rank")).equalsIgnoreCase ("A"))
			{
				nSum += 11;	// A 默认取点值 11
				if (nSum > BURST_POINT)	// 如果总和超过了 21 点，则 A 改取点值 1
					nSum -= 10;
			}
			else
				nSum += (int)card.get ("point");
		}
		return nSum;
	}
	int CalculatePoints (List<Map<String, Object>> player_cards)
	{
		return CalculatePoints (player_cards, true);
	}

	/**
	 * 点数值比较器
	 * @author liuyan
	 *
	 */
	class PointsComparator implements Comparator<String>
	{
		boolean orderAscending = false;
		public PointsComparator (boolean order)
		{
			setOrder (order);
		}
		public void setOrder (boolean order)
		{
			orderAscending = order;
		}
		@Override
		public int compare (String o1, String o2)
		{
			String k1 = getPointsKey (o1);
			String k2 = getPointsKey (o2);
			int v1 = (int)players_cards.get (k1);
			int v2 = (int)players_cards.get (k2);
			return orderAscending ? v1-v2 : v2-v1;
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
		return CalculatePoints ((List<Map<String, Object>>)players_cards.get (player), includeHoleCard);
	}
	int CalculatePoints (String player)
	{
		return CalculatePoints (player, true);
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
		sb.append (" 点。 ");
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

	@Override
	public void run ()
	{
		try
		{
			StringBuilder sb = null;	//new StringBuilder ();
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 开始…");

			// 洗牌
			InitDeck ();
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
			deal ("暗牌: ", MESSAGE_TYPE_MASK_PM);
			bot.SendMessage (channel, null, false, 1, "每人发了一张暗牌，已通过私信发送具体牌，请注意查看");

			// 分明牌
			deal ("明牌: ", MESSAGE_TYPE_MASK_PUBLIC | MESSAGE_TYPE_MASK_PM);

			// 开始
			List<String> liveParticipants = participants;
			List<String> standParticipants = new ArrayList<String> ();	// 停牌的玩家
			List<String> deadParticipants = new ArrayList<String> ();	// 爆牌的玩家
			while (true)
			{
				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, Dialog.Type.单选, "要牌么？ ", true, liveParticipants, wannaCards_CandidateAnswers,	// 发票要么？ 毛片要么？
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
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
						deal (p, "", MESSAGE_TYPE_MASK_PM | MESSAGE_TYPE_MASK_PUBLIC);
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

				// 结束条件: 只剩下一个“活着”的，或者，所有人都不要牌了
				if (liveParticipants.size () <= 1  ||  isAllDontWannaCards)
					break;
			}

			sb = new StringBuilder ();
			// 活着的人的情况
			liveParticipants.addAll (standParticipants);
			PointsComparator comparator = new PointsComparator (false);
			Collections.sort (liveParticipants, comparator);
			GeneratePlayersCardsInfoTo (liveParticipants, "存活", sb, null, true);

			// 爆牌的人的情况
			comparator.setOrder (true);
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
}
