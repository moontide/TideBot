package net.maclife.irc.dialog;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.irc.*;

public class Dialog implements Callable<Map<String, Object>>
{
	public static final int MESSAGE_TARGET_MASK_PM      = 1;
	public static final int MESSAGE_TARGET_MASK_CHANNEL = 2;

	public static final boolean SHOW_MESSAGE            = true;
	public static final boolean DO_NOT_SHOW_MESSAGE     = false;

	public long threadID = 0;
	public long starttime = 0;
	public long endtime = 0;
	public long timeout_second = 60;
	//public int maxAdviseTimes = 0;	// 答案校正次数。 正整数: 指定次数; 0 或者负数: 无限次数
	List<Dialog> dialogs = null;

	//
	String question;
	boolean isParticipantsQuantityIndefinitely = false;	// 参与者人数是不是不确定的。比如，一个频道内所有人都能参加的对话框，参与人数就是不确定的
	List<String> participants = new ArrayList<String> ();	// 参与者
	List<String[]> candidateAnswers = new ArrayList<String[]> ();	// 候选答案
	public boolean useHostAsAnswersKey = false;	// 使用 host 作为 participantAnswers 的 key。这在“参与者人数不确定”时，能够起到避免用户故意更改昵称设置多个答案
	Map<String, Object> participantAnswers = new HashMap<String, Object> ();

	public boolean showQuestion = true;
		public boolean showType = false;
		public boolean showCandidateAnswers = true;
		public boolean showUsage = true;

	LiuYanBot bot;

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
	Object[] arrayDialogUserArguments = null;
	int msgTargetMask = MESSAGE_TARGET_MASK_PM;

	public Lock lock;
	public Condition endCondition;

	DialogUser dlguser = null;
	Type type = Type.开放;	// MessageBox (只有‘确认’) QuestionDialog(YES/NO,  自定义答案)
	public enum Type
	{
		开放,	/* 任意答案 */
		单选,
		多选,
		确认,
		是否,
	}
	public static Dialog.Type parseType (String s)
	{
		if (StringUtils.equalsIgnoreCase (s, "单选") || StringUtils.equalsIgnoreCase (s, "option"))
			return Type.单选;
		else if (StringUtils.equalsIgnoreCase (s, "多选") || StringUtils.equalsIgnoreCase (s, "check"))
			return Type.多选;
		else if (StringUtils.equalsIgnoreCase (s, "确认") || StringUtils.equalsIgnoreCase (s, "confirm"))
			return Type.确认;
		else if (StringUtils.equalsIgnoreCase (s, "是否") || StringUtils.equalsIgnoreCase (s, "yesno"))
			return Type.是否;

		return Type.开放;
	}

