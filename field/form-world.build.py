CSS = """
:root{--ink:rgba(236,232,228,.92);--dim:rgba(236,232,228,.42);--faint:rgba(236,232,228,.18);--glass:rgba(10,8,9,.52);--gl:rgba(255,255,255,.08);}
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent;}
html,body{margin:0;height:100%;background:#000;overflow:hidden;}
body{font-family:ui-sans-serif,-apple-system,sans-serif;color:var(--ink);touch-action:none;}
#stage{position:fixed;inset:0;}canvas{display:block;width:100%;height:100%;touch-action:none;}
#fallback{position:fixed;inset:0;display:none;align-items:center;justify-content:center;text-align:center;background:#050009;padding:32px;}
#fallback p{max-width:44ch;font-size:12px;line-height:1.6;color:var(--dim);font-family:ui-monospace,monospace;word-break:break-word;}
#hint{position:fixed;left:50%;top:16px;transform:translateX(-50%);font-size:10px;letter-spacing:.2em;text-transform:uppercase;color:var(--dim);pointer-events:none;transition:opacity 1s;opacity:.8;text-align:center;}
#hint.gone{opacity:0;}
#panel{position:fixed;left:0;right:0;bottom:0;padding:14px 14px calc(14px + env(safe-area-inset-bottom));background:var(--glass);-webkit-backdrop-filter:blur(18px);backdrop-filter:blur(18px);border-top:1px solid var(--gl);transform:translateY(0);transition:transform .42s cubic-bezier(.22,.61,.36,1),opacity .42s;max-height:80vh;overflow-y:auto;overscroll-behavior:contain;touch-action:pan-y;}
#panel.hidden{transform:translateY(105%);opacity:0;}
.head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;}
.ttl{font-size:11px;letter-spacing:.34em;text-transform:uppercase;color:var(--dim);}
.hb{display:flex;gap:8px;}
.ib{width:34px;height:30px;border:1px solid var(--gl);border-radius:8px;background:transparent;color:var(--ink);font-size:14px;display:flex;align-items:center;justify-content:center;cursor:pointer;}
.ib:active{background:rgba(255,255,255,.07);}
.group{margin-bottom:13px;}.group:last-child{margin-bottom:2px;}
.lbl{font-size:9.5px;letter-spacing:.2em;text-transform:uppercase;color:var(--faint);margin-bottom:7px;display:block;}
.chips{display:flex;flex-wrap:wrap;gap:7px;}
.chip{font-size:11px;letter-spacing:.06em;padding:7px 11px;border-radius:999px;border:1px solid var(--gl);background:transparent;color:var(--dim);cursor:pointer;transition:color .2s,border-color .2s,background .2s;-webkit-user-select:none;user-select:none;}
.chip[aria-pressed=true]{color:#0a0809;background:var(--ink);border-color:var(--ink);}
.row{display:flex;align-items:center;gap:12px;}
.row .lbl{margin:0;min-width:52px;flex:0 0 auto;}
input[type=range]{-webkit-appearance:none;appearance:none;flex:1;height:2px;border-radius:2px;background:rgba(255,255,255,.16);outline:none;margin:13px 0;}
input[type=range]::-webkit-slider-thumb{-webkit-appearance:none;width:15px;height:15px;border-radius:50%;background:var(--ink);border:0;cursor:pointer;}
input[type=range]::-moz-range-thumb{width:15px;height:15px;border-radius:50%;background:var(--ink);border:0;cursor:pointer;}
.sw{display:flex;gap:14px;align-items:center;margin-bottom:9px;flex-wrap:wrap;}
.sw label{display:flex;align-items:center;gap:7px;}
.sw span{font-size:9.5px;letter-spacing:.16em;text-transform:uppercase;color:var(--faint);}
input[type=color]{-webkit-appearance:none;appearance:none;width:40px;height:28px;border:1px solid var(--gl);border-radius:7px;background:transparent;padding:2px;cursor:pointer;}
input[type=color]::-webkit-color-swatch-wrapper{padding:0;}
input[type=color]::-webkit-color-swatch{border:0;border-radius:5px;}
.grid{display:grid;grid-template-columns:1fr;gap:1px;}
@media(min-width:560px){.grid{grid-template-columns:1fr 1fr;gap:3px 22px;}}
"""

