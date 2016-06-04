package net.maclife.irc.hcicloud;

import java.util.*;

import com.sinovoice.hcicloudsdk.api.*;
import com.sinovoice.hcicloudsdk.api.nlu.*;
import com.sinovoice.hcicloudsdk.common.*;
import com.sinovoice.hcicloudsdk.common.nlu.*;

/**

HCI_ERR_UNKNOWN 	-1: 未知错误，通常不会出现
HCI_ERR_NONE 	0: 正确
HCI_ERR_PARAM_INVALID 	1: 函数的传入参数错误
HCI_ERR_OUT_OF_MEMORY 	2: 申请内存失败
HCI_ERR_CONFIG_INVALID 	3: 配置串参数错误
HCI_ERR_CONFIG_CAPKEY_MISSING 	4: 缺少必需的capKey配置项
HCI_ERR_CONFIG_CAPKEY_NOT_MATCH 	5: CAPKEY与当前引擎不匹配
HCI_ERR_CONFIG_DATAPATH_MISSING 	6: 缺少必需的dataPath配置项
HCI_ERR_CONFIG_UNSUPPORT 	7: 配置项不支持
HCI_ERR_SERVICE_CONNECT_FAILED 	8: 连接服务器失败，服务器无响应
HCI_ERR_SERVICE_TIMEOUT 	9: 服务器访问超时
HCI_ERR_SERVICE_DATA_INVALID 	10: 服务器返回的数据格式不正确
HCI_ERR_SERVICE_RESPONSE_FAILED 	11: 服务器返回操作失败
HCI_ERR_CAPKEY_NOT_FOUND 	12: 没有找到指定的能力
HCI_ERR_NOT_LOCAL_CAPKEY 	13: 不是本地能力的KEY
HCI_ERR_LOCAL_LIB_MISSING 	14: 本地能力引擎缺失必要的库资源
HCI_ERR_URL_MISSING 	15: 找不到对应的网络服务地址（可能是HCI能力服务地址，下载资源库地址等）
HCI_ERR_SESSION_INVALID 	16: 无效的会话
HCI_ERR_TOO_MANY_SESSION 	17: 开启会话过多(目前每种HCI能力的最大会话数为256)
HCI_ERR_ACTIVE_SESSION_EXIST 	18: 还有会话没有停止
HCI_ERR_START_LOG_FAILED 	19: 启动日志错误, 可能是日志配置参数错误，路径不存在或者没有写权限等造成
HCI_ERR_DATA_SIZE_TOO_LARGE 	20: 传入的数据量超过可处理的上限
HCI_ERR_LOAD_CODEC_DLL 	21: 加载codec编码库失败
HCI_ERR_UNSUPPORT 	22: 暂不支持
HCI_ERR_LOAD_FUNCTION_FROM_DLL 	23: 加载库失败
HCI_ERR_SYS_NOT_INIT 	100: HCI_SYS 未初始化
HCI_ERR_SYS_ALREADY_INIT 	101: HCI_SYS 多次初始化
HCI_ERR_SYS_CONFIG_AUTHPATH_MISSING 	102: 缺少必需的authPath配置项
HCI_ERR_SYS_CONFIG_CLOUDURL_MISSING 	103: 缺少必需的cloudUrl配置项
HCI_ERR_SYS_CONFIG_USERID_MISSING 	104: 缺少必需的userId配置项
HCI_ERR_SYS_CONFIG_PASSWORD_MISSING 	105: 缺少必需的password配置项
HCI_ERR_SYS_CONFIG_PLATFORMID_MISSING 	106: 缺少必需的platformId配置项
HCI_ERR_SYS_CONFIG_DEVELOPERID_MISSING 	107: 缺少必需的developerId配置项
HCI_ERR_SYS_CONFIG_DEVELOPERKEY_MISSING 	108: 缺少必需的developerKey配置项
HCI_ERR_SYS_CONFIG_APPNO_MISSING 	109: 缺少必需的appNo配置项
HCI_ERR_SYS_USERINFO_INVALID 	110: 读写用户信息文件错误
HCI_ERR_SYS_AUTHFILE_INVALID 	111: 读取授权文件错误
HCI_ERR_SYS_CHECKAUTH_RESPONSE_FAILED 	112: 服务器返回获取云端授权失败
HCI_ERR_SYS_REGISTER_RESPONSE_FAILED 	113: 服务器返回注册用户失败
HCI_ERR_SYS_USING 	114: 仍然有能力在使用（尚未反初始化）
HCI_ERR_SYS_CONFIG_APPKEY_MISSING 	115: 缺少必需的appkey配置项
HCI_ERR_ASR_NOT_INIT 	200: HCI_ASR 没有初始化
HCI_ERR_ASR_ALREADY_INIT 	201: HCI_ASR 多次初始化
HCI_ERR_ASR_CONFIRM_NO_TASK 	202: 使用confirm，但没有确认任务
HCI_ERR_ASR_PARAM_CHECK_FLAG_INVALID 	203: 错误的CheckFlag项，例如已经检测到端点仍然再发送数据，或尚未开启端点检测就发送flag为（CHECK_FLAG_END，CHECK_FLAG_PROGRESS）
HCI_ERR_ASR_GRAMMAR_DATA_TOO_LARGE 	204: 语法数据太大
HCI_ERR_ASR_ENGINE_NOT_INIT 	205: ASR本地引擎尚未初始化
HCI_ERR_ASR_ENGINE_INIT_FAILED 	206: ASR本地引擎初始化失败
HCI_ERR_ASR_OPEN_GRAMMAR_FILE 	207: 读取语法文件失败
HCI_ERR_ASR_LOAD_GRAMMAR_FAILED 	208: 加载语法文件失败
HCI_ERR_ASR_ENGINE_FAILED 	209: ASR本地引擎识别失败
HCI_ERR_ASR_GRAMMAR_ID_INVALID 	210: 语法ID无效
HCI_ERR_ASR_REALTIME_WAITING 	211: 实时识别时未检测到音频末端，继续等待数据
HCI_ERR_ASR_GRAMMAR_OVERLOAD 	212: 该加载语法数量已达上限
HCI_ERR_ASR_GRAMMAR_USING 	213: 该语法正在使用中
HCI_ERR_ASR_REALTIME_END 	214: 实时识别时检测到末端，或者缓冲区满，需要使用NULL获取结果
HCI_ERR_ASR_UPLOAD_NO_DATA 	215: 上传本地数据时，无数据上传
HCI_ERR_HWR_NOT_INIT 	300: HCI_HWR没有初始化
HCI_ERR_HWR_ALREADY_INIT 	301: HCI_HWR多次初始化
HCI_ERR_HWR_CONFIRM_NO_TASK 	302: 使用confirm，但没有确认任务
HCI_ERR_HWR_ENGINE_INIT_FAILED 	303: HWR本地引擎初始化失败
HCI_ERR_HWR_ENGINE_FAILED 	304: HWR本地引擎操作（识别、获取拼音、获取联想结果）失败
HCI_ERR_HWR_UPLOAD_NO_DATA 	305: 没有可用于上传的数据
HCI_ERR_HWR_ENGINE_SESSION_START_FAILED 	306: HWR本地引擎开启会话失败
HCI_ERR_HWR_ENGINE_NOT_INIT 	307: SDK初始化时未传入本地能力却在创建会话时使用了本地能力
HCI_ERR_HWR_CONFIG_SUBLANG_MISSING 	308: 单字能力、多语种字典时，未传入sublang
HCI_ERR_HWR_TOO_MANY_DOMAIN 	309: 传入了领域数目超过了4个
HCI_ERR_OCR_NOT_INIT 	400: HCI_OCR 没有初始化
HCI_ERR_OCR_ALREADY_INIT 	401: HCI_OCR 多次初始化
HCI_ERR_OCR_ENGINE_INIT_FAILED 	402: OCR本地引擎初始化失败
HCI_ERR_OCR_ENGINE_FAILED 	403: OCR本地引擎操作（倾斜校正、版面分析、识别、预处理、压缩）失败
HCI_ERR_OCR_ENGINE_NOT_INIT 	404: SDK初始化时未传入本地能力却在创建会话时使用了本地能力
HCI_ERR_OCR_LOAD_IMAGE 	405: 载入本地文件或者本地图片缓冲失败
HCI_ERR_OCR_SAVE_IMAGE 	406: 保存OCR_IMAGE到本地文件失败
HCI_ERR_OCR_IMAGE_NOT_SET 	407: 未设置要处理的图片就进行了倾斜校正、版面分析、识别等操作
HCI_ERR_TTS_NOT_INIT 	500: HCI_TTS 没有初始化
HCI_ERR_TTS_ALREADY_INIT 	501: HCI_TTS 多次初始化
HCI_ERR_TTS_SESSION_BUSY 	502: TTS会话正忙，例如在合成回调函数中又调用了合成
HCI_ERR_TTS_ENGINE_SESSION_START_FAILED 	503: TTS本地引擎开启会话失败
HCI_ERR_TTS_ENGINE_FAILED 	504: 本地引擎合成失败
HCI_ERR_TTS_ENGINE_INIT_FAILED 	505: TTS(NU)本地引擎初始化失败
HCI_ERR_TTS_ENGINE_NOT_INIT 	506: TTS(NU)本地引擎尚未初始化
HCI_ERR_MT_NOT_INIT 	600: HCI_MT没有初始化
HCI_ERR_MT_ALREADY_INIT 	601: HCI_MT多次初始化
HCI_ERR_NLU_NOT_INIT 	700: HCI_NLU没有初始化
HCI_ERR_NLU_ALREADY_INIT 	701: HCI_NLU多次初始化
HCI_ERR_NLU_ENGINE_SESSION_START_FAILED 	702: NLU本地引擎开启会话失败
HCI_ERR_NLU_ENGINE_FAILED 	703: NLU本地引擎识别失败
HCI_ERR_KB_NOT_INIT 	800: HCI_KB没有初始化
HCI_ERR_KB_ALREADY_INIT 	801: HCI_KB多次初始化
HCI_ERR_KB_ENGINE_SESSION_START_FAILED 	802: KB本地引擎开启会话失败
HCI_ERR_KB_ENGINE_FAILED 	803: KB本地引擎合成失败
HCI_ERR_KB_SYLLABLE_INVALID 	804: 容错音节无法判断类型
HCI_ERR_KB_UDB_WORD_EXIST 	805: 已经在用户词库中存在
HCI_ERR_KB_CONFIRM_NO_TASK 	806: 使用confirm，但没有确认任务
HCI_ERR_VPR_NOT_INIT 	900: HCI_VPR没有初始化
HCI_ERR_VPR_ALREADY_INIT 	901: HCI_VPR多次初始化
HCI_ERR_VPR_ENGINE_INIT_FAILED 	902: VPR本地引擎初始化失败
HCI_ERR_VPR_ENGINE_FAILED 	903: VPR本地引擎处理失败
HCI_ERR_VPR_USERID_NOT_EXIST 	904: VPR用户不存在
HCI_ERR_VPR_ENGINE_SESSION_START_FAILED 	905: VPR本地引擎开启会话失败

 * @author liuyan
 *
 */
