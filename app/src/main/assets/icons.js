(function(){
var st=document.createElement('style');
st.textContent='.ic{width:58px!important;height:58px!important;border-radius:18px!important;margin:0 auto 7px!important;display:flex!important;align-items:center;justify-content:center;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.28)!important}.ic img{display:block}.ic .g{width:34px!important;height:34px!important}.ic .f{width:100%!important;height:100%!important;padding:10px;box-sizing:border-box;object-fit:contain;background:#fff}.ic span{display:block!important;font-weight:800;font-size:24px}.q em{color:var(--ac);display:flex}button[type=submit]{display:flex;align-items:center;justify-content:center;background:var(--ac);background:linear-gradient(135deg,var(--ac),color-mix(in srgb,var(--ac) 62%,#000))!important;box-shadow:0 0 0 3px rgba(255,255,255,.06),0 6px 16px color-mix(in srgb,var(--ac) 55%,transparent);transition:transform .12s}button[type=submit]:active{transform:scale(.88)}';
document.head.appendChild(st);
var S=[
["YouTube","https://m.youtube.com","youtube.svg","#fff","g"],
["Facebook","https://m.facebook.com","facebook.svg","#0866FF","g"],
["Instagram","https://www.instagram.com","instagram.svg","linear-gradient(45deg,#FEDA75,#FA7E1E,#D62976,#962FBF,#4F5BD5)","g"],
["X","https://x.com","x.svg","#000","g"],
["Wikipedia","https://en.wikipedia.org","wikipedia.svg","#fff","g"],
["Amazon","https://www.amazon.in","amazon.svg","#232F3E","g"],
["Flipkart","https://www.flipkart.com","flipkart.svg","#2874F0","g"],
["Google","https://www.google.com","google.png","#fff","f"],
["Gmail","https://mail.google.com","gmail.png","#fff","f"],
["Maps","https://maps.google.com","maps.png","#fff","f"],
["WhatsApp","https://web.whatsapp.com","whatsapp.svg","#25D366","g"],
["Reddit","https://www.reddit.com","reddit.svg","#FF4500","g"]
];
window.fb=function(k){var e=document.getElementById('i'+k);if(e){e.parentNode.innerHTML='<span style="color:'+(S[k][3]=='#fff'?'#14213D':'#fff')+'">'+S[k][0][0]+'</span>';}};
var h='';
S.forEach(function(s,k){h+='<a href="'+s[1]+'"><div class="ic" style="background:'+s[3]+'"><img id="i'+k+'" class="'+s[4]+'" src="logos/'+s[2]+'" onerror="fb('+k+')"></div>'+s[0]+'</a>';});
var g=document.getElementById('g');if(g)g.innerHTML=h;
function Q(p){return '<svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">'+p+'</svg>';}
var QI=[
Q('<path d="M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z"/>'),
Q('<path d="M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z"/>'),
Q('<path d="M13,3c-4.97,0 -9,4.03 -9,9H1l3.89,3.89 0.07,0.14L9,12H6c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08V8H12z"/>'),
Q('<path d="M12,2.5 L20,7 L20,17 L12,21.5 L4,17 L4,7 Z" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" stroke-width="2"/>')
];
document.querySelectorAll('.q em').forEach(function(e,i){e.innerHTML=QI[i]||'';});
})();
