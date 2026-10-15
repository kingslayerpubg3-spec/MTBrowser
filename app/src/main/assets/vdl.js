(function(){
if(window.__mtv)return;window.__mtv=1;
var btn=null,cur=null,cs='',hideT=0,drag=false,ox=84,oy=22,sx=0,sy=0,sox=0,soy=0;
try{var sp=JSON.parse(localStorage.getItem('mtdlp')||'null');if(sp){ox=sp.x;oy=sp.y;}}catch(e){}
function src(v){var u=v.currentSrc||v.src||'';if(!u){var s=v.querySelector('source');if(s)u=s.src||'';}return u;}
function dur(v){var d=v.duration;if(d===Infinity)return 1e7;return(isFinite(d)&&d>0)?d:0;}
function rep(v){try{MTBridge.video(src(v),document.title||'');}catch(e){}}
function playing(){
var vs=document.querySelectorAll('video'),best=null,bd=-1,ba=0;
for(var i=0;i<vs.length;i++){var v=vs[i];if(v.paused||v.ended||v.readyState<2)continue;
var r=v.getBoundingClientRect(),a=r.width*r.height;if(a<5000)continue;
var d=dur(v);if(d>bd||(d===bd&&a>ba)){best=v;bd=d;ba=a;}}
return best;}
function mk(){
if(btn)return btn;
btn=document.createElement('div');
btn.style.cssText='position:fixed;z-index:2147483647;width:40px;height:40px;display:none;align-items:center;justify-content:center;opacity:0;pointer-events:none;transition:opacity .25s;touch-action:none;-webkit-tap-highlight-color:transparent';
btn.innerHTML='<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" style="filter:drop-shadow(0 1px 3px rgba(0,0,0,.85))"><path d="M12 4v11M6.5 10.5L12 16l5.5-5.5M5 20h14"/></svg>';
btn.addEventListener('click',function(e){e.stopPropagation();e.preventDefault();
if(drag||!cur)return;
try{MTBridge.dl(src(cur),document.title||'');}catch(x){}},true);
btn.addEventListener('touchstart',function(e){var t=e.touches[0];sx=t.clientX;sy=t.clientY;sox=ox;soy=oy;drag=false;show();},{passive:true});
btn.addEventListener('touchmove',function(e){var t=e.touches[0],dx=t.clientX-sx,dy=t.clientY-sy;
if(!drag&&Math.abs(dx)+Math.abs(dy)>12)drag=true;
if(drag){ox=sox-dx;oy=soy-dy;place();show();e.preventDefault();}},{passive:false});
btn.addEventListener('touchend',function(e){
if(drag){try{localStorage.setItem('mtdlp',JSON.stringify({x:ox,y:oy}));}catch(x){}e.preventDefault();setTimeout(function(){drag=false;},80);}},{passive:false});
document.documentElement.appendChild(btn);
return btn;}
function show(){
mk();btn.style.display='flex';btn.style.opacity=1;btn.style.pointerEvents='auto';
clearTimeout(hideT);
hideT=setTimeout(function(){if(btn&&!drag){btn.style.opacity=0;btn.style.pointerEvents='none';}},3500);}
function place(){
var v=playing();
if(!v){
if(cur){cur=null;cs='';try{MTBridge.gone();}catch(e){}}
if(btn){btn.style.display='none';btn.style.opacity=0;btn.style.pointerEvents='none';}
return;}
var s=src(v);
if(v!==cur||s!==cs){cur=v;cs=s;rep(v);show();}
var r=v.getBoundingClientRect(),vw=window.innerWidth,vh=window.innerHeight;
if(r.width<140||r.height<80){if(btn)btn.style.display='none';return;}
var rt=Math.min(r.right,vw),bt=Math.min(r.bottom,vh),lf=Math.max(r.left,0),tp=Math.max(r.top,0);
ox=Math.max(0,Math.min(ox,rt-lf-40));oy=Math.max(0,Math.min(oy,bt-tp-40));
mk();
if(btn.style.display==='none')btn.style.display='flex';
btn.style.left=(rt-ox-20)+'px';btn.style.top=(bt-oy-20)+'px';}
document.addEventListener('touchstart',function(e){
if(!cur||!e.touches[0])return;
var t=e.touches[0],r=cur.getBoundingClientRect();
if(t.clientX>=r.left&&t.clientX<=r.right&&t.clientY>=r.top&&t.clientY<=r.bottom)show();
},{capture:true,passive:true});
document.addEventListener('play',function(e){if(e.target&&e.target.tagName==='VIDEO')setTimeout(place,250);},true);
document.addEventListener('pause',function(){setTimeout(place,250);},true);
document.addEventListener('loadedmetadata',function(){setTimeout(place,250);},true);
window.addEventListener('scroll',place,true);
window.addEventListener('resize',place);
setInterval(place,300);
})();
