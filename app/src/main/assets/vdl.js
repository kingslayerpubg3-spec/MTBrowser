(function(){
if(window.__mtv)return;window.__mtv=1;
var lastTouch=0,lastOpen=0;
document.addEventListener('touchstart',function(){lastTouch=Date.now();},{capture:true,passive:true});
document.addEventListener('click',function(){lastTouch=Date.now();},true);
function src(v){var u=v.currentSrc||v.src||'';if(!u){var s=v.querySelector('source');if(s)u=s.src||'';}return u;}
function dur(v){var d=v.duration;if(d===Infinity)return 1e7;return(isFinite(d)&&d>0)?d:0;}
function rep(v){try{MTBridge.video(src(v),document.title||'');}catch(e){}}
function tryOpen(v){
var now=Date.now();
if(now-lastOpen<1500)return;
var r=v.getBoundingClientRect();
if(r.width<200||r.height<110)return;
var user=(now-lastTouch)<3000;
if(!user&&v.muted)return;
var d=dur(v);
if(d>0&&d<20)return;
var ok=false;try{ok=MTBridge.canOpen();}catch(e){}
if(!ok)return;
rep(v);
var done=false;
try{done=MTBridge.open(src(v),document.title||'',v.currentTime||0);}catch(e){}
if(done){lastOpen=now;try{v.pause();}catch(e){}}}
function arm(v){
var n=0;
(function t(){
if(v.paused||v.ended)return;
tryOpen(v);
if(v.paused)return;
if(++n<7)setTimeout(t,n*350);
})();}
function onPlay(e){var v=e.target;if(v&&v.tagName==='VIDEO'){rep(v);arm(v);}}
document.addEventListener('play',onPlay,true);
document.addEventListener('playing',onPlay,true);
})();