HTML_BODY = """
<div id="stage"><canvas id="gl"></canvas></div>
<div id="fallback"><p id="fmsg">This piece needs WebGL.</p></div>
<div id="hint">drag&nbsp;to&nbsp;look&nbsp;&middot;&nbsp;pinch&nbsp;to&nbsp;zoom&nbsp;&middot;&nbsp;tap&nbsp;to&nbsp;hide</div>
<div id="panel">
  <div class="head"><span class="ttl">Form&nbsp;&middot;&nbsp;World</span><div class="hb"><button class="ib" id="reseed">&#10227;</button><button class="ib" id="hide">&#9662;</button></div></div>
  <div class="group"><span class="lbl">Scene</span><div class="chips" id="scene">
    <button class="chip" data-scn="0" aria-pressed="false">Bowl</button>
    <button class="chip" data-scn="1" aria-pressed="true">Helix</button>
    <button class="chip" data-scn="2" aria-pressed="false">Towers</button>
    <button class="chip" data-scn="3" aria-pressed="false">Arch</button>
    <button class="chip" data-scn="4" aria-pressed="false">Ruins</button>
    <button class="chip" data-scn="5" aria-pressed="false">Planet</button>
  </div></div>
  <div class="group"><span class="lbl">Mode</span>
    <div class="chips" id="motion" style="margin-bottom:7px;">
      <button class="chip" data-mot="0" aria-pressed="true">Passive</button>
      <button class="chip" data-mot="1" aria-pressed="false">Active</button>
    </div>
    <div class="chips" id="recurse">
      <button class="chip" data-rec="0" aria-pressed="true">Once</button>
      <button class="chip" data-rec="1" aria-pressed="false">Recursive</button>
    </div>
  </div>
  <div class="group"><span class="lbl">Sprites</span>
    <div class="chips" id="sprbeh" style="margin-bottom:8px;">
      <button class="chip" data-spb="0" aria-pressed="true">Drift</button>
      <button class="chip" data-spb="1" aria-pressed="false">Seek</button>
      <button class="chip" data-spb="2" aria-pressed="false">Ember</button>
    </div>
    <div class="row"><span class="lbl">Count</span><input type="range" id="sprCount" min="0" max="8" step="1" value="4"></div>
  </div>
  <div class="group"><span class="lbl">Colour&nbsp;&middot;&nbsp;stone / smog / light</span>
    <div class="sw">
      <label><span>Stone</span><input type="color" id="matCol" value="#9b958c"></label>
      <label><span>Smog</span><input type="color" id="fogCol" value="#cc1a0a"></label>
      <label><span>Light</span><input type="color" id="lightCol" value="#ffd2b0"></label>
    </div>
    <div class="chips" id="pattern">
      <button class="chip" data-pat="0" aria-pressed="true">Even</button>
      <button class="chip" data-pat="1" aria-pressed="false">Flow</button>
      <button class="chip" data-pat="2" aria-pressed="false">Aurora</button>
      <button class="chip" data-pat="3" aria-pressed="false">Bloom</button>
      <button class="chip" data-pat="4" aria-pressed="false">Bands</button>
    </div>
  </div>
  <div class="group"><span class="lbl">Surface</span>
    <div class="chips" id="texture" style="margin-bottom:8px;">
      <button class="chip" data-tex="0" aria-pressed="true">Soil</button>
      <button class="chip" data-tex="1" aria-pressed="false">Hatch</button>
      <button class="chip" data-tex="2" aria-pressed="false">Warp</button>
      <button class="chip" data-tex="3" aria-pressed="false">Facet</button>
    </div>
    <div class="grid">
      <div class="row"><span class="lbl">Grit</span><input type="range" id="grit" min="0" max="1.5" step=".001" value="1"></div>
      <div class="row"><span class="lbl">Coarse</span><input type="range" id="coarse" min="0" max="1" step=".001" value=".5"></div>
      <div class="row"><span class="lbl">Grain</span><input type="range" id="grain" min="0" max="1" step=".001" value=".5"></div>
      <div class="row"><span class="lbl">Grain&nbsp;sz</span><input type="range" id="grainSc" min="0" max="1" step=".001" value=".5"></div>
    </div>
  </div>
  <div class="group"><span class="lbl">Light</span>
    <div class="chips" id="light" style="margin-bottom:8px;">
      <button class="chip" data-lite="breathe" aria-pressed="true">Breathe</button>
      <button class="chip" data-lite="flicker" aria-pressed="false">Flicker</button>
      <button class="chip" data-lite="drift"   aria-pressed="true">Drift</button>
      <button class="chip" data-lite="pulse"   aria-pressed="false">Pulse</button>
    </div>
    <div class="grid">
      <div class="row"><span class="lbl">Aim</span><input type="range" id="aim" min="0" max="6.2832" step=".001" value="2.2"></div>
      <div class="row"><span class="lbl">Glow</span><input type="range" id="glow" min=".3" max="2.0" step=".001" value="1.2"></div>
      <div class="row"><span class="lbl">Inner</span><input type="range" id="inner" min="0" max="1.5" step=".001" value=".3"></div>
    </div>
  </div>
  <div class="group grid">
    <div class="row"><span class="lbl">Zoom</span><input type="range" id="zoom" min="0" max="1" step=".001" value=".5"></div>
    <div class="row"><span class="lbl">Count</span><input type="range" id="count" min="1" max="7" step="1" value="5"></div>
    <div class="row"><span class="lbl">Look X</span><input type="range" id="yaw" min="-1.4" max="1.4" step=".001" value="0"></div>
    <div class="row"><span class="lbl">Look Y</span><input type="range" id="tilt" min="-1.0" max="1.0" step=".001" value="0"></div>
    <div class="row"><span class="lbl">Atmos</span><input type="range" id="void" min="0" max="1" step=".001" value=".5"></div>
    <div class="row"><span class="lbl">Speed</span><input type="range" id="speed" min="0" max="1" step=".001" value=".28"></div>
  </div>
</div>
"""

