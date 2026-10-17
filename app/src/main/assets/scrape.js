(function(){
if(window.__mtscr)return;window.__mtscr=1;
var seen={};
function scan(){
var h='';try{h=document.documentElement.innerHTML;}catch(e){return;}
h=h.replace(/\\\//g,'/').replace(/\\u002F/gi,'/').replace(/&amp;/g,'&');
var re=/https?:\/\/[^\s"'<>\\]+?\.(?:m3u8|mp4|webm|mkv)(?:\?[^\s"'<>\\]*)?/gi;
var m,out=[];
while((m=re.exec(h))!==null){var u=m[0];if(!seen[u]&&out.length<80){seen[u]=1;out.push(u);}}
var vs=document.querySelectorAll('video,source');
for(var i=0;i<vs.length;i++){var s=vs[i].src||vs[i].currentSrc||'';if(s.indexOf('http')===0&&!seen[s]){seen[s]=1;out.push(s);}}
if(out.length){try{MTBridge.media(JSON.stringify(out));}catch(e){}}
}
setTimeout(scan,1500);setTimeout(scan,5000);
document.addEventListener('play',function(){setTimeout(scan,600);},true);
})();
