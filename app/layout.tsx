import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: '南坡之窗｜太行山下，一座会生长的村庄',
  description: '河南焦作修武县大南坡村的出行指南、山居民宿、乡野好物与乡村故事。',
  openGraph: {
    title: '南坡之窗｜太行山下，一座会生长的村庄',
    description: '一站了解河南焦作修武县大南坡村的路线、山居、农品与乡村故事。',
    images: [{ url: '/og.png', width: 1733, height: 908, alt: '南坡之窗——太行山下，一座会生长的村庄' }],
  },
  twitter: {
    card: 'summary_large_image',
    title: '南坡之窗｜太行山下，一座会生长的村庄',
    description: '一站了解河南焦作修武县大南坡村的路线、山居、农品与乡村故事。',
    images: ['/og.png'],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
