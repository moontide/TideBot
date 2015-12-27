package net.maclife.irc.game;

public abstract class DouDiZhuBotPlayer extends TurnBasedBotPlayer
{
	public DouDiZhuBotPlayer (String name)
	{
		super (name);
	}
	public abstract Object 抢地主 (Object... params);
}
