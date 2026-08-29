INSERT INTO role (code, name) VALUES
    ('CUSTOMER', '客户'),
    ('FARMER', '农户'),
    ('CONTENT_OPERATOR', '内容运营'),
    ('REVIEWER', '审核员'),
    ('ORDER_OPERATOR', '订单运营'),
    ('SUPER_ADMIN', '超级管理员');

INSERT INTO user_account (phone, display_name, status) VALUES
    ('13800000001', '梁有福', 'ACTIVE'),
    ('13800000002', '村庄管理员', 'ACTIVE');

INSERT INTO user_role (user_id, role_code)
SELECT id, 'FARMER' FROM user_account WHERE phone = '13800000001';
INSERT INTO user_role (user_id, role_code)
SELECT id, 'SUPER_ADMIN' FROM user_account WHERE phone = '13800000002';

INSERT INTO site_profile (
    name, province, city, county, address, summary, map_keyword,
    recommended_season, status, published_at
) VALUES (
    '大南坡村', '河南省', '焦作市', '修武县',
    '河南省焦作市修武县西村乡大南坡村',
    '太行山南麓的乡村文化与农事生活窗口。',
    '大南坡艺术中心', '4—9 月', 'PUBLISHED', CURRENT_TIMESTAMP(6)
);

INSERT INTO contact_channel (
    site_id, scene, contact_name, phone, business_hours, status
)
SELECT id, 'VISITOR_SERVICE', '大南坡访客服务', '0391-0000000', '09:00—17:30', 'PUBLISHED'
FROM site_profile WHERE name = '大南坡村';

