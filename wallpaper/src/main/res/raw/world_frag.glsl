#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform vec2 uRes;
uniform float uTime,uSpeed,uSeed,uGrain,uGlow,uAim,uVoid,uPattern,uZoom,uCount,uYaw,uTilt,uInner,uTexType,uScene,uGrit,uCoarse,uGrainSc;
uniform float uActive,uRecursive,uSprCount,uSprMode;
uniform vec3 uMat,uFog,uLight;
uniform float uBreathe,uFlicker,uDrift,uPulse;
uniform vec4 uSpr[8];
uniform vec4 uSprD[8];
const int MAXS=96; const int SHS=12; const int AOS=5;
const float PI2=6.28318530718;
vec3 Lvec; float Bright;
float h11(float p){ p=fract(p*.1031); p*=p+33.33; p*=p+p; return fract(p); }
float h21(vec2 p){ vec3 q=fract(vec3(p.xyx)*.1031); q+=dot(q,q.yzx+33.33); return fract((q.x+q.y)*q.z); }
vec2 h22(vec2 p){ vec3 q=fract(vec3(p.xyx)*vec3(.1031,.1030,.0973)); q+=dot(q,q.yzx+33.33); return fract((q.xx+q.yz)*q.zy); }
float vnoise(vec2 p){ vec2 i=floor(p),f=fract(p); f=f*f*(3.-2.*f); float a=h21(i),b=h21(i+vec2(1,0)),c=h21(i+vec2(0,1)),d=h21(i+vec2(1,1)); return mix(mix(a,b,f.x),mix(c,d,f.x),f.y); }
float n3(vec3 x){ vec3 p=floor(x),f=fract(x); f=f*f*(3.-2.*f); float n=p.x+p.y*57.+113.*p.z; float a=h11(n),b=h11(n+1.),c=h11(n+57.),d=h11(n+58.),e=h11(n+113.),f1=h11(n+114.),g=h11(n+170.),hh=h11(n+171.); return mix(mix(mix(a,b,f.x),mix(c,d,f.x),f.y),mix(mix(e,f1,f.x),mix(g,hh,f.x),f.y),f.z); }
float fbm3(vec3 p){ float v=0.,a=.5; for(int i=0;i<3;i++){ v+=a*n3(p); p*=2.02; a*=.5; } return v; }
mat2 r2(float a){ float c=cos(a),s=sin(a); return mat2(c,-s,s,c); }
float smin(float a,float b,float k){ float h=clamp(.5+.5*(b-a)/k,0.,1.); return mix(b,a,h)-k*h*(1.-h); }
float sdBox(vec3 p,vec3 b){ vec3 d=abs(p)-b; return length(max(d,0.))+min(max(d.x,max(d.y,d.z)),0.); }
float sdTorusZ(vec3 p,vec2 t){ vec2 q=vec2(length(p.xy)-t.x,p.z); return length(q)-t.y; }
float sdSphere(vec3 p,float r){ return length(p)-r; }
float sdHelix(vec3 p,float R,float pitch,float rt){ float a=atan(p.z,p.x+1e-5); float c=pitch/PI2; float yy=p.y-c*a; yy-=pitch*floor(yy/pitch+.5); return (length(vec2(length(p.xz)-R,yy))-rt)*.5; }
float facetField(vec3 p){ return fbm3(p*mix(.7,2.0,uCoarse)); }
float soil(vec3 p){
  if(uTexType>2.5){ float q=floor(facetField(p)*5.)/5.; return (q-.5)*.13*uGrit; }
  float fs=mix(.65,1.6,uCoarse); float v=n3(p*11.*fs)*.05+n3(p*27.*fs)*.022+n3(p*60.*fs)*.011-.045; return v*uGrit; }
float cellEdge(vec3 p){ float e=fract(facetField(p)*5.); e=min(e,1.-e); return smoothstep(0.0,.09,e); }
float wispGlow(vec3 ro,vec3 rd,vec3 pos,vec3 wdir,float wlen,float br,float thit,float hit){
  if(br<.005) return 0.; float wRad=.040; float g=0.;
  for(int wi=0;wi<2;wi++){
    float wt=float(wi)*.65+.18;
    vec3 wp=pos+wdir*(wt-.5)*wlen;
    vec3 oc=ro-wp; float bdot=dot(rd,oc);
    float s=-bdot; if(s<.001) continue;
    if(hit>.5&&s>thit) continue;
    float dist2=max(dot(oc,oc)-bdot*bdot,0.);
    float endFade=(wi==1)?.72:.38;
    g+=exp(-dist2/(wRad*wRad))*br*endFade; }
  return g; }
