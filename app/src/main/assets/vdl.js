(function(){
if(window.__mtv)return;window.__mtv=1;
var act=null,lastTouch=0,sched=0;
document.addEventListener('touchstart',function(){lastTouch=Date.now();},{capture:true,passive:true});
document.addEventListener('click',function(){lastTouch=Date.now();},true);
function src(v){var u=v.currentSrc||v.src||'';if(!u){var s=v.querySelector('source');if(s)u=s.src||'';}return u;}
function dur(v){var d=v.duration;if(d===Infinity)return 1e7;return(isFinite(d)&&d>0)?d:0;}
function mainVideo(){
var vs=document.querySelectorAll('video'),best=null,bd=0;
for(var i=0;i<vs.length;i++){var v=vs[i];if(v.__mtno)continue;
var d=dur(v);if(d<90||d>=1e7)continue;
var r=v.getBoundingClientRect();if(r.width<200||r.height<110)continue;
if(d>bd){best=v;bd=d;}}
return best;}
function send(v){
var r=v.getBoundingClientRect();
var vis=r.width>60&&r.height>40&&r.bottom>0&&r.top<window.innerHeight&&r.right>0&&r.left<window.innerWidth;
try{MTBridge.rect(window.devicePixelRatio||1,r.left,r.top,r.width,r.height,vis);}catch(e){}}
function enforce(){if(act){try{if(!act.paused)act.pause();act.muted=true;}catch(e){}}}
function begin(v){
v.__mta=(v.__mta||0)+1;if(v.__mta>20)return;
var ok=false;try{ok=MTBridge.canOpen();}catch(e){}
if(!ok)return;
var r=v.getBoundingClientRect(),done=false;
try{done=MTBridge.inline(src(v),document.title||'',v.currentTime||0,dur(v),window.devicePixelRatio||1,r.left,r.top,r.width,r.height);}catch(e){}
if(done){act=v;v.__mtm=v.muted;enforce();}}
window.__mtrel=function(){if(act){try{act.muted=!!act.__mtm;}catch(e){}act.__mtno=true;act=null;}};
function tick(){
if(act){
if(!document.documentElement.contains(act)){act=null;try{MTBridge.vclose();}catch(e){}return;}
enforce();send(act);return;}
var v=mainVideo();if(!v||v.paused||v.ended)return;
if(v.muted&&(Date.now()-lastTouch)>8000)return;
begin(v);}
function sch(){if(sched)return;sched=1;requestAnimationFrame(function(){sched=0;if(act)send(act);});}
document.addEventListener('play',function(e){if(act&&e.target===act){try{act.pause();}catch(x){}}setTimeout(tick,150);},true);
document.addEventListener('playing',function(e){if(act&&e.target===act){try{act.pause();}catch(x){}}setTimeout(tick,150);},true);
document.addEventListener('loadedmetadata',function(){setTimeout(tick,200);},true);
window.addEventListener('scroll',sch,true);
window.addEventListener('resize',sch);
document.addEventListener('touchmove',sch,{capture:true,passive:true});
setInterval(tick,300);
})();