public class HCICloudNLUBot
{
	public static final Map<Integer, String> MAP_HCI_ERRORS = new HashMap<Integer, String> ();
	static
	{
		MAP_HCI_ERRORS.put (-1, "HCI_ERR_UNKNOWN 未知错误，通常不会出现");
		MAP_HCI_ERRORS.put (0, "HCI_ERR_NONE 正确");
		MAP_HCI_ERRORS.put (1, "HCI_ERR_PARAM_INVALID 函数的传入参数错误");
		MAP_HCI_ERRORS.put (2, "HCI_ERR_OUT_OF_MEMORY 申请内存失败");
		MAP_HCI_ERRORS.put (3, "HCI_ERR_CONFIG_INVALID 配置串参数错误");
		MAP_HCI_ERRORS.put (4, "HCI_ERR_CONFIG_CAPKEY_MISSING 缺少必需的capKey配置项");
		MAP_HCI_ERRORS.put (5, "HCI_ERR_CONFIG_CAPKEY_NOT_MATCH CAPKEY与当前引擎不匹配");
		MAP_HCI_ERRORS.put (6, "HCI_ERR_CONFIG_DATAPATH_MISSING 缺少必需的dataPath配置项");
		MAP_HCI_ERRORS.put (7, "HCI_ERR_CONFIG_UNSUPPORT 配置项不支持");
		MAP_HCI_ERRORS.put (8, "HCI_ERR_SERVICE_CONNECT_FAILED 连接服务器失败，服务器无响应");
		MAP_HCI_ERRORS.put (9, "HCI_ERR_SERVICE_TIMEOUT 服务器访问超时");
		MAP_HCI_ERRORS.put (10, "HCI_ERR_SERVICE_DATA_INVALID 服务器返回的数据格式不正确");
		MAP_HCI_ERRORS.put (11, "HCI_ERR_SERVICE_RESPONSE_FAILED 服务器返回操作失败");
		MAP_HCI_ERRORS.put (12, "HCI_ERR_CAPKEY_NOT_FOUND 没有找到指定的能力");
		MAP_HCI_ERRORS.put (13, "HCI_ERR_NOT_LOCAL_CAPKEY 不是本地能力的KEY");
		MAP_HCI_ERRORS.put (14, "HCI_ERR_LOCAL_LIB_MISSING 本地能力引擎缺失必要的库资源");
		MAP_HCI_ERRORS.put (15, "HCI_ERR_URL_MISSING 找不到对应的网络服务地址（可能是HCI能力服务地址，下载资源库地址等）");
		MAP_HCI_ERRORS.put (16, "HCI_ERR_SESSION_INVALID 无效的会话");
		MAP_HCI_ERRORS.put (17, "HCI_ERR_TOO_MANY_SESSION 开启会话过多(目前每种HCI能力的最大会话数为256)");
		MAP_HCI_ERRORS.put (18, "HCI_ERR_ACTIVE_SESSION_EXIST 还有会话没有停止");
		MAP_HCI_ERRORS.put (19, "HCI_ERR_START_LOG_FAILED 启动日志错误, 可能是日志配置参数错误，路径不存在或者没有写权限等造成");
		MAP_HCI_ERRORS.put (20, "HCI_ERR_DATA_SIZE_TOO_LARGE 传入的数据量超过可处理的上限");
		MAP_HCI_ERRORS.put (21, "HCI_ERR_LOAD_CODEC_DLL 加载codec编码库失败");
		MAP_HCI_ERRORS.put (22, "HCI_ERR_UNSUPPORT 暂不支持");
		MAP_HCI_ERRORS.put (23, "HCI_ERR_LOAD_FUNCTION_FROM_DLL 加载库失败");
		MAP_HCI_ERRORS.put (100, "HCI_ERR_SYS_NOT_INIT HCI_SYS 未初始化");
		MAP_HCI_ERRORS.put (101, "HCI_ERR_SYS_ALREADY_INIT HCI_SYS 多次初始化");
		MAP_HCI_ERRORS.put (102, "HCI_ERR_SYS_CONFIG_AUTHPATH_MISSING 缺少必需的authPath配置项");
		MAP_HCI_ERRORS.put (103, "HCI_ERR_SYS_CONFIG_CLOUDURL_MISSING 缺少必需的cloudUrl配置项");
		MAP_HCI_ERRORS.put (104, "HCI_ERR_SYS_CONFIG_USERID_MISSING 缺少必需的userId配置项");
		MAP_HCI_ERRORS.put (105, "HCI_ERR_SYS_CONFIG_PASSWORD_MISSING 缺少必需的password配置项");
		MAP_HCI_ERRORS.put (106, "HCI_ERR_SYS_CONFIG_PLATFORMID_MISSING 缺少必需的platformId配置项");
		MAP_HCI_ERRORS.put (107, "HCI_ERR_SYS_CONFIG_DEVELOPERID_MISSING 缺少必需的developerId配置项");
		MAP_HCI_ERRORS.put (108, "HCI_ERR_SYS_CONFIG_DEVELOPERKEY_MISSING 缺少必需的developerKey配置项");
		MAP_HCI_ERRORS.put (109, "HCI_ERR_SYS_CONFIG_APPNO_MISSING 缺少必需的appNo配置项");
		MAP_HCI_ERRORS.put (110, "HCI_ERR_SYS_USERINFO_INVALID 读写用户信息文件错误");
		MAP_HCI_ERRORS.put (111, "HCI_ERR_SYS_AUTHFILE_INVALID 读取授权文件错误");
		MAP_HCI_ERRORS.put (112, "HCI_ERR_SYS_CHECKAUTH_RESPONSE_FAILED 服务器返回获取云端授权失败");
		MAP_HCI_ERRORS.put (113, "HCI_ERR_SYS_REGISTER_RESPONSE_FAILED 服务器返回注册用户失败");
		MAP_HCI_ERRORS.put (114, "HCI_ERR_SYS_USING 仍然有能力在使用（尚未反初始化）");
		MAP_HCI_ERRORS.put (115, "HCI_ERR_SYS_CONFIG_APPKEY_MISSING 缺少必需的appkey配置项");
		MAP_HCI_ERRORS.put (200, "HCI_ERR_ASR_NOT_INIT HCI_ASR 没有初始化");
		MAP_HCI_ERRORS.put (201, "HCI_ERR_ASR_ALREADY_INIT HCI_ASR 多次初始化");
		MAP_HCI_ERRORS.put (202, "HCI_ERR_ASR_CONFIRM_NO_TASK 使用confirm，但没有确认任务");
		MAP_HCI_ERRORS.put (203, "HCI_ERR_ASR_PARAM_CHECK_FLAG_INVALID 错误的CheckFlag项，例如已经检测到端点仍然再发送数据，或尚未开启端点检测就发送flag为（CHECK_FLAG_END，CHECK_FLAG_PROGRESS）");
		MAP_HCI_ERRORS.put (204, "HCI_ERR_ASR_GRAMMAR_DATA_TOO_LARGE 语法数据太大");
		MAP_HCI_ERRORS.put (205, "HCI_ERR_ASR_ENGINE_NOT_INIT ASR本地引擎尚未初始化");
		MAP_HCI_ERRORS.put (206, "HCI_ERR_ASR_ENGINE_INIT_FAILED ASR本地引擎初始化失败");
		MAP_HCI_ERRORS.put (207, "HCI_ERR_ASR_OPEN_GRAMMAR_FILE 读取语法文件失败");
		MAP_HCI_ERRORS.put (208, "HCI_ERR_ASR_LOAD_GRAMMAR_FAILED 加载语法文件失败");
		MAP_HCI_ERRORS.put (209, "HCI_ERR_ASR_ENGINE_FAILED ASR本地引擎识别失败");
		MAP_HCI_ERRORS.put (210, "HCI_ERR_ASR_GRAMMAR_ID_INVALID 语法ID无效");
		MAP_HCI_ERRORS.put (211, "HCI_ERR_ASR_REALTIME_WAITING 实时识别时未检测到音频末端，继续等待数据");
		MAP_HCI_ERRORS.put (212, "HCI_ERR_ASR_GRAMMAR_OVERLOAD 该加载语法数量已达上限");
		MAP_HCI_ERRORS.put (213, "HCI_ERR_ASR_GRAMMAR_USING 该语法正在使用中");
		MAP_HCI_ERRORS.put (214, "HCI_ERR_ASR_REALTIME_END 实时识别时检测到末端，或者缓冲区满，需要使用NULL获取结果");
		MAP_HCI_ERRORS.put (215, "HCI_ERR_ASR_UPLOAD_NO_DATA 上传本地数据时，无数据上传");
		MAP_HCI_ERRORS.put (300, "HCI_ERR_HWR_NOT_INIT HCI_HWR没有初始化");
		MAP_HCI_ERRORS.put (301, "HCI_ERR_HWR_ALREADY_INIT HCI_HWR多次初始化");
		MAP_HCI_ERRORS.put (302, "HCI_ERR_HWR_CONFIRM_NO_TASK 使用confirm，但没有确认任务");
		MAP_HCI_ERRORS.put (303, "HCI_ERR_HWR_ENGINE_INIT_FAILED HWR本地引擎初始化失败");
		MAP_HCI_ERRORS.put (304, "HCI_ERR_HWR_ENGINE_FAILED HWR本地引擎操作（识别、获取拼音、获取联想结果）失败");
		MAP_HCI_ERRORS.put (305, "HCI_ERR_HWR_UPLOAD_NO_DATA 没有可用于上传的数据");
		MAP_HCI_ERRORS.put (306, "HCI_ERR_HWR_ENGINE_SESSION_START_FAILED HWR本地引擎开启会话失败");
		MAP_HCI_ERRORS.put (307, "HCI_ERR_HWR_ENGINE_NOT_INIT SDK初始化时未传入本地能力却在创建会话时使用了本地能力");
		MAP_HCI_ERRORS.put (308, "HCI_ERR_HWR_CONFIG_SUBLANG_MISSING 单字能力、多语种字典时，未传入sublang");
		MAP_HCI_ERRORS.put (309, "HCI_ERR_HWR_TOO_MANY_DOMAIN 传入了领域数目超过了4个");
		MAP_HCI_ERRORS.put (400, "HCI_ERR_OCR_NOT_INIT HCI_OCR 没有初始化");
		MAP_HCI_ERRORS.put (401, "HCI_ERR_OCR_ALREADY_INIT HCI_OCR 多次初始化");
		MAP_HCI_ERRORS.put (402, "HCI_ERR_OCR_ENGINE_INIT_FAILED OCR本地引擎初始化失败");
		MAP_HCI_ERRORS.put (403, "HCI_ERR_OCR_ENGINE_FAILED OCR本地引擎操作（倾斜校正、版面分析、识别、预处理、压缩）失败");
		MAP_HCI_ERRORS.put (404, "HCI_ERR_OCR_ENGINE_NOT_INIT SDK初始化时未传入本地能力却在创建会话时使用了本地能力");
		MAP_HCI_ERRORS.put (405, "HCI_ERR_OCR_LOAD_IMAGE 载入本地文件或者本地图片缓冲失败");
		MAP_HCI_ERRORS.put (406, "HCI_ERR_OCR_SAVE_IMAGE 保存OCR_IMAGE到本地文件失败");
		MAP_HCI_ERRORS.put (407, "HCI_ERR_OCR_IMAGE_NOT_SET 未设置要处理的图片就进行了倾斜校正、版面分析、识别等操作");
		MAP_HCI_ERRORS.put (500, "HCI_ERR_TTS_NOT_INIT HCI_TTS 没有初始化");
		MAP_HCI_ERRORS.put (501, "HCI_ERR_TTS_ALREADY_INIT HCI_TTS 多次初始化");
		MAP_HCI_ERRORS.put (502, "HCI_ERR_TTS_SESSION_BUSY TTS会话正忙，例如在合成回调函数中又调用了合成");
		MAP_HCI_ERRORS.put (503, "HCI_ERR_TTS_ENGINE_SESSION_START_FAILED TTS本地引擎开启会话失败");
		MAP_HCI_ERRORS.put (504, "HCI_ERR_TTS_ENGINE_FAILED 本地引擎合成失败");
		MAP_HCI_ERRORS.put (505, "HCI_ERR_TTS_ENGINE_INIT_FAILED TTS(NU)本地引擎初始化失败");
		MAP_HCI_ERRORS.put (506, "HCI_ERR_TTS_ENGINE_NOT_INIT TTS(NU)本地引擎尚未初始化");
		MAP_HCI_ERRORS.put (600, "HCI_ERR_MT_NOT_INIT HCI_MT没有初始化");
		MAP_HCI_ERRORS.put (601, "HCI_ERR_MT_ALREADY_INIT HCI_MT多次初始化");
		MAP_HCI_ERRORS.put (700, "HCI_ERR_NLU_NOT_INIT HCI_NLU没有初始化");
		MAP_HCI_ERRORS.put (701, "HCI_ERR_NLU_ALREADY_INIT HCI_NLU多次初始化");
		MAP_HCI_ERRORS.put (702, "HCI_ERR_NLU_ENGINE_SESSION_START_FAILED NLU本地引擎开启会话失败");
		MAP_HCI_ERRORS.put (703, "HCI_ERR_NLU_ENGINE_FAILED NLU本地引擎识别失败");
		MAP_HCI_ERRORS.put (800, "HCI_ERR_KB_NOT_INIT HCI_KB没有初始化");
		MAP_HCI_ERRORS.put (801, "HCI_ERR_KB_ALREADY_INIT HCI_KB多次初始化");
		MAP_HCI_ERRORS.put (802, "HCI_ERR_KB_ENGINE_SESSION_START_FAILED KB本地引擎开启会话失败");
		MAP_HCI_ERRORS.put (803, "HCI_ERR_KB_ENGINE_FAILED KB本地引擎合成失败");
		MAP_HCI_ERRORS.put (804, "HCI_ERR_KB_SYLLABLE_INVALID 容错音节无法判断类型");
		MAP_HCI_ERRORS.put (805, "HCI_ERR_KB_UDB_WORD_EXIST 已经在用户词库中存在");
		MAP_HCI_ERRORS.put (806, "HCI_ERR_KB_CONFIRM_NO_TASK 使用confirm，但没有确认任务");
		MAP_HCI_ERRORS.put (900, "HCI_ERR_VPR_NOT_INIT HCI_VPR没有初始化");
		MAP_HCI_ERRORS.put (901, "HCI_ERR_VPR_ALREADY_INIT HCI_VPR多次初始化");
		MAP_HCI_ERRORS.put (902, "HCI_ERR_VPR_ENGINE_INIT_FAILED VPR本地引擎初始化失败");
		MAP_HCI_ERRORS.put (903, "HCI_ERR_VPR_ENGINE_FAILED VPR本地引擎处理失败");
		MAP_HCI_ERRORS.put (904, "HCI_ERR_VPR_USERID_NOT_EXIST VPR用户不存在");
		MAP_HCI_ERRORS.put (905, "HCI_ERR_VPR_ENGINE_SESSION_START_FAILED VPR本地引擎开启会话失败 ");
	}
	Session session = new Session();
	NluConfig nlu_config = new NluConfig ();

