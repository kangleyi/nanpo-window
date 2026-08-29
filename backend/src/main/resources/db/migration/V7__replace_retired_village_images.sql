UPDATE homestay
SET cover_url = CASE
    WHEN cover_url = '/images/village-detail.jpg' THEN '/images/nanpo-courtyard.png'
    WHEN cover_url = '/images/village-pond.jpg' THEN '/images/nanpo-architecture.png'
    ELSE cover_url
END
WHERE cover_url IN ('/images/village-detail.jpg', '/images/village-pond.jpg');

UPDATE experience
SET cover_url = '/images/nanpo-sign.png'
WHERE cover_url = '/images/village-detail.jpg';
