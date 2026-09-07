ALTER TABLE user_account ADD COLUMN external_subject VARCHAR(255) NULL;
CREATE UNIQUE INDEX uk_user_account_external_subject ON user_account(external_subject);