	public void Init ()
	{
		// 创建初始化参数辅助类
		InitParam initparam = new InitParam();
		// 授权文件所在路径，此项必填
		String authDirPath = "C:\\";
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTH_PATH, authDirPath);
		// 是否自动访问云授权
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTO_CLOUD_AUTH, "no");
		// 灵云云服务的接口地址，此项必填
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_CLOUD_URL, "http://test.api.hcicloud.com:8888");
		//initparam.addParam(InitParam.AuthParam.PARAM_KEY_CLOUD_URL, "192.168.115.34:8888");
		// 开发者密钥，此项必填，由捷通华声提供
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_DEVELOPER_KEY, "55918af358c9d7c596dddd6733b41e64");
		//initparam.addParam(InitParam.AuthParam.PARAM_KEY_DEVELOPER_KEY, "developer_key");
		// 应用程序序号，此项必填，由捷通华声提供
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_APP_KEY, "cd5d549c");
		//initparam.addParam(InitParam.AuthParam.PARAM_KEY_APP_KEY, "ac5d5452");

		/*
		// 日志的路径，可选，如果不传或者为空则不生成日志
		String logDirPath = "";
		initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_PATH, logDirPath);
		// 日志数目，默认保留多少个日志文件，超过则覆盖最旧的日志
		initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_COUNT, "5");
		// 日志大小，默认一个日志文件写多大，单位为K
		initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_SIZE, "1024");
		// 日志等级，0=无，1=错误，2=警告，3=信息，4=细节，5=调试，SDK将输出小于等于logLevel的// 日志信息
		initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_LEVEL, "5");
		*/

		// 灵云系统初始化
		// 第二个参数在Android平台下，必须为当前的Context，在Windows/Linux平台下，可以为null
		System.out.println (initparam.getStringConfig());
		int nReturnCode = HciCloudSys.hciInit (initparam.getStringConfig(), null);
		if (nReturnCode != HciErrorCode.HCI_ERR_NONE)
		{
			// "系统初始化失败"
			System.err.println ("系统初始化失败: " + nReturnCode + " " + MAP_HCI_ERRORS.get (nReturnCode));
			return;
		}


		nReturnCode = HciCloudSys.hciCheckAuth();
		if (nReturnCode != HciErrorCode.HCI_ERR_NONE)
		{
			System.err.println ("获取授权文件失败: " + nReturnCode + " " + MAP_HCI_ERRORS.get (nReturnCode));
			return;
		}


		NluInitParam nlu_init_param = new NluInitParam ();
		//nlu_init_param.addParam (NluInitParam.PARAM_KEY_INIT_CAP_KEYS, "nlu.cloud");
		System.out.println (nlu_init_param.getStringConfig ());
		nReturnCode = HciCloudNlu.hciNluInit (nlu_init_param.getStringConfig ());
		if (nReturnCode != HciErrorCode.HCI_ERR_NONE)
		{
			System.err.println ("语义理解功能初始化失败: " + nReturnCode + " " + MAP_HCI_ERRORS.get (nReturnCode));
			return;
		}

		//NluConfig nlu_config = new NluConfig ();
		nlu_config.addParam (NluConfig.SessionConfig.PARAM_KEY_CAP_KEY, "nlu.cloud");
		//nlu_config.addParam (NluConfig.ResultConfig.PARAM_KEY_INTENTION, "weather;joke");
		nlu_config.addParam (NluConfig.ResultConfig.PARAM_KEY_INTENTION, "baike");
		//nlu_config.addParam (NluConfig.ResultConfig.PARAM_KEY_INTENTION, "聊天");
		System.out.println (nlu_config.getStringConfig ());

		nReturnCode = HciCloudNlu.hciNluSessionStart (nlu_config.getStringConfig (), session);
		if (nReturnCode != HciErrorCode.HCI_ERR_NONE)
		{
			System.err.println ("语义理解会话启动失败: " + nReturnCode + " " + MAP_HCI_ERRORS.get (nReturnCode));
			return;
		}
	}

	public NluRecogResult Recognize (String sQuestion)
	{
		int nReturnCode;
		String recogConfig = " " ;
		NluRecogResult nluResult = new NluRecogResult();
		System.out.println ("在 " + nlu_config.getParam (NluConfig.ResultConfig.PARAM_KEY_INTENTION) + " 中理解 【" + sQuestion + "】");
		nReturnCode = HciCloudNlu.hciNluRecog (session, sQuestion, recogConfig, nluResult);

		if(nReturnCode != HciErrorCode.HCI_ERR_NONE)
		{
			//System.err.println ("语义理解识别失败: " + nReturnCode + " " + MAP_HCI_ERRORS.get (nReturnCode));
			//return null;
			throw new RuntimeException ("语义理解识别失败: " + nReturnCode + " " + MAP_HCI_ERRORS.get (nReturnCode));
		}
		System.out.println ("----------------------------------");
		System.out.println (nluResult);
		System.out.println ("----------------------------------");
		System.out.println (nluResult.getRecogResultItemList ());
		return nluResult;
	}

	public void Close ()
	{
		int nReturnCode;
		// 关闭功能会话
		nReturnCode = HciCloudNlu.hciNluSessionStop (session);
		// 释放功能
		nReturnCode = HciCloudNlu.hciNluRelease ();

		// 终止灵云系统
		nReturnCode = HciCloudSys.hciRelease ();
	}

	public static void main (String[] args)
	{
		HCICloudNLUBot bot = new HCICloudNLUBot ();
		bot.Init ();
		bot.Recognize (args[0]);
		bot.Close ();
	}
}
