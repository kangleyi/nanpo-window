import { ChangeEvent, useEffect, useMemo, useState } from 'react';
import { uploadMediaFile } from '../services/mediaApi';

type ProductImageUploadFieldProps = {
  name: string;
  initialUrls?: string[];
  maxImages?: number;
  onBusyChange?: (busy: boolean) => void;
};

export function ProductImageUploadField({
  name,
  initialUrls = [],
  maxImages = 10,
  onBusyChange,
}: ProductImageUploadFieldProps) {
  const initialKey = useMemo(() => initialUrls.join('\n'), [initialUrls]);
  const [urls, setUrls] = useState(() => initialUrls.filter(Boolean));
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setUrls(initialKey ? initialKey.split('\n') : []);
    setError('');
  }, [initialKey]);

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.currentTarget.files || []);
    event.currentTarget.value = '';
    if (!files.length) return;
    if (files.some((file) => !file.type.startsWith('image/'))) {
      setError('请选择 JPG、PNG 或 WebP 图片');
      return;
    }
    if (urls.length + files.length > maxImages) {
      setError(`每个商品最多上传 ${maxImages} 张图片，还可选择 ${maxImages - urls.length} 张`);
      return;
    }

    setUploading(true);
    setError('');
    onBusyChange?.(true);
    try {
      const uploaded = await Promise.all(files.map((file) => uploadMediaFile(file, 'IMAGE')));
      setUrls((current) => [...current, ...uploaded.map((item) => item.contentUrl)]);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '图片上传失败');
    } finally {
      setUploading(false);
      onBusyChange?.(false);
    }
  };

  const move = (index: number, offset: number) => {
    setUrls((current) => {
      const next = [...current];
      const target = index + offset;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  };

  return <div className="product-images-field">
    <div className="listing-media-label"><span>农产品图片 * <small>（首张为封面）</small></span><b>{urls.length} / {maxImages}</b></div>
    {urls.map((url) => <input key={url} type="hidden" name={name} value={url}/>)}
    {urls.length ? <div className="product-image-grid">
      {urls.map((url, index) => <article key={url}>
        <img src={url} alt={`农产品图片 ${index + 1} 预览`}/>
        <span>{index === 0 ? '封面' : `第 ${index + 1} 张`}</span>
        <div>
          <button type="button" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`将第${index + 1}张图片前移`}>←</button>
          <button type="button" disabled={index === urls.length - 1} onClick={() => move(index, 1)} aria-label={`将第${index + 1}张图片后移`}>→</button>
          <button type="button" onClick={() => setUrls((current) => current.filter((_, itemIndex) => itemIndex !== index))} aria-label={`删除第${index + 1}张图片`}>删除</button>
        </div>
      </article>)}
    </div> : <div className="product-images-empty">暂无图片，请至少上传一张</div>}
    <label className={`listing-media-picker ${uploading ? 'uploading' : ''}`}>
      <input type="file" multiple disabled={uploading || urls.length >= maxImages} accept="image/jpeg,image/png,image/webp" onChange={upload}/>
      <span>{uploading ? '正在上传并校验…' : urls.length ? '继续添加图片' : '选择并上传图片'}</span>
      <small>可多选，JPG、PNG、WebP，单张最大 10MB</small>
    </label>
    {urls.length > 0 && <small className="listing-media-ready">✓ 已上传 {urls.length} 张，可拖动前后按钮调整展示顺序</small>}
    {error && <small className="listing-media-error" role="alert">{error}</small>}
  </div>;
}
