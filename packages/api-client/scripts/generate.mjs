import { generateFresh, writeGenerated, digest } from './generation-lib.mjs';

const files = await generateFresh();
writeGenerated(files);
console.log(`Generated ${files.size} files; SHA256 ${digest(files)}`);