float map(vec3 pos){
  float d=1e9; float aMult=mix(.15,1.,uActive);
  if(uScene<.5){
    float bowlMax=(uRecursive>.5)?28.:(uCount+1.);
    for(int i=0;i<28;i++){ if(float(i)>=bowlMax) break; float fi=float(i);
      float R=max(2.2-fi*.25,.04); float tu=max(.38-fi*.018,.04);
      float spi=fi*.55+uTime*uSpeed*aMult*(.28+fi*.055);
      float wamp=mix(.0,.08,uActive)*fi*.04;
      vec3 rp=pos-vec3(sin(spi)*wamp,cos(spi*.7)*wamp,-fi*.72);
      d=min(d,sdTorusZ(rp,vec2(R,tu))); }
    return d; }
  if(uScene<1.5){
    float H=mix(3.,7.,clamp((uCount-1.)/6.,0.,1.));
    vec3 ph=pos; ph.xz=r2(uTime*uSpeed*.06*aMult)*ph.xz; ph.y-=uTime*uSpeed*.28*aMult;
    float hd=sdHelix(ph,2.1,1.45,.52);
    d=min(d,(uRecursive>.5)?hd:max(hd,abs(ph.y)-H)); 
    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }
  else if(uScene<2.5){
    if(uRecursive>.5){
      float cell=3.4;
      vec2 cid=floor((pos.xz+cell*.5)/cell);
      vec3 q=vec3(mod(pos.x+cell*.5,cell)-cell*.5,pos.y,mod(pos.z+cell*.5,cell)-cell*.5);
      float rv=h21(cid); float top=mix(6.,13.,rv); float wi=mix(.45,.95,h21(cid+vec2(3.7,21.)));
      float bot=-10.; float cy=(top+bot)*.5; float hh=(top-bot)*.5;
      float k=mix(.3,1.1,h21(cid+vec2(1.1,5.5)));
      float sway=sin(uTime*uSpeed*aMult*.11+rv*6.28)*.04;
      q.xz=r2(k*(q.y/max(top,1.))+sway)*q.xz;
      d=min(d,sdBox(q-vec3(0,cy,0),vec3(wi,hh,wi)));
    } else {
      for(int i=0;i<4;i++){ if(float(i)>=uCount) break; float fi=float(i);
        vec2 rv=h22(vec2(uSeed+fi*3.7,21.));
        float xi=(i==0)?.6:(rv.x-.5)*8.; float zi=(i==0)?0.:-fi*3.2-2.;
        float wi=(i==0)?1.0:max(.7-fi*.09,.25); float top=(i==0)?13.:max(10.-fi*1.5,3.5);
        float bot=-7.; float cy=(top+bot)*.5; float hh=(top-bot)*.5;
        vec3 q=pos-vec3(xi,0.,zi);
        float sway=sin(uTime*uSpeed*aMult*.11+rv.x*6.28)*.03;
        q.xz=r2(mix(.4,1.2,rv.x)*(q.y/max(top,1.))+sway)*q.xz;
        d=min(d,sdBox(q-vec3(0,cy,0),vec3(wi,hh,wi))); }
    }
    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }
  else if(uScene<3.5){
    float aCount=(uRecursive>.5)?5.:1.;
    for(int ai=0;ai<5;ai++){ if(float(ai)>=aCount) break; float fai=float(ai);
      float az=-4.-fai*5.0; float sc=clamp(1.-fai*.12,.4,1.);
      float aR=3.0*sc; float aT=.72*sc; float legTop=5.0*sc; float legBot=-7.0;
      float cy=(legTop+legBot)*.5; float hh=(legTop-legBot)*.5;
      d=min(d,sdBox(pos-vec3(-aR,cy,az),vec3(aT,hh,aT)));
      d=min(d,sdBox(pos-vec3( aR,cy,az),vec3(aT,hh,aT)));
      vec3 qa=pos-vec3(0,legTop,az); vec2 qt=vec2(length(qa.xy)-aR,qa.z);
      d=min(d,max(length(qt)-aT,-qa.y)); }
    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }
  else if(uScene<4.5){
    if(uRecursive>.5){
      float cell=4.0;
      vec2 cid=floor((pos.xz+cell*.5)/cell);
      vec3 q=vec3(mod(pos.x+cell*.5,cell)-cell*.5,pos.y,mod(pos.z+cell*.5,cell)-cell*.5);
      float rv=h21(cid); float ty=h21(cid+vec2(7.3,2.1));
      float rot=h21(cid+vec2(1.7,9.))*PI2; q.xz=r2(rot)*q.xz;
      if(ty>.62){
        d=min(d,sdBox(q-vec3((rv-.5)*1.2,.32,0.),vec3(.85,.32,.6)));
      } else {
        float top=mix(2.6,7.2,rv); float wi=mix(.55,1.05,h21(cid+vec2(3.1,4.4)));
        float piece=sdBox(q-vec3(0,top*.5,0),vec3(wi,top*.5,wi*.72));
        vec3 ct=q-vec3(0,top,0); ct.yz=r2(mix(-.6,.6,h21(cid+vec2(5.5,8.2))))*ct.yz;
        piece=max(piece,-sdBox(ct,vec3(wi*1.7,wi*.55,wi*1.7)));
        d=min(d,piece);
      }
    } else {
      float base=sdBox(pos-vec3(0,.3,-2.),vec3(3.2,.3,1.8));
      vec3 cq=pos-vec3(0,0,-1.15); float sp=1.25; float cid2=clamp(floor(cq.x/sp+.5),-2.,2.); vec3 cr=cq; cr.x-=sp*cid2;
      base=min(base,sdBox(cr-vec3(0,2.0,0),vec3(.2,1.6,.2)));
      base=min(base,sdBox(pos-vec3(0,2.0,-2.7),vec3(3.0,1.7,.25)));
      base=min(base,sdBox(pos-vec3(0,3.85,-2.),vec3(3.05,.32,1.75)));
      d=min(d,base);
      vec3 Rb=vec3(1.7,1.35,1.55); vec3 bq=pos-vec3(.25,5.7,-2.);
      float boulder=(length(bq/Rb)-1.0)*min(Rb.x,min(Rb.y,Rb.z));
      vec3 brk=bq-vec3(.4,1.5,0.); brk.xy=r2(.5)*brk.xy; boulder=max(boulder,-sdBox(brk,vec3(2.2,.8,2.4)));
      d=min(d,boulder);
      for(int di=0;di<4;di++){ float fdi=float(di); vec2 hd=h22(vec2(fdi*7.1,3.));
        vec3 dp=vec3((hd.x-.5)*5.5,5.6+hd.y*2.6+sin(uTime*uSpeed*aMult*.5+fdi)*.18,-2.+(hd.y-.5)*3.0);
        vec3 qd=pos-dp; qd.xz=r2(uTime*uSpeed*aMult*.3+fdi)*qd.xz; qd.xy=r2(fdi*1.3)*qd.xy;
        d=min(d,sdBox(qd,vec3(.2,.13,.16))); }
      for(int pi=0;pi<3;pi++){ float fpi=float(pi)+1.; float pz=-1.-fpi*2.2;
        d=min(d,sdBox(pos-vec3(fpi*2.4,2.6,pz),vec3(.06,2.6,.06)));
        d=min(d,sdBox(pos-vec3(-fpi*2.4,2.6,pz-1.),vec3(.06,2.6,.06))); }    }
    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }
  else{
    float b=1e9; float rot=uTime*uSpeed*aMult*.08;
    vec3 pa=pos; pa.xz=r2(rot)*pa.xz;
    b=smin(b,sdSphere(pa-vec3(-1.4,-.5,-.5),2.8),.65);
    b=smin(b,sdSphere(pa-vec3(1.6,.9,.2),2.2),.65);
    d=min(d,b);
    d+=soil(pos)*smoothstep(.42,0.,abs(d)); return d; }
}
vec3 calcN(vec3 p){ vec2 e=vec2(1.,-1.)*.0015; return normalize(e.xyy*map(p+e.xyy)+e.yyx*map(p+e.yyx)+e.yxy*map(p+e.yxy)+e.xxx*map(p+e.xxx)); }
float calcAO(vec3 p,vec3 n){ float o=0.,s=1.; for(int i=0;i<AOS;i++){ float h=.015+.16*float(i)/4.; o+=(h-map(p+n*h))*s; s*=.80; } return clamp(1.-2.4*o,0.,1.); }
float shadow(vec3 p,vec3 L){ float res=1.,t=.05; for(int i=0;i<SHS;i++){ float h=map(p+L*t); if(h<.002) return 0.; res=min(res,9.*h/max(t,.0001)); t+=clamp(h,.06,.55); if(t>12.) break; } return clamp(res,0.,1.); }
float colorPat(float val,vec3 p,float thick){
  if(uPattern<.5) return smoothstep(.08,.96,val);
  else if(uPattern<1.5) return fbm3(p*.8+vec3(0,0,uTime*uSpeed*.25));
  else if(uPattern<2.5) return smoothstep(.2,.8,fbm3(p*.45));
  else if(uPattern<3.5) return thick;
  return .5+.5*sin((p.x+p.y)*2.2+fbm3(p*1.2)*5.8); }
