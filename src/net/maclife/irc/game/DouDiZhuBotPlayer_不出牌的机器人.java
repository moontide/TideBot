package net.maclife.irc.game;

import java.util.*;

/**
 * 最简单的斗地主机器人玩家的实现：不出牌的机器人！！！
 * @author liuyan
 *
 */
public class DouDiZhuBotPlayer_不出牌的机器人 extends DouDiZhuBotPlayer
{
	public DouDiZhuBotPlayer_不出牌的机器人 (String name)
	{
		super (name);
	}

	@Override
	public Object 抢地主 (List<Map<String, Object>> listPlayerCards, List<String[]> listCandidateAnswers)
	{
		return "n";
	}

	@Override
	public String 出牌 (List<Map<String, Object>> listPlayerCards)
	{
		return "";
	}

	@Override
	public Object 回牌 (Object... params)
	{
		return "过";
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
}
