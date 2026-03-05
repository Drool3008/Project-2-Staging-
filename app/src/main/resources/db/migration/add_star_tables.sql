-- Star / favourite feature – join tables
-- Run once against your schema before deploying the updated WAR.

CREATE TABLE IF NOT EXISTS user_starred_weblog (
    user_id   VARCHAR(48) NOT NULL,
    weblog_id VARCHAR(48) NOT NULL,
    PRIMARY KEY (user_id, weblog_id)
);

CREATE TABLE IF NOT EXISTS user_starred_entry (
    user_id  VARCHAR(48) NOT NULL,
    entry_id VARCHAR(48) NOT NULL,
    PRIMARY KEY (user_id, entry_id)
);
