/**
要赋给 mysql 用户对 mysql.proc SELECT 的权限，否则 bot 执行存储过程报错。例如：
	CREATE USER bot@localhost;
	GRANT ALL ON bot.* TO bot@localhost;
	GRANT SELECT ON mysql.proc TO bot@localhost;

	CREATE USER ''@localhost;
	GRANT ALL ON bot.* TO ''@localhost;
	GRANT SELECT ON mysql.proc TO ''@localhost;

	CREATE USER bot@'192.168.2.%';
	GRANT ALL ON bot.* TO bot@'192.168.2.%';
	GRANT SELECT ON mysql.proc TO bot@'192.168.2.%';

	CREATE USER bot@'%';
	GRANT ALL ON bot.* TO bot@'%';
	GRANT SELECT ON mysql.proc TO bot@'%';

*/

CREATE TABLE bot_ban
(
	target VARCHAR(100) CHARACTER SET ascii NOT NULL DEFAULT '',
	channel VARCHAR(20) CHARACTER SET ascii NOT NULL DEFAULT '',
	cmd VARCHAR(20) NOT NULL DEFAULT '',
	disabled BOOLEAN NOT NULL DEFAULT false,	/* */
	ban_time DATETIME DEFAULT CURRENT_TIMESTAMP,
	ban_time_length INT	/* 分钟 */
);

/*
记录 irc kick/ban 管理日志
*/
CREATE TABLE irc_votes
(
	vote_id INT UNSIGNED AUTO_INCREMENT,
	channel VARCHAR(20) CHARACTER SET ascii NOT NULL DEFAULT '',
	action ENUM('', 'kick', 'ban', 'kickban', 'quiet', 'voice', 'op'),
	target VARCHAR(100) CHARACTER SET ascii NOT NULL DEFAULT '',
	reason VARCHAR(100) NOT NULL DEFAULT '',
	operator_nick VARCHAR(20) CHARACTER SET ascii NOT NULL DEFAULT '',
	operator_user VARCHAR(20) CHARACTER SET ascii NOT NULL DEFAULT '',
	operator_host VARCHAR(100) CHARACTER SET ascii NOT NULL DEFAULT '',
	operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时刻',
	time_length INT NOT NULL DEFAULT 300 COMMENT '对于 ban 或者 kickban 操作的默认操作生效的秒数。如果是 -1，则表示永久生效',
	time_unit ENUM('', 's', 'm', 'q', 'h', 'd', 'w', 'month', 'season', 'hy', 'y') NOT NULL DEFAULT 's' COMMENT '时间单位，默认为空白，空白=s(秒)',
	undo_operator_nick VARCHAR(20) CHARACTER SET ascii NOT NULL DEFAULT '',
	undo_operator_user VARCHAR(20) CHARACTER SET ascii NOT NULL DEFAULT '',
	undo_operator_host VARCHAR(100) CHARACTER SET ascii NOT NULL DEFAULT '',
	undo_reason VARCHAR(100) NOT NULL DEFAULT '',
	undo_time DATETIME COMMENT '取消操作时刻',

	PRIMARY KEY PK__irc_votes (vote_id),
	INDEX IX__irc_votes__channel_action_target (channel, action, target),
	INDEX IX__irc_votes__operator_host (operator_host),
	INDEX IX__irc_votes__undo_operator_host (undo_operator_host)
) ENGINE MyISAM CHARACTER SET UTF8;

/*******************************************************************************

词条/标签

*******************************************************************************/

CREATE TABLE dic_digests
(
	content_digest CHAR(40) CHARACTER SET ascii PRIMARY KEY,
	content VARCHAR(500) NOT NULL DEFAULT '',
	content_lowercase VARCHAR(500) NOT NULL DEFAULT ''
) ENGINE MyISAM CHARACTER SET UTF8MB4;


CREATE TABLE dics
(
	q_digest CHAR(40) CHARACTER SET ascii NOT NULL DEFAULT '',
	a_digest CHAR(40) CHARACTER SET ascii NOT NULL DEFAULT '',
	q_number INT UNSIGNED NOT NULL AUTO_INCREMENT,

	fetched_times INT NOT NULL DEFAULT 0,
	added_by VARCHAR(16) CHARACTER SET ascii NOT NULL DEFAULT '',
	added_time datetime,
	updated_by VARCHAR(16) CHARACTER SET ascii NOT NULL DEFAULT '',
	updated_time datetime,
	updated_times INT NOT NULL DEFAULT 0,

	enabled TINYINT(1) NOT NULL DEFAULT 1,

	PRIMARY KEY PK__Q_SN (q_digest, q_number),	/* InnoDB 存储引擎不支持混合主键，只能用 MyISAM 存储引擎。 http://stackoverflow.com/questions/23794624/auto-increment-how-to-auto-increment-a-combined-key-error-1075 */
	UNIQUE KEY UQ__Q_A (q_digest, a_digest)
) ENGINE MyISAM CHARACTER SET UTF8;


