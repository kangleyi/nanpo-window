UPDATE site_profile
SET recommended_season = '全年',
    updated_at = CURRENT_TIMESTAMP
WHERE recommended_season <> '全年' OR recommended_season IS NULL;
