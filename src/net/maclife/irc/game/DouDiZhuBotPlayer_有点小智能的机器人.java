package net.maclife.irc.game;

import java.util.*;
import java.util.logging.*;

import net.maclife.ansi.*;

/**
 * 根据作者自己的出牌习惯制作的 Bot，可将其理解为作者的一部分
 * @author liuyan
 *
 */
@SuppressWarnings ({"unchecked", "unused"})
public class DouDiZhuBotPlayer_有点小智能的机器人 extends DouDiZhuBotPlayer
{
	//static Logger logger = Logger.getLogger (DouDiZhuBotPlayer_有点小智能的机器人.class.getName ());
	public DouDiZhuBotPlayer_有点小智能的机器人 (String name)
	{
		super (name);
	}

	/**
	 * 评估牌的情况。
	 * 计算出
	 * <ul>
	 * <li>打玩这些牌所需要的最小的次数 N（且是最优的）</li>
	 * <li>上面得到最小的次数 N 的 N 道牌</li>
	 * </ul>
	 * @param player_cards
	 * @return Map
	 * key 列表
 		<dl>

			<dt>当前递归深度</dt>
			<dd>深度从 1 开始，每递归一次，深度+1（由本级递归负责加一）；递归结束后，深度-1（由上级递归调用者负责减一）</dd>

			<dt>当前递归分支</dt>
			<dd>也就是说：已经分析出来的几道牌</dd>

			<dt></dt>
			<dd></dd>

			<dt>递归深度</dt>
			<dd></dd>

		</dl>
	 */
	public static Map<String, Object> EvaluatePlayerCards (List<Map<String, Object>> player_cards)
	{
		List<String> listCards = DouDiZhu.PlayerCardsToCardRanks (player_cards);
		return EvaluateCards (listCards);
	}
	public static Map<String, Object> EvaluateCards (List<String> listCards)
	{
		//logger.setLevel (Level.ALL);
		//logger.entering (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards");
		Map<String, Object> mapResult = new LinkedHashMap<String, Object> ();

		mapResult.put ("MinTimes", 14);	// 最多只需要出 14 道牌：3-A,2,王牌(1张或两张)
		mapResult.put ("递归深度", 0);

		EvaluateCards_Recursive ("开始", listCards, mapResult);
		//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards");
		return mapResult;
	}