	public Dialog (DialogUser dlgUser, LiuYanBot bot, List<Dialog> listDialogs, Dialog.Type qt, String q, boolean isShowQuestion, int messageTargetMask, Object participants, List<String[]> listCandidateAnswers,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params, Object... arrayDialogUserArguments)
	{
		this.dlguser = dlgUser;
		this.bot = bot;
		this.dialogs = listDialogs;
		listDialogs.add (this);
		type = (qt==null ? Type.开放 : qt);
		question = q;	// (StringUtils.endsWithAny (q, "?", "？") ? q : q + "?");	// 如果问题不是以问号结尾，则在问题后面加上问号
		this.showQuestion = isShowQuestion;
		this.msgTargetMask = messageTargetMask;
		if (participants instanceof Collection<?>)
			this.participants.addAll ((Collection<String>)participants);
		else if (participants instanceof String)
		{
			if ( ((String)participants).equals ("*") )
				isParticipantsQuantityIndefinitely = true;
			this.participants.add ((String)participants);
		}
		else
			throw new IllegalArgumentException ("Dialog 参与者参数的数据类型不符合要求，必须为 List<String> (多人) 或 String (单人)");

		if (type == Type.单选 || type == Type.多选)
		{
			candidateAnswers.addAll (listCandidateAnswers);
		}
		else if (type == Type.确认)
		{
			candidateAnswers.add (new String[]{"1", "确认"});
			candidateAnswers.add (new String[]{"2", "取消"});
		}
		else if (type == Type.是否)
		{
			candidateAnswers.add (new String[]{"1", "是"});
			candidateAnswers.add (new String[]{"2", "否"});
		}

		this.channel = ch;
		this.nick = nick;
		this.login = login;
		this.host = hostname;
		this.botcmd = botcmd;
		this.botCmdAlias = botCmdAlias;
		this.mapGlobalOptions = mapGlobalOptions;
		this.listCmdEnv = listCmdEnv;
		this.params = params;
		this.arrayDialogUserArguments = arrayDialogUserArguments;

		//String participant = nick;
		//boolean opt_reply_to_option_on = (boolean)mapGlobalOptions.get("opt_reply_to_option_on");
		//String opt_reply_to = (String)mapGlobalOptions.get("opt_reply_to");
		//if (opt_reply_to_option_on && !StringUtils.equalsIgnoreCase (participant, opt_reply_to))
		//	participant = opt_reply_to;
		//participants.add (participant);
System.out.println ("问题类型: " + type);
System.out.println ("问题: " + question);
System.out.println ("参与者: " + participants);
		StringBuilder sb = new StringBuilder ();
		for (String[] ca : candidateAnswers)
		{
			sb.append (Arrays.toString (ca));
			sb.append (", ");
		}
System.out.println ("候选答案: " + sb);
	}
	public Dialog (DialogUser dlgUser, LiuYanBot bot, List<Dialog> listDialogs, Dialog.Type qt, String q, boolean isShowQuestion, Object participants, List<String[]> listCandidateAnswers,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params, Object... arrayDialogUserArguments)
	{
		this (dlgUser, bot, listDialogs, qt, q, isShowQuestion, MESSAGE_TARGET_MASK_PM, participants, listCandidateAnswers,
			ch, nick, login, hostname,
			botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params,
			arrayDialogUserArguments
			);
	}
	/**
	 * 开放题构造函数
	 * @param dlgUser
	 * @param bot
	 * @param listDialogs
	 * @param q
	 * @param isShowQuestion
	 * @param participants
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	public Dialog (DialogUser dlgUser, LiuYanBot bot, List<Dialog> listDialogs, String q, boolean isShowQuestion, Object participants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params, Object... arrayDialogUserArguments)
	{
		this (dlgUser, bot, listDialogs, Type.开放, q, isShowQuestion, participants, null,
			ch, nick, login, hostname,
			botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params,
			arrayDialogUserArguments
			);
	}

	/**
	 * 检查候选答案的有效性
	 * <ul>
	 * 	<li>候选答案 不能为空</li>
	 * 	<li>候选答案值 (value) 不能为空</li>
	 * 	<li>候选答案 不能有重复，包括 数值(value) 和 标签(label)</li>
	 * </ul>
	 * @param ca
	 * @param listCandidateAnswers
	 */
	public static void ValidateCandidateAnswer (String[] ca, List<String[]> listCandidateAnswers)
	{
		if (ca == null || ca.length == 0)
			throw new IllegalArgumentException ("答案不能为空");
		if (StringUtils.isEmpty (ca[0]))
			throw new IllegalArgumentException ("答案值不能为空");
		//if (StringUtils.isEmpty (ca[1]))
		//	throw new IllegalArgumentException ("答案标签不能为空");
		for (String[] caExisted : listCandidateAnswers)
		{
			if (StringUtils.equalsIgnoreCase (ca[0], caExisted[0]))
				throw new IllegalArgumentException ("不能有重复的答案值");
			if (ca.length >= 2 && caExisted.length >= 2  && StringUtils.equalsIgnoreCase (ca[1], caExisted[1]))
				throw new IllegalArgumentException ("不能有重复的答案标签");
			if (ca.length >= 2 && caExisted.length >= 2  && (StringUtils.equalsIgnoreCase (ca[0], caExisted[1]) || StringUtils.equalsIgnoreCase (ca[1], caExisted[0])) )
				throw new IllegalArgumentException ("此问题的“答案值或标签”不能与其他答案的“标签或答案值”有重复");
		}
	}

	/**
	 * 根据提供的答案 answer 在候选答案中寻找的候选答案
	 * @param answer
	 * @param listCandidateAnswers
	 * @return 如果不是有效的答案，则返回空字符串。如果是有效的答案，则返回 <code>数值</code>(只有数值的情况) <code>数值:标签</code>(数值、标签都有的情况)
	 */
	public static String GetFullCandidateAnswerByValueOrLabel (String answer, List<String[]> listCandidateAnswers)
	{
		for (String[] ca : listCandidateAnswers)
		{
			if (StringUtils.equalsIgnoreCase (ca[0], answer) || (ca.length >= 2 && StringUtils.equalsIgnoreCase (ca[1], answer)))
			{
				return ca.length >= 2 ? ca[0] + ":" + ca[1] : ca[0];
			}
		}
		return "";
	}
	public String GetFullCandidateAnswerByValueOrLabel (String answer)
	{
		return GetFullCandidateAnswerByValueOrLabel (answer, candidateAnswers);
	}

