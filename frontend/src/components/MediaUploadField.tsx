import { ChangeEvent, useEffect, useState } from 'react';
import { uploadMediaFile } from '../services/mediaApi';

type MediaUploadFieldProps = {
  name: string;
  label: string;
  mediaType: 'IMAGE' | 'VIDEO';
  initialUrl?: string;
  required?: boolean;
  onBusyChange?: (busy: boolean) => void;
};

export function MediaUploadField({
  name,
  label,
  mediaType,
  initialUrl = '',
  required = false,
  onBusyChange,
}: MediaUploadFieldProps) {
  const [url, setUrl] = useState(initialUrl);
  const [previewUrl, setPreviewUrl] = useState(initialUrl);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setUrl(initialUrl);
    setPreviewUrl(initialUrl);
    setError('');
  }, [initialUrl]);

  useEffect(() => () => {
    if (previewUrl.startsWith('blob:')) URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0];
    event.currentTarget.value = '';
    if (!file) return;
    const valid = mediaType === 'IMAGE' ? file.type.startsWith('image/') : file.type.startsWith('video/');
    if (!valid) {
      setError(mediaType === 'IMAGE' ? '请选择图片文件' : '请选择视频文件');
      return;
    }
    const localPreview = URL.createObjectURL(file);
    setPreviewUrl(localPreview);
    setUploading(true);
    setError('');
    onBusyChange?.(true);
    try {
      const uploaded = await uploadMediaFile(file, mediaType);
      setUrl(uploaded.contentUrl);
    } catch (reason) {
      setUrl(initialUrl);
      setPreviewUrl(initialUrl);
      setError(reason instanceof Error ? reason.message : '文件上传失败');
    } finally {
      setUploading(false);
      onBusyChange?.(false);
    }
  };

  return <div className="listing-media-field">
    <div className="listing-media-label"><span>{label}{required && ' *'}</span>{url && !required && <button type="button" onClick={() => { setUrl(''); setPreviewUrl(''); }}>清除</button>}</div>
    <input type="hidden" name={name} value={url}/>
    <div className={`listing-media-preview ${previewUrl ? 'has-media' : ''}`}>
      {previewUrl
        ? mediaType === 'IMAGE'
          ? <img src={previewUrl} alt={`${label}预览`}/>
          : <video src={previewUrl} controls muted preload="metadata"/>
        : <span>{mediaType === 'IMAGE' ? '暂无图片' : '暂未上传视频'}</span>}
    </div>
    <label className={`listing-media-picker ${uploading ? 'uploading' : ''}`}>
      <input type="file" disabled={uploading} accept={mediaType === 'IMAGE' ? 'image/jpeg,image/png,image/webp' : 'video/mp4,video/webm'} onChange={upload}/>
      <span>{uploading ? '正在上传并校验…' : `${url ? '重新上传' : '选择并上传'}${mediaType === 'IMAGE' ? '图片' : '视频'}`}</span>
      <small>{mediaType === 'IMAGE' ? 'JPG、PNG、WebP，最大 10MB' : 'MP4、WebM，最大 100MB'}</small>
    </label>
    {url && <small className="listing-media-ready">✓ 已上传，保存后写入数据库</small>}
    {error && <small className="listing-media-error" role="alert">{error}</small>}
  </div>;
}
