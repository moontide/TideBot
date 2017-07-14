package net.maclife.irc.game;

import java.util.*;
import java.util.logging.*;

import net.maclife.ansi.*;

/**
 * 根据作者自己的出牌习惯制作的略带智能的机器人玩家。
 * <br/>
 * 暂时只实现：
 * <ul>
 * 	<li>解出手牌最佳出牌方案</li>
 * 	<li>会拆牌打对方（自己备注：最佳出牌方案里，可能没有打对方时所需的牌型，在非最佳方案里可能会有该牌型）</li>
 * </ul>
 *
 *
 * 将来考虑实现：
 * <ul>
 * 	<li>考虑配合：
 * 		<ul>
 * 			<li>如果是农民时，先确定自己的手牌是适合走的，还是适合辅助队友的。
				<ul>
					<li>如果牌是适合走的，则根据最优方案出牌，不拆牌；</li>
					<li>如果牌是适合辅助队友的，则
						<ul>
							<li>如果在队友上家，如果地主出牌比较大，要打地主的牌（能拆就拆，只要能打住），然后送小牌给队友？如果地主出牌小，则过牌？（万一出的牌型队友不吃呢？）</li>
							<li>如果在队友下家，要不要抬高牌挡地主？队友出牌点数已经比较高了（比如 10 或者以上），还要不要再顶牌？</li>
						</ul>
					</li>
				</ul>
			</li>
			<li>如果是地主，呃… 先按最优方案出牌。 是否拆牌打对方，仅仅在对方剩牌比较少时……
				另外，如果拆牌，要计算怎么拆：比如，拆牌后，出牌次数 N 变的（增大）太多（比如一个顺子拆了后变成了几张单牌…），则放弃
			</li>
		</ul>
	</li>
 * 	<li>记牌？ 根据出的牌，猜测排除掉敌方可能的牌型？ -- 感觉太复杂</li>
 * 	<li></li>
 * 	<li></li>
 * 	<li></li>
 * 	<li></li>
 * </ul>
 * @author liuyan
 *
 */
@SuppressWarnings ({"unchecked", "unused"})
public class DouDiZhuBotPlayer_有点小智能的机器人 extends DouDiZhuBotPlayer
{
	public static final int MASK_不拆牌 = 1;
	public static final int MASK_拆牌   = 2;

	public static boolean logging = false;

	//static Logger logger = Logger.getLogger (DouDiZhuBotPlayer_有点小智能的机器人.class.getName ());
	public DouDiZhuBotPlayer_有点小智能的机器人 (String name)
	{
		super (name);
	}

