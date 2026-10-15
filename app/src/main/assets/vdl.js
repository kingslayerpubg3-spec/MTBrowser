(function(){
if(window.__mtv)return;window.__mtv=1;
var btn=null,cur=null,hideT=0,ac='#3B82F6';
try{ac=MTBridge.accent();}catch(e){}
function src(v){var u=v.currentSrc||v.src||'';if(!u){var s=v.querySelector('source');if(s)u=s.src||'';}return u;}
function rep(v){try{MTBridge.video(src(v),document.title||'');}catch(e){}}
function playing(){
var vs=document.querySelectorAll('video'),best=null,ba=0;
for(var i=0;i<vs.length;i++){var v=vs[i];if(v.paused||v.ended||v.readyState<2)continue;
var r=v.getBoundingClientRect(),a=r.width*r.height;if(a>ba){ba=a;best=v;}}
return best;}
function mk(){
if(btn)return btn;
btn=document.createElement('div');
btn.style.cssText='position:fixed;z-index:2147483647;width:42px;height:42px;border-radius:50%;display:none;align-items:center;justify-content:center;background:'+ac+';box-shadow:0 2px 12px rgba(0,0,0,.55);opacity:0;pointer-events:none;transition:opacity .25s;-webkit-tap-highlight-color:transparent';
btn.innerHTML='<svg viewBox="0 0 24 24" width="22" height="22" fill="#fff"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>';
btn.addEventListener('click',function(e){e.stopPropagation();e.preventDefault();
if(cur){try{MTBridge.dl(src(cur),document.title||'');}catch(x){}}},true);
document.documentElement.appendChild(btn);
return btn;}
function show(){
mk();btn.style.display='flex';btn.style.opacity=1;btn.style.pointerEvents='auto';
clearTimeout(hideT);
hideT=setTimeout(function(){if(btn){btn.style.opacity=0;btn.style.pointerEvents='none';}},3500);}
function place(){
var v=playing();
if(!v){
if(cur){cur=null;try{MTBridge.gone();}catch(e){}}
if(btn){btn.style.display='none';btn.style.opacity=0;btn.style.pointerEvents='none';}
return;}
if(v!==cur){cur=v;rep(v);show();}
var r=v.getBoundingClientRect(),vw=window.innerWidth,vh=window.innerHeight;
var top=Math.max(r.top,0)+10,right=Math.min(r.right,vw);
if(r.width<140||r.height<80||top>Math.min(r.bottom,vh)-52){if(btn)btn.style.display='none';return;}
mk();
if(btn.style.display==='none')btn.style.display='flex';
btn.style.top=top+'px';btn.style.left=(right-52)+'px';}
document.addEventListener('touchstart',function(e){
if(!cur||!e.touches[0])return;
var t=e.touches[0],r=cur.getBoundingClientRect();
if(t.clientX>=r.left&&t.clientX<=r.right&&t.clientY>=r.top&&t.clientY<=r.bottom)show();
},{capture:true,passive:true});
document.addEventListener('play',function(e){if(e.target&&e.target.tagName==='VIDEO')setTimeout(place,250);},true);
document.addEventListener('pause',function(){setTimeout(place,250);},true);
window.addEventListener('scroll',place,true);
window.addEventListener('resize',place);
setInterval(place,300);
})();
