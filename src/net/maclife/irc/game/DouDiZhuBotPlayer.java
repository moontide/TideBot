package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;

public abstract class DouDiZhuBotPlayer extends TurnBasedBotPlayer
{
	public DouDiZhuBotPlayer (String name)
	{
		super (name);
	}
	public abstract Object 抢地主 (Object... args);

	public abstract String 找出点数值大于N的单 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public abstract String 找出点数值大于N的顺子 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=5 */);

	public abstract String 找出点数值大于N的对 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public abstract String 找出点数值大于N的连对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=3 */);

	public abstract String 找出点数值大于N的三 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public abstract String 找出点数值大于N的三带1 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public abstract String 找出点数值大于N的三带1对 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public abstract String 找出点数值大于N的飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */);
	public abstract String 找出点数值大于N的飞机带单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */);
	public abstract String 找出点数值大于N的飞机带对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */);

	public abstract String 找出点数值大于N的四带2 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public abstract String 找出点数值大于N的四带2对 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public abstract String 找出点数值大于N的大飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */);
	public abstract String 找出点数值大于N的大飞机带2单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */);
	public abstract String 找出点数值大于N的大飞机带2对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength /* >=2 */);

	public abstract String 找出点数值大于N的炸弹 (List<Map<String, Object>> player_cards, int nMaxPoint);
	public String 找出王炸 (List<Map<String, Object>> player_cards)
	{
		boolean 有小王 = false;
		boolean 有大王 = false;
		for (int i=player_cards.size () - 1; i>0; i--)
		{
			Map<String, Object> card = player_cards.get (i);
			if (StringUtils.equalsIgnoreCase ((String)card.get ("rank"), "☆"))
			{
				有小王 = true;
			}
			if (StringUtils.equalsIgnoreCase ((String)card.get ("rank"), "★"))
			{
				有大王 = true;
			}
			if (有小王 && 有大王)
			{
				return "☆★";
			}
		}
		return null;
	}
}
