UPDATE travel_route
SET title = '高铁到修武西站后打车',
    duration_text = '高铁约 30 分钟，到站后打车进村',
    note = '到站后建议直接打车进村；山区返程车辆较少，可提前与司机约定返程时间。',
    steps_json = '["郑州 / 郑州东","修武西站","网约车或出租车","大南坡艺术中心"]'
WHERE route_kind = 'rail';

UPDATE travel_route
SET route_kind = 'taxi',
    title = '从焦作市区打车前往',
    duration_text = '约 50—70 分钟，以实时导航为准',
    note = '进村及返程车辆可能较少，建议提前预约车辆，并与司机确认返程安排。',
    steps_json = '["焦作市区 / 焦作站","网约车或出租车","青云大道 X006","大南坡艺术中心"]'
WHERE route_kind = 'bus';
