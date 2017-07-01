package net.maclife.irc.game.sanguosha;

import java.util.*;

import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.game.*;
import net.maclife.irc.game.sanguosha.SanGuoSha.*;
import net.maclife.irc.game.sanguosha.card.*;
import net.maclife.irc.game.sanguosha.character.*;

public class SanGuoShaPlayer extends IRCPlayer
{
	public SanGuoShaPlayer (String name)
	{
		super (name);
	}

	/**
	 * 玩家所拥有的武将。对于国战或者其他玩法，武将可能不止一个。
	 */
	List<PlayerCharacter> listCharacters;

	/**
	 * 对其他玩家的标记（身份、国家）。
	 * 标记有几种：
	 * 1. 手工标记（猜测），这个与官方游戏类似
	 * 2. 玩家自己亮了角色/国家
	 */
	Map<String, Object> mapMarksForOtherPlayers;
	/**
	 * 对其他玩家的逻辑标记（身份、国家），这里的逻辑标记是指：国战中玩家A看了玩家B之后，如果玩家A攻击玩家B，则可标记“A与B不是一个国家”
	 */
	Map<String, Object> mapLogicalMarksForOtherPlayers;

	SanGuoSha_RoleRevealing.三国杀身份 身份 = null;
	SanGuoSha.国家势力 国家势力 = null;

	@Override
	public String toString ()
	{
		if (身份==null && 国家势力==null)
			return getName();

		StringBuilder sb = new StringBuilder ();
		if (身份 != null)
		{
			switch (身份)
			{
				case 主公:
					break;
				case 内奸:
					break;
				case 忠臣:
					break;
				case 反贼:
					break;
			}
		}
		if (国家势力 != null)
		{
			switch (国家势力)
			{
				case 魏:
					//sIRCColor = Colors.BLUE;
					//sANSIColor="\u001B[34;1m";
					break;
				case 蜀:
					//sIRCColor = ANSIEscapeTool.COLOR_DARK_RED;
					//sANSIColor="\u001B[31m";
					//sIRCColor = ANSIEscapeTool.COLOR_ORANGE;
					//sANSIColor="\u001B[33m";
					break;
				case 吴:
					//sIRCColor = Colors.DARK_GREEN;
					//sANSIColor="\u001B[32m";
					break;
				case 群:
					//sIRCColor = Colors.WHITE;
					//sANSIColor="\u001B[1m";
					break;
			}
		}
		sb.append (getName());
		return sb.toString ();
	}

	public void 设置身份 (SanGuoSha_RoleRevealing.三国杀身份 新身份)
	{
		身份 = 新身份;
	}

	public void 设置国家 (SanGuoSha.国家势力 新国家势力)
	{
		国家势力 = 新国家势力;
	}

	public boolean isDead ()
	{
		return false;
	}

	public boolean 拥有此阶段技能吗 (PLAY_TURN_STAGE_玩家回合阶段 阶段)
	{
		return false;
	}
}
