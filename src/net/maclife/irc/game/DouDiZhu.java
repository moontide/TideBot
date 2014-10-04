package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class DouDiZhu extends CardGame
{
	@Override
	public void run ()
	{
		try
		{
			StringBuilder sb = new StringBuilder ();
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 开始…");
			InitDeck ();

			// 每人 17 张牌
			DealInitialCards ();

			int iTurn = 0;
			int 无人继续抢地主次数 = 0;

			String answer;
			String value = null;
			String landlord = null;
			// 确定地主
			stage = STAGE_抢地主;
			while (true)
			{
				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, Dialog.Type.单选, "抢地主吗？", true, participants.subList (iTurn, iTurn+1), 抢地主候选答案,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				dlg.timeout_second = 30;
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();
					answer = (String)participantAnswers.get (participants.get (iTurn));
					value = dlg.GetCandidateAnswerValueByValueOrLabel (answer);

				if (value.equalsIgnoreCase ("3"))
				{	// 有人叫到了 3 分，抢地主立刻结束，此人称为地主
					无人继续抢地主次数 = 0;
					landlord = participants.get (iTurn);
					break;
				}
				else if (value.equalsIgnoreCase ("1") || value.equalsIgnoreCase ("2"))
				{	// 把等于低于此数值的候选答案剔除
					无人继续抢地主次数 = 0;
					landlord = participants.get (iTurn);
				}
				else if (StringUtils.isEmpty (value) || value.equalsIgnoreCase ("N"))
				{
					无人继续抢地主次数 ++;
					if ((无人继续抢地主次数==2 && landlord!=null))
					{	// 如果有人之前抢过地主（未到 3 分），其他 2 人不再继续抢，则地主就是他了
						break;
					}
					if (无人继续抢地主次数>=3 && landlord==null)
						// 连续 3 人都没人叫地主，荒局
						throw new RuntimeException ("都没人抢地主，荒局");
				}

				iTurn = NextTurn (iTurn);
			}
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 地主是 " + landlord);

			// 底牌明示，归地主所有
			assert (landlord != null);
			List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (landlord);
			player_cards.addAll (deck);
				Collections.sort (player_cards, comparator);
			GenerateCardsInfoTo (deck, sb);
			String msg = name + " 游戏 #" + Thread.currentThread ().getId () + " 地主 " + landlord + " 获得了底牌: "+ sb;
			for (String p : participants)
			{
				bot.SendMessage (null, p, false, 1, msg);
			}
			bot.SendMessage (null, landlord, false, 1, "" + GenerateCardsInfoTo (player_cards, null));

			// 开始循环
			stage = STAGE_出牌;
			iTurn = participants.indexOf (landlord);
			while (true)
			{
				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, null, "请出牌 ", true, participants, null,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				dlg.timeout_second = 30;
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				break;
			}
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

	/**
	 * 顺序轮流
	 * @param iTurn 当前玩家的索引号
	 * @return 玩家在列表中索引号 (从 0 开始)
	 */
	int NextTurn (int iTurn)
	{
		iTurn ++;
		if (iTurn >= 3)
			iTurn = 0;
		return iTurn;
	}

	public static final int STAGE_抢地主 = 1;
	public static final int STAGE_出牌   = 2;
	int stage;
	public List<String[]> 抢地主候选答案 = new ArrayList<String[]> ();	// 候选答案
	{
		抢地主候选答案.add (new String[]{"1", "1分"});
		抢地主候选答案.add (new String[]{"2", "2分"});
		抢地主候选答案.add (new String[]{"3", "3分"});
		抢地主候选答案.add (new String[]{"N", "不抢"});
	}
	public enum Type
	{
		单,
		单顺,
		对,
		对顺,
		三,
		三顺,
		三带1,
		飞机,
		飞机带翅膀,

		炸弹,
		王炸,
	}

	public DouDiZhu ()
	{

	}
	public DouDiZhu (LiuYanBot bot, List<Game> listGames, List<String> listParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("斗地主", bot, listGames, listParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
		if (listParticipants.size () < 3)
			throw new IllegalArgumentException ("需要 3 人玩。在后面用 /p 参数指定其他玩家");
		if (listParticipants.size () > 3)
			throw new IllegalArgumentException ("只能 3 人玩。请去掉 " + (listParticipants.size ()-3) + " 个玩家后重试");
	}

	/**
	 * 初始化牌堆
	 */
	void InitDeck ()
	{
		// 一副牌
		for (int i=0; i<1; i++)
		{
			// 2-A
			for (int r=1; r<=13; r++)
			{
				// '♣', '♦', '♥', '♠'
				for (int s=0; s<CARD_SUITS.length; s++)
				{
					AddCardToDeck (r, s);
				}
			}
		}
		// 加上大小王
		AddJokerCardsToDeck ();

		// 洗牌
		Collections.shuffle (deck);
System.out.println (deck);
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
		if (r==1)
			card.put ("point", 14);	// 修改 A 的大小: A 比 K 大
		else if (r==2)
			card.put ("point", 15);	// 修改 2 的大小: 2 比 A 大
		else
			card.put ("point", r);

		if (CARD_SUITS[s]=='♣' || CARD_SUITS[s]=='♠')
			card.put ("color", "");
		else if (CARD_SUITS[s]=='♦' || CARD_SUITS[s]=='♥')
			card.put ("color", Colors.RED);

		deck.add (card);
	}

	void AddJokerCardsToDeck ()
	{
		Map<String, Object> card = new HashMap<String, Object> ();
		card.put ("suit", "");	// 花色
		card.put ("rank", "☆");	// 牌面 🃟☆
		card.put ("point", 99);	// 大小
		card.put ("color", "");
		deck.add (card);

		card = new HashMap<String, Object> ();
		card.put ("suit", "");	// 花色
		card.put ("rank", "★");	// 牌面 🃏★
		card.put ("point", 100);	// 大小
		card.put ("color", Colors.PURPLE);
		deck.add (card);
	}

	public static int RankToPoint (String rank)
	{
		if (StringUtils.equalsIgnoreCase (rank, "3"))
			return 3;
		else if (StringUtils.equalsIgnoreCase (rank, "4"))
			return 4;
		else if (StringUtils.equalsIgnoreCase (rank, "5"))
			return 5;
		else if (StringUtils.equalsIgnoreCase (rank, "6"))
			return 6;
		else if (StringUtils.equalsIgnoreCase (rank, "7"))
			return 7;
		else if (StringUtils.equalsIgnoreCase (rank, "8"))
			return 8;
		else if (StringUtils.equalsIgnoreCase (rank, "9"))
			return 9;
		else if (StringUtils.equalsIgnoreCase (rank, "10") || StringUtils.equalsIgnoreCase (rank, "0") || StringUtils.equalsIgnoreCase (rank, "1"))
			return 10;
		else if (StringUtils.equalsIgnoreCase (rank, "J"))
			return 11;
		else if (StringUtils.equalsIgnoreCase (rank, "Q"))
			return 12;
		else if (StringUtils.equalsIgnoreCase (rank, "K"))
			return 13;
		else if (StringUtils.equalsIgnoreCase (rank, "A"))
			return 14;
		else if (StringUtils.equalsIgnoreCase (rank, "2"))
			return 15;
		else if (StringUtils.equalsIgnoreCase (rank, "☆"))
			return 99;
		else if (StringUtils.equalsIgnoreCase (rank, "★"))
			return 100;
		return 0;
	}

	void DealInitialCards ()
	{
		for (int ip=0; ip<3; ip++)
		{
			String p = participants.get (ip);
			List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
			if (player_cards == null)
			{
				player_cards = new ArrayList<Map<String, Object>> ();
				players_cards.put (p, player_cards);
			}
			for (int i=0; i<17; i++)
			{
				player_cards.add (deck.get (i*3 + ip));
			}
			Collections.sort (player_cards, comparator);
			bot.SendMessage (null, p, false, 1, "您摸了 " + player_cards.size () + " 张牌: " + GenerateCardsInfoTo(p));
		}
		for (int i=0; i<3*17; i++)	// 剔除摸掉的牌
			deck.remove (0);
		bot.SendMessage (channel, "", false, 1, "每人摸了 17 张牌 ");
	}

	/**
	 * 单张牌点值比较器，用于对手牌排序
	 * @author liuyan
	 *
	 */
	static class DDZPointComparator implements Comparator<Object>
	{
		@Override
		public int compare (Object o1, Object o2)
		{
			int v1 = 0;
			int v2 = 0;
			if (o1 instanceof Map)	// Map<String, Object> 牌的 Map 对象
			{
				Map<String, Object> card1 = (Map<String, Object>)o1;
				Map<String, Object> card2 = (Map<String, Object>)o2;
				v1 = (int)card1.get ("point");
				v2 = (int)card2.get ("point");
			}
			else if (o1 instanceof String)	// 只有牌的 rank
			{
				v1 = RankToPoint ((String)o1);
				v2 = RankToPoint ((String)o2);
			}
			return v1-v2;
		}
	}
	public static final Comparator<Object> comparator = new DDZPointComparator ();

	/**
	 * 	生成单个玩家的牌的信息
	 * @param p
	 * @param sb_in
	 * @return
	 */
	@SuppressWarnings ("unchecked")
	StringBuilder GenerateCardsInfoTo (String p, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
		GenerateCardsInfoTo (player_cards, sb);
		return sb;
	}
	StringBuilder GenerateCardsInfoTo (String p)
	{
		return GenerateCardsInfoTo (p, null);
	}
	StringBuilder GenerateCardsInfoTo (List<Map<String, Object>> cards, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		for (int i=0; i<cards.size (); i++)
		{
			Map<String, Object> card = cards.get (i);
			sb.append (card.get ("rank"));	// card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL
			sb.append (" ");
		}
		return sb;
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		if (stage == STAGE_抢地主)
			return true;
		// 先每一张看出的牌手里有没有，没有则报错

		// 检查是什么牌型

		// 检查是出牌发起人，还是响应出牌

			// 如果是本次战斗的发起人，则记下其牌型

			// 如果是响应出牌，则检查牌型与发起人是否一致
		return true;
	}

	/**
	 * 判断牌型。
	 * 注意：这里并不判断所有的牌是不是在自己手里（有效的、合法的），调用者需要自己判断。
	 * @param answer 玩家出的牌，需要用空格分开每张牌。如果不是的话，10 需要用 0 代替，如：890JQK <-- 顺子
	 * @return
	 */
	public String GetCardsType (String answer)
	{
		String sType = null;
System.out.println (answer);
		String[] arrayCardRanks = answer.split (" +");
		List<String> listCardRanks = Arrays.asList (arrayCardRanks);
		Collections.sort (listCardRanks, comparator);
Map<String, Object> result = CalculateCards (listCardRanks);
System.out.println (result);
		if (listCardRanks.size () == 1)
			return "单";
		else if (listCardRanks.size () == 2)
		{
			if (listCardRanks.contains ("☆") && listCardRanks.contains ("★"))
				return "王炸";
			if (listCardRanks.get (0).equalsIgnoreCase (listCardRanks.get (1)))
				return "对";
		}
System.out.println (listCardRanks);
		int nSolo = (int)result.get ("nSolo");
		int nPair = (int)result.get ("nPair");
		int nTrio = (int)result.get ("nTrio");
		int nQuartette = (int)result.get ("nQuartette");
		int nPrimaryCardType = (int)result.get ("PrimaryCardType");
		boolean isSerial = (boolean)result.get ("IsSerial");
		switch (nPrimaryCardType)
		{
		case 4:
			if (nQuartette == 1)
			{
				if (nTrio!=0)
					throw new IllegalArgumentException ("四张牌不能带 3 张牌");
				if (nSolo==0 && nPair==2)
					return "四带2对";
				if (nSolo==2 && nPair==0)
					return "四带2";
				if (nSolo==0 && nPair==0)
					return "炸弹";
				throw new IllegalArgumentException ("四张牌带的附牌数不对: " + nSolo + "张单牌, " + nPair + "双对子");
			}
			else
			{	// 不当炸弹出，真的没问题？
				if (!isSerial)
					throw new IllegalArgumentException (nTrio + " 组四张牌不是顺子/飞机");
				if (nSolo==0 && nPair==0)
					return "四顺/飞机";
				if (nSolo==nQuartette*2 && nPair==0)
					return "四顺带单牌";
				if (nSolo==0 && nPair==nQuartette*2)
					return "四顺带对子";
				throw new IllegalArgumentException ("四顺牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			//break;
		case 3:
			if (nTrio == 1)
			{
				if (nSolo==0 && nPair==0)
					return "三";
				if (nSolo==1 && nPair==0)
					return "三带1";
				if (nSolo==0 && nPair==1)
					return "三带1对";
				throw new IllegalArgumentException ("三张牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			else if (nTrio > 1)
			{
				// 检查是不是顺子
				if (!isSerial)
					throw new IllegalArgumentException (nTrio + " 组三张牌不是顺子/飞机");
				if (nSolo==0 && nPair==0)
					return "三顺/飞机";
				if (nSolo==nTrio && nPair==0)
					return "三顺带单牌";
				if (nSolo==0 && nPair==nTrio)
					return "三顺带对子";
				throw new IllegalArgumentException ("三顺牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			throw new IllegalArgumentException ("无效的三张牌组数 " + nTrio);
			//break;rio
		case 2:
			if (nSolo != 0)
				throw new IllegalArgumentException ("对子不能带单牌");
			if (nPair == 1)
				return "对";
			if (nPair >= 3)
			{
				if (isSerial)
					return "连对";
				else
					throw new IllegalArgumentException (nPair + " 双对子不是连对");
			}
			throw new IllegalArgumentException ("不能出 " + nPair + " 双对子");
			//break;
		case 1:
			return "单";
			//break;
		}
		return "";
	}

	/**
	 *
	 * @param listCardRanks
	 * @return Map 对象，其中包含的 key 有
	 * <dl>
	 * 	<dt>PrimaryCardType<dt>
	 * 	<dd>主牌牌型。整数类型。这个牌型仅仅是主牌是 1张牌 2张牌 3张牌 4张牌 的意思</dd>
	 * 	<dt>PrimaryCards<dt>
	 * 	<dd>主牌列表。List&lt;String&gt; 类型。这个列表，并非 333444 这样有重复牌的列表，只是 key 的列表，如： 34。</dd>
	 * 	<dt>IsSerial<dt>
	 * 	<dd>主牌是否顺子。 true|false，null 时为 false</dd>
	 * 	<dt>MaxPoint<dt>
	 * 	<dd>最大点数。整数类型。</dd>
	 * 	<dt>Attachments<dt>
	 * 	<dd>附带的牌。List&lt;String&gt; 类型。这些数据基本无用(不参与比较)，只用来显示用。</dd>
	 * </dl>
	 */
	public Map<String, Object> CalculateCards (List<String> listCardRanks)
	{
		Map<String, Object> result = new HashMap<String, Object> ();
		String sRank;
		for (int i=0; i<listCardRanks.size (); i++)
		{
			sRank = listCardRanks.get (i);
			if (result.get (sRank)==null)
				result.put (sRank, 1);
			else
				result.put (sRank, (int)result.get (sRank) + 1);
		}

		// 找出主牌型
		int nSolo = 0;
		int nPair = 0;
		int nTrio = 0;
		int nQuartette = 0;
		int nPrimaryCardType = 0;
		for (Object o : result.values ())
		{
			int n = (int)o;
			if (nPrimaryCardType < n)
				nPrimaryCardType = n;
			switch (n)
			{
			case 1:
				nSolo ++;
				break;
			case 2:
				nPair ++;
				break;
			case 3:
				nTrio ++;
				break;
			case 4:
				nQuartette ++;
				break;
			}
		}

		// 排成顺子
		List<String> listPrimaryCards = new ArrayList<String> ();
		for (String k : result.keySet ())
		{
			if ((int)result.get (k) == nPrimaryCardType)
				listPrimaryCards.add (k);
		}
		Collections.sort (listPrimaryCards, comparator);
		int MaxPoint = RankToPoint (listPrimaryCards.get (listPrimaryCards.size () - 1));
		boolean IsSerial = IsSerial (listPrimaryCards);

		// 保存结果
		result.put ("PrimaryCardType", nPrimaryCardType);
		result.put ("PrimaryCards", listPrimaryCards);
		result.put ("MaxPoint", MaxPoint);
		result.put ("IsSerial", IsSerial);
		result.put ("nSolo", nSolo);
		result.put ("nPair", nPair);
		result.put ("nTrio", nTrio);
		result.put ("nQuartette", nQuartette);

		return result;
	}

	/**
	 * 判断是不是顺子。并不判断牌的数量（但至少两张）
	 * @param listCardRanks 必须是按顺序排列好的，否则结果未知
	 * @return
	 */
	public boolean IsSerial (List<String> listCardRanks)
	{
		if (listCardRanks.size () < 2)
			return false;
		for (int i=0; i<listCardRanks.size (); i++)
		{
			if (i != listCardRanks.size () - 1)
			{
				String r = listCardRanks.get (i);
				String nextR = listCardRanks.get (i+1);
				int p = RankToPoint (r);
				int nextP =RankToPoint (nextR);
				if ((nextP - p) != 1)
					return false;
			}
		}
		return true;
	}

	public static void main (String[] args)
	{
		DouDiZhu ddz = new DouDiZhu ();

System.out.println ("牌型测试 开始");
		assert ddz.GetCardsType("2").equalsIgnoreCase ("单");
		assert (ddz.GetCardsType("2 2").equalsIgnoreCase ("对"));
		assert (ddz.GetCardsType("2 2 2").equalsIgnoreCase ("三"));

		assert (ddz.GetCardsType("10 3 4 5 6 7 8 9").equalsIgnoreCase ("单顺"));
		assert (ddz.GetCardsType("3 3 4 4 5 5 6 6").equalsIgnoreCase ("对顺"));
		assert (ddz.GetCardsType("6 6 6 7 7 7 8 8 8").equalsIgnoreCase ("三顺"));

		assert (ddz.GetCardsType("★☆").equalsIgnoreCase ("王炸"));
		assert (ddz.GetCardsType("Q K A 2 J").equalsIgnoreCase (""));
System.out.println ("牌型测试 结束");
	}
}