	/**
	 * 评估牌的情况。
	 * 计算出
	 * <ul>
	 * <li>打完这些牌所需要的最小的出牌次数 N</li>
	 * <li>上面得到最小的次数 N 的 Ng 道牌</li>
	 * <li>若多种方案的出牌次数都是 N，且找出最优方案</li>
	 * </ul>
	 * @param player_cards
	 * @return Map
	 * key 列表
 		<dl>

			<dt>当前递归深度</dt>
			<dd>深度从 1 开始，每递归一次，深度+1（由本级递归负责加一）；递归结束后，深度-1（由上级递归调用者负责减一）</dd>

			<dt>当前递归分支</dt>
			<dd>也就是说：已经分析出来的几道牌</dd>

			<dt></dt>
			<dd></dd>

			<dt>递归深度</dt>
			<dd></dd>

			<dt>MinSteps</dt>
			<dd>int 类型。最小出牌次数。</dd>

			<dt>MinStepsSolutions</dt>
			<dd>List&lt;List&lt;String&gt;&gt; 类型。最小出牌次数的所有可能的出牌方案</dd>

			<dt>MinTypes</dt>
			<dd>int 类型。最少出牌类型数量。</dd>

			<dt>MinTypesSolutions</dt>
			<dd>List&lt;List&lt;String&gt;&gt; 类型。最少出牌类型的所有可能的出牌方案。一般来说，从这些方案里找会好一些，因为，同一个牌型 有可能有回收牌，就容易控制局势。</dd>

		</dl>
	 */
	public static Map<String, Object> EvaluatePlayerCards (List<Map<String, Object>> player_cards)
	{
		List<String> listCards = DouDiZhu.PlayerCardsToCardRanks (player_cards);
		return EvaluateCards (listCards);
	}
	public static Map<String, Object> EvaluateCards (List<String> listCards, DouDiZhu.Type expectedCardType, int nExpectedSerialLength, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	{
		//logger.setLevel (Level.ALL);
		//logger.entering (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards");
		Map<String, Object> mapResult = new LinkedHashMap<String, Object> ();

		mapResult.put ("MinSteps", 14);	// 最多只需要出 14 道牌：3-A,2,王牌(1张或两张)
		mapResult.put ("递归深度", 0);

		EvaluateCards_Recursive ("开始", listCards, mapResult, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
		//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards");
		return mapResult;
	}
	public static Map<String, Object> EvaluateCards (List<String> listCards, DouDiZhu.Type expectedCardType, int nExpectedSerialLength, int 大于此点值, int 拆牌)
	{
		return EvaluateCards (listCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType, nExpectedSerialLength);
	}
	public static Map<String, Object> EvaluateCards (List<String> listCards)
	{
		return EvaluateCards (listCards, null, 0, 0, MASK_拆牌 | MASK_不拆牌, null, 0);
	}

	public static boolean 不限牌型 (DouDiZhu.Type 牌型)
	{
		return (牌型 == null) || (牌型 == DouDiZhu.Type.__未知牌型__);
	}

	public static boolean 不限序列长度 (int nSerialLength)
	{
		return (nSerialLength == 0) || (nSerialLength > 12);
	}

	public static boolean 不限主牌数 (int nCopy)
	{
		return (nCopy == 0) || (nCopy > 4);
	}

	//public static boolean 不限附带牌类型 (DouDiZhu.附带牌类型 expectedAttachmentType)
	//{
	//	return (expectedAttachmentType == null);
	//}

	/**
	 * 递归解牌：不同牌型、按照不同顺序依次查找，其中顺子（含连对、飞机、大飞机的情况）还要考虑各个顺子长度、牌组数的情况。
	 * @param sCurrentOperation
	 * @param listCards
	 * @param mapResult
	 * @param expectedCardType 如果为 null，则解答所有牌型。否则，只解给出的牌型（很有可能解不出该牌型）
	 * @param nExpectedSerialLength
	 * 	<ul>
	 * 		<li>如果牌型为顺子、连对、飞机、飞机带单、飞机带对、大飞机、大飞机带2单、大飞机带2对，则指定
	 * 			<ul>
	 * 				<li>“顺子”的长度</li>
	 * 				<li>对于连对，就是连对牌数量的 1/2（也是说几对数）、</li>
	 * 				<li>对于飞机，就是 1/3、</li>
	 * 				<li>大飞机的 1/4）。</li>
	 * 			</ul>
	 * 		</li>
	 * 		<li>对于其他牌型，此数值无意义。</li>
	 * 	</ul>
	 * @param 大于此点值 只寻找大于此点值的牌
	 * @param 拆牌 是否拆牌。0或者3：不关心拆不拆；1：不拆；2：拆。
	 * @param expectedCardType_Recursive
	 * @param nExpectedSerialLength_Recursive
	 */
	public static void EvaluateCards_Recursive (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type expectedCardType, int nExpectedSerialLength, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	{
		//logger.entering (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Recursive");
		if (listCards.isEmpty ())
		{
			//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Recursive");
			return;
		}

		Log (mapResult, sCurrentOperation);
		Log (mapResult, "--------------------------------------------------------------------------------");

		// 先把王炸找出来：王炸不能跟其他牌组合来出，有的话，必定是单独的 1 道牌
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.王炸)
			&& listCards.size () >=2)
		{
			sCurrentOperation = "找王炸";
			EvaluateCards_王炸 (sCurrentOperation, listCards, mapResult, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}

		int nSerialLength = 0;
		int nMaxSerialLength = 0;
		// 不同长度的顺子
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.顺子)
			&& listCards.size () >=5)
		{
			nSerialLength = 5;
			nMaxSerialLength = listCards.size () > 12 ? 12 : listCards.size ();
			if (! 不限序列长度(nExpectedSerialLength))
			{
				nSerialLength = nMaxSerialLength = nExpectedSerialLength;
			}

			for (; nSerialLength<=nMaxSerialLength; nSerialLength++)
			{
				sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的顺子";
				EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.顺子,        nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
				递归深度减少 (sCurrentOperation, mapResult, listCards);
			}
		}
		// 不同长度的连对
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.连对)
			&& listCards.size () >=6)
		{
			nSerialLength = 3;
			nMaxSerialLength = listCards.size () / 2;
			if (! 不限序列长度(nExpectedSerialLength))
			{
				nSerialLength = nMaxSerialLength = nExpectedSerialLength;
			}

			for (; nSerialLength<=nMaxSerialLength; nSerialLength++)
			{
				sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的连对";
				EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.连对,        nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
				递归深度减少 (sCurrentOperation, mapResult, listCards);
			}
		}
		// 不同长度的大飞机
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.大飞机 || expectedCardType==DouDiZhu.Type.大飞机带2单 || expectedCardType==DouDiZhu.Type.大飞机带2对)
			//&& (不限主牌数(nExpectedCopy) || nExpectedCopy==4)
			&& listCards.size () >=8)
		{
			nSerialLength = 2;
			nMaxSerialLength = listCards.size () / 4;
			if (! 不限序列长度(nExpectedSerialLength))
			{
				nSerialLength = nMaxSerialLength = nExpectedSerialLength;
			}

			if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.大飞机))
			{
				for (; nSerialLength<=nMaxSerialLength; nSerialLength++)
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的大飞机";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.大飞机,      nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					递归深度减少 (sCurrentOperation, mapResult, listCards);
				}
			}
			if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.大飞机带2单) && nSerialLength * (4 + 2) <= listCards.size ())
			{
				for (; nSerialLength<=nMaxSerialLength; nSerialLength++)
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的大飞机带2单";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.大飞机带2单, nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 大飞机带单（2单）
				}
			}
			if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.大飞机带2对) && nSerialLength * (4 + 4) <= listCards.size ())
			{
				for (; nSerialLength<=nMaxSerialLength; nSerialLength++)
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的大飞机带2对";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.大飞机带2对, nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 大飞机带对（2对）
				}
			}
		}
		// 不同长度的飞机
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.飞机 || expectedCardType==DouDiZhu.Type.飞机带单 || expectedCardType==DouDiZhu.Type.飞机带对)
			&& listCards.size () >=6)
		{
			nSerialLength = 2;
			nMaxSerialLength = listCards.size () / 3;
			if (! 不限序列长度(nExpectedSerialLength))
			{
				nSerialLength = nMaxSerialLength = nExpectedSerialLength;
			}

			for (; nSerialLength<=nMaxSerialLength; nSerialLength++)
			{
				if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.飞机))
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的飞机";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.飞机,        nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					递归深度减少 (sCurrentOperation, mapResult, listCards);
				}
				if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.飞机带单) && nSerialLength * (3 + 1) <= listCards.size ())
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的飞机带单";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.飞机带单,    nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 飞机带单
				}
				if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.飞机带对) && nSerialLength * (3 + 2) <= listCards.size ())
				{
					sCurrentOperation = "找顺子：长度 = " + nSerialLength + " 的飞机带对";
					EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.飞机带对,    nSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					递归深度减少 (sCurrentOperation, mapResult, listCards);	// 飞机带对
				}
			}
		}

		//
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.四带2)
			&& listCards.size () >=6)
		{
			sCurrentOperation = "找单组：四带2";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.四带2,   大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.四带2对)
			&& listCards.size () >=8)
		{
			sCurrentOperation = "找单组：四带2对";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.四带2对, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.炸弹)
			&& listCards.size () >=4)
		{
			sCurrentOperation = "找单组：炸弹";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.炸弹,    大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.三带1)
			&& listCards.size () >=4)
		{
			sCurrentOperation = "找单组：三带1";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.三带1,   大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.三带1对)
			&& listCards.size () >=5)
		{
			sCurrentOperation = "找单组：三带1对";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.三带1对, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.三)
			&& listCards.size () >=3)
		{
			sCurrentOperation = "找单组：三";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.三,      大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.单)
			&& listCards.size () >=1)
		{
			sCurrentOperation = "找单组：单牌";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.单,      大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		if ((不限牌型(expectedCardType) || expectedCardType==DouDiZhu.Type.对)
			&& listCards.size () >=2)
		{
			sCurrentOperation = "找单组：对子";
			EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, DouDiZhu.Type.对,      大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			递归深度减少 (sCurrentOperation, mapResult, listCards);
		}
		//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Recursive");
	}

	/**
	 * 先处理最复杂的顺子（含连对、飞机、大飞机、以及飞机附带散牌的情况）
	 * @param sCurrentOperation
	 * @param listCards
	 * @param mapResult
	 * @param expectedCardType 牌型。必须明确指定，不能为 null，不能为 .__未知牌型__，不能为王炸，不能为单组牌
	 * @param nExpectedSerialLength 序列长度
	 * @param nExpectedCopy 序列数量：顺子取值为 1、连对取值为 2、飞机/飞机带单/飞机带对取值为 3、大飞机/大飞机带2单/大飞机带两对取值为 4
	 * @param 大于此点值
	 * @param 拆牌
	 * @param expectedCardType_Recursive
	 * @param nExpectedSerialLength_Recursive
	 * @param nExpectedCopy_Recursive
	 */
	public static void EvaluateCards_Serials (String sCurrentOperation, final List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type expectedCardType, int nExpectedSerialLength, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	{
		递归深度增加 (sCurrentOperation, mapResult, listCards);

		if (listCards.isEmpty ())
			return;

		if (expectedCardType!=DouDiZhu.Type.顺子
			&& expectedCardType!=DouDiZhu.Type.连对
			&& expectedCardType!=DouDiZhu.Type.飞机 && expectedCardType!=DouDiZhu.Type.飞机带单 && expectedCardType!=DouDiZhu.Type.飞机带对
			&& expectedCardType!=DouDiZhu.Type.大飞机 && expectedCardType!=DouDiZhu.Type.大飞机带2单 && expectedCardType!=DouDiZhu.Type.大飞机带2对
		)
			return;

		int nExpectedCopy = 0;
		switch (expectedCardType)
		{
			case 顺子:
				nExpectedCopy = 1;
				break;
			case 连对:
				nExpectedCopy = 2;
				break;
			case 飞机:
			case 飞机带单:
			case 飞机带对:
				nExpectedCopy = 3;
				break;
			case 大飞机:
			case 大飞机带2单:
			case 大飞机带2对:
				nExpectedCopy = 4;
				break;
		}
		DouDiZhu.附带牌类型 expectedAttachmentType = DouDiZhu.根据牌型获取附加牌类型 (expectedCardType); /* 0:无附带的牌 1:单牌 2:对牌 */
		if (listCards.size() < nExpectedSerialLength * (nExpectedCopy + expectedAttachmentType.ordinal ()) )
			return;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculateCards (listCards);
		String s解出的一道牌 = null;
		List<String> listUniqueCards = (List<String>) mapCardsInfo.get ("UniqueCards");
		List<String> listSoloCards = (List<String>) mapCardsInfo.get ("SoloCards"), listRemainingSoloCards;
		List<String> listPairCards = (List<String>) mapCardsInfo.get ("PairCards"), listRemainingPairCards;
		List<String> listTrioCards = (List<String>) mapCardsInfo.get ("TrioCards"), listRemainingTrioCards;
		List<String> listQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards"), listRemainingQuartetteCards;
		List<String> listSubList = null;
		List<String> listRemainingCards = new ArrayList<String> ();
		Map<String, Object> mapSubCardsInfo = null;
		/*
		for (int i=0; i<listCards.size (); i++)
		{
			if (listCards.size () - i < nSerialLength)
				return;

			// 从这里开始，每个不同的牌面一个标记
			int iNextRank = 0;
			String sThisRank = listCards.get (i);
			String sThisRankPoint = listCards.get (i);
			//if
			//List<String> listSubCards = listCards.subList (i, i + nSerialLength);
			//Map<String, Object> mapSubCardsInfo = DouDiZhu.CalculateCards (listSubCards);
			//if ((boolean)mapSubCardsInfo.get ("IsSerial") && DouDiZhu.RankToPoint (listSubCards.get (listSubCards.size () - 1)) > nMaxPoint)
			{
				//return 生成用于出牌的顺子 (listSubCards, 1);
			}
		}
		*/

		int n待打的牌的最小点 = 大于此点值 - nExpectedSerialLength + 1;
		//switch (nExpectedCopy)
		switch (expectedCardType)
		{
		//case 4:
		case 大飞机:
		case 大飞机带2单:
		case 大飞机带2对:
			for (int i=0; i<=listUniqueCards.size () - nExpectedSerialLength; i++)
			{
				if(DouDiZhu.RankToPoint (listUniqueCards.get (i)) <= n待打的牌的最小点)	// 假设顺子的最小点打不过待打的牌的最小点，则继续找
					continue;
				listSubList = listUniqueCards.subList (i, i + nExpectedSerialLength);
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				boolean 每张牌够张数 = true;
				for (int j=0; j<listSubList.size (); j++)
				{
					if ((int)mapCardsInfo.get (listSubList.get (j)) < nExpectedCopy)
					{	// 只要有一张牌不够死张，本顺子就是无效的连对
						每张牌够张数 = false;
						break;
					}
				}
				if (! 每张牌够张数)
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nExpectedCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);

				if (expectedAttachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "大飞机", DouDiZhu.Type.大飞机, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				switch (expectedAttachmentType)
				{
					case 带单:
						if (listRemainingSoloCards.size () >= nExpectedSerialLength * 2)
						{
							for (int j=0; j<nExpectedSerialLength * 2; j++)
							{	// 单牌数量足够，可以直接从单牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (j);
								listRemainingCards.remove (listRemainingSoloCards.get (j));
							}
							设置解出的一道牌 (listCards, mapResult, "大飞机带2单", DouDiZhu.Type.大飞机带2单, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							int 还差单牌数 = nExpectedSerialLength - listRemainingSoloCards.size ();
							// 怎么拆，需要根据拆后的牌的情况，多考虑
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingPairCards.size () * 2 >= 还差单牌数)
							{	//
								for (int j=0; j<还差单牌数; j++)
								{	// 单牌数量不够，然后剩下的对牌足够填满剩下的单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}
								设置解出的一道牌 (listCards, mapResult, "大飞机带2单(拆了对牌)", DouDiZhu.Type.大飞机带2单, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
							}
							else
							{
								for (int j=0; j<listRemainingPairCards.size () * 2; j++)
								{	// 单牌数量不够，然后剩下的对牌也不够填满剩下的单牌，那就先把对牌全部当单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}

								int 还差单牌数2 = 还差单牌数 - listRemainingPairCards.size () * 2;
								for (int j=0; j<listRemainingTrioCards.size () * 3; j++)
								{	// 单牌数量足够，可以直接从单牌里带走
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j/3);
									listRemainingCards.remove (listRemainingTrioCards.get (j/3));
								}
								设置解出的一道牌 (listCards, mapResult, "大飞机带2单(拆了对牌和三牌组)", DouDiZhu.Type.大飞机带2单, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
							}
						}
						break;
					case 带对:
						if (listRemainingPairCards.size () >= nExpectedSerialLength * 2)
						{
							for (int j=0; j<nExpectedSerialLength * 2; j++)
							{	// 对牌数量足够，可以直接从对牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j) + listRemainingPairCards.get (j);
								listRemainingCards.remove (listRemainingPairCards.get (j));
								listRemainingCards.remove (listRemainingPairCards.get (j));
							}

							设置解出的一道牌 (listCards, mapResult, "大飞机带2对", DouDiZhu.Type.大飞机带2对, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							int 还差对牌数 = nExpectedSerialLength * 2 - listRemainingPairCards.size ();
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingTrioCards.size () >= 还差对牌数)
							{	//
								for (int j=0; j<还差对牌数; j++)
								{	// 对牌数量不够，然后剩下的三牌组足够填满剩下的对牌
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j) + listRemainingTrioCards.get (j);
									listRemainingCards.remove (listRemainingTrioCards.get (j));
									listRemainingCards.remove (listRemainingTrioCards.get (j));
								}
								设置解出的一道牌 (listCards, mapResult, "大飞机带2对(拆了三牌组)", DouDiZhu.Type.大飞机带2对, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
							}
						}
						break;
				}
			}
			break;
		//case 3:
		case 飞机:
		case 飞机带单:
		case 飞机带对:
			for (int i=0; i<=listUniqueCards.size () - nExpectedSerialLength; i++)
			{
				if(DouDiZhu.RankToPoint (listUniqueCards.get (i)) <= n待打的牌的最小点)	// 假设顺子的最小点打不过待打的牌的最小点，则继续找
					continue;
				listSubList = listUniqueCards.subList (i, i + nExpectedSerialLength);
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				boolean 每张牌够张数 = true;
				for (int j=0; j<listSubList.size (); j++)
				{
					String sRank = listSubList.get (j);
					try
					{
						if ((int)mapCardsInfo.get (sRank) < nExpectedCopy)
						{	// 只要有一张牌不够三张，本顺子就是无效的飞机
							每张牌够张数 = false;
							break;
						}
					}
					catch (Exception e)
					{
						e.printStackTrace ();
					}
				}
				if (! 每张牌够张数)
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nExpectedCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);

				if (expectedAttachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "飞机", DouDiZhu.Type.飞机, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				switch (expectedAttachmentType)
				{
					case 带单:
						if (listRemainingSoloCards.size () >= nExpectedSerialLength * 1)
						{
							for (int j=0; j<nExpectedSerialLength * 1; j++)
							{	// 单牌数量足够，可以直接从单牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (j);
								listRemainingCards.remove (listRemainingSoloCards.get (j));
							}
							设置解出的一道牌 (listCards, mapResult, "飞机带单", DouDiZhu.Type.飞机带单, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							int 还差单牌数 = nExpectedSerialLength - listRemainingSoloCards.size ();
							// 怎么拆，需要根据拆后的牌的情况，多考虑
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingPairCards.size () * 2 >= 还差单牌数)
							{	//
								for (int j=0; j<还差单牌数; j++)
								{	// 单牌数量不够，然后剩下的对牌足够填满剩下的单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}
								设置解出的一道牌 (listCards, mapResult, "飞机带单(拆了对牌)", DouDiZhu.Type.飞机带单, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
							}
							else
							{
								for (int j=0; j<listRemainingPairCards.size () * 2; j++)
								{	// 单牌数量不够，然后剩下的对牌也不够填满剩下的单牌，那就先把对牌全部当单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j/2);
									listRemainingCards.remove (listRemainingPairCards.get (j/2));
								}

								int 还差单牌数2 = 还差单牌数 - listRemainingPairCards.size () * 2;
								for (int j=0; j<listRemainingTrioCards.size () * 3; j++)
								{	// 单牌数量足够，可以直接从单牌里带走
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j/3);
									listRemainingCards.remove (listRemainingTrioCards.get (j/3));
								}
								设置解出的一道牌 (listCards, mapResult, "飞机带单(拆了对牌和三牌组)", DouDiZhu.Type.飞机带单, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
							}
						}
						break;
					case 带对:
						if (listRemainingPairCards.size () >= nExpectedSerialLength)
						{
							for (int j=0; j<nExpectedSerialLength; j++)
							{	// 对牌数量足够，可以直接从对牌里带走
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (j) + listRemainingPairCards.get (j);
								listRemainingCards.remove (listRemainingPairCards.get (j));
								listRemainingCards.remove (listRemainingPairCards.get (j));
							}
							设置解出的一道牌 (listCards, mapResult, "飞机带对", DouDiZhu.Type.飞机带对, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							int 还差对牌数 = nExpectedSerialLength - listRemainingPairCards.size ();
							// 简便起见（不做的太智能，不考虑太多），只从 对子、三牌组 中取第一张
							if (listRemainingTrioCards.size () >= 还差对牌数)
							{	//
								for (int j=0; j<还差对牌数; j++)
								{	// 单牌数量不够，然后剩下的对牌足够填满剩下的单牌
									s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (j) + listRemainingTrioCards.get (j);
									listRemainingCards.remove (listRemainingTrioCards.get (j));
									listRemainingCards.remove (listRemainingTrioCards.get (j));
								}
								设置解出的一道牌 (listCards, mapResult, "飞机带对(拆了三牌组)", DouDiZhu.Type.飞机带对, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
							}
						}
						break;
				}
			}
			break;
		//case 2:
		case 连对:
			for (int i=0; i<=listUniqueCards.size () - nExpectedSerialLength; i++)
			{
				if(DouDiZhu.RankToPoint (listUniqueCards.get (i)) <= n待打的牌的最小点)	// 假设顺子的最小点打不过待打的牌的最小点，则继续找
					continue;
				listSubList = listUniqueCards.subList (i, i + nExpectedSerialLength);	// 从不同的偏移量抽出 nSerialLength 张牌
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				boolean 每张牌够张数 = true;
				for (int j=0; j<listSubList.size (); j++)
				{
					if ((int)mapCardsInfo.get (listSubList.get (j)) < nExpectedCopy)
					{	// 只要有一张牌不够两张，本顺子就是无效的连对
						每张牌够张数 = false;
						break;
					}
				}
				if (! 每张牌够张数)
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nExpectedCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);

				设置解出的一道牌 (listCards, mapResult, "连对", DouDiZhu.Type.连对, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			}
			break;
		//case 1:
		case 顺子:
			for (int i=0; i<=listUniqueCards.size () - nExpectedSerialLength; i++)	// 从不同的偏移量抽出 nSerialLength 张牌
			{
				if(DouDiZhu.RankToPoint (listUniqueCards.get (i)) <= n待打的牌的最小点)	// 假设顺子的最小点打不过待打的牌的最小点，则继续找
					continue;
				listSubList = listUniqueCards.subList (i, i + nExpectedSerialLength);
				mapSubCardsInfo = DouDiZhu.CalculateCards (listSubList);
				if (! (boolean)mapSubCardsInfo.get ("IsSerial"))
					continue;

				Log (mapResult, "########################################");
				s解出的一道牌 = SerialToString (listSubList);
				//listSubCards.removeAll (listSubList);
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				for (int j=0; j<nExpectedCopy; j++)
					for (String rank : listSubList)
						listRemainingCards.remove (rank);
				//Log (mapResult, "✓ 从不重复牌列表 " + listUniqueCards + " 偏移量 " + i + " 中找到了长度=" + nSerialLength + "的顺子 " + FormatCardPack (s解出的一道牌));
				设置解出的一道牌 (listCards, mapResult, "顺子", DouDiZhu.Type.顺子, s解出的一道牌, (int)mapSubCardsInfo.get ("SerialLength"), (int)mapSubCardsInfo.get ("MaxPoint"), listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
			}
			break;
		}
	}
	//public static void EvaluateCards_Serials (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type 牌型, int nSerialLength, int nCopy, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	//{
	//	EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, 牌型, nSerialLength, nCopy, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
	//}
	//public static void EvaluateCards_Serials (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type 牌型, int nSerialLength, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	//{
	//	EvaluateCards_Serials (sCurrentOperation, listCards, mapResult, 牌型, nSerialLength, 1, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
	//}
	public static String SerialToString (List<String> listCards, int nCopy)
	{
		StringBuilder sb = new StringBuilder ();
		for (String rank : listCards)
		{
			for (int i=0; i<nCopy; i++)
				sb.append (rank);
		}
		return sb.toString ();
	}
	public static String SerialToString (List<String> listCards)
	{
		return SerialToString (listCards, 1);
	}

	/**
	 * 再处理相对简单的单组牌（含单、对、三牌组、三牌组附带散牌的情况）。
	 * 注意，这里只根据相同牌型比较大小，不处理炸弹能打其他牌型的情况，需要由调用者自己判断。
	 * @param sCurrentOperation
	 * @param listCards
	 * @param mapResult
	 * @param expectedCardType 牌型。必须明确指定，不能为 null，不能为 .__未知牌型__，不能为序列牌型，不能为王炸
	 * @param nExpectedCopy 主牌数量：单牌取值为 1、对子取值为 2、三牌取值为 3、普通炸弹取值为 4
	 * @param 大于此点值
	 * @param 拆牌
	 * @param expectedCardType_Recursive
	 * @param nExpectedSerialLength_Recursive
	 * @param nExpectedCopy_Recursive
	 */
	public static void EvaluateCards_Singles (String sCurrentOperation, final List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type expectedCardType, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	{
		//logger.entering (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Singles");
		递归深度增加 (sCurrentOperation, mapResult, listCards);

		if (listCards.isEmpty ())
			return;

		if (expectedCardType!=DouDiZhu.Type.单
			&& expectedCardType!=DouDiZhu.Type.对
			&& expectedCardType!=DouDiZhu.Type.三 && expectedCardType!=DouDiZhu.Type.三带1 && expectedCardType!=DouDiZhu.Type.三带1对
			&& expectedCardType!=DouDiZhu.Type.炸弹 && expectedCardType!=DouDiZhu.Type.四带2 && expectedCardType!=DouDiZhu.Type.四带2对
		)
			return;

		DouDiZhu.附带牌类型 expectedAttachmentType = DouDiZhu.根据牌型获取附加牌类型 (expectedCardType); /* 0:无附带的牌 1:单牌 2:对牌 */
		int nExpectedCopy = 0;
		switch (expectedCardType)
		{
			case 单:
				nExpectedCopy = 1;
				break;
			case 对:
				nExpectedCopy = 2;
				break;
			case 三:
			case 三带1:
			case 三带1对:
				nExpectedCopy = 3;
				break;
			case 炸弹:
			case 四带2:
			case 四带2对:
				nExpectedCopy = 4;
				break;
		}
		if (listCards.size() < (nExpectedCopy + expectedAttachmentType.ordinal ()) )
			return;

		Map<String, Object> mapCardsInfo = DouDiZhu.CalculateCards (listCards);
		String s解出的一道牌 = null;
		List<String> listRemainingCards = new ArrayList<String> ();
		List<String> listSoloCards = (List<String>) mapCardsInfo.get ("SoloCards"), listRemainingSoloCards;
		List<String> listPairCards = (List<String>) mapCardsInfo.get ("PairCards"), listRemainingPairCards;
		List<String> listTrioCards = (List<String>) mapCardsInfo.get ("TrioCards"), listRemainingTrioCards;
		List<String> listQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards"), listRemainingQuartetteCards;

		int nPoint = 0;
		//switch (nExpectedCopy)
		switch (expectedCardType)
		{
		//case 4:
		case 炸弹:
		case 四带2:
		case 四带2对:
			if (listQuartetteCards.isEmpty ())
				return;
			for (String rank : listQuartetteCards)
			{
				Log (mapResult, "########################################");
				nPoint = DouDiZhu.RankToPoint (rank);
				if (nPoint <= 大于此点值)	// 假设牌的点值打不过待打的牌的，则继续找
					continue;
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				s解出的一道牌 = rank + rank + rank+ rank;

				if (expectedAttachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "炸弹（四牌组）", DouDiZhu.Type.炸弹, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				if (expectedAttachmentType == DouDiZhu.附带牌类型.带单)
				{	// 带两张单牌
					if (listRemainingSoloCards.size () >= 2)
					{
						//Log (mapResult, "四带2 找到了两张单牌");
						listRemainingCards.remove (listRemainingSoloCards.get (0));
						listRemainingCards.remove (listRemainingSoloCards.get (1));
						s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0) + listRemainingSoloCards.get (1);
						设置解出的一道牌 (listCards, mapResult, "四带2", DouDiZhu.Type.四带2, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					}
					else if (listRemainingSoloCards.size () == 1)
					{
						//Log (mapResult, "四带2 只找到了 1 张单牌，准备拆其他的对牌、三牌组…	");
						listRemainingCards.remove (listRemainingSoloCards.get (0));
						if (! listRemainingPairCards.isEmpty () || ! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							if (! listRemainingPairCards.isEmpty () )
							{
								//Log (mapResult, "拆一对 " + listRemainingPairCards.get (0));
								listRemainingCards.remove (listRemainingPairCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0) + listRemainingPairCards.get (0);
								Log (mapResult, "✓ 找到了四带2(拆对子) " + FormatCardPack (s解出的一道牌));
							}
							else if (! listRemainingTrioCards.isEmpty ())
							{
								//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0) + listRemainingTrioCards.get (0);
								Log (mapResult, "✓ 找到了四带2(拆三牌组) " + FormatCardPack (s解出的一道牌));
							}
							设置解出的一道牌 (listCards, mapResult, "四带2（拆了其他牌组中的1张）", DouDiZhu.Type.四带2, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							//Log (mapResult, "没找到其他能拆的对牌、三牌组。");
						}
					}
					else if (listRemainingSoloCards.isEmpty ())
					{	// 根本没单牌，拿一个对子当单牌
						//Log (mapResult, "四带2 根本没单牌可以带，准备带其他的对牌、或者拆三牌组…	");
						if (! listRemainingPairCards.isEmpty () || ! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							if (! listRemainingPairCards.isEmpty () )
							{
								//Log (mapResult, "把一对 " + listRemainingPairCards.get (0) + " 当两个单带出");
								listRemainingCards.remove (listRemainingPairCards.get (0));
								listRemainingCards.remove (listRemainingPairCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0);
								Log (mapResult, "✓ 找到了四带2 " + FormatCardPack (s解出的一道牌));
							}
							else if (! listRemainingTrioCards.isEmpty ())
							{
								//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (0) + listRemainingTrioCards.get (0);
								Log (mapResult, "✓ 找到了四带2 " + FormatCardPack (s解出的一道牌));
							}
							设置解出的一道牌 (listCards, mapResult, "四带2（拆了其他牌组中的2张）", DouDiZhu.Type.四带2, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							//Log (mapResult, "没找到其他能带的对牌、能拆的三牌组。");
						}
					}
				}
				else if (expectedAttachmentType == DouDiZhu.附带牌类型.带对)
				{	// 带两组对牌
					if (listRemainingPairCards.size () >= 2)
					{
						//Log (mapResult, "四带2对 找到了两组对牌");
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (1));
						listRemainingCards.remove (listRemainingPairCards.get (1));
						s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0) + listRemainingPairCards.get (1) + listRemainingPairCards.get (1);
						设置解出的一道牌 (listCards, mapResult, "四带2对", DouDiZhu.Type.四带2对, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					}
					else if (listRemainingPairCards.size () >= 1)
					{
						//Log (mapResult, "四带2对 只找到了 1 组对牌，准备拆其他的三牌组…	");
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (0));
						if (! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0) + listRemainingTrioCards.get (0) + listRemainingTrioCards.get (0);
							设置解出的一道牌 (listCards, mapResult, "四带2（拆了其他三牌组）", DouDiZhu.Type.四带2对, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							//Log (mapResult, "没找到其他能拆的三牌组。");
						}
					}
					else if (listRemainingPairCards.isEmpty ())
					{	// 根本没1组对牌，不去拆其他 3 牌了。倒不如合并两个炸弹当成 4带2对
						//
						//Log (mapResult, "四带2 根本没对牌可以带，准备合并其他炸弹当成 4 带 2 对…	");
						if (listRemainingQuartetteCards.size() >= 1)
						{
							//Log (mapResult, "合并其他炸弹 " + listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							listRemainingCards.remove (listRemainingQuartetteCards.get (0));
							s解出的一道牌 = s解出的一道牌 + listRemainingQuartetteCards.get (0) + listRemainingQuartetteCards.get (0) + listRemainingQuartetteCards.get (0) + listRemainingQuartetteCards.get (0);
							设置解出的一道牌 (listCards, mapResult, "四带2对（两个炸弹组成 = =）", DouDiZhu.Type.四带2对, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							//Log (mapResult, "没找到其他能合并的炸弹。");
						}
					}
				}
			}
			break;
		//case 3:
		case 三:
		case 三带1:
		case 三带1对:
			if (listTrioCards.isEmpty ())
				return;	// 这里也不从炸弹里面拆牌了，拆开后，出牌次数会更多
			for (String rank : listTrioCards)
			{
				Log (mapResult, "########################################");
				nPoint = DouDiZhu.RankToPoint (rank);
				if (nPoint <= 大于此点值)	// 假设牌的点值打不过待打的牌的，则继续找
					continue;
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				s解出的一道牌 =  rank + rank + rank;

				if (expectedAttachmentType == DouDiZhu.附带牌类型.不带牌)
				{
					设置解出的一道牌 (listCards, mapResult, "三牌组", DouDiZhu.Type.三, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					return;
				}

				mapCardsInfo = DouDiZhu.CalculateCards (listRemainingCards);
				listRemainingSoloCards = (List<String>) mapCardsInfo.get ("SoloCards");
				listRemainingPairCards = (List<String>) mapCardsInfo.get ("PairCards");
				listRemainingTrioCards = (List<String>) mapCardsInfo.get ("TrioCards");
				listRemainingQuartetteCards = (List<String>) mapCardsInfo.get ("QuartetteCards");

				if (expectedAttachmentType == DouDiZhu.附带牌类型.带单)
				{	// 带 1 张单牌
					if (listRemainingSoloCards.size () >= 1)
					{
						//Log (mapResult, "三带1 找到了 1 张单牌");
						listRemainingCards.remove (listRemainingSoloCards.get (0));
						s解出的一道牌 = s解出的一道牌 + listRemainingSoloCards.get (0);
						设置解出的一道牌 (listCards, mapResult, "三带1", DouDiZhu.Type.三带1, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					}
					else if (listRemainingSoloCards.isEmpty ())
					{	// 根本没单牌，拿一个对子当单牌
						//Log (mapResult, "三带1 根本没单牌可以带，准备拆其他的对牌、或者拆三牌组…	");
						if (! listRemainingPairCards.isEmpty () || ! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							if (! listRemainingPairCards.isEmpty () )
							{
								//Log (mapResult, "拆一对 " + listRemainingPairCards.get (0));
								listRemainingCards.remove (listRemainingPairCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0);
								Log (mapResult, "找到了三带1(拆对) " + FormatCardPack (s解出的一道牌));
							}
							else if (! listRemainingTrioCards.isEmpty ())
							{
								Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
								listRemainingCards.remove (listRemainingTrioCards.get (0));
								s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (0);
								Log (mapResult, "找到了三带1(拆三) " + FormatCardPack (s解出的一道牌));
							}
							设置解出的一道牌 (listCards, mapResult, "三带1（拆了其他牌组的1张）", DouDiZhu.Type.三带1, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							//Log (mapResult, "没找到其他能拆的对牌、能拆的三牌组。");
						}
					}
				}
				else if (expectedAttachmentType == DouDiZhu.附带牌类型.带对)
				{	// 带 1 对牌
					if (listRemainingPairCards.size () >= 1)
					{
						//Log (mapResult, "三带1对 找到了 1 组对牌");
						listRemainingCards.remove (listRemainingPairCards.get (0));
						listRemainingCards.remove (listRemainingPairCards.get (0));
						s解出的一道牌 = s解出的一道牌 + listRemainingPairCards.get (0) + listRemainingPairCards.get (0);
						设置解出的一道牌 (listCards, mapResult, "三带1对", DouDiZhu.Type.三带1对, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
					}
					else if (listRemainingPairCards.isEmpty ())
					{	// 根本没1组对牌，拆其他 3 牌
						//Log (mapResult, "三带1对 根本没对牌可以带，准备拆其他 三牌组…	");
						if (! listRemainingTrioCards.isEmpty ())	// 不考虑拆炸弹牌，不划算
						{
							//Log (mapResult, "拆三张 " + listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							listRemainingCards.remove (listRemainingTrioCards.get (0));
							s解出的一道牌 = s解出的一道牌 + listRemainingTrioCards.get (0) + listRemainingTrioCards.get (0);
							设置解出的一道牌 (listCards, mapResult, "三带1对（拆了其他三牌组的两张）", DouDiZhu.Type.三带1对, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
						}
						else
						{
							Log (mapResult, "没找到其他能拆的三牌组。");
						}
					}
				}
			}
			break;
		//case 2:
		case 对:
			if (listPairCards.isEmpty ())
				return;	// 这里也不从三牌组、炸弹里面拆牌了，拆开后，出牌次数会更多
			for (String rank : listPairCards)
			{
				//String rank = listPairCards.get (0);	// 这里也不
				nPoint = DouDiZhu.RankToPoint (rank);
				if (nPoint <= 大于此点值)	// 假设牌的点值打不过待打的牌的，则继续找
					continue;
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				listRemainingCards.remove (rank);
				s解出的一道牌 = rank + rank;
				设置解出的一道牌 (listCards, mapResult, "对牌", DouDiZhu.Type.对, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
				break;
			}
			break;
		//case 1:
		case 单:
			if (listSoloCards.isEmpty ())
				return;	// 这里也不从对子、三牌组、炸弹里面拆牌了，拆开后，出牌次数会更多
			for (String rank : listSoloCards)
			{
				//String rank = listSoloCards.get (0);
				nPoint = DouDiZhu.RankToPoint (rank);
				if (nPoint <= 大于此点值)	// 假设牌的点值打不过待打的牌的，则继续找
					continue;
				listRemainingCards.clear ();
				listRemainingCards.addAll (listCards);
				listRemainingCards.remove (rank);
				s解出的一道牌 = rank;
				设置解出的一道牌 (listCards, mapResult, "单牌", DouDiZhu.Type.单, s解出的一道牌, 0, nPoint, listRemainingCards, expectedCardType, 0, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
				break;
			}
			break;
		}
		//logger.exiting (DouDiZhuBotPlayer_有点小智能的机器人.class.getName (), "EvaluateCards_Singles");
	}
	//public static void EvaluateCards_Singles (final String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type expectedCardType, int nExpectedSerialLength, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	//{
	//	EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
	//}
	//public static void EvaluateCards_Single (final String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type expectedCardType, int 大于此点值, int 拆牌, DouDiZhu.Type expectedCardType_Recursive, int nExpectedSerialLength_Recursive)
	//{
	//	EvaluateCards_Singles (sCurrentOperation, listCards, mapResult, expectedCardType, 大于此点值, 拆牌, expectedCardType_Recursive, nExpectedSerialLength_Recursive);
	//}


	// 王炸单独处理
	public static void EvaluateCards_王炸 (String sCurrentOperation, List<String> listCards, Map<String, Object> mapResult, DouDiZhu.Type expectedCardType, int nExpectedSerialLength, int 大于此点值, int 拆牌, DouDiZhu.Type cardType_Recursive, int nExpectedSerialLength_Recursive)
	{
		递归深度增加 (sCurrentOperation, mapResult, listCards);
		if (listCards.isEmpty ())
			return;
		if (listCards.size() < 2 )
			return;

		String s解出的一道牌 = null;
		if (listCards.contains ("☆") && listCards.contains ("★"))
		{
			List<String> listRemainingCards = new ArrayList<String> ();
			listRemainingCards.addAll (listCards);

			// 将王炸的牌排除
			listRemainingCards.remove ("☆");
			listRemainingCards.remove ("★");

			s解出的一道牌 = "☆★";
			设置解出的一道牌 (listCards, mapResult, "王炸", DouDiZhu.Type.王炸, s解出的一道牌, 0, Integer.MAX_VALUE, listRemainingCards, expectedCardType, nExpectedSerialLength, 大于此点值, 拆牌, cardType_Recursive, nExpectedSerialLength_Recursive);
		}
	}

	public static void 递归深度增加 (String sCurrentOperation, Map<String, Object> mapResult, List<String> listCards)
	{
		Log (mapResult, "→ " + listCards.toString () + "	" + sCurrentOperation, true, false);

		Integer nDepth = (Integer)mapResult.get ("递归深度");
		if (nDepth == null)
			nDepth = 0;
		nDepth ++;
		mapResult.put ("递归深度", nDepth);

		Long nCount = (Long)mapResult.get ("递归次数");
		if (nCount == null)
			nCount = 0L;

		nCount ++;
		mapResult.put ("递归次数", nCount);

		//Log (mapResult, "增加递归深度到 " + nDepth + " 级。累计 " + nCount + " 次");
		Log (mapResult, nDepth + " 级， " + nCount + " 次。", false, true);

		mapResult.remove ("解出的一道牌_L" + nDepth);	// 清除每次递归可能产生的 key
		mapResult.remove ("是最后一道牌_L" + nDepth);	// 首先，必须有“解出的一道牌”，才能有此 key。 如果解出的牌是最后一道牌，则为 true，否则不存在（null）
		Log (mapResult, mapResult.get ("CurrentSolution")==null ? "" : GetCardsOnly ((List<Map<String, Object>>)mapResult.get ("CurrentSolution")) );
	}
	public static void 递归深度减少 (String sCurrentOperation, Map<String, Object> mapResult, List<String> listCards)
	{
		Integer nDepth = (Integer)mapResult.get ("递归深度");
		if (nDepth == null)
			nDepth = 0;

		nDepth --;
		mapResult.put ("递归深度", nDepth);
		Log (mapResult, "← 减少递归深度： " + nDepth + " ← " + (nDepth==null ? "null" : nDepth +1) + "	" + listCards);
	}
	public static void Log (Map<String, Object> mapResult, String s, boolean withLeftPadding, boolean terminateThisLine)
	{
		Integer nDepth = (Integer)mapResult.get ("递归深度");
		if (nDepth == null)
			nDepth = 0;
		Log (nDepth, s, withLeftPadding, terminateThisLine);
	}
	public static void Log (Map<String, Object> mapResult, String s)
	{
		Log (mapResult, s, true, true);
	}
	public static void Log (int nDepth, String s, boolean withLeftPadding, boolean terminateThisLine)
	{
		if (! logging)
			return;

		if (withLeftPadding)
		{
			for (int i=0; i<nDepth; i++)
			{
				System.out.print ("	");
			}
		}
		if (terminateThisLine)
		{
			System.out.println (s);
		}
		else
		{
			System.out.print (s);
			System.out.print ("	");
		}
	}

	static String GetCardsOnly (List<Map<String, Object>> listSolution)
	{
		StringBuilder sb = new StringBuilder ();
		sb.append ("[");
		for (Map<String, Object> mapCard : listSolution)
		{
			sb.append (mapCard.get ("牌"));
			sb.append (" ");
		}
		sb.append ("]");
		return sb.toString ();
	}
	static void 设置解出的一道牌 (List<String> listCards, Map<String, Object> mapResult, String sCardsDescription,
			DouDiZhu.Type 解出的牌型, String s解出的一道牌, int nSerialLength, int nMaxPoint,
			List<String> listRemainingCards,
			DouDiZhu.Type expectedCardType, int nExpectedSerialLength, int 大于此点值, int 拆牌,
			DouDiZhu.Type cardType_Recursive, int nExpectedSerialLength_Recursive)
	{
		//Set<String> set防止重复牌型 = (Set<String>)mapResult.get ("防止重复出牌方案");
		//if (set防止重复牌型 == null)
		//{
		//	set防止重复牌型 = new HashSet<String> ();
		//	mapResult.put ("防止重复出牌方案", set防止重复牌型);
		//}
		//if (set防止重复牌型.contains (s解出的一道牌))
		//{	// 备注：因为目前没有对 对子拆牌，
		//	return;
		//}
		//set防止重复牌型.add (s解出的一道牌);

		int nDepth = (int)mapResult.get ("递归深度");
		Map<String, Object> map解出的一道牌 = new HashMap<String, Object> ();
		map解出的一道牌.put ("牌型", 解出的牌型);
		map解出的一道牌.put ("牌", s解出的一道牌);
		//map解出的一道牌.put ("长度", s解出的一道牌.length ());
		map解出的一道牌.put ("序列长度", nSerialLength);
		map解出的一道牌.put ("最大点", nMaxPoint);

		mapResult.put ("解出的一道牌_L" + nDepth, map解出的一道牌);
		Set<List<Map<String, Object>>> setSolutions = (Set<List<Map<String, Object>>> )mapResult.get ("Solutions");
		if (setSolutions == null)
		{
			setSolutions = new LinkedHashSet<List<Map<String, Object>>> ();
			mapResult.put ("Solutions", setSolutions);
		}

		List<Map<String, Object>> listCurrentSolution = (List<Map<String, Object>>)mapResult.get ("CurrentSolution");
		if (listCurrentSolution == null)
		{
			listCurrentSolution = new ArrayList<Map<String, Object>> ();
			for (int i=1; i<nDepth; i++)
			{
				Map<String, Object> map上级解出的一道牌 = (Map<String, Object>)mapResult.get ("解出的一道牌_L" + i);
				if (map上级解出的一道牌 != null)
					listCurrentSolution.add (map上级解出的一道牌);
				//else
				//	listCurrentSolution.add ("正常情况下，不可能出现这个情况：");
			}
			mapResult.put ("CurrentSolution", listCurrentSolution);
		}
		listCurrentSolution.add (map解出的一道牌);
		Collections.sort (listCurrentSolution, DouDiZhu.斗地主不同牌型比较器);
		//Collections.sort (listCurrentSolution);

		Log (mapResult, "✓ 找到了" + sCardsDescription + " " + FormatCardPack (s解出的一道牌));
		Log (mapResult, "第 " + (setSolutions.size ()+1) + " 分支 = " + GetCardsOnly (listCurrentSolution));
		if (不限牌型(cardType_Recursive) && listRemainingCards.isEmpty () || (!不限牌型(cardType_Recursive) && 解出的牌型 == expectedCardType))
		{
			setSolutions.add (listCurrentSolution);	// listSolutions 改成 setSolutions 后，必须在最后一步将 solution 添加到 setSolutions 中，否则会出现 solution 重复的情况

			设置最后一道牌标志 (mapResult);
			mapResult.remove ("CurrentSolution");

			int n最少出牌次数 = mapResult.get ("MinSteps")==null ? 14 : (int)mapResult.get ("MinSteps");
			if (listCurrentSolution.size () <= n最少出牌次数)
			{
				mapResult.put ("MinSteps", listCurrentSolution.size ());
				Set<List<Map<String, Object>>> setMinStepsSolutions = (Set<List<Map<String, Object>>>) mapResult.get ("MinStepsSolutions");
				if (setMinStepsSolutions == null)
				{
					setMinStepsSolutions = new LinkedHashSet<List<Map<String, Object>>> ();
					mapResult.put ("MinStepsSolutions", setMinStepsSolutions);
				}
				if (listCurrentSolution.size () < n最少出牌次数)	// 有更少出牌次数的方案，则清理掉原来的方案列表，从头开始
					setMinStepsSolutions.clear ();

				setMinStepsSolutions.add (listCurrentSolution);
			}
		}
		else //if (! listRemainingCards.isEmpty ())
			EvaluateCards_Recursive ("找到 " + sCardsDescription + " " + s解出的一道牌 + " 后，递归处理剩下的牌", listRemainingCards, mapResult, cardType_Recursive, nExpectedSerialLength_Recursive, 大于此点值, 拆牌, cardType_Recursive, nExpectedSerialLength_Recursive);
	}
	static void 设置最后一道牌标志 (Map<String, Object> mapResult)
	{
		mapResult.put ("是最后一道牌_L" + mapResult.get ("递归深度"), true);
		PrintEndOfThisCase (mapResult);
	}

	public static String FormatCardPack (String sPack, String sANSIColor)
	{
		return sANSIColor + sPack + ANSIEscapeTool.CSI + "m";
	}
	public static String FormatCardPack (String sPack)
	{
		return FormatCardPack (sPack, ANSIEscapeTool.CSI + "32;1m");
	}

	public static void PrintEndOfThisCase (Map<String, Object> mapResult)
	{
		List<Map<String, Object>> listCurrentSolution = (List<Map<String, Object>>)mapResult.get ("CurrentSolution");
		Set<List<Map<String, Object>>> setSolutions = (Set<List<Map<String, Object>>>)mapResult.get ("Solutions");
		Log (mapResult, FormatCardPack("第 " + (setSolutions.size ()+1) + " 分支已到尽头，出牌次数 = " + listCurrentSolution.size () + "，出牌 = " + GetCardsOnly (listCurrentSolution), ANSIEscapeTool.CSI + "41;1m"));
	}

	@Override
	public Object 抢地主 (Object... args)
	{
		// 评估手牌情况，决定抢不抢地主
		// 底牌，有可能增强手牌，也完全有可能多出 3 张废牌、多出 3 道单牌！

		return null;
	}

	@Override
	public String 找出点数值大于N的单 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的顺子 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的连对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的三 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的三带1 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的三带1对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的飞机带单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的飞机带对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的四带2 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的四带2对 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机带2单 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的大飞机带2对 (List<Map<String, Object>> player_cards, int nMaxPoint, int nSerialLength)
	{
		return null;
	}

	@Override
	public String 找出点数值大于N的炸弹 (List<Map<String, Object>> player_cards, int nMaxPoint)
	{
		return null;
	}

	@Override
	public Object 出牌 (Object... args)
	{
		return null;
	}

	@Override
	public Object 回牌 (Object... args)
	{
		return null;
	}

	public static void main (String[] args)
	{

		if (args.length == 0)
		{
			System.err.println ("用法： java " + DouDiZhuBotPlayer_有点小智能的机器人.class.getCanonicalName () + " <斗地主牌组（只需要牌面，不需要花色）> [寻找的牌型名 <序列长度> <大于此点值>]" );
		}

		String sCardRanks =args[0];
		String sCardType = null;
		DouDiZhu.Type cardType = null;
		String sSerialLength = null;
		int nSerialLength = 0;
		//String sCopy = null;
		//int nCopy = 0;
		String sBiggerThanThis = null;
		int nBiggerThanThis = 0;
		if (args.length > 1)
		{
			sCardType = args[1];
			cardType = DouDiZhu.Type.valueOf (sCardType);
		}
		if (args.length > 2)
		{
			sSerialLength = args[2];
			nSerialLength = Integer.parseInt (sSerialLength);
		}
		//if (args.length > 3)
		//{
		//	sCopy = args[3];
		//	nCopy = Integer.parseInt (sCopy);
		//}
		if (args.length > 3)
		{
			sBiggerThanThis = args[3];
			nBiggerThanThis = Integer.parseInt (sBiggerThanThis);
		}

		Set<List<Map<String, Object>>> setSolutions = null;	// new HashSet<List<Map<String, Object>>> ();
		logging = true;
		//for (String arg : args)
		{
			List<String> listCards = DouDiZhu.AnswerToCardRanksList (sCardRanks);
			Map<String, Object> mapResult = EvaluateCards (listCards, cardType, nSerialLength, nBiggerThanThis, 0);
System.out.println ();
System.out.print (sCardRanks + " 寻找" + (不限牌型(cardType) ? "任意牌型" : "牌型为" + cardType) + "(序列长度:" + nSerialLength + ", 大于此点值:" + nBiggerThanThis + ") 最少出牌次数：" + mapResult.get ("MinSteps") + "，");
			//Set<List<Map<String, Object>>> setSolutions = null;

			setSolutions = (Set<List<Map<String, Object>>>)mapResult.get ("MinStepsSolutions");
			PrintSolutions (setSolutions, "最少出牌次数");

			setSolutions = (Set<List<Map<String, Object>>>)mapResult.get ("MinTypesSolutions");
			PrintSolutions (setSolutions, "最少牌型数");

			setSolutions = (Set<List<Map<String, Object>>>)mapResult.get ("Solutions");
			PrintSolutions (setSolutions, "**不限**");
		}
	}
	static void PrintSolutions (Set<List<Map<String, Object>>> setSolutions, String sSolutionName)
	{
		if (setSolutions == null)
		System.out.println ("未找到任何【" + sSolutionName + "】的方案");
		else
		{
System.out.println ("【" + sSolutionName + "】的所有方案有 " + setSolutions.size () + " 个：");
			int i = 0;
			for (List<Map<String, Object>> solution : setSolutions)
			{
				i ++;
System.out.println (i + ": " + GetCardsOnly (solution) + "    "/* + solution*/);
			}
		}
	}
}
