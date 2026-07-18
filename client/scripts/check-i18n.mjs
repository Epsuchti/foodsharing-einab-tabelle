import { readdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';

const sourceRoot = 'src/app';
const languages = ['de', 'en', 'gws'];
const translationKeyPattern = /\bi18n\.t\(\s*['"]([^'"]+)['"]\s*\)/g;

async function sourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = await Promise.all(entries.map(async (entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      return sourceFiles(path);
    }
    return /\.(html|ts)$/.test(entry.name) ? [path] : [];
  }));
  return files.flat();
}

const keys = new Set();
for (const file of await sourceFiles(sourceRoot)) {
  const source = await readFile(file, 'utf8');
  for (const match of source.matchAll(translationKeyPattern)) {
    keys.add(match[1]);
  }
}

const missing = [];
for (const language of languages) {
  const translations = JSON.parse(await readFile(`public/i18n/${language}.json`, 'utf8'));
  for (const key of keys) {
    if (!(key in translations)) {
      missing.push(`${language}: ${key}`);
    }
  }
}

if (missing.length > 0) {
  throw new Error(`Missing translations:\n${missing.join('\n')}`);
}

console.log(`Verified ${keys.size} translation keys in ${languages.length} languages.`);