CREATE VIEW v_dics
AS
	/* 必须把所有主键都 SELECT 出来，否则在 java 的结果集中更新字段会报错：
	Result Set not updatable (referenced table has no primary keys).This result set must come from a statement that was created with a result set type of ResultSet.CONCUR_UPDATABLE, the query must select only one table, can not use functions and must select all primary keys from that table. See the JDBC 2.1 API Specification, section 5.6 for more details.
	 */
	SELECT
		d.*,
		dd_q.content_digest AS q_content_digest,
		dd_q.content AS q_content,
		dd_a.content_digest AS a_content_digest,
		dd_a.content AS a_content
	FROM dics d
		JOIN dic_digests dd_q ON d.q_digest=dd_q.content_digest
		JOIN dic_digests dd_a ON d.a_digest=dd_a.content_digest
	;


DELIMITER $$
CREATE PROCEDURE p_savedic
(
	_q VARCHAR(500) CHARACTER SET UTF8MB4,	/* question */
	_a VARCHAR(500) CHARACTER SET UTF8MB4,	/* answer */
	_n VARCHAR(16),	/* nick name 添加人的 IRC 昵称 */
	_u VARCHAR(16),	/* login 添加人的 IRC 登录帐号 */
	_h VARCHAR(50)	/* host 添加人的 IRC 登录主机 */
)
BEGIN
	DECLARE _q_lowercase, _a_lowercase VARCHAR(500) CHARACTER SET UTF8MB4 DEFAULT '';
	DECLARE _q_lowercase_digest, _a_lowercase_digest, _temp_digest_string CHAR(40) CHARACTER SET ascii;
	DECLARE _q_lowercase_digest_binary, _a_lowercase_digest_binary BINARY(20) DEFAULT '';
	SET _q = TRIM(_q);
	SET _a = TRIM(_a);
	SET _q_lowercase = LOWER(_q);
	SET _a_lowercase = LOWER(_a);
	SET _q_lowercase_digest = SHA1(_q_lowercase);
	SET _a_lowercase_digest = SHA1(_a_lowercase);
	SET _q_lowercase_digest_binary = UNHEX(_q_lowercase_digest);
	SET _a_lowercase_digest_binary = UNHEX(_a_lowercase_digest);

	/* 检查 _q 是否在 digest 中存在 */
	SET _temp_digest_string = NULL;
	SELECT content_digest INTO _temp_digest_string FROM dic_digests WHERE content_digest=_q_lowercase_digest;
	IF _temp_digest_string IS NULL THEN
		INSERT INTO dic_digests (content_digest, content, content_lowercase) VALUES (_q_lowercase_digest, _q, _q_lowercase);
	ELSE
		UPDATE dic_digests SET content=_q WHERE content_digest=_q_lowercase_digest;
	END IF;

	/* 检查 _a 是否在 digest 中存在 */
	SET _temp_digest_string = NULL;
	SELECT content_digest INTO _temp_digest_string FROM dic_digests WHERE content_digest=_a_lowercase_digest;
	IF _temp_digest_string IS NULL THEN
		INSERT INTO dic_digests (content_digest, content, content_lowercase) VALUES (_a_lowercase_digest, _a, _a_lowercase);
	ELSE
		UPDATE dic_digests SET content=_a WHERE content_digest=_a_lowercase_digest;
	END IF;

	/* 保存 */
	INSERT INTO dics (q_digest, a_digest, added_by, added_time)
		VALUES (_q_lowercase_digest, _a_lowercase_digest, _n, NOW())
		ON DUPLICATE KEY UPDATE updated_by=_n, updated_time=NOW(), updated_times=updated_times+1;

	/* 取出 */
	SELECT * FROM dics WHERE q_digest=_q_lowercase_digest AND a_digest=_a_lowercase_digest;
END
$$
DELIMITER ;


