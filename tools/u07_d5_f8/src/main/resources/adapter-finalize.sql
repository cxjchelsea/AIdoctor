INSERT INTO f8_decision(id,canonical_id,contract,fingerprint,env,profile,tenant,consultation,question,parent_wait,wait_key,digest,verdict,winner_id,sampled_at,predicate_at,owner_versions,action_epoch)
SELECT :id,:canonical,:contract,:fingerprint,:env,:profile,:tenant,:consultation,:question,:wait,:wait_key,:digest,
 CASE
 WHEN a.decision_id IS NOT NULL AND a.digest=:digest THEN 'DUPLICATE'
 WHEN CLOCK_EXPR>=o.deadline OR o.state='EXPIRED' THEN 'EXPIRED'
 WHEN o.state<>'WAITING' THEN 'REJECTED'
 WHEN o.current_wait IS NULL OR o.current_wait<>:wait OR o.clinical_version<>:clinical_version OR q.state<>'ASKED' OR p.state<>'WAITING' THEN 'REJECTED'
 WHEN w.id IS NOT NULL AND a.decision_id IS NULL THEN 'DEFER'
 WHEN a.decision_id IS NOT NULL AND a.digest<>:digest THEN 'REJECTED'
 WHEN i.state='DELIVERED' AND (c.wait_key IS NULL OR c.canonical_id IS NULL AND c.decision_id IS NULL AND c.generation=0) THEN 'ACCEPTED'
 ELSE 'DEFER' END,
 w.id,CLOCK_EXPR,CLOCK_EXPR,:owner_versions,act.epoch
FROM f8_owner o JOIN f8_owner q ON q.ref=:question_ref JOIN f8_owner p ON p.ref=:pending_ref JOIN f8_owner i ON i.ref=:issuance_ref
JOIN f8_action act ON act.scope_key=:scope_key
LEFT JOIN f8_claim c ON c.wait_key=:wait_key
LEFT JOIN f8_decision w ON w.id=c.decision_id AND w.canonical_id=c.canonical_id AND w.wait_key=c.wait_key AND w.verdict='ACCEPTED' AND c.generation=1
LEFT JOIN f8_applied a ON a.decision_id=w.id AND a.wait_key=w.wait_key AND a.canonical_id=w.canonical_id AND a.digest=w.digest AND a.generation=c.generation AND a.issuer=:issuer AND a.policy=:policy AND a.manifest=:manifest
WHERE o.ref=:consultation_ref AND o.version=:consultation_version AND q.version=:question_version AND p.version=:pending_version AND i.version=:issuance_version
 AND act.epoch=:epoch AND act.finalize_allowed=1
 AND (c.wait_key IS NULL OR c.env=:env AND c.profile=:profile AND c.tenant=:tenant AND c.consultation=:consultation AND c.question=:question AND c.parent_wait=:wait)
 AND (c.wait_key IS NULL OR c.canonical_id IS NULL AND c.decision_id IS NULL AND c.generation=0 OR w.id IS NOT NULL)
 AND (NOT EXISTS(SELECT 1 FROM f8_applied ax WHERE ax.decision_id=w.id) OR a.decision_id IS NOT NULL)
 AND NOT EXISTS(SELECT 1 FROM f8_decision exact_d WHERE exact_d.canonical_id=:canonical AND exact_d.contract=:contract)
 AND (a.decision_id IS NOT NULL AND a.digest=:digest OR CLOCK_EXPR>=o.deadline OR o.state<>'WAITING'
 OR o.current_wait IS NULL OR o.current_wait<>:wait OR o.clinical_version<>:clinical_version OR q.state<>'ASKED' OR p.state<>'WAITING'
 OR a.decision_id IS NOT NULL OR i.state='DELIVERED' AND (c.wait_key IS NULL OR c.canonical_id IS NULL AND c.decision_id IS NULL AND c.generation=0))
 AND NOT(a.decision_id IS NULL AND w.id IS NOT NULL AND CLOCK_EXPR<o.deadline AND o.state='WAITING'
 AND o.current_wait IS NOT NULL AND o.current_wait=:wait AND o.clinical_version=:clinical_version AND q.state='ASKED' AND p.state='WAITING')
