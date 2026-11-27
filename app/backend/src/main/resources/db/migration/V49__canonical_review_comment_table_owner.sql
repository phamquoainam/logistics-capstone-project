-- Owner-approved BIA-OWNER-01; forward correction only, no historical rewrite.
DO $$
DECLARE canonical NAME; actual NAME; can_login BOOLEAN;
BEGIN
 SELECT r.rolname,r.rolcanlogin INTO canonical,can_login
 FROM pg_namespace n JOIN pg_roles r ON r.oid=n.nspowner WHERE n.nspname='merchant';
 SELECT pg_get_userbyid(c.relowner) INTO actual FROM pg_class c
 JOIN pg_namespace n ON n.oid=c.relnamespace
 WHERE n.nspname='merchant' AND c.relname='application_review_comment' AND c.relkind='r';
 IF canonical IS NULL OR can_login OR actual IS NULL OR current_user<>canonical
    OR actual NOT IN (canonical,session_user) THEN
  RAISE EXCEPTION 'REVIEW_COMMENT_OWNER_SOURCE_DRIFT' USING ERRCODE='42501';
 END IF;
 IF actual<>canonical THEN
  EXECUTE format('SET LOCAL ROLE %I',session_user);
  EXECUTE format('ALTER TABLE merchant.application_review_comment OWNER TO %I',canonical);
  EXECUTE format('SET LOCAL ROLE %I',canonical);
 END IF;
END $$;