DELIMITER $$
CREATE PROCEDURE p_getdic
(
	_q VARCHAR(500) CHARACTER SET UTF8MB4,	/* question */
	_q_number INT UNSIGNED,	/* 如果未指定，则随机取。如果指定了数值，则取特定记录 */
	_reverse BOOLEAN	/* 是否“反查”，如果是“反查”，则搜索 answer 中包含（模糊匹配）查询字符串的 questions */
)
BEGIN
	DECLARE _q_lowercase VARCHAR(500) CHARACTER SET UTF8MB4;
	DECLARE _q_lowercase_digest CHAR(40) CHARACTER SET ascii;
	DECLARE _q_lowercase_digest_binary BINARY(20);
	DECLARE _count_ALL, _max_id_ALL, _count_enabled, _max_id_enabled INT UNSIGNED;	/* max_id 用来计算最大 ID 的字符串长度，用以对齐输出 */

	SET _q = TRIM(_q);
	SET _q_lowercase = LOWER(_q);
	SET _q_lowercase_digest = SHA1(_q_lowercase);
	SET _q_lowercase_digest_binary = UNHEX(_q_lowercase_digest);
	IF _reverse IS NOT NULL AND _reverse THEN
		SELECT
			COUNT(*),
			MAX(q_number),
			COUNT(CASE WHEN enabled=1 THEN 1 ELSE NULL END),
			MAX(CASE WHEN enabled=1 THEN q_number ELSE 0 END)
		INTO
			_count_ALL,
			_max_id_ALL,
			_count_enabled,
			_max_id_enabled
		FROM v_dics
		WHERE
			a_content LIKE CONCAT('%', _q, '%');

		SELECT
			q_digest, a_digest, q_number, fetched_times, added_by, added_time, updated_by, updated_time, updated_times, enabled,
			q_content_digest, q_content, a_content_digest, a_content,
			_count_ALL AS COUNT,
			_max_id_ALL AS MAX_ID,
			_count_enabled AS COUNT_ENABLED,
			_max_id_enabled AS MAX_ID_ENABLED
		FROM
			v_dics
		WHERE
			a_content LIKE CONCAT('%', _q, '%')
		ORDER BY RAND()
		;
	ELSE
		SELECT
			COUNT(*),
			MAX(q_number),
			COUNT(CASE WHEN enabled=1 THEN 1 ELSE NULL END),
			MAX(CASE WHEN enabled=1 THEN q_number ELSE 0 END)
		INTO
			_count_ALL,
			_max_id_ALL,
			_count_enabled,
			_max_id_enabled
		FROM
			v_dics
		WHERE
			q_digest = _q_lowercase_digest
		;

		IF _q_number IS NULL OR _q_number = 0 THEN
			SELECT
				q_digest, a_digest, q_number, fetched_times, added_by, added_time, updated_by, updated_time, updated_times, enabled,
				q_content_digest, q_content, a_content_digest, a_content,
				_count_ALL AS COUNT,
				_max_id_ALL AS MAX_ID,
				_count_enabled AS COUNT_ENABLED,
				_max_id_enabled AS MAX_ID_ENABLED
			FROM
				v_dics
			WHERE
				q_digest = _q_lowercase_digest
			;
		ELSE
			SELECT
				q_digest, a_digest, q_number, fetched_times, added_by, added_time, updated_by, updated_time, updated_times, enabled,
				q_content_digest, q_content, a_content_digest, a_content,
				_count_ALL AS COUNT,
				_max_id_ALL AS MAX_ID,
				_count_enabled AS COUNT_ENABLED,
				_max_id_enabled AS MAX_ID_ENABLED
			FROM
				v_dics
			WHERE
				q_digest = _q_lowercase_digest
				AND q_number=_q_number
			;
		END IF;
	END IF;
END
$$
DELIMITER ;


DELIMITER $$
CREATE PROCEDURE p__fix_dic_q_number_gap ()
BEGIN
	DECLARE done BOOL DEFAULT FALSE;
	DECLARE var_q_digest CHAR(40);
	DECLARE var_qn, var_max, var_dic_count INT;
	DECLARE q_counter INT DEFAULT 0;
	DECLARE curDicsNeedToFix CURSOR FOR SELECT q_digest, /*dd.content,*/ MAX(q_number), COUNT(*) FROM dics d JOIN dic_digests dd ON d.q_digest = dd.content_digest GROUP BY q_digest HAVING MAX(q_number) > COUNT(*);
	DECLARE curDicDefinitionQNumbers CURSOR FOR SELECT q_number FROM dics WHERE q_digest = var_q_digest;
