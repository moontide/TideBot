package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;

/**
 * 比不出牌的机器人稍微复杂一点的机器人：能出牌就出牌的机器人 / 见谁都打的的机器人！！！
 * 1. 如果抢地主，一定要 3 分
 * 2. 如果自己回合出牌，
 *    <s>只出一个最小的单牌（其实是不出牌，依赖于本 IRCBot 对斗地主游戏的实现）</s>
 *    只从单牌、对牌、三牌组、四牌组中 顺序，简单的出 1 组牌 -- 不出三带1、三带2、四带2、四带2对之类的组合牌
 * 3. 如果是回其他人的牌，则能打就打，不管是不是自己的队友（农民角色时）
 *    但是，不计算最优出牌方式、不拆牌，只按牌型找，找到一个能打的，就出牌
 *
 * 所以，这个也是一个傻机器人。
 * 因为谁都打，所以，建议让本机器人当地主。
 * @author liuyan
 *
 */
@SuppressWarnings ({"unused", "unchecked"})
public class DouDiZhuBotPlayer_能出牌就出牌的机器人 extends DouDiZhuBotPlayer
{
	public DouDiZhuBotPlayer_能出牌就出牌的机器人 (String name)
	{
		super (name);
	}

	@Override
	public Object 抢地主 (Object... params)
	{
		return "3";
	}

	@Override
	public Object 出牌 (Object... params)
	{
		if (params.length < 1)
			throw new IllegalArgumentException ("斗地主的游戏机器人，出牌 函数至少需要 1 个参数： 1.自己剩余的手牌");

		List<Map<String, Object>> player_cards = (List<Map<String, Object>>) params[0];
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
		List<String> listPairCards = (List<String>) mapCardsInfo.get ("PairCards");
		List<String> listTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
		List<String> listQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

		if (listSoloCards.size () > 0)
			return listSoloCards.get (0);
		else if (listPairCards.size () > 0)
			return listPairCards.get (0) + listPairCards.get (0);
		else if (listTrioCards.size () > 0)
			return listTrioCards.get (0) + listTrioCards.get (0) + listTrioCards.get (0);
		else if (listQuartetteCards.size () > 0)
			return listQuartetteCards.get (0) + listQuartetteCards.get (0) + listQuartetteCards.get (0) + listQuartetteCards.get (0);

		// 依赖于本 IRCBot 对斗地主游戏的实现：不出牌时，自动帮玩家出一张牌。
		return null;
	}

	@Override
	/**
	 * 根据别人所出的牌的牌型+张数，从自己的手牌中找出比其牌型大的牌打出
	 */
	public Object 回牌 (Object... params)
	{
		if (params.length < 4)
			throw new IllegalArgumentException ("斗地主的游戏机器人，回牌 函数至少需要 4 个参数： 1.别的玩家出的牌 2.别的玩家出的牌统计信息 3.别的玩家出的牌型 4.自己剩余的手牌");
		List<String> listLastPlayedCardRanks = (List<String>) params[0];
		Map<String, Object> mapLastPlayedCardsInfo = (Map<String, Object>) params[1];
		DouDiZhu.Type 别的玩家出的牌型 = (DouDiZhu.Type) params[2];
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>) params[3];

		int nPrimaryCardType = (int)mapLastPlayedCardsInfo.get ("PrimaryCardType");
		int nMaxPoint = (int)mapLastPlayedCardsInfo.get ("MaxPoint");
		int nSolo = (int)mapLastPlayedCardsInfo.get ("nSolo");
		int nPair = (int)mapLastPlayedCardsInfo.get ("nPair");
		int nTrio = (int)mapLastPlayedCardsInfo.get ("nTrio");
		int nQuartette = (int)mapLastPlayedCardsInfo.get ("nQuartette");
		boolean isBomb = (boolean)mapLastPlayedCardsInfo.get ("IsBomb");

		String cards_to_reply = null;
		String bombcards = null;

