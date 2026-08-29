UPDATE attraction
SET name = '青龙峡',
    category = '峡谷 · 瀑溪',
    distance_km = 16.00,
    drive_minutes = 35,
    summary = '峡谷幽深、潭瀑相连，适合避暑徒步，也可把沿途山路当作太行风景的一部分。',
    cover_url = '/images/spots/qinglong.jpg',
    map_url = 'https://uri.amap.com/search?keyword=青龙峡景区&city=焦作&callnative=0',
    highlights_json = '["峡谷瀑溪","爱情一号公路","清凉徒步"]',
    sort_order = 30,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE name = '青龙峡 · 峰林峡';

UPDATE attraction
SET name = '恩州驿',
    category = '古街 · 夜游',
    distance_km = 32.00,
    drive_minutes = 55,
    summary = '近千米古风街区串联老建筑、非遗手作与地方小吃，傍晚亮灯后更有烟火气。',
    cover_url = '/images/spots/enzhou.jpg',
    map_url = 'https://uri.amap.com/search?keyword=恩州驿&city=焦作&callnative=0',
    highlights_json = '["古风街区","非遗市集","夜景演艺"]',
    sort_order = 70,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE name = '焦作城市漫游';

UPDATE attraction
SET category = '峡谷 · 飞瀑',
    distance_km = 38.00,
    drive_minutes = 65,
    summary = '红石峡、潭瀑峡与茱萸峰构成经典山水组合，适合为第一次到焦作留出完整一天。',
    cover_url = '/images/spots/yuntai-new.jpg',
    map_url = 'https://uri.amap.com/search?keyword=云台山景区&city=焦作&callnative=0',
    highlights_json = '["红石峡","潭瀑峡","茱萸峰"]',
    sort_order = 80,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE name = '云台山';

INSERT INTO attraction (
    name, category, distance_km, drive_minutes, summary, cover_url,
    map_url, highlights_json, sort_order, status, published_at
)
SELECT '大南坡村', '乡村 · 艺术', 0.00, 0,
       '从老大队部到乡村书店，在灰砖院落、古树与公共文化空间之间读懂村庄更新。',
       '/images/nanpo-architecture.png', 'https://surl.amap.com/kfIn9ZYM8vC',
       '["艺术中心","方所乡村文化","碧山工销社"]', 10, 'PUBLISHED', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE name = '大南坡村');

INSERT INTO attraction (
    name, category, distance_km, drive_minutes, summary, cover_url,
    map_url, highlights_json, sort_order, status, published_at
)
SELECT '圆融寺', '古寺 · 石刻', 12.00, 25,
       '太行山前的千年古刹，院落依山展开，适合静心漫游并感受山寺建筑。',
       '/images/spots/yuanrong-new.jpg', 'https://uri.amap.com/search?keyword=圆融无碍禅寺&city=焦作&callnative=0',
       '["古寺院落","石刻碑塔","山门远眺"]', 20, 'PUBLISHED', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE name IN ('圆融寺', '圆融无碍禅寺'));

INSERT INTO attraction (
    name, category, distance_km, drive_minutes, summary, cover_url,
    map_url, highlights_json, sort_order, status, published_at
)
SELECT '峰林峡', '天池 · 峡谷', 22.00, 45,
       '峰林与碧水相拥，高峡平湖色彩清透，适合乘船观景与亲子山水体验。',
       '/images/spots/fenglin.jpg', 'https://uri.amap.com/search?keyword=峰林峡&city=焦作&callnative=0',
       '["云台天池","高峡平湖","山水游乐"]', 40, 'PUBLISHED', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE name = '峰林峡');

INSERT INTO attraction (
    name, category, distance_km, drive_minutes, summary, cover_url,
    map_url, highlights_json, sort_order, status, published_at
)
SELECT '当阳峪绞胎瓷博物馆', '非遗 · 博物馆', 24.00, 45,
       '走近“表里如一”的绞胎纹理，看多色瓷泥如何经过拉坯、修坯与烧制成为独一无二的器物。',
       '/images/spots/dangyangyu.jpg', 'https://uri.amap.com/search?keyword=当阳峪绞胎瓷博物馆&city=焦作&callnative=0',
       '["国家级非遗","绞胎瓷器","工艺体验"]', 50, 'PUBLISHED', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE name = '当阳峪绞胎瓷博物馆');