float surfTex(vec3 p){
  if(uTexType<.5){ float sc=mix(2.6,1.0,uGrainSc); float g=vnoise(gl_FragCoord.xy/sc); return .6*g+.4*h21(gl_FragCoord.xy/max(sc*.6,1.)+fract(uTime)*vec2(13.,7.)); }
  else if(uTexType<1.5) return .5+.5*sin((p.x+p.y-p.z)*mix(22.,46.,uGrainSc)+fbm3(p*6.5)*2.);
  return .5+.5*sin(fbm3(p*5.0)*mix(12.,24.,uGrainSc)+(p.x+p.z)*mix(13.,26.,uGrainSc)); }
vec3 sceneBg(vec3 rd,vec3 ro,vec3 ta){
  vec3 col;
  if(uScene<.5){
    float throat=pow(max(dot(rd,normalize(ta-ro)),0.),5.);
    col=uFog*.03; col+=mix(uFog,uLight,.6)*throat*Bright*2.2; col+=uLight*pow(throat,14.)*Bright*1.6; }
  else if(uScene<1.5){
    float h=clamp(gl_FragCoord.y/uRes.y,0.,1.); col=mix(uFog*.12,uFog*mix(.5,1.5,uVoid),h); }
  else if(uScene<4.5){
    float hz=clamp(1.-rd.y*1.1,0.,1.4);
    col=uFog*.04+uFog*hz*mix(.18,1.15,uVoid);
    col+=uFog*max(-rd.y,0.)*mix(.1,.5,uVoid); }
  else{ col=mix(uFog*.04,uMat*.05,clamp(rd.y*.5+.4,0.,1.)); }
  vec3 sdir=normalize(vec3(cos(uAim+1.2),.16,sin(uAim+1.2)));
  float sdt=dot(rd,sdir);
  float disc=smoothstep(.9986,.9992,sdt);
  float halo=pow(max(sdt,0.),140.);
  float ring=smoothstep(.9976,.9981,sdt)-smoothstep(.9981,.9986,sdt);
  col=mix(col,uLight*1.5*Bright,disc);
  col+=uLight*halo*Bright*.45;
  col+=mix(uLight,vec3(1),.5)*ring*Bright*.6;
  return col; }
