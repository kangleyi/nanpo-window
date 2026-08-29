ALTER TABLE site_profile ADD COLUMN goods_eyebrow VARCHAR(80) NOT NULL DEFAULT '山野好物';
ALTER TABLE site_profile ADD COLUMN goods_title VARCHAR(255) NOT NULL DEFAULT '每一份收成都有自己的时节。';
ALTER TABLE site_profile ADD COLUMN goods_description VARCHAR(1000) NOT NULL DEFAULT '山核桃、山花椒、小米与蜂蜜，是公开旅游资料中推荐的焦作山野物产。具体商品、价格和村民联系方式由后台上架。';
ALTER TABLE site_profile ADD COLUMN goods_season_label VARCHAR(50) NOT NULL DEFAULT '八月';
ALTER TABLE site_profile ADD COLUMN goods_season_note VARCHAR(255) NOT NULL DEFAULT '核桃与花椒陆续成熟';
ALTER TABLE site_profile ADD COLUMN goods_image_url VARCHAR(500) NOT NULL DEFAULT '/images/nanpo-workshop.png';
ALTER TABLE site_profile ADD COLUMN goods_image_caption VARCHAR(160) NOT NULL DEFAULT '工销社里的山野收成';
