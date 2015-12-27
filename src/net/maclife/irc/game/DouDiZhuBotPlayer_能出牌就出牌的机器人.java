package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;

/**
 * 比不出牌的机器人稍微复杂一点的机器人：能出牌就出牌的机器人 / 见谁都打的的机器人！！！
 * 1. 如果抢地主，一定要 3 分
 * 2. 如果自己回合出牌，只出一个最小的单牌（其实是不出牌，依赖于本 IRCBot 对斗地主游戏的实现）
 * 3. 如果是回其他人的牌，则能打就打，不管是不是自己的队友（农民角色时）
 *    而且，不计算最优出牌方式、拆牌，只要找到一个能打的，就出牌
 *
 * 所以，这个也是一个傻机器人
 * @author liuyan
 *
 */
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

		int nPrimaryCardType1 = (int)mapLastPlayedCardsInfo.get ("PrimaryCardType");
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
				cards_to_reply = 找出点数值大于N的顺子 (player_cards, nMaxPoint);
				break;

			case 对:
				cards_to_reply = 找出点数值大于N的对 (player_cards, nMaxPoint);
				break;
			case 连对:
				cards_to_reply = 找出点数值大于N的连对 (player_cards, nMaxPoint);
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
				cards_to_reply = 找出点数值大于N的飞机 (player_cards, nMaxPoint);
				break;
			case 飞机带单:
				cards_to_reply = 找出点数值大于N的飞机带单 (player_cards, nMaxPoint);
				break;
			case 飞机带对:
				cards_to_reply = 找出点数值大于N的飞机带对 (player_cards, nMaxPoint);
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
				cards_to_reply = 找出点数值大于N的大飞机 (player_cards, nMaxPoint);
				break;
			case 大飞机带2单:
				cards_to_reply = 找出点数值大于N的大飞机带2单 (player_cards, nMaxPoint);
				break;
			case 大飞机带2对:
				cards_to_reply = 找出点数值大于N的大飞机带2对 (player_cards, nMaxPoint);
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
		}
		if (StringUtils.isNotEmpty (cards_to_reply))
			return cards_to_reply;
		bombcards = 找出最小点数值的炸弹 (player_cards);
		if (StringUtils.isNotEmpty (bombcards))
			return bombcards;

		return "N";
	}

	@Override
	public String 找出点数值大于N的单 (List<Map<String, Object>> player_cards, int n)
	{
		for (Map<String, Object> card : player_cards)
		{
			if ((int)card.get ("point") > n)
			{
				return (String)card.get ("rank");
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的顺子 (List<Map<String, Object>> player_cards, int n)
	{
		for (Map<String, Object> card : player_cards)
		{
			if ((int)card.get ("point") > n)
			{
				return (String)card.get ("rank");
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的对 (List<Map<String, Object>> player_cards, int n)
	{
		for (Map<String, Object> card : player_cards)
		{
			if ((int)card.get ("point") > n)
			{
				return (String)card.get ("rank");
			}
		}
		return null;
	}

	@Override
	public String 找出点数值大于N的连对 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的三 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的三带1 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的三带1对 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的飞机 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的飞机带单 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的飞机带对 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的四带2 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的四带2对 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机带2单 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机带2对 (List<Map<String, Object>> player_cards, int n)
	{

		return null;
	}

	@Override
	public String 找出点数值大于N的炸弹 (List<Map<String, Object>> player_cards, int n)
	{
		for (Map<String, Object> card : player_cards)
		{
			if ((int)card.get ("point") > n)
			{
				return (String)card.get ("rank");
			}
		}
		return null;
	}

	public String 找出最小点数值的炸弹 (List<Map<String, Object>> player_cards)
	{
		return 找出点数值大于N的炸弹 (player_cards, 0);
	}
}
