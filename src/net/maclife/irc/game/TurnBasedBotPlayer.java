package net.maclife.irc.game;

/**
 * TurnBasedBotPlayer，回合制“机器人玩家”
 * @author liuyan
 *
 */
public abstract class TurnBasedBotPlayer extends BotPlayer
{
	public TurnBasedBotPlayer (String name)
	{
		super (name);
	}

	public abstract Object 出牌 (Object... params);

	public abstract Object 回牌 (Object... params);
}