FRAG_LINES = [
    '#ifdef GL_FRAGMENT_PRECISION_HIGH','precision highp float;','#else','precision mediump float;','#endif',
    'uniform vec2 uRes;',
    'uniform float uTime,uSpeed,uSeed,uGrain,uGlow,uAim,uVoid,uPattern,uZoom,uCount,uYaw,uTilt,uInner,uTexType,uScene,uGrit,uCoarse,uGrainSc;',
    'uniform float uActive,uRecursive,uSprCount,uSprMode;',
    'uniform vec3 uMat,uFog,uLight;',
    'uniform float uBreathe,uFlicker,uDrift,uPulse;',
    'uniform vec4 uSpr[8];',
    'uniform vec4 uSprD[8];',
    'const int MAXS=96; const int SHS=12; const int AOS=5;',
    'const float PI2=6.28318530718;',
    'vec3 Lvec; float Bright;',

    'float h11(float p){ p=fract(p*.1031); p*=p+33.33; p*=p+p; return fract(p); }',
    'float h21(vec2 p){ vec3 q=fract(vec3(p.xyx)*.1031); q+=dot(q,q.yzx+33.33); return fract((q.x+q.y)*q.z); }',
    'vec2 h22(vec2 p){ vec3 q=fract(vec3(p.xyx)*vec3(.1031,.1030,.0973)); q+=dot(q,q.yzx+33.33); return fract((q.xx+q.yz)*q.zy); }',

    'float vnoise(vec2 p){ vec2 i=floor(p),f=fract(p); f=f*f*(3.-2.*f); float a=h21(i),b=h21(i+vec2(1,0)),c=h21(i+vec2(0,1)),d=h21(i+vec2(1,1)); return mix(mix(a,b,f.x),mix(c,d,f.x),f.y); }',
    'float n3(vec3 x){ vec3 p=floor(x),f=fract(x); f=f*f*(3.-2.*f); float n=p.x+p.y*57.+113.*p.z; float a=h11(n),b=h11(n+1.),c=h11(n+57.),d=h11(n+58.),e=h11(n+113.),f1=h11(n+114.),g=h11(n+170.),hh=h11(n+171.); return mix(mix(mix(a,b,f.x),mix(c,d,f.x),f.y),mix(mix(e,f1,f.x),mix(g,hh,f.x),f.y),f.z); }',
    'float fbm3(vec3 p){ float v=0.,a=.5; for(int i=0;i<3;i++){ v+=a*n3(p); p*=2.02; a*=.5; } return v; }',

    'mat2 r2(float a){ float c=cos(a),s=sin(a); return mat2(c,-s,s,c); }',
    'float smin(float a,float b,float k){ float h=clamp(.5+.5*(b-a)/k,0.,1.); return mix(b,a,h)-k*h*(1.-h); }',
    'float sdBox(vec3 p,vec3 b){ vec3 d=abs(p)-b; return length(max(d,0.))+min(max(d.x,max(d.y,d.z)),0.); }',
    'float sdTorusZ(vec3 p,vec2 t){ vec2 q=vec2(length(p.xy)-t.x,p.z); return length(q)-t.y; }',
    'float sdSphere(vec3 p,float r){ return length(p)-r; }',
    'float sdHelix(vec3 p,float R,float pitch,float rt){ float a=atan(p.z,p.x+1e-5); float c=pitch/PI2; float yy=p.y-c*a; yy-=pitch*floor(yy/pitch+.5); return (length(vec2(length(p.xz)-R,yy))-rt)*.5; }',

    # soil/gravel displacement: distance-attenuated (kills far-field aliasing), lower top freq
    'float facetField(vec3 p){ return fbm3(p*mix(.7,2.0,uCoarse)); }',
    'float soil(vec3 p){',
    '  if(uTexType>2.5){ float q=floor(facetField(p)*5.)/5.; return (q-.5)*.13*uGrit; }',
    '  float fs=mix(.65,1.6,uCoarse); float v=n3(p*11.*fs)*.05+n3(p*27.*fs)*.022+n3(p*60.*fs)*.011-.045; return v*uGrit; }',
    'float cellEdge(vec3 p){ float e=fract(facetField(p)*5.); e=min(e,1.-e); return smoothstep(0.0,.09,e); }',

    # wisp glow: head-bright/tail-dim, elongated along velocity (never a sphere)
    'float wispGlow(vec3 ro,vec3 rd,vec3 pos,vec3 wdir,float wlen,float br,float thit,float hit){',
    '  if(br<.005) return 0.; float wRad=.040; float g=0.;',
    '  for(int wi=0;wi<2;wi++){',
    '    float wt=float(wi)*.65+.18;',
    '    vec3 wp=pos+wdir*(wt-.5)*wlen;',
    '    vec3 oc=ro-wp; float bdot=dot(rd,oc);',
    '    float s=-bdot; if(s<.001) continue;',
    '    if(hit>.5&&s>thit) continue;',
    '    float dist2=max(dot(oc,oc)-bdot*bdot,0.);',
    '    float endFade=(wi==1)?.72:.38;',
    '    g+=exp(-dist2/(wRad*wRad))*br*endFade; }',
    '  return g; }',

    # ===== scene field =====
    'float map(vec3 pos){',
    '  float d=1e9; float aMult=mix(.15,1.,uActive);',

    # BOWL (0): concentric rings receding to a glowing throat
    '  if(uScene<.5){',
    '    float bowlMax=(uRecursive>.5)?28.:(uCount+1.);',
    '    for(int i=0;i<28;i++){ if(float(i)>=bowlMax) break; float fi=float(i);',
    '      float R=max(2.2-fi*.25,.04); float tu=max(.38-fi*.018,.04);',
    '      float spi=fi*.55+uTime*uSpeed*aMult*(.28+fi*.055);',
    '      float wamp=mix(.0,.08,uActive)*fi*.04;',
    '      vec3 rp=pos-vec3(sin(spi)*wamp,cos(spi*.7)*wamp,-fi*.72);',
    '      d=min(d,sdTorusZ(rp,vec2(R,tu))); }',
    '    return d; }',

    # HELIX (1)
    '  if(uScene<1.5){',
    '    float H=mix(3.,7.,clamp((uCount-1.)/6.,0.,1.));',
    '    vec3 ph=pos; ph.xz=r2(uTime*uSpeed*.06*aMult)*ph.xz; ph.y-=uTime*uSpeed*.28*aMult;',
    '    float hd=sdHelix(ph,2.1,1.45,.52);',
    '    d=min(d,(uRecursive>.5)?hd:max(hd,abs(ph.y)-H)); ',
    '    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }',

    # TOWERS (2): plunge from deep below up high; bases dissolve via height-fog
    '  else if(uScene<2.5){',
    '    if(uRecursive>.5){',
    '      float cell=3.4;',
    '      vec2 cid=floor((pos.xz+cell*.5)/cell);',
    '      vec3 q=vec3(mod(pos.x+cell*.5,cell)-cell*.5,pos.y,mod(pos.z+cell*.5,cell)-cell*.5);',
    '      float rv=h21(cid); float top=mix(6.,13.,rv); float wi=mix(.45,.95,h21(cid+vec2(3.7,21.)));',
    '      float bot=-10.; float cy=(top+bot)*.5; float hh=(top-bot)*.5;',
    '      float k=mix(.3,1.1,h21(cid+vec2(1.1,5.5)));',
    '      float sway=sin(uTime*uSpeed*aMult*.11+rv*6.28)*.04;',
    '      q.xz=r2(k*(q.y/max(top,1.))+sway)*q.xz;',
    '      d=min(d,sdBox(q-vec3(0,cy,0),vec3(wi,hh,wi)));',
    '    } else {',
    '      for(int i=0;i<4;i++){ if(float(i)>=uCount) break; float fi=float(i);',
    '        vec2 rv=h22(vec2(uSeed+fi*3.7,21.));',
    '        float xi=(i==0)?.6:(rv.x-.5)*8.; float zi=(i==0)?0.:-fi*3.2-2.;',
    '        float wi=(i==0)?1.0:max(.7-fi*.09,.25); float top=(i==0)?13.:max(10.-fi*1.5,3.5);',
    '        float bot=-7.; float cy=(top+bot)*.5; float hh=(top-bot)*.5;',
    '        vec3 q=pos-vec3(xi,0.,zi);',
    '        float sway=sin(uTime*uSpeed*aMult*.11+rv.x*6.28)*.03;',
    '        q.xz=r2(mix(.4,1.2,rv.x)*(q.y/max(top,1.))+sway)*q.xz;',
    '        d=min(d,sdBox(q-vec3(0,cy,0),vec3(wi,hh,wi))); }',
    '    }',
    '    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }',

    # ARCH (3): monumental gateway, legs plunge, opening towers overhead
    '  else if(uScene<3.5){',
    '    float aCount=(uRecursive>.5)?5.:1.;',
    '    for(int ai=0;ai<5;ai++){ if(float(ai)>=aCount) break; float fai=float(ai);',
    '      float az=-4.-fai*5.0; float sc=clamp(1.-fai*.12,.4,1.);',
    '      float aR=3.0*sc; float aT=.72*sc; float legTop=5.0*sc; float legBot=-7.0;',
    '      float cy=(legTop+legBot)*.5; float hh=(legTop-legBot)*.5;',
    '      d=min(d,sdBox(pos-vec3(-aR,cy,az),vec3(aT,hh,aT)));',
    '      d=min(d,sdBox(pos-vec3( aR,cy,az),vec3(aT,hh,aT)));',
    '      vec3 qa=pos-vec3(0,legTop,az); vec2 qt=vec2(length(qa.xy)-aR,qa.z);',
    '      d=min(d,max(length(qt)-aT,-qa.y)); }',
    '    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }',

    # RUINS (4): Blade Runner 2049 Vegas — broken monoliths, leaning slab, collapsed arch, rubble
    '  else if(uScene<4.5){',
    '    if(uRecursive>.5){',
    '      float cell=4.0;',
    '      vec2 cid=floor((pos.xz+cell*.5)/cell);',
    '      vec3 q=vec3(mod(pos.x+cell*.5,cell)-cell*.5,pos.y,mod(pos.z+cell*.5,cell)-cell*.5);',
    '      float rv=h21(cid); float ty=h21(cid+vec2(7.3,2.1));',
    '      float rot=h21(cid+vec2(1.7,9.))*PI2; q.xz=r2(rot)*q.xz;',
    '      if(ty>.62){',
    '        d=min(d,sdBox(q-vec3((rv-.5)*1.2,.32,0.),vec3(.85,.32,.6)));',
    '      } else {',
    '        float top=mix(2.6,7.2,rv); float wi=mix(.55,1.05,h21(cid+vec2(3.1,4.4)));',
    '        float piece=sdBox(q-vec3(0,top*.5,0),vec3(wi,top*.5,wi*.72));',
    '        vec3 ct=q-vec3(0,top,0); ct.yz=r2(mix(-.6,.6,h21(cid+vec2(5.5,8.2))))*ct.yz;',
    '        piece=max(piece,-sdBox(ct,vec3(wi*1.7,wi*.55,wi*1.7)));',
    '        d=min(d,piece);',
    '      }',
    '    } else {',
    # monolith, broken top
    '      float base=sdBox(pos-vec3(0,.3,-2.),vec3(3.2,.3,1.8));',
    '      vec3 cq=pos-vec3(0,0,-1.15); float sp=1.25; float cid2=clamp(floor(cq.x/sp+.5),-2.,2.); vec3 cr=cq; cr.x-=sp*cid2;',
    '      base=min(base,sdBox(cr-vec3(0,2.0,0),vec3(.2,1.6,.2)));',
    '      base=min(base,sdBox(pos-vec3(0,2.0,-2.7),vec3(3.0,1.7,.25)));',
    '      base=min(base,sdBox(pos-vec3(0,3.85,-2.),vec3(3.05,.32,1.75)));',
    '      d=min(d,base);',
    '      vec3 Rb=vec3(1.7,1.35,1.55); vec3 bq=pos-vec3(.25,5.7,-2.);',
    '      float boulder=(length(bq/Rb)-1.0)*min(Rb.x,min(Rb.y,Rb.z));',
    '      vec3 brk=bq-vec3(.4,1.5,0.); brk.xy=r2(.5)*brk.xy; boulder=max(boulder,-sdBox(brk,vec3(2.2,.8,2.4)));',
    '      d=min(d,boulder);',
    '      for(int di=0;di<4;di++){ float fdi=float(di); vec2 hd=h22(vec2(fdi*7.1,3.));',
    '        vec3 dp=vec3((hd.x-.5)*5.5,5.6+hd.y*2.6+sin(uTime*uSpeed*aMult*.5+fdi)*.18,-2.+(hd.y-.5)*3.0);',
    '        vec3 qd=pos-dp; qd.xz=r2(uTime*uSpeed*aMult*.3+fdi)*qd.xz; qd.xy=r2(fdi*1.3)*qd.xy;',
    '        d=min(d,sdBox(qd,vec3(.2,.13,.16))); }',
    '      for(int pi=0;pi<3;pi++){ float fpi=float(pi)+1.; float pz=-1.-fpi*2.2;',
    '        d=min(d,sdBox(pos-vec3(fpi*2.4,2.6,pz),vec3(.06,2.6,.06)));',
    '        d=min(d,sdBox(pos-vec3(-fpi*2.4,2.6,pz-1.),vec3(.06,2.6,.06))); }'
    '    }',
    '    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }',

    # PLANET (5)
    '  else{',
    '    float b=1e9; float rot=uTime*uSpeed*aMult*.08;',
    '    vec3 pa=pos; pa.xz=r2(rot)*pa.xz;',
    '    b=smin(b,sdSphere(pa-vec3(-1.4,-.5,-.5),2.8),.65);',
    '    b=smin(b,sdSphere(pa-vec3(1.6,.9,.2),2.2),.65);',
    '    d=min(d,b);',
    '    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }',
    '}',

    'vec3 calcN(vec3 p){ vec2 e=vec2(1.,-1.)*.0015; return normalize(e.xyy*map(p+e.xyy)+e.yyx*map(p+e.yyx)+e.yxy*map(p+e.yxy)+e.xxx*map(p+e.xxx)); }',
    'float calcAO(vec3 p,vec3 n){ float o=0.,s=1.; for(int i=0;i<AOS;i++){ float h=.015+.16*float(i)/4.; o+=(h-map(p+n*h))*s; s*=.80; } return clamp(1.-2.4*o,0.,1.); }',
    'float shadow(vec3 p,vec3 L){ float res=1.,t=.05; for(int i=0;i<SHS;i++){ float h=map(p+L*t); if(h<.002) return 0.; res=min(res,9.*h/max(t,.0001)); t+=clamp(h,.06,.55); if(t>12.) break; } return clamp(res,0.,1.); }',

    'float colorPat(float val,vec3 p,float thick){',
    '  if(uPattern<.5) return smoothstep(.08,.96,val);',
    '  else if(uPattern<1.5) return fbm3(p*.8+vec3(0,0,uTime*uSpeed*.25));',
    '  else if(uPattern<2.5) return smoothstep(.2,.8,fbm3(p*.45));',
    '  else if(uPattern<3.5) return thick;',
    '  return .5+.5*sin((p.x+p.y)*2.2+fbm3(p*1.2)*5.8); }',

    'float surfTex(vec3 p){',
    '  if(uTexType<.5){ float sc=mix(2.6,1.0,uGrainSc); float g=vnoise(gl_FragCoord.xy/sc); return .6*g+.4*h21(gl_FragCoord.xy/max(sc*.6,1.)+fract(uTime)*vec2(13.,7.)); }',
    '  else if(uTexType<1.5) return .5+.5*sin((p.x+p.y-p.z)*mix(22.,46.,uGrainSc)+fbm3(p*6.5)*2.);',
    '  return .5+.5*sin(fbm3(p*5.0)*mix(12.,24.,uGrainSc)+(p.x+p.z)*mix(13.,26.,uGrainSc)); }',

    # background: coloured smog + light; forms stay grey so contrast reads
    'vec3 sceneBg(vec3 rd,vec3 ro,vec3 ta){',
    '  vec3 col;',
    '  if(uScene<.5){',
    '    float throat=pow(max(dot(rd,normalize(ta-ro)),0.),5.);',
    '    col=uFog*.03; col+=mix(uFog,uLight,.6)*throat*Bright*2.2; col+=uLight*pow(throat,14.)*Bright*1.6; }',
    '  else if(uScene<1.5){',
    '    float h=clamp(gl_FragCoord.y/uRes.y,0.,1.); col=mix(uFog*.12,uFog*mix(.5,1.5,uVoid),h); }',
    '  else if(uScene<4.5){',
    '    float hz=clamp(1.-rd.y*1.1,0.,1.4);',
    '    col=uFog*.04+uFog*hz*mix(.18,1.15,uVoid);',
    '    col+=uFog*max(-rd.y,0.)*mix(.1,.5,uVoid); }',
    '  else{ col=mix(uFog*.04,uMat*.05,clamp(rd.y*.5+.4,0.,1.)); }',
    '  vec3 sdir=normalize(vec3(cos(uAim+1.2),.16,sin(uAim+1.2)));',
    '  float sdt=dot(rd,sdir);',
    '  float disc=smoothstep(.9986,.9992,sdt);',
    '  float halo=pow(max(sdt,0.),140.);',
    '  float ring=smoothstep(.9976,.9981,sdt)-smoothstep(.9981,.9986,sdt);',
    '  col=mix(col,uLight*1.5*Bright,disc);',
    '  col+=uLight*halo*Bright*.45;',
    '  col+=mix(uLight,vec3(1),.5)*ring*Bright*.6;',
    '  return col; }',

    'void main(){',
    '  vec2 uv=(2.*gl_FragCoord.xy-uRes)/uRes.y;',
    '  float aim=uAim+mix(0.,sin(uTime*.12)*.35,uDrift);',

    '  vec3 ro,ta; float focal; float oAmp,pAmp,dAmp;',
    '  if(uScene<.5){       ro=vec3(.3,-.15,2.6);  ta=vec3(0,0,-5.);     focal=1.4;  oAmp=.10; pAmp=.04; dAmp=.07; }',
    '  else if(uScene<1.5){ ro=vec3(3.6,-.4,3.4);  ta=vec3(0,1.0,0);     focal=1.5;  oAmp=.22; pAmp=.05; dAmp=.05; }',
    '  else if(uScene<2.5){ ro=vec3(2.6,1.0,7.5);  ta=vec3(.3,9.0,-1.);  focal=1.4;  oAmp=.14; pAmp=.04; dAmp=.05; }',
    '  else if(uScene<3.5){ ro=vec3(.6,.2,6.8);    ta=vec3(0,5.5,-4.);   focal=1.4;  oAmp=.09; pAmp=.05; dAmp=.10; }',
    '  else if(uScene<4.5){ ro=vec3(.8,2.6,12.5); ta=vec3(0,3.8,-2.); focal=1.25; oAmp=.10; pAmp=.03; dAmp=.05; }',
    '  else{                ro=vec3(0,1.8,6.5);    ta=vec3(0,0,0);       focal=1.55; oAmp=.20; pAmp=.06; dAmp=.04; }',

    '  float ct=uTime*uSpeed*mix(.25,1.,uActive);',
    '  vec3 rel=ro-ta;',
    '  rel.xz=r2(sin(ct*1.0)*oAmp)*rel.xz;',
    '  rel.yz=r2(sin(ct*0.8)*pAmp)*rel.yz;',
    '  rel*=1.0+sin(ct*0.6)*dAmp;',
    '  rel.xz=r2(uYaw)*rel.xz; rel.yz=r2(uTilt)*rel.yz;',
    '  ro=ta+rel;',
    '  ro=mix(ro,ta,clamp((uZoom-.5)*.85,-.6,.78));',
    '  vec3 ww=normalize(ta-ro); vec3 uu=normalize(cross(ww,vec3(0,1,0))); vec3 vv=normalize(cross(uu,ww));',
    '  vec3 rd=normalize(uv.x*uu+uv.y*vv+focal*ww);',
    '  Lvec=normalize(vec3(cos(aim),.52,sin(aim)));',

    '  float breathe=mix(1.,.64+.36*sin(uTime*.62),uBreathe);',
    '  float nf=vnoise(vec2(uTime*2.4,7.3)); float sput=step(.965,h11(floor(uTime*9.)));',
    '  float flick=mix(1.,clamp(1.-smoothstep(.62,.97,nf)*.5-sput*.4,.2,1.),uFlicker);',
    '  float pulse=mix(1.,.85+.4*pow(.5+.5*sin(uTime*1.7),4.),uPulse);',
    '  Bright=clamp(uGlow*breathe*flick*pulse,0.,2.6);',

    '  vec3 sky=sceneBg(rd,ro,ta); vec3 col=sky;',

    '  float tiled=((uScene>1.5&&uScene<2.5)||(uScene>3.5&&uScene<4.5))?step(.5,uRecursive):0.;',
    '  float cellMax=(tiled>.5)?1.1:9e9;',
    '  float t=0.; float tmax=42.; float hit=0.;',
    '  for(int i=0;i<MAXS;i++){',
    '    vec3 p=ro+rd*t; float d=map(p);',
    '    if(d<.001*t+.0008){ hit=1.; break; }',
    '    t+=min(d*.55,cellMax); if(t>tmax) break; }',

    '  vec3 sprC=mix(uLight,vec3(1),.4)+vec3(.05,.02,0.);',
    '  vec3 ambCol=mix(uFog,uMat,.55);',

    '  if(hit>.5){',
    '    vec3 p=ro+rd*t; vec3 n=calcN(p);',
    '    float ao=calcAO(p,n); float sh=shadow(p+n*.025,Lvec);',
    '    float dif=clamp(dot(n,Lvec),0.,1.); float key=dif*sh;',
    '    float fres=pow(clamp(1.+dot(rd,n),0.,1.),3.2);',
    '    vec3 H=normalize(Lvec-rd); float spec=pow(clamp(dot(n,H),0.,1.),48.);',
    '    float thick=clamp(-map(p-rd*.55)/.55,0.,1.);',
    '    float val=(.06+key*Bright)*ao; val=clamp(val,0.,1.6);',
    '    float pf=clamp(colorPat(val,p,thick),0.,1.);',
    '    vec3 albedo=mix(uMat*mix(.5,1.08,pf),uLight,pf*.08);',
    '    vec3 c=albedo*.05+ambCol*.10*ao;',                       # ambient (reddish-grey fill)
    '    c+=albedo*uLight*key*Bright*1.2;',                       # key light (light colour)
    '    c+=spec*uLight*Bright*.55;',                             # specular
    '    c+=fres*mix(uMat,uLight,.5)*Bright*.15;',                # rim
    '    c+=uInner*thick*mix(uMat,uLight,.4)*(.4+.5*Bright);',    # inner glow
    '    if(uScene<.5){ vec3 td=normalize(ta-ro); c+=mix(uFog,uLight,.5)*max(dot(n,-td),0.)*exp(-length(p)*.5)*Bright*.9; }',
    # ember sprites: warm point light on surface
    '    for(int si=0;si<8;si++){',
    '      if(float(si)>=uSprCount) break;',
    '      vec4 spr=uSpr[si]; if(spr.w<1.) continue;',
    '      vec3 toS=spr.xyz-p; float dist2=dot(toS,toS);',
    '      c+=sprC*(spr.w-.8)*exp(-dist2*2.8)*max(dot(n,normalize(toS)),0.)*2.2; }',
    # coarse, distance-faded grain (no per-pixel speckle)
    '    if(uTexType>2.5){ c*=mix(.3,1.0,cellEdge(p)); }',
    '    else { float tv=surfTex(p); c+=(tv-.5)*clamp(uGrain,0.,1.)*.55*(.25+.8*clamp(val,0.,1.)); }',
    # fog: distance + height (bases dissolve into smog)
    '    float fogK=mix(.02,.085,uVoid);',
    '    float fDist=1.-exp(-max(t-3.0,0.)*fogK);',
    '    float ground=(uScene>1.5&&uScene<4.5)?1.:0.;',
    '    float fH=ground*smoothstep(3.0,-5.0,p.y);',
    '    float hStr=clamp(uVoid*1.3,0.,1.);',
    '    float fog=1.-(1.-fDist)*(1.-fH*hStr);',
    '    col=mix(c,sky,fog); }',

    # wisps (elongated, never spheres)
    '  for(int si=0;si<8;si++){',
    '    if(float(si)>=uSprCount) break;',
    '    vec4 spr=uSpr[si]; float sbr=spr.w; if(sbr<.005) continue;',
    '    vec4 spd=uSprD[si];',
    '    col+=sprC*wispGlow(ro,rd,spr.xyz,spd.xyz,spd.w,min(sbr,.92),t,hit); }',

    '  col+=(h21(gl_FragCoord.xy*.5+fract(uTime)*vec2(31,17))-.5)*.012;',
    '  float vig=1.-.30*pow(length(uv)*.55,2.2); col*=clamp(vig,0.,1.);',
    '  gl_FragColor=vec4(clamp(col,0.,1.),1.); }',
]

