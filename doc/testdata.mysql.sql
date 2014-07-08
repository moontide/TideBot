INSERT INTO dic_digests (content_digest, content, content_lowercase) VALUES
	(sha1('q1'), 'Q1', 'q1')
	,(sha1('a1'), 'A1', 'a1')
	,(sha1('q2'), 'Q2', 'q2')
	,(sha1('a2'), 'A2', 'a2')
;

INSERT INTO dics (q_digest, a_digest, added_by, added_time) VALUES
	(sha1('q1'), sha1('a1'), 'test', NOW())
	,(sha1('q2'), sha1('a2'), 'test', NOW())
	,(sha1('q1'), sha1('a2'), 'test', NOW())
;
