const pageTimestamp = Date.now().toString();

export function withPageTimestamp(source?: string): string | undefined {
  if (!source || (!source.startsWith('/images/') && !source.startsWith('/videos/'))) {
    return source;
  }
  const separator = source.includes('?') ? '&' : '?';
  return `${source}${separator}_ts=${pageTimestamp}`;
}
