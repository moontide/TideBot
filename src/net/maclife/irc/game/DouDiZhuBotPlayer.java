package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;

public abstract class DouDiZhuBotPlayer extends TurnBasedBotPlayer
{
	public DouDiZhuBotPlayer (String name)
	{
		super (name);
	}

	@Override
	public Object 出牌 (Object... args)
	{
		if (args.length < 1)
			throw new IllegalArgumentException ("斗地主的游戏机器人，出牌 函数至少需要 1 个参数： 1.自己剩余的手牌");

		List<Map<String, Object>> listPlayerCards = (List<Map<String, Object>>) args[0];
		return 出牌 (listPlayerCards);
	}

	/**
	 * 玩家回合阶段，发起一轮出牌。
	 * @param listPlayerCards 玩家当前手牌
	 * @return 所出的一道牌，String 类型。如果是单张牌，如果是多张牌。返回 null 表示让系统自己出一张牌（出一张点值最小的单牌）
	 */
	public abstract String 出牌 (List<Map<String, Object>> listPlayerCards);

	@Override
	public abstract Object 回牌 (Object... params);

	/**
	 * 抢地主
	 * @param args 参数列表：
	 * <ol>
	 * 	<li>玩家手牌列表，数据类型： <code>List&lt;Map&lt;String, Object&gt;&gt;</code> ，其中的 Map 中的 Key 有：
	 * 		<ul>
	 * 			<li>"rank" -- 牌面（字符串）、</li>
	 * 			<li>"point" -- 点值（整数）、</li>
	 * 			<li>"suit" -- 花色（字符串）、</li>
	 * 			<li>"color" -- IRC 颜色</li>
	 *		</ul>
	 * 	</li>
	 * 	<li>候选答案，数据类型： <code>List&lt;String[]&gt;</code>，其中的 <code>String[]</code> 遵循 Dialog 类候选答案的格式：<code>[0]</code> 是 value，<code>[1]</code> 是 description </li>
	 * </ol>
	 * @return 抢地主的回答，字符串类型。
	 * <dl>
	 * 	<dt><code>N</code> 或 <code>不抢</code>  或空字符串(超时未回答时)</dt>
	 * 	<dd>不抢</dd>
	 * 	<dt><code>1</code> 或 <code>1分</code></dt>
	 * 	<dd>1 分，回答 1 后，将继续询问其他玩家有没有回答更高数值的，如果都没有，则结束抢地主阶段，并成为地主。</dd>
	 * 	<dt><code>2</code> 或 <code>2分</code></dt>
	 * 	<dd>2 分，回答 2 后，将继续询问其他玩家有没有回答更高数值的，如果都没有，则结束抢地主阶段，并成为地主。</dd>
	 * 	<dt><code>3</code> 或 <code>3分</code></dt>
	 * 	<dd>3 分，回答 3 后，则结束抢地主，立刻成为地主。</dd>
	 * </dl>
	 */
	public abstract Object 抢地主 (List<Map<String, Object>> listPlayerCards, List<String[]> listCandidateAnswers);
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
