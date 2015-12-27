package net.maclife.irc.game;

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
	public Object 抢地主 (Object... params)
	{
		return "n";
	}

	@Override
	public Object 出牌 (Object... params)
	{
		return "";
	}

	@Override
	public Object 回牌 (Object... params)
	{
		return "过";
	}
}
