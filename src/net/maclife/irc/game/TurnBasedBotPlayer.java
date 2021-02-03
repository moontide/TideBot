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

	/**
	 * 在玩家回合阶段，需要玩家发起一轮出牌时，将调用该函数执行。
	 * @param params 参数列表。参数个数和参数类型将在不同的游戏中自行定义和解释，具体需要参考各游戏 BotPlayer 类的文档。
	 * @return 返回值和返回值的数据类型将在不同的游戏中自行定义和解释，具体需要参考各游戏 BotPlayer 类的文档。
	 */
	public abstract Object 出牌 (Object... params);

	/**
	 * 在别的玩家回合阶段出牌后，需要挨个询问其他玩家要回什么牌时，将调用该函数执行。
	 * @param params 参数列表。参数个数和参数类型将在不同的游戏中自行定义和解释，具体需要参考各游戏 BotPlayer 类的文档。
	 * @return 返回值和返回值的数据类型将在不同的游戏中自行定义和解释，具体需要参考各游戏 BotPlayer 类的文档。
	 */
	public abstract Object 回牌 (Object... params);
}
