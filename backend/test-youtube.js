// Quick test script for YouTube API endpoint
// Run: node test-youtube.js

const http = require('http');

const CHANNEL_ID = 'UCuzuc0P8L1fVIeeZrNLnkQA';
const BASE_URL = 'http://localhost:3000';

console.log('🧪 Testing YouTube API Endpoint...\n');
console.log(`Channel ID: ${CHANNEL_ID}\n`);

// Test 1: Get videos from channel
console.log('📹 Test 1: Fetching videos from channel...');
const testUrl = `${BASE_URL}/youtube/videos?channelId=${CHANNEL_ID}&maxResults=5`;

http.get(testUrl, (res) => {
  let data = '';
  
  res.on('data', (chunk) => {
    data += chunk;
  });
  
  res.on('end', () => {
    try {
      const json = JSON.parse(data);
      
      if (json.success) {
        console.log('✅ SUCCESS!');
        console.log(`📊 Found ${json.videos?.length || 0} videos\n`);
        
        if (json.videos && json.videos.length > 0) {
          console.log('📺 Sample Videos:');
          json.videos.slice(0, 3).forEach((video, index) => {
            console.log(`\n${index + 1}. ${video.title}`);
            console.log(`   ID: ${video.id}`);
            console.log(`   URL: ${video.videoUrl}`);
          });
        }
      } else {
        console.log('❌ FAILED!');
        console.log(`Error: ${json.message}`);
      }
    } catch (e) {
      console.log('❌ Error parsing response:', e.message);
      console.log('Response:', data);
    }
  });
}).on('error', (err) => {
  console.log('❌ Connection Error:', err.message);
  console.log('\n💡 Make sure:');
  console.log('   1. Backend server is running (npm start)');
  console.log('   2. YOUTUBE_API_KEY is set in .env file');
  console.log('   3. Server is accessible at http://localhost:3000');
});