void main(){
  vec2 uv=(2.*gl_FragCoord.xy-uRes)/uRes.y;
  float aim=uAim+mix(0.,sin(uTime*.12)*.35,uDrift);
  vec3 ro,ta; float focal; float oAmp,pAmp,dAmp;
  if(uScene<.5){       ro=vec3(.3,-.15,2.6);  ta=vec3(0,0,-5.);     focal=1.4;  oAmp=.10; pAmp=.04; dAmp=.07; }
  else if(uScene<1.5){ ro=vec3(3.6,-.4,3.4);  ta=vec3(0,1.0,0);     focal=1.5;  oAmp=.22; pAmp=.05; dAmp=.05; }
  else if(uScene<2.5){ ro=vec3(2.6,1.0,7.5);  ta=vec3(.3,9.0,-1.);  focal=1.4;  oAmp=.14; pAmp=.04; dAmp=.05; }
  else if(uScene<3.5){ ro=vec3(.6,.2,6.8);    ta=vec3(0,5.5,-4.);   focal=1.4;  oAmp=.09; pAmp=.05; dAmp=.10; }
  else if(uScene<4.5){ ro=vec3(.8,2.6,12.5); ta=vec3(0,3.8,-2.); focal=1.25; oAmp=.10; pAmp=.03; dAmp=.05; }
  else{                ro=vec3(0,1.8,6.5);    ta=vec3(0,0,0);       focal=1.55; oAmp=.20; pAmp=.06; dAmp=.04; }
  float ct=uTime*uSpeed*mix(.25,1.,uActive);
  vec3 rel=ro-ta;
  rel.xz=r2(sin(ct*1.0)*oAmp)*rel.xz;
  rel.yz=r2(sin(ct*0.8)*pAmp)*rel.yz;
  rel*=1.0+sin(ct*0.6)*dAmp;
  rel.xz=r2(uYaw)*rel.xz; rel.yz=r2(uTilt)*rel.yz;
  ro=ta+rel;
  ro=mix(ro,ta,clamp((uZoom-.5)*.85,-.6,.78));
  vec3 ww=normalize(ta-ro); vec3 uu=normalize(cross(ww,vec3(0,1,0))); vec3 vv=normalize(cross(uu,ww));
  vec3 rd=normalize(uv.x*uu+uv.y*vv+focal*ww);
  Lvec=normalize(vec3(cos(aim),.52,sin(aim)));
  float breathe=mix(1.,.64+.36*sin(uTime*.62),uBreathe);
  float nf=vnoise(vec2(uTime*2.4,7.3)); float sput=step(.965,h11(floor(uTime*9.)));
  float flick=mix(1.,clamp(1.-smoothstep(.62,.97,nf)*.5-sput*.4,.2,1.),uFlicker);
  float pulse=mix(1.,.85+.4*pow(.5+.5*sin(uTime*1.7),4.),uPulse);
  Bright=clamp(uGlow*breathe*flick*pulse,0.,2.6);
  vec3 sky=sceneBg(rd,ro,ta); vec3 col=sky;
  float tiled=((uScene>1.5&&uScene<2.5)||(uScene>3.5&&uScene<4.5))?step(.5,uRecursive):0.;
  float cellMax=(tiled>.5)?1.1:9e9;
  float t=0.; float tmax=42.; float hit=0.;
  for(int i=0;i<MAXS;i++){
    vec3 p=ro+rd*t; float d=map(p);
    if(d<.001*t+.0008){ hit=1.; break; }
    t+=min(d*.55,cellMax); if(t>tmax) break; }
  vec3 sprC=mix(uLight,vec3(1),.4)+vec3(.05,.02,0.);
  vec3 ambCol=mix(uFog,uMat,.55);
  if(hit>.5){
    vec3 p=ro+rd*t; vec3 n=calcN(p);
    float ao=calcAO(p,n); float sh=shadow(p+n*.025,Lvec);
    float dif=clamp(dot(n,Lvec),0.,1.); float key=dif*sh;
    float fres=pow(clamp(1.+dot(rd,n),0.,1.),3.2);
    vec3 H=normalize(Lvec-rd); float spec=pow(clamp(dot(n,H),0.,1.),48.);
    float thick=clamp(-map(p-rd*.55)/.55,0.,1.);
    float val=(.06+key*Bright)*ao; val=clamp(val,0.,1.6);
    float pf=clamp(colorPat(val,p,thick),0.,1.);
    vec3 albedo=mix(uMat*mix(.5,1.08,pf),uLight,pf*.08);
    vec3 c=albedo*.05+ambCol*.10*ao;
    c+=albedo*uLight*key*Bright*1.2;
    c+=spec*uLight*Bright*.55;
    c+=fres*mix(uMat,uLight,.5)*Bright*.15;
    c+=uInner*thick*mix(uMat,uLight,.4)*(.4+.5*Bright);
    if(uScene<.5){ vec3 td=normalize(ta-ro); c+=mix(uFog,uLight,.5)*max(dot(n,-td),0.)*exp(-length(p)*.5)*Bright*.9; }
    for(int si=0;si<8;si++){
      if(float(si)>=uSprCount) break;
      vec4 spr=uSpr[si]; if(spr.w<1.) continue;
      vec3 toS=spr.xyz-p; float dist2=dot(toS,toS);
      c+=sprC*(spr.w-.8)*exp(-dist2*2.8)*max(dot(n,normalize(toS)),0.)*2.2; }
    if(uTexType>2.5){ c*=mix(.3,1.0,cellEdge(p)); }
    else { float tv=surfTex(p); c+=(tv-.5)*clamp(uGrain,0.,1.)*.55*(.25+.8*clamp(val,0.,1.)); }
    float fogK=mix(.02,.085,uVoid);
    float fDist=1.-exp(-max(t-3.0,0.)*fogK);
    float ground=(uScene>1.5&&uScene<4.5)?1.:0.;
    float fH=ground*smoothstep(3.0,-5.0,p.y);
    float hStr=clamp(uVoid*1.3,0.,1.);
    float fog=1.-(1.-fDist)*(1.-fH*hStr);
    col=mix(c,sky,fog); }
  for(int si=0;si<8;si++){
    if(float(si)>=uSprCount) break;
    vec4 spr=uSpr[si]; float sbr=spr.w; if(sbr<.005) continue;
    vec4 spd=uSprD[si];
    col+=sprC*wispGlow(ro,rd,spr.xyz,spd.xyz,spd.w,min(sbr,.92),t,hit); }
  col+=(h21(gl_FragCoord.xy*.5+fract(uTime)*vec2(31,17))-.5)*.012;
  float vig=1.-.30*pow(length(uv)*.55,2.2); col*=clamp(vig,0.,1.);
  gl_FragColor=vec4(clamp(col,0.,1.),1.); }
