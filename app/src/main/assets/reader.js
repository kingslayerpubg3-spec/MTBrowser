(function(){
var ps=[].slice.call(document.querySelectorAll('article p, main p, .post p, .entry-content p, p')).filter(function(p){return p.innerText&&p.innerText.trim().length>60;});
if(ps.length<3)return 'no';
var seen={},out='';
ps.forEach(function(p){var t=p.innerText.trim();if(!seen[t]){seen[t]=1;out+='<p>'+p.innerHTML+'</p>';}});
var imgs='';var im=document.querySelector('article img, main img, .post img');
if(im&&im.src)imgs='<img src="'+im.src+'">';
var css='body{margin:0;padding:22px 18px 60px;background:__BG__;color:__TX__;font:19px/1.75 Georgia,serif}.w{max-width:680px;margin:0 auto}h1{font:700 26px/1.3 sans-serif;margin:8px 0 18px}img{max-width:100%;border-radius:12px;margin:8px 0}a{color:__AC__}p{margin:0 0 18px}';
var title=(document.title||'').replace(/</g,'&lt;');
document.open();
document.write('<meta name="viewport" content="width=device-width,initial-scale=1"><style>'+css+'</style><div class="w"><h1>'+title+'</h1>'+imgs+out+'</div>');
document.close();
return 'ok';
})()