/*
+------------------------------------------+-----------+---------------+----------+
| q_digest                                 | content   | MAX(q_number) | COUNT(*) |
+------------------------------------------+-----------+---------------+----------+
| 873ba9ccf23c55952f50c272dee3b459721a80ba | condy     |            36 |       34 |
| a67e4ebbcdcabfc25b8ac2e903ae022dd948d5ca | bruceutut |           102 |       70 |
+------------------------------------------+-----------+---------------+----------+
*/

	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

	OPEN curDicsNeedToFix;

	outer_loop:
	LOOP
		FETCH curDicsNeedToFix INTO var_q_digest, var_max, var_dic_count;
		IF done THEN
			LEAVE outer_loop;
		END IF;

		SET done = FALSE;
		OPEN curDicDefinitionQNumbers;
		inner_loop: LOOP
			FETCH curDicDefinitionQNumbers INTO var_qn;
			IF done THEN
				LEAVE inner_loop;
			END IF;

			SET q_counter = q_counter + 1;

			IF q_counter < var_qn THEN
				UPDATE dics SET q_number = q_counter WHERE q_digest = var_q_digest AND q_number = var_qn;
			END IF;
		END LOOP;
		CLOSE curDicDefinitionQNumbers;
	END LOOP;

	CLOSE curDicsNeedToFix;
END;
$$
DELIMITER ;


/*******************************************************************************

	HTML/JSON 解析器模板

*******************************************************************************/
CREATE TABLE ht_templates
(
	id INT UNSIGNED NOT NULL AUTO_INCREMENT,
	name VARCHAR(50) NOT NULL DEFAULT '' UNIQUE KEY,

	url VARCHAR(300) NOT NULL DEFAULT '',
	use_gfw_proxy TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用 GFWProxy 代理服务器访问该 URL。默认不使用，除非特别的接口需要翻墙才能访问时才需要使用。',
	ignore_https_certificate_validation TINYINT(1) NOT NULL DEFAULT 1 COMMENT '忽略 https 的证书有效性验证 -- ht 命令非关键任务，不需要验证证书有效性；此参数仅对 https 网址生效， http 不处理该参数。参见: http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/',
	content_type ENUM('', 'html', 'json', 'js') NOT NULL DEFAULT '',
	ignore_content_type TINYINT(1) NOT NULL DEFAULT 1 COMMENT '这是给 jsoup 库用到的是否忽略 http 返回的内容类型，json 目前不关心此信息（一直假定为文本数据）',
	js_cut_start INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '这是为了给 json 使用的：有的接口返回了回调函数，回调函数里的参数是 JSON，这时候就需要把 JSON 切出来。该参数指定切的起始偏移量，>=0，=0 表示不切前面的字符',
	js_cut_end INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '该参数指定从后面切的字符数，数值需要 >=0，=0 表示不切后面的字符',
	url_param_usage VARCHAR(100) NOT NULL DEFAULT '' COMMENT '如果 url 中带参数，在此说明参数用途。如果用户没有输入参数时，给出提示',

	selector VARCHAR(100) NOT NULL DEFAULT '' COMMENT '用来选择列表的 CSS 选择器表达式，此表达式仅用于 content_type 为 html 的情况，对于 json/js，不需要。',
	sub_selector VARBINARY(1024) NOT NULL DEFAULT '' COMMENT '当 content_type 为 html 时，用来选择列表内单一 element 的 CSS 选择器表达式，如果为空，则 element 就是列表中的 element； 当 content_type 为 json/js 时，用来存储 javascript 脚本',
	padding_left VARCHAR(20) NOT NULL DEFAULT '' COMMENT '取值后，填充在 左侧//前面 的字符串。可根据需要决定该字符串，以决定输出的样式（比如：更改输出的颜色、输出空格等）',
	extract VARCHAR(20) NOT NULL DEFAULT '',	/* 原数据类型:  ENUM('','text', 'html','inner','innerhtml', 'outerhtml','outer', 'attr','attribute', 'tagname', 'nodename', 'classname', 'owntext', 'data', 'id', 'val','value') */
	attr VARCHAR(20) NOT NULL DEFAULT '' COMMENT '当 extract 为 attr 时，指定 attr 参数',
	format_flags VARCHAR(1) NOT NULL DEFAULT '' COMMENT '格式化字符串中的“标志”，如 “-”“0”。在此程序中，应当只用到“-”和空字符串。默认为空字符串 -- 无标志',
	format_width VARCHAR(3) NOT NULL DEFAULT '' COMMENT '格式化字符串中的宽度。默认为空 -- 不指定宽度。',
	padding_right VARCHAR(20) NOT NULL DEFAULT '' COMMENT '取值后，填充在 右侧/后面 的字符串。可根据需要决定该字符串，以决定输出的样式（比如：闭合颜色序列、输出空格等）',

	ua VARCHAR(100) NOT NULL DEFAULT '' COMMENT '模拟浏览器 User-Agent',
	request_method ENUM('','GET', 'POST') NOT NULL DEFAULT '' COMMENT 'HTTP 方法，只允许 GET 和 POST',
	referer VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'Referer 头',

	max TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '最多获取/显示多少行。注意: 此数值仍然受 bot 最大响应行数上限的限制',

	source_type ENUM('irc', 'wechat') NOT NULL DEFAULT 'irc' COMMENT '添加模板时的来源类型。目前支持 irc 和 wechat 两种，因为 irc 和微信输出略有区别，所以在不同的机器人处理时，需要特殊处理一下：微信中输出 irc 中添加的模板时，要剔除 padding 中的 irc 转义序列字符串、单行/多行；irc 中输出微信中添加的模板时，utf8mb4 处理？…',
	added_by VARCHAR(16) NOT NULL DEFAULT '',
	added_by_user VARCHAR(16)  NOT NULL DEFAULT '',
	added_by_host VARCHAR(100) NOT NULL DEFAULT '',
	added_time datetime,
	updated_by VARCHAR(16) NOT NULL DEFAULT '',
	updated_by_user VARCHAR(16) NOT NULL DEFAULT '',
	updated_by_host VARCHAR(100) NOT NULL DEFAULT '',
	updated_time datetime,
	updated_times INT NOT NULL DEFAULT 0,

	PRIMARY KEY PK__ht_templates (id DESC),	/* http://stackoverflow.com/questions/10130230/sql-index-performance-asc-vs-desc DESC indexing is not currently implemented in MySQL... the engine ignores the provided sort and always uses ASC: */
	UNIQUE KEY UQ__ht_templates (name)
) ENGINE MyISAM CHARACTER SET UTF8;

