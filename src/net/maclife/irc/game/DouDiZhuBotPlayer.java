package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;

public abstract class DouDiZhuBotPlayer extends TurnBasedBotPlayer
{
	public DouDiZhuBotPlayer (String name)
	{
		super (name);
	}

	/**
	 * 抢地主
	 * @param args 参数列表：
	 * <ol>
	 * 	<li>玩家手牌，数据类型： <code>List&lt;Map&lt;String, Object&gt;&gt;</code> ，其中的 Map 中的 Key 有：
	 * 		<ul>
	 * 			<li>"rank" -- 牌面（字符串）、</li>
	 * 			<li>"point" -- 点值（整数）、</li>
	 * 			<li>"suit" -- 花色（字符串）、</li>
	 * 			<li>"color" -- IRC 颜色</li>
	 *		</ul>
	 * 	</li>
	 * 	<li>候选答案，数据类型： <code>List&lt;String[]&gt;</code>，其中的 <code>String[]</code> 遵循 Dialog 类候选答案的格式：<code>[0]</code> 是 value，<code>[1]</code> 是 description </li>
	 * </ol>
	 * @return
	 */
	public abstract Object 抢地主 (Object... args);
	public Object 手牌变更 (Object listMyCurrentCards, String sReason)
	{
		return null;
	}

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
	public String 找出点数值最小的炸弹 (List<Map<String, Object>> player_cards)
	{
		return 找出点数值大于N的炸弹 (player_cards, 0);
	}

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
