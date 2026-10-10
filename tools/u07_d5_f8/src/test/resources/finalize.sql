INSERT INTO decision (id,answer_id,scope_id,wait_id,digest,verdict,sampled_at,predicate_at,owner_version,winner_id)
SELECT ?,?,o.scope_id,o.historical_wait,o.digest,
 CASE
 WHEN a.decision_id IS NOT NULL AND a.digest=o.digest THEN 'DUPLICATE'
 WHEN CLOCK_EXPR >= o.deadline OR o.terminal='EXPIRED' THEN 'EXPIRED'
 WHEN o.terminal IS NOT NULL THEN 'REJECTED'
 WHEN o.current_wait IS NULL OR o.current_wait<>o.historical_wait THEN 'REJECTED'
 WHEN w.id IS NOT NULL AND a.decision_id IS NULL THEN 'DEFER'
 WHEN a.decision_id IS NOT NULL AND a.digest<>o.digest THEN 'REJECTED'
 WHEN o.delivered=1 AND (c.wait_id IS NULL OR (c.answer_id IS NULL AND c.decision_id IS NULL AND c.generation=0)) THEN 'ACCEPTED'
 ELSE 'DEFER' END,
 CLOCK_EXPR,CLOCK_EXPR,o.version,w.id
FROM owner_fact o JOIN guard g ON g.id=o.id
LEFT JOIN wait_claim c ON c.wait_id=o.historical_wait
LEFT JOIN decision w ON w.id=c.decision_id AND w.answer_id=c.answer_id AND w.scope_id=c.scope_id
 AND w.wait_id=c.wait_id AND w.verdict='ACCEPTED' AND c.generation=1
LEFT JOIN applied_evidence a ON a.decision_id=w.id AND a.scope_id=w.scope_id
 AND a.wait_id=w.wait_id AND a.digest=w.digest AND a.generation=c.generation AND a.issuer='synthetic-independent-issuer'
WHERE o.id=? AND o.scope_id=? AND o.version=? AND g.version=? AND o.authority_complete=1
 AND (c.wait_id IS NULL OR c.scope_id=o.scope_id)
 AND NOT EXISTS (SELECT 1 FROM decision exact_d WHERE exact_d.answer_id=?)
 AND (c.wait_id IS NULL OR (c.answer_id IS NULL AND c.decision_id IS NULL AND c.generation=0) OR w.id IS NOT NULL)
 AND (NOT EXISTS (SELECT 1 FROM applied_evidence ax WHERE ax.decision_id=w.id) OR a.decision_id IS NOT NULL)
 AND (
 a.decision_id IS NOT NULL AND a.digest=o.digest
 OR CLOCK_EXPR >= o.deadline OR o.terminal IS NOT NULL
 OR o.current_wait IS NULL OR o.current_wait<>o.historical_wait
 OR a.decision_id IS NOT NULL
 OR o.delivered=1 AND (c.wait_id IS NULL OR (c.answer_id IS NULL AND c.decision_id IS NULL AND c.generation=0))
 )
 AND NOT (a.decision_id IS NULL AND w.id IS NOT NULL AND CLOCK_EXPR<o.deadline
 AND o.terminal IS NULL AND o.current_wait IS NOT NULL AND o.current_wait=o.historical_wait)