CREATE TABLE ht_templates_other_sub_selectors
(
	template_id INT UNSIGNED NOT NULL DEFAULT 0,
	sub_selector_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
	sub_selector VARBINARY(1024) NOT NULL DEFAULT '' COMMENT '当 ht_template 主表的 content_type 为 html 时，用来选择列表内单一 element 的 CSS 选择器表达式，如果为空，则 element 就是列表中的 element； 当 content_type 为 json/js 时，用来存储 javascript 脚本',
	padding_left VARCHAR(20) NOT NULL DEFAULT ' ' COMMENT '取值后，填充在 左侧//前面 的字符串。可根据需要决定该字符串，以决定输出的样式（比如：更改输出的颜色、输出空格等）',
	extract VARCHAR(20) NOT NULL DEFAULT '',
	attr VARCHAR(20) NOT NULL DEFAULT '' COMMENT '当 extract 为 attr 时，指定 attr 参数',
	format_flags VARCHAR(1) NOT NULL DEFAULT '' COMMENT '格式化字符串中的“标志”，如 “-”“0”。在此程序中，应当只用到“-”和空字符串。默认为空字符串 -- 无标志',
	format_width VARCHAR(3) NOT NULL DEFAULT '' COMMENT '格式化字符串中的宽度。默认为空 -- 不指定宽度。',
	padding_right VARCHAR(20) NOT NULL DEFAULT '' COMMENT '取值后，填充在 右侧/后面 的字符串。可根据需要决定该字符串，以决定输出的样式（比如：闭合颜色序列、输出空格等）',

	PRIMARY KEY PK__ht_templates_other_sub_selectors (template_id DESC, sub_selector_id)	/* http://stackoverflow.com/questions/10130230/sql-index-performance-asc-vs-desc DESC indexing is not currently implemented in MySQL... the engine ignores the provided sort and always uses ASC: */
) ENGINE MyISAM CHARACTER SET UTF8 COMMENT '此表是 ht_templates 的延伸：允许一个模板有多个 sub_selector，这样可以让多个 sub element 组成成一个完整的信息';