JS = r"""
(function(){
"use strict";
var canvas=document.getElementById('gl'),fallback=document.getElementById('fallback'),fmsg=document.getElementById('fmsg');
function fail(m){canvas.style.display='none';fallback.style.display='flex';if(m)fmsg.textContent=m;console.error('[fw] '+m);}
var gl=canvas.getContext('webgl',{antialias:false,alpha:false,premultipliedAlpha:false,powerPreference:'high-performance'})
    ||canvas.getContext('experimental-webgl');
if(!gl){fail('WebGL unavailable.');return;}

var VERT='attribute vec2 aPos; void main(){ gl_Position=vec4(aPos,0.,1.); }';
var FRAG=FRAG_PLACEHOLDER;

function compile(type,src){
  var s=gl.createShader(type); gl.shaderSource(s,src); gl.compileShader(s);
  if(!gl.getShaderParameter(s,gl.COMPILE_STATUS)){
    var log=gl.getShaderInfoLog(s)||'';
    console.error('[fw] compile:\n'+log);
    console.error(src.split('\n').map(function(l,i){return (i+1)+': '+l;}).join('\n'));
    gl.deleteShader(s); return {err:(log.split('\n').filter(Boolean)[0]||'compile error')};
  }
  return {sh:s};
}
var vr=compile(gl.VERTEX_SHADER,VERT), fr=compile(gl.FRAGMENT_SHADER,FRAG);
if(vr.err||fr.err){fail('Shader failed: '+(fr.err||vr.err));return;}
var prog=gl.createProgram();
gl.attachShader(prog,vr.sh); gl.attachShader(prog,fr.sh);
gl.bindAttribLocation(prog,0,'aPos'); gl.linkProgram(prog);
if(!gl.getProgramParameter(prog,gl.LINK_STATUS)){fail('Link: '+(gl.getProgramInfoLog(prog)||'').split('\n')[0]);return;}
gl.useProgram(prog);
var buf=gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER,buf);
gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),gl.STATIC_DRAW);
gl.enableVertexAttribArray(0); gl.vertexAttribPointer(0,2,gl.FLOAT,false,0,0);

var U={};
['uRes','uTime','uSpeed','uSeed','uGrain','uGlow','uAim','uVoid','uPattern','uZoom','uCount',
 'uYaw','uTilt','uInner','uTexType','uScene','uActive','uRecursive','uSprCount','uSprMode',
 'uMat','uFog','uLight','uBreathe','uFlicker','uDrift','uPulse','uSpr','uSprD','uGrit','uCoarse','uGrainSc'].forEach(function(n){U[n]=gl.getUniformLocation(prog,n);});

function hex2rgb(h){h=h.replace('#','');return [parseInt(h.substr(0,2),16)/255,parseInt(h.substr(2,2),16)/255,parseInt(h.substr(4,2),16)/255];}
function rnd(a,b){return a+Math.random()*(b-a);}

var SPRN=8;
var sprArr=new Float32Array(SPRN*4);
var sprDArr=new Float32Array(SPRN*4);
var sprs=[];
function initSprites(){
  sprs=[];
  for(var i=0;i<SPRN;i++){
    sprs.push({
      p:[rnd(-2.4,2.4),rnd(-1.4,1.4),rnd(-2.4,2.4)],
      v:[(Math.random()-.5)*.3,(Math.random()-.5)*.12,(Math.random()-.5)*.3],
      w:rnd(.4,.75), phase:Math.random()*6.28, state:0, timer:rnd(1,5), target:[0,0,0]
    });
  }
}
initSprites();

function updateSprites(dt,st){
  var actMult=state.active>.5?1.0:.18;
  var sprMode=state.sprMode;
  var count=Math.ceil(state.sprCount);
  var box=[2.8,1.8,2.8];
  for(var i=0;i<SPRN;i++){
    var s=sprs[i];
    if(i>=count){ sprArr[i*4+3]=0; sprDArr[i*4+3]=0; continue; }
    s.timer-=dt;
    var pulse=.5+.38*Math.sin(s.phase+st*1.3);
    if(s.state===0){
      for(var j=0;j<3;j++){ s.v[j]+=(Math.random()-.5)*.65*dt; s.v[j]*=Math.max(0,1-1.4*dt); }
      var spd=Math.sqrt(s.v[0]*s.v[0]+s.v[1]*s.v[1]+s.v[2]*s.v[2]); var baseSpd=.32*actMult;
      if(spd>.001){var sc=baseSpd/spd; for(var j=0;j<3;j++)s.v[j]*=sc;}
      for(var j=0;j<3;j++)s.p[j]+=s.v[j]*dt;
      for(var j=0;j<3;j++){if(s.p[j]>box[j]){s.p[j]=box[j];s.v[j]=-Math.abs(s.v[j]);}if(s.p[j]<-box[j]){s.p[j]=-box[j];s.v[j]=Math.abs(s.v[j]);}}
      s.w=pulse*.72;
      if(sprMode>=1&&s.timer<0){s.state=1;s.timer=rnd(1.5,4);s.target=[rnd(-1.8,1.8),rnd(-.9,.9),rnd(-1.8,1.8)];}
    } else if(s.state===1){
      var dx=s.target[0]-s.p[0],dy=s.target[1]-s.p[1],dz=s.target[2]-s.p[2];
      var dd=Math.sqrt(dx*dx+dy*dy+dz*dz)+1e-4; var sk=.85*actMult;
      s.v[0]=dx/dd*sk; s.v[1]=dy/dd*sk*.6; s.v[2]=dz/dd*sk;
      for(var j=0;j<3;j++)s.p[j]+=s.v[j]*dt;
      s.w=pulse*.88;
      if(dd<.38||(s.timer<0&&dd<1.6)){ s.state=2; s.timer=rnd(2,6.5); for(var j=0;j<3;j++)s.p[j]=s.target[j]; }
      else if(s.timer<0){s.state=0;s.timer=rnd(1,4);}
    } else {
      s.w=1.0+(.9+.5*Math.sin(s.phase+st*2.9))*(sprMode>=2?1.4:.75);
      if(s.timer<0){ s.state=0;s.w=pulse*.72;s.timer=rnd(1.5,5);
        var ang=Math.random()*6.28; s.v[0]=Math.cos(ang)*.36*actMult; s.v[1]=.08; s.v[2]=Math.sin(ang)*.36*actMult; }
    }
    if(sprMode===2&&s.state===0&&s.timer<0){ s.state=1;s.timer=rnd(.5,2);s.target=[rnd(-1.5,1.5),rnd(-.8,.8),rnd(-1.5,1.5)]; }
    sprArr[i*4]=s.p[0]; sprArr[i*4+1]=s.p[1]; sprArr[i*4+2]=s.p[2]; sprArr[i*4+3]=Math.max(0,s.w);
    var spd=Math.sqrt(s.v[0]*s.v[0]+s.v[1]*s.v[1]+s.v[2]*s.v[2]); var wlen;
    if(s.state===2){
      wlen=.09;
      var prevLen=Math.sqrt(sprDArr[i*4]*sprDArr[i*4]+sprDArr[i*4+1]*sprDArr[i*4+1]+sprDArr[i*4+2]*sprDArr[i*4+2]);
      if(prevLen<.001){sprDArr[i*4]=1;sprDArr[i*4+1]=0;sprDArr[i*4+2]=0;}
    } else {
      wlen=Math.min(.12+spd*.55,s.state===1?.5:.35);
      if(spd>.001){ sprDArr[i*4]=s.v[0]/spd; sprDArr[i*4+1]=s.v[1]/spd; sprDArr[i*4+2]=s.v[2]/spd; }
    }
    sprDArr[i*4+3]=wlen;
  }
}

var state={
  scene:1,speed:.28,grain:.5,glow:1.2,aim:2.2,void:.5,pattern:0,texType:0,inner:.3,grit:1.0,coarse:.5,grainSc:.5,zoom:.5,count:5,yaw:0,tilt:0,
  active:0,recursive:0,sprCount:4,sprMode:0,
  seed:Math.random()*100, mat:hex2rgb('#9b958c'), fog:hex2rgb('#cc1a0a'), light:hex2rgb('#ffd2b0'),
  lite:{breathe:1,flicker:0,drift:1,pulse:0}
};

var reduced=window.matchMedia&&window.matchMedia('(prefers-reduced-motion:reduce)').matches;
var DPRCAP=2,t0=performance.now(),simTime=reduced?14:0,running=true,needsDraw=true,last=performance.now();

function resize(){
  var dpr=Math.min(window.devicePixelRatio||1,DPRCAP);
  var w=Math.max(1,Math.floor(canvas.clientWidth*dpr)),h=Math.max(1,Math.floor(canvas.clientHeight*dpr));
  if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;}
  gl.viewport(0,0,canvas.width,canvas.height); needsDraw=true;
}
window.addEventListener('resize',resize);

function draw(now){
  if(!reduced) simTime=(now-t0)/1000;
  gl.uniform2f(U.uRes,canvas.width,canvas.height);
  gl.uniform1f(U.uTime,simTime); gl.uniform1f(U.uSeed,state.seed);
  gl.uniform1f(U.uSpeed,state.speed*2.2); gl.uniform1f(U.uGrain,state.grain);
  gl.uniform1f(U.uGlow,state.glow); gl.uniform1f(U.uAim,state.aim);
  gl.uniform1f(U.uVoid,state.void); gl.uniform1f(U.uPattern,state.pattern);
  gl.uniform1f(U.uTexType,state.texType); gl.uniform1f(U.uInner,state.inner);
  gl.uniform1f(U.uGrit,state.grit); gl.uniform1f(U.uCoarse,state.coarse); gl.uniform1f(U.uGrainSc,state.grainSc);
  gl.uniform1f(U.uScene,state.scene); gl.uniform1f(U.uZoom,state.zoom);
  gl.uniform1f(U.uCount,state.count); gl.uniform1f(U.uYaw,state.yaw); gl.uniform1f(U.uTilt,state.tilt);
  gl.uniform1f(U.uActive,state.active); gl.uniform1f(U.uRecursive,state.recursive);
  gl.uniform1f(U.uSprCount,state.sprCount); gl.uniform1f(U.uSprMode,state.sprMode);
  gl.uniform3f(U.uMat,state.mat[0],state.mat[1],state.mat[2]);
  gl.uniform3f(U.uFog,state.fog[0],state.fog[1],state.fog[2]);
  gl.uniform3f(U.uLight,state.light[0],state.light[1],state.light[2]);
  gl.uniform1f(U.uBreathe,state.lite.breathe); gl.uniform1f(U.uFlicker,state.lite.flicker);
  gl.uniform1f(U.uDrift,state.lite.drift); gl.uniform1f(U.uPulse,state.lite.pulse);
  gl.uniform4fv(U.uSpr,sprArr); gl.uniform4fv(U.uSprD,sprDArr);
  gl.drawArrays(gl.TRIANGLES,0,3);
}
function frame(now){
  var dt=Math.min((now-last)/1000,.05);
  if(running){if(!reduced){updateSprites(dt,simTime);draw(now);}else if(needsDraw){draw(now);needsDraw=false;}}
  last=now; requestAnimationFrame(frame);
}
resize(); draw(performance.now()); requestAnimationFrame(frame);

if('IntersectionObserver' in window)
  new IntersectionObserver(function(es){running=es[0].isIntersecting;
    if(running&&!reduced){t0=performance.now()-simTime*1000;last=performance.now();}
    if(running)needsDraw=true;},{threshold:.01}).observe(canvas);

function kick(){needsDraw=true;}
var panel=document.getElementById('panel'),hint=document.getElementById('hint');
function singleSel(id,attr,setter){
  document.getElementById(id).addEventListener('click',function(e){
    var b=e.target.closest('.chip'); if(!b) return;
    setter(parseInt(b.getAttribute(attr),10));
    Array.prototype.forEach.call(this.querySelectorAll('.chip'),function(c){c.setAttribute('aria-pressed',c===b?'true':'false');});
    kick();
  });
}
singleSel('scene','data-scn',function(v){state.scene=v;});
singleSel('pattern','data-pat',function(v){state.pattern=v;});
singleSel('texture','data-tex',function(v){state.texType=v;});
singleSel('motion','data-mot',function(v){state.active=v;});
singleSel('recurse','data-rec',function(v){state.recursive=v;});
singleSel('sprbeh','data-spb',function(v){state.sprMode=v;});
document.getElementById('light').addEventListener('click',function(e){
  var b=e.target.closest('.chip'); if(!b) return;
  var k=b.getAttribute('data-lite'); state.lite[k]=state.lite[k]?0:1;
  b.setAttribute('aria-pressed',state.lite[k]?'true':'false'); kick();
});
function bindSlider(id,prop,asInt){
  var el=document.getElementById(id);
  el.addEventListener('input',function(){state[prop]=asInt?parseInt(el.value,10):parseFloat(el.value);kick();});
  return el;
}
var zoomEl=bindSlider('zoom','zoom'),yawEl=bindSlider('yaw','yaw'),tiltEl=bindSlider('tilt','tilt');
bindSlider('count','count',true); bindSlider('sprCount','sprCount',true);
['aim','glow','grain','inner','void','speed','grit','coarse','grainSc'].forEach(function(id){bindSlider(id,id);});
document.getElementById('matCol').addEventListener('input',function(){state.mat=hex2rgb(this.value);kick();});
document.getElementById('fogCol').addEventListener('input',function(){state.fog=hex2rgb(this.value);kick();});
document.getElementById('lightCol').addEventListener('input',function(){state.light=hex2rgb(this.value);kick();});
document.getElementById('reseed').addEventListener('click',function(){state.seed=Math.random()*100;initSprites();kick();});
document.getElementById('hide').addEventListener('click',function(){panel.classList.add('hidden');});

var pts={},dragging=false,moved=false,sx=0,sy=0,syaw=0,stilt=0,pinchD=0,zoomS=0;
function pc(){var n=0;for(var k in pts)n++;return n;}
function pd2(){var a=[];for(var k in pts)a.push(pts[k]);return Math.hypot(a[0].x-a[1].x,a[0].y-a[1].y);}
canvas.addEventListener('pointerdown',function(e){
  pts[e.pointerId]={x:e.clientX,y:e.clientY};
  try{canvas.setPointerCapture(e.pointerId);}catch(err){}
  if(pc()===1){dragging=true;moved=false;sx=e.clientX;sy=e.clientY;syaw=state.yaw;stilt=state.tilt;}
  else if(pc()===2){dragging=false;pinchD=pd2();zoomS=state.zoom;}
});
canvas.addEventListener('pointermove',function(e){
  if(!pts[e.pointerId])return; pts[e.pointerId]={x:e.clientX,y:e.clientY};
  if(pc()>=2){var d=pd2();if(pinchD>0){var z=zoomS+(d-pinchD)/(window.innerWidth*.6);state.zoom=Math.min(1,Math.max(0,z));zoomEl.value=state.zoom;kick();}return;}
  if(dragging){var dx=e.clientX-sx,dy=e.clientY-sy;if(Math.abs(dx)+Math.abs(dy)>8)moved=true;
    if(moved){state.yaw=Math.min(1.4,Math.max(-1.4,syaw+dx/(window.innerWidth*.5)*1.4));yawEl.value=state.yaw;
      state.tilt=Math.min(1,Math.max(-1,stilt-dy/(window.innerHeight*.6)*1.0));tiltEl.value=state.tilt;kick();}}
});
function up(e){delete pts[e.pointerId];if(pc()<2)pinchD=0;
  if(pc()===0){if(dragging&&!moved)panel.classList.toggle('hidden');dragging=false;}}
canvas.addEventListener('pointerup',up);
canvas.addEventListener('pointercancel',function(e){delete pts[e.pointerId];dragging=false;});
setTimeout(function(){hint.classList.add('gone');},3600);
})();
"""

frag_str = '\n'.join(FRAG_LINES)
frag_js = "'" + frag_str.replace('\\','\\\\').replace("'","\\'").replace('\n','\\n') + "'"
js = JS.replace('FRAG_PLACEHOLDER', frag_js)

html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover">
<title>Form-World</title>
<style>
{CSS}
</style>
</head>
<body>
{HTML_BODY}
<script>
{js}
</script>
</body>
</html>"""

with open('/mnt/user-data/outputs/form-world.html','w') as f:
    f.write(html)
print(f"written: {len(html)} bytes")
