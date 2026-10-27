// npm install --prefix /tmp/skyncar-test playwright
// NODE_PATH=/tmp/skyncar-test/node_modules node tests/ui.smoke.cjs
const {chromium}=require('playwright');
const http=require('node:http');const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
(async()=>{
 const root=path.resolve(__dirname,'../assets');
 const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+decodeURIComponent(req.url==='/'?'/index.html':req.url));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}fs.readFile(file,(e,b)=>{res.writeHead(e?404:200,{'Content-Type':file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.png')?'image/png':'text/html'});res.end(e?'not found':b);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:1024,height:600}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:'+server.address().port);await page.locator('h1').filter({hasText:'Hola, Isias'}).waitFor();
 for(const id of ['navigation','music','phone','vehicle','split','apps','radio','settings','home']){await page.locator(`[data-page="${id}"]`).click();assert(await page.locator('#screen').innerText());}
 await page.locator('[data-page="settings"]').click();await page.locator('[name="name"]').fill('Isias Romero');await page.locator('[name="theme"]').selectOption('light');await page.getByText('Guardar preferencias',{exact:true}).click();assert.equal(await page.locator('html').getAttribute('data-theme'),'light');
 await page.reload();await page.locator('h1').filter({hasText:'Hola, Isias Romero'}).waitFor();
 await page.locator('[data-page="vehicle"]').click();await page.locator('[name="current"]').fill('12000');await page.locator('[name="last"]').fill('10000');await page.locator('[name="interval"]').fill('5000');await page.getByText('Guardar mantenimiento',{exact:true}).click();assert.match(await page.locator('#screen').innerText(),/3[.,]000 km/);
 await page.reload();await page.locator('[data-page="vehicle"]').click();assert.equal(await page.locator('[name="current"]').inputValue(),'12000');
 await page.locator('[data-page="home"]').click();await page.locator('#sleep').click();assert(await page.locator('#rest').isVisible());await page.locator('#wake').click();assert(!(await page.locator('#rest').isVisible()));
 for(const viewport of [{width:1024,height:600},{width:800,height:480},{width:1280,height:720}]){await page.setViewportSize(viewport);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));}
 await page.setViewportSize({width:1024,height:600});await page.locator('[data-page="settings"]').click();await page.locator('[name="name"]').fill('Isias');await page.locator('[name="theme"]').selectOption('dark');await page.getByText('Guardar preferencias',{exact:true}).click();await page.locator('[data-page="home"]').click();
 // Exercise the UI contract with native-style factory activities, not fake hardware claims.
 const nativePage=await browser.newPage({viewport:{width:1024,height:600}});
 await nativePage.addInitScript(()=>{
   window.nativeCalls=[];
   const apps=[
     {name:'Radio FM original',package:'com.vendor.fm',component:'com.vendor.fm/.Radio',system:true},
     {name:'Bluetooth original',package:'com.vendor.bt',component:'com.vendor.bt/.Music',system:true},
     {name:'Teléfono original',package:'com.vendor.bt',component:'com.vendor.bt/.Phone',system:true},
     {name:'Waze',package:'com.waze',component:'com.waze/.Main',system:false},
     {name:'Inicio de fábrica',package:'com.vendor.home',component:'com.vendor.home/.Home',system:true}
   ];
   window.SkyNative={load:()=>localStorage.getItem('nativeProfile')||'{}',save:j=>localStorage.setItem('nativeProfile',j),apps:()=>JSON.stringify(apps),media:()=>'{"available":false}',assignments:()=>localStorage.getItem('assignments')||'{}',
     setAssignment:(slot,id)=>{const a=JSON.parse(localStorage.getItem('assignments')||'{}');a[slot]={id,name:apps.find(x=>x.component===id).name};localStorage.setItem('assignments',JSON.stringify(a));},
     open:a=>window.nativeCalls.push(a),app:a=>window.nativeCalls.push(a)};
 });
 await nativePage.goto('http://127.0.0.1:'+server.address().port);
 await nativePage.getByRole('button',{name:'Waze',exact:true}).click();
 await nativePage.getByRole('button',{name:'Google Maps',exact:true}).click();
 assert.deepEqual(await nativePage.evaluate(()=>nativeCalls),['waze','googleMaps']);
 await nativePage.locator('[data-page="apps"]').click();
 assert.equal(await nativePage.locator('[data-component]').count(),5);
 await nativePage.locator('[data-filter="system"]').click();assert.equal(await nativePage.locator('[data-component]').count(),4);
 await nativePage.locator('[data-favorite="com.vendor.bt/.Phone"]').click();
 await nativePage.locator('[data-filter="favorites"]').click();assert.equal(await nativePage.locator('[data-component]').count(),1);
 await nativePage.locator('[data-component="com.vendor.bt/.Phone"]').click();
 assert.equal(await nativePage.evaluate(()=>nativeCalls[nativeCalls.length-1]),'com.vendor.bt/.Phone');
 await nativePage.locator('[data-page="radio"]').click();await nativePage.getByRole('button',{name:'Elegir aplicación FM',exact:true}).click();
 await nativePage.locator('#search').fill('com.vendor.fm');assert.equal(await nativePage.locator('[data-component]').count(),1);
 await nativePage.locator('[data-component="com.vendor.fm/.Radio"]').click();
 await nativePage.getByRole('button',{name:'Abrir radio FM',exact:true}).click();
 assert.equal(await nativePage.evaluate(()=>nativeCalls[nativeCalls.length-1]),'radio');
 await nativePage.reload();await nativePage.locator('[data-page="radio"]').click();assert.match(await nativePage.locator('#screen').innerText(),/Radio FM original/);
 await nativePage.locator('[data-page="settings"]').click();await nativePage.locator('[data-pick="navigation"]').click();await nativePage.locator('[data-component="com.waze/.Main"]').click();
 assert.match(await nativePage.locator('.app-assignments').innerText(),/Waze/);
 await nativePage.getByRole('button',{name:'Dejar SkyNCar y elegir otro inicio',exact:true}).click();assert.equal(await nativePage.evaluate(()=>nativeCalls[nativeCalls.length-1]),'homeSettings');
 await nativePage.locator('[data-pick="factory"]').click();await nativePage.locator('[data-component="com.vendor.home/.Home"]').click();await nativePage.locator('[data-page="home"]').click();await nativePage.locator('[data-action="factory"]').click();assert.equal(await nativePage.evaluate(()=>nativeCalls[nativeCalls.length-1]),'factory');
 await nativePage.close();
 console.log('Native bridge UI contract: factory/system activities, search, favorites, distinct Waze/Maps, FM assignment persistence and original home access passed.');
 const output=path.resolve(__dirname,'../build');fs.mkdirSync(output,{recursive:true});await page.screenshot({path:path.join(output,'home.png')});assert.deepEqual(errors,[]);console.log('UI: nine screens, profile and oil persistence, themes, rest and 3 sizes passed.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