DELIMITER $$
CREATE PROCEDURE p_save_ht_template
(
	_id INT UNSIGNED,
	_name VARCHAR(50),

	_url VARCHAR(300),
	_url_param_usage VARCHAR(100),
	_selector VARCHAR(100),
	_sub_selector VARCHAR(100),
	_extract VARCHAR(20),
	_attr VARCHAR(20),

	_ua VARCHAR(100),
	_method VARCHAR(10),
	_referer VARCHAR(100),

	_max TINYINT,

	_nick VARCHAR(16),
	_user VARCHAR(16),
	_host VARCHAR(100)
)
BEGIN
	DECLARE _temp_id INT DEFAULT 0;

	/* 检查模板是否在已存在 */
	SET _temp_id = NULL;
	IF _id IS NOT NULL AND _id<>0 THEN
		SELECT id INTO _temp_id FROM ht_templates WHERE id=_id;
	ELSE
		SELECT id INTO _temp_id FROM ht_templates WHERE name=_name;
	END IF;

	IF _temp_id IS NULL THEN
		INSERT ht_templates (name, url, url_param_usage, selector, sub_selector, extract, attr, ua, method, referer, max, added_by, added_by_user, added_by_host, added_time)
		VALUES (_name, _url, _url_param_usage, _selector, _sub_selector, _extract, _attr, _ua, _method, _referer, _max, _nick, _user, _host, CURRENT_TIMESTAMP)
		;
		SELECT LAST_INSERT_ID();
	ELSE
		UPDATE ht_templates
		SET
			name = _name,
			url = _url,
			url_param_usage = _url_param_usage,
			selector = _selector,
			sub_selector = _sub_selector,
			extract = _extract,
			attr = _attr,
			ua = _ua,
			method = _method,
			referer = _referer,
			max = _max,
			updated_by = _nick,
			updated_by_user = _user,
			updated_by_host = _host,
			updated_time = CURRENT_TIMESTAMP,
			updated_times = updated_time + 1
		WHERE id=_id
		;
		SELECT _id;
	END IF;
END
$$
DELIMITER ;




/*******************************************************************************

类似 263 跑车的动作表情命令数据库

*******************************************************************************/
CREATE TABLE actions
(
	type TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '0(00) - 自己动作，无参数。这个在 irc 频道里应该不会使用； 1(01) - 自己做动作，有第二个人做参数。这个在 irc 频道里应该不会使用； 2(10) - 代替别人做动作，无参数； 3(11) - 代替别人做动作，有第二个人做参数',
	cmd VARCHAR(50) NOT NULL DEFAULT '',	/* 简短易记的命令，不能有空格 */
	action VARCHAR(200) CHARACTER SET UTF8MB4 NOT NULL DEFAULT '',	/* */
	action_number INT UNSIGNED NOT NULL AUTO_INCREMENT,
	language VARCHAR(3) CHARACTER SET ascii NOT NULL DEFAULT '',
	source VARCHAR(10) NOT NULL DEFAULT '',	/* 数据来源，用来记录这些数据是从哪里来的，比如，直接从 263 聊天跑车的文件中“导出/复制粘贴”的 */
	gender VARCHAR(1) NOT NULL DEFAULT '',

	fetched_times INT NOT NULL DEFAULT 0,
	added_by VARCHAR(16) CHARACTER SET ascii NOT NULL DEFAULT '' COMMENT '添加人的 IRC 昵称',
	added_by_user VARCHAR(16) CHARACTER SET ascii NOT NULL DEFAULT '' COMMENT '添加人的 IRC 用户',
	added_by_host VARCHAR(100) CHARACTER SET ascii NOT NULL DEFAULT '' COMMENT '添加人的 IRC 主机',
	added_time datetime,
	updated_by VARCHAR(16) CHARACTER SET ascii NOT NULL DEFAULT '' COMMENT '更新人的 IRC 昵称',
	updated_by_user VARCHAR(16) CHARACTER SET ascii NOT NULL DEFAULT '' COMMENT '更新人的 IRC 用户',
	updated_by_host VARCHAR(16) CHARACTER SET ascii NOT NULL DEFAULT '' COMMENT '更新人的 IRC 主机',
	updated_time datetime,
	updated_times INT UNSIGNED NOT NULL DEFAULT 0,

	enabled TINYINT(1) NOT NULL DEFAULT 1,

	PRIMARY KEY PK__actions (type, action, action_number)	/* InnoDB 存储引擎不支持混合主键，只能用 MyISAM 存储引擎。 http://stackoverflow.com/questions/23794624/auto-increment-how-to-auto-increment-a-combined-key-error-1075 */
	UNIQUE KEY UQ__actions (type, cmd, action)
) ENGINE MyISAM CHARACTER SET UTF8 COMMENT '类似 263 跑车的动作表情命令数据库';
