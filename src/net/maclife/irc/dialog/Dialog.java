package net.maclife.irc.dialog;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.irc.*;

public class Dialog implements Runnable
{
	public long threadID = 0;
	public boolean running = false;
	public long starttime = 0;
	public long endtime = 0;
	public long timeout_second = 60;
	List<Dialog> dialogs = null;

	// 对话发起人信息
	String channel;
	String nick;
	String login;
	String host;
	String botcmd;
	String botCmdAlias;
	Map<String, Object> mapGlobalOptions;
	List<String> listCmdEnv;
	String params;

	public Lock lock;
	public Condition endCondition;

	int type;	// MessageBox (只有‘确认’) QuestionDialog(YES/NO,  自定义答案)
	//
	List<String> participants = new ArrayList<String> ();	// 受访者
	List<String> candidateAnswers;	// 候选答案
	String question;
	String answer;

	LiuYanBot bot;

	public Dialog (LiuYanBot bot, List<Dialog> listDialogs, String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		this.bot = bot;
		this.dialogs = listDialogs;
		listDialogs.add (this);

		this.channel = ch;
		this.nick = nick;
		this.login = login;
		this.host = hostname;
		this.botcmd = botcmd;
		this.botCmdAlias = botCmdAlias;
		this.mapGlobalOptions = mapGlobalOptions;
		this.listCmdEnv = listCmdEnv;
		this.params = params;
		question = params;

		String participant = nick;
		boolean opt_reply_to_option_on = (boolean)mapGlobalOptions.get("opt_reply_to_option_on");
		String opt_reply_to = (String)mapGlobalOptions.get("opt_reply_to");
		if (opt_reply_to_option_on && !StringUtils.equalsIgnoreCase (participant, opt_reply_to))
			participant = opt_reply_to;
		participants.add (participant);
		System.out.println ("参与者: " + participants);
	}

	/**
	 * 有人尝试应答对话框
	 * @param ch 哪个频道
	 * @param n 哪个昵称
	 * @param u 哪个帐号
	 * @param host 哪个主机名
	 * @param answer 提供的答案
	 * @return true - 当用户属于对话框的参与者。 false - 用户不属于对话框的参与者
	 * @throws RuntimeException
	 */
	public boolean onAnswerReceived (String ch, String n, String u, String host, String answer) throws RuntimeException
	{
		if (! participants.contains (n))
		{
			throw new RuntimeException ("您不是本对话框的参与者");
		}
		this.answer = answer;
		try
		{
			lock.lock ();
			endCondition.signal ();
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		finally
		{
			lock.unlock ();
		}
		return true;
	}

	@Override
	public void run ()
	{
		threadID = Thread.currentThread ().getId ();
		System.out.println ("Dialog #" + threadID + " started");
		bot.SendMessage (channel, nick, mapGlobalOptions, "Dialog #" + threadID + " (" + Colors.BOLD + question + Colors.BOLD + ") started. 请通过 " + Colors.BOLD + bot.getNick () + ": <答案>" + Colors.BOLD + " 的方式来回答问题，您有 " + timeout_second + " 秒钟的回答时间");
		starttime = System.currentTimeMillis ();

		// prepare lock
		lock = new ReentrantLock ();
		endCondition = lock.newCondition ();
		try
		{
			//System.out.println (dialogs);
			lock.lock ();
			System.out.println ("Dialog #" + threadID + " is waiting for answers or timeout...");
			endCondition.await (timeout_second, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		finally
		{
			lock.unlock ();
		}
		endtime = System.currentTimeMillis ();
		System.out.println ("Dialog #" + threadID + " ended. cost " + (endtime - starttime)/1000 + " s");
		bot.SendMessage (channel, nick, mapGlobalOptions, "Dialog #" + threadID + " ended. cost " + (endtime - starttime)/1000 + " s." + (StringUtils.isEmpty (answer) ? "" : " 获得答案: " + Colors.BOLD + answer + Colors.BOLD));
		dialogs.remove (this);
	}
}