		// 王炸，斗地主中的最大牌，肯定打不过，不再找牌打这道牌，直接过
		if (别的玩家出的牌型 == DouDiZhu.Type.王炸)
			return "过";
		// 然后，处理普通牌型（非炸弹的牌型，非炸弹的牌型都可以用炸弹打）、炸弹牌型
		switch (别的玩家出的牌型)
		{
			case 单:
				cards_to_reply = 找出点数值大于N的单 (player_cards, nMaxPoint);
				break;
			case 顺子:
				cards_to_reply = 找出点数值大于N的顺子 (player_cards, nMaxPoint, nSolo);
				break;

			case 对:
				cards_to_reply = 找出点数值大于N的对 (player_cards, nMaxPoint);
				break;
			case 连对:
				cards_to_reply = 找出点数值大于N的连对 (player_cards, nMaxPoint, nPair);
				break;

			case 三:
				cards_to_reply = 找出点数值大于N的三 (player_cards, nMaxPoint);
				break;
			case 三带1:
				cards_to_reply = 找出点数值大于N的三带1 (player_cards, nMaxPoint);
				break;
			case 三带1对:
				cards_to_reply = 找出点数值大于N的三带1对 (player_cards, nMaxPoint);
				break;
			case 飞机:
				cards_to_reply = 找出点数值大于N的飞机 (player_cards, nMaxPoint, nTrio);
				break;
			case 飞机带单:
				cards_to_reply = 找出点数值大于N的飞机带单 (player_cards, nMaxPoint, nTrio);
				break;
			case 飞机带对:
				cards_to_reply = 找出点数值大于N的飞机带对 (player_cards, nMaxPoint, nTrio);
				break;

			//case 四:
			//	break;
			case 四带2:
				cards_to_reply = 找出点数值大于N的四带2 (player_cards, nMaxPoint);
				break;
			case 四带2对:
				cards_to_reply = 找出点数值大于N的四带2对 (player_cards, nMaxPoint);
				break;
			case 大飞机:
				cards_to_reply = 找出点数值大于N的大飞机 (player_cards, nMaxPoint, nQuartette);
				break;
			case 大飞机带2单:
				cards_to_reply = 找出点数值大于N的大飞机带2单 (player_cards, nMaxPoint, nQuartette);
				break;
			case 大飞机带2对:
				cards_to_reply = 找出点数值大于N的大飞机带2对 (player_cards, nMaxPoint, nQuartette);
				break;

			case 炸弹:
				cards_to_reply = 找出点数值大于N的炸弹 (player_cards, nMaxPoint);
				if (StringUtils.isNotEmpty (cards_to_reply))
					return cards_to_reply;
				else
				{
					cards_to_reply = 找出王炸 (player_cards);
					if (StringUtils.isNotEmpty (cards_to_reply))
						return cards_to_reply;
					return "过";	// 必须在这里返回“过”牌，因为后面的还有针对【普通牌】找【炸弹牌】打的处理
				}
			default:
				return "过";
		}

		// 如果找到同牌型的牌，则出牌
		if (StringUtils.isNotEmpty (cards_to_reply))
			return cards_to_reply;

		// 再不行就找炸弹
		bombcards = 找出最小点数值的炸弹 (player_cards);
		if (StringUtils.isNotEmpty (bombcards))
			return bombcards;

		// 还不行就找王炸
		bombcards = 找出王炸 (player_cards);
		if (StringUtils.isNotEmpty (bombcards))
			return bombcards;