	// 不同牌型、按照不同顺序依次查找，其中顺子（含连对、飞机、大飞机的情况）还要考虑各个顺子长度、牌组数的情况
	public static void EvaluateCards_Recursive (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult)
	{
		//logger.entering (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Recursive");
		if (listCards.isEmpty ())
		{
			//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Recursive");
			return;
		}

		Log (mapResult, sCurrentOperation);
		Log (mapResult, "--------------------------------------------------------------------------------");

		// 先把王炸找出来：王炸不能跟其他牌组合来出，有的话，必定是单独的 1 道牌
		if (listCards.size () >=2)
		{
			sCurrentOperation = "找王炸";
			EvaluateCards_王炸 (sCurrentOperation, listCards, mapResult);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}

		// 不同长度的顺子
		if (listCards.size () >=5)
		{
			int nMax = listCards.size () > 12 ? 12 : listCards.size ();
			for (int nSerialLength=5; nSerialLength<=nMax; nSerialLength++)
			{
				sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的顺子";
				EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 1);
				递归深度减少 (sCurrentOperation, mapResult, listCards);
			}
		}
		// 不同长度的连对
		if (listCards.size () >=6)
		{
			for (int nSerialLength=3; nSerialLength<=listCards.size () / 2; nSerialLength++)
			{
				sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的连对";
				EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 2);
				递归深度减少 (sCurrentOperation, mapResult, listCards);
			}
		}
		// 不同长度的大飞机
		if (listCards.size () >=8)
		{
			for (int nSerialLength=2; nSerialLength<=listCards.size () / 4; nSerialLength++)
			{
				sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的大飞机";
				EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 4);
				递归深度减少 (sCurrentOperation, mapResult, listCards);
				if (nSerialLength * (4 + 2) <= listCards.size ())
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的大飞机带2单";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 4, DouDiZhu.附带牌类型.带单);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 大飞机带单（2单）
				}
				if (nSerialLength * (4 + 4) <= listCards.size ())
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的大飞机带2对";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 4, DouDiZhu.附带牌类型.带对);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 大飞机带对（2对）
				}
			}
		}
		// 不同长度的飞机
		if (listCards.size () >=6)
		{
			for (int nSerialLength=2; nSerialLength<=listCards.size () / 3; nSerialLength++)
			{
				sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的飞机";
				EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 3);
				递归深度减少 (sCurrentOperation, mapResult, listCards);
				if (nSerialLength * (3 + 1) <= listCards.size ())
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的飞机带单";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 3, DouDiZhu.附带牌类型.带单);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 飞机带单
				}
				if (nSerialLength * (3 + 2) <= listCards.size ())
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的飞机带对";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 3, DouDiZhu.附带牌类型.带对);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 飞机带对
				}
			}
		}

		//
		if (listCards.size () >=6)
		{
			sCurrentOperation = "找单组：四带2";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 4, DouDiZhu.附带牌类型.带单);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if (listCards.size () >=8)
		{
			sCurrentOperation = "找单组：四带2对";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 4, DouDiZhu.附带牌类型.带对);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if (listCards.size () >=4)
		{
			sCurrentOperation = "找单组：炸弹";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 4);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if (listCards.size () >=4)
		{
			sCurrentOperation = "找单组：三带1";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 3, DouDiZhu.附带牌类型.带单);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if (listCards.size () >=5)
		{
			sCurrentOperation = "找单组：三带1对";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 3, DouDiZhu.附带牌类型.带对);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if (listCards.size () >=3)
		{
			sCurrentOperation = "找单组：三";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 3);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if (listCards.size () >=1)
		{
			sCurrentOperation = "找单组：单牌";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 1);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if (listCards.size () >=2)
		{
			sCurrentOperation = "找单组：对子";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 2);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Recursive");
	}

	// 先处理最复杂的顺子（含连对、飞机、大飞机、以及飞机附带散牌的情况）
	public static void EvaluateCards_Serials (String sCurrentOperation, final List<String> listCards, Map<String, Object> mapResult, int nSerialLength, int nCopy, DouDiZhu.附带牌类型 attachmentType /* 0:无附带的牌 1:单牌 2:对牌 */)
	{
		递归深度增加 (sCurrentOperation, mapResult, listCards);
		if (listCards.isEmpty ())
			return;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculateCards (listCards);
		if (listCards.size() < nSerialLength * (nCopy + attachmentType.ordinal ()) )
			return;

		String s解出的一道牌 = null;
		List<String> listUniqueCards = (List<String>) mapCardsInfo.get ("UniqueCards");
		List<String> listSoloCards = (List<String>) mapCardsInfo.get ("SoloCards"), listRemainingSoloCards;
		List<String> listPairCards = (List<String>) mapCardsInfo.get ("PairCards"), listRemainingPairCards;
		List<String> listTrioCards = (List<String>) mapCardsInfo.get ("TrioCards"), listRemainingTrioCards;
		List<String> listQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards"), listRemainingQuartetteCards;
		List<String> listSubList = null;
		List<String> listRemainingCards = new ArrayList<String> ();
		Map<String, Object> mapSubCardsInfo = null;
		/*
		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return;

			// 从这里开始，每个不同的牌面一个标记
			int iNextRank = 0;
			String sThisRank = listCards.get (i);
			String sThisRankPoint = listCards.get (i);
			//if
			//List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			//Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			//if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				//return 生成用于出牌的顺子 (listSubCards, 1);
			}
		}
		*/

		switch (nCopy)
		{
		case 4:
			for (int i=0; i<=listUniqueCards.size () - nSerialLength; i++)
			{
				listSubList = listUniqueCards.subList (i, i + nSerialLength);
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				boolean 每张牌够张数 = true;
				for (int j=0; j<listSubList.size (); j++)
				{
					if ((int)mapCardsInfo.get (listSubList.get (j)) < nCopy)
					{	// 只要有一张牌不够死张，本顺子就是无效的连对
						每张牌够张数 = false;
						break;
					}
				}
				if (! 每张牌够张数)
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList, nCopy);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);

				if (attachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "大飞机", DouDiZhu.Type.大飞机, s解出的一道牌, listRemainingCards);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				switch (attachmentType)
				{
					case 带单:
						if (listRemainingSoloCards.size () >= nSerialLength * 2)
						{
							for (int j=0; j<nSerialLength * 2; j++)
							{	// 单牌数量足够，可以直接从单牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (j);
								listRemainingCards.remove (listRemainingSoloCards.get (j));
							}
							设置解出的一道牌 (listCards, mapResult, "大飞机带2单", DouDiZhu.Type.大飞机带2单, s解出的一道牌, listRemainingCards);
						}
						else
						{
							int 还差单牌数 = nSerialLength - listRemainingSoloCards.size ();
							// 怎么拆，需要根据拆后的牌的情况，多考虑
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingPairCards.size () * 2 >= 还差单牌数)
							{	//
								for (int j=0; j<还差单牌数; j++)
								{	// 单牌数量不够，然后剩下的对牌足够填满剩下的单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}
								设置解出的一道牌 (listCards, mapResult, "大飞机带2单(拆了对牌)", DouDiZhu.Type.大飞机带2单, s解出的一道牌, listRemainingCards);
							}
							else
							{
								for (int j=0; j<listRemainingPairCards.size () * 2; j++)
								{	// 单牌数量不够，然后剩下的对牌也不够填满剩下的单牌，那就先把对牌全部当单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}

								int 还差单牌数2 = 还差单牌数 - listRemainingPairCards.size () * 2;
								for (int j=0; j<listRemainingTrioCards.size () * 3; j++)
								{	// 单牌数量足够，可以直接从单牌里带走
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j/3);
									listRemainingCards.remove (listRemainingTrioCards.get (j/3));
								}
								设置解出的一道牌 (listCards, mapResult, "大飞机带2单(拆了对牌和三牌组)", DouDiZhu.Type.大飞机带2单, s解出的一道牌, listRemainingCards);
							}
						}
						break;
					case 带对:
						if (listRemainingPairCards.size () >= nSerialLength * 2)
						{
							for (int j=0; j<nSerialLength * 2; j++)
							{	// 对牌数量足够，可以直接从对牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j) + listRemainingPairCards.get (j);
								listRemainingCards.remove (listRemainingPairCards.get (j));
								listRemainingCards.remove (listRemainingPairCards.get (j));
							}

							设置解出的一道牌 (listCards, mapResult, "大飞机带2对", DouDiZhu.Type.大飞机带2对, s解出的一道牌, listRemainingCards);
						}
						else
						{
							int 还差对牌数 = nSerialLength * 2 - listRemainingPairCards.size ();
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingTrioCards.size () >= 还差对牌数)
							{	//
								for (int j=0; j<还差对牌数; j++)
								{	// 对牌数量不够，然后剩下的三牌组足够填满剩下的对牌
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j) + listRemainingTrioCards.get (j);
									listRemainingCards.remove (listRemainingTrioCards.get (j));
									listRemainingCards.remove (listRemainingTrioCards.get (j));
								}
								设置解出的一道牌 (listCards, mapResult, "大飞机带2对(拆了三牌组)", DouDiZhu.Type.大飞机带2对, s解出的一道牌, listRemainingCards);
							}
						}
						break;
				}
			}
			break;
		case 3:
			for (int i=0; i<=listUniqueCards.size () - nSerialLength; i++)
			{
				listSubList = listUniqueCards.subList (i, i + nSerialLength);
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				boolean 每张牌够张数 = true;
				for (int j=0; j<listSubList.size (); j++)
				{
					String sRank = listSubList.get (j);
					try
					{
						if ((int)mapCardsInfo.get (sRank) < nCopy)
						{	// 只要有一张牌不够三张，本顺子就是无效的飞机
							每张牌够张数 = false;
							break;
						}
					}
					catch (Exception e)
					{
						e.printStackTrace ();
					}
				}
				if (! 每张牌够张数)
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList, nCopy);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);

				if (attachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "飞机", DouDiZhu.Type.飞机, s解出的一道牌, listRemainingCards);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				switch (attachmentType)
				{
					case 带单:
						if (listRemainingSoloCards.size () >= nSerialLength * 1)
						{
							for (int j=0; j<nSerialLength * 1; j++)
							{	// 单牌数量足够，可以直接从单牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (j);
								listRemainingCards.remove (listRemainingSoloCards.get (j));
							}
							设置解出的一道牌 (listCards, mapResult, "飞机带单", DouDiZhu.Type.飞机带单, s解出的一道牌, listRemainingCards);
						}
						else
						{
							int 还差单牌数 = nSerialLength - listRemainingSoloCards.size ();
							// 怎么拆，需要根据拆后的牌的情况，多考虑
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingPairCards.size () * 2 >= 还差单牌数)
							{	//
								for (int j=0; j<还差单牌数; j++)
								{	// 单牌数量不够，然后剩下的对牌足够填满剩下的单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}
								设置解出的一道牌 (listCards, mapResult, "飞机带单(拆了对牌)", DouDiZhu.Type.飞机带单, s解出的一道牌, listRemainingCards);
							}
							else
							{
								for (int j=0; j<listRemainingPairCards.size () * 2; j++)
								{	// 单牌数量不够，然后剩下的对牌也不够填满剩下的单牌，那就先把对牌全部当单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}

								int 还差单牌数2 = 还差单牌数 - listRemainingPairCards.size () * 2;
								for (int j=0; j<listRemainingTrioCards.size () * 3; j++)
								{	// 单牌数量足够，可以直接从单牌里带走
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j/3);
									listRemainingCards.remove (listRemainingTrioCards.get (j/3));
								}
								设置解出的一道牌 (listCards, mapResult, "飞机带单(拆了对牌和三牌组)", DouDiZhu.Type.飞机带单, s解出的一道牌, listRemainingCards);
							}
						}
						break;
					case 带对:
						if (listRemainingPairCards.size () >= nSerialLength)
						{
							for (int j=0; j<nSerialLength; j++)
							{	// 对牌数量足够，可以直接从对牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j) + listRemainingPairCards.get (j);
								listRemainingCards.remove (listRemainingPairCards.get (j));
								listRemainingCards.remove (listRemainingPairCards.get (j));
							}
							设置解出的一道牌 (listCards, mapResult, "飞机带对", DouDiZhu.Type.飞机带对, s解出的一道牌, listRemainingCards);
						}
						else
						{
							int 还差对牌数 = nSerialLength - listRemainingPairCards.size ();
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingTrioCards.size () >= 还差对牌数)
							{	//
								for (int j=0; j<还差对牌数; j++)
								{	// 单牌数量不够，然后剩下的对牌足够填满剩下的单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j) + listRemainingTrioCards.get (j);
									listRemainingCards.remove (listRemainingTrioCards.get (j));
									listRemainingCards.remove (listRemainingTrioCards.get (j));
								}
								设置解出的一道牌 (listCards, mapResult, "飞机带对(拆了三牌组)", DouDiZhu.Type.飞机带对, s解出的一道牌, listRemainingCards);
							}
						}
						break;
				}
			}
			break;
		case 2:
			for (int i=0; i<=listUniqueCards.size () - nSerialLength; i++)
			{
				listSubList = listUniqueCards.subList (i, i + nSerialLength);	// 从不同的偏移量抽出 nSerialLength 张牌
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				boolean 每张牌够张数 = true;
				for (int j=0; j<listSubList.size (); j++)
				{
					if ((int)mapCardsInfo.get (listSubList.get (j)) < nCopy)
					{	// 只要有一张牌不够两张，本顺子就是无效的连对
						每张牌够张数 = false;
						break;
					}
				}
				if (! 每张牌够张数)
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList, nCopy);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);

				设置解出的一道牌 (listCards, mapResult, "连对", DouDiZhu.Type.连对, s解出的一道牌, listRemainingCards);
			}
			break;
		case 1:
			for (int i=0; i<=listUniqueCards.size () - nSerialLength; i++)	// 从不同的偏移量抽出 nSerialLength 张牌
			{
				listSubList = listUniqueCards.subList (i, i + nSerialLength);
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList);
				//listSubCards.removeAll (listSubList);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);
				//Log (mapResult, "✓ 从不重复牌列表 " + listUniqueCards + " 偏移量 " + i + " 中找到了长度=" + nSerialLength + "的顺子 " + FormatCardPack (s解出的一道牌));
				设置解出的一道牌 (listCards, mapResult, "顺子", DouDiZhu.Type.顺子, s解出的一道牌, listRemainingCards);
			}
			break;
		}
	}
	public static void EvaluateCards_Serials (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, int nSerialLength, int nCopy)
	{
		EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, nCopy, DouDiZhu.附带牌类型.不带牌);
	}
	public static void EvaluateCards_Serials (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, int nSerialLength)
	{
		EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, nSerialLength, 1, DouDiZhu.附带牌类型.不带牌);
	}
	public static String SerialToString (List<String> listCards, int nCopy)
	{
		StringBuilder sb = new StringBuilder ();
		for (String rank : listCards)
		{
			for (int i=0; i<nCopy; i++)
				sb.append (rank);
		}
		return sb.toString ();
	}
	public static String SerialToString (List<String> listCards)
	{
		return SerialToString (listCards, 1);
	}


	// 再处理相对简单的单组牌（含单、对、三牌组、三牌组附带散牌的情况）
	public static void EvaluateCards_Singles (String sCurrentOperation, final List<String> listCards, Map<String, Object> mapResult, int nCopy, DouDiZhu.附带牌类型 attachmentType /* 0:无附带的牌 1:单牌 2:对牌 */)
	{
		//logger.entering (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Singles");
		递归深度增加 (sCurrentOperation, mapResult, listCards);
		if (listCards.isEmpty ())
			return;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculateCards (listCards);
		if (listCards.size() < (nCopy + attachmentType.ordinal ()) )
			return;

		String s解出的一道牌 = null;
		List<String> listRemainingCards = new ArrayList<String> ();
		List<String> listSoloCards = (List<String>) mapCardsInfo.get ("SoloCards"), listRemainingSoloCards;
		List<String> listPairCards = (List<String>) mapCardsInfo.get ("PairCards"), listRemainingPairCards;
		List<String> listTrioCards = (List<String>) mapCardsInfo.get ("TrioCards"), listRemainingTrioCards;
		List<String> listQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards"), listRemainingQuartetteCards;

		switch (nCopy)
		{
		case 4:
			if (listQuartetteCards.isEmpty ())
				return;
			for (String rank : listQuartetteCards)
			{
				Log (mapResult, "########################################");
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				s解出的一道牌 = rank + rank + rank+ rank;

				if (attachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "炸弹（四牌组）", DouDiZhu.Type.炸弹, s解出的一道牌, listRemainingCards);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				if (attachmentType == DouDiZhu.附带牌类型.带单)
				{	// 带两张单牌
					if (listRemainingSoloCards.size () >= 2)
					{
						//Log (mapResult, "四带2 找到了两张单牌");
						listRemainingCards.remove (listRemainingSoloCards.get (0));
						listRemainingCards.remove (listRemainingSoloCards.get (1));
						s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0) + listRemainingSoloCards.get (1);
						设置解出的一道牌 (listCards, mapResult, "四带2", DouDiZhu.Type.四带2, s解出的一道牌, listRemainingCards);
					}
					else if (listRemainingSoloCards.size () == 1)
					{
						//Log (mapResult, "四带2 只找到了 1 张单牌，准备拆其他的对牌、三牌组…	");
						listRemainingCards.remove (listRemainingSoloCards.get (0));
						if (! listRemainingPairCards.isEmpty () || ! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							if (! listRemainingPairCards.isEmpty () )
							{
								//Log (mapResult, "拆一对 " + listRemainingPairCards.get (0));
								listRemainingCards.remove (listRemainingPairCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0) + listRemainingPairCards.get (0);
								Log (mapResult, "✓ 找到了四带2(拆对子) " + FormatCardPack (s解出的一道牌));
							}
							else if (! listRemainingTrioCards.isEmpty ())
							{
								//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0) + listRemainingTrioCards.get (0);
								Log (mapResult, "✓ 找到了四带2(拆三牌组) " + FormatCardPack (s解出的一道牌));
							}
							设置解出的一道牌 (listCards, mapResult, "四带2（拆了其他牌组中的1张）", DouDiZhu.Type.四带2, s解出的一道牌, listRemainingCards);
						}
						else
						{
							//Log (mapResult, "没找到其他能拆的对牌、三牌组。");
						}
					}
					else if (listRemainingSoloCards.isEmpty ())
					{	// 根本没单牌，拿一个对子当单牌
						//Log (mapResult, "四带2 根本没单牌可以带，准备带其他的对牌、或者拆三牌组…	");
						if (! listRemainingPairCards.isEmpty () || ! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							if (! listRemainingPairCards.isEmpty () )
							{
								//Log (mapResult, "把一对 " + listRemainingPairCards.get (0) + " 当两个单带出");
								listRemainingCards.remove (listRemainingPairCards.get (0));
								listRemainingCards.remove (listRemainingPairCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0);
								Log (mapResult, "✓ 找到了四带2 " + FormatCardPack (s解出的一道牌));
							}
							else if (! listRemainingTrioCards.isEmpty ())
							{
								//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (0) + listRemainingTrioCards.get (0);
								Log (mapResult, "✓ 找到了四带2 " + FormatCardPack (s解出的一道牌));
							}
							设置解出的一道牌 (listCards, mapResult, "四带2（拆了其他牌组中的2张）", DouDiZhu.Type.四带2, s解出的一道牌, listRemainingCards);
						}
						else
						{
							//Log (mapResult, "没找到其他能带的对牌、能拆的三牌组。");
						}
					}
				}
				else if (attachmentType == DouDiZhu.附带牌类型.带对)
				{	// 带两组对牌
					if (listRemainingPairCards.size () >= 2)
					{
						//Log (mapResult, "四带2对 找到了两组对牌");
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (1));
						listRemainingCards.remove (listRemainingPairCards.get (1));
						s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0) + listRemainingPairCards.get (1) + listRemainingPairCards.get (1);
						设置解出的一道牌 (listCards, mapResult, "四带2对", DouDiZhu.Type.四带2对, s解出的一道牌, listRemainingCards);
					}
					else if (listRemainingPairCards.size () >= 1)
					{
						//Log (mapResult, "四带2对 只找到了 1 组对牌，准备拆其他的三牌组…	");
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (0));
						if (! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0) + listRemainingTrioCards.get (0) + listRemainingTrioCards.get (0);
							设置解出的一道牌 (listCards, mapResult, "四带2（拆了其他三牌组）", DouDiZhu.Type.四带2对, s解出的一道牌, listRemainingCards);
						}
						else
						{
							//Log (mapResult, "没找到其他能拆的三牌组。");
						}
					}
					else if (listRemainingPairCards.isEmpty ())
					{	// 根本没1组对牌，不去拆其他 3 牌了。倒不如合并两个炸弹当成 4带2对
						//
						//Log (mapResult, "四带2 根本没对牌可以带，准备合并其他炸弹当成 4 带 2 对…	");
						if (listRemainingQuartetteCards.size() >= 1)
						{
							//Log (mapResult, "合并其他炸弹 " + listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							s解出的一道牌 = s解出的一道牌 + listRemainingQuartetteCards.get (0) + listRemainingQuartetteCards.get (0) + listRemainingQuartetteCards.get (0) + listRemainingQuartetteCards.get (0);
							设置解出的一道牌 (listCards, mapResult, "四带2对（两个炸弹组成 = =）", DouDiZhu.Type.四带2对, s解出的一道牌, listRemainingCards);
						}
						else
						{
							//Log (mapResult, "没找到其他能合并的炸弹。");
						}
					}
				}
			}
			break;
		case 3:
			if (listTrioCards.isEmpty ())
				return;	// 这里也不从炸弹里面拆牌了，拆开后，出牌次数会更多
			for (String rank : listTrioCards)
			{
				Log (mapResult, "########################################");
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				s解出的一道牌 =  rank + rank + rank;

				if (attachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "三牌组", DouDiZhu.Type.三, s解出的一道牌, listRemainingCards);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				if (attachmentType == DouDiZhu.附带牌类型.带单)
				{	// 带 1 张单牌
					if (listRemainingSoloCards.size () >= 1)
					{
						//Log (mapResult, "三带1 找到了 1 张单牌");
						listRemainingCards.remove (listRemainingSoloCards.get (0));
						s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0);
						设置解出的一道牌 (listCards, mapResult, "三带1", DouDiZhu.Type.三带1, s解出的一道牌, listRemainingCards);
					}
					else if (listRemainingSoloCards.isEmpty ())
					{	// 根本没单牌，拿一个对子当单牌
						//Log (mapResult, "三带1 根本没单牌可以带，准备拆其他的对牌、或者拆三牌组…	");
						if (! listRemainingPairCards.isEmpty () || ! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							if (! listRemainingPairCards.isEmpty () )
							{
								//Log (mapResult, "拆一对 " + listRemainingPairCards.get (0));
								listRemainingCards.remove (listRemainingPairCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0);
								Log (mapResult, "找到了三带1(拆对) " + FormatCardPack (s解出的一道牌));
							}
							else if (! listRemainingTrioCards.isEmpty ())
							{
								Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (0);
								Log (mapResult, "找到了三带1(拆三) " + FormatCardPack (s解出的一道牌));
							}
							设置解出的一道牌 (listCards, mapResult, "三带1（拆了其他牌组的1张）", DouDiZhu.Type.三带1, s解出的一道牌, listRemainingCards);
						}
						else
						{
							//Log (mapResult, "没找到其他能拆的对牌、能拆的三牌组。");
						}
					}
				}
				else if (attachmentType == DouDiZhu.附带牌类型.带对)
				{	// 带 1 对牌
					if (listRemainingPairCards.size () >= 1)
					{
						//Log (mapResult, "三带1对 找到了 1 组对牌");
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (0));
						s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0);
						设置解出的一道牌 (listCards, mapResult, "三带1对", DouDiZhu.Type.三带1对, s解出的一道牌, listRemainingCards);
					}
					else if (listRemainingPairCards.isEmpty ())
					{	// 根本没1组对牌，拆其他 3 牌
						//Log (mapResult, "三带1对 根本没对牌可以带，准备拆其他 三牌组…	");
						if (! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (0) + listRemainingTrioCards.get (0);
							设置解出的一道牌 (listCards, mapResult, "三带1对（拆了其他三牌组的两张）", DouDiZhu.Type.三带1对, s解出的一道牌, listRemainingCards);
						}
						else
						{
							Log (mapResult, "没找到其他能拆的三牌组。");
						}
					}
				}
			}
			break;
		case 2:
			if (listPairCards.isEmpty ())
				return;	// 这里也不从三牌组、炸弹里面拆牌了，拆开后，出牌次数会更多
			//for (String rank : listPairCards)
			{
				String rank = listPairCards.get (0);	// 这里也不
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				s解出的一道牌 = rank + rank;
				//ResetCurrentCase (mapResult, listCopyOfCurrentCase);	// 对于不【循环尝试不同位置】的对子，就不需要复位当前出牌道
				设置解出的一道牌 (listCards, mapResult, "对牌", DouDiZhu.Type.对, s解出的一道牌, listRemainingCards);
			}
			break;
		case 1:
			if (listSoloCards.isEmpty ())
				return;	// 这里也不从对子、三牌组、炸弹里面拆牌了，拆开后，出牌次数会更多
			//for (String rank : listSoloCards)
			{
				String rank = listSoloCards.get (0);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				s解出的一道牌 = rank;
				//ResetCurrentCase (mapResult, listCopyOfCurrentCase);
				设置解出的一道牌 (listCards, mapResult, "单牌", DouDiZhu.Type.单, s解出的一道牌, listRemainingCards);
			}
			break;
		}
		//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Singles");
	}
	public static void EvaluateCards_Singles (final String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, int nCopy)
	{
		EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, nCopy, DouDiZhu.附带牌类型.不带牌);
	}
	public static void EvaluateCards_Single (final String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult)
	{
		EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, 1, DouDiZhu.附带牌类型.不带牌);
	}


	// 王炸单独处理
	public static void EvaluateCards_王炸 (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult)
	{
		递归深度增加 (sCurrentOperation, mapResult, listCards);
		if (listCards.isEmpty ())
			return;
		if (listCards.size() < 2 )
			return;

		String s解出的一道牌 = null;
		if (listCards.contains ("☆") && listCards.contains ("★"))
		{
			List<String> listRemainingCards = new ArrayList<String> ();
			listRemainingCards.addAll (listCards);

			// 将王炸的牌排除
			listRemainingCards.remove ("☆");
			listRemainingCards.remove ("★");

			s解出的一道牌 = "☆★";
			设置解出的一道牌 (listCards, mapResult, "王炸", DouDiZhu.Type.王炸, s解出的一道牌, listRemainingCards);
		}
	}

	public static void 递归深度增加 (String sCurrentOperation, Map<String, Object> mapResult, List<String> listCards)
	{
		Log (mapResult, "→ " + listCards.toString () + "	" + sCurrentOperation, true, false);

		Integer nDepth = (Integer)mapResult.get ("递归深度");
		if (nDepth == null)
			nDepth = 0;
		nDepth ++;
		mapResult.put ("递归深度", nDepth);

		Long nCount = (Long)mapResult.get ("递归次数");
		if (nCount == null)
			nCount = 0L;

		nCount ++;
		mapResult.put ("递归次数", nCount);

		//Log (mapResult, "增加递归深度到 " + nDepth + " 级。累计 " + nCount + " 次");
		Log (mapResult, nDepth + " 级， " + nCount + " 次。", false, true);

		mapResult.remove ("解出的一道牌_L" + nDepth);	// 清除每次递归可能产生的 key
		mapResult.remove ("是最后一道牌_L" + nDepth);	// 首先，必须有“解出的一道牌”，才能有此 key。 如果解出的牌是最后一道牌，则为 true，否则不存在（null）
		Log (mapResult, mapResult.get ("CurrentCase")==null ? "" : ((List<String>)mapResult.get ("CurrentCase")).toString () );
	}
	public static void 递归深度减少 (String sCurrentOperation, Map<String, Object> mapResult, List<String> listCards)
	{
		Integer nDepth = (Integer)mapResult.get ("递归深度");
		if (nDepth == null)
			nDepth = 0;

		/*
		if (mapResult.get ("解出的一道牌_L" + nDepth) != null)
		{
			List<String> listCurrentCase = (List<String>)mapResult.get ("CurrentCase");
			if (listCurrentCase!=null && listCurrentCase.size () >= nDepth)
			{
				//listCurrentCase.remove (listCurrentCase.size () - 1);
				while (listCurrentCase.size () >= nDepth)
					listCurrentCase.remove (listCurrentCase.size () - 1);

				System.out.println ("递归深度" + nDepth + "减少，当前出牌案例=" + listCurrentCase);
			}
		}
		*/

		nDepth --;
		mapResult.put ("递归深度", nDepth);
		Log (mapResult, "← 减少递归深度： " + nDepth + " ← " + (nDepth==null ? "null" : nDepth +1) + "	" + listCards);
	}
	public static void Log (Map<String, Object> mapResult, String s, boolean withLeftPadding, boolean terminateThisLine)
	{
		Integer nDepth = (Integer)mapResult.get ("递归深度");
		if (nDepth == null)
			nDepth = 0;
		Log (nDepth, s, withLeftPadding, terminateThisLine);
	}
	public static void Log (Map<String, Object> mapResult, String s)
	{
		Log (mapResult, s, true, true);
	}
	public static void Log (int nDepth, String s, boolean withLeftPadding, boolean terminateThisLine)
	{
		if (withLeftPadding)
		{
			for (int i=0; i<nDepth; i++)
			{
				System.out.print ("	");
			}
		}
		if (terminateThisLine)
		{
			System.out.println (s);
		}
		else
		{
			System.out.print (s);
			System.out.print ("	");
		}
	}

	static void 设置解出的一道牌 (List<String> listCards, Map<String, Object> mapResult, String sCardsDescription, DouDiZhu.Type type, String s解出的一道牌, List<String> listRemainingCards)
	{
		int nDepth = (int)mapResult.get ("递归深度");
		mapResult.put ("解出的一道牌_L" + nDepth, s解出的一道牌);
		mapResult.put ("解出的一道牌的牌型_L" + nDepth, type);
		List<List<String>> listAlmostAllCases = (List<List<String>> )mapResult.get ("AlmostAllCases");
		if (listAlmostAllCases == null)
		{
			listAlmostAllCases = new ArrayList<List<String>> ();
			mapResult.put ("AlmostAllCases", listAlmostAllCases);
		}

		List<String> listCurrentCase = (List<String>)mapResult.get ("CurrentCase");
		//if ((int)mapResult.get ("递归深度") == 1)
		//	listCurrentCase = null;
		if (listCurrentCase == null)
		{
			listCurrentCase = new ArrayList<String> ();
			for (int i=1; i<nDepth; i++)
			{
				String s上级解出的一道牌 = (String)mapResult.get ("解出的一道牌_L" + i);
				if (s上级解出的一道牌 != null)
					listCurrentCase.add (s上级解出的一道牌);
				//else
				//	listCurrentCase.add ("正常情况下，不可能出现这个情况：");
			}
			mapResult.put ("CurrentCase", listCurrentCase);
			listAlmostAllCases.add (listCurrentCase);
		}
		listCurrentCase.add (s解出的一道牌);

		Log (mapResult, "✓ 找到了" + sCardsDescription + " " + FormatCardPack (s解出的一道牌));
		Log (mapResult, "第 " + listAlmostAllCases.size () + " 分支=" + listCurrentCase);
		if (listRemainingCards.isEmpty ())
		{
			设置最后一道牌标志 (mapResult);
			mapResult.remove ("CurrentCase");
		}
		else //if (! listRemainingCards.isEmpty ())
			EvaluateCards_Recursive ("找到 " + sCardsDescription + " " + s解出的一道牌 + " 后，递归处理剩下的牌", listRemainingCards, mapResult);
	}
	static void 设置最后一道牌标志 (Map<String, Object> mapResult)
	{
		mapResult.put ("是最后一道牌_L" + mapResult.get ("递归深度"), true);
		//List<String> listCurrentCase = (List<String>)mapResult.get ("CurrentCase");
		PrintEndOfThisCase (mapResult);
		//mapResult.remove ("CurrentCase");
	}

	/*
	static void ResetCurrentCase (Map<String, Object> mapResult, List<String> listRef)
	{
		if (listRef == null)
		{
			System.out.println ("reset CurrentCase to null (removed)");
			mapResult.remove ("CurrentCase");
			return;
		}
		List<String> listCurrentCase = (List<String>) mapResult.get ("CurrentCase");
		if (listRef.isEmpty () && listCurrentCase!=null && !listCurrentCase.isEmpty ())
		{
			System.out.println ("reset CurrentCase to empty");
			listCurrentCase.clear ();
		}
		else if (!listRef.isEmpty () && listCurrentCase!=null && !listCurrentCase.isEmpty ())
		{
			while (listCurrentCase.size () > listRef.size ())
				listCurrentCase.remove (listCurrentCase.size () - 1);
			System.out.println ("reset CurrentCase to " + listCurrentCase);
		}
		else
		{
			System.out.println ("listCurrentCase=" + listCurrentCase);
			System.out.println ("listRef=" + listRef);
		}
	}
	*/

	public static String FormatCardPack (String sPack, String sANSIColor)
	{
		return sANSIColor + sPack + ANSIEscapeTool.CSI + "m";
	}
	public static String FormatCardPack (String sPack)
	{
		return FormatCardPack (sPack, ANSIEscapeTool.CSI + "32;1m");
	}

	public static void PrintEndOfThisCase (Map<String, Object> mapResult)
	{
		List<String> listCurrentCase = (List<String>)mapResult.get ("CurrentCase");
		List<List<String>> listAlmostAllCases = (List<List<String>> )mapResult.get ("AlmostAllCases");
		Log (mapResult, FormatCardPack("第 " + listAlmostAllCases.size () + " 分支已到尽头，出牌次数 = " + listCurrentCase.size () + "，出牌=" + listCurrentCase, ANSIEscapeTool.CSI + "41;1m"));
		if (listAlmostAllCases.size () == 6)
		{
System.out.println ("测试 3334445 时，到了第 7 分支发现有问题：少了前面解出的一道牌 333，只有 444,5 了。所以，从第 6 分支看起，看看什么问题");
		}
		if (listAlmostAllCases.size () == 7)
		{
System.out.println ("测试 3334445 时，到了第 7 分支发现有问题：少了前面解出的一道牌 333，只有 444,5 了，看看什么问题");
		}
	}

	@Override
	public Object 抢地主 (Object... params)
	{
		// 评估手牌情况，决定抢不抢地主
		// 底牌，有可能增强手牌，也完全有可能多出 3 张废牌、多出 3 道单牌！

		return null;
	}

	@Override
	public String 找出点数值大于N的单 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的顺子 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的连对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的三 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的三带1 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的三带1对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的飞机带单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的飞机带对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的四带2 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的四带2对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机带2单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机带2对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的炸弹 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public Object 出牌 (Object... params)
	{
		return null;
	}

	@Override
	public Object 回牌 (Object... params)
	{
		return null;
	}

	public static void main (String[] args)
	{
		if (args.length == 0)
		{
			System.err.println ("用法： java " + DouDiZhuBotPlayer_有点小智能的机器人.class.getCanonicalName () + " [斗地主牌组（只需要牌面，不需要花色）]..." );
		}

		for (String arg : args)
		{
			List<String> listCards = DouDiZhu.AnswerToCardRanksList (arg);
			EvaluateCards (listCards);
		}
	}
}
