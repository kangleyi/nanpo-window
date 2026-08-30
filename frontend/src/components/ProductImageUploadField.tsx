import { ChangeEvent, useEffect, useMemo, useState } from 'react';
import { uploadMediaFile } from '../services/mediaApi';

type ProductImageUploadFieldProps = {
  name: string;
  initialUrls?: string[];
  maxImages?: number;
  onBusyChange?: (busy: boolean) => void;
  onCoverChange?: (cover: ProductCoverSource | null) => void;
};

export type ProductCoverSource = {
  url: string;
  file?: File;
};

export function ProductImageUploadField({
  name,
  initialUrls = [],
  maxImages = 10,
  onBusyChange,
  onCoverChange,
}: ProductImageUploadFieldProps) {
  const initialKey = useMemo(() => initialUrls.join('\n'), [initialUrls]);
  const [images, setImages] = useState<ProductCoverSource[]>(() => initialUrls.filter(Boolean).map((url) => ({ url })));
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const cover = images[0] || null;

  useEffect(() => {
    setImages(initialKey ? initialKey.split('\n').map((url) => ({ url })) : []);
    setError('');
  }, [initialKey]);

  useEffect(() => {
    onCoverChange?.(cover);
  }, [cover, onCoverChange]);

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.currentTarget.files || []);
    event.currentTarget.value = '';
    if (!files.length) return;
    if (files.some((file) => !file.type.startsWith('image/'))) {
      setError('请选择 JPG、PNG 或 WebP 图片');
      return;
    }
    if (images.length + files.length > maxImages) {
      setError(`每个商品最多上传 ${maxImages} 张图片，还可选择 ${maxImages - images.length} 张`);
      return;
    }

    setUploading(true);
    setError('');
    onBusyChange?.(true);
    try {
      const uploaded = await Promise.all(files.map((file) => uploadMediaFile(file, 'IMAGE')));
      setImages((current) => [
        ...current,
        ...uploaded.map((item, index) => ({ url: item.contentUrl, file: files[index] })),
      ]);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '图片上传失败');
    } finally {
      setUploading(false);
      onBusyChange?.(false);
    }
  };

  const move = (index: number, offset: number) => {
    setImages((current) => {
      const next = [...current];
      const target = index + offset;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  };

  return <div className="product-images-field">
    <div className="listing-media-label"><span>农产品图片 * <small>（首张为封面及 AI 识别图）</small></span><b>{images.length} / {maxImages}</b></div>
    {images.map((image) => <input key={image.url} type="hidden" name={name} value={image.url}/>)}
    {images.length ? <div className="product-image-grid">
      {images.map((image, index) => <article key={image.url}>
        <img src={image.url} alt={`农产品图片 ${index + 1} 预览`}/>
        <span>{index === 0 ? '封面' : `第 ${index + 1} 张`}</span>
        <div>
          <button type="button" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`将第${index + 1}张图片前移`}>←</button>
          <button type="button" disabled={index === images.length - 1} onClick={() => move(index, 1)} aria-label={`将第${index + 1}张图片后移`}>→</button>
          <button type="button" onClick={() => setImages((current) => current.filter((_, itemIndex) => itemIndex !== index))} aria-label={`删除第${index + 1}张图片`}>删除</button>
        </div>
      </article>)}
    </div> : <div className="product-images-empty">暂无图片，请至少上传一张</div>}
    <label className={`listing-media-picker ${uploading ? 'uploading' : ''}`}>
      <input type="file" multiple disabled={uploading || images.length >= maxImages} accept="image/jpeg,image/png,image/webp" onChange={upload}/>
      <span>{uploading ? '正在上传并校验…' : images.length ? '继续添加图片' : '选择并上传图片'}</span>
      <small>可多选，JPG、PNG、WebP，单张最大 10MB</small>
    </label>
    {images.length > 0 && <small className="listing-media-ready">✓ 已上传 {images.length} 张；AI 仅识别当前首张封面</small>}
    {error && <small className="listing-media-error" role="alert">{error}</small>}
  </div>;
}