INSERT INTO travel_route (
    route_kind, title, duration_text, note, steps_json, source_name,
    verified_at, sort_order, status, published_at
) VALUES
    ('drive', '从郑州自驾出发', '约 1.5 小时', '006 县道穿村而过，建议导航“大南坡艺术中心”。', '["郑云高速 S87","云台山站下高速","焦辉路 S306","青云大道 X006","大南坡村"]', '焦作文旅公开信息', CURRENT_TIMESTAMP(6), 10, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('rail', '高铁到修武西站', '郑州出发约 30 分钟', '公交班次可能随季节调整，请在出发前向车站或村庄服务点确认。', '["郑州 / 郑州东","修武西站","修武至西村公交","大南坡站"]', '焦作文旅公开信息', CURRENT_TIMESTAMP(6), 20, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('bus', '从焦作市区乘公交', '约 50—70 分钟', '线路和站点可能临时调整，以当日公交公告为准。', '["焦作站南广场","37 路公交","山阳建国站","换乘 29 路","大南坡站"]', '焦作文旅公开信息', CURRENT_TIMESTAMP(6), 30, 'PUBLISHED', CURRENT_TIMESTAMP(6));

INSERT INTO attraction (
    name, category, distance_km, drive_minutes, summary, cover_url,
    map_url, highlights_json, sort_order, status, published_at
) VALUES
    ('青龙峡 · 峰林峡', '峡谷 · 湖泊', 25.00, 50, '峡谷、瀑溪与高峡平湖，适合清凉徒步和山水摄影。', '/images/spots/qinglong.jpg', 'https://uri.amap.com/search?keyword=青龙峡景区峰林峡&city=焦作&callnative=0', '["峡谷瀑溪","翡翠湖色","山地体验"]', 10, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('焦作城市漫游', '古街 · 夜游', 30.00, 55, '恩州驿、南水北调天河公园与焦作夜市，适合轻松逛吃。', '/images/spots/jiaozuo-city.jpg', 'https://uri.amap.com/search?keyword=恩州驿&city=焦作&callnative=0', '["恩州驿","天河公园","夜市烟火"]', 20, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('云台山', '峡谷 · 飞瀑', 40.00, 70, '红石峡、潭瀑峡与茱萸峰，第一次来焦作的经典山水选择。', '/images/spots/yuntai.jpg', 'https://uri.amap.com/search?keyword=云台山景区&city=焦作&callnative=0', '["红石峡","潭瀑峡","茱萸峰"]', 30, 'PUBLISHED', CURRENT_TIMESTAMP(6));

INSERT INTO travel_plan (
    slug, name, duration_text, suitable_for, distance_text, summary,
    stops_json, tips_json, sort_order, status, published_at
) VALUES
    ('canyon', '南坡慢游 + 太行双峡', '0.5 DAY', '家庭 / 摄影 / 避暑', '单程约 20—30 km', '上午在村里慢慢走，午后去青龙峡或峰林峡二选一。', '[{"time":"09:00","title":"大南坡村","detail":"艺术中心、乡村书店与老村散步"},{"time":"13:30","title":"青龙峡 / 峰林峡","detail":"根据开放情况与体力二选一"}]', '["山区弯道较多，建议白天行车","出发前确认景区开放安排"]', 10, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('yuntai', '云台山峡谷一日', '1 DAY', '初访 / 山水 / 徒步', '单程约 35—45 km', '从大南坡出发，把红石峡与潭瀑峡安排在同一天。', '[{"time":"07:30","title":"大南坡出发","detail":"早餐后自驾前往云台山"},{"time":"09:00","title":"红石峡","detail":"峡谷步道较集中，建议错峰进入"}]', '["景区面积大，不建议一天塞满所有点位","观光车以当日安排为准"]', 20, 'PUBLISHED', CURRENT_TIMESTAMP(6));

INSERT INTO homestay (
    name, lodging_type, summary, capacity_text, price_text, cover_url,
    consultation_phone, sort_order, status, published_at
) VALUES
    ('牛大爷的院子', '乡土院落', '老院落、核桃树与山里清晨，适合一家人慢住两晚。', '2—4 人', '价格待确认', '/images/homestay.jpg', '0391-0000000', 10, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('南坡山居 · 一号院', '整院包住', '灰砖院落保留北方村居尺度，步行可达艺术中心。', '4—6 人', '价格待确认', '/images/village-detail.jpg', '0391-0000000', 20, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('松风小院', '双床客房', '面向太行山南麓，适合周末、研学与小型团队。', '2 人', '价格待确认', '/images/village-pond.jpg', '0391-0000000', 30, 'PUBLISHED', CURRENT_TIMESTAMP(6));

INSERT INTO experience (
    name, category, season_text, duration_text, summary, price_text,
    cover_url, video_url, booking_notes, sort_order, status, published_at
) VALUES
    ('核桃采收体验', '农事采摘', '8—9 月', '约 2 小时', '跟着农户认树、采收、分选，把劳动过程变成一堂自然课。', '¥ 68 / 人起', '/images/walnut-yard.jpg', '/videos/nanpo-experience.mp4', '至少提前一天咨询', 10, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('山花椒采摘', '季节限定', '7—8 月', '约 1.5 小时', '学习辨认成熟花椒、体验手工采摘，并了解晾晒过程。', '¥ 48 / 人起', '/images/products.jpg', NULL, '雨天可能取消', 20, 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    ('村庄美学导览', '文化讲解', '全年可约', '约 2 小时', '从老礼堂到艺术中心，听村庄空间更新与乡土生活的故事。', '价格待确认', '/images/village-detail.jpg', '/videos/nanpo-experience.mp4', '团队请提前预约', 30, 'PUBLISHED', CURRENT_TIMESTAMP(6));

INSERT INTO farmer_profile (
    user_id, farmer_code, name, village_group, introduction,
    certification_status, certified_at, status
)
SELECT id, 'NP-F-001', '梁有福', '大南坡村三组', '多年种植核桃与山花椒，持续记录地块与采收过程。', 'APPROVED', CURRENT_TIMESTAMP(6), 'ACTIVE'
FROM user_account WHERE phone = '13800000001';

INSERT INTO land_plot (
    farmer_id, plot_code, location_text, area_text, main_crop, cover_url, status
)
SELECT id, 'NP-03', '大南坡村核桃坡 3 号地', '约 3.5 亩', '核桃', '/images/walnut-yard.jpg', 'PUBLISHED'
FROM farmer_profile WHERE farmer_code = 'NP-F-001';

INSERT INTO product (
    farmer_id, land_plot_id, name, category, season_text, summary,
    cover_url, sort_order, status, published_at
)
SELECT f.id, p.id, '太行山核桃', '坚果', '秋季新收', '来自核桃坡 3 号地，根据已审核生产记录展示整地、生长与采收过程。', '/images/products.jpg', 10, 'PUBLISHED', CURRENT_TIMESTAMP(6)
FROM farmer_profile f JOIN land_plot p ON p.farmer_id = f.id
WHERE f.farmer_code = 'NP-F-001' AND p.plot_code = 'NP-03';

INSERT INTO product (
    farmer_id, land_plot_id, name, category, season_text, summary,
    cover_url, sort_order, status, published_at
)
SELECT f.id, p.id, '南坡山花椒', '调味品', '农户晾制', '当季采收后由农户晾制，实际规格与库存以下单时为准。', '/images/products.jpg', 20, 'PUBLISHED', CURRENT_TIMESTAMP(6)
FROM farmer_profile f JOIN land_plot p ON p.farmer_id = f.id
WHERE f.farmer_code = 'NP-F-001' AND p.plot_code = 'NP-03';

INSERT INTO product_sku (product_id, sku_code, specification, unit_price, stock_note)
SELECT id, 'NP-WALNUT-500', '500g / 袋', 29.90, '当周库存以后台确认为准' FROM product WHERE name = '太行山核桃';
INSERT INTO product_sku (product_id, sku_code, specification, unit_price, stock_note)
SELECT id, 'NP-PEPPER-100', '100g / 袋', 19.90, '季节限量' FROM product WHERE name = '南坡山花椒';

INSERT INTO farm_record (
    farmer_id, product_id, land_plot_id, stage, occurred_at,
    original_text, confirmed_text, truth_confirmed, status, reviewed_at, published_at
)
SELECT p.farmer_id, p.id, p.land_plot_id, 'GROWING', TIMESTAMP '2026-06-18 08:00:00',
       '这几天人工除了草，核桃叶子和果子长势稳定。',
       '6 月 18 日，核桃坡 3 号地完成人工除草，现场记录显示叶片和果实生长稳定。',
       TRUE, 'PUBLISHED', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM product p WHERE p.name = '太行山核桃';

INSERT INTO farm_record (
    farmer_id, product_id, land_plot_id, stage, occurred_at,
    original_text, confirmed_text, truth_confirmed, status, reviewed_at, published_at
)
SELECT p.farmer_id, p.id, p.land_plot_id, 'HARVEST', TIMESTAMP '2026-08-22 06:00:00',
       '今早六点和家里人一起摘的，挑出外壳完整的先晾晒。',
       '8 月 22 日清晨，梁有福与家人完成首批核桃采收，并对外壳完整的核桃进行初选和晾晒。',
       TRUE, 'PUBLISHED', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM product p WHERE p.name = '太行山核桃';

