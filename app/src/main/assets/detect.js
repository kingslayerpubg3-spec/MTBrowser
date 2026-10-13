(function(){
  var r=[],s={};
  function add(u,l){
    if(!u||u.indexOf('blob:')==0||u.indexOf('data:')==0)return;
    try{u=new URL(u,location.href).href}catch(e){return}
    if(s[u])return; s[u]=1; r.push({u:u,l:l||''});
  }
  document.querySelectorAll('video,audio').forEach(function(v){
    add(v.currentSrc||v.src, v.tagName.toLowerCase());
    v.querySelectorAll('source').forEach(function(x){
      add(x.src, x.getAttribute('label')||x.getAttribute('res')||x.getAttribute('title')||x.type);
    });
  });
  document.querySelectorAll('a[href]').forEach(function(a){
    if(/\.(mp4|webm|mkv|3gp|mp3|m4a|aac|wav|ogg|pdf|zip|rar|7z|apk|docx?|xlsx?|pptx?|jpe?g|png|gif)(\?|$)/i.test(a.href))
      add(a.href,(a.textContent||'').trim().slice(0,30));
  });
  return JSON.stringify(r);
})()
