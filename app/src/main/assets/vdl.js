(function(){
if(window.__mtv)return;window.__mtv=1;
function src(v){var u=v.currentSrc||v.src||'';if(!u){var s=v.querySelector('source');if(s)u=s.src||'';}return u;}
function rep(v){try{MTBridge.video(src(v),document.title||'');}catch(e){}}
function chk(){
var vs=document.querySelectorAll('video'),pl=false;
for(var i=0;i<vs.length;i++){var v=vs[i];
if(!v.paused&&!v.ended&&v.readyState>1){pl=true;var u=src(v);if(v.__r!==u){v.__r=u;rep(v);}}}
if(pl)window.__mtp=1;
if(vs.length===0&&window.__mtp){window.__mtp=0;try{MTBridge.gone();}catch(e){}}
}
document.addEventListener('play',function(e){if(e.target&&e.target.tagName==='VIDEO')setTimeout(chk,300);},true);
document.addEventListener('pause',function(){setTimeout(chk,300);},true);
setInterval(chk,1500);
})();