INSERT INTO attraction (
    name, category, distance_km, drive_minutes, summary, cover_url,
    map_url, highlights_json, sort_order, status, published_at
)
SELECT '圆通寺', '古寺 · 山麓', 28.00, 50,
       '巡返村旁的山麓寺院，殿宇沿地势铺开，观音像与太行山背景构成醒目的远观点位。',
       '/images/spots/yuantong.jpg', 'https://uri.amap.com/search?keyword=巡返圆通寺&city=焦作&callnative=0',
       '["巡返古寺","观音像","太行山麓"]', 60, 'PUBLISHED', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE name = '圆通寺');

INSERT INTO attraction (
    name, category, distance_km, drive_minutes, summary, cover_url,
    map_url, highlights_json, sort_order, status, published_at
)
SELECT '宝泉', '峡谷 · 瀑布', 58.00, 85,
       '高峡、碧水与成群瀑布是这里的主角，适合安排一日亲水游并预留充足步行时间。',
       '/images/spots/baoquan.jpg', 'https://uri.amap.com/search?keyword=河南宝泉旅游区&city=新乡&callnative=0',
       '["翡翠湖色","峡谷瀑布","亲水步道"]', 90, 'PUBLISHED', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE name = '宝泉');

UPDATE travel_plan
SET name = '南坡村落 + 青龙峡',
    duration_text = '1 DAY',
    suitable_for = '家庭 / 摄影 / 避暑',
    distance_text = '单程约 16 km',
    summary = '上午沿村内公共文化空间慢慢走，午后进入青龙峡，把乡村更新与太行山水放进同一天。',
    stops_json = '[{"time":"09:00","title":"大南坡村","detail":"艺术中心、乡村书店与老村散步"},{"time":"12:00","title":"村中午餐","detail":"提前向民宿或村庄服务点预约"},{"time":"13:30","title":"青龙峡","detail":"沿峡谷、潭瀑与山路轻徒步"},{"time":"17:30","title":"返回南坡","detail":"住进山居，留一晚看山间暮色"}]',
    tips_json = '["山区弯道较多，建议白天行车","景区开放安排可能调整，出发前确认"]',
    sort_order = 10,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'canyon';

UPDATE travel_plan
SET distance_text = '单程约 38 km',
    summary = '从大南坡出发，把红石峡与潭瀑峡安排在同一天；想登高可用茱萸峰替换一个峡谷。',
    stops_json = '[{"time":"07:30","title":"大南坡出发","detail":"早餐后自驾前往云台山游客中心"},{"time":"09:00","title":"红石峡","detail":"峡谷步道较集中，建议错峰进入"},{"time":"12:00","title":"岸上服务区","detail":"午餐、补水并确认下午交通"},{"time":"13:30","title":"潭瀑峡 / 茱萸峰","detail":"亲水轻徒步或登高观景二选一"},{"time":"18:30","title":"返回南坡","detail":"也可住岸上小镇，次日继续深度游"}]',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'yuntai';

INSERT INTO travel_plan (
    slug, name, duration_text, suitable_for, distance_text, summary,
    stops_json, tips_json, sort_order, status, published_at
)
SELECT 'culture', '怀川文化打卡线', '2 DAYS', '亲子 / 研学 / 非遗', '单程最远约 32 km',
       '第一天认识大南坡的乡村更新与当阳峪绞胎瓷，第二天串联圆融寺、圆通寺和恩州驿。',
       '[{"time":"D1 上午","title":"大南坡村","detail":"乡村更新、艺术空间与在地午餐"},{"time":"D1 下午","title":"当阳峪绞胎瓷博物馆","detail":"看绞胎纹理、窑火与非遗工艺"},{"time":"D1 晚间","title":"恩州驿","detail":"逛古街、非遗市集与夜间灯景"},{"time":"D2 上午","title":"圆融寺","detail":"沿山寺院落慢走，看碑塔与古建"},{"time":"D2 下午","title":"圆通寺","detail":"到巡返村山麓看寺院与太行远景"}]',
       '["两日线路建议自驾或包车","博物馆体验和团队讲解建议提前预约"]',
       30, 'PUBLISHED', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM travel_plan WHERE slug = 'culture');
