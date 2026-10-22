CREATE TABLE local_title_ad_group (
  id                              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  local_title_id                  BIGINT NOT NULL,
  ad_group_id                     BIGINT NOT NULL,

  CONSTRAINT fk_local_title_ad_group_global_title_id FOREIGN KEY (local_title_id) REFERENCES local_title(id) ON DELETE CASCADE,
  CONSTRAINT fk_local_title_ad_group_ad_group FOREIGN KEY (ad_group_id) REFERENCES ad_group(id) ON DELETE CASCADE
);
