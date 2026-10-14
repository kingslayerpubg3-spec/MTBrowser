(function(){
var st=document.createElement('style');
st.textContent='.ic{background:none!important;box-shadow:none!important}.ic svg{width:58px;height:58px;display:block;border-radius:18px;box-shadow:0 4px 12px rgba(0,0,0,.25)}.q em svg{display:block}';
document.head.appendChild(st);
function T(bg,inner,defs){return '<svg viewBox="0 0 48 48"><defs>'+(defs||'')+'</defs><rect width="48" height="48" rx="12" fill="'+bg+'"/>'+inner+'</svg>';}
var IG='<linearGradient id="ig" x1="0" y1="1" x2="1" y2="0"><stop offset="0" stop-color="#FEDA75"/><stop offset=".3" stop-color="#FA7E1E"/><stop offset=".55" stop-color="#D62976"/><stop offset=".8" stop-color="#962FBF"/><stop offset="1" stop-color="#4F5BD5"/></linearGradient>';
var S=[
["YouTube","https://m.youtube.com",T('#fff','<rect x="6" y="13" width="36" height="22" rx="7" fill="#FF0000"/><path d="M20,18.5 L31,24 L20,29.5Z" fill="#fff"/>')],
["Facebook","https://m.facebook.com",T('#1877F2','<path d="M26.5,41 V27 h4.7 l.8,-5.7 h-5.5 v-3.6 c0,-1.7 .8,-2.8 3,-2.8 h2.7 V9.9 c-.5,-.1 -2.3,-.3 -4.2,-.3 -4.3,0 -6.9,2.5 -6.9,7 v4.7 H16 V27 h4.6 v14 z" fill="#fff"/>')],
["Instagram","https://www.instagram.com",T('url(#ig)','<rect x="12" y="12" width="24" height="24" rx="7" fill="none" stroke="#fff" stroke-width="3"/><circle cx="24" cy="24" r="5.5" fill="none" stroke="#fff" stroke-width="3"/><circle cx="31.2" cy="16.8" r="1.8" fill="#fff"/>',IG)],
["X","https://x.com",T('#000','<path d="M13,12 L35,36 M35,12 L13,36" stroke="#fff" stroke-width="3.6" stroke-linecap="round"/>')],
["Wikipedia","https://en.wikipedia.org",T('#fff','<text x="24" y="34" font-size="28" font-family="serif" text-anchor="middle" fill="#111">W</text>')],
["Amazon","https://www.amazon.in",T('#232F3E','<text x="24" y="29" font-size="26" font-weight="bold" font-family="sans-serif" text-anchor="middle" fill="#fff">a</text><path d="M12,34 Q24,41 36,33" fill="none" stroke="#FF9900" stroke-width="3" stroke-linecap="round"/>')],
["Flipkart","https://www.flipkart.com",T('#2874F0','<path d="M15,17 h18 l-1.5,19 h-15z" fill="#FFE11B"/><path d="M19,17 c0,-6 10,-6 10,0" fill="none" stroke="#FFE11B" stroke-width="2.4"/><text x="24" y="32" font-size="15" font-weight="bold" font-family="sans-serif" text-anchor="middle" fill="#2874F0">f</text>')],
["Google","https://www.google.com",T('#fff','<g fill="none" stroke-width="5"><path d="M32.43,16.93 A11,11 0 0 0 16.22,16.22" stroke="#EA4335"/><path d="M16.22,16.22 A11,11 0 0 0 14.99,30.31" stroke="#FBBC05"/><path d="M14.99,30.31 A11,11 0 0 0 31.07,32.43" stroke="#34A853"/><path d="M31.07,32.43 A11,11 0 0 0 35,24 L24,24" stroke="#4285F4"/></g>')],
["Gmail","https://mail.google.com",T('#fff','<rect x="9" y="14" width="30" height="21" rx="3" fill="#fff" stroke="#DADCE0" stroke-width="1.5"/><path d="M9.8,16 L24,27 L38.2,16" fill="none" stroke="#EA4335" stroke-width="3.2"/><path d="M10,33 V17" stroke="#4285F4" stroke-width="3.2"/><path d="M38,33 V17" stroke="#34A853" stroke-width="3.2"/>')],
["Maps","https://maps.google.com",T('#fff','<path d="M24,9 c-5.5,0 -10,4.4 -10,9.8 c0,7.4 10,20.2 10,20.2 s10,-12.8 10,-20.2 c0,-5.4 -4.5,-9.8 -10,-9.8z" fill="#EA4335"/><circle cx="24" cy="19" r="4" fill="#fff"/>')],
["WhatsApp","https://web.whatsapp.com",T('#25D366','<circle cx="24" cy="23" r="13" fill="none" stroke="#fff" stroke-width="3"/><path d="M12,38 l2.5,-7" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round"/><path d="M19.2,17.5 c-.5,2.8 .9,5.6 3.2,7.9 s5.2,3.4 7.6,3.1 l1.2,-2.4 -3,-1.5 -1.3,1.3 c-1.6,-.8 -3.1,-2.2 -3.9,-3.9 l1.3,-1.3 -1.5,-3z" fill="#fff"/>')],
["Reddit","https://www.reddit.com",T('#FF4500','<ellipse cx="24" cy="28" rx="12" ry="8.5" fill="#fff"/><circle cx="11.5" cy="25" r="2.6" fill="#fff"/><circle cx="36.5" cy="25" r="2.6" fill="#fff"/><circle cx="19.5" cy="27" r="2.2" fill="#FF4500"/><circle cx="28.5" cy="27" r="2.2" fill="#FF4500"/><path d="M19.5,32 q4.5,3 9,0" fill="none" stroke="#FF4500" stroke-width="1.8" stroke-linecap="round"/><path d="M24,19.5 L26,12 L31,13.5" fill="none" stroke="#fff" stroke-width="1.8"/><circle cx="32" cy="13.5" r="2.4" fill="#fff"/>')]
];
var h='';
document.getElementById('g').innerHTML=h;
function Q(p){return '<svg viewBox="0 0 24 24" width="22" height="22" fill="#2E8BFF">'+p+'</svg>';}
var QI=[
Q('<path d="M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z"/>'),
Q('<path d="M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z"/>'),
Q('<path d="M13,3c-4.97,0 -9,4.03 -9,9H1l3.89,3.89 0.07,0.14L9,12H6c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08V8H12z"/>'),
Q('<path d="M12,2.5 L20,7 L20,17 L12,21.5 L4,17 L4,7 Z" fill="none" stroke="#2E8BFF" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="none" stroke="#2E8BFF" stroke-width="2"/>')
];
document.querySelectorAll('.q em').forEach(function(e,i){e.innerHTML=QI[i]||'';});
})();
