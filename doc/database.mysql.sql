CREATE TABLE dic_digests
(
	content_digest CHAR(40) CHARACTER SET ascii PRIMARY KEY,
	content VARCHAR(500) NOT NULL DEFAULT '',
	content_lowercase VARCHAR(500) NOT NULL DEFAULT ''
) ENGINE MyISAM CHARACTER SET UTF8;


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
	_q VARCHAR(500),	/* question */
	_a VARCHAR(500),	/* answer */
	_n VARCHAR(16),	/* nick name 添加人的 IRC 昵称 */
	_u VARCHAR(16),	/* login 添加人的 IRC 登录帐号 */
	_h VARCHAR(50)	/* host 添加人的 IRC 登录主机 */
)
BEGIN
	DECLARE _q_lowercase, _a_lowercase VARCHAR(500) DEFAULT '';
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
	_q VARCHAR(500),	/* question */
	_q_number INT UNSIGNED,	/* 如果未指定，则随机取。如果指定了数值，则取特定记录 */
	_reverse BOOLEAN	/* 是否“反查”，如果是“反查”，则搜索 answer 中包含（模糊匹配）查询字符串的 questions */
)
BEGIN
	DECLARE _q_lowercase VARCHAR(500);
	DECLARE _q_lowercase_digest CHAR(40) CHARACTER SET ascii;
	DECLARE _q_lowercase_digest_binary BINARY(20);
	DECLARE _count, _max_id INT UNSIGNED;	/* max_id 用来计算最大 ID 的字符串长度，用以对齐输出 */

	SET _q = TRIM(_q);
	SET _q_lowercase = LOWER(_q);
	SET _q_lowercase_digest = SHA1(_q_lowercase);
	SET _q_lowercase_digest_binary = UNHEX(_q_lowercase_digest);
	IF _reverse IS NOT NULL AND _reverse THEN
		SELECT COUNT(*), MAX(q_number) INTO _count, _max_id
		FROM v_dics
		WHERE
			a_content LIKE CONCAT('%', _q, '%');

		SELECT *, _count AS COUNT, _max_id AS MAX_ID
		FROM v_dics
		WHERE
			a_content LIKE CONCAT('%', _q, '%')
		ORDER BY RAND()
		;
	ELSE
		SELECT COUNT(*), MAX(q_number) INTO _count, _max_id
		FROM v_dics
		WHERE
			q_digest = _q_lowercase_digest
		;

		IF _q_number IS NULL OR _q_number = 0 THEN
			SELECT *, _count AS COUNT, _max_id AS MAX_ID
			FROM v_dics
			WHERE
				q_digest = _q_lowercase_digest
			;
		ELSE
			SELECT *, _count AS COUNT, _max_id AS MAX_ID
			FROM v_dics
			WHERE
				q_digest = _q_lowercase_digest
				AND q_number=_q_number
			;
		END IF;
	END IF;
END
$$