		return "过";
	}

	@Override
	public String 找出点数值大于N的单 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		//// 方案 1（会产生拆牌的情况）：单是最小单位，所以，只要能找到任何一个点数大于 nMaxPoint 的牌，就出这张牌
		//for (Map<String, Object> card : player_cards)
		//{
		//	if ((int)card.get ("point") > nMaxPoint)
		//	{
		//		return (String)card.get ("rank");
		//	}
		//}

		// 方案 2：只从单牌里找（但这样也会把顺子给拆掉，因为顺子在本 Bot 里当作单牌处理的）
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("SoloCards");

		if (listCards.isEmpty ())
			return null;

		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				return rank;
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的顺子 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=5 */)
	{
		if (nSerialLength < 5)
			throw new IllegalArgumentException ("你他喵的在逗我，顺子少于 5 张牌");
		if (player_cards.size () < nSerialLength * 1)	// 剩余牌数不足：剩余牌数 < (顺子长度 * 1)，不考虑炸弹、王炸的情况，肯定打不过
			return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("SoloCards");

		if (listCards.size() < nSerialLength)
			return null;

		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				return 生成用于出牌的顺子 (listSubCards, 1);
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		if (player_cards.size () == 1)	// 只剩下 1 张牌，肯定打不过
			return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("PairCards");

		if (listCards.isEmpty ())
			return null;

		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				return rank + rank;
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的连对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=3 */)
	{
		if (nSerialLength < 3)
			throw new IllegalArgumentException ("你他喵的在逗我：连对序列长度少于 3");
		if (player_cards.size () < nSerialLength * 2)	// 剩余牌数 < (顺子长度 * 2)，不考虑炸弹、王炸的情况，肯定打不过
			return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("PairCards");

		if (listCards.size () < nSerialLength)
			return null;

		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				return 生成用于出牌的顺子 (listSubCards, 2);
			}
		}

		return null;
	}

	@Override
	public String 找出点数值大于N的三 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		if (player_cards.size () < 3)	// 剩余牌数不足，不考虑炸弹、王炸的情况，肯定打不过
			return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("TrioCards");

		if (listCards.isEmpty ())
			return null;

		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				return rank + rank + rank;
			}
		}

		return null;
	}

	@Override
	public String 找出点数值大于N的三带1 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		if (player_cards.size () < 4)	// 剩余牌数不足，不考虑炸弹、王炸的情况，肯定打不过
			return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("TrioCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("SoloCards");

		if (listCards.isEmpty () || listAttachmentCards.isEmpty ())
			return null;

		String sTrio = null;
		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				sTrio = rank + rank + rank;
			}
		}
		if (sTrio == null)
			return null;

		return sTrio + listAttachmentCards.get (0);
	}

	@Override
	public String 找出点数值大于N的三带1对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		if (player_cards.size () < 5)	// 剩余牌数不足，不考虑炸弹、王炸的情况，肯定打不过
			return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("TrioCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("PairCards");

		if (listCards.isEmpty () || listAttachmentCards.isEmpty ())
			return null;

		String sTrio = null;
		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				sTrio = rank + rank + rank;
			}
		}
		if (sTrio == null)
			return null;

		return sTrio + listAttachmentCards.get (0) + listAttachmentCards.get (0);
	}

	@Override
	public String 找出点数值大于N的飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */)
	{
		if (nSerialLength < 2)
			throw new IllegalArgumentException ("你他喵的在逗我：飞机序列长度少于 2");
		if (player_cards.size () < nSerialLength * 3)	// 剩余牌数 < (顺子长度 * 3)，不考虑炸弹、王炸的情况，肯定打不过
			return null;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("TrioCards");

		if (listCards.size () < nSerialLength)
			return null;

		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				return 生成用于出牌的顺子 (listSubCards, 3);
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的飞机带单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */)
	{
		if (nSerialLength < 2)
			throw new IllegalArgumentException ("你他喵的在逗我：飞机序列长度少于 2");
		if (player_cards.size () < nSerialLength * (3 + 1))	// 剩余牌数 < (顺子长度 * 4)，不考虑炸弹、王炸的情况，肯定打不过
			return null;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("TrioCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("SoloCards");

		if (listCards.size () < nSerialLength || listAttachmentCards.size () < nSerialLength)
			return null;

		String sTrioSerial = null;
		StringBuilder sbCards = new StringBuilder ();
		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				sTrioSerial = 生成用于出牌的顺子 (listSubCards, 3, sbCards);
			}
		}
		if (sTrioSerial == null)
			return null;
		for (int i=0; i<nSerialLength; i++)
			sbCards.append (listAttachmentCards.get (i));
		return sbCards.toString ();
	}

	@Override
	public String 找出点数值大于N的飞机带对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */)
	{
		if (nSerialLength < 2)
			throw new IllegalArgumentException ("你他喵的在逗我：飞机序列长度少于 2");
		if (player_cards.size () < nSerialLength * (3 + 2))	// 剩余牌数 < (顺子长度 * 5)，不考虑炸弹、王炸的情况，肯定打不过
			return null;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("TrioCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("PairCards");

		if (listCards.size () < nSerialLength || listAttachmentCards.size () < nSerialLength)
			return null;

		String sTrioSerial = null;
		StringBuilder sbCards = new StringBuilder ();
		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				sTrioSerial = 生成用于出牌的顺子 (listSubCards, 3, sbCards);
			}
		}
		if (sTrioSerial == null)
			return null;
		for (int i=0; i<nSerialLength; i++)
		{
			sbCards.append (listAttachmentCards.get (i));
			sbCards.append (listAttachmentCards.get (i));
		}
		return sbCards.toString ();
	}

	@Override
	public String 找出点数值大于N的四带2 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		if (player_cards.size () < 5)	// 剩余牌数不足，不考虑炸弹、王炸的情况，肯定打不过
				return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("QuartetteCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("SoloCards");

		if (listCards.size () < 2 || listAttachmentCards.size () < 2)
			return null;

		String sQuartette = null;
		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				sQuartette = rank + rank + rank + rank;
			}
		}
		if (sQuartette == null)
			return null;

		return sQuartette + listAttachmentCards.get (0) + listAttachmentCards.get (1);
	}

	@Override
	public String 找出点数值大于N的四带2对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		if (player_cards.size () < 6)	// 剩余牌数不足，不考虑炸弹、王炸的情况，肯定打不过
				return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("QuartetteCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("PairCards");

		if (listCards.size () < 2 || listAttachmentCards.size () < 2)
			return null;

		String sQuartette = null;
		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				sQuartette = rank + rank + rank + rank;
			}
		}
		if (sQuartette == null)
			return null;

		return sQuartette + listAttachmentCards.get (0) + listAttachmentCards.get (0) + listAttachmentCards.get (1) + listAttachmentCards.get (1);
	}

	@Override
	public String 找出点数值大于N的大飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */)
	{
		if (nSerialLength < 2)
			throw new IllegalArgumentException ("你他喵的在逗我：大飞机序列长度少于 2");
		if (player_cards.size () < nSerialLength * 4)	// 剩余牌数 < (顺子长度 * 4)，不考虑炸弹、王炸的情况，肯定打不过
			return null;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

		if (listCards.size () < nSerialLength)
			return null;

		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				return 生成用于出牌的顺子 (listSubCards, 4);
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机带2单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */)
	{
		if (nSerialLength < 2)
			throw new IllegalArgumentException ("你他喵的在逗我：大飞机序列长度少于 2");
		if (player_cards.size () < nSerialLength * (4 + 2))	// 剩余牌数 < (顺子长度 * 6)，不考虑炸弹、王炸的情况，肯定打不过
			return null;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("QuartetteCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("SoloCards");

		if (listCards.size () < nSerialLength || listAttachmentCards.size () < nSerialLength * 2)
			return null;

		String sQuartetteSerial = null;
		StringBuilder sbCards = new StringBuilder ();
		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				sQuartetteSerial = 生成用于出牌的顺子 (listSubCards, 3, sbCards);
			}
		}
		if (sQuartetteSerial == null)
			return null;
		for (int i=0; i<nSerialLength * 2; i++)
		{
			sbCards.append (listAttachmentCards.get (i));
		}
		return sbCards.toString ();
	}

	@Override
	public String 找出点数值大于N的大飞机带2对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */)
	{
		if (nSerialLength < 2)
			throw new IllegalArgumentException ("你他喵的在逗我：大飞机序列长度少于 2");
		if (player_cards.size () < nSerialLength * (4 + 4))	// 剩余牌数 < (顺子长度 * 8)，不考虑炸弹、王炸的情况，肯定打不过
			return null;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("QuartetteCards");
		List<String> listAttachmentCards = (List<String>) mapCardsInfo.get ("PairCards");

		if (listCards.size () < nSerialLength || listAttachmentCards.size () < nSerialLength * 2)
			return null;

		String sQuartetteSerial = null;
		StringBuilder sbCards = new StringBuilder ();
		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return null;

			List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				sQuartetteSerial = 生成用于出牌的顺子 (listSubCards, 3, sbCards);
			}
		}
		if (sQuartetteSerial == null)
			return null;
		for (int i=0; i<nSerialLength * 2; i++)
		{
			sbCards.append (listAttachmentCards.get (i));
			sbCards.append (listAttachmentCards.get (i));
		}
		return sbCards.toString ();
	}

	@Override
	public String 找出点数值大于N的炸弹 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		if (player_cards.size () < 4)	// 剩余牌数不足，不考虑炸弹、王炸的情况，肯定打不过
			return null;
		Map<String, Object> mapCardsInfo = DouDiZhu.CalculatePlayerCards (player_cards);
		List<String> listCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

		if (listCards.isEmpty ())
			return null;

		for (String rank : listCards)
		{
			if (DouDiZhu.RankToPoint (rank) > nMaxPoint)
			{
				return rank + rank + rank + rank;
			}
		}
		return null;
	}

	public String 找出最小点数值的炸弹 (List<Map<String, Object>> player_cards)
	{
		return 找出点数值大于N的炸弹 (player_cards, 0);
	}

	/**
	 * 生成用于出牌的顺子
	 * @param cards 一个顺子 -- 调用者必须确保是顺子
	 * @param nCopy 多少个拷贝， 1：就是单顺子  2：就是连对（两牌组顺子）  3：就是飞机（三牌组顺子）  4：就是大飞机（四牌组顺子）
	 * @return
	 */
	public static String 生成用于出牌的顺子 (List<String> cards, int nCopy, StringBuilder sbCards)
	{
		if (sbCards == null)
			sbCards = new StringBuilder ();
		for (String rank : cards)
		{
			/**
			 * ！！！警告！！！
			 * 这里的 case 没加/不需要加 break ！
			 * 可用来当做不加 break 的 switch case 例子
			 */
			switch (nCopy)
			{
			case 4:
				sbCards.append (rank);
			case 3:
				sbCards.append (rank);
			case 2:
				sbCards.append (rank);
			case 1:
			default:
				sbCards.append (rank);
				break;
			}
		}
		return sbCards.toString ();
	}
	public static String 生成用于出牌的顺子 (List<String> cards, int nCopy)
	{
		return 生成用于出牌的顺子 (cards, nCopy, null);
	}
	public static String 生成用于出牌的顺子 (List<String> cards, StringBuilder sbCards)
	{
		return 生成用于出牌的顺子 (cards, 1, sbCards);
	}
	public static String 生成用于出牌的顺子 (List<String> cards)
	{
		return 生成用于出牌的顺子 (cards, 1, null);
	}
}