	/**
	 * 根据提供的答案 answer 在候选答案中找出的候选答案的 value
	 * @param answer
	 * @param listCandidateAnswers
	 * @return 如果不是有效的答案，则返回空字符串。如果是有效的答案，则返回候选答案的 <code>数值</code>(只有数值的情况)
	 */
	public static String GetCandidateAnswerValueByValueOrLabel (String answer, List<String[]> listCandidateAnswers)
	{
		for (String[] ca : listCandidateAnswers)
		{
			if (StringUtils.equalsIgnoreCase (ca[0], answer) || (ca.length >= 2 && StringUtils.equalsIgnoreCase (ca[1], answer)))
			{
				return ca[0];
			}
		}
		return "";
	}
	public String GetCandidateAnswerValueByValueOrLabel (String answer)
	{
		return GetCandidateAnswerValueByValueOrLabel (answer, candidateAnswers);
	}

	public boolean isParticipant (String n)
	{
		if (isParticipantsQuantityIndefinitely)
			return true;
		boolean isParticipant = false;
		for (String p : participants)
		{
			if (StringUtils.equalsIgnoreCase (p, n))
			{
				isParticipant = true;
				break;
			}
		}
		return isParticipant;
	}

	/**
	 * 有人尝试应答对话
	 * @param ch 哪个频道
	 * @param n 哪个昵称
	 * @param u 哪个帐号
	 * @param host 哪个主机名
	 * @param answer 提供的答案
	 * @return 1 - 当用户属于对话的参与者（已处理）； 0 - 用户不属于对话的参与者（已处理）； 负数 - 非法输入未处理，其他的命令可以继续处理输入
	 * @throws RuntimeException
	 */
	public boolean onAnswerReceived (String ch, String n, String u, String host, String answer)
	{
		boolean isValidParticipant = isParticipant (n);
		if (! isValidParticipant)
		{
			//throw new RuntimeException ("您不是当前对话的参与者");
			bot.SendMessage (ch, n, LiuYanBot.OPT_OUTPUT_USER_NAME, 1, "您不是当前对话的参与者");
			return false;	// 如果抛出异常，则可能让不是对话参与者、但想执行其他 bot 命令的人在对话期间无法操作
		}

		// 使用者先检查答案有效性
		if (dlguser != null)
		{
			if (! dlguser.ValidateAnswer (ch, n, u, host, answer, arrayDialogUserArguments))
				return false;
		}

		String irc_user = n;
		if (useHostAsAnswersKey)
			irc_user = host;

		// 检查答案有效性
		if (type == Type.单选)
		{
			String sFullAnswer = GetFullCandidateAnswerByValueOrLabel (answer);
			if (StringUtils.isEmpty (sFullAnswer) )	// 答案无效
				throw new IllegalArgumentException ("无效单选回答。");

			participantAnswers.put (irc_user, answer);
		}
		else if (type == Type.多选)
		{
			List<String> listAnswers = LiuYanBot.splitCommandLine (answer);
			if (listAnswers == null)
				throw new IllegalArgumentException ("请提供一个多选题的回答。");
			for (String a : listAnswers)
			{
//System.out.println ("[" + a + "]");
				String sFullAnswer = GetFullCandidateAnswerByValueOrLabel (a);
//System.out.println ("[" + sFullAnswer + "]");
				if (StringUtils.isEmpty (sFullAnswer) )	// 答案无效
					throw new IllegalArgumentException ("无效多选回答 " + a);
			}
			participantAnswers.put (irc_user, listAnswers);
		}
		else if (type == Type.确认)
		{
			if (StringUtils.equalsIgnoreCase (answer, "1") || StringUtils.equalsIgnoreCase (answer, "2") || StringUtils.equalsIgnoreCase (answer, "确认") || StringUtils.equalsIgnoreCase (answer, "取消"))
				participantAnswers.put (irc_user, answer);
			else
			{
				throw new IllegalArgumentException ("无效回答。回答只能为 1、2、确认、取消");
			}
		}
		else if (type == Type.是否)
		{
			if (StringUtils.equalsIgnoreCase (answer, "1") || StringUtils.equalsIgnoreCase (answer, "2") || StringUtils.equalsIgnoreCase (answer, "是") || StringUtils.equalsIgnoreCase (answer, "否"))
				participantAnswers.put (irc_user, answer);
			else
				throw new IllegalArgumentException ("无效回答。回答只能为 1、2、是、否");
		}
		else if (type == Type.开放)
		{
			participantAnswers.put (irc_user, answer);
		}

		if (isParticipantsQuantityIndefinitely)
			return true;	// 人数不确定时，收到答案不触发结束条件。等待 Dialog 超时自己结束……
		else
		{
			// 还有其他用户没回答，不触发 endCondition 条件
			if (participantAnswers.size () != participants.size ())
			{
				String msg = "谢谢，请等待其他 " + (participants.size () - participantAnswers.size ()) +  " 人回答完毕。";
				//// 发到对话发起的频道里
				//bot.SendMessage (channel, n, LiuYanBot.OPT_OUTPUT_USER_NAME, 1, msg);
				//if (StringUtils.isEmpty (ch))	// 如果用户通过私信发送的答案，则也再在私信里发一次
				//	bot.SendMessage (null, n, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, msg);
				bot.SendMessage (ch, n, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, msg);
				return true;
			}
		}


		try
		{
			lock.lock ();
			endCondition.signal ();	// 所有人回答完毕，触发结束条件
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
	public Map<String, Object> call ()
	{
		threadID = Thread.currentThread ().getId ();
System.out.println ("Dialog #" + threadID + " started. " + question);
		StringBuilder sb = new StringBuilder ();

		if (dlguser == null || showQuestion)
		{
			sb.append ("对话框 #");
			sb.append (Colors.GREEN);
			sb.append (threadID);
			sb.append (Colors.NORMAL);
			sb.append (" ");
			if (! isParticipantsQuantityIndefinitely)
			{
				// 显示对话框用法
				for (int i=0; i<participants.size (); i++)
				{
					if (i>0)
						sb.append (" ");
					sb.append (participants.get (i));
				}
				sb.append (": ");
			}
			if (showType)
			{
				sb.append (type);
				sb.append ("题:");
			}
			sb.append (Colors.BOLD);
			sb.append (question);
			sb.append (Colors.BOLD);
			if (type != Type.开放 && showCandidateAnswers)
			{
				sb.append (". 候选答案:");
				for (String[] ca : candidateAnswers)
				{
					sb.append (Colors.BOLD);
					sb.append (ca[0]);
					sb.append (Colors.BOLD);
					if (ca.length >= 2 && ! StringUtils.isEmpty (ca[1]))
					{
						sb.append (":");
						sb.append (ca[1]);
					}
					sb.append (" ");
				}
			}
			if (showUsage)
			{
				sb.append (". 请通过 " + Colors.BOLD + bot.getNick () + ": <答案>");
				if (type == Type.多选)
					sb.append (" [答案]...");
				sb.append (Colors.BOLD);
				sb.append (" 的回答方式或者通过私信发送 ");
				sb.append (Colors.BOLD);
					sb.append ("<答案>");
				if (type == Type.多选)
					sb.append (" [答案]...");
				sb.append (Colors.BOLD);
				sb.append (" 来回答问题");
			}
			sb.append ("     您有 " + timeout_second + " 秒钟的回答时间");
			if (isParticipantsQuantityIndefinitely || participants.size () > 1)
				mapGlobalOptions.put ("opt_output_username", false);	// 不输出用户名，因为可能参与者可能是多人

			if ((msgTargetMask & MESSAGE_TARGET_MASK_CHANNEL) > 0)
				bot.SendMessage (channel, nick, mapGlobalOptions, sb.toString ());	// Dialog #" + threadID + " (" + Colors.BOLD + question + Colors.BOLD + ") started

			if ((msgTargetMask & MESSAGE_TARGET_MASK_PM) > 0)
			{
				for (String p : participants)
					bot.SendMessage (null, p, mapGlobalOptions, sb.toString ());
			}
		}
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

			endtime = System.currentTimeMillis ();

			System.out.println ("Dialog #" + threadID + " ended. cost " + (endtime - starttime)/1000 + " s");
			if (dlguser == null)
			{	// 仅仅在对话没有使用者的情况下生成默认的对话结果消息
				sb = new StringBuilder ();
				sb.append ("对话 [");
				sb.append (Colors.BOLD);
				sb.append (question);
				sb.append (Colors.BOLD);
				sb.append ("]");
				if (participantAnswers.size () == 0)
					sb.append (" 没收到任何回答.");
				else
				{
					if (participants.size () == 1 && !isParticipantsQuantityIndefinitely)
					{	// 一个人、且不是不定人数，只有一个答案，显示“明细”
						sb.append (" 收到回答: ");
						if (type == Type.单选 || type == Type.确认 || type == Type.是否 || type == Type.开放)
						{
							String answer = null;
							for (Object a : participantAnswers.values ())
							{
								answer = (String)a;
								break;
							}
							if (type == Type.开放)
								sb.append (answer);
							else
								sb.append ("•" + GetFullCandidateAnswerByValueOrLabel(answer));	// • http://en.wikipedia.org/wiki/Bullet_%28typography%29
						}
						else if (type == Type.多选)
						{
							List<String> listAnswers = null;
							for (Object a : participantAnswers.values ())
							{
								listAnswers = (List<String>)a;
								for (int i=0; i<listAnswers.size (); i++)
								{
									if (i>0)
										sb.append (" ");
									sb.append ("✓" + GetFullCandidateAnswerByValueOrLabel(listAnswers.get (i)));	// ✓ http://en.wikipedia.org/wiki/Check_mark
								}
								break;	// 只有一个人
							}
						}
					}
					else
					{	// 多个人，多个答案，显示“统计”(非开放问题)

						if (isParticipantsQuantityIndefinitely)	// 如果参与人数不定，则只显示收到多少份回答
						{
							sb.append (" 收到了 " + participantAnswers.size () + " 份回答. ");
						}
						else
						{
							if (participantAnswers.size () == participants.size ())
								sb.append (" 收到了所有人的回答. ");
							else
								sb.append (" 收到了 " + participantAnswers.size () + " 份回答, " + (participants.size () - participantAnswers.size ()) + " 人未回答. ");
						}
						if (type == Type.单选 || type == Type.确认 || type == Type.是否 || type == Type.多选)
						{
							sb.append ("回答统计: ");
							String answer = null;
							Map<String, Integer> mapAnswerCount = new HashMap<String, Integer> ();
							for (Object a : participantAnswers.values ())
							{
								if (type == Type.多选)
								{
									List<String> listAnswers = (List<String>)a;
									for (int i=0; i<listAnswers.size (); i++)
									{
										answer = GetFullCandidateAnswerByValueOrLabel(listAnswers.get (i));
										if (mapAnswerCount.get (answer) == null)
											mapAnswerCount.put (answer, 1);
										else
											mapAnswerCount.put (answer, mapAnswerCount.get (answer) + 1);
									}
								}
								else
								{
									answer = GetFullCandidateAnswerByValueOrLabel ((String)a);
									if (mapAnswerCount.get (answer) == null)
										mapAnswerCount.put (answer, 1);
									else
										mapAnswerCount.put (answer, mapAnswerCount.get (answer) + 1);
								}
							}
							for (String ca : mapAnswerCount.keySet ())
							{
								sb.append (" ");
								if (type == Type.多选)
									sb.append ("✓");
								else //if (type == Type.单选 || type == Type.确认 || type == Type.是否 )
									sb.append ("•");
								sb.append (ca);
								sb.append ("=");
								sb.append (mapAnswerCount.get (ca));
							}
						}
						else if (type == Type.开放)
						{	// 开放问题无法统计，只能显示明细
							sb.append (": ");
							//for (String p : participants)
							for (String p : participantAnswers.keySet ())	// 因为目前支持不定人数的参与者，所以，不能再从 participants 取的参与人名称，因为里面的名称可能就只是一个通配符
							{
								String answer = (String)participantAnswers.get (p);
								sb.append (" ");
								sb.append (p);
								sb.append ("=");
								if (answer == null)
								{	// 无答案
									sb.append (Colors.DARK_GRAY);
									sb.append ("-无-");
									sb.append (Colors.NORMAL);
								}
								else
									sb.append (answer);
							}
						}
					}
				}
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		finally
		{
			lock.unlock ();
		}

		if (dlguser == null)
			bot.SendMessage (channel, nick, mapGlobalOptions, sb.toString ());	// "Dialog #" + threadID + " ended. cost " + (endtime - starttime)/1000 + " s." + (StringUtils.isEmpty (answer) ? "" : " 获得答案: " + Colors.BOLD + answer + Colors.BOLD
		//else
		//	user.

		dialogs.remove (this);
		return participantAnswers;
	}
}
